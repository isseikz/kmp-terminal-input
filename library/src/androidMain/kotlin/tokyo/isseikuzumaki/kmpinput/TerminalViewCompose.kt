package tokyo.isseikuzumaki.kmpinput

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
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
 * @param modifier Modifier for the composable
 * @param inputMode Input mode (RAW or TEXT)
 * @param content The composable content to wrap
 */
@Composable
actual fun TerminalInputContainer(
    onInputHandlerReady: (TerminalInputHandler) -> Unit,
    modifier: ComposeModifier,
    inputMode: InputMode,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TerminalView(ctx).apply {
                setInputMode(inputMode)
                handler.attach(scope)

                // Add a ComposeView as child to host the content
                val composeView = ComposeView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                addView(composeView)
                composeView.setContent { content() }

                onInputHandlerReady(this.handler)
            }
        },
        update = { view ->
            view.setInputMode(inputMode)
        }
    )
}
