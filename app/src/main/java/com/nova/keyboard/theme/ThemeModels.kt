package com.nova.keyboard.theme

import kotlinx.serialization.Serializable

/**
 * A complete, self-contained keyboard theme. Colours are ARGB ints so the model
 * is usable from both the View-based keyboard and the Compose settings UI.
 */
@Serializable
data class NovaTheme(
    val id: String,
    val name: String,
    val background: Int,
    val keyBackground: Int,
    val keyPressed: Int,
    val keyBorder: Int,
    val textColor: Int,
    val accent: Int,
    val specialKey: Int,
    val specialText: Int,
    val suggestionBar: Int,
    val suggestionText: Int,
    val radiusDp: Int = 14,
    val borderWidthDp: Int = 0,
    val keyAlpha: Float = 1f,
    val glow: Boolean = false,
    val shadow: Boolean = false,
    val font: String = "sans",
    val builtIn: Boolean = false
)
