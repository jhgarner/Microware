package com.example.microware.editor.sprite.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteBounds
import com.example.microware.ui.components.IconButton

private class SelectionTransform(
    val description: String,
    val transform: (FloatingSelection) -> FloatingSelection,
    val icon: DrawScope.(Float) -> Unit,
)

private val transforms = listOf(
    SelectionTransform("Copy selection", FloatingSelection::copied) { drawCopyIcon(it) },
    SelectionTransform("Rotate selection left", FloatingSelection::rotatedCounterClockwise) {
        drawRotateIcon(clockwise = false, it)
    },
    SelectionTransform("Rotate selection right", FloatingSelection::rotatedClockwise) {
        drawRotateIcon(clockwise = true, it)
    },
    SelectionTransform("Flip selection horizontally", FloatingSelection::flippedHorizontally) {
        drawFlipIcon(vertical = false, it)
    },
    SelectionTransform("Flip selection vertically", FloatingSelection::flippedVertically) {
        drawFlipIcon(vertical = true, it)
    },
)

@Composable
internal fun SelectionOptions(
    selection: FloatingSelection?,
    onDeselect: () -> Unit,
    onChange: (FloatingSelection) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        SelectionButton(
            description = "Deselect",
            onClick = onDeselect.takeIf { selection != null },
        ) { drawDeselectIcon(it) }
        for (transform in transforms) {
            SelectionButton(
                description = transform.description,
                onClick = selection?.let { { onChange(transform.transform(it)) } },
                icon = transform.icon,
            )
        }
    }
}

@Composable
private fun SelectionButton(
    description: String,
    onClick: (() -> Unit)?,
    icon: DrawScope.(Float) -> Unit,
) {
    IconButton(
        description = description,
        modifier = Modifier.size(56.dp),
        enabled = onClick != null,
        onClick = { onClick?.invoke() },
        icon = icon,
    )
}

@Preview(widthDp = 360)
@Composable
fun SelectionOptionsPreview() {
    val selection = FloatingSelection.lift(Sprite.solid(4, 4, Palette.Navy), SpriteBounds(0, 0, 2, 2))
    Column(Modifier.background(Palette.OffWhite.color)) {
        SelectionOptions(selection = null, onDeselect = {}, onChange = {})
        SelectionOptions(selection = selection, onDeselect = {}, onChange = {})
    }
}
