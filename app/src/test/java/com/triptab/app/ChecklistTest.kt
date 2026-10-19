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
import kotlinx.serialization.json.Json
import org.junit.Assert.*
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
class ChecklistTest {
    @get:Rule val compose = createComposeRule()

    private fun repo() = LocalJsonRepository(File(Files.createTempDirectory("tt").toFile(), "d.json"))

    @Test fun newTripGetsTheList() {
        val r = repo()
        r.createTrip("Fayoum", "22 Oct 2026", "24 Oct 2026", "Nada", emptyList(), true)
        val l = r.data.value.trips[0].checklist
        assertEquals(25, l.size)
        assertEquals(listOf("Covers", "Towels"), l.take(2).map { it.text })
        assertTrue(l.any { it.text == "Board games / cards" } && l.any { it.text == "Plastic cups" })
        assertEquals(6, l.map { it.section }.distinct().size)
        assertTrue(l.none { it.done })
    }

    @Test fun existingTripsGetTheListOnceOnly() {
        val dir = Files.createTempDirectory("tt").toFile(); val f = File(dir, "d.json")
        // A trip saved by v0.6 (no checklist fields).
        f.writeText(Json.encodeToString(AppData.serializer(), AppData(listOf(Trip(name = "Fayoum", ownerName = "N"))))
            .replace(",\"checklist\":[]", "").replace(",\"checklistSeeded\":false", ""))
        var r = LocalJsonRepository(f)
        val t = r.data.value.trips[0]
        assertEquals(25, t.checklist.size)
        // Delete everything: reopening the app must not bring the list back.
        t.checklist.forEach { r.removeChecklistItem(t.id, it.id) }
        r = LocalJsonRepository(f)
        assertEquals(0, r.data.value.trips[0].checklist.size)
    }

    @Test fun checkAddEditResetAndShare() {
        val r = repo()
        val id = r.createTrip("Fayoum", "", "", "Nada", emptyList(), false)
        fun t() = r.data.value.trips[0]
        val cash = t().checklist.first { it.text == "Cash" }
        r.setChecked(id, cash.id, true)
        r.setChecked(id, t().checklist.first { it.text == "Covers" }.id, true)
        r.upsertChecklistItem(id, ChecklistItem(text = "Flashlight", section = "Fun & tech"))
        val l = t().checklist
        assertEquals("Flashlight", l[l.indexOfFirst { it.text == "Speakers" } + 1].text)   // added at the end of its section
        r.upsertChecklistItem(id, cash.copy(text = "Cash (small notes)", done = true))
        assertTrue(t().checklist.any { it.text == "Cash (small notes)" && it.done })

        val text = PackingList.shareText(t())
        assertTrue(text, text.startsWith("🎒 Fayoum — packing list\n2 of 26 packed"))
        assertTrue(text.contains("*Clothes*\n☐ Socks"))
        assertFalse(text.contains("Covers")); assertFalse(text.contains("Cash"))

        r.uncheckAll(id)
        assertTrue(t().checklist.none { it.done })
    }

    @Test fun screenshots() {
        val r = repo()
        val id = r.createTrip("Fayoum", "22 Oct 2026", "24 Oct 2026", "Nada", listOf("Yehia", "Alya"), true)
        listOf("Covers", "Towels", "Socks", "Cash", "Chargers").forEach { n ->
            r.setChecked(id, r.data.value.trips[0].checklist.first { it.text == n }.id, true)
        }
        compose.setContent { TripTabTheme { val d by r.data.collectAsState(); ChecklistScreen(d.trips[0], r, {}) } }
        compose.onRoot().captureRoboImage("build/screenshots/16_checklist.png")
        compose.onNodeWithText("Shampoo").performClick()     // tap a row to tick it
        compose.waitForIdle()
        assertTrue(r.data.value.trips[0].checklist.first { it.text == "Shampoo" }.done)
    }

    @Test fun homeCard() {
        val r = repo()
        r.createTrip("Fayoum", "22 Oct 2026", "24 Oct 2026", "Nada", listOf("Yehia", "Alya"), true)
        r.setChecked(r.data.value.trips[0].id, r.data.value.trips[0].checklist[0].id, true)
        compose.setContent { TripTabTheme { val d by r.data.collectAsState(); TripHomeScreen(d.trips[0], r, 1, {}, {}, { _, _ -> }, {}, {}) } }
        compose.onNodeWithText("1 of 25 packed").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/17_home_checklist.png")
    }
}
