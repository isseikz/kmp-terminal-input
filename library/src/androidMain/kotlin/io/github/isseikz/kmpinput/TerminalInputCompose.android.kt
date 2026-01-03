package io.github.isseikz.kmpinput

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier as ComposeModifier

/**
 * Jetpack Compose wrapper for TerminalView that wraps content.
 *
 * This composable wraps the given content and handles keyboard input.
 * When tapped anywhere within the content area, the software keyboard appears.
 *
 * @param state State holder for the terminal input
 * @param modifier Modifier for the composable
 * @param inputMode Initial input mode (RAW or TEXT)
 * @param onLongPress Callback for long press events. Return true if handled, false to pass to children.
 * @param content The composable content to wrap
 */
@Composable
actual fun TerminalInputContainer(
    state: TerminalInputContainerState,
    modifier: ComposeModifier,
    inputMode: InputMode,
    onLongPress: OnLongPress?,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Track the handler created by THIS instance to avoid detaching wrong handler
    var localHandler: TerminalInputHandler? = null

    DisposableEffect(Unit) {
        onDispose {
            // Only detach if our handler is still the current one
            if (localHandler != null && state.handler === localHandler) {
                state.detach()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TerminalView(ctx).apply {
                setInputMode(inputMode)
                handler.attach(scope)
                localHandler = handler
                state.handler = handler

                // Set keyboard show callback
                state.showKeyboardCallback = { showKeyboard() }

                // Set long press listener if provided
                if (onLongPress != null) {
                    onLongPressListener = OnLongPressListener { x, y ->
                        onLongPress(x, y)
                    }
                }

                // Add a ComposeView as child to host the content
                val composeView = ComposeView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                addView(composeView)
                composeView.setContent { content() }
            }
        },
        update = { view ->
            view.setInputMode(inputMode)
            // Update long press listener
            view.onLongPressListener = if (onLongPress != null) {
                OnLongPressListener { x, y -> onLongPress(x, y) }
            } else {
                null
            }
        }
    )
}
