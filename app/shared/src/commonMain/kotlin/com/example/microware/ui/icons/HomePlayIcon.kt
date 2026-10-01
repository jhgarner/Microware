package com.example.microware.ui.icons

import androidx.compose.animation.core.EaseInQuint
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

private class Item(val x: Float, val y: Float, val scale: Float, val deg: Float)

private val fails = listOf(
    Item(0.7f, 0.4f, 1.4f, 0f),
    Item(0.11f, 0.2f, 3f, 0f),
    Item(0.42f, 0.1f, 2.5f, 0f),
)

private val trophies = listOf(
    Item(0.4f, 0.4f, 2.0f, 30f),
    Item(0.05f, 0.2f, 1.5f, -30f),
    Item(0.2f, 0.1f, 1.75f, 10f),
)

fun DrawScope.drawHomePlayIcon(t: Float) {
    val playT = animate(t / 0.2f) { anim(1f, until(1f)) }
    within(fromPercents(Rect(Offset(0.27f, 0.2f), Size(0.5f, 0.5f)))) {
        drawStartIcon(lerp(0f, 1f, playT))
    }

    val failMove = animate((t - 0.2f) / 0.4f, 0f) {
        anim(1f, 0.5f, ease = EaseOutQuint)
        anim(0f, until(1f), EaseInQuint)
    }
    for (fail in fails) {
        within(
            fromPercents(
                Rect(
                    Offset(fail.x, lerp(1.2f, fail.y, failMove)),
                    Size(0.1f, 0.1f) * fail.scale,
                ),
            ),
        ) {
            rotate(fail.deg) {
                drawLoseIcon()
            }
        }
    }

    val trophyAnim = lerp(0f, 1f, (t - 0.6f) / 0.4f)
    val trophyMove = animate((t - 0.6f) / 0.4f, 0f) {
        anim(1f, 0.5f, ease = EaseOutQuint)
        anim(0f, until(1f), EaseInQuint)
    }
    for (trophy in trophies) {
        within(
            fromPercents(
                Rect(
                    Offset(trophy.x, lerp(1.2f, trophy.y, trophyMove)),
                    Size(0.3f, 0.3f) * trophy.scale,
                ),
            ),
        ) {
            rotate(trophy.deg) {
                drawWinIcon(trophyAnim)
            }
        }
    }
}

@Composable
private fun PreviewCanvas(t: Float) = Canvas(
    modifier = Modifier.width(150.dp).height(150.dp),
) {
    drawHomePlayIcon(t)
}

@Preview
@Composable
private fun StartState() = PreviewCanvas(0f)

@Preview
@Composable
private fun Anim() =
    Animated(duration = 3000, size = DpSize(150.dp, 150.dp)) { drawHomePlayIcon(it) }
