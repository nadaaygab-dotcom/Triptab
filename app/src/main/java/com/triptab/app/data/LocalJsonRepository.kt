package com.triptab.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/** Keeps everything in one JSON file in the app's private storage. Works fully offline. */
class LocalJsonRepository(private val file: File) : TripRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    private val state = MutableStateFlow(upgradeMenus(load()))
    override val data: StateFlow<AppData> = state.asStateFlow()

    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    } catch (e: Exception) {
        // Keep a copy of an unreadable file rather than silently losing it.
        file.copyTo(File(file.parentFile, file.name + ".corrupt"), overwrite = true)
        AppData()
    }

    /** One-time updates to built-in menus (e.g. a restaurant's new prices); saved if anything changed. */
    private fun upgradeMenus(d: AppData): AppData {
        val next = d.copy(trips = d.trips.map(KomElDikkaMenu::upgrade))
        if (next != d) runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(AppData.serializer(), next))
            tmp.renameTo(file)
        }
        return next
    }

    @Synchronized
    private fun mutate(block: (AppData) -> AppData) {
        val next = block(state.value)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), next))
        tmp.renameTo(file)
        state.value = next
    }

    private fun mutateTrip(tripId: String, block: (Trip) -> Trip) = mutate { d ->
        d.copy(trips = d.trips.map { if (it.id == tripId) block(it) else it })
    }

    private fun Trip.mapPlace(placeId: String, block: (Place) -> Place) =
        copy(places = places.map { if (it.id == placeId) block(it) else it })

    override fun createTrip(
        name: String, startDate: String, endDate: String, ownerName: String,
        familyNames: List<String>, includeFayoumPlaces: Boolean,
    ): String {
        val members = (listOf(ownerName) + familyNames)
            .map { it.trim() }.filter { it.isNotEmpty() }.map { Member(name = it) }
        val trip = Trip(
            name = name.trim(), startDate = startDate.trim(), endDate = endDate.trim(),
            ownerName = ownerName.trim(), members = members,
            places = if (includeFayoumPlaces) FayoumSeed.places() else emptyList(),
        )
        mutate { it.copy(trips = it.trips + trip) }
        return trip.id
    }

    override fun deleteTrip(tripId: String) = mutate { d -> d.copy(trips = d.trips.filterNot { it.id == tripId }) }

    override fun updateTripInfo(tripId: String, name: String, startDate: String, endDate: String) =
        mutateTrip(tripId) { it.copy(name = name.trim(), startDate = startDate.trim(), endDate = endDate.trim()) }

    override fun addMember(tripId: String, name: String) =
        mutateTrip(tripId) { it.copy(members = it.members + Member(name = name.trim())) }

    override fun renameMember(tripId: String, memberId: String, name: String) = mutateTrip(tripId) { t ->
        t.copy(members = t.members.map { if (it.id == memberId) it.copy(name = name.trim()) else it })
    }

    override fun removeMember(tripId: String, memberId: String) = mutateTrip(tripId) { t ->
        t.copy(members = t.members.filterNot { it.id == memberId },
               orders = t.orders.filterNot { it.memberId == memberId })
    }

    override fun upsertPlace(tripId: String, place: Place) = mutateTrip(tripId) { t ->
        if (t.places.any { it.id == place.id }) t.mapPlace(place.id) { place }
        else t.copy(places = t.places + place)
    }

    override fun removePlace(tripId: String, placeId: String) = mutateTrip(tripId) { t ->
        t.copy(places = t.places.filterNot { it.id == placeId },
               orders = t.orders.filterNot { it.placeId == placeId })
    }

    override fun upsertItem(tripId: String, placeId: String, item: MenuItem) = mutateTrip(tripId) { t ->
        t.mapPlace(placeId) { p ->
            if (p.items.any { it.id == item.id }) p.copy(items = p.items.map { if (it.id == item.id) item else it })
            else p.copy(items = p.items + item)
        }
    }

    override fun removeItem(tripId: String, placeId: String, itemId: String) = mutateTrip(tripId) { t ->
        t.mapPlace(placeId) { p -> p.copy(items = p.items.filterNot { it.id == itemId }) }
            .copy(orders = t.orders.filterNot { it.placeId == placeId && it.itemId == itemId })
    }

    override fun setQuantity(tripId: String, memberId: String, placeId: String, itemId: String, day: Int, quantity: Int) =
        mutateTrip(tripId) { t ->
            fun OrderLine.matches() = this.memberId == memberId && this.placeId == placeId &&
                this.itemId == itemId && this.day == day
            val existing = t.orders.find { it.matches() }
            when {
                quantity <= 0 -> t.copy(orders = t.orders.filterNot { it.matches() })
                existing != null -> t.copy(orders = t.orders.map { if (it.matches()) it.copy(quantity = quantity) else it })
                else -> t.copy(orders = t.orders + OrderLine(memberId = memberId, placeId = placeId,
                    itemId = itemId, quantity = quantity, day = day))
            }
        }

    override fun createCompilation(name: String): String {
        val c = Compilation(name = name.trim().ifEmpty { "Compiled order" }, createdAt = BillExport.now())
        mutate { it.copy(compilations = it.compilations + c) }
        return c.id
    }

    private fun mutateCompilation(id: String, block: (Compilation) -> Compilation) = mutate { d ->
        d.copy(compilations = d.compilations.map { if (it.id == id) block(it) else it })
    }

    override fun renameCompilation(id: String, name: String) = mutateCompilation(id) { it.copy(name = name.trim()) }

    override fun deleteCompilation(id: String) = mutate { d -> d.copy(compilations = d.compilations.filterNot { it.id == id }) }

    override fun addImports(compilationId: String, orders: List<ImportedOrder>) = mutateCompilation(compilationId) { c ->
        val stamp = BillExport.now()
        orders.fold(c) { acc, o0 ->
            val o = o0.copy(importedAt = stamp)
            val i = acc.imports.indexOfFirst { it.sourceId == o.sourceId }
            if (i >= 0) acc.copy(imports = acc.imports.toMutableList().also { it[i] = o })
            else acc.copy(imports = acc.imports + o)
        }
    }

    override fun removeImport(compilationId: String, sourceId: String) = mutateCompilation(compilationId) { c ->
        c.copy(imports = c.imports.filterNot { it.sourceId == sourceId })
    }

    override fun clearOrders(tripId: String, memberId: String, placeId: String, day: Int) = mutateTrip(tripId) { t ->
        t.copy(orders = t.orders.filterNot { it.memberId == memberId && it.placeId == placeId && it.day == day })
    }
}
