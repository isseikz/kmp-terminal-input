package io.github.isseikz.kmpinput

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier as ComposeModifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun TerminalInputContainer(
    state: TerminalInputContainerState,
    modifier: ComposeModifier,
    inputMode: InputMode,
    onLongPress: OnLongPress?,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val terminalView = remember { TerminalInputView(CGRectZero.readValue()) }

    DisposableEffect(terminalView) {
        terminalView.setInputMode(inputMode)
        terminalView.handler.attach(scope)
        state.handler = terminalView.handler
        onDispose {
            state.detach()
        }
    }

    // Update input mode when it changes
    DisposableEffect(inputMode) {
        terminalView.setInputMode(inputMode)
        onDispose { }
    }

    Box(
        modifier = modifier.pointerInput(onLongPress) {
            detectTapGestures(
                onTap = {
                    // Show keyboard on tap
                    terminalView.becomeFirstResponder()
                },
                onLongPress = { offset ->
                    // Handle long press
                    val handled = onLongPress?.invoke(offset.x, offset.y) ?: false
                    if (!handled) {
                        // If not handled, still show keyboard
                        terminalView.becomeFirstResponder()
                    }
                }
            )
        }
    ) {
        content()
        // Hidden UIKitView (zero size) to handle keyboard input
        UIKitView(
            factory = { terminalView },
            modifier = ComposeModifier.size(0.dp)
        )
    }
}