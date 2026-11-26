package com.triptab.app

import com.triptab.app.data.*
import org.junit.Assert.*
import org.junit.Test

class BloomsAndBeansMenuTest {
    private val p = BloomsAndBeansMenu.place()
    private fun price(name: String) = p.items.single { it.nameEn == name }.price

    @Test fun menuShape() {
        assertEquals(137, p.items.size)
        assertEquals(17, p.items.map { it.category }.distinct().size)
        assertEquals(p.items.size, p.items.map { it.nameEn }.distinct().size)
        assertTrue(p.items.all { it.price > 0 })
        assertEquals(12.0, p.servicePct, 0.0); assertEquals(14.0, p.taxPct, 0.0)
    }

    @Test fun spotPrices() {
        assertEquals(480.0, price("Croissant egg royal"), 0.0)
        assertEquals(474.0, price("Pasta al tartufo"), 0.0)
        assertEquals(820.0, price("Lemon butter salmon fillet"), 0.0)
        assertEquals(130.0, price("Latte (large)"), 0.0)
        assertEquals(25.0, price("Water"), 0.0)
        assertEquals(40.0, price("Flavour (vanilla / caramel / hazelnut / cinnamon / Irish)"), 0.0)
    }

    @Test fun billAddsServiceThenTax() {
        // 1000 + 12% = 1120, + 14% of 1120 = 156.80 → 1276.80
        assertEquals(1276.8, Billing.bill(p, 1000.0).total, 1e-9)
    }

    @Test fun savedCopyGetsFreshIds() {
        val make = SavedMenus.all.first { it.first == BloomsAndBeansMenu.NAME }.second
        val a = SavedMenus.copy(make, "x"); val b = SavedMenus.copy(make, "y")
        assertEquals("x", a.id)
        assertTrue(a.items.map { it.id }.intersect(b.items.map { it.id }.toSet()).isEmpty())
    }
}
