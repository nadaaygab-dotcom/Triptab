package com.triptab.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.triptab.app.data.Compilation
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.triptab.app.data.TripDays
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.triptab.app.data.LocalJsonRepository
import com.triptab.app.data.TripRepository
import com.triptab.app.ui.*
import java.io.File

class MainActivity : ComponentActivity() {
    companion object {
        // One repository per process.
        private var repoInstance: TripRepository? = null
    }

    /** Bill files shared/opened from WhatsApp or a file manager, waiting to be imported. */
    private val incoming = mutableStateOf<List<Uri>>(emptyList())

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ExcelFiles.urisFrom(intent).takeIf { it.isNotEmpty() }?.let { incoming.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) ExcelFiles.urisFrom(intent).takeIf { it.isNotEmpty() }?.let { incoming.value = it }
        val settings = AppSettings(this)
        enableEdgeToEdge()
        val repo = repoInstance ?: LocalJsonRepository(File(filesDir, "triptab.json")).also { repoInstance = it }

        setContent {
            TripTabTheme {
                val nav = rememberNavController()
                val data by repo.data.collectAsStateWithLifecycle()
                val tripArg = navArgument("tripId") { type = NavType.StringType }
                // Selected day, shared by the trip, menu and summary screens (ALL_DAYS = sum of all days).
                var day by rememberSaveable { mutableIntStateOf(1) }
                var admin by remember { mutableStateOf(settings.admin) }
                // Result of importing shared files, shown on the compiled order they went into.
                var importNote by remember { mutableStateOf<Pair<String, String>?>(null) }
                val pending by incoming
                var importing by remember { mutableStateOf(false) }
                LaunchedEffect(pending) {
                    if (pending.isEmpty()) return@LaunchedEffect
                    importing = true
                    val results = ExcelFiles.readBills(this@MainActivity, pending)
                    incoming.value = emptyList()
                    val orders = results.mapNotNull { it.order }
                    val tripName = orders.firstOrNull()?.tripName?.trim().orEmpty()
                    // Goes into the compiled order with the same trip name, or a new one.
                    val existing: Compilation? = repo.data.value.compilations.firstOrNull { it.name.trim().equals(tripName, ignoreCase = true) }
                        ?: repo.data.value.compilations.singleOrNull()?.takeIf { tripName.isBlank() }
                    if (orders.isEmpty()) {
                        importNote = "" to importMessage(results, existing)
                    } else {
                        val id = existing?.id ?: repo.createCompilation(tripName.ifBlank { "Compiled order" })
                        repo.addImports(id, orders)
                        if (!admin) { admin = true; settings.admin = true }
                        importNote = id to importMessage(results, existing) +
                            if (existing == null) "\n\nStarted a new compiled order: ${tripName.ifBlank { "Compiled order" }}." else ""
                        nav.navigate("compiled/$id") { popUpTo("trips") }
                    }
                    importing = false
                }
                if (importing) AlertDialog(onDismissRequest = {}, confirmButton = {},
                    title = { Text("Importing bills…") }, text = { CircularProgressIndicator() })
                importNote?.takeIf { it.first.isEmpty() }?.let { (_, m) ->
                    AlertDialog(onDismissRequest = { importNote = null }, title = { Text("Import") }, text = { Text(m) },
                        confirmButton = { TextButton(onClick = { importNote = null }) { Text("OK") } })
                }
                fun openTrip(id: String) {
                    data.trips.find { it.id == id }?.let { day = TripDays.defaultDay(it) }
                    nav.navigate("trip/$id")
                }

                NavHost(navController = nav, startDestination = "trips") {
                    composable("trips") {
                        TripListScreen(
                            data = data,
                            onOpen = { openTrip(it) },
                            onNew = { nav.navigate("newTrip") },
                            onDelete = repo::deleteTrip,
                            admin = admin,
                            onAdmin = { admin = it; settings.admin = it },
                            onOpenCompilation = { nav.navigate("compiled/$it") },
                            onNewCompilation = { n -> nav.navigate("compiled/${repo.createCompilation(n)}") },
                        )
                    }
                    composable("newTrip") {
                        NewTripScreen(
                            onBack = { nav.popBackStack() },
                            onCreate = { name, start, end, owner, family, fayoum ->
                                val id = repo.createTrip(name, start, end, owner, family, fayoum)
                                repo.data.value.trips.find { it.id == id }?.let { day = TripDays.defaultDay(it) }
                                nav.navigate("trip/$id") { popUpTo("trips") }
                            },
                        )
                    }
                    composable("trip/{tripId}", listOf(tripArg)) { e ->
                        val trip = data.trips.find { it.id == e.arguments?.getString("tripId") }
                        if (trip != null) TripHomeScreen(
                            trip = trip, repo = repo,
                            day = day, onDay = { day = it },
                            onBack = { nav.popBackStack() },
                            onOpenPlace = { memberId, placeId -> nav.navigate("menu/${trip.id}/$memberId/$placeId") },
                            onSummary = { nav.navigate("summary/${trip.id}") },
                            onEditPlace = { placeId -> nav.navigate("editPlace/${trip.id}/$placeId") },
                        )
                    }
                    composable(
                        "menu/{tripId}/{memberId}/{placeId}",
                        listOf(tripArg, navArgument("memberId") { type = NavType.StringType },
                               navArgument("placeId") { type = NavType.StringType }),
                    ) { e ->
                        val trip = data.trips.find { it.id == e.arguments?.getString("tripId") }
                        val member = trip?.members?.find { it.id == e.arguments?.getString("memberId") }
                        val place = trip?.places?.find { it.id == e.arguments?.getString("placeId") }
                        if (trip != null && member != null && place != null) MenuScreen(
                            trip = trip, member = member, place = place, repo = repo,
                            day = day, onDay = { day = it },
                            onBack = { nav.popBackStack() },
                            onEdit = { nav.navigate("editPlace/${trip.id}/${place.id}") },
                            onSwitchMember = { m -> nav.navigate("menu/${trip.id}/${m}/${place.id}") {
                                popUpTo("trip/${trip.id}")
                            } },
                            onScan = { nav.navigate("scan/${trip.id}/${place.id}") },
                        )
                    }
                    composable("compiled/{id}", listOf(navArgument("id") { type = NavType.StringType })) { e ->
                        val id = e.arguments?.getString("id")
                        val c = data.compilations.find { it.id == id }
                        val note = importNote?.takeIf { it.first == id }?.second
                        LaunchedEffect(note) { if (note != null) importNote = null }
                        if (c != null) CompiledScreen(
                            compilation = c, trips = data.trips, repo = repo,
                            onBack = { nav.popBackStack() },
                            initialMessage = note,
                        )
                    }
                    composable("summary/{tripId}", listOf(tripArg)) { e ->
                        val trip = data.trips.find { it.id == e.arguments?.getString("tripId") }
                        if (trip != null) SummaryScreen(trip = trip, day = day, onDay = { day = it },
                            onBack = { nav.popBackStack() })
                    }
                    composable(
                        "editPlace/{tripId}/{placeId}",
                        listOf(tripArg, navArgument("placeId") { type = NavType.StringType }),
                    ) { e ->
                        val trip = data.trips.find { it.id == e.arguments?.getString("tripId") }
                        val placeId = e.arguments?.getString("placeId") ?: "new"
                        if (trip != null) PlaceEditScreen(
                            trip = trip, placeId = placeId, repo = repo,
                            onBack = { nav.popBackStack() },
                            onScan = { id -> nav.navigate("scan/${trip.id}/$id") },
                        )
                    }
                    composable(
                        "scan/{tripId}/{placeId}",
                        listOf(tripArg, navArgument("placeId") { type = NavType.StringType }),
                    ) { e ->
                        val trip = data.trips.find { it.id == e.arguments?.getString("tripId") }
                        val place = trip?.places?.find { it.id == e.arguments?.getString("placeId") }
                        if (trip != null && place != null) ScanMenuScreen(
                            trip = trip, place = place, repo = repo,
                            onBack = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
