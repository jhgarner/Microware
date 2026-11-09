package com.example.microware.ui.icons

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.lerp
import com.example.microware.Palette

private sealed interface Anim {
    val firstStart: Float get() = 0f;
    val secondStart: Float get() = 0f;
    val firstEnd: Float get() = 1f;
    val secondEnd: Float get() = 1f;

    val total: Float;

    class First(t: Float) : Anim {
        override val firstEnd = t
        override val secondEnd = 0f
        override val total = t
    }

    class Second(t: Float) : Anim {
        override val secondEnd = t
        override val total = t + 1f
    }

    class OutFirst(t: Float) : Anim {
        override val firstStart = t
        override val total = t + 2f
    }

    class OutSecond(t: Float) : Anim {
        override val firstStart = 1f
        override val secondStart = t
        override val total = t + 3f
    }
}

private fun animLerp(lhs: Anim, rhs: Anim, t: Float): Anim =
    when (val result = lerp(lhs.total, rhs.total, t)) {
        in 0f..1f -> Anim.First(result)
        in 1f..2f -> Anim.Second(result - 1f)
        in 2f..3f -> Anim.OutFirst(result - 2f)
        else -> Anim.OutSecond(result - 3f)
    }

fun DrawScope.drawCheckmarkIcon(t: Float) {
    val path = animate(t, Anim.First(0f), ::animLerp) {
        anim(Anim.First(1f), .25f, EaseIn)
        anim(Anim.Second(1f), .5f, EaseOutQuint)
        anim(Anim.OutSecond(1f), until(1f), EaseInOut)
    }

    if (path.firstEnd > 0f && path.firstStart < 1f) {
        val firstLineStart = fromPercents(Offset(.25f, .5f))
        val firstLineEnd = fromPercents(Offset(.5f, .75f))
        drawLine(
            Palette.Green.color,
            strokeWidth = .1f * size.width,
            cap = StrokeCap.Round,
            start = firstLineEnd + (firstLineStart - firstLineEnd) * (1f - path.firstStart),
            end = firstLineStart + (firstLineEnd - firstLineStart) * path.firstEnd,
        )
    }

    if (path.secondEnd > 0f && path.secondStart < 1f) {
        val secondLineStart = fromPercents(Offset(.5f, .75f))
        val secondLineEnd = fromPercents(Offset(.75f, .25f))
        drawLine(
            Palette.Green.color,
            strokeWidth = .1f * size.width,
            cap = StrokeCap.Round,
            start = secondLineEnd + (secondLineStart - secondLineEnd) * (1f - path.secondStart),
            end = secondLineStart + (secondLineEnd - secondLineStart) * path.secondEnd,
        )
    }
}

@Preview
@Composable
private fun CheckmarkPreviewStart() = Icon { drawCheckmarkIcon(0f) }

@Preview
@Composable
private fun CheckmarkPreviewEnd() = Icon { drawCheckmarkIcon(1f) }

@Preview
@Composable
private fun CheckmarkPreviewAnimated() = Animated { drawCheckmarkIcon(it) }
