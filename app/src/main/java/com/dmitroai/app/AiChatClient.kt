package com.dmitroai.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AiMessage(val role: String, val content: String)

object AiChatClient {
    suspend fun generate(apiKey: String, history: List<AiMessage>, dictionaryContext: String): String? =
        withContext(Dispatchers.IO) {
            val messages = JSONArray()
            messages.put(
                JSONObject()
                    .put("role", "system")
                    .put(
                        "content",
                        "Ты Dima AI, дружелюбный русскоязычный помощник. Отвечай естественно и связно, " +
                            "учитывай предыдущие сообщения, мягко исправляй опечатки. Если пользователь пишет " +
                            "на другом языке, отвечай на нём. Не придумывай факты. Не показывай скрытые " +
                            "внутренние рассуждения; дай только полезный вывод. " +
                            if (dictionaryContext.isBlank()) "" else "Локальные подсказки словаря: $dictionaryContext"
                    )
            )
            history.takeLast(12).forEach { message ->
                if (message.role == "user" || message.role == "assistant") {
                    messages.put(
                        JSONObject()
                            .put("role", message.role)
                            .put("content", message.content.take(4_000))
                    )
                }
            }

            val body = JSONObject()
                .put("model", "openai/gpt-5.4-nano")
                .put("messages", messages)
                .put("max_completion_tokens", 700)
                .put("reasoning_effort", "medium")
                .put("stream", false)

            val connection = (URL("https://gen.pollinations.ai/v1/chat/completions")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 45_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            try {
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }
                if (connection.responseCode !in 200..299) return@withContext null
                val response = JSONObject(
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                )
                response.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
}