package com.triptab.app

import com.triptab.app.data.*
import com.triptab.app.ui.applyReview
import com.triptab.app.ui.buildReview
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ClaudeReaderTest {
    private val reply = """
        Here is the menu:
        {"items":[
          {"category":"Pizza","nameEn":"Chicken pizza","nameAr":"بتزا فراخ","price":300},
          {"category":"Hot drinks","nameEn":"Turkish coffee","nameAr":"قهوة تركي","price":"٦٦"},
          {"category":"Hot drinks","nameEn":"","nameAr":"","price":10},
          {"category":"Desserts","nameEn":"Om Ali","nameAr":"ام علي","price":null}
        ],"servicePct":12,"taxPct":14}
    """.trimIndent()

    @Test fun parsesItemsAndCharges() {
        val r = ClaudeMenuReader.parseItemsJson(reply)
        assertEquals(listOf("Chicken pizza", "Turkish coffee"), r.items.map { it.nameEn })
        assertEquals(66.0, r.items[1].price, 0.0)          // Arabic-Indic digits in a string
        assertEquals("بتزا فراخ", r.items[0].nameAr)
        assertEquals(12.0, r.servicePct!!, 0.0); assertEquals(14.0, r.taxPct!!, 0.0)
    }

    @Test fun parsesApiEnvelope() {
        val body = buildJsonObject {
            putJsonArray("content") { addJsonObject { put("type", "text"); put("text", reply) } }
        }.toString()
        assertEquals(2, ClaudeMenuReader.parseResponse(body).items.size)
    }

    @Test fun requestCarriesImagesThenPrompt() {
        val req = Json.parseToJsonElement(ClaudeMenuReader.buildRequest("claude-sonnet-5-5", listOf(byteArrayOf(1, 2), byteArrayOf(3))))
            .jsonObject
        assertEquals("claude-sonnet-5-5", req["model"]!!.jsonPrimitive.content)
        val content = req["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonArray
        assertEquals(listOf("image", "image", "text"), content.map { it.jsonObject["type"]!!.jsonPrimitive.content })
        assertEquals("image/jpeg", content[0].jsonObject["source"]!!.jsonObject["media_type"]!!.jsonPrimitive.content)
    }

    @Test fun friendlyErrors() {
        assertTrue(ClaudeMenuReader.friendlyError(401, "").contains("key"))
        assertTrue(ClaudeMenuReader.friendlyError(400, """{"error":{"message":"bad image"}}""").contains("bad image"))
    }

    @Test fun reviewAddsNewUpdatesChangedSkipsSame() {
        val repo = LocalJsonRepository(File(Files.createTempDirectory("tt").toFile(), "d.json"))
        val tripId = repo.createTrip("T", "", "", "N", emptyList(), false)
        val place = Place(name = "Cafe", items = listOf(
            MenuItem(category = "Hot", nameEn = "Turkish coffee", price = 60.0),
            MenuItem(category = "Hot", nameEn = "Tea", price = 40.0)))
        repo.upsertPlace(tripId, place)
        val scan = ScanResult(listOf(
            ScannedItem("Hot", "Turkish Coffee", "قهوة تركي", 66.0),   // price changed → update
            ScannedItem("Hot", "tea", price = 40.0),                    // same → unticked
            ScannedItem("Cold", "Lemon mint", price = 65.0)), 12.0, null) // new → add
        val rows = buildReview(place, scan)
        assertEquals(listOf(true, false, true), rows.map { it.selected })
        applyReview(repo, tripId, place, rows, scan.servicePct to scan.taxPct)
        val p = repo.data.value.trips[0].places[0]
        assertEquals(3, p.items.size)
        assertEquals(66.0, p.items.first { it.nameEn == "Turkish coffee" }.price, 0.0)
        assertEquals("قهوة تركي", p.items.first { it.nameEn == "Turkish coffee" }.nameAr)
        assertEquals(12.0, p.servicePct, 0.0); assertEquals(0.0, p.taxPct, 0.0)
    }
}
