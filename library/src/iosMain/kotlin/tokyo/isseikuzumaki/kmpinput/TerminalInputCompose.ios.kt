package tokyo.isseikuzumaki.kmpinput

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier as ComposeModifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero

@Composable
actual fun TerminalInputContainer(
    onInputHandlerReady: (TerminalInputHandler) -> Unit,
    modifier: ComposeModifier,
    inputMode: InputMode,
    content: @Composable (() -> Unit),
) {
}