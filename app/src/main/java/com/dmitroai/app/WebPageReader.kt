package com.dmitroai.app

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL

data class ReadWebPage(val url: String, val text: String)

object WebPageReader {
    private const val MAX_RESPONSE_BYTES = 1_500_000
    private const val MAX_PAGE_CHARS = 8_000
    private const val MAX_REDIRECTS = 4

    fun findUrl(message: String): String? =
        Regex("https://[^\\s<>\\\"']+", RegexOption.IGNORE_CASE)
            .find(message)
            ?.value
            ?.trimEnd('.', ',', '!', '?', ')', ']', '}', '"', '\'')

    suspend fun read(address: String): ReadWebPage? = withContext(Dispatchers.IO) {
        runCatching { readPage(address) }.getOrNull()
    }

    private fun readPage(address: String): ReadWebPage? {
        var currentUrl = URL(address)
        var redirects = 0

        while (redirects <= MAX_REDIRECTS) {
            validatePublicHttps(currentUrl)
            val connection = (currentUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "DimaAI/0.0.3 (Android reader)")
                setRequestProperty("Accept", "text/html,text/plain;q=0.9")
                setRequestProperty("Accept-Encoding", "identity")
            }

            try {
                val code = connection.responseCode
                if (code in REDIRECT_CODES) {
                    val location = connection.getHeaderField("Location") ?: return null
                    currentUrl = URL(currentUrl, location)
                    redirects++
                    continue
                }
                if (code !in 200..299) return null

                val type = connection.contentType.orEmpty().lowercase()
                if (!(type.contains("text/html") || type.contains("text/plain") || type.contains("application/xhtml"))) {
                    throw IOException("Поддерживаются HTML-страницы и обычный текст, не PDF.")
                }
                if (connection.contentLengthLong > MAX_RESPONSE_BYTES) {
                    throw IOException("Страница слишком большая для чтения.")
                }

                val bytes = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_RESPONSE_BYTES) throw IOException("Страница слишком большая для чтения.")
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                val html = String(bytes, Charsets.UTF_8)
                val cleanHtml = html.replace(
                    Regex("(?is)<(script|style|noscript|svg|iframe)[^>]*>.*?</\\1>"),
                    " "
                )
                val text = Html.fromHtml(cleanHtml, Html.FROM_HTML_MODE_COMPACT)
                    .toString()
                    .replace(Regex("[\\t\\x0B\\f\\r ]+"), " ")
                    .replace(Regex("\\n{3,}"), "\n\n")
                    .trim()
                    .take(MAX_PAGE_CHARS)
                if (text.isBlank()) return null
                return ReadWebPage(currentUrl.toString(), text)
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private fun validatePublicHttps(url: URL) {
        if (!url.protocol.equals("https", ignoreCase = true)) {
            throw IOException("Разрешены только HTTPS-ссылки.")
        }
        val host = url.host.lowercase()
        if (host.isBlank() || host == "localhost" || host.endsWith(".local")) {
            throw IOException("Локальные адреса нельзя открывать из чата.")
        }
        val addresses = InetAddress.getAllByName(host)
        if (addresses.isEmpty() || addresses.any {
                it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress ||
                    it.isSiteLocalAddress || it.isMulticastAddress
            }
        ) {
            throw IOException("Адрес должен быть доступен из публичного интернета.")
        }
    }

    private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
}