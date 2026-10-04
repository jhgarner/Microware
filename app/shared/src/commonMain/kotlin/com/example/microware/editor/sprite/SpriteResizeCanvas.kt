package com.example.microware.editor.sprite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.sprite.MaxSpriteHeight
import com.example.microware.game.sprite.MaxSpriteWidth
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteBounds
import com.example.microware.ui.gestures.racing
import com.example.microware.ui.gestures.sequence
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class ResizeHandle(
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

private data class ResizeGeometry(
    val origin: Offset,
    val cellSize: Float,
    val viewport: Size,
) {
    fun position(col: Int, row: Int) = Offset(
        x = origin.x + col * cellSize,
        y = origin.y + row * cellSize,
    )

    fun handleCenters(bounds: SpriteBounds): Map<ResizeHandle, Offset> {
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
}

private fun visibleMidpoint(start: Float, end: Float, viewportEnd: Float): Float? {
    val visibleStart = max(min(start, end), 0f)
    val visibleEnd = min(max(start, end), viewportEnd)
    return if (visibleStart <= visibleEnd) (visibleStart + visibleEnd) / 2f else null
}

@Composable
fun SpriteResizeCanvas(
    sprite: Sprite,
    boundsState: MutableState<SpriteBounds>,
    panState: MutableState<Offset>,
    zoomState: MutableState<Float>,
) {
    var pan by panState
    var zoom by zoomState
    val currentBounds = rememberUpdatedState(boundsState.value)
    val imagePaint = remember {
        Paint().apply { filterQuality = FilterQuality.None }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(sprite.width, sprite.height) {
                fun geometry() = resizeGeometry(
                    viewport = Size(size.width.toFloat(), size.height.toFloat()),
                    sprite = sprite,
                    pan = panState.value,
                    zoom = zoomState.value,
                )

                racing {
                    sequence {
                        val initial = expectDown()
                        val startGeometry = geometry()
                        val startBounds = currentBounds.value
                        val hitSize = 20.dp.toPx()
                        val handle = startGeometry.handleCenters(startBounds)
                            .filterValues { center ->
                                abs(initial.position.x - center.x) <= hitSize &&
                                        abs(initial.position.y - center.y) <= hitSize
                            }
                            .minByOrNull { (_, center) ->
                                (initial.position - center).getDistance()
                            }
                            ?.key ?: unexpected(initial)
                        var traveled = 0f
                        var dragging = false

                        while (true) {
                            val change = expectFor(initial.id)
                            if (change.changedToUp()) {
                                if (!dragging) unexpected(change)
                                break
                            }
                            traveled += change.positionChange().getDistance()
                            if (!dragging && traveled > 10f) {
                                dragging = true
                                claim()
                            }
                            if (dragging) {
                                val delta = change.position - initial.position
                                val columns = (delta.x / startGeometry.cellSize).roundToInt()
                                val rows = (delta.y / startGeometry.cellSize).roundToInt()
                                boundsState.value = startBounds.moved(handle, columns, rows)
                                change.consume()
                            }
                        }
                    }
                    sequence {
                        var lastOffset: Offset? = null
                        var lastSize: Float? = null
                        val baseSize = fittedSpriteSize(
                            Size(size.width.toFloat(), size.height.toFloat()),
                            sprite,
                        )
                        val spriteOrigin = Offset(
                            (size.width - baseSize.width) / 2f,
                            (size.height - baseSize.height) / 2f,
                        )
                        followPinch { offset, size ->
                            val previousOffset = lastOffset ?: offset
                            val previousSize = lastSize ?: size
                            val zoomFactor = size / previousSize
                            panState.value += (offset - previousOffset) +
                                    (panState.value + spriteOrigin - previousOffset) * (zoomFactor - 1f)
                            zoomState.value *= zoomFactor
                            lastOffset = offset
                            lastSize = size
                        }
                    }
                }
            },
    ) {
        val geometry = resizeGeometry(size, sprite, pan, zoom)
        drawSprite(sprite, geometry, imagePaint)
        drawResizeOverlay(boundsState.value, geometry)
    }
}

private fun SpriteBounds.moved(
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

private fun resizeGeometry(
    viewport: Size,
    sprite: Sprite,
    pan: Offset,
    zoom: Float,
): ResizeGeometry {
    val fittedSize = fittedSpriteSize(viewport, sprite)
    return ResizeGeometry(
        origin = Offset(
            (viewport.width - fittedSize.width) / 2f,
            (viewport.height - fittedSize.height) / 2f,
        ) + pan,
        cellSize = fittedSize.width / sprite.width * zoom,
        viewport = viewport,
    )
}

private fun DrawScope.drawSprite(
    sprite: Sprite,
    geometry: ResizeGeometry,
    paint: Paint,
) {
    val left = floor(geometry.origin.x)
    val top = floor(geometry.origin.y)
    val right = floor(geometry.origin.x + sprite.width * geometry.cellSize)
    val bottom = floor(geometry.origin.y + sprite.height * geometry.cellSize)
    drawIntoCanvas { canvas ->
        canvas.drawImageRect(
            image = sprite.bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(sprite.width, sprite.height),
            dstOffset = IntOffset(left.toInt(), top.toInt()),
            dstSize = IntSize(
                (right - left).toInt().coerceAtLeast(1),
                (bottom - top).toInt().coerceAtLeast(1),
            ),
            paint = paint,
        )
    }
}

private fun DrawScope.drawResizeOverlay(bounds: SpriteBounds, geometry: ResizeGeometry) {
    val topLeft = geometry.position(bounds.left, bounds.top)
    val bottomRight = geometry.position(bounds.right, bounds.bottom)
    drawOutsideShade(topLeft, bottomRight)

    val remainingWidth = MaxSpriteWidth - bounds.width
    val remainingHeight = MaxSpriteHeight - bounds.height
    val maximumTopLeft = Offset(
        topLeft.x - remainingWidth * geometry.cellSize,
        topLeft.y - remainingHeight * geometry.cellSize,
    )
    val maximumBottomRight = Offset(
        bottomRight.x + remainingWidth * geometry.cellSize,
        bottomRight.y + remainingHeight * geometry.cellSize,
    )
    drawHatches(0f, 0f, size.width, maximumTopLeft.y)
    drawHatches(0f, maximumBottomRight.y, size.width, size.height)
    drawHatches(0f, maximumTopLeft.y, maximumTopLeft.x, maximumBottomRight.y)
    drawHatches(maximumBottomRight.x, maximumTopLeft.y, size.width, maximumBottomRight.y)

    val dots = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 5.dp.toPx()))
    val borderWidth = 2.dp.toPx()
    drawLine(
        Color.White,
        topLeft,
        Offset(bottomRight.x, topLeft.y),
        borderWidth,
        StrokeCap.Round,
        dots,
    )
    drawLine(
        Color.White,
        Offset(bottomRight.x, topLeft.y),
        bottomRight,
        borderWidth,
        StrokeCap.Round,
        dots,
    )
    drawLine(
        Color.White,
        bottomRight,
        Offset(topLeft.x, bottomRight.y),
        borderWidth,
        StrokeCap.Round,
        dots,
    )
    drawLine(
        Color.White,
        Offset(topLeft.x, bottomRight.y),
        topLeft,
        borderWidth,
        StrokeCap.Round,
        dots,
    )

    val handleSize = 12.dp.toPx()
    geometry.handleCenters(bounds).values.forEach { center ->
        drawRect(
            color = Color.White,
            topLeft = center - Offset(handleSize / 2f, handleSize / 2f),
            size = Size(handleSize, handleSize),
        )
        drawRect(
            color = Color.Black,
            topLeft = center - Offset(handleSize / 2f, handleSize / 2f),
            size = Size(handleSize, handleSize),
            style = Stroke(2.dp.toPx()),
        )
    }
}

private fun DrawScope.drawOutsideShade(topLeft: Offset, bottomRight: Offset) {
    val left = topLeft.x.coerceIn(0f, size.width)
    val right = bottomRight.x.coerceIn(0f, size.width)
    val top = topLeft.y.coerceIn(0f, size.height)
    val bottom = bottomRight.y.coerceIn(0f, size.height)
    val shade = Color.Black.copy(alpha = 0.58f)
    if (left >= right || top >= bottom) {
        drawRect(shade)
        return
    }
    drawRect(shade, size = Size(size.width, top))
    drawRect(shade, Offset(0f, bottom), Size(size.width, size.height - bottom))
    drawRect(shade, Offset(0f, top), Size(left, bottom - top))
    drawRect(shade, Offset(right, top), Size(size.width - right, bottom - top))
}

private fun DrawScope.drawHatches(left: Float, top: Float, right: Float, bottom: Float) {
    val clippedLeft = left.coerceIn(0f, size.width)
    val clippedRight = right.coerceIn(0f, size.width)
    val clippedTop = top.coerceIn(0f, size.height)
    val clippedBottom = bottom.coerceIn(0f, size.height)
    if (clippedLeft >= clippedRight || clippedTop >= clippedBottom) return

    clipRect(clippedLeft, clippedTop, clippedRight, clippedBottom) {
        val spacing = 10.dp.toPx()
        val height = clippedBottom - clippedTop
        var x = clippedLeft - height
        while (x < clippedRight) {
            drawLine(
                color = Palette.Red.color.copy(alpha = 0.7f),
                start = Offset(x, clippedBottom),
                end = Offset(x + height, clippedTop),
                strokeWidth = 2.dp.toPx(),
            )
            x += spacing
        }
    }
}
