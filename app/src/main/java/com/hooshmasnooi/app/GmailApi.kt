package com.hooshmasnooi.app

import com.hooshmasnooi.app.core.AtlasText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GmailApi {
    suspend fun latestInbox(accessToken: String, limit: Int = 8): List<String> = withContext(Dispatchers.IO) {
        val list = getJson(
            "https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=$limit&q=in%3Ainbox",
            accessToken
        )
        val messages = list.optJSONArray("messages") ?: return@withContext emptyList()
        (0 until messages.length()).mapNotNull { index ->
            val id = messages.optJSONObject(index)?.optString("id") ?: return@mapNotNull null
            val detail = getJson(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=metadata&metadataHeaders=From&metadataHeaders=Subject&metadataHeaders=Date",
                accessToken
            )
            val headers = detail.optJSONObject("payload")?.optJSONArray("headers")
            fun header(name: String): String {
                for (i in 0 until (headers?.length() ?: 0)) {
                    val item = headers?.optJSONObject(i) ?: continue
                    if (item.optString("name").equals(name, ignoreCase = true)) {
                        return item.optString("value")
                    }
                }
                return ""
            }
            val subject = header("Subject").ifBlank { AtlasText.of("(بدون موضوع)", "(No subject)") }
            val from = header("From").ifBlank { AtlasText.of("فرستنده نامشخص", "Unknown sender") }
            val date = header("Date").ifBlank { AtlasText.of("تاریخ نامشخص", "Unknown date") }
            AtlasText.of("موضوع: $subject\nفرستنده: $from\nتاریخ: $date\nخلاصه: ${detail.optString("snippet")}", "Subject: $subject\nFrom: $from\nDate: $date\nPreview: ${detail.optString("snippet")}")
        }
    }

    private fun getJson(endpoint: String, token: String): JSONObject {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (status !in 200..299) throw IllegalStateException("Gmail API HTTP $status")
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
