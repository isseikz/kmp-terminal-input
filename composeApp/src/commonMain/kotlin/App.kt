package tokyo.isseikuzumaki.kmpinput.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.kmpinput.InputMode
import tokyo.isseikuzumaki.kmpinput.TerminalInputContainer
import tokyo.isseikuzumaki.kmpinput.TerminalInputHandler

expect fun logD(tag: String, message: String): Unit

/**
 * Demo app UI.
 */
@Composable
fun App() {
    val logs = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()

    // Point 1/2 Key input can be obtained via TerminalInputHandler
    var inputHandler by remember { mutableStateOf<TerminalInputHandler?>(null) }
    val inputState by inputHandler?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(inputHandler) {
        inputHandler?.ptyInputStream?.collect { bytes ->
            val hex = bytes.joinToString(" ") {
                it.toInt().and(0xFF).toString(16)
                    .padStart(2, '0')
            }
            val text = bytes.decodeToString()
            logs.add("Text: '$text', Bytes: $hex")
            logD("TerminalOutput", "Bytes: $hex | Text: $text")

            if (logs.isNotEmpty()) {
                listState.animateScrollToItem(logs.size - 1)
            }
        }
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("KMP Terminal Input Demo", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Tap log area to show keyboard", style = MaterialTheme.typography.caption)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(
                    enabled = inputHandler != null,
                    onClick = { inputHandler?.setInputMode(InputMode.RAW) }
                ) {
                    Text("RAW Mode")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Button(
                    enabled = inputHandler != null,
                    onClick = { inputHandler?.setInputMode(InputMode.TEXT) }
                ) {
                    Text("TEXT Mode")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Current Mode: ${inputState?.inputMode ?: "N/A"}")
            Text("Composing: ${inputState?.isComposing ?: "N/A"} - '${inputState?.composingText ?: "N/A"}'")

            Spacer(modifier = Modifier.height(16.dp))

            Text("Output Log:")

            // Point 2/2 Keyboard input area is wrapped in TerminalInputContainer
            TerminalInputContainer(
                onInputHandlerReady = { handler ->
                    inputHandler = handler
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                        .padding(8.dp)
                ) {
                    LazyColumn(state = listState) {
                        items(logs.size) { index ->
                            Text(
                                text = logs[index],
                                color = Color.White,
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }
                }
            }
        }
    }
}
