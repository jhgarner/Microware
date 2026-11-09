package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** How far, in radians, the holder turns their head to each side. */
private const val LookAngle = .25f

/** How much of the turn the eyepieces follow: 0 holds them still, 1 turns them rigidly with the lenses. */
private const val EyepieceFollow = .75f

/** The camera's angle, in radians, above the binoculars: 0 looks straight along them, π/2 straight down. */
private const val CameraElevation = .5f

/** How much nearer parts grow and farther parts shrink. */
private const val Perspective = .4f

private const val BarrelX = .24f
private const val EyepieceZ = .25f
private const val LensZ = -.3f
private const val EyepieceRadius = .11f
private const val LensRadius = .18f
private const val EyecupSize = .8f

private val Body = Palette.DarkGray.color
private val Wheel = Palette.LightGray.color
private val Eyecup = Palette.LightGray.color

/**
 * A point on the binoculars seen from the top, where x points right and z points toward the
 * viewer. Each barrel's axis runs along z.
 */
private data class Point(val x: Float, val z: Float) {
    val scale get() = 1f + z * Perspective

    /**
     * Rotates around the vertical axis through the eyepieces' midpoint: move that pivot to the
     * origin, apply a 2D rotation in the x-z plane, then move it back.
     */
    fun turned(angle: Float): Point {
        val depth = z - EyepieceZ
        return Point(
            x = x * cos(angle) + depth * sin(angle),
            z = EyepieceZ - x * sin(angle) + depth * cos(angle),
        )
    }
}

/**
 * Binoculars seen from slightly above and behind, looking into the screen. They look left, then
 * right, as if their holder turns their head.
 */
fun DrawScope.drawBinoculars(t: Float) {
    val yaw = animate(t) {
        anim(-1f, .2f)
        hold(.1f)
        anim(1f, .25f)
        hold(.1f)
        anim(0f, until(1f))
    }
    val lensAngle = yaw * LookAngle
    val eyepieceAngle = lensAngle * EyepieceFollow
    val unit = size.minDimension

    /** Seen from above at [CameraElevation], farther points rise up the screen. */
    fun Point.onScreen() = center + Offset(x, z * sin(CameraElevation)) * unit

    /** A barrel's round end, which faces along the barrel and so narrows as it turns away. */
    fun end(radius: Float, point: Point) =
        Size(2 * radius * cos(lensAngle), 2 * radius * cos(CameraElevation)) * point.scale * unit

    val barrels = listOf(-BarrelX, BarrelX).map { x ->
        Point(x, EyepieceZ).turned(eyepieceAngle) to Point(x, LensZ).turned(lensAngle)
    }
    val (hingeLeft, hingeRight) = barrels.map { (eyepiece, lens) ->
        lerp(eyepiece.onScreen(), lens.onScreen(), .5f)
    }
    drawLine(Body, hingeLeft, hingeRight, .14f * unit, StrokeCap.Round)

    barrels.sortedBy { (eyepiece, _) -> eyepiece.z }.forEach { (eyepiece, lens) ->
        val eyepieceEnd = end(EyepieceRadius, eyepiece)
        drawBarrel(eyepiece.onScreen(), eyepieceEnd, lens.onScreen(), end(LensRadius, lens))
        drawCenteredOval(Eyecup, eyepiece.onScreen(), eyepieceEnd * EyecupSize)
    }
}

/** Fills the barrel between its two round ends, which share an aspect ratio. */
private fun DrawScope.drawBarrel(eyepiece: Offset, eyepieceEnd: Size, lens: Offset, lensEnd: Size) {
    val aspect = eyepieceEnd.width / eyepieceEnd.height
    fun Offset.squashed() = Offset(x / aspect, y)
    fun Offset.stretched() = Offset(x * aspect, y)

    val (left, right) = listOf(-1f, 1f).map { side ->
        outerTangent(eyepiece.squashed(), eyepieceEnd.height / 2, lens.squashed(), lensEnd.height / 2, side)
            .let { (onEyepiece, onLens) -> onEyepiece.stretched() to onLens.stretched() }
    }
    val sides = Path().apply {
        moveTo(left.first.x, left.first.y)
        lineTo(left.second.x, left.second.y)
        lineTo(right.second.x, right.second.y)
        lineTo(right.first.x, right.first.y)
        close()
    }
    drawPath(sides, Body)
    drawCenteredOval(Body, lens, lensEnd)
    drawCenteredOval(Body, eyepiece, eyepieceEnd)
}

/**
 * Where a line touching both circles on the same [side] (-1 or 1) meets each of them. The line's
 * normal leans toward the smaller circle just enough to be [fromRadius] from one center and
 * [toRadius] from the other.
 */
private fun outerTangent(from: Offset, fromRadius: Float, to: Offset, toRadius: Float, side: Float): Pair<Offset, Offset> {
    val distance = (to - from).getDistance()
    val along = (to - from) / distance
    val across = Offset(-along.y, along.x)
    val lean = ((fromRadius - toRadius) / distance).coerceIn(-1f, 1f)
    val normal = along * lean + across * side * sqrt(1 - lean * lean)
    return (from + normal * fromRadius) to (to + normal * toRadius)
}

private fun DrawScope.drawFocusWheel(center: Offset, unit: Float) {
    val halfWidth = Offset(.05f * unit, 0f)
    drawLine(Wheel, center - halfWidth, center + halfWidth, .08f * unit, StrokeCap.Round)
    for (ridge in listOf(-.025f, 0f, .025f)) {
        val x = Offset(ridge * unit, 0f)
        val halfHeight = Offset(0f, .025f * unit)
        drawLine(
            Body,
            center + x - halfHeight,
            center + x + halfHeight,
            .01f * unit,
            StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawCenteredOval(color: Color, center: Offset, size: Size) =
    drawOval(color, topLeft = center - Offset(size.width, size.height) / 2f, size = size)

private val PreviewSize = DpSize(128.dp, 112.dp)

@Preview
@Composable
fun BinocularsPreview() = Animated(size = PreviewSize) { drawBinoculars(0f) }

@Preview
@Composable
fun BinocularsLeftPreview() = Animated(size = PreviewSize) { drawBinoculars(.3f) }

@Preview
@Composable
fun BinocularsRightPreview() = Animated(size = PreviewSize) { drawBinoculars(.75f) }

@Preview
@Composable
fun BinocularsAnimatedPreview() =
    Animated(duration = 4800, size = PreviewSize) { drawBinoculars(it) }
