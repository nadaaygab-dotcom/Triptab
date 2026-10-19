package com.triptab.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/**
 * All IDs are UUID strings so that, when shared live sync is added later,
 * records created on different phones can be merged without collisions.
 */
@Serializable
data class AppData(
    val trips: List<Trip> = emptyList(),
    val schemaVersion: Int = 1,
)

@Serializable
data class Trip(
    val id: String = newId(),
    val name: String,
    val startDate: String = "",
    val endDate: String = "",
    /** The phone owner's own name. The owner is also the first entry in [members]. */
    val ownerName: String,
    val members: List<Member> = emptyList(),
    val places: List<Place> = emptyList(),
    val orders: List<OrderLine> = emptyList(),
)

@Serializable
data class Member(
    val id: String = newId(),
    val name: String,
)

@Serializable
enum class PlaceType { RESTAURANT, ACTIVITY }

@Serializable
data class Place(
    val id: String = newId(),
    val name: String,
    val nameAr: String = "",
    val type: PlaceType = PlaceType.RESTAURANT,
    /** Service charge, as a percentage (12.0 = 12%). */
    val servicePct: Double = 0.0,
    /** Tax, as a percentage (14.0 = 14%). */
    val taxPct: Double = 0.0,
    /** If true, tax is charged on (subtotal + service) — the usual practice on Egyptian bills. */
    val taxOnService: Boolean = true,
    val note: String = "",
    val items: List<MenuItem> = emptyList(),
    /** Which edition of a built-in menu this is (0 = added by hand / first edition). */
    val menuVersion: Int = 0,
)

@Serializable
data class MenuItem(
    val id: String = newId(),
    val category: String,
    val nameEn: String,
    val nameAr: String = "",
    val price: Double,
)

@Serializable
data class OrderLine(
    val id: String = newId(),
    val memberId: String,
    val placeId: String,
    val itemId: String,
    val quantity: Int,
    /** Trip day, 1-based (Day 1 = start date). Orders saved before days existed default to Day 1. */
    val day: Int = 1,
)

/** A bill for a set of order lines at one place. */
data class Bill(
    val subtotal: Double,
    val service: Double,
    val tax: Double,
) {
    val total: Double get() = subtotal + service + tax
    operator fun plus(o: Bill) = Bill(subtotal + o.subtotal, service + o.service, tax + o.tax)

    companion object {
        val ZERO = Bill(0.0, 0.0, 0.0)
    }
}

object Billing {
    fun bill(place: Place, subtotal: Double): Bill {
        val service = subtotal * place.servicePct / 100.0
        val taxBase = if (place.taxOnService) subtotal + service else subtotal
        val tax = taxBase * place.taxPct / 100.0
        return Bill(subtotal, service, tax)
    }

    fun subtotal(trip: Trip, place: Place, memberId: String?, day: Int? = null): Double {
        val prices = place.items.associate { it.id to it.price }
        return trip.orders
            .filter { it.placeId == place.id && (memberId == null || it.memberId == memberId) &&
                (day == null || it.day == day) }
            .sumOf { (prices[it.itemId] ?: 0.0) * it.quantity }
    }

    /**
     * Bill for one member (or the whole family when memberId == null) at one place,
     * on one day (or summed over all days when day == null).
     */
    fun billAt(trip: Trip, place: Place, memberId: String?, day: Int? = null): Bill =
        bill(place, subtotal(trip, place, memberId, day))

    /** Bill for one member (or the whole family) across all places, on one day or all days. */
    fun billTotal(trip: Trip, memberId: String?, day: Int? = null): Bill =
        trip.places.fold(Bill.ZERO) { acc, p -> acc + billAt(trip, p, memberId, day) }
}

fun formatEgp(v: Double): String {
    val rounded = Math.round(v * 100) / 100.0
    return if (rounded % 1.0 == 0.0) "EGP %,d".format(rounded.toLong())
    else "EGP %,.2f".format(rounded)
}
