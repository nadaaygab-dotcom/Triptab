package com.triptab.app

import com.triptab.app.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingTest {
    private val dad = Member(name = "Dad")
    private val kid = Member(name = "Kid")

    private fun tripWith(place: Place, vararg orders: OrderLine) =
        Trip(name = "T", ownerName = "Dad", members = listOf(dad, kid), places = listOf(place), orders = orders.toList())

    @Test fun komElDikkaServiceThenTax() {
        val p = FayoumSeed.places().first { it.name == "Kom El Dikka" }
        val kofta = p.items.first { it.nameEn == "Grilled kofta" }      // 360
        val water = p.items.first { it.nameEn == "Mineral water large" } // 38
        val t = tripWith(p,
            OrderLine(memberId = dad.id, placeId = p.id, itemId = kofta.id, quantity = 1),
            OrderLine(memberId = dad.id, placeId = p.id, itemId = water.id, quantity = 2))
        val b = Billing.billAt(t, p, dad.id)
        assertEquals(436.0, b.subtotal, 0.001)        // 360 + 2×38
        assertEquals(52.32, b.service, 0.001)         // 12%
        assertEquals(68.3648, b.tax, 0.001)           // 14% of 488.32
        assertEquals(556.6848, b.total, 0.001)
        assertEquals(0.0, Billing.billAt(t, p, kid.id).total, 0.001)
    }

    @Test fun ibisServiceOnly() {
        val p = FayoumSeed.places().first { it.name == "Ibis Restaurant" }
        val pizza = p.items.first { it.nameEn == "Margherita" }  // 210
        val t = tripWith(p, OrderLine(memberId = kid.id, placeId = p.id, itemId = pizza.id, quantity = 2))
        assertEquals(470.4, Billing.billTotal(t, null).total, 0.001) // 420 × 1.12
    }

    @Test fun daysSplitAndSum() {
        val p = FayoumSeed.places().first { it.name == "Ibis Restaurant" }
        val pizza = p.items.first { it.nameEn == "Margherita" }  // 210
        val t = tripWith(p,
            OrderLine(memberId = dad.id, placeId = p.id, itemId = pizza.id, quantity = 1, day = 1),
            OrderLine(memberId = dad.id, placeId = p.id, itemId = pizza.id, quantity = 2, day = 3))
        assertEquals(235.2, Billing.billTotal(t, dad.id, 1).total, 0.001)
        assertEquals(0.0, Billing.billTotal(t, dad.id, 2).total, 0.001)
        assertEquals(470.4, Billing.billTotal(t, dad.id, 3).total, 0.001)
        assertEquals(705.6, Billing.billTotal(t, dad.id).total, 0.001)  // all days
    }

    @Test fun tripDays() {
        val t = Trip(name = "F", ownerName = "N", startDate = "22 Oct 2026", endDate = "24 Oct 2026")
        assertEquals(listOf(1, 2, 3), TripDays.days(t))
        assertEquals("Day 1 · Thu 22 Oct", TripDays.label(t, 1))
        assertEquals("Day 3 · Sat 24 Oct", TripDays.label(t, 3))
        assertEquals(2, TripDays.defaultDay(t, java.time.LocalDate.of(2026, 10, 23)))
        assertEquals(1, TripDays.defaultDay(t, java.time.LocalDate.of(2026, 10, 14)))
        // Unreadable dates fall back to plain day numbers.
        assertEquals("Day 1", TripDays.label(t.copy(startDate = "next week"), 1))
    }

    @Test fun oldSavedOrdersDefaultToDay1() {
        val old = """{"trips":[{"id":"t","name":"F","ownerName":"N","orders":[{"id":"o","memberId":"m","placeId":"p","itemId":"i","quantity":2}]}]}"""
        val d = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(AppData.serializer(), old)
        assertEquals(1, d.trips[0].orders[0].day)
    }

    @Test fun seedSanity() {
        val places = FayoumSeed.places()
        assertEquals(4, places.size)
        assertTrue(places.first { it.name == "Kom El Dikka" }.items.size > 100)
        assertTrue(places.first { it.name == "Ibis Restaurant" }.items.size > 50)
        // Every seed item gets a unique id.
        val ids = places.flatMap { it.items }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun formatting() {
        assertEquals("EGP 1,250", formatEgp(1250.0))
        assertEquals("EGP 556.68", formatEgp(556.6848))
    }
}
