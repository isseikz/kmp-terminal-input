package io.github.isseikz.kmpinput

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier as ComposeModifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIColor

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
        // Set long press listener
        terminalView.onLongPressListener = onLongPress?.let { callback ->
            OnLongPressListener { x, y -> callback(x, y) }
        }
        onDispose {
            state.detach()
        }
    }

    // Update input mode when it changes
    DisposableEffect(inputMode) {
        terminalView.setInputMode(inputMode)
        onDispose { }
    }

    // Update long press listener when it changes
    DisposableEffect(onLongPress) {
        terminalView.onLongPressListener = onLongPress?.let { callback ->
            OnLongPressListener { x, y -> callback(x, y) }
        }
        onDispose { }
    }

    Box(modifier = modifier) {
        // Transparent UIKitView at the bottom to handle touch and keyboard input
        @Suppress("DEPRECATION")
        UIKitView(
            factory = {
                terminalView.apply {
                    setBackgroundColor(UIColor.clearColor)
                    setUserInteractionEnabled(true)
                }
            },
            modifier = ComposeModifier.fillMaxSize(),
            interactive = true
        )
        // Content on top (visible)
        content()
    }
}