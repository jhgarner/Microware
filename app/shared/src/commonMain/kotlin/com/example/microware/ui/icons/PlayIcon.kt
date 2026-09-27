package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.game.sprite.Palette

internal fun DrawScope.drawPlayIcon() {
    val path = Path().apply {
        moveTo(size.width * .34f, size.height * .2f)
        lineTo(size.width * .76f, size.height * .5f)
        lineTo(size.width * .34f, size.height * .8f)
        close()
    }
    drawPath(path, Palette.Green.color)
    drawPath(path, Color.Black, style = Stroke(2.dp.toPx()))
}

@Preview
@Composable
fun PlayIconPreview() = Icon { drawPlayIcon() }
