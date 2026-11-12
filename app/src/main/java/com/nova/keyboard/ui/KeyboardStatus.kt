package com.nova.keyboard.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

object KeyboardStatus {

    /** True only when Android itself lists NOVA among enabled input methods. */
    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_INPUT_METHODS
        ) ?: return false
        return enabled.split(':').any { it.startsWith(context.packageName + "/") }
    }

    fun isSelected(context: Context): Boolean {
        val current = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: return false
        return current.startsWith(context.packageName + "/")
    }

    fun openKeyboardSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun showPicker(context: Context) {
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        manager?.showInputMethodPicker()
    }
}
