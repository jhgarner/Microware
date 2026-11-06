package com.example.microware.editor.sprite

import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.microware.game.sprite.Sprite
import com.example.microware.ui.gestures.PointerScope
import kotlin.math.floor

/** Maps between sprite cells and screen positions for a panned and zoomed sprite. */
internal data class SpriteGeometry(
    val origin: Offset,
    val cellSize: Float,
    val viewport: Size,
) {
    fun position(col: Int, row: Int) = Offset(
        x = origin.x + col * cellSize,
        y = origin.y + row * cellSize,
    )

    fun cellAt(position: Offset) = Cell(
        row = floor((position.y - origin.y) / cellSize).toInt(),
        col = floor((position.x - origin.x) / cellSize).toInt(),
    )
}

internal fun spriteGeometry(
    viewport: Size,
    sprite: Sprite,
    pan: Offset = Offset.Zero,
    zoom: Float = 1f,
) = spriteGeometry(viewport, sprite.width, sprite.height, pan, zoom)

internal fun spriteGeometry(
    viewport: Size,
    width: Int,
    height: Int,
    pan: Offset = Offset.Zero,
    zoom: Float = 1f,
): SpriteGeometry {
    val fittedSize = fittedSize(viewport, width, height)
    return SpriteGeometry(
        origin = Offset(
            (viewport.width - fittedSize.width) / 2f,
            (viewport.height - fittedSize.height) / 2f,
        ) + pan,
        cellSize = fittedSize.width / width * zoom,
        viewport = viewport,
    )
}

/** Draws [image] with its top-left pixel at [cell], snapping its edges to the screen pixel grid. */
internal fun DrawScope.drawPixels(
    image: ImageBitmap,
    geometry: SpriteGeometry,
    paint: Paint,
    cell: Cell = Cell(0, 0),
) {
    val topLeft = geometry.position(cell.col, cell.row)
    val bottomRight = geometry.position(cell.col + image.width, cell.row + image.height)
    val left = floor(topLeft.x).toInt()
    val top = floor(topLeft.y).toInt()
    drawIntoCanvas { canvas ->
        canvas.drawImageRect(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(
                (floor(bottomRight.x).toInt() - left).coerceAtLeast(1),
                (floor(bottomRight.y).toInt() - top).coerceAtLeast(1),
            ),
            paint = paint,
        )
    }
}

/** Pinch to pan and zoom, keeping the content under the fingers stationary. */
internal suspend fun PointerScope.followPinchZoom(
    panState: MutableState<Offset>,
    zoomState: MutableState<Float>,
    spriteOrigin: Offset,
) {
    var lastOffset: Offset? = null
    var lastSize: Float? = null
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
