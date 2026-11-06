package com.example.microware.editor.sprite

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.microware.game.sprite.MaxSpriteHeight
import com.example.microware.game.sprite.MaxSpriteWidth
import com.example.microware.game.sprite.SpriteBounds
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal enum class ResizeHandle(
    val movesLeft: Boolean = false,
    val movesTop: Boolean = false,
    val movesRight: Boolean = false,
    val movesBottom: Boolean = false,
) {
    TopLeft(movesLeft = true, movesTop = true),
    Top(movesTop = true),
    TopRight(movesTop = true, movesRight = true),
    Right(movesRight = true),
    BottomRight(movesRight = true, movesBottom = true),
    Bottom(movesBottom = true),
    BottomLeft(movesLeft = true, movesBottom = true),
    Left(movesLeft = true),
}

internal fun SpriteGeometry.handleCenters(bounds: SpriteBounds): Map<ResizeHandle, Offset> {
    val topLeft = position(bounds.left, bounds.top)
    val bottomRight = position(bounds.right, bounds.bottom)
    val visibleHorizontalCenter = visibleMidpoint(topLeft.x, bottomRight.x, viewport.width)
    val visibleVerticalCenter = visibleMidpoint(topLeft.y, bottomRight.y, viewport.height)
    return buildMap {
        put(ResizeHandle.TopLeft, topLeft)
        put(ResizeHandle.TopRight, Offset(bottomRight.x, topLeft.y))
        put(ResizeHandle.BottomRight, bottomRight)
        put(ResizeHandle.BottomLeft, Offset(topLeft.x, bottomRight.y))
        visibleHorizontalCenter?.let { x ->
            put(ResizeHandle.Top, Offset(x, topLeft.y))
            put(ResizeHandle.Bottom, Offset(x, bottomRight.y))
        }
        visibleVerticalCenter?.let { y ->
            put(ResizeHandle.Left, Offset(topLeft.x, y))
            put(ResizeHandle.Right, Offset(bottomRight.x, y))
        }
    }
}

internal fun SpriteGeometry.handleAt(
    position: Offset,
    bounds: SpriteBounds,
    hitSize: Float,
): ResizeHandle? = handleCenters(bounds)
    .filterValues { center ->
        abs(position.x - center.x) <= hitSize && abs(position.y - center.y) <= hitSize
    }
    .minByOrNull { (_, center) -> (position - center).getDistance() }
    ?.key

private fun visibleMidpoint(start: Float, end: Float, viewportEnd: Float): Float? {
    val visibleStart = max(min(start, end), 0f)
    val visibleEnd = min(max(start, end), viewportEnd)
    return if (visibleStart <= visibleEnd) (visibleStart + visibleEnd) / 2f else null
}

internal fun SpriteBounds.moved(
    handle: ResizeHandle,
    columns: Int,
    rows: Int,
): SpriteBounds {
    val newLeft = if (handle.movesLeft) {
        (left + columns).coerceIn(right - MaxSpriteWidth, right - 1)
    } else {
        left
    }
    val newRight = if (handle.movesRight) {
        (right + columns).coerceIn(left + 1, left + MaxSpriteWidth)
    } else {
        right
    }
    val newTop = if (handle.movesTop) {
        (top + rows).coerceIn(bottom - MaxSpriteHeight, bottom - 1)
    } else {
        top
    }
    val newBottom = if (handle.movesBottom) {
        (bottom + rows).coerceIn(top + 1, top + MaxSpriteHeight)
    } else {
        bottom
    }
    return SpriteBounds(newLeft, newTop, newRight, newBottom)
}

/** The dotted border and drag handles shared by the resize and selection tools. */
internal fun DrawScope.drawBoundsFrame(
    bounds: SpriteBounds,
    geometry: SpriteGeometry,
    color: Color = Color.White,
) {
    val topLeft = geometry.position(bounds.left, bounds.top)
    val bottomRight = geometry.position(bounds.right, bounds.bottom)
    val corners = listOf(
        topLeft,
        Offset(bottomRight.x, topLeft.y),
        bottomRight,
        Offset(topLeft.x, bottomRight.y),
    )
    val dots = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 5.dp.toPx()))
    corners.zip(corners.drop(1) + corners.first()).forEach { (start, end) ->
        drawLine(color, start, end, 2.dp.toPx(), StrokeCap.Round, dots)
    }

    val handleSize = 12.dp.toPx()
    geometry.handleCenters(bounds).values.forEach { center ->
        val handleTopLeft = center - Offset(handleSize / 2f, handleSize / 2f)
        drawRect(color, handleTopLeft, Size(handleSize, handleSize))
        drawRect(Color.Black, handleTopLeft, Size(handleSize, handleSize), style = Stroke(2.dp.toPx()))
    }
}
