package com.example.microware.ui.icons

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawGridNavigationArrow(next: Boolean, color: Color) {
    val centerX = size.width / 2f
    val centerYOffset = if (next) 5f else -5f
    val centerY = size.height / 2f + centerYOffset
    val strokeWidth = 4.dp.toPx()
    val shaftStart: Offset
    val shaftEnd: Offset
    val arrowHeadStart: Offset
    val arrowHeadEnd: Offset

    if (next) {
        shaftStart = Offset(centerX, centerY - 9.dp.toPx())
        shaftEnd = Offset(centerX, centerY + 5.dp.toPx())
        arrowHeadStart = Offset(centerX - 8.dp.toPx(), centerY - 3.dp.toPx())
        arrowHeadEnd = Offset(centerX, centerY + 5.dp.toPx())
    } else {
        shaftStart = Offset(centerX, centerY + 9.dp.toPx())
        shaftEnd = Offset(centerX, centerY - 5.dp.toPx())
        arrowHeadStart = Offset(centerX - 8.dp.toPx(), centerY + 3.dp.toPx())
        arrowHeadEnd = Offset(centerX, centerY - 5.dp.toPx())
    }

    val secondHeadStart = Offset(
        x = centerX + (centerX - arrowHeadStart.x),
        y = arrowHeadStart.y,
    )
    val path = Path().apply {
        moveTo(shaftStart.x, shaftStart.y)
        lineTo(shaftEnd.x, shaftEnd.y)
        moveTo(arrowHeadStart.x, arrowHeadStart.y)
        lineTo(arrowHeadEnd.x, arrowHeadEnd.y)
        moveTo(secondHeadStart.x, secondHeadStart.y)
        lineTo(arrowHeadEnd.x, arrowHeadEnd.y)
    }
    drawPath(path, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

@Preview
@Composable
private fun GridNavigationArrowPreview() {
    Row {
        Icon { drawGridNavigationArrow(next = false, color = Color.Black) }
        Icon { drawGridNavigationArrow(next = true, color = Color.Black) }
    }
}

@Preview
@Composable
private fun TransparentGridNavigationArrowPreview() {
    Row {
        Icon { drawGridNavigationArrow(next = false, color = Color.Black.copy(alpha = .5f)) }
        Icon { drawGridNavigationArrow(next = true, color = Color.Black.copy(alpha = .5f)) }
    }
}
