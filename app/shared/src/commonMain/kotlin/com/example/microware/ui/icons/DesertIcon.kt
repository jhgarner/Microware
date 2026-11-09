package com.example.microware.ui.icons

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.microware.Palette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val Sand = Palette.LightGray.color
private val Shade = Palette.DarkGray.color

/**
 * A monochrome desert whose sun, dunes, and cactus [rise] into view while a tumbleweed
 * [roll]s across the front dune.
 */
fun DrawScope.drawDesert(rise: Float, roll: Float) {
    val sun = animate(rise) { hold(.3f); anim(1f, .7f, EaseOutBack) }
    val backDune = animate(rise) { anim(1f, .6f, EaseOutCubic); hold() }
    val frontDune = animate(rise) { hold(.15f); anim(1f, .6f, EaseOutCubic); hold() }
    val cactus = animate(rise) { hold(.45f); anim(1f, .45f, EaseOutBack); hold() }

    withFadedEdges {
        drawCircle(
            Sand,
            radius = .14f * size.height,
            center = fromPercents(Offset(.72f, lerp(.9f, .3f, sun))),
        )
        translate(top = (1f - backDune) * size.height) {
            drawDune(left = .72f, peak = Offset(.3f, .48f), right = .66f, color = Sand)
            scale(cactus, pivot = fromPercents(Offset(.32f, .82f))) { drawCactus(x = .32f) }
        }
        translate(top = (1f - frontDune) * size.height) {
            drawDune(left = FrontDuneLeft, peak = Offset(.5f, FrontDunePeak), right = FrontDuneRight, color = Shade)
            drawTumbleweed(roll)
        }
    }
}

/** Draws [content] so it fades out toward the left, right, and bottom edges instead of ending abruptly. */
private fun DrawScope.withFadedEdges(content: DrawScope.() -> Unit) {
    val opaque = Palette.Black.color
    val clear = Palette.Clear.color
    drawContext.canvas.saveLayer(size.toRect(), Paint())
    content()
    drawRect(Brush.horizontalGradient(0f to clear, .25f to opaque, .75f to opaque, 1f to clear), blendMode = BlendMode.DstIn)
    drawRect(Brush.verticalGradient(.5f to opaque, 1f to clear), blendMode = BlendMode.DstIn)
    drawContext.canvas.restore()
}

private const val FrontDuneLeft = .86f
private const val FrontDunePeak = .7f
private const val FrontDuneRight = .88f

/** The front dune's height at [x], where its quadratic curve happens to have x(t) = t. */
private fun frontDuneY(x: Float): Float =
    (1 - x) * (1 - x) * FrontDuneLeft + 2 * x * (1 - x) * FrontDunePeak + x * x * FrontDuneRight

private fun DrawScope.drawDune(left: Float, peak: Offset, right: Float, color: Color) {
    val dune = Path().apply {
        moveTo(0f, left * size.height)
        quadraticTo(peak.x * size.width, peak.y * size.height, size.width, right * size.height)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(dune, color)
}

private fun DrawScope.drawCactus(x: Float) {
    val unit = size.height
    val trunk = x * size.width
    fun at(dx: Float, y: Float) = Offset(trunk + dx * unit, y * unit)
    val cactus = Path().apply {
        moveTo(at(0f, .86f))
        lineTo(at(0f, .48f))
        moveTo(at(-.11f, .56f))
        lineTo(at(-.11f, .66f))
        lineTo(at(0f, .66f))
        moveTo(at(.1f, .5f))
        lineTo(at(.1f, .6f))
        lineTo(at(0f, .6f))
    }
    drawPath(cactus, Shade, style = Stroke(.08f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawTumbleweed(roll: Float) {
    val radius = .085f * size.height
    val x = lerp(-.1f, 1.1f, roll)
    val hop = abs(sin(roll * 3 * PI.toFloat())) * .08f * size.height
    val center = Offset(x * size.width, frontDuneY(x.coerceIn(0f, 1f)) * size.height - radius - hop)
    drawTumbleweed(center, radius, turnDegrees = roll * 1080f)
}

/**
 * One open loop of the tumbleweed. Every loop touches the bottom of the outermost one, [size] is
 * relative to that outermost loop, and angles are clockwise degrees from 3 o'clock.
 */
private data class Strand(val size: Float, val startDegrees: Float, val sweepDegrees: Float, val twigDegrees: List<Float>)

private val TumbleweedStrands = listOf(
    Strand(size = 1f, startDegrees = 330f, sweepDegrees = 310f, twigDegrees = listOf(20f, 150f, 215f, 270f)),
    Strand(size = .7f, startDegrees = 285f, sweepDegrees = 300f, twigDegrees = listOf(210f, 320f, 40f)),
    Strand(size = .45f, startDegrees = 330f, sweepDegrees = 280f, twigDegrees = listOf(140f, 240f)),
)
private const val TwigLength = .3f
private const val TwigLeanDegrees = 35f

private fun DrawScope.drawTumbleweed(center: Offset, radius: Float, turnDegrees: Float) {
    val stroke = Stroke(.14f * radius, cap = StrokeCap.Round)
    val ground = center + Offset(0f, radius)
    rotate(turnDegrees, pivot = center) {
        for (strand in TumbleweedStrands) drawStrand(strand, ground, radius, stroke)
    }
}

private fun DrawScope.drawStrand(strand: Strand, ground: Offset, fullRadius: Float, stroke: Stroke) {
    val radius = strand.size * fullRadius
    val center = ground - Offset(0f, radius)
    drawArc(
        Shade,
        startAngle = strand.startDegrees,
        sweepAngle = strand.sweepDegrees,
        useCenter = false,
        topLeft = center - Offset(radius, radius),
        size = Size(2 * radius, 2 * radius),
        style = stroke,
    )
    strand.twigDegrees.forEachIndexed { index, degrees ->
        val lean = if (index % 2 == 0) TwigLeanDegrees else -TwigLeanDegrees
        val root = center + direction(degrees) * radius
        val tip = root + direction(degrees + lean) * (TwigLength * fullRadius)
        drawLine(Shade, root, tip, stroke.width, StrokeCap.Round)
    }
}

private fun direction(degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    return Offset(cos(radians), sin(radians))
}

private fun Path.moveTo(offset: Offset) = moveTo(offset.x, offset.y)
private fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)

@Preview
@Composable
fun DesertPreview() = Animated(size = DpSize(240.dp, 160.dp)) { drawDesert(rise = 1f, roll = .6f) }

@Preview
@Composable
fun DesertRisingPreview() = Animated(size = DpSize(240.dp, 160.dp)) { drawDesert(rise = .5f, roll = 0f) }

@Preview
@Composable
fun DesertAnimatedPreview() =
    Animated(duration = 5000, size = DpSize(240.dp, 160.dp)) { drawDesert(rise = 1f, roll = it) }

@Preview
@Composable
fun TumbleweedPreview() = Icon(size = 120.dp) { drawTumbleweed(center, radius = .3f * size.width, turnDegrees = 0f) }
