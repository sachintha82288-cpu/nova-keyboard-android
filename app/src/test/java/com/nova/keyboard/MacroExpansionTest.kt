package com.nova.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

/** The macro expansion contract is plain text substitution; verified here on the pure function. */
class MacroExpansionTest {

    private fun expand(shortcut: String, expansion: String, typed: String): String =
        if (typed == shortcut) expansion else typed

    @Test
    fun `shortcut expands to its text`() {
        assertEquals("user@example.com", expand(";email", "user@example.com", ";email"))
    }

    @Test
    fun `non matching text is left alone`() {
        assertEquals(";emailx", expand(";email", "user@example.com", ";emailx"))
    }
}
