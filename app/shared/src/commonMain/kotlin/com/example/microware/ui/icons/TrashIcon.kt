package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawTrashIcon() {
    drawRect(
        Color.Black,
        Offset(size.width * .25f, size.height * .3f),
        Size(size.width * .5f, size.height * .55f),
        style = Stroke(2.dp.toPx()),
    )
    drawLine(
        Color.Black,
        Offset(size.width * .15f, size.height * .25f),
        Offset(size.width * .85f, size.height * .25f),
        2.dp.toPx(),
    )
    drawLine(
        Color.Black,
        Offset(size.width * .4f, size.height * .15f),
        Offset(size.width * .6f, size.height * .15f),
        2.dp.toPx(),
    )
    drawLine(
        Color.Black,
        Offset(size.width * .4f, size.height * .4f),
        Offset(size.width * .4f, size.height * .75f),
        1.dp.toPx(),
    )
    drawLine(
        Color.Black,
        Offset(size.width * .6f, size.height * .4f),
        Offset(size.width * .6f, size.height * .75f),
        1.dp.toPx(),
    )
}

@Preview
@Composable
fun TrashIconPreview() = Icon { drawTrashIcon() }
