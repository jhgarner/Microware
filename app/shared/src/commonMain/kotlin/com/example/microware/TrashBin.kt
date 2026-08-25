package com.example.microware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

val TrashSize = 56.dp
val TrashGap = 8.dp

@Composable
fun TrashTarget(isActive: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val background = if (isActive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = if (isActive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(TrashSize)
            .background(background, shape)
            .border(2.dp, foreground, shape)
    ) {
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
}

@Composable
fun TrashControls(
    isTrashActive: Boolean,
    trashOffsetX: Int,
    onCreate: () -> Unit,
    onTrashBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = TrashGap),
        horizontalArrangement = Arrangement.spacedBy(TrashGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrashTarget(
            isActive = isTrashActive,
            modifier = Modifier
                .offset { IntOffset(trashOffsetX, 0) }
                .onGloballyPositioned { coordinates ->
                    onTrashBoundsChanged(coordinates.boundsInRoot())
                }
        )
        Button(onClick = onCreate) {
            Text("Create")
        }
    }
}
