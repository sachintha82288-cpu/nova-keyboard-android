package com.nova.keyboard.language

/**
 * Singlish (romanised Sinhala) to Sinhala Unicode transliteration.
 *
 * Scheme (longest match wins, applied left to right):
 *   consonants  k kh g gh ng | c ch chh j jh ny | T Th D Dh N | t th d dh n
 *               p ph b bh m | y r l v w sh Sh s h L f
 *   vowels      a aa ae aee i ii u uu e ee ai o oo au
 *   signs       aa=\u0DCF ae=\u0DD0 aee=\u0DD1 i=\u0DD2 ii=\u0DD3 u=\u0DD4 uu=\u0DD6
 *               e=\u0DD9 ee=\u0DDA ai=\u0DDB o=\u0DDC oo=\u0DDD au=\u0DDE
 *   hal         \u0DCA (implicit between two consonants, or typed as "~")
 *   anusvara    M=\u0D82, H=\u0D83
 *
 * A vowel after a consonant becomes its dependent sign; a bare "a" after a
 * consonant is the inherent vowel and produces no extra codepoint.
 */
object SinhalaTransliterator {

    const val HAL = "\u0DCA"

    private const val VOWEL_SIGNS = "\u0DCF\u0DD0\u0DD1\u0DD2\u0DD3\u0DD4\u0DD6\u0DD8\u0DD9\u0DDA\u0DDB\u0DDC\u0DDD\u0DDE\u0DDF"

    private val consonants = listOf(
        "kh" to "\u0D9B", "k" to "\u0D9A", "gh" to "\u0D9D", "g" to "\u0D9C", "ng" to "\u0D9E",
        "chh" to "\u0DAA", "ch" to "\u0DA1", "c" to "\u0DA0", "jh" to "\u0DA3", "j" to "\u0DA2",
        "ny" to "\u0DA4",
        "Th" to "\u0DA8", "T" to "\u0DA7", "Dh" to "\u0DAA", "D" to "\u0DA9", "N" to "\u0DAB",
        "th" to "\u0DAE", "t" to "\u0DAD", "dh" to "\u0DB0", "d" to "\u0DAF", "n" to "\u0DB1",
        "ph" to "\u0DB5", "p" to "\u0DB4", "bh" to "\u0DB7", "b" to "\u0DB6", "m" to "\u0DB8",
        "y" to "\u0DBA", "r" to "\u0DBB", "l" to "\u0DBD", "v" to "\u0DC0", "w" to "\u0DC0",
        "sh" to "\u0DC1", "Sh" to "\u0DC2", "s" to "\u0DC3", "h" to "\u0DC4", "L" to "\u0DC5",
        "f" to "\u0DC6"
    ).sortedByDescending { it.first.length }

    private val independentVowels = listOf(
        "aee" to "\u0D88", "aa" to "\u0D86", "ae" to "\u0D87", "ai" to "\u0D93",
        "au" to "\u0D96", "ee" to "\u0D92", "ii" to "\u0D8A", "oo" to "\u0D95",
        "uu" to "\u0D8C", "a" to "\u0D85", "e" to "\u0D91", "i" to "\u0D89",
        "o" to "\u0D94", "u" to "\u0D8B"
    ).sortedByDescending { it.first.length }

    private val vowelSigns = listOf(
        "aee" to "\u0DD1", "aa" to "\u0DCF", "ae" to "\u0DD0", "ai" to "\u0DDB",
        "au" to "\u0DDE", "ee" to "\u0DDA", "ii" to "\u0DD3", "oo" to "\u0DDD",
        "uu" to "\u0DD6", "e" to "\u0DD9", "i" to "\u0DD2", "o" to "\u0DDC",
        "u" to "\u0DD4"
    ).sortedByDescending { it.first.length }

    fun isConsonantChar(ch: Char): Boolean = ch in '\u0D9A'..'\u0DC6' && ch != '\u0DCA'

    fun isSignChar(ch: Char): Boolean = VOWEL_SIGNS.indexOf(ch) >= 0

    /** Converts a romanised buffer into Sinhala script. */
    fun transliterate(input: String): String {
        val out = StringBuilder()
        var i = 0
        var lastConsonant = false
        while (i < input.length) {
            val rest = input.substring(i)
            val cons = consonants.firstOrNull { rest.startsWith(it.first) }
            if (cons != null) {
                if (lastConsonant) out.append(HAL)
                out.append(cons.second)
                lastConsonant = true
                i += cons.first.length
                continue
            }
            val sign = vowelSigns.firstOrNull { rest.startsWith(it.first) }
            if (sign != null && lastConsonant) {
                out.append(sign.second)
                lastConsonant = false
                i += sign.first.length
                continue
            }
            // A bare "a" after a consonant is the inherent vowel: no codepoint at all.
            if (lastConsonant && rest.startsWith("a")) {
                lastConsonant = false
                i += 1
                continue
            }
            val indep = independentVowels.firstOrNull { rest.startsWith(it.first) }
            if (indep != null) {
                out.append(indep.second)
                lastConsonant = false
                i += indep.first.length
                continue
            }
            when {
                rest.startsWith("M") -> { out.append("\u0D82"); lastConsonant = false; i += 1 }
                rest.startsWith("H") -> { out.append("\u0D83"); lastConsonant = false; i += 1 }
                rest.startsWith("~") -> { out.append(HAL); lastConsonant = false; i += 1 }
                rest.startsWith(" ") -> { out.append(' '); lastConsonant = false; i += 1 }
                else -> { out.append(input[i]); lastConsonant = false; i += 1 }
            }
        }
        return out.toString()
    }

    /**
     * Candidate list for the current romanised buffer: the full conversion first,
     * then dictionary words that share the buffer's leading consonant skeleton.
     */
    fun candidates(buffer: String, dictionary: List<String>, limit: Int = 6): List<String> {
        if (buffer.isBlank()) return emptyList()
        val direct = transliterate(buffer)
        val out = LinkedHashSet<String>()
        if (direct.isNotBlank()) out.add(direct)
        val prefix = transliterate(buffer.substring(0, minOf(buffer.length, 2)))
        if (prefix.isNotBlank()) {
            dictionary.filter { it.startsWith(prefix) && it != direct }
                .take(limit - out.size)
                .forEach { out.add(it) }
        }
        return out.take(limit)
    }
}
