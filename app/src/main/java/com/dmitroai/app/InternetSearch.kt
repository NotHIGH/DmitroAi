package com.dmitroai.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object InternetSearch {
    fun search(query: String): String? {
        if (query.isBlank()) return null
        val encodedQuery = URLEncoder.encode(query.take(180), Charsets.UTF_8.name())
        val searchJson = getJson(
            "https://ru.wikipedia.org/w/rest.php/v1/search/page?q=$encodedQuery&limit=1"
        ) ?: return null
        val page = searchJson.optJSONArray("pages")
            ?.optJSONObject(0)
            ?: return null
        val title = page.optString("title").takeIf(String::isNotBlank) ?: return null
        val encodedTitle = URLEncoder.encode(title, Charsets.UTF_8.name()).replace("+", "%20")
        val summary = getJson(
            "https://ru.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"
        ) ?: return null
        val extract = summary.optString("extract").trim()
        if (extract.isBlank()) return null
        return "$title\n\n$extract\n\nИсточник: Википедия"
    }

    private fun getJson(address: String): JSONObject? {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "DimaAI/0.0.3 (Android dictionary lookup)")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}