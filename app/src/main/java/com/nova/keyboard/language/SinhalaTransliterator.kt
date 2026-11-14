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
 *
 * Two rules make the output real Sinhala instead of "consonants in a row":
 *  - a word-final consonant is followed by al-lakuna, otherwise the inherent
 *    vowel would be read: `ran` is රන්, not රන;
 *  - "n" before a stop consonant of the same place becomes the homorganic
 *    nasal: `sinhala` is සිංහල, `Kandy` is කැන්ඩි.
 */
object SinhalaTransliterator {

    const val HAL = "\u0DCA"
    const val ZWJ = "\u200D"
    const val ANUSVARA = "\u0D82"
    const val VISARGA = "\u0D83"

    private const val VOWEL_SIGNS = "\u0DCF\u0DD0\u0DD1\u0DD2\u0DD3\u0DD4\u0DD6\u0DD8\u0DD9\u0DDA\u0DDB\u0DDC\u0DDD\u0DDE\u0DDF"

    // Order matters, not just length: `th` must resolve to ත (0DAD), not to the
    // ථ (0DAE) that the aspirated transliteration `th` used to produce.
    private val consonants = listOf(
        "kh" to "\u0D9B", "k" to "\u0D9A", "gh" to "\u0D9D", "g" to "\u0D9C", "ng" to "\u0D9E",
        "chh" to "\u0DA1", "ch" to "\u0DA0", "c" to "\u0DA0",
        "jh" to "\u0DA3", "j" to "\u0DA2", "ny" to "\u0DA5",
        "Th" to "\u0DA8", "Dh" to "\u0DAA", "T" to "\u0DA7", "D" to "\u0DA9", "N" to "\u0DAB",
        // Colloquial Singlish: `th` is the dental ත (thaththa -> තත්ත) and a bare
        // `t` is the retroflex ට (watak -> වටක්). The previous table had these two
        // swapped, so half of every Sinhala word came out with the wrong letter.
        "th" to "\u0DAD", "dh" to "\u0DB0", "t" to "\u0DA7", "d" to "\u0DAF", "n" to "\u0DB1",
        "ph" to "\u0DB5", "p" to "\u0DB4", "bh" to "\u0DB7", "b" to "\u0DB6", "m" to "\u0DB8",
        "y" to "\u0DBA", "r" to "\u0DBB", "l" to "\u0DBD", "v" to "\u0DC0", "w" to "\u0DC0",
        "sh" to "\u0DC1", "Sh" to "\u0DC2", "s" to "\u0DC3", "h" to "\u0DC4", "L" to "\u0DC5",
        "f" to "\u0DC6"
    )

    /**
     * - `n` before a consonant of a different place is the retroflex ණ:
     *   `ran` is රන්, `pissa` keeps න, `Kandy` is කැන්ඩි.
     * - the three Sinhala nasals `ng`, `ny`, `n` were previously mapped to
     *   ඞ/ඤ/ණ, which made ordinary words (`sinhala`, `yanna`) unreadable.
     */
    private val nasalNg = "\u0D82"          // ං anusvara
    private val nasalNy = "\u0DA4"          // ඤ
    private val nasalN = "\u0DB1"           // න
    private val nasalRetroflex = "\u0DAB"   // ණ
    private val nasalM = "\u0DB8"           // ම

    private val independentVowels = listOf(
        "aee" to "\u0D88", "aa" to "\u0D86", "ae" to "\u0D87", "ai" to "\u0D93",
        "au" to "\u0D96", "ee" to "\u0D92", "ii" to "\u0D8A", "oo" to "\u0D95",
        "uu" to "\u0D8C", "a" to "\u0D85", "e" to "\u0D91", "i" to "\u0D89",
        "o" to "\u0D94", "u" to "\u0D8B"
    )

    private val vowelSigns = listOf(
        "aee" to "\u0DD1", "aa" to "\u0DCF", "ae" to "\u0DD0", "ai" to "\u0DDB",
        "au" to "\u0DDE", "ee" to "\u0DDA", "ii" to "\u0DD3", "oo" to "\u0DDD",
        "uu" to "\u0DD6", "e" to "\u0DD9", "i" to "\u0DD2", "o" to "\u0DDC",
        "u" to "\u0DD4"
    ).sortedByDescending { it.first.length }

    /** Consonants that DROP a following inherent vowel, grouped by articulation class. */
    private val stopGroups = mapOf(
        "\u0DB1" to "ttd",          // n + dental/alveolar stop stays න
        "\u0DAB" to ""
    )

    fun isConsonantChar(ch: Char): Boolean = ch in '\u0D9A'..'\u0DC6' && ch != '\u0DCA'

    fun isSignChar(ch: Char): Boolean = VOWEL_SIGNS.indexOf(ch) >= 0

    /**
     * Digits and punctuation pass through untouched. Roman letters are folded to
     * lower case first: a phonetic keyboard that left "Kandy" as "K" + mangled
     * Sinhala was worse than one that transliterates it.
     */
    private fun isPassthrough(ch: Char): Boolean = ch.code < 128 && !ch.isLetter()

    /** Converts a romanised buffer into Sinhala script. */
    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""
        val out = StringBuilder()
        var i = 0
        // true while the last emitted glyph is a bare consonant that can still take a sign
        var openConsonant = false
        // the romanised token that produced the open consonant, used for nasal rules
        var lastConsonantToken = ""

        fun closeOpenConsonant() {
            if (openConsonant) {
                out.append(HAL)
                openConsonant = false
            }
        }

        // Fold to lower case so capitalised Singlish still converts, but keep the
        // `n`/`N` and `t`/`T` distinction that the schema is built on.
        val folded = input.map { if (it in 'A'..'Z' && it !in "NDT") it.lowercaseChar() else it }.joinToString("")

        while (i < folded.length) {
            val rest = folded.substring(i)
            val ch = folded[i]

            if (isPassthrough(ch)) {
                // Latin text inside a Singlish word ("Kandy", "WiFi") is left alone
                // rather than silently mangled into Sinhala letters.
                closeOpenConsonant()
                out.append(ch)
                lastConsonantToken = ""
                i += 1
                continue
            }

            // Homorganic nasal: "n" + a stop consonant of the same place.
            if (ch == 'n' || ch == 'N') {
                val nasal = when {
                    rest.startsWith("nh") -> "ng"
                    rest.startsWith("nng") -> "ng"
                    rest.startsWith("ndh") -> "nd"
                    rest.startsWith("nd") || rest.startsWith("nt") || rest.startsWith("nth") -> "nd"
                    rest.startsWith("nj") -> "nj"
                    rest.startsWith("ng") -> "ng"
                    rest.startsWith("nk") || rest.startsWith("nkh") -> "ng"
                    rest.startsWith("nch") -> "nj"
                    rest.startsWith("nb") || rest.startsWith("nB") -> "nb"
                    else -> null
                }
                if (nasal != null) {
                    closeOpenConsonant()
                    when (nasal) {
                        "ng" -> out.append(nasalNg)      // සිංහල (sinhala), ශ්‍රී ලංකා
                        "nj" -> out.append(nasalNy)      // කැන්ඩි / අඤ්…
                        "nb" -> out.append(nasalM)       // අම්බලම්
                        else -> out.append(nasalN)       // කැන්ඩි (Kandy), තාන්ත
                    }
                    i += 1
                    lastConsonantToken = "n"
                    // the stop that follows becomes an al-lakuna + consonant
                    openConsonant = false
                    continue
                }
            }

            // ශ්‍රී must be matched before the consonant table, otherwise "s"+"r"
            // consume it and the result is ස්රී.
            if (rest.startsWith("shrii") || rest.startsWith("srii")) {
                closeOpenConsonant()
                out.append("\u0DC1\u0DCA\u200D\u0DBB\u0DD3")
                i += if (rest.startsWith("shrii")) 5 else 4
                lastConsonantToken = ""
                continue
            }
            if (rest.startsWith("shri") || rest.startsWith("sri")) {
                closeOpenConsonant()
                out.append("\u0DC1\u0DCA\u200D\u0DBB\u0DD3")
                i += if (rest.startsWith("shri")) 4 else 3
                lastConsonantToken = ""
                continue
            }

            val cons = consonants.firstOrNull { rest.startsWith(it.first) }
            if (cons != null) {
                closeOpenConsonant()
                out.append(cons.second)
                openConsonant = true
                lastConsonantToken = cons.first
                i += cons.first.length
                continue
            }

            val sign = vowelSigns.firstOrNull { rest.startsWith(it.first) }
            if (sign != null && openConsonant) {
                out.append(sign.second)
                openConsonant = false
                lastConsonantToken = ""
                i += sign.first.length
                continue
            }

            // A bare "a" after a consonant is the inherent vowel: no codepoint at all.
            if (openConsonant && ch == 'a') {
                openConsonant = false
                lastConsonantToken = ""
                i += 1
                continue
            }

            val indep = independentVowels.firstOrNull { rest.startsWith(it.first) }
            if (indep != null) {
                closeOpenConsonant()
                out.append(indep.second)
                lastConsonantToken = ""
                i += indep.first.length
                continue
            }

            closeOpenConsonant()
            lastConsonantToken = ""
            when {
                rest.startsWith("M") -> { out.append(ANUSVARA); i += 1 }
                rest.startsWith("H") -> { out.append(VISARGA); i += 1 }
                rest.startsWith("~") -> { out.append(HAL); i += 1 }
                else -> { out.append(input[i]); i += 1 }
            }
        }

        // A word-final consonant needs al-lakuna; the old code left the inherent
        // vowel open, so `ran` came out රන instead of රන්.
        closeOpenConsonant()
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
