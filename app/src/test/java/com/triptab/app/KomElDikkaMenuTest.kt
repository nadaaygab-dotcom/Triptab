package com.triptab.app

import com.triptab.app.data.*
import org.junit.Assert.*
import org.junit.Test

class KomElDikkaMenuTest {

    /** The place as the October-menu version (v0.4–v0.8) left it, with item ids carried from the first menu. */
    private fun octoberPlace(): Place = Place(
        name = "Kom El Dikka", servicePct = 12.0, taxPct = 14.0, menuVersion = 2,
        note = "Agri Lodge — October 2026 menu. A 12% service charge and 14% taxes are added to the final bill.",
        items = KomElDikkaMenu.entries.map { MenuItem(category = it.category, nameEn = it.nameEn, nameAr = it.nameAr, price = it.price) },
    )

    @Test fun firstMenuWithOriginalPrices() {
        val p = KomElDikkaMenu.place()
        assertEquals(120, p.items.size)
        assertEquals(KomElDikkaMenu.VERSION, p.menuVersion)
        assertEquals(18, p.items.map { it.category }.distinct().size)
        val m = p.items.associateBy { it.nameEn }
        assertEquals(360.0, m["Grilled kofta"]!!.price, 0.0)
        assertEquals(200.0, m["Margherita pizza"]!!.price, 0.0)
        assertEquals(38.0, m["Mineral water large"]!!.price, 0.0)
        assertEquals(510.0, m["Freekeh tagine with meat"]!!.price, 0.0)
        assertEquals("كفته مشوية", m["Grilled kofta"]!!.nameAr)
        assertEquals(listOf("Salads", "Appetizers", "Soups"), p.items.map { it.category }.distinct().take(3))
        assertNull(m["Lotus Pie"])
    }

    @Test fun phonesWithTheOctoberMenuGoBackAndKeepOrders() {
        val old = octoberPlace()
        val kofta = old.items.first { it.nameEn == "Grilled Kofta" }
        val lotus = old.items.first { it.nameEn == "Lotus Pie" }        // October-only, ordered → kept
        val tuna = old.items.first { it.nameEn == "Tuna Pizza" }        // October-only, not ordered → removed
        val mine = MenuItem(category = "Extras", nameEn = "Birthday cake", price = 500.0)
        val member = Member(name = "N")
        val trip = Trip(name = "Fayoum", ownerName = "N", members = listOf(member),
            places = listOf(old.copy(items = old.items + mine)),
            orders = listOf(
                OrderLine(memberId = member.id, placeId = old.id, itemId = kofta.id, quantity = 2),
                OrderLine(memberId = member.id, placeId = old.id, itemId = lotus.id, quantity = 1),
            ))
        val up = KomElDikkaMenu.upgrade(trip)
        val p = up.places.single()
        assertEquals(KomElDikkaMenu.VERSION, p.menuVersion)
        val k = p.items.first { it.id == kofta.id }
        assertEquals("Grilled kofta", k.nameEn); assertEquals(360.0, k.price, 0.0); assertEquals("Main dishes", k.category)
        assertEquals("No longer on the menu", p.items.first { it.id == lotus.id }.category)
        assertNull(p.items.find { it.id == tuna.id })
        assertNotNull(p.items.find { it.id == mine.id })
        assertNotNull(p.items.find { it.nameEn == "Marshmallow" })   // back on the menu
        assertEquals(120 + 2, p.items.size)
        assertEquals(trip.orders, up.orders)
        assertEquals(2 * 360.0 + 423.0, Billing.subtotal(up, p, member.id), 0.0)
        assertTrue(p.note.startsWith("Agri Lodge. Menu says"))
        assertSame(up, KomElDikkaMenu.upgrade(up))
    }

    @Test fun phonesFromBeforeOctoberGetTheSameMenu() {
        val v1 = Place(name = "Kom El Dikka", items = KomElDikkaMenu.V1_MENU.map {
            MenuItem(category = it.category, nameEn = it.nameEn, nameAr = it.nameAr, price = it.oldPrice.toDouble()) })
        val p = KomElDikkaMenu.upgrade(Trip(name = "F", ownerName = "N", places = listOf(v1))).places.single()
        assertEquals(v1.items.map { it.id }, p.items.map { it.id })
        assertEquals(KomElDikkaMenu.current.map { it.price }, p.items.map { it.price })
    }

    @Test fun receiptPricesOnTheMenu() {
        val m = KomElDikkaMenu.place().items.associateBy { it.nameEn }
        assertEquals(180.0, m["Pasta tomato sauce"]!!.price, 0.0)
        assertEquals(335.0, m["Pasta bolognese"]!!.price, 0.0)
        assertEquals(340.0, m["Chicken strips"]!!.price, 0.0)
        assertEquals(165.0, m["Chicken soup"]!!.price, 0.0)
        assertEquals(75.0, m["Sautéed vegetables"]!!.price, 0.0)
        assertEquals(50.0, m["Pepsi / 7UP / Mirinda"]!!.price, 0.0)
        assertEquals(360.0, m["Grilled kofta"]!!.price, 0.0)       // mains unchanged
        assertEquals(26.0, m["Mineral water small"]!!.price, 0.0)
    }

    @Test fun phonesOnVersion3OnlyGetTheReceiptPrices() {
        val v3 = KomElDikkaMenu.place().let { p ->
            p.copy(menuVersion = 3, items = p.items.map { i ->
                val old = KomElDikkaMenu.V1_MENU.first { it.nameEn == i.nameEn }.oldPrice.toDouble()
                if (i.nameEn == "Grilled kofta") i.copy(price = 999.0) else i.copy(price = old)
            } + MenuItem(category = "No longer on the menu", nameEn = "Penne Alfredo", price = 280.0))
        }
        val member = Member(name = "Y")
        val strips = v3.items.first { it.nameEn == "Chicken strips" }
        val t = Trip(name = "F", ownerName = "N", members = listOf(member), places = listOf(v3),
            orders = listOf(OrderLine(memberId = member.id, placeId = v3.id, itemId = strips.id, quantity = 2)))
        val up = KomElDikkaMenu.upgrade(t)
        val p = up.places.single()
        assertEquals(KomElDikkaMenu.VERSION, p.menuVersion)
        assertEquals(v3.items.map { it.id }, p.items.map { it.id })
        val m = p.items.associateBy { it.nameEn }
        assertEquals(999.0, m["Grilled kofta"]!!.price, 0.0)        // the user's own edit stays
        assertEquals(335.0, m["Penne Alfredo"]!!.price, 0.0)
        assertEquals(680.0, Billing.subtotal(up, p, member.id), 0.0)
        assertSame(up, KomElDikkaMenu.upgrade(up))
    }
}
