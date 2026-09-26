package com.example.microware.editor.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.microware.game.model.Obj
import com.example.microware.game.model.World
import com.example.microware.game.model.WorldHeight
import com.example.microware.game.model.WorldWidth
import com.example.microware.game.sprite.Palette
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val PreviewWorldPixels = 24f

@Composable
internal fun ObjectPositionPreviewCanvas(
    world: World,
    selectedObject: Obj,
    position: IntOffset,
    modifier: Modifier = Modifier,
) {
    val imagePaint = remember { Paint().apply { filterQuality = FilterQuality.None } }
    val pixelSizeRange = with(LocalDensity.current) {
        6.dp.toPx()..18.dp.toPx()
    }

    Canvas(modifier.clipToBounds()) {
        if (size.minDimension <= 0f) return@Canvas

        drawRect(Palette.LightGray.color)

        val pixelSize = (min(size.width, size.height) / PreviewWorldPixels)
            .coerceIn(pixelSizeRange.start, pixelSizeRange.endInclusive)
        val focusedCenter = Offset(
            position.x + selectedObject.sprite.width / 2f,
            position.y + selectedObject.sprite.height / 2f,
        )
        val worldOrigin = Offset(size.width / 2f, size.height / 2f) - focusedCenter * pixelSize

        var selectedObjectDrawn = false
        world.objects.forEach { worldObject ->
            val displayedObject = if (worldObject.id == selectedObject.id) {
                selectedObjectDrawn = true
                selectedObject.copy(offset = Offset(position.x.toFloat(), position.y.toFloat()))
            } else {
                worldObject
            }
            drawWorldSprite(
                sprite = displayedObject.sprite,
                origin = worldOrigin + displayedObject.offset * pixelSize,
                pixelSize = pixelSize,
                paint = imagePaint,
            )
        }

        val positionedObject = selectedObject.copy(
            offset = Offset(position.x.toFloat(), position.y.toFloat()),
        )
        if (!selectedObjectDrawn) {
            drawWorldSprite(
                sprite = positionedObject.sprite,
                origin = worldOrigin + positionedObject.offset * pixelSize,
                pixelSize = pixelSize,
                paint = imagePaint,
            )
        }

        drawPixelGrid(worldOrigin, pixelSize)

        val objectOrigin = worldOrigin + Offset(position.x * pixelSize, position.y * pixelSize)
        val left = floor(objectOrigin.x)
        val top = floor(objectOrigin.y)
        val right = floor(objectOrigin.x + selectedObject.sprite.width * pixelSize)
        val bottom = floor(objectOrigin.y + selectedObject.sprite.height * pixelSize)
        val objectTopLeft = Offset(left, top)
        val objectSize = Size(right - left, bottom - top)
        drawRect(
            color = Palette.OffWhite.color,
            topLeft = objectTopLeft,
            size = objectSize,
            style = Stroke(width = 4.dp.toPx()),
        )
        drawRect(
            color = Palette.Navy.color,
            topLeft = objectTopLeft,
            size = objectSize,
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

private fun DrawScope.drawPixelGrid(
    worldOrigin: Offset,
    pixelSize: Float,
) {
    val gridColor = Palette.Navy.color.copy(alpha = .2f)
    val worldTop = max(0f, worldOrigin.y)
    val worldLeft = max(0f, worldOrigin.x)
    val worldBottom = min(size.height, worldOrigin.y + WorldHeight * pixelSize)
    val worldRight = min(size.width, worldOrigin.x + WorldWidth * pixelSize)
    val firstColumn = ceil(-worldOrigin.x / pixelSize).toInt()
    val lastColumn = floor((size.width - worldOrigin.x) / pixelSize).toInt()
    val firstRow = ceil(-worldOrigin.y / pixelSize).toInt()
    val lastRow = floor((size.height - worldOrigin.y) / pixelSize).toInt()

    for (column in firstColumn..lastColumn) {
        if (column in 0..WorldWidth) {
            val x = floor(worldOrigin.x + column * pixelSize)
            drawLine(gridColor, Offset(x, worldTop), Offset(x, worldBottom), 1.dp.toPx())
        }
    }
    for (row in firstRow..lastRow) {
        if (row in 0..WorldHeight) {
            val y = floor(worldOrigin.y + row * pixelSize)
            drawLine(gridColor, Offset(worldLeft, y), Offset(worldRight, y), 1.dp.toPx())
        }
    }
}
