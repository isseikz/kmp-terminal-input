package tokyo.isseikuzumaki.kmpinput.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIColor
import platform.UIKit.UIViewController
import tokyo.isseikuzumaki.kmpinput.TerminalInputView

@OptIn(ExperimentalForeignApi::class)
fun MainViewController(): UIViewController {
    val terminalView = TerminalInputView(CGRectZero.readValue())
    terminalView.backgroundColor = UIColor.lightGrayColor
    val scope = CoroutineScope(Dispatchers.Main + Job())
    terminalView.handler.attach(scope)
    
    // Log output to console (print)
    scope.launch {
        terminalView.handler.ptyInputStream.collect { bytes ->
            val text = bytes.decodeToString()
            println("TerminalOutput: $text")
        }
    }

    return ComposeUIViewController {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                App(terminalView.handler, onShowKeyboard = {
                    terminalView.becomeFirstResponder()
                })
            }
            
            // Embed the native TerminalInputView
            UIKitView(
                factory = {
                    terminalView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                update = { view ->
                     // Make sure it can become first responder
                }
            )
        }
    }
}
