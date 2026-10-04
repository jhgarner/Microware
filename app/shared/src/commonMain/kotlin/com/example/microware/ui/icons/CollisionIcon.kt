package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.Palette

internal fun DrawScope.drawCollisionIcon() {
    drawCollisionIcon(0f)
}

internal fun DrawScope.drawCollisionIcon(t: Float) {
    val separation = animate(t, 0f) {
        anim(.45f, 0.4f)
        anim(.05f, .15f)
        anim(.35f, .3f)
        anim(.0f, .15f)
    }
    drawCircle(
        Palette.Cyan.color,
        size.minDimension * .25f,
        Offset(size.width * (.36f - separation), size.height * .5f),
    )
    drawCircle(
        Palette.Red.color,
        size.minDimension * .25f,
        Offset(size.width * (.64f + separation), size.height * .5f),
    )
}

@Preview
@Composable
fun CollisionIconPreview() = Icon { drawCollisionIcon() }

@Preview
@Composable
fun CollisionAnimationPreview() = Animated { drawCollisionIcon(it) }
