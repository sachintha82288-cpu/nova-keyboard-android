package com.nova.keyboard

import com.nova.keyboard.language.WijesekaraEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WijesekaraComposerTest {

    private fun type(keys: List<String>): String {
        var state = WijesekaraEngine.State()
        val out = StringBuilder()
        keys.forEach { key ->
            val result = WijesekaraEngine.input(state, key)
            result.commit?.let { out.append(it) }
            state = result.state
        }
        out.append(state.composing)
        return out.toString()
    }

    private val KA = "\u0D9A"
    private val KOMBUVA = "\u0DD9"
    private val AELA = "\u0DCF"
    private val I = "\u0DD2"
    private val HAL = "\u0DCA"

    @Test
    fun `consonant then sign`() {
        assertEquals(KA + I, type(listOf(KA, I)))
    }

    @Test
    fun `kombuva typed first lands after the consonant`() {
        assertEquals(KA + KOMBUVA, type(listOf(KOMBUVA, KA)))
    }

    @Test
    fun `kombuva plus aela becomes ko`() {
        assertEquals(KA + "\u0DDC", type(listOf(KOMBUVA, KA, AELA)))
    }

    @Test
    fun `second aela becomes koo`() {
        assertEquals(KA + "\u0DDD", type(listOf(KOMBUVA, KA, AELA, AELA)))
    }

    @Test
    fun `hal allows conjuncts`() {
        assertEquals(KA + HAL + KA, type(listOf(KA, HAL, KA)))
    }

    @Test
    fun `new consonant commits the previous syllable`() {
        var state = WijesekaraEngine.State()
        val first = WijesekaraEngine.input(state, KA)
        state = first.state
        val second = WijesekaraEngine.input(state, "\u0DB8")
        assertEquals(KA, second.commit)
        assertEquals("\u0DB8", second.state.composing)
    }

    @Test
    fun `base layer maps the standard wijesekara letters`() {
        assertEquals("\u0D85", WijesekaraEngine.base["w"])
        assertEquals("\u0DAF", WijesekaraEngine.base["o"])
        assertEquals("\u0D9A", WijesekaraEngine.base["l"])
        assertEquals("\u0DC1", WijesekaraEngine.shifted["y"])
        assertTrue(WijesekaraEngine.longPress.containsKey("\u0DAF"))
    }

    @Test
    fun `consonant detection covers the sinhala block`() {
        assertTrue(WijesekaraEngine.isConsonant("\u0D9A"))
        assertTrue(!WijesekaraEngine.isConsonant(HAL))
        assertTrue(WijesekaraEngine.isIndependentVowel("\u0D85"))
    }
}
