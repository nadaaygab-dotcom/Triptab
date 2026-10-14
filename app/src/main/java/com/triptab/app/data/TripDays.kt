package com.triptab.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Turns the trip's free-text dates into a list of numbered days. */
object TripDays {
    private val formats = listOf("d MMM yyyy", "d MMMM yyyy", "yyyy-MM-dd", "d/M/yyyy", "d-M-yyyy", "MMM d yyyy", "MMM d, yyyy")
        .map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }

    fun parse(text: String): LocalDate? {
        val t = text.trim().replace(Regex("\\s+"), " ")
        if (t.isEmpty()) return null
        for (f in formats) {
            try { return LocalDate.parse(t, f) } catch (_: Exception) {}
        }
        return null
    }

    /** Number of days in the trip: from the dates when readable, and never fewer than any day already used. */
    fun count(trip: Trip): Int {
        val s = parse(trip.startDate)
        val e = parse(trip.endDate)
        val fromDates = if (s != null && e != null && !e.isBefore(s))
            (ChronoUnit.DAYS.between(s, e).toInt() + 1).coerceAtMost(60) else 1
        val used = trip.orders.maxOfOrNull { it.day } ?: 1
        return maxOf(fromDates, used)
    }

    fun days(trip: Trip): List<Int> = (1..count(trip)).toList()

    fun date(trip: Trip, day: Int): LocalDate? = parse(trip.startDate)?.plusDays((day - 1).toLong())

    /** "Day 1 · Thu 22 Oct", or just "Day 1" when the start date can't be read. */
    fun label(trip: Trip, day: Int): String {
        val d = date(trip, day) ?: return "Day $day"
        return "Day $day · " + d.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))
    }

    fun shortLabel(trip: Trip, day: Int): String {
        val d = date(trip, day) ?: return "Day $day"
        return d.format(DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH))
    }

    /** Today if it falls inside the trip, otherwise Day 1. */
    fun defaultDay(trip: Trip, today: LocalDate = LocalDate.now()): Int {
        val s = parse(trip.startDate) ?: return 1
        val i = ChronoUnit.DAYS.between(s, today).toInt() + 1
        return if (i in 1..count(trip)) i else 1
    }
}
