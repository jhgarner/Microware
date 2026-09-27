package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.game.sprite.Palette

internal fun DrawScope.drawLoseIcon() {
    drawCircle(Palette.Red.color, size.minDimension * .5f, center)
    drawCloseIcon()
}

private fun DrawScope.drawCloseIcon() {
    drawLine(
        Color.Black,
        Offset(size.width * .2f, size.height * .2f),
        Offset(size.width * .8f, size.height * .8f),
        3.dp.toPx(),
    )
    drawLine(
        Color.Black,
        Offset(size.width * .8f, size.height * .2f),
        Offset(size.width * .2f, size.height * .8f),
        3.dp.toPx(),
    )
}

@Preview
@Composable
fun LoseIconPreview() = Icon { drawLoseIcon() }
