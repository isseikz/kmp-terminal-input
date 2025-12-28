package tokyo.isseikuzumaki.kmpinput

import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo

class TerminalInputConnection(
    targetView: View,
    private val dispatcher: TerminalInputDispatcher
) : BaseInputConnection(targetView, false) {

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (text == null) return false
        dispatcher.commitText(text.toString())
        return true
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (text == null) return false
        dispatcher.setComposingText(text.toString(), newCursorPosition)
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (beforeLength > 0 && afterLength == 0) {
            repeat(beforeLength) {
                dispatcher.sendSpecialKey(VirtualKey.BACKSPACE)
            }
            return true
        }
        return super.deleteSurroundingText(beforeLength, afterLength)
    }

    override fun sendKeyEvent(event: android.view.KeyEvent?): Boolean {
        // Basic implementation for hardware keys if they are passed through here
        return super.sendKeyEvent(event)
    }
}
