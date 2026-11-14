package com.nova.keyboard.ime

/**
 * Pure text-editing helpers extracted from the IME so they can be unit tested
 * without an InputConnection. Both exist because of real bugs:
 *
 *  - a word that does not end at the cursor must never be treated as "the word
 *    before the cursor", or autocorrect deletes the wrong characters;
 *  - backspace must not split a surrogate pair, or an emoji becomes garbage.
 */
object TextEditing {

    /**
     * Letters, apostrophes, and combining marks. The marks matter: a Sinhala
     * vowel sign such as \u0DDC is a spacing combining mark, not a letter, so
     * `isLetter()` alone cut \u0D9A\u0DDC\u0DC4\u0DDC\u0DB8 down to \u0DB8.
     */
    fun isWordChar(c: Char): Boolean {
        if (c.isLetter() || c == '\'' || c == '\u2019') return true
        return when (Character.getType(c)) {
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt() -> true
            else -> false
        }
    }

    /** The word ending exactly at the cursor, or "" when the cursor is not after a word. */
    fun trailingWord(before: String): String {
        if (before.isEmpty()) return ""
        if (!isWordChar(before.last())) return ""
        return before.takeLastWhile { isWordChar(it) }
    }

    /** How many characters one backspace should remove, keeping surrogate pairs whole. */
    fun backspaceLength(window: String, requested: Int = 1): Int {
        var count = minOf(requested, window.length)
        if (count == 1 && window.length >= 2 &&
            Character.isLowSurrogate(window[window.length - 1]) &&
            Character.isHighSurrogate(window[window.length - 2])
        ) {
            count = 2
        }
        return count
    }
}
