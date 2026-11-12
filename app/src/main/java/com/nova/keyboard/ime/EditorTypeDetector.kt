package com.nova.keyboard.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo

/** What kind of field the keyboard is currently typing into. */
data class EditorProfile(
    val isPassword: Boolean = false,
    val isEmail: Boolean = false,
    val isUrl: Boolean = false,
    val isNumber: Boolean = false,
    val isPhone: Boolean = false,
    val isMultiline: Boolean = false,
    val isAutoComplete: Boolean = false,
    val imeAction: Int = EditorInfo.IME_ACTION_UNSPECIFIED,
    val fieldId: Int = 0
) {
    /** Sensitive fields get no prediction, no learning, no AI, no clipboard capture. */
    val isSensitive: Boolean get() = isPassword
}

object EditorTypeDetector {

    fun detect(info: EditorInfo?): EditorProfile {
        if (info == null) return EditorProfile()
        val type = info.inputType
        val cls = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION

        val password = when (cls) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }

        val email = cls == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            )

        val url = cls == InputType.TYPE_CLASS_TEXT &&
            variation == InputType.TYPE_TEXT_VARIATION_URI

        val number = cls == InputType.TYPE_CLASS_NUMBER ||
            cls == InputType.TYPE_CLASS_DATETIME

        val phone = cls == InputType.TYPE_CLASS_PHONE

        val multiline = (type and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0 ||
            (type and InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE) != 0

        return EditorProfile(
            isPassword = password,
            isEmail = email,
            isUrl = url,
            isNumber = number,
            isPhone = phone,
            isMultiline = multiline,
            isAutoComplete = (type and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0,
            imeAction = info.imeOptions and EditorInfo.IME_MASK_ACTION,
            fieldId = info.fieldId
        )
    }
}
