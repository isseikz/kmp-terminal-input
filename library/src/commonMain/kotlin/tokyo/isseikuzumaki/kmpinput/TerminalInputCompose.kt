package tokyo.isseikuzumaki.kmpinput

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier as ComposeModifier


/**
 * A container composable that wraps content and handles keyboard input.
 *
 * When tapped anywhere within the content area, the software keyboard appears.
 * On Android, it uses TerminalView (FrameLayout-based).
 * On iOS, it uses TerminalInputView with a clickable overlay.
 *
 * @param modifier Modifier for the container
 * @param inputMode Input mode (RAW or TEXT)
 * @param onHandlerReady Callback to receive the TerminalInputHandler when ready
 * @param content The composable content to wrap
 */
@Composable
expect fun TerminalInputContainer(
    onInputHandlerReady: (TerminalInputHandler) -> Unit,
    modifier: ComposeModifier = ComposeModifier,
    inputMode: InputMode = InputMode.RAW,
    content: @Composable () -> Unit
)
