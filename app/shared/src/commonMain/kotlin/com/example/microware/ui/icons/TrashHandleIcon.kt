package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawTrashHandleIcon(color: Color) {
    val strokeWidth = 2.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * .28f, size.height * .25f),
        size = Size(size.width * .44f, size.height * .58f),
        cornerRadius = CornerRadius(strokeWidth, strokeWidth),
        style = Stroke(strokeWidth),
    )
    drawLine(
        color = color,
        start = Offset(size.width * .2f, size.height * .18f),
        end = Offset(size.width * .8f, size.height * .18f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(size.width * .43f, size.height * .08f),
        end = Offset(size.width * .57f, size.height * .08f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

@Preview
@Composable
fun TrashHandleIconPreview() = Icon { drawTrashHandleIcon(Color.Black) }
