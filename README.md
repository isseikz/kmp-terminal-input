# KMP Terminal Input

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/tokyo.isseikuzumaki/kmp-terminal-input.svg)](https://central.sonatype.com/artifact/tokyo.isseikuzumaki/kmp-terminal-input)
[![Platform](https://img.shields.io/badge/platform-Android%20|%20iOS-lightgrey.svg)](https://kotlinlang.org/docs/multiplatform.html)

[日本語版 README はこちら](README.ja.md)

**KMP Terminal Input** is a Kotlin Multiplatform library that simplifies terminal input handling on mobile devices. It provides a unified API to consume character streams and control input modes, bridging the gap between native mobile keyboards (IMEs) and terminal emulators.

Key features:
*   **Unified Byte Stream**: Receives input as a standard `Flow<ByteArray>`, ready to be piped into a PTY or SSH session.
*   **Dual Input Modes**:
    *   **RAW Mode**: Direct key events, no predictive text. Ideal for Shell/Vim/SSH.
    *   **TEXT Mode**: Full IME support with predictive text, glide typing, and voice input. Ideal for AI chats or natural language prompts.
*   **Virtual Keys**: Handles Arrows, Ctrl+Key, Home/End, etc., mapping them to ANSI escape sequences.
*   **Compose Multiplatform Support**: Provides `TerminalInputContainer` composable for easy integration.
*   **Native UI Components**: Provides `TerminalView` (Android) and `TerminalInputView` (iOS) for direct platform usage.

---

## Installation

Add the dependency to your `commonMain` source set in `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("tokyo.isseikuzumaki:kmp-terminal-input:1.0.0")
        }
    }
}
```

Ensure `mavenCentral()` is in your repositories configuration:

```kotlin
repositories {
    mavenCentral()
}
```

---

## Usage

### Compose Multiplatform (Recommended)

The simplest way to use this library is with `TerminalInputContainer` and `rememberTerminalInputContainerState()`:

```kotlin
import tokyo.isseikuzumaki.kmpinput.*

@Composable
fun TerminalScreen() {
    val terminalState = rememberTerminalInputContainerState()
    val logs = remember { mutableStateListOf<String>() }

    // Collect keyboard input
    LaunchedEffect(terminalState.isReady) {
        terminalState.ptyInputStream.collect { bytes ->
            val text = bytes.decodeToString()
            logs.add(text)
            // Send bytes to your PTY, SSH, or process
        }
    }

    Column {
        // Mode switching buttons
        Row {
            Button(onClick = { terminalState.setInputMode(InputMode.RAW) }) {
                Text("RAW Mode")
            }
            Button(onClick = { terminalState.setInputMode(InputMode.TEXT) }) {
                Text("TEXT Mode")
            }
        }

        // Wrap your content with TerminalInputContainer
        // Tapping inside will show the keyboard
        TerminalInputContainer(
            state = terminalState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            // Your terminal display content here
            LazyColumn {
                items(logs.size) { index ->
                    Text(logs[index])
                }
            }
        }
    }
}
```

### TerminalInputContainerState API

| Property/Method | Description |
|----------------|-------------|
| `isReady` | Whether the handler is ready and attached |
| `uiState` | StateFlow of current input mode and composing state |
| `ptyInputStream` | Flow of byte arrays from keyboard input |
| `setInputMode(mode)` | Switch between RAW and TEXT mode |
| `injectKey(key, modifiers)` | Programmatically inject a virtual key |
| `injectString(text)` | Programmatically inject text |

### Android Native (XML Layout)

Use `TerminalView` directly in your Activity or Fragment:

**XML Layout:**
```xml
<tokyo.isseikuzumaki.kmpinput.TerminalView
    android:id="@+id/terminalView"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Your terminal content here -->
    <TextView
        android:id="@+id/terminalOutput"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</tokyo.isseikuzumaki.kmpinput.TerminalView>
```

**Kotlin Code:**
```kotlin
val terminalView = findViewById<TerminalView>(R.id.terminalView)
val handler = terminalView.handler

// Initialize
handler.attach(lifecycleScope)

// Collect input
lifecycleScope.launch {
    handler.ptyInputStream.collect { bytes ->
        // Handle input
    }
}

// Programmatically show/hide keyboard
terminalView.showKeyboard()
terminalView.hideKeyboard()
```

### iOS Native (Swift/UIKit)

Use `TerminalInputView` (a `UIView` subclass) in your layout:

```swift
import Shared
import UIKit

class ViewController: UIViewController {
    let terminalInputView = TerminalInputView(frame: .zero)

    override func viewDidLoad() {
        super.viewDidLoad()

        terminalInputView.frame = view.bounds
        view.addSubview(terminalInputView)

        let handler = terminalInputView.handler
        // Bind handler to your shared logic...
    }

    func showKeyboard() {
        terminalInputView.becomeFirstResponder()
    }
}
```

---

## Input Modes

| Mode | Description | Use Case |
|------|-------------|----------|
| **RAW** | No predictive text, autocorrect disabled | Shell, Vim, SSH |
| **TEXT** | Full IME support, predictive text enabled | AI chat, natural language input |

---

## Architecture

*   **Platform Layer**:
    *   **Android**: `TerminalView` extends `FrameLayout` and uses a custom `InputConnection`. Wraps child views and captures touch events to show keyboard.
    *   **iOS**: `TerminalInputView` implements `UITextInput` protocol with full marked text support for Japanese IME.
*   **Core Layer**: `TerminalInputCore` normalizes events into ANSI sequences (e.g., `Up Arrow` -> `\u001b[A`, `Ctrl+C` -> `\u0003`).
*   **Compose Layer**: `TerminalInputContainer` provides cross-platform Compose integration using `expect`/`actual` pattern.

## Demo App

Check the `composeApp` module in this repository for a complete working example using Compose Multiplatform.

## License

Apache License 2.0
