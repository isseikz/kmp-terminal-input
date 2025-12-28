# KMP Terminal Input

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/tokyo.isseikuzumaki/kmp-terminal-input.svg)](https://central.sonatype.com/artifact/tokyo.isseikuzumaki/kmp-terminal-input)
[![Platform](https://img.shields.io/badge/platform-Android%20|%20iOS-lightgrey.svg)](https://kotlinlang.org/docs/multiplatform.html)

**KMP Terminal Input** is a Kotlin Multiplatform library that simplifies terminal input handling on mobile devices. It provides a unified API to consume character streams and control input modes, bridging the gap between native mobile keyboards (IMEs) and terminal emulators.

Key features:
*   **Unified Byte Stream**: Receives input as a standard `Flow<ByteArray>`, ready to be piped into a PTY or SSH session.
*   **Dual Input Modes**:
    *   **RAW Mode**: Direct key events, no predictive text. Ideal for Shell/Vim/SSH.
    *   **TEXT Mode**: Full IME support with predictive text, glide typing, and voice input. Ideal for AI chats or natural language prompts.
*   **Virtual Keys**: Handles Arrows, Ctrl+Key, Home/End, etc., mapping them to ANSI escape sequences.
*   **Native UI Components**: Provides `TerminalView` (Android) and `TerminalInputView` (iOS) that handle focus and keyboard interactions correctly.

---

## 📦 Installation

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

## 🚀 Usage

### 1. Shared Logic (Common Code)

The core interface is `TerminalInputHandler`. You typically access this from the platform-specific views.

```kotlin
import tokyo.isseikuzumaki.kmpinput.*

// Accessing the handler (passed from platform code)
fun bindInput(handler: TerminalInputHandler, scope: CoroutineScope) {
    
    // Switch Input Mode
    handler.setInputMode(InputMode.TEXT) // Enable Auto-correct/Prediction
    // handler.setInputMode(InputMode.RAW) // Disable Prediction (Shell mode)

    // Observe Output
    scope.launch {
        handler.ptyInputStream.collect { bytes ->
            val text = bytes.decodeToString()
            // Send 'bytes' to your PTY, SSH, or process
            println("Received: $text") 
        }
    }
}
```

### 2. Android Integration

Use `TerminalView` in your Activity, Fragment, or Composable (via `AndroidView`).

**XML Layout:**
```xml
<tokyo.isseikuzumaki.kmpinput.TerminalView
    android:id="@+id/terminalView"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    android:background="#EEE" />
```

**Kotlin Code:**
```kotlin
val terminalView = findViewById<TerminalView>(R.id.terminalView)
val handler = terminalView.handler

// Initialize
handler.attach(lifecycleScope)

// Show Keyboard on tap
terminalView.setOnClickListener {
    terminalView.requestFocus()
    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
}
```

### 3. iOS Integration

Use `TerminalInputView` (a `UIView` subclass) in your layout.

**Swift / UIKit:**
```swift
import Shared
import UIKit

class ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()

        let terminalInputView = TerminalInputView(frame: self.view.bounds)
        self.view.addSubview(terminalInputView)
        
        let handler = terminalInputView.handler
        // Bind handler to your shared logic...
    }
    
    // Show keyboard
    func showKeyboard() {
        terminalInputView.becomeFirstResponder()
    }
}
```

**Compose Multiplatform (iOS):**
```kotlin
// In your iOS MainViewController.kt
fun MainViewController() = ComposeUIViewController {
    val terminalView = remember { TerminalInputView(CGRectZero.readValue()) }
    
    UIKitView(
        factory = { terminalView },
        modifier = Modifier.fillMaxWidth().height(100.dp)
    )
}
```

---

## 🛠️ Architecture

*   **Platform Layer**:
    *   **Android**: `TerminalView` extends `View` and uses a custom `InputConnection`. It handles the complexity of "Dummy" input connections vs "Full Editor" connections to support both Raw keys and IME features like Japanese predictive input.
    *   **iOS**: `TerminalInputView` implements `UITextInput` protocol to interface with the system keyboard, handling autocorrect flags and text insertion.
*   **Core Layer**: `TerminalInputCore` normalizes events into ANSI sequences (e.g., `Up Arrow` -> `\u001b[A`, `Ctrl+C` -> `\u0003`).

## 📱 Demo App

Check the `composeApp` module in this repository for a complete working example using Compose Multiplatform.

## License

Apache License 2.0
