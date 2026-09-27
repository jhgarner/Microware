package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawIncrementIcon() {
    val side = size.minDimension
    val center = Offset(size.width / 2f, size.height / 2f)
    val halfLength = side * .35f
    drawLine(
        Color.Black,
        Offset(center.x, center.y - halfLength),
        Offset(center.x, center.y + halfLength),
        3.dp.toPx(),
    )
    drawLine(
        Color.Black,
        Offset(center.x - halfLength, center.y),
        Offset(center.x + halfLength, center.y),
        3.dp.toPx(),
    )
}

@Preview
@Composable
fun IncrementIconPreview() = Icon { drawIncrementIcon() }
