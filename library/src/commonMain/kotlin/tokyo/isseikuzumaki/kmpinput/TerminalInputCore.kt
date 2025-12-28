package tokyo.isseikuzumaki.kmpinput

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalInputCore : TerminalInputHandler {
    private var scope: CoroutineScope? = null
    private val _ptyInputStream = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 64
    )
    override val ptyInputStream: SharedFlow<ByteArray> = _ptyInputStream.asSharedFlow()

    private val _uiState = MutableStateFlow(InputUiState())
    override val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    private var bracketedPasteMode = false

    val dispatcher = object : TerminalInputDispatcher {
        override fun commitText(text: String) {
            sendBytes(text.encodeToByteArray())
        }

        override fun setComposingText(text: String, cursorIndex: Int) {
            _uiState.value = _uiState.value.copy(
                isComposing = text.isNotEmpty(),
                composingText = text
            )
        }

        override fun sendSpecialKey(key: VirtualKey) {
            val sequence = getSequenceForKey(key)
            if (sequence != null) {
                sendBytes(sequence.encodeToByteArray())
            }
        }

        override fun sendControlKey(baseChar: Char) {
            val ctrlByte = (baseChar.uppercaseChar().code - '@'.code).toByte()
            sendBytes(byteArrayOf(ctrlByte))
        }
    }

    override fun attach(scope: CoroutineScope) {
        this.scope = scope
    }

    override fun detach() {
        this.scope = null
    }

    override fun setCursorMode(mode: CursorMode) {
        // ANSI escape sequence for cursor mode could be sent here if needed by the terminal
    }

    override fun setBracketedPasteMode(enabled: Boolean) {
        bracketedPasteMode = enabled
    }

    override fun setInputMode(mode: InputMode) {
        _uiState.value = _uiState.value.copy(inputMode = mode)
    }

    override fun injectKey(key: VirtualKey, modifiers: Set<Modifier>) {
        // TODO: Handle modifiers
        val sequence = getSequenceForKey(key)
        if (sequence != null) {
            sendBytes(sequence.encodeToByteArray())
        }
    }

    override fun injectString(text: String) {
        if (bracketedPasteMode) {
            sendBytes("\u001b[200~".encodeToByteArray())
            sendBytes(text.encodeToByteArray())
            sendBytes("\u001b[201~".encodeToByteArray())
        } else {
            sendBytes(text.encodeToByteArray())
        }
    }

    private fun sendBytes(bytes: ByteArray) {
        scope?.launch {
            _ptyInputStream.emit(bytes)
        }
    }

    private fun getSequenceForKey(key: VirtualKey): String? {
        return when (key) {
            VirtualKey.ENTER -> "\r"
            VirtualKey.ESCAPE -> "\u001b"
            VirtualKey.BACKSPACE -> "\u007f"
            VirtualKey.TAB -> "\t"
            VirtualKey.ARROW_UP -> "\u001b[A"
            VirtualKey.ARROW_DOWN -> "\u001b[B"
            VirtualKey.ARROW_RIGHT -> "\u001b[C"
            VirtualKey.ARROW_LEFT -> "\u001b[D"
            VirtualKey.HOME -> "\u001b[H"
            VirtualKey.END -> "\u001b[F"
            VirtualKey.PAGE_UP -> "\u001b[5~"
            VirtualKey.PAGE_DOWN -> "\u001b[6~"
            VirtualKey.DELETE -> "\u001b[3~"
            VirtualKey.F1 -> "\u001bOP"
            VirtualKey.F2 -> "\u001bOQ"
            VirtualKey.F3 -> "\u001bOR"
            VirtualKey.F4 -> "\u001bOS"
            VirtualKey.F5 -> "\u001b[15~"
            VirtualKey.F6 -> "\u001b[17~"
            VirtualKey.F7 -> "\u001b[18~"
            VirtualKey.F8 -> "\u001b[19~"
            VirtualKey.F9 -> "\u001b[20~"
            VirtualKey.F10 -> "\u001b[21~"
            VirtualKey.F11 -> "\u001b[23~"
            VirtualKey.F12 -> "\u001b[24~"
        }
    }
}