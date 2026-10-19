package com.triptab.app.data

import kotlinx.serialization.Serializable
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------------------------------------------------------------------------
// One family's orders as exported to Excel and imported by the admin.
// ---------------------------------------------------------------------------------------------

@Serializable
data class ImportedPlace(
    val name: String,
    val servicePct: Double = 0.0,
    val taxPct: Double = 0.0,
    val taxOnService: Boolean = true,
)

@Serializable
data class ImportedLine(
    val member: String,
    val day: Int,
    /** ISO date (2026-10-22) when the trip dates were readable, else "". */
    val date: String = "",
    val place: String,
    val category: String = "",
    val item: String,
    val quantity: Int,
    val unitPrice: Double,
    /** Arabic dish name, when the menu has one (files from v0.5 don't carry it). */
    val itemAr: String = "",
) {
    val lineTotal get() = unitPrice * quantity
    val dayKey get() = date.ifBlank { "day:$day" }
}

@Serializable
data class ImportedOrder(
    /** Stable id of the exporting phone's trip — a newer file from the same family replaces the older one. */
    val sourceId: String,
    /** Who sent it (the trip owner on that phone). */
    val family: String,
    val tripName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val exportedAt: String = "",
    val members: List<String> = emptyList(),
    val places: List<ImportedPlace> = emptyList(),
    val lines: List<ImportedLine> = emptyList(),
    val importedAt: String = "",
) {
    val familyLabel get() = if (family.isBlank()) "Family" else "$family's family"

    private fun charges(place: String) = places.firstOrNull { it.name == place } ?: ImportedPlace(place)

    /** Bill for one member (or everyone in this family when null), optionally one day. Same maths as the app. */
    fun bill(member: String? = null, dayKey: String? = null, place: String? = null): Bill =
        lines.filter { (member == null || it.member == member) && (dayKey == null || it.dayKey == dayKey) && (place == null || it.place == place) }
            .groupBy { it.place }
            .entries.fold(Bill.ZERO) { acc, (p, ls) ->
                val c = charges(p)
                acc + Billing.bill(Place(name = p, servicePct = c.servicePct, taxPct = c.taxPct, taxOnService = c.taxOnService),
                    ls.sumOf { it.lineTotal })
            }

    /** Members in order, including anyone who appears only in the lines. */
    val allMembers get() = (members + lines.map { it.member }).distinct()
}

@Serializable
data class Compilation(
    val id: String = newId(),
    val name: String,
    val createdAt: String = "",
    val imports: List<ImportedOrder> = emptyList(),
) {
    fun bill(dayKey: String? = null): Bill = imports.fold(Bill.ZERO) { acc, o -> acc + o.bill(dayKey = dayKey) }

    /** Days found in the imported lines, in order, with labels. */
    fun days(): List<Pair<String, String>> = imports.flatMap { it.lines }
        .distinctBy { it.dayKey }
        .sortedWith(compareBy({ it.date.ifBlank { "9999" } }, { it.day }))
        .map { it.dayKey to BillExport.dayLabel(it.day, it.date) }

    fun places(): List<String> = imports.flatMap { o -> o.lines.map { it.place } }.distinct()
}

// ---------------------------------------------------------------------------------------------
// Excel files.
// ---------------------------------------------------------------------------------------------

object BillExport {
    const val FORMAT = 1
    const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)

    fun now(): String = LocalDateTime.now().format(STAMP)

    fun dayLabel(day: Int, isoDate: String): String {
        val d = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return "Day $day"
        return "Day $day · " + d.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))
    }

    /** A trip on this phone, as one family's order. */
    fun fromTrip(trip: Trip, exportedAt: String = now()): ImportedOrder {
        // Two people with the same name would merge — keep them apart.
        val seen = HashMap<String, Int>()
        val names = trip.members.associate { m ->
            val n = m.name.trim().ifEmpty { "Guest" }
            val k = seen.merge(n.lowercase(), 1, Int::plus)!!
            m.id to if (k == 1) n else "$n ($k)"
        }
        val lines = trip.orders.mapNotNull { o ->
            val place = trip.places.find { it.id == o.placeId } ?: return@mapNotNull null
            val item = place.items.find { it.id == o.itemId } ?: return@mapNotNull null
            ImportedLine(
                member = names[o.memberId] ?: return@mapNotNull null,
                day = o.day, date = TripDays.date(trip, o.day)?.toString().orEmpty(),
                place = place.name, category = item.category, item = item.nameEn,
                quantity = o.quantity, unitPrice = item.price, itemAr = item.nameAr,
            )
        }.sortedWith(compareBy({ trip.members.indexOfFirst { m -> names[m.id] == it.member } }, { it.day },
            { p -> trip.places.indexOfFirst { it.name == p.place } }))
        return ImportedOrder(
            sourceId = trip.id, family = trip.ownerName, tripName = trip.name,
            startDate = trip.startDate, endDate = trip.endDate, exportedAt = exportedAt,
            members = trip.members.map { names[it.id]!! },
            places = trip.places.filter { p -> lines.any { it.place == p.name } }
                .map { ImportedPlace(it.name, it.servicePct, it.taxPct, it.taxOnService) },
            lines = lines,
        )
    }

    private fun billRow(label: String, day: String, place: String, b: Bill, bold: Boolean): Array<Any?> = arrayOf(
        Xlsx.Text(label, bold), Xlsx.Text(day, bold), Xlsx.Text(place, bold),
        Xlsx.Num(b.subtotal, bold), Xlsx.Num(b.service, bold), Xlsx.Num(b.tax, bold), Xlsx.Num(b.total, bold),
    )

    private fun header(vararg h: String) = h.map { Xlsx.Text(it, bold = true) }.toTypedArray<Any?>()

    /** One family's bill file: readable "Bill" and "Orders" sheets, plus a hidden "Data" sheet for importing. */
    fun familyWorkbook(o: ImportedOrder): ByteArray {
        val bill = Xlsx.Sheet("Bill", widths = listOf(24, 22, 22, 14, 12, 12, 14))
        bill.row(Xlsx.Text("TripTab bill — ${o.familyLabel}", bold = true))
        bill.row("Trip", listOf(o.tripName, listOf(o.startDate, o.endDate).filter { it.isNotBlank() }.joinToString(" – ")).filter { it.isNotBlank() }.joinToString(", "))
        bill.row("Exported", o.exportedAt)
        bill.blank()
        bill.row(*header("Person", "Day", "Place", "Menu prices", "Service", "Tax", "Total"))
        for (m in o.allMembers) {
            val mine = o.lines.filter { it.member == m }
            for ((dk, ls) in mine.groupBy { it.dayKey }) for (p in ls.map { it.place }.distinct()) {
                bill.row(*billRow(m, dayLabel(ls[0].day, ls[0].date), p, o.bill(m, dk, p), false))
            }
            bill.row(*billRow("$m — total", "", "", o.bill(m), true))
            bill.blank()
        }
        bill.row(*billRow("Family total", "", "", o.bill(), true))
        bill.blank()
        o.places.filter { it.servicePct > 0 || it.taxPct > 0 }.forEach {
            val parts = listOfNotNull(
                it.servicePct.takeIf { v -> v > 0 }?.let { v -> "service ${fmtPct(v)}" },
                it.taxPct.takeIf { v -> v > 0 }?.let { v -> "tax ${fmtPct(v)}" })
            val note = if (it.taxOnService && it.taxPct > 0 && it.servicePct > 0) " (tax on menu prices + service)" else ""
            bill.row("${it.name}: ${parts.joinToString(", ")}$note")
        }

        val orders = Xlsx.Sheet("Orders", widths = listOf(20, 22, 20, 18, 34, 6, 12, 14))
        orders.row(*header("Person", "Day", "Place", "Section", "Item", "Qty", "Price", "Line total"))
        for (l in o.lines) orders.row(l.member, dayLabel(l.day, l.date), l.place, l.category, l.item, l.quantity, l.unitPrice, l.lineTotal)

        return Xlsx.write(listOf(bill, orders, dataSheet(o)))
    }

    private fun fmtPct(v: Double) = if (v % 1.0 == 0.0) "${v.toInt()}%" else "$v%"

    private fun dataSheet(o: ImportedOrder) = Xlsx.Sheet("Data", hidden = true).apply {
        row("TripTab", "bill-export", FORMAT)
        row("source", o.sourceId); row("family", o.family); row("trip", o.tripName)
        row("start", o.startDate); row("end", o.endDate); row("exported", o.exportedAt)
        o.members.forEach { row("member", it) }
        o.places.forEach { row("place", it.name, it.servicePct, it.taxPct, if (it.taxOnService) 1 else 0) }
        o.lines.forEach { row("line", it.member, it.day, it.date, it.place, it.category, it.item, it.quantity, it.unitPrice, it.itemAr) }
    }

    /** Reads a family's bill file. Throws IOException with a user-facing message if it isn't one. */
    fun parse(bytes: ByteArray): ImportedOrder {
        val sheets = Xlsx.read(bytes)
        val data = sheets.entries.firstOrNull { it.key.equals("Data", ignoreCase = true) }?.value
        if (data == null || data.firstOrNull()?.firstOrNull() != "TripTab")
            throw IOException("This isn't a TripTab bill. Ask them to use Summary → Export Excel in TripTab.")
        val fmt = data[0].getOrNull(2)?.toDoubleOrNull()?.toInt() ?: 0
        if (fmt > FORMAT) throw IOException("This bill was made by a newer TripTab. Update the app to import it.")

        fun num(s: String?) = s?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        val meta = HashMap<String, String>()
        val members = ArrayList<String>(); val places = ArrayList<ImportedPlace>(); val lines = ArrayList<ImportedLine>()
        for (r in data.drop(1)) {
            fun c(i: Int) = r.getOrNull(i).orEmpty()
            when (c(0)) {
                "member" -> members += c(1)
                "place" -> places += ImportedPlace(c(1), num(c(2)), num(c(3)), c(4).trim() != "0")
                "line" -> {
                    val q = num(c(7)).toInt()
                    if (q > 0) lines += ImportedLine(c(1), num(c(2)).toInt().coerceAtLeast(1), c(3), c(4), c(5), c(6), q, num(c(8)), c(9))
                }
                "" -> {}
                else -> meta[c(0)] = c(1)
            }
        }
        val source = meta["source"].orEmpty()
        if (source.isBlank()) throw IOException("This TripTab bill is missing its family id and can't be imported.")
        return ImportedOrder(source, meta["family"].orEmpty(), meta["trip"].orEmpty(), meta["start"].orEmpty(),
            meta["end"].orEmpty(), meta["exported"].orEmpty(), members, places, lines)
    }

    /** The admin's compiled workbook: summary per family and person, totals per day and place, every order. */
    fun compiledWorkbook(c: Compilation): ByteArray {
        val sum = Xlsx.Sheet("Summary", widths = listOf(24, 24, 14, 12, 12, 14))
        sum.row(Xlsx.Text("TripTab — ${c.name} (compiled)", bold = true))
        sum.row("Families", c.imports.size)
        sum.row("People", c.imports.sumOf { it.allMembers.size })
        sum.row("Compiled", now())
        sum.blank()
        sum.row(*header("Family", "Person", "Menu prices", "Service", "Tax", "Total"))
        fun money(label1: String, label2: String, b: Bill, bold: Boolean) = arrayOf<Any?>(
            Xlsx.Text(label1, bold), Xlsx.Text(label2, bold), Xlsx.Num(b.subtotal, bold), Xlsx.Num(b.service, bold), Xlsx.Num(b.tax, bold), Xlsx.Num(b.total, bold))
        for (o in c.imports) {
            for (m in o.allMembers) sum.row(*money(o.familyLabel, m, o.bill(m), false))
            sum.row(*money("${o.familyLabel} — total", "", o.bill(), true))
            sum.blank()
        }
        sum.row(*money("Everyone", "", c.bill(), true))

        val byDay = Xlsx.Sheet("By day", widths = listOf(22, 22, 14, 12, 12, 14))
        byDay.row(*header("Day", "Place", "Menu prices", "Service", "Tax", "Total"))
        for ((dk, label) in c.days()) {
            for (p in c.places()) {
                val b = c.imports.fold(Bill.ZERO) { acc, o -> acc + o.bill(dayKey = dk, place = p) }
                if (b.subtotal > 0) byDay.row(*money(label, p, b, false))
            }
            byDay.row(*money("$label — total", "", c.bill(dk), true))
            byDay.blank()
        }

        val orders = Xlsx.Sheet("Orders", widths = listOf(22, 20, 22, 20, 18, 34, 6, 12, 14))
        orders.row(*header("Family", "Person", "Day", "Place", "Section", "Item", "Qty", "Price", "Line total"))
        for (o in c.imports) for (l in o.lines)
            orders.row(o.familyLabel, l.member, dayLabel(l.day, l.date), l.place, l.category, l.item, l.quantity, l.unitPrice, l.lineTotal)

        return Xlsx.write(listOf(sum, byDay, Kitchen.sheet(c, Kitchen.all(c)), orders))
    }

    fun fileName(vararg parts: String): String =
        parts.filter { it.isNotBlank() }.joinToString(" - ").replace(Regex("""[\\/:*?"<>|]"""), "").take(80) + ".xlsx"
}

// ---------------------------------------------------------------------------------------------
// Kitchen order: how many of each dish to prepare, for one restaurant (and day).
// ---------------------------------------------------------------------------------------------

data class KitchenLine(
    val category: String,
    val item: String,
    val itemAr: String,
    val quantity: Int,
    /** Who ordered it: family label → person → quantity. */
    val who: List<Pair<String, Int>>,
)

data class KitchenOrder(val place: String, val dayKey: String?, val dayLabel: String, val lines: List<KitchenLine>) {
    val dishes get() = lines.sumOf { it.quantity }
    val people get() = lines.flatMap { l -> l.who.map { it.first } }.distinct().size
    /** Lines grouped by section, sections in course order. */
    val sections get() = lines.groupBy { it.category }.entries.map { it.key to it.value }
}

object Kitchen {
    /** Course order for sections, so the kitchen list reads starters → mains → desserts → drinks. */
    fun courseRank(category: String): Int {
        val c = category.lowercase()
        return when {
            listOf("breakfast").any { it in c } -> 0
            listOf("salad", "appetizer", "starter", "soup", "mezz").any { it in c } -> 1
            listOf("side").any { it in c } -> 3
            listOf("dessert", "sweet").any { it in c } -> 4
            listOf("drink", "coffee", "tea", "juice", "mocktail", "water", "soft", "beverage", "infusion").any { it in c } -> 5
            listOf("shisha").any { it in c } -> 6
            listOf("activity").any { it in c } -> 7
            c.contains("no longer") -> 8
            else -> 2
        }
    }

    fun order(c: Compilation, place: String, dayKey: String?): KitchenOrder {
        val label = dayKey?.let { k -> c.days().firstOrNull { it.first == k }?.second } ?: "All days"
        val rows = c.imports.flatMap { o -> o.lines.filter { it.place == place && (dayKey == null || it.dayKey == dayKey) }.map { o to it } }
        val lines = rows.groupBy { (_, l) -> MenuMerge.key(l.item) }.values.map { group ->
            val first = group.first().second
            KitchenLine(
                category = first.category.ifBlank { "Other" },
                item = first.item,
                itemAr = group.firstOrNull { it.second.itemAr.isNotBlank() }?.second?.itemAr.orEmpty(),
                quantity = group.sumOf { it.second.quantity },
                who = group.groupBy { (o, l) -> l.member + if (c.imports.count { it.allMembers.contains(l.member) } > 1) " (${o.family})" else "" }
                    .map { (name, g) -> name to g.sumOf { it.second.quantity } },
            )
        }.sortedWith(compareBy({ courseRank(it.category) }, { it.category }, { it.item.lowercase() }))
        return KitchenOrder(place, dayKey, label, lines)
    }

    /** The message to send the restaurant (WhatsApp). */
    fun message(c: Compilation, k: KitchenOrder, withNames: Boolean, withArabic: Boolean, note: String): String = buildString {
        appendLine("Order for ${k.place}")
        appendLine("${c.name} group · ${if (k.dayKey == null) "all days" else k.dayLabel}")
        appendLine("${k.dishes} items for ${k.people} people")
        if (note.isNotBlank()) appendLine(note.trim())
        for ((section, ls) in k.sections) {
            appendLine()
            appendLine("*${section}*")
            for (l in ls) {
                append("${l.quantity} × ${l.item}")
                if (withArabic && l.itemAr.isNotBlank() && l.itemAr != l.item) append(" (${l.itemAr})")
                if (withNames) append(" — " + l.who.joinToString(", ") { (n, q) -> if (q > 1) "$n ×$q" else n })
                appendLine()
            }
        }
        appendLine()
        append("Thank you!")
    }

    fun sheet(c: Compilation, orders: List<KitchenOrder>): Xlsx.Sheet = Xlsx.Sheet("Kitchen order", widths = listOf(22, 20, 18, 34, 26, 6, 40)).apply {
        row(*listOf("Day", "Place", "Section", "Item", "Arabic", "Qty", "Who ordered").map { Xlsx.Text(it, bold = true) }.toTypedArray<Any?>())
        for (k in orders) {
            for (l in k.lines) row(k.dayLabel, k.place, l.category, l.item, l.itemAr, l.quantity,
                l.who.joinToString(", ") { (n, q) -> if (q > 1) "$n ×$q" else n })
            row(Xlsx.Text("${k.dayLabel} · ${k.place} — total", bold = true), null, null, null, null, Xlsx.Num(k.dishes.toDouble(), bold = true, money = false))
            blank()
        }
    }

    /** Every restaurant × day that has orders, in order. */
    fun all(c: Compilation): List<KitchenOrder> = c.days().flatMap { (dk, _) ->
        c.places().map { order(c, it, dk) }.filter { it.lines.isNotEmpty() }
    }

    fun workbook(c: Compilation, orders: List<KitchenOrder>): ByteArray = Xlsx.write(listOf(sheet(c, orders)))
}
