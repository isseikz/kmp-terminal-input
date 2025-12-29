package io.github.isseikz.kmpinput

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGPoint
import platform.Foundation.NSComparisonResult
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedDescending
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSRange
import platform.Foundation.NSMakeRange
import platform.UIKit.*
import platform.darwin.NSObject

// Custom UITextPosition implementation
private class SimpleTextPosition(val offset: Int) : UITextPosition()

// Custom UITextRange implementation
private class SimpleTextRange(
    private val startPos: SimpleTextPosition,
    private val endPos: SimpleTextPosition
) : UITextRange() {
    override fun start(): UITextPosition = startPos
    override fun end(): UITextPosition = endPos
    override fun isEmpty(): Boolean = startPos.offset == endPos.offset

    companion object {
        fun create(start: Int, end: Int): SimpleTextRange {
            return SimpleTextRange(SimpleTextPosition(start), SimpleTextPosition(end))
        }
    }
}

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

    // Track current marked (composing) text for Japanese IME
    private var currentMarkedText: String = ""

    // Track the marked text range
    private var _markedTextRange: SimpleTextRange? = null

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

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        becomeFirstResponder()
        super.touchesBegan(touches, withEvent)
    }

    override fun hasText(): Boolean = currentMarkedText.isNotEmpty()

    override fun insertText(text: String) {
        // Clear marked text state when inserting
        currentMarkedText = ""
        _markedTextRange = null
        inputCore.dispatcher.setComposingText("", 0)
        inputCore.dispatcher.commitText(text)
    }

    override fun deleteBackward() {
        if (currentMarkedText.isNotEmpty()) {
            // Delete from marked text
            currentMarkedText = currentMarkedText.dropLast(1)
            if (currentMarkedText.isEmpty()) {
                _markedTextRange = null
            } else {
                _markedTextRange = SimpleTextRange.create(0, currentMarkedText.length)
            }
            inputCore.dispatcher.setComposingText(currentMarkedText, 0)
        } else {
            inputCore.dispatcher.sendSpecialKey(VirtualKey.BACKSPACE)
        }
    }

    // UITextInput implementation for Japanese IME support
    override fun textInRange(range: UITextRange): String? {
        // Return the marked text if the range covers it
        if (currentMarkedText.isNotEmpty() && range is SimpleTextRange) {
            val start = (range.start() as? SimpleTextPosition)?.offset ?: 0
            val end = (range.end() as? SimpleTextPosition)?.offset ?: currentMarkedText.length
            if (start >= 0 && end <= currentMarkedText.length && start <= end) {
                return currentMarkedText.substring(start, end)
            }
        }
        return currentMarkedText.ifEmpty { null }
    }

    override fun replaceRange(range: UITextRange, withText: String) {
        // Some IMEs use replaceRange to commit text
        currentMarkedText = ""
        _markedTextRange = null
        inputCore.dispatcher.setComposingText("", 0)
        if (withText.isNotEmpty()) {
            inputCore.dispatcher.commitText(withText)
        }
    }

    override fun selectedTextRange(): UITextRange? {
        // Return cursor position at the end of marked text
        if (currentMarkedText.isNotEmpty()) {
            val pos = currentMarkedText.length
            return SimpleTextRange.create(pos, pos)
        }
        return SimpleTextRange.create(0, 0)
    }

    override fun setSelectedTextRange(selectedTextRange: UITextRange?) {}

    override fun markedTextRange(): UITextRange? = _markedTextRange

    override fun setMarkedText(markedText: String?, selectedRange: CValue<NSRange>) {
        currentMarkedText = markedText ?: ""
        _markedTextRange = if (currentMarkedText.isNotEmpty()) {
            SimpleTextRange.create(0, currentMarkedText.length)
        } else {
            null
        }
        inputCore.dispatcher.setComposingText(currentMarkedText, 0)
    }

    override fun unmarkText() {
        // When unmarkText is called, commit the current marked text if any
        // This handles Japanese IME where text might be committed via unmarkText
        if (currentMarkedText.isNotEmpty()) {
            val textToCommit = currentMarkedText
            currentMarkedText = ""
            _markedTextRange = null
            inputCore.dispatcher.setComposingText("", 0)
            inputCore.dispatcher.commitText(textToCommit)
        } else {
            _markedTextRange = null
            inputCore.dispatcher.setComposingText("", 0)
        }
    }

    override fun beginningOfDocument(): UITextPosition = SimpleTextPosition(0)
    override fun endOfDocument(): UITextPosition = SimpleTextPosition(currentMarkedText.length)

    override fun textRangeFromPosition(fromPosition: UITextPosition, toPosition: UITextPosition): UITextRange? {
        val start = (fromPosition as? SimpleTextPosition)?.offset ?: 0
        val end = (toPosition as? SimpleTextPosition)?.offset ?: 0
        return SimpleTextRange.create(start, end)
    }

    override fun positionFromPosition(position: UITextPosition, offset: Long): UITextPosition? {
        val currentOffset = (position as? SimpleTextPosition)?.offset ?: 0
        val newOffset = currentOffset + offset.toInt()
        if (newOffset >= 0 && newOffset <= currentMarkedText.length) {
            return SimpleTextPosition(newOffset)
        }
        return null
    }

    override fun positionFromPosition(position: UITextPosition, inDirection: UITextLayoutDirection, offset: Long): UITextPosition? {
        return positionFromPosition(position, offset)
    }

    override fun comparePosition(position: UITextPosition, toPosition: UITextPosition): NSComparisonResult {
        val pos1 = (position as? SimpleTextPosition)?.offset ?: 0
        val pos2 = (toPosition as? SimpleTextPosition)?.offset ?: 0
        return when {
            pos1 < pos2 -> NSOrderedAscending
            pos1 > pos2 -> NSOrderedDescending
            else -> NSOrderedSame
        }
    }

    override fun offsetFromPosition(from: UITextPosition, toPosition: UITextPosition): Long {
        val pos1 = (from as? SimpleTextPosition)?.offset ?: 0
        val pos2 = (toPosition as? SimpleTextPosition)?.offset ?: 0
        return (pos2 - pos1).toLong()
    }
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