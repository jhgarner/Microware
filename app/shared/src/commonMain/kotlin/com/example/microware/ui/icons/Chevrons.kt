package com.example.microware.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

internal enum class ChevronDirection {
    Left,
    Right,
    Down,
}

@Composable
internal fun ChevronIcon(direction: ChevronDirection, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        drawChevron(direction, color)
    }
}

internal fun DrawScope.drawChevron(
    direction: ChevronDirection,
    color: Color = Color.Black,
) {
    val strokeWidth = 2.dp.toPx()
    val top = Offset(size.width * 0.5f, size.height * 0.15f)
    val right = Offset(size.width * 0.85f, size.height * 0.5f)
    val bottom = Offset(size.width * 0.5f, size.height * 0.85f)
    val left = Offset(size.width * 0.15f, size.height * 0.5f)
    val centeringOffset = size.minDimension * 0.175f
    val (translationX, translationY) = when (direction) {
        ChevronDirection.Left -> centeringOffset to 0f
        ChevronDirection.Right -> -centeringOffset to 0f
        ChevronDirection.Down -> 0f to -centeringOffset
    }

    val (start, middle, end) = when (direction) {
        ChevronDirection.Left -> Triple(top, left, bottom)
        ChevronDirection.Right -> Triple(top, right, bottom)
        ChevronDirection.Down -> Triple(left, bottom, right)
    }
    translate(left = translationX, top = translationY) {
        drawLine(color, start, middle, strokeWidth, StrokeCap.Round)
        drawLine(color, middle, end, strokeWidth, StrokeCap.Round)
    }
}
