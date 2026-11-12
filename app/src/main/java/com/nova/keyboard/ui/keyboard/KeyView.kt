package com.nova.keyboard.ui.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.nova.keyboard.theme.NovaTheme

/**
 * A single key. Drawn manually rather than composed from XML so that a full
 * layout inflates in one measure pass with no view hierarchy overhead.
 */
class KeyView(context: Context) : View(context) {

    var spec: KeySpec = KeySpec("")
        set(value) {
            field = value
            contentDescription = value.label
            invalidate()
        }

    var theme: NovaTheme? = null
        set(value) {
            field = value
            applyTheme()
            invalidate()
        }

    var onKey: ((KeySpec) -> Unit)? = null
    var onLongPressKey: ((KeySpec) -> Unit)? = null
    var hapticsEnabled: Boolean = true
    var previewEnabled: Boolean = true
    var soundEnabled: Boolean = true
    var hapticConstant: Int = HapticFeedbackConstants.KEYBOARD_TAP

    private var pressed = false
    private var longPressFired = false
    private var downAt = 0L

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()
    private var radiusPx = 0f
    private var borderPx = 0f
    private var glowAlpha = 0

    private val repeatRunnable = object : Runnable {
        override fun run() {
            onKey?.invoke(spec)
            if (pressed && spec.code == KeyCode.BACKSPACE) postDelayed(this, 55L)
        }
    }

    private val longPressRunnable = Runnable {
        longPressFired = true
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPressKey?.invoke(spec)
    }

    private fun applyTheme() {
        val t = theme ?: return
        radiusPx = t.radiusDp * resources.displayMetrics.density
        borderPx = t.borderWidthDp * resources.displayMetrics.density
        stroke.color = t.keyBorder
        stroke.strokeWidth = borderPx
        label.color = when (spec.style) {
            KeyStyle.ACCENT -> t.accent
            KeyStyle.SPECIAL -> t.specialText
            KeyStyle.DANGER -> 0xFFFF6B6B.toInt()
            else -> t.textColor
        }
        label.typeface = if (t.font == "mono") Typeface.MONOSPACE else Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val len = spec.label.length
        val factor = when {
            len <= 1 -> 0.44f
            len == 2 -> 0.38f
            len == 3 -> 0.30f
            else -> 0.24f
        }
        label.textSize = h * factor
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        val t = theme ?: return
        val pressedColor = t.keyPressed
        val baseColor = when (spec.style) {
            KeyStyle.SPECIAL -> t.specialKey
            KeyStyle.ACCENT -> blend(t.keyBackground, t.accent, 0.22f)
            KeyStyle.DANGER -> blend(t.keyBackground, 0xFFFF3B3B.toInt(), 0.25f)
            else -> t.keyBackground
        }
        val alpha = if (spec.style == KeyStyle.SPECIAL || spec.style == KeyStyle.ACCENT) 255
        else (t.keyAlpha * 255).toInt().coerceIn(20, 255)

        if (t.glow && spec.style == KeyStyle.ACCENT) {
            glowAlpha = 40
            fill.color = t.accent
            fill.alpha = glowAlpha
            canvas.drawRoundRect(rect, radiusPx, radiusPx, fill)
            fill.alpha = 255
        }

        fill.color = if (pressed) pressedColor else baseColor
        fill.alpha = alpha
        canvas.drawRoundRect(rect, radiusPx, radiusPx, fill)
        fill.alpha = 255
        if (borderPx > 0f) canvas.drawRoundRect(rect, radiusPx, radiusPx, stroke)

        val text = spec.label
        if (text.isNotEmpty()) {
            val metrics = label.fontMetrics
            val baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(text, rect.centerX(), baseline, label)
        }
    }

    private fun blend(base: Int, overlay: Int, ratio: Float): Int {
        fun ch(shift: Int, color: Int) = (color shr shift) and 0xFF
        val r = (ch(16, base) * (1 - ratio) + ch(16, overlay) * ratio).toInt()
        val g = (ch(8, base) * (1 - ratio) + ch(8, overlay) * ratio).toInt()
        val b = (ch(0, base) * (1 - ratio) + ch(0, overlay) * ratio).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressed = true
                longPressFired = false
                downAt = System.currentTimeMillis()
                invalidate()
                if (hapticsEnabled) performHapticFeedback(hapticConstant)
                if (soundEnabled) {
                    runCatching { playSoundEffect(android.view.SoundEffectConstants.CLICK) }
                }
                if (spec.alternates.isNotEmpty()) postDelayed(longPressRunnable, 380)
                if (spec.code == KeyCode.BACKSPACE) postDelayed(repeatRunnable, 420L)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressed && (event.x < 0 || event.x > width || event.y < 0 || event.y > height)) {
                    pressed = false
                    removeCallbacks(longPressRunnable)
                    removeCallbacks(repeatRunnable)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(longPressRunnable)
                removeCallbacks(repeatRunnable)
                val wasPressed = pressed
                val heldLong = System.currentTimeMillis() - downAt > 420L
                pressed = false
                invalidate()
                // Backspace already repeated while held; every other key fires once.
                if (wasPressed && !longPressFired && !(heldLong && spec.code == KeyCode.BACKSPACE)) {
                    onKey?.invoke(spec)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                removeCallbacks(repeatRunnable)
                pressed = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPressRunnable)
        removeCallbacks(repeatRunnable)
        super.onDetachedFromWindow()
    }
}
