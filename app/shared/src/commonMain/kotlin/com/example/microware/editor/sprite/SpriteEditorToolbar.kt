package com.example.microware.editor.sprite
import com.example.microware.game.sprite.Palette
import com.example.microware.game.sprite.scaled
import com.example.microware.game.sprite.tiled
import com.example.microware.game.sprite.transparent
import com.example.microware.util.roundToPx
import com.example.microware.util.toPx

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class SpriteEditorTool {
    Pen,
    Resize,
}

private const val TransparentTileSizeDp = 16

@Composable
fun SpriteToolPicker(
    selectedTool: SpriteEditorTool,
    onSelectTool: (SpriteEditorTool) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        ToolButton(
            description = "Pen tool",
            selected = selectedTool == SpriteEditorTool.Pen,
            onClick = { onSelectTool(SpriteEditorTool.Pen) },
        ) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
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
        }
        ToolButton(
            description = "Resize tool",
            selected = selectedTool == SpriteEditorTool.Resize,
            onClick = { onSelectTool(SpriteEditorTool.Resize) },
        ) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val strokeWidth = 2.dp.toPx()
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(size.width * 0.2f, size.height * 0.2f),
                    size = Size(size.width * 0.6f, size.height * 0.6f),
                    style = Stroke(strokeWidth),
                )
                val handleRadius = 3.dp.toPx()
                listOf(
                    Offset(size.width * 0.2f, size.height * 0.2f),
                    Offset(size.width * 0.8f, size.height * 0.2f),
                    Offset(size.width * 0.2f, size.height * 0.8f),
                    Offset(size.width * 0.8f, size.height * 0.8f),
                ).forEach { drawCircle(Color.Black, handleRadius, it) }
            }
        }
    }
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
fun PenOptions(
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
