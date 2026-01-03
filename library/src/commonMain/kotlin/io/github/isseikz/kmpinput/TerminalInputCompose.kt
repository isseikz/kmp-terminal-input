package io.github.isseikz.kmpinput

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier as ComposeModifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * State holder for TerminalInputContainer.
 *
 * Similar to LazyListState, this class holds the state of the terminal input
 * and provides access to the handler's functionality.
 *
 * @see rememberTerminalInputContainerState
 */
@Stable
class TerminalInputContainerState {
    private var _handler: TerminalInputHandler? by mutableStateOf(null)

    /** Version counter that increments when handler changes. Use this in LaunchedEffect keys. */
    private var _handlerVersion by mutableStateOf(0)
    val handlerVersion: Int get() = _handlerVersion

    internal var handler: TerminalInputHandler?
        get() = _handler
        set(value) {
            _handler = value
            _handlerVersion++
        }

    /** Platform-specific callback to show the keyboard. Set by TerminalInputContainer. */
    internal var showKeyboardCallback: (() -> Unit)? = null

    /** Whether the handler is ready and attached. */
    val isReady: Boolean get() = _handler != null

    /** Show the software keyboard for this terminal. */
    fun showKeyboard() {
        showKeyboardCallback?.invoke()
    }

    /** Current UI state (input mode, composing state). Returns null if not ready. */
    val uiState: StateFlow<InputUiState>? get() = handler?.uiState

    /** Stream of bytes from keyboard input. Returns empty flow if not ready. */
    val ptyInputStream: Flow<ByteArray> get() = handler?.ptyInputStream ?: emptyFlow()

    /** Set the input mode (RAW or TEXT). */
    fun setInputMode(mode: InputMode) {
        handler?.setInputMode(mode)
    }

    /** Set the cursor mode. */
    fun setCursorMode(mode: CursorMode) {
        handler?.setCursorMode(mode)
    }

    /** Enable or disable bracketed paste mode. */
    fun setBracketedPasteMode(enabled: Boolean) {
        handler?.setBracketedPasteMode(enabled)
    }

    /** Inject a virtual key. */
    fun injectKey(key: VirtualKey, modifiers: Set<Modifier> = emptySet()) {
        handler?.injectKey(key, modifiers)
    }

    /** Inject a string as if typed. */
    fun injectString(text: String) {
        handler?.injectString(text)
    }

    /** Attach to a coroutine scope. Called internally by TerminalInputContainer. */
    internal fun attach(scope: CoroutineScope) {
        handler?.attach(scope)
    }

    /** Detach from the coroutine scope. */
    internal fun detach() {
        handler?.detach()
    }
}

/**
 * Creates and remembers a [TerminalInputContainerState].
 *
 * Usage:
 * ```
 * val terminalState = rememberTerminalInputContainerState()
 *
 * LaunchedEffect(terminalState.isReady) {
 *     terminalState.ptyInputStream.collect { bytes ->
 *         // Handle input
 *     }
 * }
 *
 * TerminalInputContainer(state = terminalState) {
 *     // Your content here
 * }
 *
 * Button(onClick = { terminalState.setInputMode(InputMode.RAW) }) {
 *     Text("RAW Mode")
 * }
 * ```
 */
@Composable
fun rememberTerminalInputContainerState(): TerminalInputContainerState {
    return remember { TerminalInputContainerState() }
}

/**
 * Callback for long press events in TerminalInputContainer.
 *
 * @param x The x coordinate of the long press
 * @param y The y coordinate of the long press
 * @return true if the event was handled, false to pass to child views
 */
typealias OnLongPress = (x: Float, y: Float) -> Boolean

/**
 * A container composable that wraps content and handles keyboard input.
 *
 * When tapped anywhere within the content area, the software keyboard appears.
 * On Android, it uses TerminalView (FrameLayout-based).
 * On iOS, it uses TerminalInputView with a clickable overlay.
 *
 * @param state The state holder for this container
 * @param modifier Modifier for the container
 * @param inputMode Initial input mode (RAW or TEXT)
 * @param onLongPress Callback for long press events. Return true if handled, false to pass to children.
 * @param content The composable content to wrap
 */
@Composable
expect fun TerminalInputContainer(
    state: TerminalInputContainerState,
    modifier: ComposeModifier = ComposeModifier,
    inputMode: InputMode = InputMode.RAW,
    onLongPress: OnLongPress? = null,
    content: @Composable () -> Unit
)
