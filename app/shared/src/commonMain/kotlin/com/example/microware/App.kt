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

@Composable
fun App() {

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(Palette.OffWhite.color)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val game = remember {
                mutableStateOf(
                    Game(
                        world = World(
                            Sprite.solid(WorldWidth, WorldHeight, Palette.Rust), listOf(
                                Obj(
                                    id = 1,
                                    offset = Offset(20f, 20f),
                                    sprite = Sprite.solid(20, 20, Palette.Cyan)
                                )
                            )
                        ),
                        logics = emptyList(),
                    )
                )
            }
            CompositionLocalProvider(LocalInsets provides Insets.new()) {
                val library = remember { mutableStateOf(loadLibrary()) }
                GameEditor(
                    mutableGame = game,
                    library = library,
                )
            }
        }
    }
}
