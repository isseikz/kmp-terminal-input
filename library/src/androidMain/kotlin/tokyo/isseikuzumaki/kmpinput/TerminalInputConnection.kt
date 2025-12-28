package tokyo.isseikuzumaki.kmpinput

import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo

class TerminalInputConnection(
    targetView: View,
    private val dispatcher: TerminalInputDispatcher,
    fullEditor: Boolean
) : BaseInputConnection(targetView, fullEditor) {

    private var isComposing = false

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        isComposing = false
        val result = super.commitText(text, newCursorPosition)
        if (text != null) {
            dispatcher.commitText(text.toString())
        }
        return result
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        isComposing = !text.isNullOrEmpty()
        val result = super.setComposingText(text, newCursorPosition)
        if (text != null) {
            dispatcher.setComposingText(text.toString(), newCursorPosition)
        }
        return result
    }
    
    override fun finishComposingText(): Boolean {
        isComposing = false
        return super.finishComposingText()
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        super.deleteSurroundingText(beforeLength, afterLength)
        if (!isComposing && beforeLength > 0 && afterLength == 0) {
            repeat(beforeLength) {
                dispatcher.sendSpecialKey(VirtualKey.BACKSPACE)
            }
            return true
        }
        return true
    }

    override fun sendKeyEvent(event: android.view.KeyEvent?): Boolean {
        // Basic implementation for hardware keys if they are passed through here
        return super.sendKeyEvent(event)
    }
}
