package com.triptab.app.data

import kotlinx.serialization.Serializable

@Serializable
data class ChecklistItem(
    val id: String = newId(),
    val text: String,
    val section: String = "",
    val done: Boolean = false,
)

/** The starting packing list every trip gets (editable per trip). */
object PackingList {
    private val DEFAULTS: List<Pair<String, List<String>>> = listOf(
        "Bedding & bath" to listOf("Covers", "Towels", "Tooth brush and paste", "Shampoo", "Soap", "Wipes", "Tissues"),
        "Clothes" to listOf("Socks", "Crocs", "Shoes", "Jackets", "Swimsuit", "Caps"),
        "Health" to listOf("Medications and first aid", "Mosquito repellent", "Sunblock cream"),
        "Food & kitchen" to listOf("Snacks", "Marshmallows and sticks", "Kettle", "Hot drinks", "Plastic cups"),
        "Fun & tech" to listOf("Board games / cards", "Chargers", "Speakers"),
        "Money" to listOf("Cash"),
    )

    fun defaults(): List<ChecklistItem> = DEFAULTS.flatMap { (section, items) -> items.map { ChecklistItem(text = it, section = section) } }

    /** Gives a trip the starting list once (trips made before the checklist existed included). */
    fun seed(trip: Trip): Trip = if (trip.checklistSeeded) trip
        else trip.copy(checklist = trip.checklist + defaults(), checklistSeeded = true)

    /** Plain-text list for WhatsApp: what's still to pack, by section. */
    fun shareText(trip: Trip): String = buildString {
        val todo = trip.checklist.filter { !it.done }
        appendLine("🎒 ${trip.name} — packing list")
        appendLine("${trip.checklist.count { it.done }} of ${trip.checklist.size} packed")
        if (todo.isEmpty()) { appendLine(); append("Everything is packed ✅"); return@buildString }
        appendLine(); appendLine("Still to pack:")
        for ((section, items) in todo.groupBy { it.section.ifBlank { "Other" } }) {
            appendLine(); appendLine("*$section*")
            items.forEach { appendLine("☐ ${it.text}") }
        }
    }.trimEnd()
}
