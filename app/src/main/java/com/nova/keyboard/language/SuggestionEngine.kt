package com.nova.keyboard.language

import android.content.Context
import com.nova.keyboard.data.LearningRepository

/**
 * Word prediction, autocorrection and next-word prediction.
 * Bundled dictionaries are read from assets; personal learning lives in SQLite
 * and can be erased from the Privacy Center at any time.
 */
class SuggestionEngine(
    private val context: Context,
    private val learning: LearningRepository
) {

    private val english: List<String> by lazy { load("english_words.txt") }
    private val sinhala: List<String> by lazy { load("sinhala_words.txt") }

    /** Rank in the bundled list == real-world frequency order. */
    private val englishRank: Map<String, Int> by lazy {
        english.withIndex().associate { it.value to it.index }
    }
    private val sinhalaRank: Map<String, Int> by lazy {
        sinhala.withIndex().associate { it.value to it.index }
    }

    private fun load(name: String): List<String> = runCatching {
        context.assets.open(name).bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toList()
        }
    }.getOrDefault(emptyList())

    private val userCache = HashMap<String, Pair<Long, List<String>>>()

    /** Cached so a keystroke never waits on SQLite. */
    private fun userWords(language: String): List<String> {
        val now = System.currentTimeMillis()
        userCache[language]?.let { (at, words) -> if (now - at < 4000L) return words }
        val words = runCatching { learning.topWords(language, 400) }.getOrDefault(emptyList())
        userCache[language] = now to words
        return words
    }

    fun invalidateCache() = userCache.clear()

    fun dictionary(language: String): List<String> = if (language == "si") sinhala else english

    private fun rank(language: String): Map<String, Int> =
        if (language == "si") sinhalaRank else englishRank

    fun isKnown(word: String, language: String): Boolean {
        val w = word.lowercase()
        if (w.isEmpty()) return true
        // Words with digits or symbols are never "misspelled" and must never be
        // rewritten by autocorrect.
        if (w.any { !it.isLetter() && it != '\'' && it != '\u2019' }) return true
        if (w.length <= 2) return true
        if (runCatching { learning.frequency(w, language) > 0 }.getOrDefault(false)) return true
        return rank(language).containsKey(w)
    }

    fun suggestions(prefix: String, language: String, limit: Int = 3): List<String> {
        val p = prefix.lowercase()
        if (p.isEmpty()) return emptyList()
        val out = LinkedHashSet<String>()

        // 1. Words this user actually types win.
        userWords(language)
            .filter { it.startsWith(p) && it != p }
            .take(limit)
            .forEach { out.add(it) }

        // 2. Bundled dictionary, ordered by real-world frequency.
        val dict = dictionary(language)
        val ranks = rank(language)
        dict.asSequence()
            .filter { it.startsWith(p) && it.length > p.length }
            .sortedBy { ranks[it] ?: Int.MAX_VALUE }
            .take(limit)
            .forEach { out.add(it) }

        return out.take(limit).toList()
    }

    fun nextWords(previous: String, language: String, limit: Int = 3): List<String> {
        if (previous.isBlank()) return emptyList()
        val out = LinkedHashSet<String>(runCatching { learning.nextWords(previous, limit) }.getOrDefault(emptyList()))
        if (out.size < limit) {
            dictionary(language).asSequence()
                .filter { it.startsWith(previous.take(1)) && it != previous }
                .take(limit)
                .forEach { out.add(it) }
        }
        return out.take(limit).toList()
    }

    /**
     * Returns a replacement word, or null when the typed word is already fine.
     *
     * Only a *unique* best candidate is returned. The previous version returned the
     * first word within the edit distance, in dictionary order, so "teh" could
     * become anything from "tea" to "ten" — a wrong autocorrection is worse than
     * no autocorrection. Ties now resolve by frequency, and a tie in frequency
     * means no correction at all.
     */
    fun correct(word: String, language: String): String? {
        val w = word.lowercase()
        if (w.length < 3 || isKnown(w, language)) return null
        val dict = dictionary(language)
        if (dict.isEmpty()) return null
        val ranks = rank(language)
        val maxDistance = if (w.length > 6) 2 else 1
        var best: String? = null
        var bestDistance = maxDistance + 1
        var bestRank = Int.MAX_VALUE
        var ambiguous = false

        for (candidate in dict) {
            if (candidate.length !in (w.length - 1)..(w.length + 1)) continue
            if (candidate[0] != w[0]) continue
            val d = editDistance(w, candidate, maxDistance)
            if (d > maxDistance) continue
            val r = ranks[candidate] ?: Int.MAX_VALUE
            when {
                d < bestDistance -> {
                    best = candidate; bestDistance = d; bestRank = r; ambiguous = false
                }
                d == bestDistance && r < bestRank -> {
                    best = candidate; bestRank = r; ambiguous = false
                }
                d == bestDistance -> ambiguous = true
            }
        }
        if (ambiguous && bestDistance > 1) return null
        return best
    }

    fun learn(word: String, language: String, previous: String?) {
        learning.recordWord(word, language)
        if (!previous.isNullOrBlank()) learning.recordBigram(previous, word)
        userCache.clear()
    }

    companion object {
        /** Bounded Levenshtein distance; returns max+1 once the bound is exceeded. */
        fun editDistance(a: String, b: String, max: Int = 2): Int {
            if (kotlin.math.abs(a.length - b.length) > max) return max + 1
            var prev = IntArray(b.length + 1) { it }
            var cur = IntArray(b.length + 1)
            for (i in 1..a.length) {
                cur[0] = i
                var rowMin = cur[0]
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                    if (cur[j] < rowMin) rowMin = cur[j]
                }
                if (rowMin > max) return max + 1
                val swap = prev; prev = cur; cur = swap
            }
            return prev[b.length]
        }
    }
}
