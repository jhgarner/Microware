package com.example.microware.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.model.LogicTag
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val logicTagColors = Palette.entries.filter { it != Palette.Clear }

fun DrawScope.drawLogicTag(tag: LogicTag) {
    drawLogicTagAt(
        tag = tag,
        center = Offset(size.width / 2f, size.height / 2f),
        size = size.minDimension,
    )
}

fun DrawScope.drawLogicTagAt(tag: LogicTag, center: Offset, size: Float, color: Color? = null) {
    val fillColor = color ?: logicTagColors[tag.tag % logicTagColors.size].color
    val sides = tag.tag / logicTagColors.size + 3
    val startAngle = if (sides == 4) -3 * PI / 4 else -PI / 2
    val radius = size * 0.42f
    val path = regularPolygon(center, radius, sides, startAngle)

    drawPath(path, fillColor)
    drawPath(path, Palette.Black.color, style = Stroke(width = 1.dp.toPx()))
}

private fun regularPolygon(
    center: Offset,
    radius: Float,
    sides: Int,
    startAngle: Double,
): Path {
    return Path().apply {
        repeat(sides) { index ->
            val angle = startAngle + 2 * PI * index / sides
            val point = Offset(
                x = center.x + radius * cos(angle).toFloat(),
                y = center.y + radius * sin(angle).toFloat(),
            )
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        close()
    }
}

@Preview
@Composable
fun LogicTagVariantsPreview() {
    val colorsPerShape = Palette.entries.count { it != Palette.Clear }
    Canvas(Modifier.size(width = 160.dp, height = 40.dp)) {
        repeat(4) { shape ->
            drawLogicTagAt(
                tag = LogicTag(shape * colorsPerShape + shape),
                center = Offset(size.width * (shape + .5f) / 4f, size.height / 2f),
                size = size.height * .8f,
            )
        }
    }
}
