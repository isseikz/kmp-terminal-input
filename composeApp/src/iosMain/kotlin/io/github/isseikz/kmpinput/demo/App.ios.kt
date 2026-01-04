package io.github.isseikz.kmpinput.demo

import platform.Foundation.NSLog

actual fun logD(tag: String, message: String) {
    NSLog("[$tag] $message")
}