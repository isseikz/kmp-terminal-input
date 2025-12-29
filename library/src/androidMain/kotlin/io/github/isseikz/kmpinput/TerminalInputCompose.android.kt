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
 * @param content The composable content to wrap
 */
@Composable
actual fun TerminalInputContainer(
    state: TerminalInputContainerState,
    modifier: ComposeModifier,
    inputMode: InputMode,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            state.detach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TerminalView(ctx).apply {
                setInputMode(inputMode)
                handler.attach(scope)
                state.handler = handler

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
        }
    )
}
