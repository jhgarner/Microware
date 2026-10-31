package com.example.microware.platform.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.microware.app.App
import com.example.microware.platform.initializeAppStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.argb(0x80, 0x1b, 0x1b, 0x1b),
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.argb(0x80, 0x1b, 0x1b, 0x1b),
            ),
        )
        super.onCreate(savedInstanceState)
        initializeAppStorage(applicationContext)

        setContent {
            App()
        }
    }
}
