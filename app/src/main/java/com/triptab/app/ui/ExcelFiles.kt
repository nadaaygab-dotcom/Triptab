package com.triptab.app.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.triptab.app.data.BillExport
import com.triptab.app.data.ImportedOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Writing Excel files to share (WhatsApp etc.) and reading files the admin imports. */
object ExcelFiles {

    /** Saves the workbook in the app's cache and opens the share sheet. */
    fun share(context: Context, fileName: String, bytes: ByteArray, title: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 86_400_000) it.delete() }
        val f = File(dir, fileName).apply { writeBytes(bytes) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = BillExport.MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName.removeSuffix(".xlsx"))
            clipData = ClipData.newRawUri(fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, title))
    }

    /** A file's families (a phone tracking several families sends them in one file), or why it couldn't be read. */
    data class ReadResult(val fileName: String, val orders: List<ImportedOrder>, val error: String?) {
        val ok get() = error == null
    }

    /** Reads each picked/shared file; a file that fails doesn't stop the others. */
    suspend fun readBills(context: Context, uris: List<Uri>): List<ReadResult> = withContext(Dispatchers.IO) {
        uris.map { uri ->
            val name = displayName(context, uri)
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { s ->
                    val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(16384); var total = 0
                    while (true) {
                        val n = s.read(buf); if (n < 0) break
                        total += n; if (total > 15_000_000) throw IOException("This file is too large to be a TripTab bill.")
                        out.write(buf, 0, n)
                    }
                    out.toByteArray()
                } ?: throw IOException("Couldn't open the file.")
                ReadResult(name, BillExport.parseAll(bytes), null)
            } catch (e: IOException) {
                ReadResult(name, emptyList(), e.message ?: "Couldn't read the file.")
            } catch (e: Exception) {
                ReadResult(name, emptyList(), "Couldn't read the file.")
            }
        }
    }

    private fun displayName(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file"

    /** Uris carried by a share / open-with intent from WhatsApp or a file manager. */
    fun urisFrom(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            Intent.ACTION_SEND -> listOfNotNull(
                if (android.os.Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE ->
                (if (android.os.Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)).orEmpty()
            else -> emptyList()
        }
    }
}
