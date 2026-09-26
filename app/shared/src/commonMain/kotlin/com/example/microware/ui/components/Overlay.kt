package com.example.microware.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.time.Duration

typealias OverlayAnimation = DrawScope.(Float) -> Unit

class TimedOverlayAnimation(val duration: Duration, val animation: OverlayAnimation)

private val animationState = mutableStateOf<TimedOverlayAnimation?>(null)

fun setOverlayAnimation(duration: Duration, animation: OverlayAnimation) {
    animationState.value = TimedOverlayAnimation(duration, animation)
}

@Composable
fun Overlay() {
    animationState.value?.let { overlayAnimation ->
        val progress = remember { Animatable(0f) }
        LaunchedEffect(overlayAnimation) {
            try {
                progress.snapTo(0f)
                progress.animateTo(
                    1f,
                    animationSpec = tween(
                        durationMillis = overlayAnimation.duration.inWholeMilliseconds.toInt(),
                        easing = LinearEasing
                    )
                )
            } finally {
                animationState.value = null
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            overlayAnimation.animation.invoke(this, progress.value)
        }
    }
}
