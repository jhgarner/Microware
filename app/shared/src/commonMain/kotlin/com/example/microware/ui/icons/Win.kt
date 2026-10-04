package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette

private data class ConfettiPiece(
    val startX: Float,
    val startY: Float,
    val driftX: Float,
    val driftY: Float,
    val length: Float,
    val thickness: Float,
    val rotation: Float,
    val color: Color,
    val delay: Float,
)

private val winConfetti = listOf(
    ConfettiPiece(.3f, .2f, -.3f, -.48f, .09f, .035f, -25f, Palette.Red.color, .02f),
    ConfettiPiece(.4f, .18f, -.16f, -.66f, .07f, .03f, 20f, Palette.Cyan.color, .08f),
    ConfettiPiece(.5f, .16f, .02f, -.78f, .08f, .035f, 75f, Palette.Orange.color, .04f),
    ConfettiPiece(.6f, .18f, .2f, -.6f, .07f, .03f, -45f, Palette.Purple.color, .1f),
    ConfettiPiece(.7f, .2f, .34f, -.43f, .09f, .035f, 35f, Palette.Green.color, .06f),
    ConfettiPiece(.35f, .21f, -.5f, -.3f, .06f, .03f, 60f, Palette.Pink.color, .14f),
    ConfettiPiece(.65f, .21f, .48f, -.28f, .06f, .03f, -70f, Palette.Yellow.color, .12f),
)

fun DrawScope.drawWinIcon(t: Float = 0f) {
    val strokeWidth = 2.dp.toPx()
    val left = size.width * .27f
    val right = size.width * .73f
    val top = size.height * .2f
    val bottom = size.height * .6f

    val leftHandle = Path().apply {
        moveTo(left + strokeWidth, size.height * .28f)
        quadraticTo(size.width * .1f, size.height * .23f, size.width * .14f, size.height * .42f)
        quadraticTo(size.width * .17f, size.height * .53f, left + strokeWidth, size.height * .48f)
    }
    val rightHandle = Path().apply {
        moveTo(right - strokeWidth, size.height * .28f)
        quadraticTo(size.width * .9f, size.height * .23f, size.width * .86f, size.height * .42f)
        quadraticTo(size.width * .83f, size.height * .53f, right - strokeWidth, size.height * .48f)
    }
    drawPath(leftHandle, Color.Black, style = Stroke(strokeWidth))
    drawPath(rightHandle, Color.Black, style = Stroke(strokeWidth))

    val cup = Path().apply {
        moveTo(left, top)
        lineTo(right, top)
        lineTo(size.width * .69f, size.height * .4f)
        quadraticTo(size.width * .66f, bottom, size.width * .5f, bottom)
        quadraticTo(size.width * .34f, bottom, size.width * .31f, size.height * .4f)
        close()
    }
    drawPath(cup, Palette.Yellow.color)
    drawPath(cup, Color.Black, style = Stroke(strokeWidth))
    drawLine(
        Color.Black,
        Offset(size.width * .5f, bottom),
        Offset(size.width * .5f, size.height * .76f),
        strokeWidth,
        StrokeCap.Round,
    )
    drawRoundRect(
        color = Palette.Yellow.color,
        topLeft = Offset(size.width * .32f, size.height * .74f),
        size = Size(size.width * .36f, size.height * .1f),
        cornerRadius = CornerRadius(strokeWidth),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = Offset(size.width * .32f, size.height * .74f),
        size = Size(size.width * .36f, size.height * .1f),
        cornerRadius = CornerRadius(strokeWidth),
        style = Stroke(strokeWidth),
    )

    winConfetti.forEach { piece ->
        val localTime = t - piece.delay
        val alpha = animate(localTime) {
            anim(0f, .08f)
            anim(1f, .12f)
            anim(1f, .44f)
            anim(0f, .16f)
        }
        if (alpha <= 0f) return@forEach

        val travel = animate(localTime) {
            anim(0f, .12f)
            anim(1f, .62f)
            anim(1.1f, .16f)
        }
        val rotation = animate(localTime, piece.rotation) {
            anim(piece.rotation + 180f, .28f)
            anim(piece.rotation + 360f, .62f)
        }
        val confettiCenter = Offset(
            size.width * (piece.startX + piece.driftX * travel),
            size.height * (piece.startY + piece.driftY * travel),
        )
        rotate(rotation, pivot = confettiCenter) {
            drawRoundRect(
                color = piece.color.copy(alpha = alpha),
                topLeft = Offset(
                    confettiCenter.x - size.width * piece.length / 2f,
                    confettiCenter.y - size.height * piece.thickness / 2f,
                ),
                size = Size(
                    size.width * piece.length,
                    size.height * piece.thickness,
                ),
                cornerRadius = CornerRadius(size.height * piece.thickness / 2f),
            )
        }
    }
}

@Preview
@Composable
fun StaticWinIcon() = Icon { drawWinIcon(0f) }

@Preview
@Composable
fun FullWinAnimation() = Animated { drawWinIcon(it) }
