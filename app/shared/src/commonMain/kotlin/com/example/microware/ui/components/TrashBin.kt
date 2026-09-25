package com.example.microware.ui.components
import com.example.microware.util.Identifiable
import com.example.microware.util.toPx
import com.example.microware.util.upserting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

val TrashGap = 8.dp

data class Trash<T : Identifiable<T>>(
    val garbage: PersistentList<T> = persistentListOf(),
) {
    fun discard(t: T): Trash<T> = copy(garbage = garbage.upserting(t))
}

@Composable
fun TrashControls(foreground: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        val strokeWidth = 2.dp.toPx()
        val canLeft = size.width * 0.28f
        val canTop = size.height * 0.25f
        val canWidth = size.width * 0.44f
        val canHeight = size.height * 0.58f
        drawRoundRect(
            color = foreground,
            topLeft = Offset(canLeft, canTop),
            size = Size(canWidth, canHeight),
            cornerRadius = CornerRadius(strokeWidth, strokeWidth),
            style = Stroke(strokeWidth)
        )
        drawLine(
            color = foreground,
            start = Offset(size.width * 0.2f, size.height * 0.18f),
            end = Offset(size.width * 0.8f, size.height * 0.18f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = foreground,
            start = Offset(size.width * 0.43f, size.height * 0.08f),
            end = Offset(size.width * 0.57f, size.height * 0.08f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
