package com.nova.keyboard.language

/**
 * Sinhala Wijesekara (Wij 9) Unicode keyboard.
 *
 * Base and shift layers follow the standard Wijesekara mapping used by
 * SLS 1134 based keyboards; characters that Windows/Android expose on the
 * AltGr layer (\u0D9E, \u0D9F, \u0DA6, \u0DAC, \u0DB3, \u0DB9, \u0D86 ...) are
 * provided as long-press alternates here instead, which is the standard
 * touch-keyboard convention.
 *
 * Sinhala is encoded in logical order: the consonant comes first and the
 * vowel sign after it, even when the sign is drawn to the left (\u0DD9).
 * [WijesekaraComposer] therefore buffers one syllable so that a sign typed
 * "as you write" lands in the right place.
 */
object WijesekaraEngine {

    const val HAL = "\u0DCA"          // al-lakuna
    const val KOMBUVA = "\u0DD9"      // ෙ
    const val AELA = "\u0DCF"         // ා
    const val KOMBUVA_AELA = "\u0DDC" // ො
    const val KOMBUVA_DIGA = "\u0DDD" // ෝ
    const val ZWJ = "\u200D"

    /** key -> glyph for the unshifted layer */
    val base: Map<String, String> = mapOf(
        "q" to "\u0DD4", "w" to "\u0D85", "e" to "\u0DD0", "r" to "\u0DBB",
        "t" to "\u0D91", "y" to "\u0DC4", "u" to "\u0DB8", "i" to "\u0DC3",
        "o" to "\u0DAF", "p" to "\u0DA0",
        "a" to HAL, "s" to "\u0DD2", "d" to AELA, "f" to KOMBUVA, "g" to "\u0DA7",
        "h" to "\u0DBA", "j" to "\u0DC0", "k" to "\u0DB1", "l" to "\u0D9A",
        ";" to "\u0DAD",
        "z" to "\u2018", "x" to "\u0D82", "c" to "\u0DA2", "v" to "\u0DA9",
        "b" to "\u0D89", "n" to "\u0DB6", "m" to "\u0DB4",
        "," to "\u0DBD", "." to "\u0D9C", "/" to "/"
    )

    /** key -> glyph for the shifted layer */
    val shifted: Map<String, String> = mapOf(
        "q" to "\u0DD6", "w" to "\u0D8B", "e" to "\u0DD1", "r" to "\u0D8D",
        "t" to "\u0D94", "y" to "\u0DC1", "u" to "\u0DB9", "i" to "\u0DC2",
        "o" to "\u0DB0", "p" to "\u0DA1",
        "a" to "\u0DDF", "s" to "\u0DD3", "d" to "\u0DD8", "f" to "\u0DC6",
        "g" to "\u0DA8", "h" to HAL + ZWJ + "\u0DBA", "j" to "\u0DC5\u0DD4",
        "k" to "\u0DAB", "l" to "\u0D9B", ";" to "\u0DAE",
        "z" to "\u201C", "x" to "\u0D83", "c" to "\u0DA3", "v" to "\u0DAA",
        "b" to "\u0D8A", "n" to "\u0DB7", "m" to "\u0DB5",
        "," to "\u0DC5", "." to "\u0D9D", "/" to "?"
    )

    /** Long-press alternates: characters that sit on the AltGr layer of Wij 9. */
    val longPress: Map<String, List<String>> = mapOf(
        "\u0D85" to listOf("\u0D86", "\u0D87", "\u0D88"),      // අ -> ආ ඇ ඈ
        "\u0D89" to listOf("\u0D8A", "\u0D8D"),                  // ඉ -> ඊ ඍ
        "\u0D8B" to listOf("\u0D8C"),                            // උ -> ඌ
        "\u0D91" to listOf("\u0D92", "\u0D93"),                  // එ -> ඒ ඓ
        "\u0D94" to listOf("\u0D95", "\u0D96"),                  // ඔ -> ඕ ඖ
        "\u0D9A" to listOf("\u0D9B"),                            // ක -> ඛ
        "\u0D9C" to listOf("\u0D9D", "\u0D9F"),                  // ග -> ඝ ඟ
        "\u0DA2" to listOf("\u0DA3", "\u0DA6"),                  // ජ -> ඣ ඦ
        "\u0DA7" to listOf("\u0DA8"),                            // ට -> ඨ
        "\u0DA9" to listOf("\u0DAA", "\u0DAC"),                  // ඩ -> ඪ ඬ
        "\u0DAD" to listOf("\u0DAE"),                            // ත -> ථ
        "\u0DAF" to listOf("\u0DB0", "\u0DB3"),                  // ද -> ධ ඳ
        "\u0DB1" to listOf("\u0DAB", "\u0D9E"),                  // න -> ණ ඞ
        "\u0DB4" to listOf("\u0DB5"),                            // ප -> ඵ
        "\u0DB6" to listOf("\u0DB7", "\u0DB9"),                  // බ -> භ ඹ
        "\u0DC3" to listOf("\u0DC2", "\u0DC1"),                  // ස -> ෂ ශ
        "\u0DBD" to listOf("\u0DC5"),                            // ල -> ළ
        "\u0DA0" to listOf("\u0DA1"),                            // ච -> ඡ
        "\u0DA4" to listOf("\u0DA5"),                            // ඤ -> ඥ
        KOMBUVA to listOf(KOMBUVA_AELA, KOMBUVA_DIGA, "\u0DDB", "\u0DDE"),
        "\u0DD2" to listOf("\u0DD3"),
        "\u0DD4" to listOf("\u0DD6", "\u0DD8"),
        "\u0DD0" to listOf("\u0DD1"),
        AELA to listOf("\u0DDF"),
        HAL to listOf(ZWJ, "\u0DCA" + ZWJ + "\u0DBB")
    )

    fun isConsonant(value: String): Boolean =
        value.length == 1 && value[0] in '\u0D9A'..'\u0DC6' && value[0] != '\u0DCA'

    fun isIndependentVowel(value: String): Boolean =
        value.length == 1 && value[0] in '\u0D85'..'\u0D96'

    fun isSign(value: String): Boolean =
        value.isNotEmpty() && value.all { it in '\u0DCA'..'\u0DDF' || it == '\u200D' }

    /** Composer state: the syllable being built plus the pending kombuva flag. */
    data class State(val composing: String = "", val pendingPrebase: Boolean = false)

    data class Output(val state: State, val commit: String? = null)

    /**
     * Feeds one key value into the composer. Returns the new state and, when a
     * syllable is complete, the text that should be committed.
     */
    fun input(state: State, value: String): Output {
        // The kombuva key is typed before the consonant but encoded after it.
        if (value == KOMBUVA) {
            return Output(State("", pendingPrebase = true), state.composing.ifEmpty { null })
        }

        if (isConsonant(value)) {
            val glyph = if (state.pendingPrebase) value + KOMBUVA else value
            val continues = state.composing.isEmpty() || state.composing.endsWith(HAL)
            return if (continues) {
                Output(State(state.composing + glyph, pendingPrebase = false))
            } else {
                Output(State(glyph, pendingPrebase = false), state.composing)
            }
        }

        if (isIndependentVowel(value)) {
            val commit = state.composing.ifEmpty { null }
            return Output(State(value, pendingPrebase = false), commit)
        }

        if (value == HAL) {
            return Output(State(state.composing + HAL, pendingPrebase = false))
        }

        // Any other glyph (vowel sign, anusvara, punctuation) closes the vowel slot.
        val composing = state.composing
        return when {
            composing.endsWith(KOMBUVA) && value == AELA ->
                Output(State(composing.dropLast(1) + KOMBUVA_AELA, pendingPrebase = false))
            composing.endsWith(KOMBUVA_AELA) && value == AELA ->
                Output(State(composing.dropLast(1) + KOMBUVA_DIGA, pendingPrebase = false))
            state.pendingPrebase ->
                Output(State(composing + KOMBUVA + value, pendingPrebase = false))
            else -> Output(State(composing + value, pendingPrebase = false))
        }
    }

    fun finish(state: State): String = state.composing
}
