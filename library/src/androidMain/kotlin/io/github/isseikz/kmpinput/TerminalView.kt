package io.github.isseikz.kmpinput

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
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
import kotlin.math.abs

/**
 * Listener for long press events on TerminalView.
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
 *
 * Long press behavior:
 * - If an [OnLongPressListener] is set and returns true, it handles the event
 * - Otherwise, the long press is passed to child views (e.g., for text selection)
 */
class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val inputCore = TerminalInputCore()
    val handler: TerminalInputHandler get() = inputCore
    private var scope: CoroutineScope? = null

    // Long press detection for passing events to child views
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressDetected = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    /**
     * Listener for long press events. If set and returns true, the event is consumed.
     * Otherwise, the event is passed to child views.
     */
    var onLongPressListener: OnLongPressListener? = null

    private val longPressRunnable = Runnable {
        longPressDetected = true
        handleLongPress(touchDownX, touchDownY)
    }

    private fun handleLongPress(x: Float, y: Float) {
        // First, try the custom listener
        val handled = onLongPressListener?.onLongPress(x, y) ?: false

        // If not handled, pass to child views
        if (!handled) {
            passLongPressToChildren(x, y)
        }
    }

    private fun passLongPressToChildren(x: Float, y: Float) {
        // Find the child view at the touch coordinates and trigger long click
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (isPointInsideView(x, y, child)) {
                child.performLongClick()
                break
            }
        }
    }

    private fun isPointInsideView(x: Float, y: Float, view: android.view.View): Boolean {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val parentLocation = IntArray(2)
        getLocationInWindow(parentLocation)

        val relativeLeft = location[0] - parentLocation[0]
        val relativeTop = location[1] - parentLocation[1]
        val relativeRight = relativeLeft + view.width
        val relativeBottom = relativeTop + view.height

        return x >= relativeLeft && x <= relativeRight && y >= relativeTop && y <= relativeBottom
    }

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
        longPressHandler.removeCallbacks(longPressRunnable)
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
        if (ev == null) return false

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                // Start long press detection
                longPressDetected = false
                touchDownX = ev.x
                touchDownY = ev.y
                longPressHandler.postDelayed(
                    longPressRunnable,
                    ViewConfiguration.getLongPressTimeout().toLong()
                )
                // Always pass ACTION_DOWN to children so they can track the gesture
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                // Cancel long press if moved beyond touch slop before timeout
                val dx = abs(ev.x - touchDownX)
                val dy = abs(ev.y - touchDownY)
                if (dx > touchSlop || dy > touchSlop) {
                    if (!longPressDetected) {
                        longPressHandler.removeCallbacks(longPressRunnable)
                    }
                }
                // If long press detected, let children handle drag
                if (longPressDetected) {
                    return false
                }
                // Before long press timeout, don't intercept
                return false
            }
            MotionEvent.ACTION_UP -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                val dx = abs(ev.x - touchDownX)
                val dy = abs(ev.y - touchDownY)
                // If it was a quick tap (not long press and not moved much), show keyboard
                if (!longPressDetected && dx <= touchSlop && dy <= touchSlop) {
                    requestFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    performClick()
                }
                longPressDetected = false
                return false
            }
            MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                longPressDetected = false
                return false
            }
        }

        return false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // This is called when no child handles the event
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                // Reset state for direct touch handling
                longPressDetected = false
                touchDownX = event.x
                touchDownY = event.y
                longPressHandler.removeCallbacks(longPressRunnable)
                longPressHandler.postDelayed(
                    longPressRunnable,
                    ViewConfiguration.getLongPressTimeout().toLong()
                )
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - touchDownX)
                val dy = abs(event.y - touchDownY)
                if (dx > touchSlop || dy > touchSlop) {
                    if (!longPressDetected) {
                        longPressHandler.removeCallbacks(longPressRunnable)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                val dx = abs(event.x - touchDownX)
                val dy = abs(event.y - touchDownY)
                // If it was a quick tap (not long press and not moved much), show keyboard
                if (!longPressDetected && dx <= touchSlop && dy <= touchSlop) {
                    requestFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    performClick()
                }
                longPressDetected = false
            }
            MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                longPressDetected = false
            }
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
