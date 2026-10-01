package com.dmitroai.app

import org.json.JSONObject
import java.io.InputStream
import java.util.Locale
import kotlin.math.min

data class RussianWord(
    val name: String,
    val synonyms: List<String>,
    val similarWords: List<String>
)

data class RussianWordMatch(
    val word: RussianWord,
    val inputToken: String,
    val correctedToken: String,
    val editDistance: Int,
    val score: Double
)

private data class MutableWordMatch(
    val word: RussianWord,
    var score: Double,
    var inputToken: String,
    var correctedToken: String,
    var editDistance: Int,
    val matchedTokens: MutableSet<String> = mutableSetOf()
)

class RussianDictionary {
    var wordCount: Int = 0
        private set

    var synonymCount: Int = 0
        private set

    private var aliasIndex: Map<String, List<RussianWord>> = emptyMap()
    private var aliasesByLength: Map<Int, List<String>> = emptyMap()

    fun load(input: InputStream) {
        val dictionary = JSONObject(input.bufferedReader(Charsets.UTF_8).use { it.readText() })
        val sourceWords = dictionary.getJSONArray("wordlist")
        val nextIndex = HashMap<String, MutableSet<RussianWord>>()
        var totalSynonyms = 0

        fun addAlias(alias: String, word: RussianWord) {
            val normalized = normalize(alias)
            if (normalized.isBlank()) return
            nextIndex.getOrPut(normalized) { linkedSetOf() }.add(word)
            TOKEN_REGEX.findAll(normalized)
                .map { it.value }
                .filter { it.length > 1 }
                .forEach { token -> nextIndex.getOrPut(token) { linkedSetOf() }.add(word) }
        }

        val parsedWords = ArrayList<RussianWord>(sourceWords.length())
        for (index in 0 until sourceWords.length()) {
            val source = sourceWords.getJSONObject(index)
            val name = source.optString("name").trim()
            if (name.isEmpty()) continue

            val synonyms = source.stringList("synonyms")
            val similarWords = source.stringList("similars")
            val word = RussianWord(name, synonyms, similarWords)
            parsedWords.add(word)
            totalSynonyms += synonyms.size
            addAlias(name, word)
            synonyms.forEach { addAlias(it, word) }
            similarWords.forEach { addAlias(it, word) }
        }

        wordCount = parsedWords.size
        synonymCount = totalSynonyms
        aliasIndex = nextIndex.mapValues { (_, words) -> words.toList() }
        aliasesByLength = aliasIndex.keys
            .filterNot { it.contains(' ') }
            .groupBy { it.length }
    }

    fun search(text: String, resultLimit: Int = 5): List<RussianWordMatch> {
        if (aliasIndex.isEmpty()) return emptyList()

        val queryTokens = TOKEN_REGEX.findAll(normalize(text))
            .map { it.value }
            .filter { it.length > 1 && it !in STOP_WORDS }
            .distinct()
            .toList()
        if (queryTokens.isEmpty()) return emptyList()

        val matches = linkedMapOf<String, MutableWordMatch>()
        queryTokens.forEach { token ->
            val directMatches = aliasIndex[token]
            val resolved = if (!directMatches.isNullOrEmpty()) {
                token to 0
            } else {
                closestAlias(token) ?: return@forEach
            }
            val (matchedAlias, distance) = resolved
            val matchedWords = directMatches ?: aliasIndex[matchedAlias].orEmpty()

            matchedWords.forEach { word ->
                val key = normalize(word.name)
                val tokenScore = when (distance) {
                    0 -> if (normalize(word.name) == token) 3.0 else 2.0
                    else -> 1.5 / distance
                }
                val current = matches[key]
                if (current == null) {
                    matches[key] = MutableWordMatch(
                        word = word,
                        score = tokenScore,
                        inputToken = token,
                        correctedToken = matchedAlias,
                        editDistance = distance
                    ).also { it.matchedTokens.add(token) }
                } else if (current.matchedTokens.add(token)) {
                    current.score += tokenScore
                    if (distance < current.editDistance) {
                        current.inputToken = token
                        current.correctedToken = matchedAlias
                        current.editDistance = distance
                    }
                }
            }
        }

        return matches.values
            .sortedWith(compareByDescending<MutableWordMatch> { it.score }
                .thenByDescending { it.matchedTokens.size }
                .thenBy { it.word.name.length })
            .take(resultLimit)
            .map {
                RussianWordMatch(
                    word = it.word,
                    inputToken = it.inputToken,
                    correctedToken = it.correctedToken,
                    editDistance = it.editDistance,
                    score = it.score
                )
            }
    }

    private fun closestAlias(query: String): Pair<String, Int>? {
        val maxDistance = if (query.length < 5) 1 else 2

        fun scan(sameFirstCharacter: Boolean): Pair<String, Int>? {
            var closest: String? = null
            var closestDistance = maxDistance + 1
            for (length in (query.length - maxDistance).coerceAtLeast(1)..query.length + maxDistance) {
                for (candidate in aliasesByLength[length].orEmpty()) {
                    if (candidate == query) continue
                    if (sameFirstCharacter && candidate.first() != query.first()) continue
                    val distance = editDistance(candidate, query, maxDistance)
                    if (distance < closestDistance) {
                        closest = candidate
                        closestDistance = distance
                        if (distance == 1) return closest to distance
                    }
                }
            }
            return closest?.let { it to closestDistance }
        }

        val sameFirst = scan(sameFirstCharacter = true)
        if (sameFirst?.second == 1) return sameFirst
        val best = scan(sameFirstCharacter = false) ?: return sameFirst
        return if (sameFirst != null && sameFirst.second < best.second) sameFirst else best
    }

    private fun editDistance(left: String, right: String, limit: Int): Int {
        if (kotlin.math.abs(left.length - right.length) > limit) return limit + 1
        var previous = IntArray(right.length + 1) { it }
        for (leftIndex in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = leftIndex + 1
            var rowMinimum = current[0]
            for (rightIndex in right.indices) {
                val substitution = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    previous[rightIndex + 1] + 1,
                    current[rightIndex] + 1,
                    previous[rightIndex] + substitution
                )
                rowMinimum = min(rowMinimum, current[rightIndex + 1])
            }
            if (rowMinimum > limit) return limit + 1
            previous = current
        }
        return previous[right.length]
    }

    private fun normalize(text: String): String =
        text.lowercase(Locale.ROOT).replace('ё', 'е').trim()

    private fun JSONObject.stringList(key: String): List<String> {
        val values = optJSONArray(key) ?: return emptyList()
        return List(values.length()) { values.optString(it).trim() }
            .filter(String::isNotEmpty)
    }

    private companion object {
        val TOKEN_REGEX = Regex("[\\p{L}]+")
        val STOP_WORDS = setOf(
            "что", "как", "это", "или", "где", "кто", "почему", "когда", "чем",
            "для", "меня", "мне", "тебя", "твой", "если", "такой", "такое", "такая",
            "будет", "быть", "был", "есть", "она", "они", "его", "ее", "их", "наш",
            "этот", "эти", "при", "про", "вот"
        )
    }
}