package com.nova.keyboard.ui.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.nova.keyboard.theme.NovaTheme

enum class ToolbarAction {
    SETTINGS, AI, CLIPBOARD, SNIPPETS, EMOJI, GAMING, DEV, ONE_HANDED, HIDE, CURSOR_LEFT, CURSOR_RIGHT
}

/**
 * The IME's whole surface: toolbar, suggestion strip, key grid and the
 * emoji / clipboard / snippet / AI panels that swap in over the key grid.
 */
class KeyboardRootView(context: Context) : LinearLayout(context) {

    var onKey: ((KeySpec) -> Unit)? = null
    var onLongPressKey: ((KeySpec) -> Unit)? = null
    var onCandidate: ((String) -> Unit)? = null
    var onToolbar: ((ToolbarAction) -> Unit)? = null

    private val density = resources.displayMetrics.density

    private val toolbarRow = LinearLayout(context)
    private val toolbarScroll = HorizontalScrollView(context)
    private val suggestionScroll = HorizontalScrollView(context)
    private val suggestionRow = LinearLayout(context)
    private val keyContainer = FrameLayout(context)
    private val keyRows = LinearLayout(context)

    private var theme: NovaTheme = com.nova.keyboard.theme.BuiltInThemes.amoledCyber
    private var keyHeightPx = dp(52)
    private var spacingPx = dp(3)
    private var panelView: View? = null
    private var languageLabel = "EN"
    private var modeLabel = ""

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.background)

        toolbarRow.orientation = HORIZONTAL
        toolbarRow.gravity = Gravity.CENTER_VERTICAL
        toolbarRow.setPadding(dp(4), 0, dp(4), 0)
        // Scrollable so the 11 toolbar actions never clip on narrow screens.
        toolbarScroll.isHorizontalScrollBarEnabled = false
        toolbarScroll.addView(
            toolbarRow,
            FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        )
        addView(toolbarScroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(38)))

        suggestionRow.orientation = HORIZONTAL
        suggestionRow.gravity = Gravity.CENTER_VERTICAL
        suggestionScroll.addView(suggestionRow, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        suggestionScroll.isHorizontalScrollBarEnabled = false
        addView(suggestionScroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(40)))

        keyRows.orientation = VERTICAL
        keyContainer.addView(keyRows, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(keyContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        buildToolbar()
        restyle()
    }

    private fun dp(value: Int): Int = (value * density).toInt()
    private fun dp(value: Float): Float = value * density

    fun applyTheme(newTheme: NovaTheme) {
        theme = newTheme
        restyle()
        rebuildKeysIfPresent()
    }

    private var lastRows: List<List<KeySpec>> = emptyList()

    fun setLayout(rows: List<List<KeySpec>>, animate: Boolean = false) {
        lastRows = rows
        hidePanel()
        keyRows.removeAllViews()
        rows.forEach { row ->
            val rowView = LinearLayout(context)
            rowView.orientation = HORIZONTAL
            rowView.gravity = Gravity.CENTER_VERTICAL
            row.forEach { spec ->
                val keyView = KeyView(context)
                keyView.spec = spec
                keyView.theme = theme
                keyView.hapticsEnabled = hapticsEnabled
                keyView.soundEnabled = soundEnabled
                keyView.hapticConstant = hapticConstant
                keyView.onKey = { onKey?.invoke(it) }
                keyView.onLongPressKey = { onLongPressKey?.invoke(it) }
                val lp = LayoutParams(0, keyHeightPx, spec.weight)
                lp.setMargins(spacingPx, spacingPx, spacingPx, spacingPx)
                rowView.addView(keyView, lp)
            }
            keyRows.addView(rowView, LayoutParams(LayoutParams.MATCH_PARENT, keyHeightPx + spacingPx * 2))
        }
    }

    private fun rebuildKeysIfPresent() {
        if (lastRows.isNotEmpty()) setLayout(lastRows)
    }

    var hapticsEnabled: Boolean = true
    var soundEnabled: Boolean = true
    var hapticConstant: Int = android.view.HapticFeedbackConstants.KEYBOARD_TAP

    fun setFeedback(haptics: Boolean, sound: Boolean, constant: Int) {
        hapticsEnabled = haptics
        soundEnabled = sound
        hapticConstant = constant
    }

    fun setSizes(heightScale: Float, spacingDp: Int) {
        keyHeightPx = dp((52 * heightScale).toInt().coerceIn(36, 84))
        spacingPx = dp(spacingDp)
    }

    fun setLabels(language: String, mode: String) {
        languageLabel = language
        modeLabel = mode
        buildToolbar()
    }

    fun showSuggestions(items: List<String>, composing: String? = null) {
        suggestionRow.removeAllViews()
        if (composing != null) {
            val chip = TextView(context)
            chip.text = composing
            chip.setTextColor(theme.accent)
            chip.textSize = 15f
            chip.typeface = Typeface.DEFAULT_BOLD
            chip.setPadding(dp(10), 0, dp(10), 0)
            suggestionRow.addView(chip)
        }
        items.forEach { item ->
            val chip = TextView(context)
            chip.text = item
            chip.setTextColor(theme.suggestionText)
            chip.textSize = 16f
            chip.maxLines = 1
            chip.setPadding(dp(12), 0, dp(12), 0)
            chip.isClickable = true
            chip.setOnClickListener { onCandidate?.invoke(item) }
            suggestionRow.addView(chip)
        }
        suggestionScroll.visibility = View.VISIBLE
    }

    fun clearSuggestions() {
        suggestionRow.removeAllViews()
    }

    fun showPanel(panel: View) {
        hidePanel()
        panelView = panel
        keyRows.visibility = View.GONE
        keyContainer.addView(panel, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun hidePanel() {
        panelView?.let { keyContainer.removeView(it) }
        panelView = null
        keyRows.visibility = View.VISIBLE
    }

    fun isPanelOpen(): Boolean = panelView != null

    private fun buildToolbar() {
        toolbarRow.removeAllViews()
        toolbarScroll.setBackgroundColor(theme.suggestionBar)

        val badge = TextView(context)
        badge.text = if (modeLabel.isEmpty()) languageLabel else languageLabel + " · " + modeLabel
        badge.setTextColor(theme.accent)
        badge.textSize = 11f
        badge.typeface = Typeface.DEFAULT_BOLD
        badge.setPadding(dp(8), 0, dp(10), 0)
        toolbarRow.addView(badge)

        data class Action(val label: String, val action: ToolbarAction)
        val actions = listOf(
            Action("\u2190", ToolbarAction.CURSOR_LEFT),
            Action("\u2192", ToolbarAction.CURSOR_RIGHT),
            Action("AI", ToolbarAction.AI),
            Action("\u2630", ToolbarAction.CLIPBOARD),
            Action("{ }", ToolbarAction.SNIPPETS),
            Action("\uD83D\uDE00", ToolbarAction.EMOJI),
            Action("\uD83C\uDFAE", ToolbarAction.GAMING),
            Action("DEV", ToolbarAction.DEV),
            Action("1H", ToolbarAction.ONE_HANDED),
            Action("\u2699", ToolbarAction.SETTINGS),
            Action("\u25BE", ToolbarAction.HIDE)
        )
        actions.forEach { item ->
            val button = TextView(context)
            button.text = item.label
            button.setTextColor(theme.specialText)
            button.textSize = 13f
            button.gravity = Gravity.CENTER
            button.setPadding(dp(9), 0, dp(9), 0)
            button.isClickable = true
            button.setOnClickListener { onToolbar?.invoke(item.action) }
            toolbarRow.addView(button, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        }
    }

    private fun restyle() {
        setBackgroundColor(theme.background)
        toolbarRow.setBackgroundColor(theme.suggestionBar)
        toolbarScroll.setBackgroundColor(theme.suggestionBar)
        suggestionScroll.setBackgroundColor(theme.suggestionBar)
        keyContainer.setBackgroundColor(theme.background)
        buildToolbar()
        for (i in 0 until suggestionRow.childCount) {
            (suggestionRow.getChildAt(i) as? TextView)?.setTextColor(theme.suggestionText)
        }
    }

    fun colorWithAlpha(color: Int, alpha: Int): Int = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
