package com.example.microware.editor.sprite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.ui.components.IconPopupButton
import com.example.microware.ui.components.IntegerSliderControl
import com.example.microware.ui.components.IntegerSliderDimension
import com.example.microware.ui.components.SlidingIconSwitch
import kotlin.math.floor

private const val MaxBrushSize = 16

@Composable
internal fun BrushOptions(brush: PixelBrush, onBrushChange: (PixelBrush) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SlidingIconSwitch(
            checked = brush.shape == BrushShape.Square,
            onCheckedChange = {
                onBrushChange(brush.copy(shape = if (it) BrushShape.Square else BrushShape.Round))
            },
            description = "Brush shape",
            stateDescription = brush.shape.name,
            firstIcon = { drawBrush(PixelBrush(7, BrushShape.Round)) },
            secondIcon = { drawBrush(PixelBrush(7, BrushShape.Square)) },
        )
        IconPopupButton(
            description = "Brush size: ${brush.size} pixels",
            modifier = Modifier.size(48.dp),
            icon = { drawBrush(brush, gridSize = MaxBrushSize, minimumDiameter = 4.dp.toPx()) },
        ) {
            BrushSizeOptions(brush, onBrushChange)
        }
    }
}

@Composable
private fun BrushSizeOptions(brush: PixelBrush, onBrushChange: (PixelBrush) -> Unit) {
    Column(
        modifier = Modifier.width(280.dp).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(80.dp)) {
            drawBrush(brush, gridSize = MaxBrushSize)
        }
        IntegerSliderControl(
            dimension = IntegerSliderDimension.BrushSize,
            value = brush.size,
            maxValue = MaxBrushSize,
            onValueChange = { onBrushChange(brush.copy(size = it!!)) },
        )
    }
}

private fun DrawScope.drawBrush(
    brush: PixelBrush,
    gridSize: Int = brush.size,
    minimumDiameter: Float = 0f,
) {
    val pixelSize = maxOf(size.minDimension / gridSize, minimumDiameter / brush.size)
    val origin = center - Offset(brush.size * pixelSize / 2, brush.size * pixelSize / 2)
    val anchor = brush.size / 2
    brush.cellsAt(Cell(anchor, anchor)).forEach { cell ->
        val left = floor(origin.x + cell.col * pixelSize)
        val top = floor(origin.y + cell.row * pixelSize)
        val right = floor(origin.x + (cell.col + 1) * pixelSize)
        val bottom = floor(origin.y + (cell.row + 1) * pixelSize)
        drawRect(
            color = Palette.Navy.color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
        )
    }
}

@Preview(widthDp = 360)
@Composable
fun BrushOptionsPreview() {
    var brush by remember { mutableStateOf(PixelBrush(7)) }
    Row(
        modifier = Modifier.background(Palette.OffWhite.color),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrushOptions(brush) { brush = it }
        SpriteColorPicker(Palette.Navy, {}, Modifier.weight(1f))
    }
}

@Preview
@Composable
fun BrushSizeOptionsPreview() {
    var brush by remember { mutableStateOf(PixelBrush(7)) }
    Column(Modifier.background(Palette.OffWhite.color)) {
        BrushSizeOptions(brush) { brush = it }
    }
}
