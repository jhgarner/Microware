package com.example.microware.platform.desktop

import com.example.microware.app.App

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MicroWare",
    ) {
        App()
    }
}