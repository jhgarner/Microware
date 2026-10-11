package com.example.microware.ui.icons

import androidx.compose.animation.core.EaseInOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import kotlin.math.PI
import kotlin.math.sin

private const val MoveToResetStart = .84f

internal fun DrawScope.drawMoveToIcon(t: Float = 0f, teleport: Boolean = false) {
    val strokeWidth = 2.dp.toPx()
    val start = Offset(size.width * .2f, size.height * .63f)
    val target = Offset(size.width * .77f, size.height * .55f)

    if (t < MoveToResetStart) {
        drawMoveToScene(start, target, teleport, targetPulse(t), strokeWidth)
        drawMoveToAction(t, start, target, teleport, strokeWidth)
        return
    }

    val reset = EaseInOut.transform((t - MoveToResetStart) / (1f - MoveToResetStart))
    drawMoveToScene(start, target, teleport, 1f, strokeWidth)
    drawMovementPuck(
        target + (start - target) * reset,
        size.minDimension * .13f,
        strokeWidth,
    )
}

private fun DrawScope.drawMoveToScene(
    start: Offset,
    target: Offset,
    teleport: Boolean,
    targetScale: Float,
    strokeWidth: Float,
) {
    drawCircle(
        Palette.Red.color,
        size.minDimension * .2f * targetScale,
        target,
        style = Stroke(strokeWidth),
    )
    drawCircle(
        Palette.Red.color,
        size.minDimension * .075f * targetScale,
        target,
    )

    if (teleport) {
        drawTeleportSparkle(
            Offset(size.width * .48f, size.height * .32f),
            scale = .65f,
        )
    } else {
        for (step in 1..4) {
            drawCircle(
                Palette.Black.color,
                strokeWidth * .65f,
                curvedPoint(start, target, step / 5f),
            )
        }
    }
}

private fun DrawScope.drawMoveToAction(
    t: Float,
    start: Offset,
    target: Offset,
    teleport: Boolean,
    strokeWidth: Float,
) {
    if (teleport) {
        when {
            t < .12f -> drawMovementPuck(start, size.minDimension * .13f, strokeWidth)
            t < .25f -> {
                drawTeleportSparkle(start, .8f)
                drawTeleportSparkle(target, .8f)
            }
            else -> drawMovementPuck(target, size.minDimension * .13f, strokeWidth)
        }
        return
    }

    val center = when {
        t < .08f -> start
        t < .48f -> curvedPoint(start, target, (t - .08f) / .4f)
        else -> target
    }
    drawMovementPuck(center, size.minDimension * .13f, strokeWidth)
}

private fun targetPulse(t: Float): Float = animate(t, 1f) {
    hold(.38f)
    anim(.82f, .08f)
    anim(1.08f, .1f)
    anim(1f, .1f)
    hold()
}

private fun DrawScope.curvedPoint(start: Offset, target: Offset, progress: Float): Offset {
    val direct = start + (target - start) * progress
    return direct - Offset(0f, sin(progress * PI).toFloat() * size.height * .2f)
}

@Preview
@Composable
fun MoveToIconPreview() = Icon { drawMoveToIcon() }

@Preview
@Composable
fun MoveToAnimationPreview() = Animated { drawMoveToIcon(it) }

@Preview
@Composable
fun TeleportToAnimationPreview() = Animated { drawMoveToIcon(it, teleport = true) }
