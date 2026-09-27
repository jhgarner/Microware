package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private fun DrawScope.drawCheckIcon() {
    drawLine(
        Color.Black,
        Offset(size.width * .15f, size.height * .52f),
        Offset(size.width * .42f, size.height * .78f),
        3.dp.toPx(),
    )
    drawLine(
        Color.Black,
        Offset(size.width * .42f, size.height * .78f),
        Offset(size.width * .88f, size.height * .2f),
        3.dp.toPx(),
    )
}

@Preview
@Composable
fun CheckIconPreview() = Icon { drawCheckIcon() }
