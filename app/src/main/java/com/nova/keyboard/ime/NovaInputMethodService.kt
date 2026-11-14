package com.nova.keyboard.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.nova.keyboard.core.CrashReporter
import com.nova.keyboard.core.ServiceLocator
import com.nova.keyboard.ui.keyboard.KeyboardRootView

/**
 * The real IME. Registered in AndroidManifest.xml with BIND_INPUT_METHOD and
 * declared through res/xml/method.xml so Android lists it in keyboard settings.
 */
class NovaInputMethodService : InputMethodService(), InputSession.Host {

    private var rootView: KeyboardRootView? = null
    private var controller: KeyboardController? = null

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }

    override fun onCreateInputView(): View {
        ServiceLocator.ensureInitialised(this)
        return try {
            val root = KeyboardRootView(this)
            rootView = root
            controller = KeyboardController(this, this, root)
            root
        } catch (t: Throwable) {
            // A keyboard that throws here takes the app being typed into down
            // with it, so degrade to a visible message instead.
            CrashReporter.record(this, t)
            fallbackView(t)
        }
    }

    private fun fallbackView(t: Throwable): View = android.widget.TextView(this).apply {
        text = "NOVA keyboard could not start.\n" +
            t.javaClass.simpleName + ": " + (t.message ?: "") +
            "\n\nOpen NOVA KEYBOARD \u2192 About to see the crash log."
        setTextColor(0xFFFF6B6B.toInt())
        textSize = 13f
        setPadding(24, 24, 24, 24)
        setBackgroundColor(0xFF05060A.toInt())
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        try {
            super.onStartInputView(info, restarting)
            controller?.onStartInput(info)
        } catch (t: Throwable) {
            CrashReporter.record(this, t)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        try {
            controller?.onFinishInput()
            super.onFinishInputView(finishingInput)
        } catch (t: Throwable) {
            CrashReporter.record(this, t)
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onWindowShown() {
        try {
            super.onWindowShown()
            // Not onStartInput: re-running the full reset on every window show wiped
            // the in-progress Sinhala syllable and re-read the clipboard.
            controller?.onWindowShown()
        } catch (t: Throwable) {
            CrashReporter.record(this, t)
        }
    }

    override fun onDestroy() {
        // Drop the controller's reference to the view so the controller, key grid
        // and panels can be collected when the IME window goes away.
        rootView = null
        controller = null
        super.onDestroy()
    }

    // InputSession.Host
    override val inputConnection: InputConnection?
        get() = currentInputConnection

    override val inputMethodManager: InputMethodManager?
        get() = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

    override fun appContext(): Context = this

    override fun finishInputSafely() {
        requestHideSelf(0)
    }
}
