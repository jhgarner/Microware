package com.example.microware.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.model.LogicTag
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val logicTagColors = Palette.entries.filter { it != Palette.Clear }

private const val MaxShownLogicTags = 4

/** The tags to draw, where null stands for the ellipsis that replaces the ones left out. */
private fun shownLogicTags(tags: Set<LogicTag>): List<LogicTag?> {
    val ordered = tags.sortedBy { it.tag }
    return if (ordered.size <= MaxShownLogicTags) {
        ordered
    } else {
        ordered.take(MaxShownLogicTags - 1) + null
    }
}

/** A single line of tags, each [tagSize] wide. */
@Composable
fun LogicTagRow(tags: Set<LogicTag>, tagSize: Dp, modifier: Modifier = Modifier) {
    val shownCount = shownLogicTags(tags).size
    Canvas(modifier.size(width = tagSize * shownCount, height = tagSize)) {
        drawLogicTags(tags, tagsPerRow = MaxShownLogicTags)
    }
}

/** Fills the canvas with one large tag, or shrinks several into rows of [tagsPerRow]. */
fun DrawScope.drawLogicTags(tags: Set<LogicTag>, tagsPerRow: Int = 2) {
    val rows = shownLogicTags(tags).chunked(tagsPerRow)
    val widestRow = rows.maxOfOrNull { it.size } ?: return
    val tagSize = min(size.width / widestRow, size.height / rows.size)

    rows.forEachIndexed { rowIndex, row ->
        row.forEachIndexed { columnIndex, tag ->
            val tagCenter = center + Offset(
                x = (columnIndex - (row.size - 1) / 2f) * tagSize,
                y = (rowIndex - (rows.size - 1) / 2f) * tagSize,
            )
            if (tag == null) {
                drawEllipsis(tagCenter, tagSize)
            } else {
                drawLogicTagAt(tag, tagCenter, tagSize)
            }
        }
    }
}

private fun DrawScope.drawEllipsis(center: Offset, size: Float) {
    for (dot in -1..1) {
        drawCircle(
            color = Palette.Black.color,
            radius = size * .08f,
            center = center + Offset(dot * size * .26f, 0f),
        )
    }
}

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

@Preview
@Composable
fun LogicTagGroupsPreview() {
    val tags = listOf(3, 20, 37, 54, 11, 28).map(::LogicTag)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (count in 1..6) {
            Canvas(Modifier.size(40.dp)) { drawLogicTags(tags.take(count).toSet()) }
        }
    }
}

@Preview
@Composable
fun LogicTagRowsPreview() {
    val tags = listOf(3, 20, 37, 54, 11, 28).map(::LogicTag)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (count in 1..6) {
            LogicTagRow(tags.take(count).toSet(), tagSize = 16.dp)
        }
    }
}
