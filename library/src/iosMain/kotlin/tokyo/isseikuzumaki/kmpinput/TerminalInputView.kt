package tokyo.isseikuzumaki.kmpinput

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGPoint
import platform.Foundation.NSComparisonResult
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSRange
import platform.UIKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class TerminalInputView(frame: CValue<CGRect>) : UIView(frame), UITextInputProtocol {
    private val inputCore = TerminalInputCore()
    val handler: TerminalInputHandler get() = inputCore

    private var _inputDelegate: UITextInputDelegateProtocol? = null
    override fun inputDelegate(): UITextInputDelegateProtocol? = _inputDelegate
    override fun setInputDelegate(inputDelegate: UITextInputDelegateProtocol?) {
        _inputDelegate = inputDelegate
    }

    private val _tokenizer = UITextInputStringTokenizer(this)
    override fun tokenizer(): UITextInputTokenizerProtocol = _tokenizer

    // UITextInput traits
    private var _autocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeNo
    override fun autocorrectionType(): UITextAutocorrectionType = _autocorrectionType
    override fun setAutocorrectionType(autocorrectionType: UITextAutocorrectionType) {
        _autocorrectionType = autocorrectionType
    }
    
    private var _spellCheckingType = UITextSpellCheckingType.UITextSpellCheckingTypeNo
    override fun spellCheckingType(): UITextSpellCheckingType = _spellCheckingType
    override fun setSpellCheckingType(spellCheckingType: UITextSpellCheckingType) {
        _spellCheckingType = spellCheckingType
    }

    fun setInputMode(mode: InputMode) {
        inputCore.setInputMode(mode)
        when (mode) {
            InputMode.RAW -> {
                _autocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeNo
                _spellCheckingType = UITextSpellCheckingType.UITextSpellCheckingTypeNo
            }
            InputMode.TEXT -> {
                _autocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeYes
                _spellCheckingType = UITextSpellCheckingType.UITextSpellCheckingTypeYes
            }
        }
        reloadInputViews()
    }

    override fun canBecomeFirstResponder(): Boolean = true

    override fun hasText(): Boolean = false

    override fun insertText(text: String) {
        inputCore.dispatcher.commitText(text)
    }

    override fun deleteBackward() {
        inputCore.dispatcher.sendSpecialKey(VirtualKey.BACKSPACE)
    }

    // Minimal UITextInput implementation
    override fun textInRange(range: UITextRange): String? = null
    override fun replaceRange(range: UITextRange, withText: String) {}
    override fun selectedTextRange(): UITextRange? = null
    override fun setSelectedTextRange(selectedTextRange: UITextRange?) {}
    override fun markedTextRange(): UITextRange? = null
    
    override fun setMarkedText(markedText: String?, selectedRange: CValue<NSRange>) {
        inputCore.dispatcher.setComposingText(markedText ?: "", 0)
    }
    
    override fun unmarkText() {
        inputCore.dispatcher.setComposingText("", 0)
    }
    
    override fun beginningOfDocument(): UITextPosition = UITextPosition()
    override fun endOfDocument(): UITextPosition = UITextPosition()
    override fun textRangeFromPosition(fromPosition: UITextPosition, toPosition: UITextPosition): UITextRange? = null
    override fun positionFromPosition(position: UITextPosition, offset: Long): UITextPosition? = null
    override fun positionFromPosition(position: UITextPosition, inDirection: UITextLayoutDirection, offset: Long): UITextPosition? = null
    override fun comparePosition(position: UITextPosition, toPosition: UITextPosition): NSComparisonResult = 0L
    override fun offsetFromPosition(from: UITextPosition, toPosition: UITextPosition): Long = 0L
    override fun positionWithinRange(range: UITextRange, farthestInDirection: UITextLayoutDirection): UITextPosition? = null
    override fun characterRangeByExtendingPosition(position: UITextPosition, inDirection: UITextLayoutDirection): UITextRange? = null
    override fun baseWritingDirectionForPosition(position: UITextPosition, inDirection: UITextStorageDirection): NSWritingDirection = 0L
    override fun setBaseWritingDirection(writingDirection: NSWritingDirection, forRange: UITextRange) {}
    
    override fun firstRectForRange(range: UITextRange): CValue<CGRect> = CGRectZero.readValue()
    override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> = CGRectZero.readValue()
    
    override fun selectionRectsForRange(range: UITextRange): List<*> = emptyList<Any?>()
    
    override fun closestPositionToPoint(point: CValue<CGPoint>): UITextPosition? = null
    override fun closestPositionToPoint(point: CValue<CGPoint>, withinRange: UITextRange): UITextPosition? = null
    override fun characterRangeAtPoint(point: CValue<CGPoint>): UITextRange? = null
    
    override fun markedTextStyle(): Map<Any?, *>? = null
    override fun setMarkedTextStyle(markedTextStyle: Map<Any?, *>?) {}
}