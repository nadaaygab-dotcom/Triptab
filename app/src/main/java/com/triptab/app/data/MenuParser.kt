package com.triptab.app.data

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** One line of recognised text with its position on the photo (pixels). */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val height get() = bottom - top
    val centerY get() = (top + bottom) / 2.0
    val centerX get() = (left + right) / 2.0
}

/** A menu item found in a photo, before the user reviews it. */
data class ScannedItem(
    val category: String,
    val nameEn: String,
    val nameAr: String = "",
    val price: Double,
)

/** What a scan found: items, plus service/tax percentages if the menu states them. */
data class ScanResult(
    val items: List<ScannedItem>,
    val servicePct: Double? = null,
    val taxPct: Double? = null,
)

/**
 * Turns OCR lines into menu items by layout: a price is paired with the text on the same row
 * (names wrapped over two lines are joined), and unpriced short lines become category headings.
 */
object MenuParser {
    private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"
    private val CURRENCY = Regex("""(?i)\b(?:L\.?\s?E\.?|EGP|E\.?G\.?P|EGY)\b|ج\.?\s?م\.?|جنيه""")
    private val NUM = """\d{1,5}(?:[.,]\d{1,2})?"""
    /** A line that is only a price: "100", "LE 100", "100 –", "LE 90-125", "290/ 380". */
    private val PRICE_ONLY = Regex("""^[\s\-–—:.]*($NUM)(?:\s*([-–/])\s*($NUM))?[\s\-–—:.]*$""")
    /** Text ending in a price: "Espresso ...... 75". */
    private val TRAILING_PRICE = Regex("""^(.*?\p{L}.*?)[\s.·…:\-–—]+($NUM)(?:\s*([-–/])\s*($NUM))?[\s\-–—]*$""")
    private val LETTER = Regex("""\p{L}""")
    private val ARABIC = Regex("""[؀-ۿ]""")
    private val PERCENT = Regex("""(\d{1,2}(?:[.,]\d)?)\s*[%٪]""")

    fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            val i = ARABIC_DIGITS.indexOf(c)
            sb.append(if (i >= 0) ('0' + i) else c)
        }
        return CURRENCY.replace(sb.toString(), " ").replace('٫', '.').replace(Regex("""\s+"""), " ").trim()
    }

    private fun num(s: String) = s.replace(',', '.').toDouble()

    private data class Price(val first: Double, val sep: String?, val second: Double?)

    private fun priceOnly(text: String): Price? {
        val m = PRICE_ONLY.matchEntire(normalize(text)) ?: return null
        val a = num(m.groupValues[1])
        val b = m.groupValues[3].takeIf { it.isNotEmpty() }?.let(::num)
        return Price(a, m.groupValues[2].ifEmpty { null }, b)
    }

    /** At least two letters once currency words ("LE", "EGP", "ج.م") are removed. */
    private fun isText(text: String) = LETTER.findAll(normalize(text)).count() >= 2

    /** Expands one name + price into items: "A / B" with "290/380" → two items; "90-125" keeps the low price. */
    private fun expand(category: String, rawName: String, p: Price): List<ScannedItem> {
        val name = rawName.trim().trim('.', '-', '–', ':', '·', ' ')
        fun item(n: String, price: Double) =
            if (ARABIC.containsMatchIn(n) && !Regex("[A-Za-z]").containsMatchIn(n))
                ScannedItem(category, nameEn = n, nameAr = n, price = price)
            else ScannedItem(category, nameEn = n, price = price)

        if (p.second != null && p.sep == "/") {
            val parts = name.split("/").map { it.trim() }.filter { it.isNotEmpty() }
            return if (parts.size == 2) listOf(item(parts[0], p.first), item(parts[1], p.second))
            else listOf(item(name, p.first), item("$name (large)", p.second))
        }
        if (p.second != null) {
            val lo = min(p.first, p.second); val hi = max(p.first, p.second)
            return listOf(item("$name (${fmt(lo)}–${fmt(hi)})", lo))
        }
        return listOf(item(name, p.first))
    }

    private fun fmt(d: Double) = if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()

    fun parse(lines: List<OcrLine>, defaultCategory: String = "Scanned"): ScanResult {
        val clean = lines.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() }
        val (service, tax) = findCharges(clean.map { it.text })

        // 1) Classify lines.
        data class Entry(val y: Double, val left: Int, val name: String, val price: Price)
        val entries = mutableListOf<Entry>()
        val prices = mutableListOf<Pair<OcrLine, Price>>()
        val texts = mutableListOf<OcrLine>()
        for (l in clean) {
            val p = priceOnly(l.text)
            if (p != null) { prices += l to p; continue }
            val norm = normalize(l.text)
            val m = TRAILING_PRICE.matchEntire(norm)
            if (m != null && isText(m.groupValues[1]) && !PERCENT.containsMatchIn(norm)) {
                val a = num(m.groupValues[2])
                val b = m.groupValues[4].takeIf { it.isNotEmpty() }?.let(::num)
                entries += Entry(l.centerY, l.left, m.groupValues[1], Price(a, m.groupValues[3].ifEmpty { null }, b))
                continue
            }
            if (isText(l.text) && !PERCENT.containsMatchIn(l.text)) texts += l
        }

        // 2) Pair each price-only line with the text on its row. Prefer text to the left (names
        //    usually come before prices); a name wrapped over two lines is joined.
        val used = mutableSetOf<OcrLine>()
        for ((pl, p) in prices.sortedBy { it.first.top }) {
            val h = max(pl.height, texts.map { it.height }.average().takeIf { !it.isNaN() }?.toInt() ?: pl.height)
            val rowMates = texts.filter { it !in used && abs(it.centerY - pl.centerY) <= h * 0.95 }
            if (rowMates.isEmpty()) continue
            val left = rowMates.filter { it.right <= pl.left + h }
            val right = rowMates.filter { it.left >= pl.right - h }
            val side = left.ifEmpty { right }.ifEmpty { rowMates }
            // Closest line horizontally, plus any line stacked directly with it (wrapped name).
            val nearest = side.minBy { abs(it.centerY - pl.centerY) * 2 + abs(it.centerX - pl.centerX) / 10 }
            // The other half of a wrapped name may sit just outside the price's row, so look at all
            // unused lines in the same column directly above/below — but only on the far side of
            // the price centre, so a neighbouring item is never swallowed.
            val group = texts.filter {
                it !in used && (it == nearest || (abs(it.left - nearest.left) <= h * 2 &&
                    abs(it.centerY - nearest.centerY) <= h * 1.6 &&
                    (it.centerY - nearest.centerY) * (pl.centerY - nearest.centerY) > 0 &&
                    prices.none { (op, _) -> op !== pl && abs(op.centerY - it.centerY) < abs(pl.centerY - it.centerY) }))
            }.sortedBy { it.top }
            used += group
            entries += Entry(pl.centerY, group.minOf { it.left }, group.joinToString(" ") { it.text }, p)
        }

        // 3) Leftover short text lines are headings; each item takes the nearest heading above it.
        //    A heading sits in the names' column on a row of its own (not beside a price),
        //    which keeps e.g. an Arabic names column from being taken for headings.
        val rowYs = entries.map { it.y } + prices.map { it.first.centerY }
        val nameLeft = entries.map { it.left }.sorted().let { if (it.isEmpty()) 0 else it[it.size / 2] }
        val avgH = texts.map { it.height }.average().takeIf { !it.isNaN() } ?: 20.0
        val headings = texts.filter { t ->
            t !in used && t.text.length in 3..32 && !t.text.contains(Regex("[\\d(/]")) &&
                t.text.trim().split(Regex("\\s+")).size <= 4 &&
                rowYs.none { abs(it - t.centerY) < avgH * 0.8 } &&
                abs(t.left - nameLeft) < avgH * 6
        }.sortedBy { it.top }
        val items = entries.sortedBy { it.y }.flatMap { e ->
            val cat = headings.lastOrNull { it.centerY < e.y }?.text?.let(::titleCase) ?: defaultCategory
            expand(cat, e.name, e.price)
        }.filter { it.price > 0 && it.nameEn.length >= 2 }
        return ScanResult(items, service, tax)
    }

    /** Finds "12% service" / "14% tax" (English or Arabic) anywhere in the text. */
    fun findCharges(texts: List<String>): Pair<Double?, Double?> {
        var service: Double? = null; var tax: Double? = null
        for (raw in texts) {
            val t = normalize(raw).lowercase()
            for (m in PERCENT.findAll(t)) {
                val v = num(m.groupValues[1])
                val after = t.substring(m.range.last + 1).take(25)
                val before = t.substring(0, m.range.first).takeLast(25)
                val near = "$before $after"
                when {
                    service == null && Regex("service|خدمة|خدمه").containsMatchIn(after.ifBlank { near }) -> service = v
                    tax == null && Regex("tax|vat|ضريب|ضرائب").containsMatchIn(after.ifBlank { near }) -> tax = v
                    service == null && Regex("service|خدمة|خدمه").containsMatchIn(near) -> service = v
                    tax == null && Regex("tax|vat|ضريب|ضرائب").containsMatchIn(near) -> tax = v
                }
            }
        }
        return service to tax
    }

    private fun titleCase(s: String): String =
        if (s == s.uppercase() && Regex("[A-Z]").containsMatchIn(s))
            s.lowercase().split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        else s
}

/** Matches scanned items against the place's existing menu so re-scanning doesn't create duplicates. */
object MenuMerge {
    fun key(s: String) = s.lowercase().replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()

    fun findExisting(place: Place, s: ScannedItem): MenuItem? = place.items.firstOrNull {
        (s.nameEn.isNotBlank() && key(it.nameEn) == key(s.nameEn)) ||
            (s.nameAr.isNotBlank() && it.nameAr.isNotBlank() && key(it.nameAr) == key(s.nameAr))
    }
}
