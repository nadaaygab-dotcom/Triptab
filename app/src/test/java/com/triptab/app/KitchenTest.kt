package com.triptab.app

import com.triptab.app.data.*
import org.junit.Assert.*
import org.junit.Test

class KitchenTest {
    private fun line(member: String, item: String, qty: Int, cat: String, place: String = "Kom El Dikka", day: Int = 1, ar: String = "") =
        ImportedLine(member, day, "2026-10-2${1 + day}", place, cat, item, qty, 100.0, ar)

    private fun compilation() = Compilation(name = "Fayoum", imports = listOf(
        ImportedOrder("a", "Nada", members = listOf("Nada", "Yehia"), lines = listOf(
            line("Nada", "Grilled Kofta", 1, "Main Course", ar = "كفته مشوية"),
            line("Yehia", "Grilled Kofta", 1, "Main Course"),
            line("Yehia", "Large Mineral Water", 2, "Soft Drinks & Water"),
            line("Nada", "Baladi Salad", 1, "Salads"),
            line("Nada", "Om Aly (with mixed nuts)", 1, "Dessert", day = 2),
            line("Nada", "Margherita", 1, "Pizza", place = "Ibis Restaurant"),
        )),
        ImportedOrder("b", "Sara", members = listOf("Sara", "Omar"), lines = listOf(
            line("Sara", "grilled kofta", 1, "Main Course"),       // same dish, different capitalisation
            line("Omar", "Penne Pesto", 2, "Pasta"),
            line("Omar", "Large Mineral Water", 1, "Soft Drinks & Water"),
        )),
    ))

    @Test fun totalsPerDishAcrossFamilies() {
        val k = Kitchen.order(compilation(), "Kom El Dikka", "2026-10-22")
        val q = k.lines.associate { it.item to it.quantity }
        assertEquals(mapOf("Grilled Kofta" to 3, "Penne Pesto" to 2, "Large Mineral Water" to 3, "Baladi Salad" to 1), q)
        assertEquals(9, k.dishes)
        assertEquals(4, k.people)
        // Course order: salads → mains/pasta → drinks.
        assertEquals(listOf("Salads", "Main Course", "Pasta", "Soft Drinks & Water"), k.sections.map { it.first })
        assertEquals("كفته مشوية", k.lines.first { it.item == "Grilled Kofta" }.itemAr)
        assertEquals(listOf("Nada" to 1, "Yehia" to 1, "Sara" to 1), k.lines.first { it.item == "Grilled Kofta" }.who)
        // Other restaurants and days are left out.
        assertTrue(k.lines.none { it.item == "Margherita" || it.item.startsWith("Om Aly") })
        assertEquals(10, Kitchen.order(compilation(), "Kom El Dikka", null).dishes)  // all days
    }

    @Test fun messageForWhatsApp() {
        val c = compilation()
        val k = Kitchen.order(c, "Kom El Dikka", "2026-10-22")
        val plain = Kitchen.message(c, k, withNames = false, withArabic = false, note = "We arrive at 2 pm")
        assertTrue(plain, plain.startsWith("Order for Kom El Dikka\nFayoum group · Day 1 · Thu 22 Oct\n9 items for 4 people\nWe arrive at 2 pm"))
        assertTrue(plain.contains("*Main Course*\n3 × Grilled Kofta\n"))
        assertFalse(plain.contains("Nada"))
        val full = Kitchen.message(c, k, withNames = true, withArabic = true, note = "")
        assertTrue(full, full.contains("3 × Grilled Kofta (كفته مشوية) — Nada, Yehia, Sara"))
        assertTrue(full.contains("3 × Large Mineral Water — Yehia ×2, Omar"))
    }

    @Test fun sameNameInTwoFamiliesIsLabelled() {
        val c = Compilation(name = "T", imports = listOf(
            ImportedOrder("a", "Nada", members = listOf("Ali"), lines = listOf(line("Ali", "Okra", 1, "Farm Dishes"))),
            ImportedOrder("b", "Sara", members = listOf("Ali"), lines = listOf(line("Ali", "Okra", 1, "Farm Dishes")))))
        assertEquals(listOf("Ali (Nada)" to 1, "Ali (Sara)" to 1), Kitchen.order(c, "Kom El Dikka", null).lines.single().who)
    }

    @Test fun arabicNamesTravelInBillFilesAndOldFilesStillImport() {
        val trip = BillExportTest().sampleTrip()
        val o = BillExport.fromTrip(trip)
        assertEquals("قهوة تركية", o.lines.first { it.item == "Turkish coffee" }.itemAr)   // Ibis has Arabic names
        assertEquals(o, BillExport.parse(BillExport.familyWorkbook(o)))
        // A v0.5 file has no Arabic column: still imports, with blank Arabic names.
        val v05 = o.copy(lines = o.lines.map { it.copy(itemAr = "") })
        val sheets = Xlsx.read(BillExport.familyWorkbook(v05))
        assertTrue(sheets["Data"]!!.filter { it.firstOrNull() == "line" }.all { it.size == 9 || it.getOrNull(9).isNullOrEmpty() })
        assertEquals(v05.lines, BillExport.parse(BillExport.familyWorkbook(v05)).lines)
    }

    @Test fun compiledWorkbookHasKitchenSheet() {
        val c = compilation()
        val rows = Xlsx.read(BillExport.compiledWorkbook(c))["Kitchen order"]!!
        assertEquals(listOf("Day", "Place", "Section", "Item", "Arabic", "Qty", "Who ordered"), rows[0])
        val kofta = rows.first { it.getOrNull(3) == "Grilled Kofta" }
        assertEquals("3", kofta[5]); assertEquals("Day 1 · Thu 22 Oct", kofta[0])
    }
}
