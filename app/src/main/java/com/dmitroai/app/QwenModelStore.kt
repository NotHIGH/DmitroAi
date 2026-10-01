package com.dmitroai.app

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class QwenModelStore(context: Context) {
    private val modelFile = File(context.filesDir, MODEL_FILE_NAME)
    private val temporaryFile = File(context.filesDir, "$MODEL_FILE_NAME.part")

    fun isDownloaded(): Boolean = modelFile.isFile && modelFile.length() >= EXPECTED_MODEL_BYTES * 99 / 100

    suspend fun download(onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        if (isDownloaded()) return@withContext modelFile

        val storage = StatFs(modelFile.parentFile!!.absolutePath)
        if (storage.availableBytes < EXPECTED_MODEL_BYTES + 128L * 1024 * 1024) {
            throw IOException("Недостаточно места. Освободи минимум 1,4 ГБ.")
        }

        temporaryFile.delete()
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "DimaAI/0.0.3 Android model downloader")
            setRequestProperty("Accept-Encoding", "identity")
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Не удалось скачать модель (HTTP ${connection.responseCode}).")
            }
            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: EXPECTED_MODEL_BYTES
            var receivedBytes = 0L
            var lastReportedProgress = -1
            connection.inputStream.use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 16)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        receivedBytes += count
                        val progress = (receivedBytes * 100 / totalBytes).toInt().coerceIn(0, 99)
                        if (progress != lastReportedProgress) {
                            lastReportedProgress = progress
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                    output.fd.sync()
                }
            }
            if (receivedBytes < EXPECTED_MODEL_BYTES * 99 / 100) {
                throw IOException("Загрузка прервалась. Попробуй ещё раз через Wi-Fi.")
            }
            if (modelFile.exists() && !modelFile.delete()) {
                throw IOException("Не удалось заменить старый файл модели.")
            }
            if (!temporaryFile.renameTo(modelFile)) {
                throw IOException("Не удалось сохранить файл модели.")
            }
            withContext(Dispatchers.Main) { onProgress(100) }
            modelFile
        } catch (error: Exception) {
            temporaryFile.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun modelPath(): String = modelFile.absolutePath

    companion object {
        const val MODEL_SIZE_LABEL = "1,28 ГБ"
        const val EXPECTED_MODEL_BYTES = 1_282_439_264L
        const val MODEL_FILE_NAME = "Qwen3-1.7B-Q4_K_M.gguf"
        const val MODEL_URL =
            "https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf?download=true"
    }
}