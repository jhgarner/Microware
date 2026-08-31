package com.example.microware

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(Palette.OffWhite.color)
//                .windowInsetsPadding(
//                    WindowInsets.safeContent.only(WindowInsetsSides.Vertical)
//                )
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
//            var sprite by remember { mutableStateOf(Sprite.empty(20, 20)) }
//            EditableCanvas(sprite) { sprite = sprite.update(it) }
            val world = remember {
                mutableStateOf(
                    World(
                        Sprite.solid(WorldWidth, WorldHeight, Palette.Rust), listOf(
                            Obj(
                                id = 1,
                                offset = Offset(20f, 20f),
                                sprite = Sprite.solid(20, 20, Palette.Cyan)
                            )
                        )
                    )
                )
            }
            CompositionLocalProvider(LocalInsets provides Insets.new()) {
                val library = remember { mutableStateOf(loadLibrary()) }
                WorldEditor(
                    mutableWorld = world,
                    library = library,
                )
            }
        }
    }
}
