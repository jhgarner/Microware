package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.model.LogicTag

internal fun DrawScope.drawLogicIcon() {
    val pageStroke = 2.dp.toPx()
    val textStroke = 1.5.dp.toPx()
    val pageColor = Palette.Tan.color
    val outlineColor = Color.Black
    fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
    fun Float.x(): Float = size.width * this
    fun Float.y(): Float = size.width * this

    val leftPage = Path().apply {
        moveTo(.5f.x(), .23f.y())
        lineTo(.34f.x(), .2f.y())
        lineTo(.17f.x(), .21f.y())
        lineTo(.17f.x(), .73f.y())
        lineTo(.34f.x(), .72f.y())
        lineTo(.5f.x(), .75f.y())
        close()
    }
    val rightPage = Path().apply {
        moveTo(.5f.x(), .23f.y())
        lineTo(.66f.x(), .2f.y())
        lineTo(.83f.x(), .21f.y())
        lineTo(.83f.x(), .73f.y())
        lineTo(.66f.x(), .72f.y())
        lineTo(.5f.x(), .75f.y())
        close()
    }

    drawPath(leftPage, pageColor)
    drawPath(rightPage, pageColor)
    drawPath(leftPage, outlineColor, style = Stroke(pageStroke))
    drawPath(rightPage, outlineColor, style = Stroke(pageStroke))
    drawLine(outlineColor, point(.5f, .23f), point(.5f, .75f), pageStroke)

    drawLine(Palette.DarkGray.color, point(.23f, .34f), point(.43f, .34f), textStroke)
    drawLine(Palette.DarkGray.color, point(.23f, .63f), point(.43f, .63f), textStroke)
    drawLine(Palette.DarkGray.color, point(.57f, .34f), point(.77f, .34f), textStroke)
    drawLine(Palette.DarkGray.color, point(.57f, .63f), point(.77f, .63f), textStroke)

    drawLogicTagAt(LogicTag(58), point(.33f, .49f), size.minDimension * .17f)
    drawLogicTagAt(LogicTag(29), point(.67f, .49f), size.minDimension * .17f)
}

@Preview
@Composable
fun LogicIconPreview() = Icon { drawLogicIcon() }
