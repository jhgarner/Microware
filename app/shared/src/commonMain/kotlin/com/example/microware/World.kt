package com.example.microware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

const val WorldWidth = 128
const val WorldHeight = 256

data class World(val background: Sprite, val objects: List<Obj>) {
    constructor() : this(Sprite.empty(WorldWidth, WorldHeight), emptyList())

    fun moveObject(id: Int, delta: Offset): World {
        return copy(
            objects = objects.map { obj ->
                if (obj.id != id) {
                    obj
                } else {
                    val maxX = (background.width - obj.sprite.width).coerceAtLeast(0).toFloat()
                    val maxY = (background.height - obj.sprite.height).coerceAtLeast(0).toFloat()
                    obj.copy(
                        offset = Offset(
                            (obj.offset.x + delta.x).coerceIn(0f, maxX),
                            (obj.offset.y + delta.y).coerceIn(0f, maxY)
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun WorldEditor(world: World, moveObject: (Int, Offset) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.roundToPx() }
        val availableHeight = with(density) { maxHeight.roundToPx() }
        val fitScale = min(
            availableWidth.toFloat() / world.background.width,
            availableHeight.toFloat() / world.background.height
        )
        val scale = if (fitScale >= 1f) floor(fitScale) else fitScale

        if (scale > 0f) {
            val worldWidth = (world.background.width * scale).roundToInt()
            val worldHeight = (world.background.height * scale).roundToInt()
            Box(
                modifier = Modifier
                    .requiredSize(
                        with(density) { worldWidth.toDp() },
                        with(density) { worldHeight.toDp() }
                    )
                    .align(Alignment.Center)
                    .clipToBounds()
            ) {
                WorldSprite(Offset.Zero, scale, world.background)
                for (obj in world.objects) {
                    WorldSprite(obj.offset, scale, obj.sprite, obj.id, moveObject)
                }
            }
        }
    }
}

@Composable
private fun WorldSprite(
    offset: Offset,
    scale: Float,
    sprite: Sprite,
    objectId: Int? = null,
    moveObject: ((Int, Offset) -> Unit)? = null
) {
    val density = LocalDensity.current
    val image = remember(sprite, scale) {
        scaled(
            sprite.bitmap,
            (sprite.width * scale).roundToInt().coerceAtLeast(1),
            (sprite.height * scale).roundToInt().coerceAtLeast(1)
        )
    }
    val currentMoveObject = rememberUpdatedState(moveObject)
    var modifier = Modifier
        .requiredSize(
            with(density) { image.width.toDp() },
            with(density) { image.height.toDp() }
        )
        .offset {
            IntOffset(
                (offset.x * scale).roundToInt(),
                (offset.y * scale).roundToInt()
            )
        }

    if (objectId != null) {
        modifier = modifier.pointerInput(objectId, scale) {
            racing {
                sequence {
                    var accumulated = Offset.Zero
                    var sent = Offset.Zero
                    followPointer { change ->
                        accumulated += change.positionChange() / scale
                        val snapped = Offset(
                            accumulated.x.roundToInt().toFloat(),
                            accumulated.y.roundToInt().toFloat()
                        )
                        val delta = snapped - sent
                        if (delta != Offset.Zero) {
                            currentMoveObject.value?.invoke(objectId, delta)
                            sent = snapped
                        }
                    }
                }
            }
        }
    }

    Canvas(modifier) {
        drawImage(image)
    }
}

// TODO This menu should appear When the user clicks on a Sprite. You can probably use the new
//  expectTap method to detect that compared to a drag. When the button in the menu is pressed, it
//  should navigate the user to a page with an EditableCanvas so they can edit the sprite. After
//  editing the sprite, they can return back here and see the edited sprite in the world.
@Composable
private fun ObjMenu(obj: Obj) {
    Row {
        Button(onClick = {}) {
            Text("Edit")
        }
    }
}
