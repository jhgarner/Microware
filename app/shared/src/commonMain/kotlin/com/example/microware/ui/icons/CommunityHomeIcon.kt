package com.example.microware.ui.icons

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInQuart
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
import kotlin.random.Random

val hands: List<Offset> = mutableListOf<Offset>().apply {
    val rand = Random(31)
    repeat(40) {
        add(Offset(rand.nextFloat(), rand.nextFloat()))
    }
}

fun DrawScope.drawCommunityHomeIcon(t: Float) {
    val escapeY = animate(t / 0.55f) {
        anim(.3f, until(0.4f), EaseInOutCubic)
        anim(-2f, until(1f), EaseInCubic)
    }
    within(fromPercents(Rect(Offset(.22f, .3f + escapeY * 0.7f), Size(0.5f, 0.5f)))) {
        drawTapHand(TAP_START)
    }
    within(fromPercents(Rect(Offset(-.10f, .4f + escapeY * 1.8f), Size(0.5f, 0.5f)))) {
        rotate(-30f) {
            drawTapHand(TAP_START)
        }
    }
    within(fromPercents(Rect(Offset(.55f, .33f + escapeY * 1.2f), Size(0.5f, 0.5f)))) {
        rotate(30f) {
            drawTapHand(TAP_START)
        }
    }

    val y = animate((t) / .9f, 0f) { anim(1f, until(1f), EaseInQuart) }
    for ((i, hand) in hands.withIndex()) {
        within(
            fromPercents(
                Rect(
                    hand - Offset(.18f, y * (((i - 0f) / 020f) + 1f) * 3f - 1.5f),
                    Size(0.3f, 0.3f),
                ),
            ),
        ) {
            drawTapHand(TAP_START)
        }
    }

    val enterY = animate((t - 0.75f) / 0.25f, 1.5f) {
        anim(0f, until(1f), EaseInOutCubic)
    }
    within(fromPercents(Rect(Offset(.22f, .3f + enterY), Size(0.5f, 0.5f)))) {
        drawTapHand(TAP_START)
    }
    within(fromPercents(Rect(Offset(-.10f, .4f + enterY), Size(0.5f, 0.5f)))) {
        rotate(-30f) {
            drawTapHand(TAP_START)
        }
    }
    within(fromPercents(Rect(Offset(.55f, .33f + enterY), Size(0.5f, 0.5f)))) {
        rotate(30f) {
            drawTapHand(TAP_START)
        }
    }
}

@Composable
private fun PreviewCanvas(t: Float) = Canvas(
    modifier = Modifier.width(150.dp).height(150.dp),
) {
    drawCommunityHomeIcon(t)
}

@Preview
@Composable
private fun StartState() = PreviewCanvas(0f)

@Preview
@Composable
private fun Anim() =
    Animated(duration = 3000, size = DpSize(150.dp, 150.dp)) { drawCommunityHomeIcon(it) }
