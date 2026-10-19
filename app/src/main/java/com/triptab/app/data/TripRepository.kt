package com.triptab.app.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Storage boundary for the app. Phase 1 = [LocalJsonRepository] (one family per phone, offline).
 * Phase 2 (shared live sync) can provide another implementation of this interface
 * without touching the screens.
 */
interface TripRepository {
    val data: StateFlow<AppData>

    fun createTrip(name: String, startDate: String, endDate: String, ownerName: String,
                   familyNames: List<String>, includeFayoumPlaces: Boolean): String
    fun deleteTrip(tripId: String)
    fun updateTripInfo(tripId: String, name: String, startDate: String, endDate: String)

    fun addMember(tripId: String, name: String)
    fun renameMember(tripId: String, memberId: String, name: String)
    fun removeMember(tripId: String, memberId: String)

    fun upsertPlace(tripId: String, place: Place)
    fun removePlace(tripId: String, placeId: String)
    fun upsertItem(tripId: String, placeId: String, item: MenuItem)
    fun removeItem(tripId: String, placeId: String, itemId: String)

    /** Sets the quantity a member ordered of one item at one place on one day (0 removes it). */
    fun setQuantity(tripId: String, memberId: String, placeId: String, itemId: String, day: Int, quantity: Int)
    fun clearOrders(tripId: String, memberId: String, placeId: String, day: Int)

    // Admin mode — compiled orders.
    fun createCompilation(name: String): String
    fun renameCompilation(id: String, name: String)
    fun deleteCompilation(id: String)
    /** Adds families' orders; a newer file from the same family (same sourceId) replaces the older one. */
    fun addImports(compilationId: String, orders: List<ImportedOrder>)
    fun removeImport(compilationId: String, sourceId: String)
}
