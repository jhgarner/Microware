package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private fun DrawScope.drawPlusIcon() {
    drawLine(
        Color.Black,
        Offset(size.width * .5f, size.height * .15f),
        Offset(size.width * .5f, size.height * .85f),
        3.dp.toPx(),
    )
    drawLine(
        Color.Black,
        Offset(size.width * .15f, size.height * .5f),
        Offset(size.width * .85f, size.height * .5f),
        3.dp.toPx(),
    )
}

@Preview
@Composable
fun PlusIconPreview() = Icon { drawPlusIcon() }
