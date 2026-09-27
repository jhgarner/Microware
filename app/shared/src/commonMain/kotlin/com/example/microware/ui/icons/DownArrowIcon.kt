package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawDownArrowIcon() {
    val centerX = size.width / 2f
    val end = Offset(centerX, size.height * .85f)
    val strokeWidth = 2.dp.toPx()
    drawLine(Color.Black, Offset(centerX, size.height * .1f), end, strokeWidth, StrokeCap.Round)
    drawLine(
        Color.Black,
        Offset(centerX - size.height * .22f, size.height * .62f),
        end,
        strokeWidth,
        StrokeCap.Round,
    )
    drawLine(
        Color.Black,
        Offset(centerX + size.height * .22f, size.height * .62f),
        end,
        strokeWidth,
        StrokeCap.Round,
    )
}

@Preview
@Composable
fun DownArrowIconPreview() = Icon { drawDownArrowIcon() }
