package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.microware.game.sprite.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun DrawScope.drawStartIcon(t: Float = 0f) {
    for (i in 0..<8) {
        val delay = lerp(0.2f, 0.4f, i / 8f)
        val endX = cos(2f * PI.toFloat() / 8f * i - PI.toFloat() / 2f)
        val endY = sin(2f * PI.toFloat() / 8f * i - PI.toFloat() / 2f)
        val shineStart = animate(t - delay, 0.5f) {
            anim(.5f, .3f)
            anim(1.1f, .2f)
        }
        val shineEnd = animate(t - delay, 0.5f) {
            anim(1.1f, .3f)
            anim(1.1f, .2f)
        }
        lineCenter(
            Palette.Orange,
            Offset(shineStart * endX, shineStart * endY),
            Offset(shineEnd * endX, shineEnd * endY),
            10f,
        )
    }
    val compress = animate(t) {
        anim(0.09f, .1f)
        anim(-.15f, .2f)
        anim(-.15f, .5f)
        anim(0.09f, .2f)
        anim(0f, .1f)
    }
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * (.15f - compress), size.height * (.15f - compress))
        lineTo(size.width * (.85f + compress), size.height * .5f)
        lineTo(size.width * (.15f - compress), size.height * (.85f + compress))
        close()
    }
    drawPath(path, Palette.Green.color)
    drawPath(path, Color.Black, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.lineCenter(color: Palette, start: Offset, end: Offset, width: Float) {
    val halfSize = Offset(size.width, size.height) * .5f
    drawLine(
        color.color,
        Offset(start.x * size.width, start.y * size.height) * .5f + halfSize,
        Offset(end.x * size.width, end.y * size.height) * .5f + halfSize,
        width,
    )
}

@Preview
@Composable
fun StartIconPreview() = Icon { drawStartIcon() }

@Preview
@Composable
fun StartAnimationPreview() = Animated { drawStartIcon(it) }
