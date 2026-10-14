package com.triptab.app.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.triptab.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/** Where the Claude API key and model choice are kept (this phone only). */
class ScanSettings(context: Context) {
    private val prefs = context.getSharedPreferences("scan_settings", Context.MODE_PRIVATE)
    var apiKey: String
        get() = prefs.getString("claude_api_key", "").orEmpty()
        set(v) = prefs.edit().putString("claude_api_key", v.trim()).apply()
    var model: String
        get() = prefs.getString("claude_model", ClaudeMenuReader.DEFAULT_MODEL) ?: ClaudeMenuReader.DEFAULT_MODEL
        set(v) = prefs.edit().putString("claude_model", v).apply()
    val hasClaude get() = apiKey.isNotBlank()
}

enum class ScanEngine { ON_DEVICE, CLAUDE }

object MenuScanner {

    /** Decodes a photo upright and no larger than [maxEdge] px on its long side. */
    fun loadBitmap(context: Context, uri: Uri, maxEdge: Int): Bitmap {
        val cr = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val bmp = cr.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalArgumentException("Couldn't open that photo")
        val rotation = cr.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        val scale = maxEdge.toFloat() / max(bmp.width, bmp.height)
        val m = Matrix().apply {
            if (rotation != 0) postRotate(rotation.toFloat())
            if (scale < 1f) postScale(scale, scale)
        }
        return if (m.isIdentity) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    private fun jpeg(b: Bitmap): ByteArray = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()

    /** Free, offline: ML Kit text recognition + layout parser. Reads Latin script only. */
    suspend fun readOnDevice(context: Context, uris: List<Uri>): ScanResult = withContext(Dispatchers.Default) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val all = mutableListOf<ScannedItem>()
            var service: Double? = null; var tax: Double? = null
            for (uri in uris) {
                val bmp = loadBitmap(context, uri, 2400)
                val text = suspendCancellableCoroutine { cont ->
                    recognizer.process(InputImage.fromBitmap(bmp, 0))
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resumeWithException(it) }
                }
                val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { l ->
                    val r = l.boundingBox ?: return@mapNotNull null
                    OcrLine(l.text, r.left, r.top, r.right, r.bottom)
                }
                val res = MenuParser.parse(lines)
                all += res.items
                service = service ?: res.servicePct; tax = tax ?: res.taxPct
            }
            ScanResult(all, service, tax)
        } finally {
            recognizer.close()
        }
    }

    /** Claude: Arabic + English, any layout. Needs internet and the user's API key. */
    suspend fun readWithClaude(context: Context, uris: List<Uri>, settings: ScanSettings): ScanResult =
        withContext(Dispatchers.IO) {
            val jpegs = uris.map { jpeg(loadBitmap(context, it, 2000)) }
            ClaudeMenuReader.read(settings.apiKey, settings.model, jpegs)
        }
}
