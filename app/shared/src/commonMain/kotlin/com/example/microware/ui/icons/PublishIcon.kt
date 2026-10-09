package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.Palette

internal fun DrawScope.drawPublishIcon(t: Float = 0f) {
    val drift = animate(t) {
        anim(1f, .4f)
        anim(0f, .4f)
        hold()
    }
    val strokeWidth = size.minDimension * .035f
    val stroke = Stroke(strokeWidth, cap = StrokeCap.Round)
    translate(top = -drift * size.height * .025f) {
        rotate(-18f + drift * 6f) {
            drawLine(
                Palette.Navy.color,
                fromPercents(Offset(.25f, .59f)),
                fromPercents(Offset(.75f, .59f)),
                strokeWidth = strokeWidth * 2f,
            )
            for (x in listOf(.08f, .65f)) {
                drawRect(
                    Palette.Cyan.color,
                    fromPercents(Offset(x, .46f)),
                    fromPercents(Size(.27f, .26f)),
                )
                drawRect(
                    Palette.Navy.color,
                    fromPercents(Offset(x, .46f)),
                    fromPercents(Size(.27f, .26f)),
                    style = stroke,
                )
                for (column in 1..2) {
                    val gridX = x + .09f * column
                    drawLine(
                        Palette.Navy.color,
                        fromPercents(Offset(gridX, .46f)),
                        fromPercents(Offset(gridX, .72f)),
                        strokeWidth = strokeWidth * .6f,
                    )
                }
                drawLine(
                    Palette.Navy.color,
                    fromPercents(Offset(x, .59f)),
                    fromPercents(Offset(x + .27f, .59f)),
                    strokeWidth = strokeWidth * .6f,
                )
            }
            drawRoundRect(
                Palette.Yellow.color,
                fromPercents(Offset(.39f, .43f)),
                fromPercents(Size(.22f, .32f)),
                cornerRadius = CornerRadius(size.minDimension * .04f),
            )
            drawRoundRect(
                Palette.Navy.color,
                fromPercents(Offset(.39f, .43f)),
                fromPercents(Size(.22f, .32f)),
                cornerRadius = CornerRadius(size.minDimension * .04f),
                style = stroke,
            )
            drawLine(
                Palette.Navy.color,
                fromPercents(Offset(.5f, .39f)),
                fromPercents(Offset(.5f, .43f)),
                strokeWidth = strokeWidth,
            )
            drawArc(
                Palette.OffWhite.color,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = fromPercents(Offset(.35f, .09f)),
                size = fromPercents(Size(.30f, .30f)),
            )
            drawArc(
                Palette.Navy.color,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = fromPercents(Offset(.35f, .09f)),
                size = fromPercents(Size(.30f, .30f)),
                style = stroke,
            )
            drawLine(
                Palette.Navy.color,
                fromPercents(Offset(.5f, .31f)),
                fromPercents(Offset(.5f, .21f)),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawCircle(
                Palette.Orange.color,
                radius = strokeWidth,
                center = fromPercents(Offset(.5f, .21f)),
            )
            repeat(3) { wave ->
                val progress = (t + wave / 3f + 1f / 6f) % 1f
                val radius = .06f + .18f * progress
                val opacity = (progress / .15f).coerceIn(0f, 1f) *
                    ((1f - progress) / .4f).coerceIn(0f, 1f)
                drawArc(
                    Palette.Cyan.color.copy(alpha = opacity),
                    startAngle = 230f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = fromPercents(Offset(.5f - radius, .24f - radius)),
                    size = fromPercents(Size(radius * 2f, radius * 2f)),
                    style = stroke,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PublishIconPreview() = Icon { drawPublishIcon() }

@Preview
@Composable
private fun PublishIconAnimationPreview() = Animated { drawPublishIcon(it) }
