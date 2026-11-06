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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.sprite.MaxSpriteHeight
import com.example.microware.game.sprite.MaxSpriteWidth
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteBounds
import com.example.microware.ui.gestures.racing
import com.example.microware.ui.gestures.sequence
import kotlin.math.roundToInt

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
                fun viewport() = Size(size.width.toFloat(), size.height.toFloat())
                fun geometry() = spriteGeometry(viewport(), sprite, panState.value, zoomState.value)

                racing {
                    sequence {
                        val initial = expectDown()
                        val startGeometry = geometry()
                        val startBounds = currentBounds.value
                        val handle = startGeometry.handleAt(initial.position, startBounds, 20.dp.toPx())
                            ?: unexpected(initial)
                        followDrag(initial) { change ->
                            change.consume()
                            val delta = change.position - initial.position
                            val columns = (delta.x / startGeometry.cellSize).roundToInt()
                            val rows = (delta.y / startGeometry.cellSize).roundToInt()
                            boundsState.value = startBounds.moved(handle, columns, rows)
                        }
                    }
                    sequence {
                        followPinchZoom(panState, zoomState, spriteGeometry(viewport(), sprite).origin)
                    }
                }
            },
    ) {
        val geometry = spriteGeometry(size, sprite, pan, zoom)
        drawPixels(sprite.bitmap, geometry, imagePaint)
        drawResizeOverlay(boundsState.value, geometry)
    }
}

private fun DrawScope.drawResizeOverlay(bounds: SpriteBounds, geometry: SpriteGeometry) {
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

    drawBoundsFrame(bounds, geometry)
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
