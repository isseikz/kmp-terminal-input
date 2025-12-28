package tokyo.isseikuzumaki.kmpinput

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * A container view that handles terminal keyboard input.
 *
 * TerminalView wraps child views and handles keyboard input when tapped.
 * It has no intrinsic size - its size is determined by its children or layout params.
 *
 * Usage:
 * ```xml
 * <TerminalView
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <!-- Your terminal display view here -->
 *     <TextView android:id="@+id/terminal_output" ... />
 *
 * </TerminalView>
 * ```
 *
 * When tapped anywhere within its bounds (including over child views),
 * the software keyboard will appear and input will be sent to the handler.
 */
class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val inputCore = TerminalInputCore()
    val handler: TerminalInputHandler get() = inputCore
    private var scope: CoroutineScope? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = CoroutineScope(Dispatchers.Main + Job())
        scope?.launch {
            inputCore.uiState
                .map { it.inputMode }
                .distinctUntilChanged()
                .collect { _ ->
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.restartInput(this@TerminalView)
                }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope?.cancel()
        scope = null
    }

    fun setInputMode(mode: InputMode) {
        if (inputCore.uiState.value.inputMode != mode) {
            inputCore.setInputMode(mode)
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI

        val isTextMode = inputCore.uiState.value.inputMode == InputMode.TEXT

        when (inputCore.uiState.value.inputMode) {
            InputMode.RAW -> {
                outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            InputMode.TEXT -> {
                outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                        InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
        }

        return TerminalInputConnection(this, inputCore.dispatcher, isTextMode)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // Intercept touch events to handle focus and keyboard
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP) {
            requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    /**
     * Programmatically show the software keyboard.
     */
    fun showKeyboard() {
        requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Programmatically hide the software keyboard.
     */
    fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyDown(keyCode, event)

        val virtualKey = mapAndroidKeyCodeToVirtualKey(keyCode)
        if (virtualKey != null) {
            val modifiers = mutableSetOf<Modifier>()
            if (event.isCtrlPressed) modifiers.add(Modifier.CTRL)
            if (event.isAltPressed) modifiers.add(Modifier.ALT)
            if (event.isShiftPressed) modifiers.add(Modifier.SHIFT)
            if (event.isMetaPressed) modifiers.add(Modifier.META)

            // Special handling for Ctrl+Key
            if (event.isCtrlPressed && keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) {
                val baseChar = 'A' + (keyCode - KeyEvent.KEYCODE_A)
                inputCore.dispatcher.sendControlKey(baseChar)
                return true
            }

            inputCore.injectKey(virtualKey, modifiers)
            return true
        }

        // Handle regular character input via onKeyDown if not handled by IME

        return super.onKeyDown(keyCode, event)
    }

    private fun mapAndroidKeyCodeToVirtualKey(keyCode: Int): VirtualKey? {
        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> VirtualKey.ENTER
            KeyEvent.KEYCODE_ESCAPE -> VirtualKey.ESCAPE
            KeyEvent.KEYCODE_DEL -> VirtualKey.BACKSPACE
            KeyEvent.KEYCODE_TAB -> VirtualKey.TAB
            KeyEvent.KEYCODE_DPAD_UP -> VirtualKey.ARROW_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> VirtualKey.ARROW_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> VirtualKey.ARROW_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> VirtualKey.ARROW_RIGHT
            KeyEvent.KEYCODE_MOVE_HOME -> VirtualKey.HOME
            KeyEvent.KEYCODE_MOVE_END -> VirtualKey.END
            KeyEvent.KEYCODE_PAGE_UP -> VirtualKey.PAGE_UP
            KeyEvent.KEYCODE_PAGE_DOWN -> VirtualKey.PAGE_DOWN
            KeyEvent.KEYCODE_FORWARD_DEL -> VirtualKey.DELETE
            KeyEvent.KEYCODE_F1 -> VirtualKey.F1
            KeyEvent.KEYCODE_F2 -> VirtualKey.F2
            KeyEvent.KEYCODE_F3 -> VirtualKey.F3
            KeyEvent.KEYCODE_F4 -> VirtualKey.F4
            KeyEvent.KEYCODE_F5 -> VirtualKey.F5
            KeyEvent.KEYCODE_F6 -> VirtualKey.F6
            KeyEvent.KEYCODE_F7 -> VirtualKey.F7
            KeyEvent.KEYCODE_F8 -> VirtualKey.F8
            KeyEvent.KEYCODE_F9 -> VirtualKey.F9
            KeyEvent.KEYCODE_F10 -> VirtualKey.F10
            KeyEvent.KEYCODE_F11 -> VirtualKey.F11
            KeyEvent.KEYCODE_F12 -> VirtualKey.F12
            else -> null
        }
    }
}
