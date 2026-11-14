package com.nova.keyboard

import com.nova.keyboard.ime.EditorProfile
import com.nova.keyboard.ime.EditorTypeDetector
import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Password detection is the one place where a bug leaks keystrokes, so it is
 * pinned here. `TYPE_MASK_VARIATION` only has meaning inside its input class:
 * variation `0x10` is TYPE_TEXT_VARIATION_PASSWORD for text fields and
 * TYPE_NUMBER_VARIATION_PASSWORD for number fields.
 */
class EditorTypeDetectorTest {

    private fun profile(inputType: Int): EditorProfile {
        val info = EditorInfo()
        info.inputType = inputType
        return EditorTypeDetector.detect(info)
    }

    private fun text(variation: Int) = InputType.TYPE_CLASS_TEXT or variation
    private fun number(variation: Int) = InputType.TYPE_CLASS_NUMBER or variation

    @Test
    fun `text password variations are sensitive`() {
        assertTrue(profile(text(InputType.TYPE_TEXT_VARIATION_PASSWORD)).isSensitive)
        assertTrue(profile(text(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)).isSensitive)
        assertTrue(profile(text(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)).isSensitive)
    }

    @Test
    fun `numeric password is sensitive`() {
        assertTrue(profile(number(InputType.TYPE_NUMBER_VARIATION_PASSWORD)).isSensitive)
    }

    @Test
    fun `a plain text field is not sensitive`() {
        assertFalse(profile(text(InputType.TYPE_TEXT_VARIATION_NORMAL)).isSensitive)
        assertFalse(profile(InputType.TYPE_CLASS_TEXT).isSensitive)
    }

    @Test
    fun `email url and number fields are classified`() {
        assertTrue(profile(text(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)).isEmail)
        assertTrue(profile(text(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)).isEmail)
        assertTrue(profile(text(InputType.TYPE_TEXT_VARIATION_URI)).isUrl)
        assertTrue(profile(InputType.TYPE_CLASS_NUMBER).isNumber)
        assertTrue(profile(InputType.TYPE_CLASS_PHONE).isPhone)
    }

    @Test
    fun `multiline flag is detected`() {
        assertTrue(
            profile(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE).isMultiline
        )
    }

    @Test
    fun `null editor info is safe`() {
        val p = EditorTypeDetector.detect(null)
        assertFalse(p.isSensitive)
        assertFalse(p.isEmail)
    }
}
