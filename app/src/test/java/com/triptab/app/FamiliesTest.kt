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
class FamiliesTest {
    @get:Rule val compose = createComposeRule()

    /** Nada's trip with her sister Sara's family added, and orders for both. */
    private fun twoFamilies(): Pair<LocalJsonRepository, String> {
        val r = LocalJsonRepository(File(Files.createTempDirectory("tt").toFile(), "d.json"))
        val id = r.createTrip("Fayoum", "22 Oct 2026", "24 Oct 2026", "Nada", listOf("Yehia", "Alya"), true)
        val sara = r.addFamily(id, "Sara's family", listOf("Sara", "Omar", ""))
        val t = r.data.value.trips[0]
        val kd = t.places.first { it.name == "Kom El Dikka" }
        fun item(n: String) = kd.items.first { it.nameEn == n }.id
        fun m(n: String) = t.members.first { it.name == n }.id
        r.setQuantity(id, m("Nada"), kd.id, item("Grilled kofta"), 1, 1)       // 360
        r.setQuantity(id, m("Yehia"), kd.id, item("Margherita pizza"), 1, 1)   // 200
        r.setQuantity(id, m("Sara"), kd.id, item("Grilled kofta"), 1, 2)       // 720
        r.setQuantity(id, m("Omar"), kd.id, item("Pasta basil"), 2, 1)         // 280, day 2
        return r to sara
    }

    @Test fun addFamilyAndFamilyBills() {
        val (r, saraId) = twoFamilies()
        val t = r.data.value.trips[0]
        assertEquals(listOf("Nada's family", "Sara's family"), t.familyList.map { it.name })
        assertEquals(listOf("Sara", "Omar"), t.membersOf(saraId).map { it.name })      // blank names skipped
        val k = 1.12 * 1.14
        assertEquals((360 + 200) * k, Billing.familyBill(t, t.familyList[0].id).total, 1e-9)
        assertEquals((720 + 280) * k, Billing.familyBill(t, saraId).total, 1e-9)
        assertEquals(720 * k, Billing.familyBill(t, saraId, 1).total, 1e-9)
        // Adding a person goes to the chosen family.
        r.addMember(t.id, "Laila", saraId)
        assertEquals(saraId, r.data.value.trips[0].members.first { it.name == "Laila" }.familyId)
    }

    @Test fun removeFamilyRemovesItsPeopleAndOrders() {
        val (r, saraId) = twoFamilies()
        val id = r.data.value.trips[0].id
        r.removeFamily(id, saraId)
        val t = r.data.value.trips[0]
        assertEquals(1, t.familyList.size)
        assertEquals(listOf("Nada", "Yehia", "Alya"), t.members.map { it.name })
        assertEquals(2, t.orders.size)
        r.removeFamily(id, t.familyList[0].id)                     // the last family stays
        assertEquals(1, r.data.value.trips[0].familyList.size)
    }

    @Test fun oldTripsGetOneFamily() {
        val f = File(Files.createTempDirectory("tt").toFile(), "d.json")
        val old = Trip(id = "t1", name = "Fayoum", ownerName = "Nada", members = listOf(Member(name = "Nada"), Member(name = "Yehia")))
        f.writeText(Json.encodeToString(AppData.serializer(), AppData(listOf(old))))
        val t = LocalJsonRepository(f).data.value.trips[0]
        assertEquals(listOf(Family("t1", "Nada's family")), t.families)
        assertTrue(t.members.all { it.familyId == "t1" })
    }

    @Test fun oneFileCarriesEachFamilySeparately() {
        val (r, _) = twoFamilies()
        val t = r.data.value.trips[0]
        val orders = BillExport.fromTrip(t, "2026-10-24 22:00")
        assertEquals(listOf("Nada's family", "Sara's family"), orders.map { it.familyLabel })
        assertEquals(t.id, orders[0].sourceId)                       // same id as files from v0.7, so re-imports replace
        assertNotEquals(orders[0].sourceId, orders[1].sourceId)
        assertEquals("Sara", orders[1].family)
        assertEquals(listOf("Sara", "Omar"), orders[1].members)

        val bytes = BillExport.familyWorkbook(orders)
        assertEquals(orders, BillExport.parseAll(bytes))
        val sheets = Xlsx.read(bytes)
        assertEquals("TripTab bill — Nada's family + Sara's family", sheets["Bill"]!![0][0])
        assertTrue(sheets["Bill"]!!.any { it.firstOrNull() == "All families — total" })
        assertEquals("Family", sheets["Orders"]!![0][0])

        // Admin: both families arrive as separate families.
        val c = r.createCompilation("Fayoum")
        r.addImports(c, BillExport.parseAll(bytes))
        val comp = r.data.value.compilations.single()
        assertEquals(listOf("Nada's family", "Sara's family"), comp.imports.map { it.familyLabel })
        assertEquals(Billing.familyBill(t, t.familyList[0].id).total + Billing.familyBill(t, t.familyList[1].id).total,
            comp.bill().total, 1e-9)
    }

    @Test fun singleFamilyFilesAreUnchangedAndOldFilesImport() {
        val trip = BillExportTest().sampleTrip()
        val one = BillExport.fromTrip(trip).single()
        val sheets = Xlsx.read(BillExport.familyWorkbook(one))
        assertEquals("1", sheets["Data"]!![0][2])                     // v0.7 apps can still read it
        assertEquals("Person", sheets["Orders"]!![0][0])
        // A v0.7 file (no family name row) still gets a sensible label.
        val v07 = one.copy(familyName = "")
        assertEquals("Nada's family", BillExport.parse(BillExport.familyWorkbook(v07)).familyLabel)
    }

    @Test fun screens() {
        val (r, _) = twoFamilies()
        compose.setContent { TripTabTheme { val d by r.data.collectAsState(); TripHomeScreen(d.trips[0], r, 1, {}, {}, { _, _ -> }, {}, {}) } }
        compose.onNodeWithText("Sara's family").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/18_home_families.png")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Add family").performClick()
        compose.mainClock.advanceTimeBy(2000)
        compose.onNode(androidx.compose.ui.test.isDialog()).captureRoboImage("build/screenshots/19_add_family.png")
    }

    @Test fun summaryAllFamilies() {
        val (r, _) = twoFamilies()
        compose.setContent { TripTabTheme { val d by r.data.collectAsState(); SummaryScreen(d.trips[0], ALL_DAYS, {}, {}) } }
        compose.onRoot().captureRoboImage("build/screenshots/20_summary_families.png")
        val text = buildShareText(r.data.value.trips[0])
        assertTrue(text, text.contains("👨‍👩‍👧 Sara's family: EGP 1,276.80"))
        assertTrue(text.contains("All families total"))
    }
}
