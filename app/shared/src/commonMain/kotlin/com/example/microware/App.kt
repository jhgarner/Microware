package com.example.microware

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                .windowInsetsPadding(
                    WindowInsets.safeContent.only(WindowInsetsSides.Vertical)
                )
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
//            var sprite by remember { mutableStateOf(Sprite.empty(20, 20)) }
//            EditableCanvas(sprite) { sprite = sprite.update(it) }
            var world by remember {
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
            var library by remember { mutableStateOf(loadLibrary()) }
            WorldEditor(
                world = world,
                updateObject = { id, edit -> world = world.updateObject(id, edit) },
                finishObjectDrag = { id, offset, overTrash ->
                    world = if (overTrash) {
                        world.deleteObject(id)
                    } else {
                        world.placeObject(id, offset).clampObject(id)
                    }
                },
                createObject = { width, height ->
                    val id = (world.objects.maxOfOrNull { it.id } ?: 0) + 1
                    world = world.addObject(Obj(id, width, height))
                    id
                },
                spawnObject = { obj ->
                    val id = (world.objects.maxOfOrNull { it.id } ?: 0) + 1
                    world = world.addObject(obj.copy(id = id)).clampObject(id)
                },
                library = library,
                onObjectDroppedOnLibrary = { obj ->
                    library = library.add(obj)
                    saveLibrary(library)
                }
            )
        }
    }
}
