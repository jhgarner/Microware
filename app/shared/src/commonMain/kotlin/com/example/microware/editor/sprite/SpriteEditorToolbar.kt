package com.example.microware.editor.sprite

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.sprite.scaled
import com.example.microware.game.sprite.tiled
import com.example.microware.game.sprite.transparent
import com.example.microware.ui.icons.resizeIcon

enum class SpriteEditorTool {
    Pen,
    Bucket,
    Resize,
    Position,
}

private const val TransparentTileSizeDp = 16

@Composable
fun SpriteToolPicker(
    selectedTool: SpriteEditorTool,
    onSelectTool: (SpriteEditorTool) -> Unit,
    tools: List<SpriteEditorTool> = SpriteEditorTool.entries,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (tool in tools) {
            ToolButton(
                description = when (tool) {
                    SpriteEditorTool.Pen -> "Pen tool"
                    SpriteEditorTool.Bucket -> "Paint bucket tool"
                    SpriteEditorTool.Resize -> "Resize tool"
                    SpriteEditorTool.Position -> "Position tool"
                },
                selected = selectedTool == tool,
                onClick = { onSelectTool(tool) },
            ) {
                Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                    when (tool) {
                        SpriteEditorTool.Pen -> {
                            drawLine(
                                color = Color.Black,
                                start = Offset(size.width * 0.25f, size.height * 0.75f),
                                end = Offset(size.width * 0.75f, size.height * 0.25f),
                                strokeWidth = 5.dp.toPx(),
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 3.dp.toPx(),
                                center = Offset(size.width * 0.22f, size.height * 0.78f),
                            )
                        }

                        SpriteEditorTool.Bucket -> drawBucketToolIcon()
                        SpriteEditorTool.Resize -> resizeIcon()
                        SpriteEditorTool.Position -> {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.minDimension * .42f
                            val directions = listOf(
                                Offset(0f, -1f),
                                Offset(1f, 0f),
                                Offset(0f, 1f),
                                Offset(-1f, 0f),
                            )
                            val corners = directions.map { center + it * radius }
                            val headLength = .40f
                            val stroke = 2.dp.toPx()

                            corners.forEachIndexed { index, corner ->
                                val previousCorner = corners[(index - 1 + corners.size) % corners.size]
                                val nextCorner = corners[(index + 1) % corners.size]
                                val leftHead = corner + (previousCorner - corner) * headLength
                                val rightHead = corner + (nextCorner - corner) * headLength

                                drawLine(Color.Black, center, corner, stroke)
                                drawLine(Color.Black, leftHead, corner, stroke)
                                drawLine(Color.Black, rightHead, corner, stroke)
                            }
                            drawCircle(Color.Black, 1.5.dp.toPx(), center)
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBucketToolIcon() {
    val bucket = Path().apply {
        moveTo(size.width * .25f, size.height * .38f)
        lineTo(size.width * .75f, size.height * .38f)
        lineTo(size.width * .63f, size.height * .82f)
        lineTo(size.width * .37f, size.height * .82f)
        close()
    }
    drawPath(bucket, Palette.Navy.color)
    drawLine(
        color = Color.Black,
        start = Offset(size.width * .32f, size.height * .34f),
        end = Offset(size.width * .50f, size.height * .15f),
        strokeWidth = 2.dp.toPx(),
    )
    drawLine(
        color = Color.Black,
        start = Offset(size.width * .50f, size.height * .15f),
        end = Offset(size.width * .68f, size.height * .34f),
        strokeWidth = 2.dp.toPx(),
    )
    drawCircle(
        color = Palette.Cyan.color,
        radius = size.minDimension * .10f,
        center = Offset(size.width * .72f, size.height * .25f),
    )
}

@Composable
private fun ToolButton(
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .size(48.dp)
            .semantics { contentDescription = description },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Palette.Cyan.color else Palette.OffWhite.color,
            contentColor = Color.Black,
        ),
        shape = RectangleShape,
        border = BorderStroke(2.dp, if (selected) Color.Black else Palette.LightGray.color),
        contentPadding = PaddingValues(0.dp),
    ) {
        content()
    }
}

@Composable
fun SpriteColorPicker(
    selectedColor: Palette,
    onSelectColor: (Palette) -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
        for (color in Palette.entries) {
            ColorPickerButton(color, selectedColor == color) {
                onSelectColor(color)
            }
        }
    }
}

@Composable
private fun ColorPickerButton(
    color: Palette,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val clearBrush = if (color == Palette.Clear) {
        val tileSize = with(LocalDensity.current) { TransparentTileSizeDp.dp.roundToPx() }
        remember(tileSize) { tiled(scaled(transparent, tileSize, tileSize)) }
    } else {
        null
    }

    Button(
        onClick = onClick,
        modifier = Modifier.padding(4.dp).size(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (color == Palette.Clear) Color.Transparent else color.color,
            contentColor = color.color,
        ),
        shape = RectangleShape,
        border = if (isSelected) BorderStroke(2.dp, Color.Black) else null,
        contentPadding = PaddingValues(0.dp),
    ) {
        if (clearBrush != null) {
            Box(modifier = Modifier.fillMaxSize().background(clearBrush))
        }
    }
}

@Composable
fun ResizeOptions(
    canReset: Boolean,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onReset,
            enabled = canReset,
            modifier = Modifier.padding(4.dp),
        ) {
            Text("Reset")
        }
        Button(
            onClick = onConfirm,
            enabled = canReset,
            modifier = Modifier.padding(4.dp),
        ) {
            Text("✓ Confirm")
        }
    }
}
