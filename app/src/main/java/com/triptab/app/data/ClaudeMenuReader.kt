package com.triptab.app.data

import kotlinx.serialization.json.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * Reads menu photos with Claude (Arabic and English, any layout).
 * Only used when the user has saved their own Anthropic API key; photos go only to api.anthropic.com.
 */
object ClaudeMenuReader {
    const val DEFAULT_MODEL = "claude-sonnet-5-5"
    const val CHEAP_MODEL = "claude-haiku-5-5"

    val PROMPT = """
        These photos show a restaurant or activity menu (possibly several pages, possibly in Arabic and/or English).
        Extract every item that has a price. Reply with ONLY a JSON object, no other text:
        {"items":[{"category":"...","nameEn":"...","nameAr":"...","price":123}],"servicePct":12,"taxPct":14}
        Rules:
        - nameEn: English name. If the menu only gives Arabic, translate it to a short natural English dish name.
        - nameAr: the Arabic name exactly as printed, or "" if none is printed.
        - category: the menu's section heading in English (e.g. "Pizza", "Hot drinks"); translate Arabic headings.
        - price: a number in the menu's currency, digits only (convert Arabic-Indic digits).
        - "A / B" with "290/380": two items, A at 290 and B at 380.
        - A price range like "90-125": one item at the lower price, with the range added to nameEn, e.g. "Waffle (90–125)".
        - Skip items with no price. Do not invent items. Keep the menu's order.
        - servicePct / taxPct: the service charge and tax percentages if the menu states them, otherwise null.
    """.trimIndent()

    fun buildRequest(model: String, jpegs: List<ByteArray>): String = buildJsonObject {
        put("model", model)
        put("max_tokens", 16000)
        putJsonArray("messages") {
            addJsonObject {
                put("role", "user")
                putJsonArray("content") {
                    for (img in jpegs) addJsonObject {
                        put("type", "image")
                        putJsonObject("source") {
                            put("type", "base64"); put("media_type", "image/jpeg")
                            put("data", Base64.getEncoder().encodeToString(img))
                        }
                    }
                    addJsonObject { put("type", "text"); put("text", PROMPT) }
                }
            }
        }
    }.toString()

    /** Parses Claude's API response body into a ScanResult. */
    fun parseResponse(body: String): ScanResult {
        val root = Json.parseToJsonElement(body).jsonObject
        val text = root["content"]?.jsonArray
            ?.mapNotNull { it.jsonObject.takeIf { b -> b["type"]?.jsonPrimitive?.content == "text" }?.get("text")?.jsonPrimitive?.content }
            ?.joinToString("") ?: throw IOException("Claude returned no text")
        return parseItemsJson(text)
    }

    fun parseItemsJson(text: String): ScanResult {
        val start = text.indexOf('{'); val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) throw IOException("Couldn't find the menu list in Claude's reply")
        val obj = Json.parseToJsonElement(text.substring(start, end + 1)).jsonObject
        fun JsonElement?.str() = (this as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        fun JsonElement?.dbl() = (this as? JsonPrimitive)?.let { p ->
            p.doubleOrNull ?: MenuParser.normalize(p.content).replace(Regex("[^0-9.]"), "").toDoubleOrNull()
        }
        val items = obj["items"]?.jsonArray.orEmpty().mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            val price = o["price"].dbl() ?: return@mapNotNull null
            val en = o["nameEn"].str(); val ar = o["nameAr"].str()
            if (price <= 0 || (en.isEmpty() && ar.isEmpty())) return@mapNotNull null
            ScannedItem(category = o["category"].str().ifEmpty { "Menu" }, nameEn = en.ifEmpty { ar }, nameAr = ar, price = price)
        }
        return ScanResult(items, obj["servicePct"].dbl(), obj["taxPct"].dbl())
    }

    /** Blocking network call — run off the main thread. */
    fun read(apiKey: String, model: String, jpegs: List<ByteArray>): ScanResult {
        val conn = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("content-type", "application/json")
            setRequestProperty("x-api-key", apiKey.trim())
            setRequestProperty("anthropic-version", "2023-06-01")
        }
        try {
            conn.outputStream.use { it.write(buildRequest(model, jpegs).toByteArray()) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException(friendlyError(code, body))
            return parseResponse(body)
        } finally {
            conn.disconnect()
        }
    }

    fun friendlyError(code: Int, body: String): String {
        val msg = runCatching {
            Json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
        return when (code) {
            401 -> "The Claude API key was rejected. Check it in Settings."
            403 -> "This API key isn't allowed to use that model."
            429 -> "Claude is busy or your API limit was reached. Try again in a minute."
            413 -> "The photos are too large. Try fewer photos at once."
            in 500..599 -> "Claude is temporarily unavailable ($code). Try again shortly."
            else -> "Claude couldn't read the menu ($code)" + (msg?.let { ": $it" } ?: "")
        }
    }
}
