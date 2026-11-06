package com.example.microware.editor.sprite

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.Palette
import com.example.microware.game.sprite.Sprite
import com.example.microware.ui.layout.Insets
import com.example.microware.ui.layout.LocalInsets

@Preview(widthDp = 360, heightDp = 640)
@Composable
fun EditableCanvasPreview() {
    var sprite by remember {
        mutableStateOf(
            Sprite.pattern(24, 24) { index ->
                val row = index / 24
                val col = index % 24
                when {
                    row in 7..9 && (col in 6..8 || col in 15..17) -> Palette.Navy
                    row in 16..17 && col in 8..15 -> Palette.Navy
                    row in 3..20 && col in 3..20 -> Palette.Yellow
                    else -> Palette.Clear
                }
            },
        )
    }
    CompositionLocalProvider(LocalInsets provides Insets.new()) {
        EditableCanvas(
            sprite = sprite,
            commit = { sprite = it },
            resize = { resized, _ -> sprite = resized },
            modifier = Modifier.background(Palette.OffWhite.color),
            positionEditor = {},
        )
    }
}
