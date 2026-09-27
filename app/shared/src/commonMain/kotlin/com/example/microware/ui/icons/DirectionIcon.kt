package com.example.microware.ui.icons

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.game.model.Direction

private data class DirectionStar(
    val crossAxisPosition: Float,
    val travelOffset: Float,
    val radius: Float,
)

internal fun DrawScope.drawDirectionIcon(direction: Direction, t: Float = 0f) {
    val travelAxis = when (direction) {
        Direction.N -> Offset(0f, -1f)
        Direction.S -> Offset(0f, 1f)
        Direction.E -> Offset(1f, 0f)
        Direction.W -> Offset(-1f, 0f)
    }
    val stars = listOf(
        DirectionStar(-.08f, -.5f, .016f),
        DirectionStar(.08f, -.65f, .018f),
        DirectionStar(.92f, -.35f, .024f),
        DirectionStar(1.08f, -.2f, .02f),
        DirectionStar(.3f, -.05f, .015f),
        DirectionStar(.7f, .25f, .02f),
        DirectionStar(-.04f, .4f, .013f),
        DirectionStar(.16f, .55f, .016f),
        DirectionStar(.84f, .82f, .022f),
        DirectionStar(1.04f, .95f, .018f),
    )
    val starAlpha = animate(t) {
        anim(1f, .15f)
        anim(1f, .65f)
        anim(0f, .2f)
    }
    val starTravel = animate(t, 1.25f) {
        anim(-.25f, 1f)
    }
    val starElongation = animate(t) {
        anim(0f, .15f)
        anim(.12f, .15f)
        anim(.12f, .5f)
        anim(0f, .2f)
    }
    stars.forEach { star ->
        if (starAlpha > 0f) {
            val travelPosition = starTravel + star.travelOffset
            val starCenter = when (direction) {
                Direction.N -> Offset(
                    size.width * star.crossAxisPosition,
                    size.height * (1f - travelPosition),
                )

                Direction.S -> Offset(
                    size.width * star.crossAxisPosition,
                    size.height * travelPosition,
                )

                Direction.E -> Offset(
                    size.width * travelPosition,
                    size.height * star.crossAxisPosition,
                )

                Direction.W -> Offset(
                    size.width * (1f - travelPosition),
                    size.height * star.crossAxisPosition,
                )
            }
            val starColor = Color.Black.copy(alpha = starAlpha)
            val starRadius = size.minDimension * star.radius
            if (starElongation == 0f) {
                drawCircle(starColor, starRadius, starCenter)
            } else {
                val halfLength = size.minDimension * starElongation / 2f
                drawLine(
                    color = starColor,
                    start = starCenter - travelAxis * halfLength,
                    end = starCenter + travelAxis * halfLength,
                    strokeWidth = starRadius * 2f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    val (baseStart, baseEnd, baseArrowHead) = when (direction) {
        Direction.N -> Triple(
            Offset(size.width * .5f, size.height * .88f),
            Offset(size.width * .5f, size.height * .12f),
            listOf(
                Offset(size.width * .25f, size.height * .38f),
                Offset(size.width * .75f, size.height * .38f),
            ),
        )

        Direction.S -> Triple(
            Offset(size.width * .5f, size.height * .12f),
            Offset(size.width * .5f, size.height * .88f),
            listOf(
                Offset(size.width * .25f, size.height * .62f),
                Offset(size.width * .75f, size.height * .62f),
            ),
        )

        Direction.E -> Triple(
            Offset(size.width * .12f, size.height * .5f),
            Offset(size.width * .88f, size.height * .5f),
            listOf(
                Offset(size.width * .62f, size.height * .25f),
                Offset(size.width * .62f, size.height * .75f),
            ),
        )

        Direction.W -> Triple(
            Offset(size.width * .88f, size.height * .5f),
            Offset(size.width * .12f, size.height * .5f),
            listOf(
                Offset(size.width * .38f, size.height * .25f),
                Offset(size.width * .38f, size.height * .75f),
            ),
        )
    }
    val arrowElongation = animate(t) {
        anim(.06f, .18f)
        anim(.1f, .22f)
        anim(.1f, .4f)
        anim(0f, .2f)
    }
    val arrowExtension = travelAxis * (size.minDimension * arrowElongation)
    val start = baseStart - arrowExtension
    val end = baseEnd + arrowExtension
    val arrowHead = baseArrowHead.map { it + arrowExtension }
    val strokeWidth = 3.dp.toPx()
    drawLine(Color.Black, start, end, strokeWidth, StrokeCap.Round)
    arrowHead.forEach { point ->
        drawLine(Color.Black, point, end, strokeWidth, StrokeCap.Round)
    }
}

internal fun DrawScope.drawArrowIcon() = drawDirectionIcon(Direction.E)

@Preview
@Composable
fun DirectionIconPreview() {
    Row {
        Direction.entries.forEach { direction ->
            Icon { drawDirectionIcon(direction) }
        }
    }
}

@Preview
@Composable
fun DirectionAnimationPreview() = Animated { drawDirectionIcon(Direction.E, it) }

@Preview
@Composable
fun ArrowIconPreview() = Icon { drawArrowIcon() }
