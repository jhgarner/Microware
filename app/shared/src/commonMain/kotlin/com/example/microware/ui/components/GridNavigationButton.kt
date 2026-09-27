package com.example.microware.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.microware.game.sprite.Palette
import com.example.microware.ui.icons.drawGridNavigationArrow

internal enum class GridPageDirection(val pageDelta: Int) {
    Previous(-1),
    Next(1),
}

@Composable
internal fun GridNavigationButton(
    direction: GridPageDirection,
    enabled: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        ),
    ) {
        val strokeWidth = 2.dp.toPx()
        val inset = strokeWidth / 2f
        val right = size.width - inset
        val bottom = size.height - inset
        val buttonColor = if (enabled) Palette.OffWhite.color else Palette.LightGray.color
        val borderColor = if (enabled) Palette.Black.color else Palette.DarkGray.color.copy(alpha = .45f)
        val arrowColor = if (enabled) Palette.Black.color else Palette.DarkGray.color.copy(alpha = .45f)

        drawRect(buttonColor)
        drawRect(
            color = borderColor,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            style = Stroke(strokeWidth),
        )
        drawGridNavigationArrow(direction == GridPageDirection.Next, arrowColor)
        if (enabled) drawProgressBorder(progress, Palette.Yellow.color)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressBorder(
    progress: Float,
    color: Color,
) {
    val strokeWidth = 3.dp.toPx()
    val inset = strokeWidth / 2f
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    val points = listOf(
        Offset((left + right) / 2f, top),
        Offset(left, top),
        Offset(left, bottom),
        Offset(right, bottom),
        Offset(right, top),
        Offset((left + right) / 2f, top),
    )
    val lengths = points.zipWithNext { start, end -> (end - start).getDistance() }

    var remaining = lengths.sum() * progress.coerceIn(0f, 1f)
    for ((index, segment) in points.zipWithNext().withIndex()) {
        if (remaining <= 0f) break

        val (start, end) = segment
        val segmentLength = lengths[index]
        val segmentProgress = (remaining / segmentLength).coerceAtMost(1f)
        drawLine(
            color = color,
            start = start,
            end = start + (end - start) * segmentProgress,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt,
        )
        remaining -= segmentLength
    }
}
