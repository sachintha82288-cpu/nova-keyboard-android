package com.nova.keyboard

import com.nova.keyboard.language.SuggestionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionEngineTest {

    @Test
    fun `identical strings have zero distance`() {
        assertEquals(0, SuggestionEngine.editDistance("hello", "hello"))
    }

    @Test
    fun `single edit is detected`() {
        assertEquals(1, SuggestionEngine.editDistance("helo", "hello"))
        assertEquals(1, SuggestionEngine.editDistance("hello", "hallo"))
    }

    @Test
    fun `distance is bounded`() {
        assertTrue(SuggestionEngine.editDistance("cat", "elephant", 2) > 2)
    }

    @Test
    fun `insertions and deletions`() {
        assertEquals(3, SuggestionEngine.editDistance("kitten", "sitting", 5))
    }
}
