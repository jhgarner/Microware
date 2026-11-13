package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.Palette

private const val RestingDistance = .28f

internal fun DrawScope.drawSeparationIcon() {
    drawSeparationIcon(0f)
}

internal fun DrawScope.drawSeparationIcon(t: Float) {
    val distance = animate(t, RestingDistance) {
        anim(.12f, .35f)
        anim(.36f, .2f)
        anim(.24f, .25f)
        anim(RestingDistance, .2f)
    }
    drawCircle(
        Palette.Cyan.color,
        size.minDimension * .2f,
        Offset(size.width * (.5f - distance), size.height * .5f),
    )
    drawCircle(
        Palette.Red.color,
        size.minDimension * .2f,
        Offset(size.width * (.5f + distance), size.height * .5f),
    )
}

@Preview
@Composable
fun SeparationIconPreview() = Icon { drawSeparationIcon() }

@Preview
@Composable
fun SeparationAnimationPreview() = Animated { drawSeparationIcon(it) }
