package com.nova.keyboard

import com.nova.keyboard.ime.TextEditing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextEditingTest {

    @Test
    fun `word ending at the cursor is returned`() {
        assertEquals("hello", TextEditing.trailingWord("say hello"))
        assertEquals("dont'", TextEditing.trailingWord("I dont'"))
    }

    @Test
    fun `cursor not after a word returns empty`() {
        // Regression: "hello," used to yield "hello", so autocorrect deleted the comma too.
        assertEquals("", TextEditing.trailingWord("hello,"))
        assertEquals("", TextEditing.trailingWord("hello "))
        assertEquals("", TextEditing.trailingWord(""))
    }

    @Test
    fun `apostrophes stay inside the word`() {
        assertEquals("don't", TextEditing.trailingWord("I don't"))
    }

    @Test
    fun `emoji backspace keeps the surrogate pair together`() {
        assertEquals(2, TextEditing.backspaceLength("\uD83D\uDE42", 1))
        assertEquals(1, TextEditing.backspaceLength("a", 1))
        assertEquals(0, TextEditing.backspaceLength("", 1))
    }

    @Test
    fun `backspace is clamped to the window`() {
        assertEquals(2, TextEditing.backspaceLength("ab", 5))
    }

    @Test
    fun `sinhala vowel signs count as word characters`() {
        // Regression: isLetter() alone cut a word at every vowel sign.
        assertEquals(
            "\u0D9A\u0DDC\u0DC4\u0DDC\u0DB8",
            TextEditing.trailingWord("x \u0D9A\u0DDC\u0DC4\u0DDC\u0DB8")
        )
    }

    @Test
    fun `emoji backspace works for repeat too`() {
        // Regression: the surrogate check used to run only when count == 1, so
        // backspace auto-repeat still split emoji.
        val window = "a\uD83D\uDE42\uD83D\uDE00"
        assertTrue(TextEditing.backspaceLength(window, 2) >= 2)
        assertFalse(TextEditing.backspaceLength(window, 2) == 1)
    }
}
