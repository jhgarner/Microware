package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.Palette
import kotlin.math.min
import kotlin.math.pow

const val TAP_START = 0f
const val TAP_DOWN = .3f

fun DrawScope.drawTapCircle(t: Float) {
    val touchPoint = Offset(size.width * .5f, size.height * .35f)
    val radScale = animate(t, 1.0f) {
        anim(1.0f, .2f)
        anim(0.8f, .1f)
        hold(.3f)
        anim(1.3f, .2f)
        anim(1.0f, .2f)
    }
    val strokeWidth = size.width / 20
    val outerCircleSize = size.minDimension * .38f * radScale
    drawCircle(
        Palette.Cyan.color,
        outerCircleSize,
        touchPoint,
    )

    drawCircle(
        Palette.OffWhite.color,
        min(size.minDimension * .28f * radScale.pow(4), outerCircleSize * .9f),
        touchPoint,
    )
}

fun DrawScope.drawTapHand(t: Float) {
    val fingerMove = animate(t) {
        anim(0f, .2f)
        anim(.2f, .1f)
        hold(.3f)
        anim(0f, .2f)
    }
    val finger = Path().apply {
        moveTo(size.width * .5f, size.height * .82f)
        lineTo(size.width * .34f, size.height * .62f)
        lineTo(size.width * .34f, size.height * .5f)
        quadraticTo(size.width * .34f, size.height * .43f, size.width * .42f, size.height * .5f)
        lineTo(size.width * .42f, size.height * (.22f + fingerMove))
        quadraticTo(
            size.width * .42f,
            size.height * (.12f + fingerMove),
            size.width * .5f,
            size.height * (.12f + fingerMove),
        )
        quadraticTo(
            size.width * .58f,
            size.height * (.12f + fingerMove),
            size.width * .58f,
            size.height * (.22f + fingerMove),
        )
        lineTo(size.width * .58f, size.height * .48f)
        quadraticTo(size.width * .82f, size.height * .43f, size.width * .82f, size.height * .62f)
        lineTo(size.width * .72f, size.height * .82f)
        close()
    }
    drawPath(finger, Palette.OffWhite.color)
    val strokeWidth = size.width / 20
    drawPath(finger, Color.Black, style = Stroke(strokeWidth))
}

fun DrawScope.drawTapIcon(t: Float = TAP_START) {
    drawTapCircle(t)
    drawTapHand(t)
}

fun DrawScope.drawDownIcon(t: Float = 0f) {
    val t = (t + TAP_DOWN) % 1f
    drawTapCircle(t)
    drawTapHand(t)
}

@Preview
@Composable
fun StartOfHand() = Icon { drawTapHand(TAP_START) }

@Preview
@Composable
fun TapOfHand() = Icon { drawTapHand(TAP_DOWN) }

@Preview
@Composable
fun FullTapAnimation() = Animated { drawTapIcon(it) }

@Preview
@Composable
fun FullDownAnimation() = Animated { drawDownIcon(it) }

@Preview
@Composable
fun TapIconPreview() = Icon { drawTapIcon(TAP_START) }

@Preview
@Composable
fun TapCirclePreview() = Icon { drawTapCircle(TAP_START) }

@Preview
@Composable
fun TapCircleAnimationPreview() = Animated { drawTapCircle(it) }
