# KMP Terminal Input

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.isseikz/kmp-terminal-input.svg)](https://central.sonatype.com/artifact/io.github.isseikz/kmp-terminal-input)
[![Platform](https://img.shields.io/badge/platform-Android%20|%20iOS-lightgrey.svg)](https://kotlinlang.org/docs/multiplatform.html)

[English README](README.md)

**KMP Terminal Input** は、モバイルデバイスでのターミナル入力処理を簡素化する Kotlin Multiplatform ライブラリです。ネイティブモバイルキーボード（IME）とターミナルエミュレータの橋渡しをする統一 API を提供します。

主な機能:
*   **統一バイトストリーム**: 入力を標準的な `Flow<ByteArray>` として受信。PTY や SSH セッションに直接パイプ可能。
*   **2つの入力モード**:
    *   **RAW モード**: 直接キーイベント、予測変換なし。Shell/Vim/SSH に最適。
    *   **TEXT モード**: 予測変換、フリック入力、音声入力などフル IME サポート。AI チャットや自然言語入力に最適。
*   **仮想キー**: 矢印キー、Ctrl+Key、Home/End などを ANSI エスケープシーケンスにマッピング。
*   **Compose Multiplatform サポート**: `TerminalInputContainer` コンポーザブルで簡単に統合。
*   **ネイティブ UI コンポーネント**: `TerminalView`（Android）と `TerminalInputView`（iOS）を直接利用可能。

---

## インストール

`build.gradle.kts` の `commonMain` ソースセットに依存関係を追加:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.isseikz:kmp-terminal-input:1.0.0")
        }
    }
}
```

リポジトリ設定に `mavenCentral()` があることを確認:

```kotlin
repositories {
    mavenCentral()
}
```

---

## 使い方

### Compose Multiplatform（推奨）

`TerminalInputContainer` と `rememberTerminalInputContainerState()` を使用するのが最も簡単です:

```kotlin
import io.github.isseikz.kmpinput.*

@Composable
fun TerminalScreen() {
    val terminalState = rememberTerminalInputContainerState()
    val logs = remember { mutableStateListOf<String>() }

    // キーボード入力を収集
    LaunchedEffect(terminalState.isReady) {
        terminalState.ptyInputStream.collect { bytes ->
            val text = bytes.decodeToString()
            logs.add(text)
            // bytes を PTY、SSH、またはプロセスに送信
        }
    }

    Column {
        // モード切替ボタン
        Row {
            Button(onClick = { terminalState.setInputMode(InputMode.RAW) }) {
                Text("RAW モード")
            }
            Button(onClick = { terminalState.setInputMode(InputMode.TEXT) }) {
                Text("TEXT モード")
            }
        }

        // コンテンツを TerminalInputContainer でラップ
        // 内部をタップするとキーボードが表示される
        TerminalInputContainer(
            state = terminalState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            // ターミナル表示コンテンツをここに配置
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

| プロパティ/メソッド | 説明 |
|-------------------|------|
| `isReady` | ハンドラーが準備完了かどうか |
| `uiState` | 現在の入力モードとコンポージング状態の StateFlow |
| `ptyInputStream` | キーボード入力のバイト配列の Flow |
| `setInputMode(mode)` | RAW と TEXT モードを切り替え |
| `injectKey(key, modifiers)` | 仮想キーをプログラム的に注入 |
| `injectString(text)` | テキストをプログラム的に注入 |

### Android ネイティブ（XML レイアウト）

Activity や Fragment で `TerminalView` を直接使用:

**XML レイアウト:**
```xml
<io.github.isseikz.kmpinput.TerminalView
    android:id="@+id/terminalView"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- ターミナルコンテンツをここに配置 -->
    <TextView
        android:id="@+id/terminalOutput"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</io.github.isseikz.kmpinput.TerminalView>
```

**Kotlin コード:**
```kotlin
val terminalView = findViewById<TerminalView>(R.id.terminalView)
val handler = terminalView.handler

// 初期化
handler.attach(lifecycleScope)

// 入力を収集
lifecycleScope.launch {
    handler.ptyInputStream.collect { bytes ->
        // 入力を処理
    }
}

// プログラム的にキーボードを表示/非表示
terminalView.showKeyboard()
terminalView.hideKeyboard()
```

### iOS ネイティブ（Swift/UIKit）

`TerminalInputView`（`UIView` サブクラス）をレイアウトで使用:

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
        // ハンドラーを共有ロジックにバインド...
    }

    func showKeyboard() {
        terminalInputView.becomeFirstResponder()
    }
}
```

---

## 入力モード

| モード | 説明 | ユースケース |
|--------|------|-------------|
| **RAW** | 予測変換なし、オートコレクト無効 | Shell、Vim、SSH |
| **TEXT** | フル IME サポート、予測変換有効 | AI チャット、自然言語入力 |

---

## アーキテクチャ

*   **プラットフォーム層**:
    *   **Android**: `TerminalView` は `FrameLayout` を継承し、カスタム `InputConnection` を使用。子ビューをラップし、タッチイベントをキャプチャしてキーボードを表示。
    *   **iOS**: `TerminalInputView` は `UITextInput` プロトコルを実装し、日本語 IME のマークドテキストを完全サポート。
*   **コア層**: `TerminalInputCore` がイベントを ANSI シーケンスに正規化（例: `上矢印` -> `\u001b[A`、`Ctrl+C` -> `\u0003`）。
*   **Compose 層**: `TerminalInputContainer` が `expect`/`actual` パターンでクロスプラットフォーム Compose 統合を提供。

## デモアプリ

このリポジトリの `composeApp` モジュールで、Compose Multiplatform を使用した完全な動作例を確認できます。

## ライセンス

Apache License 2.0