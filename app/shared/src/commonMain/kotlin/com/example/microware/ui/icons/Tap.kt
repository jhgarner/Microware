package com.example.microware.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.editor.logic.animate
import com.example.microware.game.sprite.Palette

const val TAP_START = 0f
const val TAP_DOWN = .3f

fun DrawScope.drawTapCircle(t: Float) {
    val touchPoint = Offset(size.width * .5f, size.height * .2f)
    val radScale = animate(t, 1.0f) {
        anim(1.0f, .3f)
        anim(1.4f, .2f)
        anim(1.0f, .2f)
    }
    val strokeWidth = size.width / 20
    drawCircle(
        Palette.Cyan.color,
        size.minDimension * .18f * radScale,
        touchPoint,
        style = Stroke(strokeWidth),
    )
}

fun DrawScope.drawTapHand(t: Float) {
    val fingerMove = animate(t) {
        anim(0f, .2f)
        anim(.1f, .1f)
        anim(.1f, .3f)
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

@Preview
@Composable
fun StartOfHand() = Icon { drawTapHand(TAP_START) }

@Preview
@Composable
fun TapOfHand() = Icon { drawTapHand(TAP_DOWN) }

@Preview
@Composable
fun FullTapAnimation() = Animated { drawTapIcon(it) }

