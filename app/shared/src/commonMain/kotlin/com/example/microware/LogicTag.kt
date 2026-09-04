package com.example.microware

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val logicTagColors = Palette.entries.filter { it != Palette.Clear }
private const val logicTagShapeCount = 4
private val logicTagCount = logicTagColors.size * logicTagShapeCount

data class LogicTag(val tag: Int) {
    init {
        require(tag in 0..<logicTagCount) { "Invalid tag $tag" }
    }

    fun DrawScope.draw() {
        drawAt(
            center = Offset(size.width / 2f, size.height / 2f),
            size = size.minDimension,
        )
    }

    fun DrawScope.drawAt(center: Offset, size: Float, color: Color? = null) {
        val fillColor = color ?: logicTagColors[tag % logicTagColors.size].color
        val sides = tag / logicTagColors.size + 3
        val startAngle = if (sides == 4) -3 * PI / 4 else -PI / 2
        val radius = size * 0.42f
        val path = regularPolygon(center, radius, sides, startAngle)

        drawPath(path, fillColor)
        drawPath(
            path = path,
            color = Palette.Black.color,
            style = Stroke(width = 1.dp.toPx()),
        )
    }

    companion object {
        val all: List<LogicTag> = (0..<logicTagCount).map(::LogicTag)
    }
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
