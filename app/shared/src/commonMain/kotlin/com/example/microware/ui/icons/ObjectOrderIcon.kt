package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.game.sprite.Palette

internal fun DrawScope.drawObjectOrderIcon() {
    val stroke = 2.dp.toPx()
    val layers = listOf(
        Palette.Cyan.color to .24f,
        Palette.Orange.color to .44f,
        Palette.Green.color to .64f,
    )
    layers.forEach { (color, top) ->
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * .2f, size.height * top),
            size = Size(size.width * .6f, size.height * .16f),
            cornerRadius = CornerRadius(stroke * 2f),
        )
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(size.width * .2f, size.height * top),
            size = Size(size.width * .6f, size.height * .16f),
            cornerRadius = CornerRadius(stroke * 2f),
            style = Stroke(stroke),
        )
    }
}

@Preview
@Composable
fun ObjectOrderIconPreview() = Icon { drawObjectOrderIcon() }
