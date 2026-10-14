package com.triptab.app

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = repoInstance ?: LocalJsonRepository(File(filesDir, "triptab.json")).also { repoInstance = it }

        setContent {
            TripTabTheme {
                val nav = rememberNavController()
                val data by repo.data.collectAsStateWithLifecycle()
                val tripArg = navArgument("tripId") { type = NavType.StringType }
                // Selected day, shared by the trip, menu and summary screens (ALL_DAYS = sum of all days).
                var day by rememberSaveable { mutableIntStateOf(1) }
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
                        )
                    }
                }
            }
        }
    }
}
