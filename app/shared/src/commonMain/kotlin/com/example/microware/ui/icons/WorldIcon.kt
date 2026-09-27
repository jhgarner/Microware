package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.game.sprite.Palette

internal fun DrawScope.drawWorldIcon() {
    val stroke = 2.dp.toPx()
    fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
    fun Float.x(): Float = size.width * this
    fun Float.y(): Float = size.width * this

    val mountain = Path().apply {
        moveTo(.08f.x(), .68f.y())
        lineTo(.3f.x(), .23f.y())
        lineTo(.43f.x(), .46f.y())
        lineTo(.61f.x(), .16f.y())
        lineTo(.92f.x(), .68f.y())
        close()
    }
    drawPath(mountain, Palette.Navy.color)
    drawPath(mountain, Color.Black, style = Stroke(stroke))

    val snow = Path().apply {
        moveTo(.3f.x(), .23f.y())
        lineTo(.22f.x(), .39f.y())
        lineTo(.3f.x(), .35f.y())
        lineTo(.39f.x(), .39f.y())
        close()
    }
    drawPath(snow, Palette.OffWhite.color)
    drawPath(snow, Color.Black, style = Stroke(stroke))

    val snow2 = Path().apply {
        moveTo(.61f.x(), .16f.y())
        lineTo(.51f.x(), .33f.y())
        lineTo(.61f.x(), .28f.y())
        lineTo(.712f.x(), .33f.y())
        close()
    }
    drawPath(snow2, Palette.OffWhite.color)
    drawPath(snow2, Color.Black, style = Stroke(stroke))

    val house = Path().apply {
        moveTo(.25f.x(), .53f.y())
        lineTo(.45f.x(), .38f.y())
        lineTo(.65f.x(), .53f.y())
        lineTo(.62f.x(), .53f.y())
        lineTo(.62f.x(), .83f.y())
        lineTo(.29f.x(), .83f.y())
        lineTo(.29f.x(), .53f.y())
        close()
    }
    drawPath(house, Palette.Orange.color)
    drawPath(house, Color.Black, style = Stroke(stroke))

    drawRect(
        color = Palette.Navy.color,
        topLeft = point(.40f, .70f),
        size = Size(size.width * .10f, size.height * .13f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.40f, .70f),
        size = Size(size.width * .10f, size.height * .13f),
        style = Stroke(stroke / 1.5f),
    )
    drawRect(
        color = Palette.Cyan.color,
        topLeft = point(.35f, .56f),
        size = Size(size.width * .07f, size.height * .07f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.35f, .56f),
        size = Size(size.width * .07f, size.height * .07f),
        style = Stroke(stroke / 1.5f),
    )

    drawRect(
        color = Palette.Cyan.color,
        topLeft = point(.48f, .56f),
        size = Size(size.width * .07f, size.height * .07f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.48f, .56f),
        size = Size(size.width * .07f, size.height * .07f),
        style = Stroke(stroke / 1.5f),
    )
}

@Preview
@Composable
fun WorldIconPreview() = Icon { drawWorldIcon() }
