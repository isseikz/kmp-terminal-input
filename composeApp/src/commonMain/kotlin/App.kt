package tokyo.isseikuzumaki.kmpinput.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.kmpinput.InputMode
import tokyo.isseikuzumaki.kmpinput.TerminalInputHandler

import androidx.compose.runtime.LaunchedEffect

@Composable
fun App(inputHandler: TerminalInputHandler, onShowKeyboard: () -> Unit) {
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val uiState by inputHandler.uiState.collectAsState()
            val logs = remember { mutableStateListOf<String>() }

            LaunchedEffect(inputHandler) {
                inputHandler.ptyInputStream.collect { bytes ->
                    val hex = bytes.joinToString(" ") { "%02x".format(it) }
                    // Keep last 20 logs to avoid performance issues
                    if (logs.size > 20) logs.removeAt(0)
                    logs.add("Bytes: $hex")
                }
            }

            Text("KMP Terminal Input Demo", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(onClick = { inputHandler.setInputMode(InputMode.RAW) }) {
                    Text("RAW Mode")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Button(onClick = { inputHandler.setInputMode(InputMode.TEXT) }) {
                    Text("TEXT Mode")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onShowKeyboard) {
                Text("Show Keyboard")
            }
            
            Text("Current Mode: ${uiState.inputMode}")
            Text("Composing: ${uiState.isComposing} - '${uiState.composingText}'")

            Spacer(modifier = Modifier.height(16.dp))
            
            // Log area to show what keys/text are being sent
            Text("Output Log (Hex):")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.LightGray)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                 if (logs.isEmpty()) {
                     Text("Type in the terminal view below/above to see output.")
                 }
                 logs.forEach { log ->
                     Text(log)
                 }
            }
        }
    }
}
