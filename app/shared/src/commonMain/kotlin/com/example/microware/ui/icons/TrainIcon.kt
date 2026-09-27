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
import com.example.microware.game.sprite.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class TrainCloud(
    val x: Float,
    val y: Float,
    val scale: Float,
)

private data class TrainSteamPuff(
    val x: Float,
    val y: Float,
    val driftX: Float,
    val driftY: Float,
    val radius: Float,
    val delay: Float,
)

private val trainClouds = listOf(
    TrainCloud(.2f, .14f, .9f),
    TrainCloud(.72f, .22f, .7f),
)

private val trainSteamPuffs = listOf(
    TrainSteamPuff(.59f, .27f, -.08f, -.15f, .055f, .0f),
    TrainSteamPuff(.62f, .28f, .09f, -.2f, .05f, .18f),
    TrainSteamPuff(.57f, .28f, -.14f, -.24f, .045f, .34f),
)

internal fun DrawScope.drawTrainIcon(t: Float = 0f) {
    val strokeWidth = 2.dp.toPx()
    val point = { x: Float, y: Float -> Offset(size.width * x, size.height * y) }

    val cloudTravel = animate(t) {
        anim(-1f, 1f)
    }
    trainClouds.forEach { cloud ->
        drawTrainCloud(cloud.x + cloudTravel, cloud.y, cloud.scale, strokeWidth)
        drawTrainCloud(cloud.x + cloudTravel + 1f, cloud.y, cloud.scale, strokeWidth)
    }

    drawRoundRect(
        color = Palette.DarkGray.color,
        topLeft = point(.17f, .57f),
        size = Size(size.width * .67f, size.height * .12f),
        cornerRadius = CornerRadius(size.minDimension * .025f),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = point(.17f, .57f),
        size = Size(size.width * .67f, size.height * .12f),
        cornerRadius = CornerRadius(size.minDimension * .025f),
        style = Stroke(strokeWidth),
    )

    drawRect(
        color = Palette.Orange.color,
        topLeft = point(.2f, .34f),
        size = Size(size.width * .22f, size.height * .28f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.2f, .34f),
        size = Size(size.width * .22f, size.height * .27f),
        style = Stroke(strokeWidth / 1.5f),
    )
    drawPath(
        Path().apply {
            moveTo(point(.16f, .34f).x, point(.16f, .34f).y)
            lineTo(point(.46f, .34f).x, point(.46f, .34f).y)
            lineTo(point(.42f, .29f).x, point(.42f, .29f).y)
            lineTo(point(.2f, .29f).x, point(.2f, .29f).y)
            close()
        },
        Palette.Maroon.color,
    )
    drawLine(
        Color.Black,
        point(.16f, .34f),
        point(.46f, .34f),
        strokeWidth / 1.5f,
        StrokeCap.Round,
    )

    drawRect(
        color = Palette.Cyan.color,
        topLeft = point(.25f, .39f),
        size = Size(size.width * .1f, size.height * .1f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.25f, .39f),
        size = Size(size.width * .1f, size.height * .1f),
        style = Stroke(strokeWidth / 1.75f),
    )

    drawRoundRect(
        color = Palette.Rust.color,
        topLeft = point(.38f, .4f),
        size = Size(size.width * .43f, size.height * .2f),
        cornerRadius = CornerRadius(size.minDimension * .08f),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = point(.38f, .4f),
        size = Size(size.width * .43f, size.height * .2f),
        cornerRadius = CornerRadius(size.minDimension * .08f),
        style = Stroke(strokeWidth / 1.5f),
    )

    drawRoundRect(
        color = Palette.DarkGray.color,
        topLeft = point(.56f, .26f),
        size = Size(size.width * .1f, size.height * .17f),
        cornerRadius = CornerRadius(size.minDimension * .02f),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = point(.56f, .26f),
        size = Size(size.width * .1f, size.height * .17f),
        cornerRadius = CornerRadius(size.minDimension * .02f),
        style = Stroke(strokeWidth),
    )
    drawRoundRect(
        color = Palette.DarkGray.color,
        topLeft = point(.53f, .24f),
        size = Size(size.width * .16f, size.height * .05f),
        cornerRadius = CornerRadius(size.minDimension * .02f),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = point(.53f, .24f),
        size = Size(size.width * .16f, size.height * .05f),
        cornerRadius = CornerRadius(size.minDimension * .02f),
        style = Stroke(strokeWidth),
    )

    drawCircle(Palette.Yellow.color, size.minDimension * .045f, point(.78f, .43f))
    drawCircle(
        Color.Black,
        size.minDimension * .045f,
        point(.78f, .43f),
        style = Stroke(strokeWidth / 1.5f),
    )

    trainSteamPuffs.forEach { puff ->
        val localTime = t - puff.delay
        val alpha = animate(localTime) {
            anim(1f, .12f)
            anim(1f, .25f)
            anim(0f, .21f)
        }
        if (alpha <= 0f) return@forEach

        val travel = animate(localTime) {
            anim(1f, .58f)
        }
        val puffCenter = point(
            puff.x + puff.driftX * travel,
            puff.y + puff.driftY * travel,
        )
        val puffRadius = size.minDimension * puff.radius
        drawCircle(
            Palette.LightGray.color.copy(alpha = alpha),
            puffRadius,
            puffCenter,
        )
        drawCircle(
            Palette.DarkGray.color.copy(alpha = alpha),
            puffRadius,
            puffCenter,
            style = Stroke(strokeWidth / 1.5f),
        )
    }

    val wheelRotation = animate(t) {
        anim(360f, 1f)
    }
    val wheelRadius = size.minDimension * .105f
    val leftWheel = point(.3f, .72f)
    val rightWheel = point(.68f, .72f)
    drawTrainWheel(leftWheel, wheelRadius, wheelRotation, strokeWidth)
    drawTrainWheel(rightWheel, wheelRadius, wheelRotation, strokeWidth)

    val couplingAngle = wheelRotation * PI.toFloat() / 180f
    val crankRadius = wheelRadius * .55f
    val leftCrank = leftWheel + Offset(
        cos(couplingAngle) * crankRadius,
        sin(couplingAngle) * crankRadius,
    )
    val rightCrank = rightWheel + Offset(
        cos(couplingAngle) * crankRadius,
        sin(couplingAngle) * crankRadius,
    )
    drawLine(Palette.LightGray.color, leftCrank, rightCrank, strokeWidth, StrokeCap.Round)
    drawCircle(Palette.LightGray.color, strokeWidth * .75f, leftCrank)
    drawCircle(Palette.LightGray.color, strokeWidth * .75f, rightCrank)
}

private fun DrawScope.drawTrainCloud(
    centerX: Float,
    centerY: Float,
    scale: Float,
    strokeWidth: Float,
) {
    val center = Offset(size.width * centerX, size.height * centerY)
    val cloudWidth = size.width * .24f * scale
    val cloudHeight = size.height * .1f * scale
    val cloud = Path().apply {
        moveTo(center.x - cloudWidth * .5f, center.y + cloudHeight * .25f)
        quadraticTo(
            center.x - cloudWidth * .52f,
            center.y - cloudHeight * .2f,
            center.x - cloudWidth * .25f,
            center.y - cloudHeight * .15f,
        )
        quadraticTo(
            center.x - cloudWidth * .14f,
            center.y - cloudHeight * .75f,
            center.x + cloudWidth * .1f,
            center.y - cloudHeight * .48f,
        )
        quadraticTo(
            center.x + cloudWidth * .32f,
            center.y - cloudHeight * .7f,
            center.x + cloudWidth * .38f,
            center.y - cloudHeight * .1f,
        )
        quadraticTo(
            center.x + cloudWidth * .55f,
            center.y - cloudHeight * .05f,
            center.x + cloudWidth * .5f,
            center.y + cloudHeight * .25f,
        )
        close()
    }
    drawPath(cloud, Palette.LightGray.color)
    drawPath(cloud, Palette.DarkGray.color, style = Stroke(strokeWidth / 1.5f))
}

private fun DrawScope.drawTrainWheel(
    center: Offset,
    radius: Float,
    rotation: Float,
    strokeWidth: Float,
) {
    drawCircle(Color.Black, radius, center)
    drawCircle(Palette.Black.color, radius * .8f, center)
    rotate(rotation, pivot = center) {
        repeat(4) { index ->
            val angle = 2f * PI.toFloat() * index / 4f
            drawLine(
                Palette.DarkGray.color,
                center,
                center + Offset(
                    cos(angle) * radius * .65f,
                    sin(angle) * radius * .65f,
                ),
                strokeWidth / 1.5f,
                StrokeCap.Round,
            )
        }
    }
    drawCircle(Color.Black, radius, center, style = Stroke(strokeWidth / 1.5f))
}

@Preview
@Composable
fun TrainIconPreview() = Icon { drawTrainIcon() }

@Preview
@Composable
fun TrainAnimationPreview() = Animated { drawTrainIcon(it) }
