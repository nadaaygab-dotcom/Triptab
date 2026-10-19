package com.triptab.app.data

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/** A minimal Excel (.xlsx) writer and reader — no external library, works on Android and in tests. */
object Xlsx {

    sealed interface Cell
    data class Text(val value: String, val bold: Boolean = false) : Cell
    data class Num(val value: Double, val bold: Boolean = false, val money: Boolean = true) : Cell
    data object Empty : Cell

    class Sheet(val name: String, val hidden: Boolean = false, val widths: List<Int> = emptyList()) {
        val rows = mutableListOf<List<Cell>>()
        fun row(vararg cells: Any?) {
            rows += cells.map {
                when (it) {
                    null -> Empty
                    is Cell -> it
                    is Int -> Num(it.toDouble(), money = false)
                    is Number -> Num(it.toDouble())
                    else -> Text(it.toString())
                }
            }
        }
        fun blank() { rows += emptyList<Cell>() }
    }

    // ---------- Writer ----------

    private fun esc(s: String) = buildString(s.length) {
        for (c in s) when {
            c == '&' -> append("&amp;"); c == '<' -> append("&lt;"); c == '>' -> append("&gt;")
            c == '"' -> append("&quot;")
            c.code < 0x20 && c != '\t' && c != '\n' && c != '\r' -> {}   // not allowed in XML
            else -> append(c)
        }
    }

    fun colName(index: Int): String {
        var n = index + 1; val sb = StringBuilder()
        while (n > 0) { val r = (n - 1) % 26; sb.insert(0, 'A' + r); n = (n - 1) / 26 }
        return sb.toString()
    }

    /** Style ids in styles.xml: 0 normal, 1 bold, 2 money, 3 bold money, 4 integer, 5 bold integer. */
    private fun style(c: Cell) = when (c) {
        is Text -> if (c.bold) 1 else 0
        is Num -> when { c.money && c.bold -> 3; c.money -> 2; c.bold -> 5; else -> 4 }
        Empty -> 0
    }

    private fun sheetXml(s: Sheet): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        if (s.widths.isNotEmpty()) {
            append("<cols>")
            s.widths.forEachIndexed { i, w -> append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
            append("</cols>")
        }
        append("<sheetData>")
        s.rows.forEachIndexed { ri, cells ->
            append("""<row r="${ri + 1}">""")
            cells.forEachIndexed { ci, c ->
                val ref = colName(ci) + (ri + 1)
                when (c) {
                    is Text -> append("""<c r="$ref" s="${style(c)}" t="inlineStr"><is><t xml:space="preserve">${esc(c.value)}</t></is></c>""")
                    is Num -> {
                        val v = if (c.money) Math.round(c.value * 100) / 100.0 else c.value
                        append("""<c r="$ref" s="${style(c)}"><v>${if (v % 1.0 == 0.0) v.toLong() else v}</v></c>""")
                    }
                    Empty -> {}
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    fun write(sheets: List<Sheet>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            fun put(name: String, text: String) { z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray(Charsets.UTF_8)); z.closeEntry() }
            put("[Content_Types].xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
                append("""<Default Extension="xml" ContentType="application/xml"/>""")
                append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
                append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
                sheets.indices.forEach { append("""<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""") }
                append("</Types>")
            })
            put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            put("xl/workbook.xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
                sheets.forEachIndexed { i, s ->
                    append("""<sheet name="${esc(s.name.take(31))}" sheetId="${i + 1}"${if (s.hidden) " state=\"hidden\"" else ""} r:id="rId${i + 1}"/>""")
                }
                append("</sheets></workbook>")
            })
            put("xl/_rels/workbook.xml.rels", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                sheets.indices.forEach { append("""<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""") }
                append("""<Relationship Id="rId${sheets.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
                append("</Relationships>")
            })
            put("xl/styles.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="1"><numFmt numFmtId="164" formatCode="#,##0.00"/></numFmts><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="6"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="164" fontId="1" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/><xf numFmtId="1" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="1" fontId="1" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>""")
            sheets.forEachIndexed { i, s -> put("xl/worksheets/sheet${i + 1}.xml", sheetXml(s)) }
        }
        return out.toByteArray()
    }

    // ---------- Reader ----------

    private const val MAX_PART = 20L * 1024 * 1024

    /** Reads every sheet as rows of cell text (numbers as plain text, e.g. "12.5"). Tolerates files re-saved by Excel. */
    fun read(bytes: ByteArray): Map<String, List<List<String>>> {
        val parts = HashMap<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val name = e.name.trimStart('/')
                    if (!name.endsWith(".xml") && !name.endsWith(".rels")) continue
                    val buf = ByteArrayOutputStream(); val tmp = ByteArray(16384); var total = 0L
                    while (true) {
                        val n = z.read(tmp); if (n < 0) break
                        total += n; if (total > MAX_PART) throw IOException("File is too large")
                        buf.write(tmp, 0, n)
                    }
                    parts[name] = buf.toByteArray()
                }
            }
        } catch (e: IOException) { throw e } catch (e: Exception) { throw IOException("This file isn't an Excel (.xlsx) file") }
        val wb = parts["xl/workbook.xml"] ?: throw IOException("This file isn't an Excel (.xlsx) file")

        val rels = parts["xl/_rels/workbook.xml.rels"]?.let(::parse)?.let { d ->
            d.byTag("Relationship").associate { it.getAttribute("Id") to it.getAttribute("Target") }
        } ?: emptyMap()
        val shared = parts["xl/sharedStrings.xml"]?.let(::parse)?.byTag("si")?.map { si ->
            si.byTag("t").joinToString("") { it.textContent }
        } ?: emptyList()

        val result = LinkedHashMap<String, List<List<String>>>()
        for (s in parse(wb).byTag("sheet")) {
            val name = s.getAttribute("name")
            val rid = s.attributes.let { a -> (0 until a.length).map { a.item(it) }.firstOrNull { it.nodeName.endsWith(":id") }?.nodeValue }
            var target = rels[rid] ?: continue
            target = target.removePrefix("/").let { if (it.startsWith("xl/")) it else "xl/$it" }
            val sheet = parts[target] ?: continue
            val rows = ArrayList<List<String>>()
            for (r in parse(sheet).byTag("row")) {
                val cells = ArrayList<String>()
                var next = 0
                for (c in r.childElements("c")) {
                    val ref = c.getAttribute("r")
                    val col = if (ref.isNotEmpty()) colIndex(ref) else next
                    while (cells.size < col) cells += ""
                    val type = c.getAttribute("t")
                    val v = c.childElements("v").firstOrNull()?.textContent.orEmpty()
                    val text = when (type) {
                        "s" -> v.toIntOrNull()?.let { shared.getOrNull(it) }.orEmpty()
                        "inlineStr" -> c.byTag("t").joinToString("") { it.textContent }
                        else -> v
                    }
                    if (cells.size == col) cells += text else cells[col] = text
                    next = col + 1
                }
                val ri = r.getAttribute("r").toIntOrNull()?.minus(1) ?: rows.size
                while (rows.size < ri) rows += emptyList<String>()
                rows += cells
            }
            result[name] = rows
        }
        return result
    }

    private fun colIndex(ref: String): Int {
        var n = 0
        for (ch in ref) { if (!ch.isLetter()) break; n = n * 26 + (ch.uppercaseChar() - 'A' + 1) }
        return n - 1
    }

    private fun parse(xml: ByteArray): org.w3c.dom.Document {
        val f = DocumentBuilderFactory.newInstance()
        f.isNamespaceAware = true
        f.isExpandEntityReferences = false
        runCatching { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { f.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        return f.newDocumentBuilder().parse(ByteArrayInputStream(xml))
    }

    private fun org.w3c.dom.Node.byTag(local: String): List<Element> {
        val list = when (this) {
            is org.w3c.dom.Document -> getElementsByTagNameNS("*", local)
            is Element -> getElementsByTagNameNS("*", local)
            else -> return emptyList()
        }
        return (0 until list.length).map { list.item(it) as Element }
    }

    private fun Element.childElements(local: String): List<Element> {
        val out = ArrayList<Element>(); var n = firstChild
        while (n != null) {
            if (n is Element && (n.localName ?: n.nodeName.substringAfter(':')) == local) out += n
            n = n.nextSibling
        }
        return out
    }
}
