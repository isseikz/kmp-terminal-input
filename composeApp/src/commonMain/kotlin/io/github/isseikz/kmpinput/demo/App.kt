package io.github.isseikz.kmpinput.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.isseikz.kmpinput.InputMode
import io.github.isseikz.kmpinput.TerminalInputContainer
import io.github.isseikz.kmpinput.rememberTerminalInputContainerState

expect fun logD(tag: String, message: String): Unit

/**
 * Demo app UI.
 */
@Composable
fun App() {
    val logs = remember { mutableStateListOf<String>() }
    val callbackLogs = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val callbackListState = rememberLazyListState()

    // Create terminal states for both areas
    val terminalState1 = rememberTerminalInputContainerState()
    val terminalState2 = rememberTerminalInputContainerState()
    val inputState by terminalState1.uiState?.collectAsState() ?: remember { mutableStateOf(null) }

    // Collect keyboard input from both terminals
    // terminalState1 (orange) -> callbackLogs (orange area)
    LaunchedEffect(terminalState1.isReady) {
        terminalState1.ptyInputStream.collect { bytes ->
            val text = bytes.decodeToString()
            callbackLogs.add("Input: '$text'")
            if (callbackLogs.isNotEmpty()) {
                callbackListState.animateScrollToItem(callbackLogs.size - 1)
            }
        }
    }

    // terminalState2 (green) -> logs (green area)
    LaunchedEffect(terminalState2.isReady) {
        terminalState2.ptyInputStream.collect { bytes ->
            val text = bytes.decodeToString()
            logs.add("Input: '$text'")
            if (logs.isNotEmpty()) {
                listState.animateScrollToItem(logs.size - 1)
            }
        }
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Long Press Demo", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(onClick = {
                    terminalState1.setInputMode(InputMode.RAW)
                    terminalState2.setInputMode(InputMode.RAW)
                }) {
                    Text("RAW")
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = {
                    terminalState1.setInputMode(InputMode.TEXT)
                    terminalState2.setInputMode(InputMode.TEXT)
                }) {
                    Text("TEXT")
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = {
                    logs.clear()
                    callbackLogs.clear()
                }) {
                    Text("Clear")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pattern 1: Long press triggers callback
            Text("1. Callback Pattern", style = MaterialTheme.typography.subtitle1)
            Text("Long press shows coordinates in log", style = MaterialTheme.typography.caption)
            Spacer(modifier = Modifier.height(4.dp))

            TerminalInputContainer(
                state = terminalState1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFFF9800)),
                onLongPress = { x, y ->
                    val msg = "Long press at (${x.toInt()}, ${y.toInt()})"
                    callbackLogs.add(msg)
                    logD("Callback", msg)
                    true // Return true: event handled, don't pass to children
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2D2D2D))
                        .padding(8.dp)
                ) {
                    LazyColumn(state = callbackListState) {
                        items(callbackLogs.size) { index ->
                            Text(
                                text = callbackLogs[index],
                                color = Color(0xFFFF9800),
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }
                }
            }
            Button(
                onClick = { terminalState1.showKeyboard() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Keyboard for Callback Pattern")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pattern 2: Long press enables text selection
            Text("2. Text Selection Pattern", style = MaterialTheme.typography.subtitle1)
            Text("Long press to select & copy text", style = MaterialTheme.typography.caption)
            Spacer(modifier = Modifier.height(4.dp))

            TerminalInputContainer(
                state = terminalState2,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF4CAF50)),
                onLongPress = { x, y ->
                    logD("Selection", "Long press at ($x, $y) - passing to children")
                    false // Return false: pass to children for text selection
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                        .padding(8.dp)
                ) {
                    SelectionContainer {
                        LazyColumn(state = listState) {
                            items(logs.size) { index ->
                                Text(
                                    text = logs[index],
                                    color = Color(0xFF4CAF50),
                                    style = MaterialTheme.typography.body2
                                )
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { terminalState2.showKeyboard() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Keyboard for Text Selection Pattern")
            }
        }
    }
}
