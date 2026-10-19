package com.triptab.app

import com.triptab.app.data.*
import org.junit.Assert.*
import org.junit.Test

class KomElDikkaMenuTest {

    /** The place as v0.1–v0.3 created it (first-edition menu, menuVersion 0). */
    private fun v1Place(): Place = Place(
        name = "Kom El Dikka", servicePct = 12.0, taxPct = 14.0,
        note = "Agri Lodge. Menu says 12% service and 14% taxes are added to the final bill.",
        items = KomElDikkaMenu.V1.keys.map { MenuItem(category = "Old", nameEn = it, nameAr = KomElDikkaMenu.V1[it]!!, price = 1.0) },
    )

    @Test fun newMenuShape() {
        val p = KomElDikkaMenu.place()
        assertEquals(122, p.items.size)
        assertEquals(KomElDikkaMenu.VERSION, p.menuVersion)
        assertEquals(17, p.items.map { it.category }.distinct().size)
        assertEquals(p.items.size, p.items.map { it.nameEn }.toSet().size)  // no duplicate names
        val m = p.items.associateBy { it.nameEn }
        assertEquals(435.0, m["Grilled Kofta"]!!.price, 0.0)
        assertEquals(685.0, m["Margherita Pizza"]!!.price, 0.0)
        assertEquals(60.0, m["Pepsi Diet"]!!.price, 0.0)
        assertEquals("كفته مشوية", m["Grilled Kofta"]!!.nameAr)   // Arabic kept for the same dish
        assertEquals("", m["Fillet with Rice"]!!.nameAr)            // dish changed → old Arabic dropped
        // Each old dish is replaced at most once.
        val was = KomElDikkaMenu.entries.mapNotNull { it.was }
        assertEquals(was.size, was.toSet().size)
    }

    @Test fun upgradeKeepsOrdersAndIds() {
        val old = v1Place()
        val kofta = old.items.first { it.nameEn == "Grilled kofta" }
        val freekeh = old.items.first { it.nameEn == "Freekeh tagine with meat" }   // dropped from the new menu
        val mine = MenuItem(category = "Extras", nameEn = "Birthday cake", price = 500.0)  // added by the user
        val member = Member(name = "N")
        val ibis = FayoumSeed.ibis
        val trip = Trip(name = "Fayoum", ownerName = "N", members = listOf(member),
            places = listOf(old.copy(items = old.items + mine), ibis),
            orders = listOf(
                OrderLine(memberId = member.id, placeId = old.id, itemId = kofta.id, quantity = 2, day = 1),
                OrderLine(memberId = member.id, placeId = old.id, itemId = freekeh.id, quantity = 1, day = 2),
            ))

        val up = KomElDikkaMenu.upgrade(trip)
        val p = up.places.first { it.name == "Kom El Dikka" }
        assertEquals(KomElDikkaMenu.VERSION, p.menuVersion)
        assertTrue(p.note.contains("October 2026"))

        val k = p.items.first { it.id == kofta.id }                 // same item, new name and price
        assertEquals("Grilled Kofta", k.nameEn); assertEquals(435.0, k.price, 0.0); assertEquals("Main Course", k.category)
        val f = p.items.first { it.id == freekeh.id }               // ordered → kept
        assertEquals("No longer on the menu", f.category)
        assertNotNull(p.items.find { it.id == mine.id })            // user's own item kept
        assertNull(p.items.find { it.nameEn == "Marshmallow" })     // removed and never ordered → gone
        assertEquals(122 + 2, p.items.size)

        assertEquals(trip.orders, up.orders)                        // orders untouched
        assertEquals(870.0 + 1.0 * 1.0, Billing.subtotal(up, p, member.id), 0.0)  // 2×435 + old freekeh price
        assertSame(ibis, up.places.first { it.name == "Ibis Restaurant" })       // other places untouched

        assertSame(up, KomElDikkaMenu.upgrade(up))                  // running again changes nothing
    }

    @Test fun upgradeOnLoadIsSaved() {
        val dir = java.nio.file.Files.createTempDirectory("tt").toFile()
        val f = java.io.File(dir, "d.json")
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val trip = Trip(name = "Fayoum", ownerName = "N", members = listOf(Member(name = "N")), places = listOf(v1Place()))
        f.writeText(json.encodeToString(AppData.serializer(), AppData(listOf(trip))).replace(",\"menuVersion\":0", ""))
        assertFalse(f.readText().contains("menuVersion"))           // looks like a file saved by v0.3

        val repo = LocalJsonRepository(f)
        assertEquals(122, repo.data.value.trips[0].places[0].items.size)
        assertTrue(f.readText().contains("\"menuVersion\":${KomElDikkaMenu.VERSION}"))   // written back
    }
}
