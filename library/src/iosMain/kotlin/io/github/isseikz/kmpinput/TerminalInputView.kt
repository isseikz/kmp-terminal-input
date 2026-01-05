package io.github.isseikz.kmpinput

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.Foundation.NSComparisonResult
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedDescending
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSRange
import platform.Foundation.NSMakeRange
import platform.UIKit.*
import platform.darwin.NSObject

/**
 * Listener for long press events on TerminalInputView.
 */
fun interface OnLongPressListener {
    /**
     * Called when a long press is detected.
     *
     * @param x The x coordinate of the long press relative to the view
     * @param y The y coordinate of the long press relative to the view
     * @return true if the event was handled, false to pass to child views
     */
    fun onLongPress(x: Float, y: Float): Boolean
}

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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class TerminalInputView(frame: CValue<CGRect>) : UIView(frame), UITextInputProtocol, UIGestureRecognizerDelegateProtocol {
    private val inputCore = TerminalInputCore()
    val handler: TerminalInputHandler get() = inputCore

    /**
     * Listener for long press events. If set and returns true, the event is consumed.
     * Otherwise, the event is passed to child views.
     */
    var onLongPressListener: OnLongPressListener? = null

    // Gesture recognizers
    private val longPressGestureRecognizer: UILongPressGestureRecognizer
    private val tapGestureRecognizer: UITapGestureRecognizer

    // Track if long press was detected to prevent tap from showing keyboard
    private var longPressDetected = false

    // Touch slop value (in points) - movement beyond this cancels long press
    // Android's default is around 8dp, iOS default allowableMovement is 10 points
    private val touchSlop = 10.0

    init {
        // Setup tap gesture recognizer
        tapGestureRecognizer = UITapGestureRecognizer(
            target = this,
            action = NSSelectorFromString("handleTap:")
        )
        tapGestureRecognizer.delegate = this
        addGestureRecognizer(tapGestureRecognizer)

        // Setup long press gesture recognizer
        longPressGestureRecognizer = UILongPressGestureRecognizer(
            target = this,
            action = NSSelectorFromString("handleLongPress:")
        )
        longPressGestureRecognizer.delegate = this
        // Set allowable movement to match touch slop behavior
        longPressGestureRecognizer.allowableMovement = touchSlop
        addGestureRecognizer(longPressGestureRecognizer)

        // Tap should wait for long press to fail
        tapGestureRecognizer.requireGestureRecognizerToFail(longPressGestureRecognizer)
    }

    @ObjCAction
    fun handleTap(gestureRecognizer: UITapGestureRecognizer) {
        platform.Foundation.NSLog("TerminalInputView: handleTap called, state=${gestureRecognizer.state}")
        if (gestureRecognizer.state == UIGestureRecognizerStateEnded) {
            platform.Foundation.NSLog("TerminalInputView: calling becomeFirstResponder")
            val result = becomeFirstResponder()
            platform.Foundation.NSLog("TerminalInputView: becomeFirstResponder returned $result")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @ObjCAction
    fun handleLongPress(gestureRecognizer: UILongPressGestureRecognizer) {
        if (gestureRecognizer.state == UIGestureRecognizerStateBegan) {
            longPressDetected = true
            val location = gestureRecognizer.locationInView(this)
            location.useContents {
                val handled = onLongPressListener?.onLongPress(x.toFloat(), y.toFloat()) ?: false
                // If not handled, pass to subviews by allowing default behavior
                if (!handled) {
                    // Find and trigger long press on subviews
                    passLongPressToSubviews(x.toFloat(), y.toFloat())
                }
            }
        } else if (gestureRecognizer.state == UIGestureRecognizerStateEnded ||
                   gestureRecognizer.state == UIGestureRecognizerStateCancelled) {
            longPressDetected = false
        }
    }

    private fun passLongPressToSubviews(x: Float, y: Float) {
        val point = CGPointMake(x.toDouble(), y.toDouble())

        // Find the deepest subview at the touch point
        val targetView = findDeepestSubview(this, point)

        if (targetView != null && targetView != this) {
            // Try to trigger long press gesture recognizer on the target view
            triggerLongPressOnView(targetView, point)
        }
    }

    private fun findDeepestSubview(view: UIView, point: CValue<CGPoint>): UIView? {
        // Convert point to view's coordinate system
        val localPoint = view.convertPoint(point, fromView = this)

        if (!view.pointInside(localPoint, withEvent = null)) {
            return null
        }

        // Check subviews in reverse order (front to back)
        view.subviews.reversed().forEach { subview ->
            val child = subview as? UIView ?: return@forEach
            if (child.isUserInteractionEnabled()) {
                val result = findDeepestSubview(child, point)
                if (result != null) {
                    return result
                }
            }
        }

        return view
    }

    private fun triggerLongPressOnView(view: UIView, point: CValue<CGPoint>) {
        // Look for UILongPressGestureRecognizer on the view
        view.gestureRecognizers?.forEach { recognizer ->
            val gestureRecognizer = recognizer as? UILongPressGestureRecognizer
            if (gestureRecognizer != null && gestureRecognizer.isEnabled()) {
                // We can't directly trigger the gesture recognizer,
                // but we can perform the long press action if available
                view.performSelector(NSSelectorFromString("longPress:"), withObject = gestureRecognizer)
                return
            }
        }

        // Fallback: try to perform long press action via responder chain
        // For UIKit views that support long press (like UITextView)
        if (view.canPerformAction(NSSelectorFromString("select:"), withSender = null)) {
            view.performSelector(NSSelectorFromString("select:"), withObject = null)
        }
    }

    // UIGestureRecognizerDelegateProtocol
    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean {
        return true
    }

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

    /**
     * Programmatically show the software keyboard.
     */
    fun showKeyboard() {
        becomeFirstResponder()
    }

    /**
     * Programmatically hide the software keyboard.
     */
    fun hideKeyboard() {
        resignFirstResponder()
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        // Don't show keyboard immediately - wait for touchesEnded to distinguish from long press
        super.touchesBegan(touches, withEvent)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        // Only show keyboard if it wasn't a long press
        if (!longPressDetected) {
            becomeFirstResponder()
        }
        super.touchesEnded(touches, withEvent)
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        longPressDetected = false
        super.touchesCancelled(touches, withEvent)
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