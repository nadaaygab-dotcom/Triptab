package com.triptab.app

import com.triptab.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MenuParserTest {

    /** Rows modelled on the Ibis menu photo: English name | price | Arabic name, ~33px apart. */
    private fun ibisLike(): List<OcrLine> {
        val rows = listOf(
            "EGYPTIAN BREAKFAST" to "LE 350",
            "CARROT CAKE / BANANA CAKE" to "LE 95",
            "SOUP SPECIAL" to "LE 110",
            "CAESAR / CAESAR WITH CHICKEN" to "LE 290/380",
            "IBIS HOUSE SALAD" to "LE 330",
        )
        val out = mutableListOf<OcrLine>()
        out += OcrLine("SALADS & STARTERS", 40, 470, 220, 488)
        rows.forEachIndexed { i, (name, price) ->
            val y = 512 + i * 33
            out += OcrLine(name, 40, y, 40 + name.length * 9, y + 16)
            out += OcrLine(price, 360, y + 2, 420, y + 16)
            out += OcrLine("ﻓﻄﺎر ﻣﺼﺮي", 600, y, 700, y + 16) // Arabic column
        }
        // A name wrapped over two lines with the price centred between them.
        out += OcrLine("FRESH APPLE CRUMBLE WITH", 40, 700, 260, 714)
        out += OcrLine("VANILLA ICE CREAM", 40, 716, 200, 730)
        out += OcrLine("LE 200", 360, 708, 420, 722)
        // A range price.
        out += OcrLine("FRESH WAFFLES / CREPES", 40, 760, 240, 774)
        out += OcrLine("LE 90-125", 350, 760, 420, 774)
        // Footer charges.
        out += OcrLine("12% service charge will be added", 40, 900, 400, 914)
        return out
    }

    @Test fun pairsNamesWithPricesByRow() {
        val r = MenuParser.parse(ibisLike())
        val byName = r.items.associateBy { it.nameEn }
        assertEquals(350.0, byName["EGYPTIAN BREAKFAST"]!!.price, 0.0)
        assertEquals(110.0, byName["SOUP SPECIAL"]!!.price, 0.0)
        assertEquals(330.0, byName["IBIS HOUSE SALAD"]!!.price, 0.0)
        assertEquals("Salads & Starters", byName["SOUP SPECIAL"]!!.category)
    }

    @Test fun splitsSlashPricesAndKeepsRangeLow() {
        val r = MenuParser.parse(ibisLike())
        val names = r.items.map { it.nameEn }
        assertTrue(names.toString(), "CAESAR" in names && "CAESAR WITH CHICKEN" in names)
        assertEquals(380.0, r.items.first { it.nameEn == "CAESAR WITH CHICKEN" }.price, 0.0)
        val waffle = r.items.first { it.nameEn.startsWith("FRESH WAFFLES") }
        assertEquals(90.0, waffle.price, 0.0)
        assertTrue(waffle.nameEn.contains("90–125"))
    }

    @Test fun joinsWrappedNames() {
        val r = MenuParser.parse(ibisLike())
        assertTrue(r.items.map { it.nameEn }.toString(),
            r.items.any { it.nameEn == "FRESH APPLE CRUMBLE WITH VANILLA ICE CREAM" && it.price == 200.0 })
    }

    @Test fun readsTextWithTrailingPriceAndArabicDigits() {
        val r = MenuParser.parse(listOf(
            OcrLine("Turkish coffee ........ 66", 10, 10, 300, 30),
            OcrLine("Fresh juice  ١١٧", 10, 40, 300, 60),
            OcrLine("7 UP", 10, 70, 60, 90), OcrLine("50", 200, 70, 230, 90),
        ))
        val m = r.items.associate { it.nameEn to it.price }
        assertEquals(66.0, m["Turkish coffee"]!!, 0.0)
        assertEquals(117.0, m["Fresh juice"]!!, 0.0)
        assertEquals(50.0, m["7 UP"]!!, 0.0)
    }

    @Test fun findsServiceAndTax() {
        val (s, t) = MenuParser.findCharges(listOf("سيضاف ١٢٪ رسوم خدمة و١٤٪ ضرائب إلى الفاتورة النهائية"))
        assertEquals(12.0, s!!, 0.0); assertEquals(14.0, t!!, 0.0)
        val (s2, t2) = MenuParser.findCharges(listOf("12% service charge will be added"))
        assertEquals(12.0, s2!!, 0.0); assertNull(t2)
        assertEquals(12.0, MenuParser.parse(ibisLike()).servicePct!!, 0.0)
    }

    @Test fun mergeFindsExistingItems() {
        val p = Place(name = "X", items = listOf(MenuItem(category = "c", nameEn = "Turkish Coffee", price = 60.0)))
        assertNotNull(MenuMerge.findExisting(p, ScannedItem("c", "turkish  coffee", price = 66.0)))
        assertNull(MenuMerge.findExisting(p, ScannedItem("c", "Latte", price = 66.0)))
    }

    /** Dev aid: parse OCR lines dumped from a real photo (set OCR_LINES_DIR). Not an assertion. */
    @Test fun parseRealPhotoDumps() {
        val dir = System.getenv("OCR_LINES_DIR")?.let(::File) ?: return
        dir.listFiles { f -> f.name.endsWith(".lines.jsonl") }?.sorted()?.forEach { f ->
            val lines = f.readLines().filter { it.isNotBlank() }.map { l ->
                val arr = kotlinx.serialization.json.Json.parseToJsonElement(l) as kotlinx.serialization.json.JsonArray
                fun i(k: Int) = arr[k].toString().toInt()
                OcrLine(arr[0].toString().trim('"'), i(1), i(2), i(3), i(4))
            }
            val r = MenuParser.parse(lines)
            File(dir, f.name.replace(".lines.jsonl", ".parsed.txt")).writeText(
                r.items.joinToString("\n") { "${it.category} | ${it.nameEn} | ${it.price}" } +
                    "\nservice=${r.servicePct} tax=${r.taxPct}\n")
        }
    }
}
