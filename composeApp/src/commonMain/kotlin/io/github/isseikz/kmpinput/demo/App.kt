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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.isseikz.kmpinput.InputMode
import io.github.isseikz.kmpinput.TerminalInputContainer
import io.github.isseikz.kmpinput.rememberTerminalInputContainerState

expect fun logD(tag: String, message: String): Unit

/**
 * Demo app showing key features of kmp-terminal-input library:
 *
 * - Tap TerminalInputContainer to show keyboard
 * - Customize long press behavior (developer-defined callback)
 * - Switch between RAW (direct) and TEXT (IME) input modes
 * - Use received input inside or outside of TerminalInputContainer
 */
@Composable
fun App() {
    // Logs for blue area - displayed OUTSIDE container
    val blueAreaLogs = remember { mutableStateListOf<String>() }
    val blueListState = rememberLazyListState()
    val longPressLogs = remember { mutableStateListOf<String>() }
    val longPressListState = rememberLazyListState()

    // Logs for orange area - displayed INSIDE container
    val orangeAreaLogs = remember { mutableStateListOf<String>() }
    val orangeListState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    // Separate terminal states for each area
    val terminalState1 = rememberTerminalInputContainerState() // Blue area (input outside)
    val terminalState2 = rememberTerminalInputContainerState() // Orange area (input inside)
    val uiState by terminalState1.uiState?.collectAsState() ?: remember { mutableStateOf(null) }

    // Collect input from blue area - displayed outside container
    LaunchedEffect(terminalState1.isReady) {
        terminalState1.ptyInputStream.collect { bytes ->
            val hex = bytes.joinToString(" ") { it.toInt().and(0xFF).toString(16).padStart(2, '0') }
            val text = bytes.decodeToString()
            blueAreaLogs.add("'$text' ($hex)")
            logD("BlueInput", "Text: '$text', Hex: $hex")
            if (blueAreaLogs.isNotEmpty()) {
                blueListState.animateScrollToItem(blueAreaLogs.size - 1)
            }
        }
    }

    // Collect input from orange area - displayed inside container
    LaunchedEffect(terminalState2.isReady) {
        terminalState2.ptyInputStream.collect { bytes ->
            val hex = bytes.joinToString(" ") { it.toInt().and(0xFF).toString(16).padStart(2, '0') }
            val text = bytes.decodeToString()
            orangeAreaLogs.add("'$text' ($hex)")
            logD("OrangeInput", "Text: '$text', Hex: $hex")
            if (orangeAreaLogs.isNotEmpty()) {
                orangeListState.animateScrollToItem(orangeAreaLogs.size - 1)
            }
        }
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Header
            Text(
                "KMP Terminal Input Demo",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Input Area: tap to show keyboard, long press custom action
            Text(
                "Input Area",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray
            )
            Text("Tap to show keyboard / Long press for custom action", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))

            TerminalInputContainer(
                state = terminalState1,
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF2196F3)),
                // Custom long press behavior
                onLongPress = { x, y ->
                    val msg = "Long press at (${x.toInt()}, ${y.toInt()})"
                    longPressLogs.add(msg)
                    logD("LongPress", msg)
                    scope.launch {
                        if (longPressLogs.isNotEmpty()) {
                            longPressListState.animateScrollToItem(longPressLogs.size - 1)
                        }
                    }
                    true // handled by callback
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tap here to show keyboard", color = Color.White, fontSize = 14.sp)
                        Text("Long press to trigger callback", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Two columns: Input logs (left) and Long press events (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                // Left: Input logs from blue area (outside container)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Input (outside):", fontSize = 11.sp, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE3F2FD))
                            .padding(6.dp)
                    ) {
                        if (blueAreaLogs.isEmpty()) {
                            Text("Type in blue area...", fontSize = 10.sp, color = Color.Gray)
                        } else {
                            LazyColumn(state = blueListState) {
                                items(blueAreaLogs.size) { index ->
                                    Text(blueAreaLogs[index], fontSize = 10.sp, color = Color(0xFF1565C0))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Right: Long press events (custom callback)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Long Press Events:", fontSize = 11.sp, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFF3E0))
                            .padding(6.dp)
                    ) {
                        if (longPressLogs.isEmpty()) {
                            Text("Long press blue area...", fontSize = 10.sp, color = Color.Gray)
                        } else {
                            LazyColumn(state = longPressListState) {
                                items(longPressLogs.size) { index ->
                                    Text(longPressLogs[index], fontSize = 10.sp, color = Color(0xFFE65100))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input Mode: RAW / TEXT switch
            Text(
                "Input Mode",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        terminalState1.setInputMode(InputMode.RAW)
                        terminalState2.setInputMode(InputMode.RAW)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (uiState?.inputMode == InputMode.RAW) Color(0xFF2196F3) else Color.LightGray
                    )
                ) {
                    Text("RAW (Direct)")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        terminalState1.setInputMode(InputMode.TEXT)
                        terminalState2.setInputMode(InputMode.TEXT)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (uiState?.inputMode == InputMode.TEXT) Color(0xFF2196F3) else Color.LightGray
                    )
                ) {
                    Text("TEXT (IME)")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    blueAreaLogs.clear()
                    orangeAreaLogs.clear()
                    longPressLogs.clear()
                }) {
                    Text("Clear")
                }
            }
            Text(
                "Current: ${uiState?.inputMode ?: "N/A"}" +
                    if (uiState?.isComposing == true) " (composing: '${uiState?.composingText}')" else "",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Input inside container + long press for text selection
            Text(
                "Input Inside Container",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray
            )
            Text("Long press to select text", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))

            TerminalInputContainer(
                state = terminalState2,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFFF9800)),
                onLongPress = { _, _ ->
                    false // Pass to children for text selection
                }
            ) {
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2D2D2D))
                            .padding(8.dp)
                    ) {
                        if (orangeAreaLogs.isEmpty()) {
                            Text(
                                "Type here, then long press to select & copy",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        } else {
                            LazyColumn(state = orangeListState) {
                                items(orangeAreaLogs.size) { index ->
                                    Text(
                                        text = orangeAreaLogs[index],
                                        color = Color(0xFFFF9800),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
