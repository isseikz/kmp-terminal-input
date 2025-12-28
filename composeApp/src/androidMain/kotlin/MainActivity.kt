package tokyo.isseikuzumaki.kmpinput.demo

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.kmpinput.TerminalView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val terminalView = TerminalView(this)
        terminalView.setBackgroundColor(AndroidColor.LTGRAY) // Make it visible
        val scope = CoroutineScope(Dispatchers.Main + Job())
        terminalView.handler.attach(scope)
        
        // Log output to Logcat
        scope.launch {
            terminalView.handler.ptyInputStream.collect { bytes ->
                val hex = bytes.joinToString(" ") { "%02x".format(it) }
                val text = String(bytes)
                Log.d("TerminalOutput", "Bytes: $hex | Text: $text")
            }
        }

        setContent {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    App(terminalView.handler, onShowKeyboard = {
                        terminalView.requestFocus()
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
                    })
                }
                
                // Embed the native TerminalView
                AndroidView(
                    factory = { terminalView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}
