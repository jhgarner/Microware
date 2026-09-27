package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private fun DrawScope.drawEditIcon() {
    val strokeWidth = 3.dp.toPx()
    drawLine(
        Color.Black,
        Offset(size.width * .2f, size.height * .8f),
        Offset(size.width * .75f, size.height * .25f),
        strokeWidth,
        StrokeCap.Round,
    )
    drawLine(
        Color.Black,
        Offset(size.width * .68f, size.height * .18f),
        Offset(size.width * .82f, size.height * .32f),
        strokeWidth,
        StrokeCap.Round,
    )
    drawCircle(Color.Black, strokeWidth * .65f, Offset(size.width * .18f, size.height * .82f))
}

@Preview
@Composable
fun EditIconPreview() = Icon { drawEditIcon() }
