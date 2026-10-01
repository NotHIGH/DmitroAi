package com.dmitroai.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QwenLocalModel {
    @Volatile
    private var loaded = false

    private val lock = Any()

    suspend fun load(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (!loaded) {
                loaded = runCatching {
                    System.loadLibrary("dima-llama")
                    nativeLoadModel(modelPath) == 0
                }.getOrDefault(false)
            }
            loaded
        }
    }

    suspend fun generate(prompt: String): String? = withContext(Dispatchers.Default) {
        if (!loaded) return@withContext null
        runCatching { nativeGenerate(prompt) }
            .getOrNull()
            ?.let(::hideInternalReasoning)
            ?.takeIf(String::isNotBlank)
    }

    private fun hideInternalReasoning(output: String): String {
        val closingTag = output.lastIndexOf("</think>")
        if (closingTag < 0) return if (output.contains("<think>")) "" else output.trim()
        return output.substring(closingTag + "</think>".length)
            .replace(Regex("<\\|[^>]+\\|>"), "")
            .trim()
    }

    fun release() {
        synchronized(lock) {
            if (loaded) {
                nativeRelease()
                loaded = false
            }
        }
    }

    private external fun nativeLoadModel(modelPath: String): Int
    private external fun nativeGenerate(prompt: String): String
    private external fun nativeRelease()
}