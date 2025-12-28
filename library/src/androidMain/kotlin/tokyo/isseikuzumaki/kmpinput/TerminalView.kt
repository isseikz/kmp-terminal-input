package tokyo.isseikuzumaki.kmpinput

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val inputCore = TerminalInputCore()
    val handler: TerminalInputHandler get() = inputCore

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setInputMode(mode: InputMode) {
        if (inputCore.uiState.value.inputMode != mode) {
            inputCore.setInputMode(mode)
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.restartInput(this)
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI

        when (inputCore.uiState.value.inputMode) {
            InputMode.RAW -> {
                outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            InputMode.TEXT -> {
                outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                        InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            }
        }

        return TerminalInputConnection(this, inputCore.dispatcher)
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
        val unicodeChar = event.unicodeChar
        if (unicodeChar != 0) {
            inputCore.dispatcher.commitText(unicodeChar.toChar().toString())
            return true
        }

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
