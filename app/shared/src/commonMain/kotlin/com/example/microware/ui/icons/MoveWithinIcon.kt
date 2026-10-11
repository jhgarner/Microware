package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette

private const val WithinEntryDuration = .28f
private const val WithinResetStart = .84f

private val withinStops = listOf(
    Offset(.55f, .38f),
    Offset(.75f, .62f),
    Offset(.57f, .69f),
)

internal fun DrawScope.drawMoveWithinIcon(t: Float = 0f, teleport: Boolean = false) {
    val strokeWidth = 2.dp.toPx()
    val start = Offset(size.width * .17f, size.height * .55f)
    val stops = withinStops.map { Offset(size.width * it.x, size.height * it.y) }

    drawMoveWithinScene(stops, teleport, strokeWidth)
    drawMoveWithinAction(t, start, stops, teleport, strokeWidth)
}

private fun DrawScope.drawMoveWithinScene(
    stops: List<Offset>,
    teleport: Boolean,
    strokeWidth: Float,
) {
    drawRoundRect(
        color = Palette.Red.color,
        topLeft = Offset(size.width * .38f, size.height * .2f),
        size = Size(size.width * .53f, size.height * .63f),
        cornerRadius = CornerRadius(size.minDimension * .06f),
        style = Stroke(strokeWidth),
    )
    stops.forEach { stop ->
        drawCircle(
            Palette.Red.color,
            size.minDimension * .035f,
            stop,
        )
    }
    if (teleport) {
        drawTeleportSparkle(
            Offset(size.width * .27f, size.height * .28f),
            scale = .45f,
        )
    }
}

private fun DrawScope.drawMoveWithinAction(
    t: Float,
    start: Offset,
    stops: List<Offset>,
    teleport: Boolean,
    strokeWidth: Float,
) {
    if (teleport) {
        stops.forEach { stop ->
            drawTeleportEntry(t, start, stop, strokeWidth)
        }
    } else {
        stops.forEach { stop ->
            var alpha = 1f
            val loc = animate(t, start) {
                anim(stop, .5f) { alpha = 1f - it / 2f }
                hold(.4f)
                anim(start, .1f) { alpha = .5f + it / 2f }
            }
            drawPuck(loc, strokeWidth, alpha = alpha)
        }
    }
}

private fun DrawScope.drawTeleportEntry(
    t: Float,
    start: Offset,
    destination: Offset,
    strokeWidth: Float,
) {
    val t = animate(t) {
        hold(.1f)
        anim(1f, .1f)
        hold(until(.9f))
        anim(2f, until(1f))
    }
    when {
        t == 0f -> drawPuck(start, strokeWidth)
        t < 1f -> {
            drawTeleportSparkle(start, .7f)
            drawTeleportSparkle(destination, .7f)
        }

        else -> drawPuck(
            center = lerp(destination, start, t - 1f),
            strokeWidth,
            alpha = .5f + (t - 1f) / 2f,
        )
    }
}

private fun DrawScope.drawPuck(center: Offset, strokeWidth: Float, alpha: Float = 1f) {
    drawMovementPuck(center, size.minDimension * .115f, strokeWidth, alpha)
}

@Preview
@Composable
fun MoveWithinIconPreview() = Icon { drawMoveWithinIcon() }

@Preview
@Composable
fun MoveWithinAnimationPreview() = Animated { drawMoveWithinIcon(it) }

@Preview
@Composable
fun TeleportWithinAnimationPreview() = Animated { drawMoveWithinIcon(it, teleport = true) }
