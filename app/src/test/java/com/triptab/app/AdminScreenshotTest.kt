package com.triptab.app

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.triptab.app.data.*
import com.triptab.app.ui.*
import org.junit.Rule
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
class AdminScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun repoWithCompilation(): Pair<TripRepository, String> {
        val t = BillExportTest()
        val repo = LocalJsonRepository(File(Files.createTempDirectory("tt").toFile(), "d.json"))
        repo.createTrip("Fayoum", "22 Oct 2026", "24 Oct 2026", "Nada", listOf("Yehia", "Alya"), true)
        val c = repo.createCompilation("Fayoum")
        repo.addImports(c, listOf(
            BillExport.fromTrip(t.sampleTrip("Sara", listOf("Omar", "Laila")), "2026-10-24 21:40"),
            BillExport.fromTrip(t.sampleTrip("Karim", listOf("Hana")), "2026-10-24 22:05"),
        ))
        return repo to c
    }

    @Test fun tripListAdmin() {
        val (repo, _) = repoWithCompilation()
        compose.setContent { TripTabTheme { val d by repo.data.collectAsState(); TripListScreen(d, {}, {}, {}, admin = true) } }
        compose.onRoot().captureRoboImage("build/screenshots/9_trips_admin.png")
    }

    @Test fun compiledScreen() {
        val (repo, id) = repoWithCompilation()
        compose.setContent {
            TripTabTheme {
                val d by repo.data.collectAsState()
                CompiledScreen(d.compilations.first { it.id == id }, d.trips, repo, {})
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/10_compiled.png")
        compose.onNodeWithText("Omar").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/11_compiled_person.png")
    }

    @Test fun compiledEmpty() {
        val repo = LocalJsonRepository(File(Files.createTempDirectory("tt").toFile(), "d.json"))
        val c = repo.createCompilation("Fayoum")
        compose.setContent { TripTabTheme { val d by repo.data.collectAsState(); CompiledScreen(d.compilations.first(), d.trips, repo, {}) } }
        compose.onRoot().captureRoboImage("build/screenshots/12_compiled_empty.png")
    }

    @Test fun summaryWithExport() {
        val trip = BillExportTest().sampleTrip()
        compose.setContent { TripTabTheme { SummaryScreen(trip, ALL_DAYS, {}, {}) } }
        compose.onRoot().captureRoboImage("build/screenshots/13_summary_export.png")
    }
}
