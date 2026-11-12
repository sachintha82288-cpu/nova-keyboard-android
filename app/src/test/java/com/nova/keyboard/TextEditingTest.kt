package com.nova.keyboard

import com.nova.keyboard.ime.TextEditing
import org.junit.Assert.assertEquals
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
    fun `apostrophe inside a word is kept`() {
        assertEquals("don't", TextEditing.trailingWord("I don't"))
    }

    @Test
    fun `backspace keeps surrogate pairs whole`() {
        // Regression: one backspace used to delete half an emoji.
        assertEquals(2, TextEditing.backspaceLength("\uD83D\uDE42", 1))
        assertEquals(1, TextEditing.backspaceLength("a", 1))
        assertEquals(0, TextEditing.backspaceLength("", 1))
    }

    @Test
    fun `backspace never asks for more than the window holds`() {
        assertEquals(2, TextEditing.backspaceLength("ab", 5))
    }

    @Test
    fun `sinhala letters count as word characters`() {
        assertEquals("\u0D9A\u0DDC\u0DC4\u0DDC\u0DB8", TextEditing.trailingWord("x \u0D9A\u0DDC\u0DC4\u0DDC\u0DB8"))
    }
}
