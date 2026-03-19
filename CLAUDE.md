# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

KMP Terminal Input is a Kotlin Multiplatform library that provides unified terminal input handling for Android and iOS. It converts mobile keyboard/IME events into byte streams (`Flow<ByteArray>`) with ANSI escape sequences, suitable for terminal emulators, SSH clients, and PTY-based apps. Published to Maven Central as `io.github.isseikz:kmp-terminal-input`.

## Build Commands

```bash
# Build the library
./gradlew :library:build

# Build demo Android APK
./gradlew :composeApp:assembleDebug

# Publish to Maven Central
./gradlew :library:publishReleasePublicationToMavenCentralRepository
```

There are no test targets configured yet.

## Architecture

The project has two modules:

- **`library/`** — The publishable KMP library
- **`composeApp/`** — A demo app showcasing the library

### Library Source Sets

- **`commonMain`** — Platform-agnostic core: input mode enums (`RAW`/`TEXT`), virtual key definitions, ANSI sequence mapping, and the `TerminalInputHandler`/`TerminalInputDispatcher` interfaces. Also contains the `expect` declaration for `TerminalInputContainer` (Compose).
- **`androidMain`** — `TerminalView` (FrameLayout-based container with touch/long-press handling), `TerminalInputConnection` (custom `BaseInputConnection` adapting Android IME to the dispatcher), and the `actual` Compose implementation wrapping `AndroidView`.
- **`iosMain`** — `TerminalInputView` (UIView implementing `UITextInputProtocol` with Japanese IME/marked text support) and the `actual` Compose implementation using `UIKitView`.

### Key Design Patterns

- **expect/actual** for `TerminalInputContainer` Composable — each platform provides its own native view integration
- **`TerminalInputDispatcher`** interface decouples platform IME adapters from core byte-stream logic
- **`SharedFlow<ByteArray>`** (buffer=64) emits input asynchronously for consumer collection
- **`TerminalInputContainerState`** is the main Compose API surface — holds handler reference, exposes `ptyInputStream` flow, input mode switching, and key injection

### Input Modes

- **RAW**: Direct key events, no IME suggestions (for shell/vim usage). Android uses `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` to suppress IME.
- **TEXT**: Full IME with predictive text, autocorrect, glide typing (for natural language input).

### iOS Long Press

Long press passthrough (`onLongPress` callback) is implemented on Android only. iOS support is noted as a future plan.

## Key Versions

Kotlin 2.3.0, Compose Multiplatform 1.7.3, AGP 8.13.0, Android minSdk 24 / compileSdk 36. Version catalog is in `gradle/libs.versions.toml`.
