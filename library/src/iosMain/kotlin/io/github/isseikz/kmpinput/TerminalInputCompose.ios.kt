package io.github.isseikz.kmpinput

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier as ComposeModifier
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
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val terminalView = remember { TerminalInputView(CGRectZero.readValue()) }
    val interactionSource = remember { MutableInteractionSource() }

    DisposableEffect(terminalView) {
        terminalView.setInputMode(inputMode)
        terminalView.handler.attach(scope)
        state.handler = terminalView.handler
        onDispose {
            state.detach()
        }
    }

    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            terminalView.becomeFirstResponder()
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