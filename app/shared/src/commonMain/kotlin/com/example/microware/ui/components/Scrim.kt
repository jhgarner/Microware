package com.example.microware.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex

private var scrimOpen by mutableStateOf(false)

fun showScrim() {
    scrimOpen = true
}

fun hideScrim() {
    scrimOpen = false
}

@Composable
fun Scrim() {
    val fraction = animateFloatAsState(if (scrimOpen) 1f else 0f, tween(durationMillis = 180))
    val scrimAlpha = fraction.value * 0.32f
    if (scrimAlpha > 0f) {
        Box(
            modifier = Modifier
                .zIndex(DrawerScrimZIndex)
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable {}
        )
    }
}
