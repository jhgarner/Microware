package com.example.microware.ui.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

fun DrawScope.resizeIcon() {
    val strokeWidth = 2.dp.toPx()
    drawRect(
        color = Color.Black,
        topLeft = Offset(size.width * 0.2f, size.height * 0.2f),
        size = Size(size.width * 0.6f, size.height * 0.6f),
        style = Stroke(strokeWidth),
    )
    val handleRadius = 3.dp.toPx()
    listOf(
        Offset(size.width * 0.2f, size.height * 0.2f),
        Offset(size.width * 0.8f, size.height * 0.2f),
        Offset(size.width * 0.2f, size.height * 0.8f),
        Offset(size.width * 0.8f, size.height * 0.8f),
    ).forEach { drawCircle(Color.Black, handleRadius, it) }
}