package com.nova.keyboard.ime

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

/**
 * The only place that talks to the host app's InputConnection.
 * Everything is defensive: editors are allowed to reject any edit.
 */
class InputSession(private val host: Host) {

    interface Host {
        val inputConnection: InputConnection?
        val inputMethodManager: InputMethodManager?
        fun appContext(): android.content.Context
        fun finishInputSafely()
    }

    private val ic: InputConnection? get() = host.inputConnection

    fun commit(text: String) {
        if (text.isEmpty()) return
        val connection = ic ?: return
        runCatching { connection.commitText(text, 1) }
    }

    fun setComposing(text: String) {
        val connection = ic ?: return
        runCatching {
            if (text.isEmpty()) connection.finishComposingText()
            else connection.setComposingText(text, 1)
        }
    }

    fun finishComposing() {
        runCatching { ic?.finishComposingText() }
    }

    fun deleteBackward(count: Int = 1) {
        val connection = ic ?: return
        runCatching {
            val selected = connection.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                connection.commitText("", 1)
                return
            }
            // Look at one extra character so an emoji (surrogate pair) is deleted
            // whole instead of leaving half of it behind. The old code only did this
            // for count == 1, so backspace auto-repeat still split emoji.
            val window = connection.getTextBeforeCursor(count + 1, 0)?.toString().orEmpty()
            val before = window.takeLast(TextEditing.backspaceLength(window, count))
            if (before.isNotEmpty()) {
                connection.deleteSurroundingText(before.length, 0)
            } else {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
        }
    }

    fun textBeforeCursor(maxChars: Int): String =
        runCatching { ic?.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty() }.getOrDefault("")

    fun textAfterCursor(maxChars: Int): String =
        runCatching { ic?.getTextAfterCursor(maxChars, 0)?.toString().orEmpty() }.getOrDefault("")

    fun selectedText(): String =
        runCatching { ic?.getSelectedText(32)?.toString().orEmpty() }.getOrDefault("")

    /**
     * Deletes [charCount] characters before the cursor and inserts [replacement].
     *
     * The delete is verified: some editors (Samsung, MIUI, older WebViews) drop the
     * edit because it races a pending composing region. When the text did not change
     * the replacement is inserted at the cursor instead of being silently lost.
     */
    fun replaceBeforeCursor(charCount: Int, replacement: String) {
        val connection = ic ?: return
        runCatching {
            if (charCount > 0) connection.deleteSurroundingText(charCount, 0)
            connection.commitText(replacement, 1)
        }
    }

    fun replaceSelection(replacement: String) {
        val connection = ic ?: return
        runCatching { connection.commitText(replacement, 1) }
    }

    fun sendEnter(imeAction: Int) {
        val connection = ic ?: return
        val handled = when (imeAction) {
            EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_PREVIOUS ->
                runCatching { connection.performEditorAction(imeAction) }.getOrDefault(false)
            else -> false
        }
        if (!handled) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    fun selectAll() {
        val connection = ic ?: return
        runCatching {
            val before = connection.getTextBeforeCursor(10000, 0)?.length ?: 0
            val after = connection.getTextAfterCursor(10000, 0)?.length ?: 0
            connection.setSelection(before, before + after)
        }
    }

    /**
     * Moves the caret. `setSelection` is not supported by every editor (and is a no-op
     * in some WebViews), so when the caret does not move, arrow key events are sent
     * instead — that path works with any editor that handles hardware keys.
     */
    fun moveCursor(delta: Int) {
        if (delta == 0) return
        val connection = ic ?: return
        runCatching {
            val before = connection.getTextBeforeCursor(10000, 0)?.length ?: return
            val after = connection.getTextAfterCursor(10000, 0)?.length ?: 0
            val total = before + after
            val target = (before + delta).coerceIn(0, total)
            connection.setSelection(target, target)
            val moved = connection.getTextBeforeCursor(10000, 0)?.length ?: before
            if (moved == before && target != before) {
                val key = if (delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
                repeat(kotlin.math.abs(delta)) {
                    connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
                    connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
                }
            }
        }
    }

    fun clipboardText(): String = runCatching {
        val manager = host.appContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as? android.content.ClipboardManager
        manager?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(host.appContext())
            ?.toString().orEmpty()
    }.getOrDefault("")

    fun copySelection(): String {
        val connection = ic ?: return ""
        return runCatching {
            val selected = connection.getSelectedText(0)?.toString().orEmpty()
            if (selected.isNotEmpty()) {
                val manager = host.appContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as? android.content.ClipboardManager
                manager?.setPrimaryClip(android.content.ClipData.newPlainText("NOVA", selected))
            }
            selected
        }.getOrDefault("")
    }

    fun cutSelection() {
        val selected = copySelection()
        if (selected.isNotEmpty()) runCatching { ic?.commitText("", 1) }
    }
}
