package com.nova.keyboard.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
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
        ServiceLocator.init(this)
        val root = KeyboardRootView(this)
        rootView = root
        controller = KeyboardController(this, this, root)
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        controller?.onStartInput(info)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        controller?.onFinishInput()
        super.onFinishInputView(finishingInput)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onWindowShown() {
        super.onWindowShown()
        controller?.onStartInput(currentInputEditorInfo)
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
