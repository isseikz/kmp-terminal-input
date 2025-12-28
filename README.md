# KMP Terminal Input

**KMP Terminal Input** is a Kotlin Multiplatform library that provides a robust, cross-platform abstraction for terminal input on mobile devices. It bridges native input systems (Android IME, iOS UITextInput) to a standard, xterm-compliant byte stream (`Flow<ByteArray>`).

This library is designed for modern terminal applications that need to switch between:
*   **Raw Mode**: For traditional shell tasks (SSH, Vim) where precise key control is needed and predictive text gets in the way.
*   **Text Mode**: For AI chat interfaces (e.g., interacting with LLMs via CLI) where users want full IME support, predictive text, voice input, and native editing capabilities.

## Features

*   **Multiplatform Support**:
    *   🤖 **Android**: `TerminalView` (extends `View`, manages `InputConnection`).
    *   🍎 **iOS**: `TerminalInputView` (extends `UIView`, implements `UITextInput`).
*   **Dual Input Modes**:
    *   **`InputMode.RAW`**: Disables auto-correct, suggestions, and auto-capitalization. Ideal for password entry, coding (Vim/Nano), and shell commands.
    *   **`InputMode.TEXT`**: Enables full system IME (Gboard, iOS Keyboard) with predictive text, flick input, and voice dictation.
*   **Standard Output**: Emits ANSI-encoded byte sequences (UTF-8 text + Escape sequences for special keys) via a Kotlin Coroutines `Flow`.
*   **Virtual Key Support**: Handles special keys like Arrows, Home/End, PageUp/Down, F1-F12, and modifier keys (Ctrl+Key).
*   **Bracketed Paste**: Support for safe pasting of text (optional).

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

## Usage

### Common Code (Shared Logic)

The core interface is `TerminalInputHandler`. You typically interact with this through the platform-specific views.

```kotlin
import tokyo.isseikuzumaki.kmpinput.*

// Accessing the handler from your view (see platform sections below)
val inputHandler: TerminalInputHandler = terminalView.handler

// observing the output stream (connect this to your PTY/SSH session)
scope.launch {
    inputHandler.ptyInputStream.collect { bytes ->
        // bytes contains UTF-8 characters or ANSI escape sequences
        // e.g., sendToPty(bytes)
    }
}

// Switching modes dynamically
fun toggleInputMode() {
    val newMode = if (currentMode == InputMode.RAW) InputMode.TEXT else InputMode.RAW
    inputHandler.setInputMode(newMode)
}
```

### Android

Add `TerminalView` to your layout (XML or programmatically).

```kotlin
// In your Activity or Fragment
val terminalView = TerminalView(context)
layout.addView(terminalView)

// Important: Ensure the view creates the InputConnection
terminalView.requestFocus()

// Switch modes
terminalView.setInputMode(InputMode.TEXT) // Enable predictive text
terminalView.setInputMode(InputMode.RAW)  // Disable predictive text (Shell-like)
```

### iOS

Use `TerminalInputView` within your UIKit hierarchy or wrap it for SwiftUI.

```swift
import UIKit
import Shared

// In your ViewController
let terminalInputView = TerminalInputView(frame: .zero)
view.addSubview(terminalInputView)

// Become first responder to show keyboard
terminalInputView.becomeFirstResponder()

// Switch modes
terminalInputView.setInputMode(mode: .text) // Enable autocorrect/suggestions
terminalInputView.setInputMode(mode: .raw)  // Disable autocorrect
```

## Architecture

The library uses an Adapter pattern to normalize platform-specific input events into a common stream.

1.  **Platform Layer**:
    *   **Android**: `TerminalView` uses a custom `InputConnection` to intercept IME events. It manipulates `EditorInfo` flags to control the keyboard type (Text vs. Visible Password/No Suggestions).
    *   **iOS**: `TerminalInputView` implements `UITextInput` protocol. It toggles `autocorrectionType` and `spellCheckingType` based on the selected mode.
2.  **Core Layer**: `TerminalInputCore` receives abstract events (`commitText`, `sendSpecialKey`) and encodes them into ANSI byte sequences (e.g., `Up Arrow` -> `\u001b[A`).
3.  **Output**: A `SharedFlow<ByteArray>` exposes the resulting stream to the consumer.

## License

[License Name] - See LICENSE file.