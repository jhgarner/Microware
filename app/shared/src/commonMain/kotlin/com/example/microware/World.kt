package com.example.microware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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

    fun updateObject(id: Int, edit: Sprite.Edits): World {
        return copy(
            objects = objects.map { obj ->
                if (obj.id == id) obj.copy(sprite = obj.sprite.update(edit)) else obj
            }
        )
    }
}

@Composable
fun WorldEditor(
    world: World,
    moveObject: (Int, Offset) -> Unit,
    updateObject: (Int, Sprite.Edits) -> Unit
) {
    var selectedObjectId = remember { mutableStateOf<Int?>(null) }
    var editingObjectId = remember { mutableStateOf<Int?>(null) }
    var menuSize = remember { mutableStateOf(IntSize.Zero) }
    val objectBeingEdited = world.objects.firstOrNull { it.id == editingObjectId.value }

    if (objectBeingEdited != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { editingObjectId.value = null },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Back")
            }
            EditableCanvas(
                sprite = objectBeingEdited.sprite,
                edit = { edit -> updateObject(objectBeingEdited.id, edit) },
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val availableWidth = with(density) { maxWidth.roundToPx() }
            val availableHeight = with(density) { maxHeight.roundToPx() }
            val fitScale = min(
                availableWidth.toFloat() / world.background.width,
                availableHeight.toFloat() / world.background.height
            )
            val scale = if (fitScale >= 1f) floor(fitScale) else fitScale
            val worldWidth = (world.background.width * scale).roundToInt()
            val worldHeight = (world.background.height * scale).roundToInt()

            if (scale > 0f) {
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
                        WorldSprite(
                            offset = obj.offset,
                            scale = scale,
                            sprite = obj.sprite,
                            objectId = obj.id,
                            moveObject = moveObject,
                            onTap = { selectedObjectId.value = it }
                        )
                    }
                }
            }

            val selectedObject = world.objects.firstOrNull { it.id == selectedObjectId.value }
            if (selectedObject != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedObject.id) {
                            racing {
                                sequence {
                                    expectTap()
                                    selectedObjectId.value = null
                                }
                            }
                        }
                )
                val worldLeft = ((availableWidth - worldWidth) / 2).coerceAtLeast(0)
                val worldTop = ((availableHeight - worldHeight) / 2).coerceAtLeast(0)
                val objectLeft = worldLeft + (selectedObject.offset.x * scale).roundToInt()
                val objectTop = worldTop + (selectedObject.offset.y * scale).roundToInt()
                val objectWidth = (selectedObject.sprite.width * scale).roundToInt()
                val menuGap = with(density) { 4.dp.roundToPx() }
                val menuLeft = (
                    objectLeft + (objectWidth - menuSize.value.width) / 2
                ).coerceIn(0, (availableWidth - menuSize.value.width).coerceAtLeast(0))
                val menuTop = (objectTop - menuSize.value.height - menuGap).coerceAtLeast(0)
                ObjMenu(
                    obj = selectedObject,
                    onEdit = {
                        editingObjectId.value = selectedObject.id
                        selectedObjectId.value = null
                    },
                    modifier = Modifier
                        .offset { IntOffset(menuLeft, menuTop) }
                        .alpha(if (menuSize.value == IntSize.Zero) 0f else 1f)
                        .onSizeChanged { menuSize.value = it }
                )
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
    moveObject: ((Int, Offset) -> Unit)? = null,
    onTap: ((Int) -> Unit)? = null
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
    val currentOnTap = rememberUpdatedState(onTap)
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
                    expectTap()
                    currentOnTap.value?.invoke(objectId)
                }
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

@Composable
private fun ObjMenu(obj: Obj, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Button(onClick = onEdit) {
            Text("Edit ${obj.id}")
        }
    }
}
