package com.example.microware.editor.sprite.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun bounce(progress: Float) = sin(progress * PI).toFloat()

private fun DrawScope.dashedSquare(color: Color, inset: Float) {
    drawRect(
        color = color,
        topLeft = Offset(size.width * inset, size.height * inset),
        size = Size(size.width * (1 - 2 * inset), size.height * (1 - 2 * inset)),
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
        ),
    )
}

internal fun DrawScope.drawSelectToolIcon() {
    dashedSquare(Color.Black, inset = .2f)
    drawCircle(Color.Black, 3.dp.toPx(), Offset(size.width * .8f, size.height * .8f))
}

internal fun DrawScope.drawDeselectIcon(progress: Float) {
    scale(1f - bounce(progress) * .25f) {
        dashedSquare(Palette.Navy.color, inset = .15f)
        val stroke = 3.dp.toPx()
        val near = .35f
        val far = .65f
        drawLine(
            Palette.Red.color,
            Offset(size.width * near, size.height * near),
            Offset(size.width * far, size.height * far),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            Palette.Red.color,
            Offset(size.width * far, size.height * near),
            Offset(size.width * near, size.height * far),
            stroke,
            StrokeCap.Round,
        )
    }
}

internal fun DrawScope.drawCopyIcon(progress: Float) {
    val square = Size(size.width * .5f, size.height * .5f)
    val back = Offset(size.width * .15f, size.height * .15f)
    val front = back + Offset(size.width * .2f, size.height * .2f) * (1f + bounce(progress) * .6f)
    drawRect(Palette.Navy.color, back, square, style = Stroke(2.dp.toPx()))
    drawRect(Palette.Cyan.color, front, square)
    drawRect(Palette.Navy.color, front, square, style = Stroke(2.dp.toPx()))
}

internal fun DrawScope.drawRotateIcon(clockwise: Boolean, progress: Float) {
    val direction = if (clockwise) 1f else -1f
    scale(scaleX = direction, scaleY = 1f) {
        val radius = size.minDimension * .38f
        val stroke = 3.dp.toPx()
        val endAngle = -30f
        drawArc(
            color = Palette.Navy.color,
            startAngle = endAngle - 270f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        val radians = endAngle / 180f * PI.toFloat()
        val tip = center + Offset(cos(radians), sin(radians)) * radius
        val forward = Offset(-sin(radians), cos(radians)) * size.minDimension * .16f
        val outward = Offset(cos(radians), sin(radians)) * size.minDimension * .14f
        val head = Path().apply {
            moveTo(tip.x - forward.x + outward.x, tip.y - forward.y + outward.y)
            lineTo(tip.x + forward.x * .3f, tip.y + forward.y * .3f)
            lineTo(tip.x - forward.x - outward.x, tip.y - forward.y - outward.y)
        }
        drawPath(head, Palette.Navy.color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    rotate(degrees = direction * 90f * easeInOut(progress)) {
        val square = size.minDimension * .3f
        drawRect(
            color = Palette.Cyan.color,
            topLeft = center - Offset(square / 2, square / 2),
            size = Size(square, square),
        )
    }
}

internal fun DrawScope.drawFlipIcon(vertical: Boolean, progress: Float) {
    rotate(if (vertical) 90f else 0f) {
        drawLine(
            color = Palette.Navy.color,
            start = Offset(center.x, size.height * .1f),
            end = Offset(center.x, size.height * .9f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
        )
        val triangle = Path().apply {
            moveTo(size.width * .4f, size.height * .25f)
            lineTo(size.width * .4f, size.height * .75f)
            lineTo(size.width * .1f, size.height * .75f)
            close()
        }
        scale(scaleX = cos(progress * 2 * PI).toFloat(), scaleY = 1f) {
            drawPath(triangle, Palette.Cyan.color)
            drawPath(triangle, Palette.Navy.color, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}

private fun easeInOut(progress: Float) = (1 - cos(progress * PI).toFloat()) / 2
