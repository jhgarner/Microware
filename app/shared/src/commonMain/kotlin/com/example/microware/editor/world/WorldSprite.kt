package com.example.microware.editor.world

import com.example.microware.game.model.Obj
import com.example.microware.game.sprite.Palette
import com.example.microware.game.sprite.Sprite
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.gestures.dragSource
import com.example.microware.util.toDp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
internal fun WorldSprite(
    obj: Obj,
    scale: Float,
    origin: IntOffset = IntOffset.Zero,
    opacity: Float = 1f,
    from: Any,
    dragController: DragController<Obj>? = null,
) {
    val width = (obj.sprite.width * scale).roundToInt().coerceAtLeast(1)
    val height = (obj.sprite.height * scale).roundToInt().coerceAtLeast(1)
    val isFullyTransparent = remember(obj.sprite) { obj.sprite.isFullyTransparent() }
    val imagePaint = remember { Paint().apply { filterQuality = FilterQuality.None } }
    val density = LocalDensity.current
    val animatedOpacity by animateFloatAsState(
        targetValue = opacity,
        animationSpec = tween(durationMillis = 120),
        label = "sprite opacity",
    )

    Canvas(
        modifier = Modifier
            .requiredSize(
                with(density) { width.toDp() },
                with(density) { height.toDp() },
            )
            .offset {
                IntOffset(
                    origin.x + (obj.offset.x * scale).roundToInt(),
                    origin.y + (obj.offset.y * scale).roundToInt()
                )
            }
            .alpha(animatedOpacity)
            .then(dragController?.let { Modifier.dragSource(it, from, obj) } ?: Modifier)
    ) {
        drawWorldSprite(
            sprite = obj.sprite,
            origin = Offset.Zero,
            pixelSize = scale,
            paint = imagePaint,
            outlineColor = if (isFullyTransparent) Palette.Black.color else null,
            outlineWidth = if (isFullyTransparent) 1.dp.toPx() else 0f,
        )
    }
}

internal fun DrawScope.drawWorldSprite(
    sprite: Sprite,
    origin: Offset,
    pixelSize: Float,
    paint: Paint,
    outlineColor: Color? = null,
    outlineWidth: Float = 0f,
) {
    val left = floor(origin.x)
    val top = floor(origin.y)
    val right = floor(origin.x + sprite.width * pixelSize)
    val bottom = floor(origin.y + sprite.height * pixelSize)
    val destination = IntOffset(left.toInt(), top.toInt())
    val destinationSize = IntSize(
        (right - left).toInt().coerceAtLeast(1),
        (bottom - top).toInt().coerceAtLeast(1),
    )

    drawIntoCanvas { canvas ->
        canvas.drawImageRect(
            image = sprite.bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(sprite.width, sprite.height),
            dstOffset = destination,
            dstSize = destinationSize,
            paint = paint,
        )
    }

    if (outlineColor != null && outlineWidth > 0f) {
        drawRect(
            color = outlineColor,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            style = Stroke(outlineWidth),
        )
    }
}
