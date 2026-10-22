package com.triptab.app

import com.triptab.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class BillExportTest {

    /** A Fayoum trip with orders over two days at two places. */
    fun sampleTrip(owner: String = "Nada", kids: List<String> = listOf("Yehia", "Alya")): Trip {
        val repo = LocalJsonRepository(File(Files.createTempDirectory("tt").toFile(), "d.json"))
        val id = repo.createTrip("Fayoum", "22 Oct 2026", "24 Oct 2026", owner, kids, true)
        val t = repo.data.value.trips.first()
        val kd = t.places.first { it.name == "Kom El Dikka" }; val ibis = t.places.first { it.name == "Ibis Restaurant" }
        fun item(p: Place, n: String) = p.items.first { it.nameEn == n }.id
        val m = t.members
        repo.setQuantity(id, m[0].id, kd.id, item(kd, "Grilled kofta"), 1, 1)            // 435
        repo.setQuantity(id, m[0].id, kd.id, item(kd, "Mineral water large"), 1, 2)      // 2 × 45
        repo.setQuantity(id, m[0].id, ibis.id, item(ibis, "Turkish coffee"), 2, 1)       // 65
        if (m.size > 1) repo.setQuantity(id, m[1].id, kd.id, item(kd, "Margherita pizza"), 1, 1) // 685
        if (m.size > 2) repo.setQuantity(id, m[2].id, ibis.id, item(ibis, "Mango juice"), 3, 2)  // 2 × 100
        return repo.data.value.trips.first()
    }

    @Test fun roundTripKeepsEveryOrderAndBill() {
        val trip = sampleTrip()
        val o = BillExport.fromTrip(trip, exportedAt = "2026-10-24 22:00").single()
        val back = BillExport.parse(BillExport.familyWorkbook(o))
        assertEquals(o, back)
        // Bills match the app's own maths for each person and the family.
        for (m in trip.members) assertEquals(Billing.billTotal(trip, m.id).total, back.bill(m.name).total, 1e-9)
        assertEquals(Billing.billTotal(trip, null).total, back.bill().total, 1e-9)
        assertEquals(556.6848, back.bill("Nada", dayKey = "2026-10-22").total, 1e-9)   // 436 + 12% + 14%
        assertEquals("Day 2 · Fri 23 Oct", BillExport.dayLabel(2, "2026-10-23"))
    }

    @Test fun readableSheetsLookRight() {
        val sheets = Xlsx.read(BillExport.familyWorkbook(BillExport.fromTrip(sampleTrip()).single()))
        assertEquals(listOf("Bill", "Orders", "Data"), sheets.keys.toList())
        val bill = sheets["Bill"]!!
        assertEquals("TripTab bill — Nada's family", bill[0][0])
        val familyTotal = bill.first { it.firstOrNull() == "Family total" }
        assertEquals(BillExport.fromTrip(sampleTrip()).single().bill().total, familyTotal[6].toDouble(), 0.01)
        val orders = sheets["Orders"]!!
        assertEquals(listOf("Person", "Day", "Place", "Section", "Item", "Qty", "Price", "Line total"), orders[0])
        assertTrue(orders.any { it.getOrNull(4) == "Grilled kofta" && it[1] == "Day 1 · Thu 22 Oct" })
    }

    @Test fun sameNamesStayApart() {
        val o = BillExport.fromTrip(sampleTrip(owner = "Ali", kids = listOf("Ali"))).single()
        assertEquals(listOf("Ali", "Ali (2)"), o.members)
    }

    @Test fun rejectsOtherFiles() {
        val plain = Xlsx.write(listOf(Xlsx.Sheet("Sheet1").apply { row("hello", 1) }))
        val e = assertThrows(IOException::class.java) { BillExport.parse(plain) }
        assertTrue(e.message!!.contains("isn't a TripTab bill"))
        assertThrows(IOException::class.java) { BillExport.parse("not a zip".toByteArray()) }
    }

    @Test fun readsFileResavedByExcel() {
        // Produced from our export by a different spreadsheet library (shared strings, styles, its own layout).
        val bytes = javaClass.getResourceAsStream("/resaved-bill.xlsx")?.readBytes() ?: return
        val o = BillExport.parse(bytes)
        assertEquals("Nada", o.family)
        assertEquals(listOf("Nada", "Yehia", "Alya"), o.members)
        assertEquals(5, o.lines.size)
        assertEquals(1841.73, o.bill().total, 0.01)   // the total in that saved file
    }

    @Test fun compiledTotalsAndReimportReplaces() {
        val repo = LocalJsonRepository(File(Files.createTempDirectory("tt").toFile(), "d.json"))
        val c = repo.createCompilation("Fayoum")
        val nada = BillExport.fromTrip(sampleTrip("Nada")).single()
        val sara = BillExport.fromTrip(sampleTrip("Sara", listOf("Omar"))).single()
        repo.addImports(c, listOf(nada, sara))
        var comp = repo.data.value.compilations.single()
        assertEquals(2, comp.imports.size)
        assertEquals(nada.bill().total + sara.bill().total, comp.bill().total, 1e-9)
        assertEquals(listOf("2026-10-22", "2026-10-23", "2026-10-24"), comp.days().map { it.first })

        // Sara sends an updated file: it replaces hers, not adds a third family.
        val saraLater = sara.copy(lines = sara.lines.take(1), exportedAt = "later")
        repo.addImports(c, listOf(saraLater))
        comp = repo.data.value.compilations.single()
        assertEquals(2, comp.imports.size)
        assertEquals("later", comp.imports.first { it.family == "Sara" }.exportedAt)

        val wb = Xlsx.read(BillExport.compiledWorkbook(comp))
        assertEquals(listOf("Summary", "By day", "Kitchen order", "Orders"), wb.keys.toList())
        assertEquals(comp.bill().total, wb["Summary"]!!.first { it.firstOrNull() == "Everyone" }[5].toDouble(), 0.01)

        repo.removeImport(c, sara.sourceId)
        assertEquals(1, repo.data.value.compilations.single().imports.size)
    }

    /** Dev aid: writes a sample export for external checks (set EXPORT_SAMPLE_DIR). */
    @Test fun writeSample() {
        val dir = System.getenv("EXPORT_SAMPLE_DIR")?.let(::File) ?: return
        File(dir, "sample-bill.xlsx").writeBytes(BillExport.familyWorkbook(BillExport.fromTrip(sampleTrip(), "2026-10-24 22:00").single()))
        val repo = LocalJsonRepository(File(Files.createTempDirectory("tt").toFile(), "d.json"))
        val c = repo.createCompilation("Fayoum")
        repo.addImports(c, listOf(BillExport.fromTrip(sampleTrip("Nada")).single(), BillExport.fromTrip(sampleTrip("Sara", listOf("Omar"))).single()))
        File(dir, "sample-compiled.xlsx").writeBytes(BillExport.compiledWorkbook(repo.data.value.compilations.single()))
    }
}
