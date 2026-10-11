package com.example.microware.ui.icons

import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.model.Direction

internal fun DrawScope.drawMoveIcon(direction: Direction, t: Float = 0f) {
    rotate(direction.rotation) {
        val speed = animate(t) {
            hold(.06f)
            anim(1f, .08f)
            hold(.66f)
            anim(0f, .08f)
            hold()
        }
        val trailTravel = animate(t) {
            hold(.06f)
            anim(.12f, .08f, LinearEasing)
            anim(1.45f, .66f, LinearEasing)
            anim(1.58f, .08f, LinearEasing)
            hold()
        }
        val center = Offset(size.width * (.42f + speed * .1f), size.height * .5f)
        val strokeWidth = 2.dp.toPx()
        val arrowColor = Palette.Black.color.copy(alpha = 1f - speed)

        drawLine(
            arrowColor,
            Offset(size.width * .32f, size.height * .5f),
            Offset(size.width * .82f, size.height * .5f),
            strokeWidth,
            StrokeCap.Round,
        )
        drawLine(
            arrowColor,
            Offset(size.width * .68f, size.height * .34f),
            Offset(size.width * .82f, size.height * .5f),
            strokeWidth,
            StrokeCap.Round,
        )
        drawLine(
            arrowColor,
            Offset(size.width * .68f, size.height * .66f),
            Offset(size.width * .82f, size.height * .5f),
            strokeWidth,
            StrokeCap.Round,
        )

        drawSpeedTrails(center.x, trailTravel, speed, strokeWidth)
        drawMovementPuck(center, size.minDimension * .13f, strokeWidth)
    }
}

private fun DrawScope.drawSpeedTrails(
    puckX: Float,
    travel: Float,
    speed: Float,
    strokeWidth: Float,
) {
    val puckRear = puckX - size.minDimension * .16f
    val dashLength = size.width * (.05f + speed * .06f)
    speedTrails.forEach { trail ->
        repeat(6) { index ->
            val phase = (
                travel * trail.travelRate +
                    trail.phase +
                    index * trail.spacing
                ) % TrailCycle
            val endX = puckRear - size.width * phase
            val alpha = speed * (endX / size.width).coerceIn(0f, 1f)
            if (alpha > 0f) {
                drawLine(
                    Palette.Black.color.copy(alpha = alpha),
                    Offset(endX - dashLength, size.height * trail.y),
                    Offset(endX, size.height * trail.y),
                    strokeWidth,
                    StrokeCap.Round,
                )
            }
        }
    }
}

private data class SpeedTrail(
    val y: Float,
    val spacing: Float,
    val phase: Float,
    val travelRate: Float,
)

private val speedTrails = listOf(
    SpeedTrail(y = .36f, spacing = .24f, phase = .05f, travelRate = .95f),
    SpeedTrail(y = .5f, spacing = .28f, phase = .16f, travelRate = 1.08f),
    SpeedTrail(y = .64f, spacing = .22f, phase = .11f, travelRate = 1f),
)

private const val TrailCycle = 1.25f

internal fun DrawScope.drawMovementPuck(
    center: Offset,
    radius: Float,
    strokeWidth: Float = 2.dp.toPx(),
    alpha: Float = 1f,
) {
    drawCircle(Palette.Cyan.color.copy(alpha = alpha), radius, center)
    drawCircle(
        Palette.Black.color.copy(alpha = alpha),
        radius,
        center,
        style = Stroke(strokeWidth),
    )
}

internal fun DrawScope.drawTeleportSparkle(
    center: Offset,
    scale: Float = 1f,
    alpha: Float = 1f,
) {
    val longRadius = size.minDimension * .12f * scale
    val shortRadius = size.minDimension * .06f * scale
    val strokeWidth = 2.dp.toPx()
    drawLine(
        Palette.Black.color.copy(alpha = alpha),
        center - Offset(longRadius, 0f),
        center + Offset(longRadius, 0f),
        strokeWidth,
        StrokeCap.Round,
    )
    drawLine(
        Palette.Black.color.copy(alpha = alpha),
        center - Offset(0f, longRadius),
        center + Offset(0f, longRadius),
        strokeWidth,
        StrokeCap.Round,
    )
    drawLine(
        Palette.Black.color.copy(alpha = alpha),
        center - Offset(shortRadius, shortRadius),
        center + Offset(shortRadius, shortRadius),
        strokeWidth,
        StrokeCap.Round,
    )
    drawLine(
        Palette.Black.color.copy(alpha = alpha),
        center + Offset(shortRadius, -shortRadius),
        center + Offset(-shortRadius, shortRadius),
        strokeWidth,
        StrokeCap.Round,
    )
}

private val Direction.rotation: Float
    get() = when (this) {
        Direction.N -> -90f
        Direction.E -> 0f
        Direction.S -> 90f
        Direction.W -> 180f
    }

@Preview
@Composable
fun MoveIconPreview() = Icon { drawMoveIcon(Direction.E) }

@Preview
@Composable
fun MoveIconAnimationPreview() = Animated { drawMoveIcon(Direction.E, it) }
