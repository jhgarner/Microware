package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.game.sprite.Palette

internal fun DrawScope.drawMoveToIcon() {
    drawCircle(
        Palette.Cyan.color,
        size.minDimension * .18f,
        Offset(size.width * .2f, size.height * .5f),
    )
    val arrowEnd = Offset(size.width * .68f, size.height * .5f)
    drawLine(Color.Black, Offset(size.width * .38f, size.height * .5f), arrowEnd, 2.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .55f, size.height * .38f), arrowEnd, 2.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .55f, size.height * .62f), arrowEnd, 2.dp.toPx())
    drawRect(
        Palette.Red.color,
        Offset(size.width * .7f, size.height * .32f),
        Size(size.width * .28f, size.height * .36f),
    )
}

@Preview
@Composable
fun MoveToIconPreview() = Icon { drawMoveToIcon() }
