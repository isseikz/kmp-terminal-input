package tokyo.isseikuzumaki.kmpinput

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * InputMode defines the behavior of the input system.
 */
enum class InputMode {
    /**
     * Raw Mode (Default):
     * For SSH, Vim, Shell operations.
     * Disables predictive text, auto-correct, and auto-capitalization.
     */
    RAW,

    /**
     * Text Mode:
     * For AI chat, natural language prompts.
     * Enables IME predictive text, flick input, and voice input.
     */
    TEXT
}

/**
 * CursorMode defines the cursor behavior.
 */
enum class CursorMode {
    BLINKING_BLOCK,
    STEADY_BLOCK,
    BLINKING_UNDERLINE,
    STEADY_UNDERLINE,
    BLINKING_BAR,
    STEADY_BAR
}

/**
 * VirtualKey represents special keys in terminal.
 */
enum class VirtualKey {
    ENTER,
    ESCAPE,
    BACKSPACE,
    TAB,
    ARROW_UP,
    ARROW_DOWN,
    ARROW_LEFT,
    ARROW_RIGHT,
    HOME,
    END,
    PAGE_UP,
    PAGE_DOWN,
    DELETE,
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12
}

/**
 * Modifier keys.
 */
enum class Modifier {
    CTRL,
    ALT,
    SHIFT,
    META
}

/**
 * UI State for the input.
 */
data class InputUiState(
    val inputMode: InputMode = InputMode.RAW,
    val isComposing: Boolean = false,
    val composingText: String = ""
)

/**
 * TerminalInputDispatcher: Interface from adapter to core logic.
 */
interface TerminalInputDispatcher {
    fun commitText(text: String)
    fun setComposingText(text: String, cursorIndex: Int)
    fun sendSpecialKey(key: VirtualKey)
    fun sendControlKey(baseChar: Char)
}

/**
 * TerminalInputHandler: Public interface of the library.
 */
interface TerminalInputHandler {
    fun attach(scope: CoroutineScope)
    fun detach()

    // Terminal control settings
    fun setCursorMode(mode: CursorMode)
    fun setBracketedPasteMode(enabled: Boolean)

    // Input mode switching
    fun setInputMode(mode: InputMode)

    // External input injection
    fun injectKey(key: VirtualKey, modifiers: Set<Modifier> = emptySet())
    fun injectString(text: String)

    // Output stream
    val ptyInputStream: Flow<ByteArray>

    // UI state
    val uiState: StateFlow<InputUiState>
}
