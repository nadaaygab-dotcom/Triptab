package com.triptab.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.takahirom.roborazzi.captureRoboImage
import com.triptab.app.data.*
import com.triptab.app.ui.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xhdpi")
class ScreenshotTest {

    private fun sampleRepo(): Pair<TripRepository, String> {
        val dir = Files.createTempDirectory("tt").toFile()
        val repo = LocalJsonRepository(File(dir, "d.json"))
        val id = repo.createTrip("Fayoum", "22 Oct 2026", "24 Oct 2026", "Nada", listOf("Yehia", "Alya"), true)
        val t = repo.data.value.trips.first()
        val kd = t.places.first { it.name == "Kom El Dikka" }
        val ibis = t.places.first { it.name == "Ibis Restaurant" }
        fun item(p: Place, n: String) = p.items.first { it.nameEn == n }.id
        val (nada, yehia, alya) = t.members
        repo.setQuantity(id, nada.id, kd.id, item(kd, "Grilled kofta"), 1, 1)
        repo.setQuantity(id, nada.id, kd.id, item(kd, "Baladi salad"), 1, 1)
        repo.setQuantity(id, nada.id, kd.id, item(kd, "Mineral water large"), 1, 2)
        repo.setQuantity(id, nada.id, kd.id, item(kd, "Turkish coffee"), 2, 1)
        repo.setQuantity(id, nada.id, kd.id, item(kd, "Mineral water large"), 2, 1)
        repo.setQuantity(id, yehia.id, kd.id, item(kd, "Chicken pizza"), 1, 1)
        repo.setQuantity(id, alya.id, ibis.id, item(ibis, "Margherita"), 2, 1)
        repo.setQuantity(id, alya.id, ibis.id, item(ibis, "Mango juice"), 2, 1)
        return repo to id
    }

    private fun shot(name: String, content: @Composable () -> Unit) =
        captureRoboImage("build/screenshots/$name.png") { TripTabTheme { content() } }

    @Test fun screens() {
        val (repo, id) = sampleRepo()
        shot("0_trips") { val d by repo.data.collectAsState(); TripListScreen(d, {}, {}, {}) }
        shot("1_new_trip") { NewTripScreen({}, { _, _, _, _, _, _ -> }) }
        shot("2_home") {
            val d by repo.data.collectAsState()
            TripHomeScreen(d.trips.first { it.id == id }, repo, 1, {}, {}, { _, _ -> }, {}, {})
        }
        shot("3_menu") {
            val d by repo.data.collectAsState()
            val t = d.trips.first()
            MenuScreen(t, t.members[0], t.places.first { it.name == "Kom El Dikka" }, repo, 1, {}, {}, {}, {})
        }
        shot("3b_menu_all_days") {
            val d by repo.data.collectAsState()
            val t = d.trips.first()
            MenuScreen(t, t.members[0], t.places.first { it.name == "Kom El Dikka" }, repo, ALL_DAYS, {}, {}, {}, {})
        }
        shot("4_summary") { val d by repo.data.collectAsState(); SummaryScreen(d.trips.first(), ALL_DAYS, {}, {}) }
        shot("4b_summary_day2") { val d by repo.data.collectAsState(); SummaryScreen(d.trips.first(), 2, {}, {}) }
        shot("6_scan_photos") {
            val d by repo.data.collectAsState()
            val t = d.trips.first()
            ScanMenuScreen(t, t.places.first { it.name == "Tibtunis" }, repo, {})
        }
        shot("7_scan_review") {
            val d by repo.data.collectAsState()
            val t = d.trips.first()
            val kd = t.places.first { it.name == "Kom El Dikka" }
            ScanMenuScreen(t, kd, repo, {}, initialResult = ScanResult(listOf(
                ScannedItem("Hot drinks", "Turkish coffee", "قهوة تركي", 70.0),
                ScannedItem("Hot drinks", "Espresso", "اسبريسو", 75.0),
                ScannedItem("Hot drinks", "Sahlab", "سحلب", 85.0),
                ScannedItem("Desserts", "Basbousa", "بسبوسة", 95.0),
                ScannedItem("Desserts", "Om Ali", "ام علي", 150.0)), servicePct = 12.0, taxPct = 14.0))
        }
        shot("8_menu_empty") {
            val d by repo.data.collectAsState()
            val t = d.trips.first()
            MenuScreen(t, t.members[0], t.places.first { it.name == "Tibtunis" }, repo, 1, {}, {}, {}, {})
        }
        shot("5_edit_place") {
            val d by repo.data.collectAsState()
            val t = d.trips.first()
            PlaceEditScreen(t, t.places.first { it.name == "Safari" }.id, repo, {})
        }
    }
}
