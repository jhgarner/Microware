package com.example.microware.ui.icons

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
internal fun Icon(size: Dp = 40.dp, draw: DrawScope.() -> Unit) {
    Canvas(modifier = Modifier.size(size).clipToBounds()) {
        draw()
    }
}

@Composable
internal fun Animated(
    duration: Int = 1000,
    size: DpSize = DpSize(40.dp, 40.dp),
    draw: DrawScope.(Float) -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition()

    val animatedProgress: Float by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    Canvas(modifier = Modifier.size(size)) {
        draw(animatedProgress)
    }
}
