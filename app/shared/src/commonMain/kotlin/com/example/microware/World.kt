package com.example.microware

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
private val TrashSize = 56.dp
private val TrashGap = 8.dp

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

    fun dragObject(id: Int, delta: Offset): World {
        return copy(
            objects = objects.map { obj ->
                if (obj.id == id) obj.copy(offset = obj.offset + delta) else obj
            }
        )
    }

    fun placeObject(id: Int, offset: Offset): World {
        return copy(
            objects = objects.map { obj ->
                if (obj.id == id) obj.copy(offset = offset) else obj
            }
        )
    }

    fun clampObject(id: Int): World {
        return copy(
            objects = objects.map { obj ->
                if (obj.id != id) {
                    obj
                } else {
                    val maxX = (background.width - obj.sprite.width).coerceAtLeast(0).toFloat()
                    val maxY = (background.height - obj.sprite.height).coerceAtLeast(0).toFloat()
                    obj.copy(
                        offset = Offset(
                            obj.offset.x.coerceIn(0f, maxX),
                            obj.offset.y.coerceIn(0f, maxY)
                        )
                    )
                }
            }
        )
    }

    fun deleteObject(id: Int): World = copy(objects = objects.filterNot { it.id == id })

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
    updateObject: (Int, Sprite.Edits) -> Unit,
    finishObjectDrag: (Int, Offset, Boolean) -> Unit
) {
    var selectedObjectId = remember { mutableStateOf<Int?>(null) }
    var editingObjectId = remember { mutableStateOf<Int?>(null) }
    var menuSize = remember { mutableStateOf(IntSize.Zero) }
    var draggingObjectId = remember { mutableStateOf<Int?>(null) }
    var dragGhostOffset = remember { mutableStateOf<Offset?>(null) }
    var dragPointerPagePosition = remember { mutableStateOf<Offset?>(null) }
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
            val trashSize = with(density) { TrashSize.roundToPx() }
            val trashGap = with(density) { TrashGap.roundToPx() }
            val boardAvailableHeight = (availableHeight - trashSize - trashGap).coerceAtLeast(1)
            val fitScale = min(
                availableWidth.toFloat() / world.background.width,
                boardAvailableHeight.toFloat() / world.background.height
            )
            val scale = if (fitScale >= 1f) floor(fitScale) else fitScale
            val worldWidth = (world.background.width * scale).roundToInt()
            val worldHeight = (world.background.height * scale).roundToInt()
            val worldLeft = ((availableWidth - worldWidth) / 2).coerceAtLeast(0)
            val worldTop = (
                (availableHeight - worldHeight - trashGap - trashSize) / 2
            ).coerceAtLeast(0)
            val trashLeft = worldLeft
            val trashTop = worldTop + worldHeight + trashGap
            val trashBounds = Rect(
                left = trashLeft.toFloat(),
                top = trashTop.toFloat(),
                right = (trashLeft + trashSize).toFloat(),
                bottom = (trashTop + trashSize).toFloat()
            )
            val isDragging = draggingObjectId.value != null
            val isOverTrash = dragPointerPagePosition.value?.let(trashBounds::contains) == true
            val hiddenTrashSlide = -(availableWidth + trashSize).toFloat()
            val trashSlideX = remember(trashLeft, trashSize) {
                Animatable(hiddenTrashSlide)
            }
            LaunchedEffect(isDragging, trashLeft) {
                if (isDragging) {
                    trashSlideX.snapTo(hiddenTrashSlide)
                    trashSlideX.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 180)
                    )
                } else {
                    trashSlideX.animateTo(
                        targetValue = hiddenTrashSlide,
                        animationSpec = tween(durationMillis = 180)
                    )
                }
            }

            if (scale > 0f) {
                Box(
                    modifier = Modifier
                        .requiredSize(
                            with(density) { worldWidth.toDp() },
                            with(density) { worldHeight.toDp() }
                        )
                        .offset { IntOffset(worldLeft, worldTop) }
                        .clipToBounds()
                ) {
                    WorldSprite(Offset.Zero, scale, world.background)
                }
                for (obj in world.objects) {
                    WorldSprite(
                        offset = obj.offset,
                        origin = IntOffset(worldLeft, worldTop),
                        scale = scale,
                        sprite = obj.sprite,
                        objectId = obj.id,
                        onTap = { selectedObjectId.value = it },
                        onDragStart = { id ->
                            world.objects.firstOrNull { it.id == id }?.let { obj ->
                                draggingObjectId.value = id
                                dragGhostOffset.value = obj.offset
                                dragPointerPagePosition.value = null
                                selectedObjectId.value = null
                            }
                        },
                        onDragMove = { id, delta, pointerPosition ->
                            val currentObject = world.objects.firstOrNull { it.id == id }
                            if (currentObject != null) {
                                dragGhostOffset.value = (
                                    dragGhostOffset.value ?: currentObject.offset
                                ) + delta
                                dragPointerPagePosition.value = Offset(
                                    worldLeft + currentObject.offset.x * scale + pointerPosition.x,
                                    worldTop + currentObject.offset.y * scale + pointerPosition.y
                                )
                            }
                        },
                        onDragEnd = { id, pointerPosition ->
                            val currentObject = world.objects.firstOrNull { it.id == id }
                            val ghostOffset = dragGhostOffset.value ?: currentObject?.offset
                            val pointerPagePosition = currentObject?.let {
                                Offset(
                                    worldLeft + it.offset.x * scale + pointerPosition.x,
                                    worldTop + it.offset.y * scale + pointerPosition.y
                                )
                            }
                            finishObjectDrag(
                                id,
                                ghostOffset ?: Offset.Zero,
                                pointerPagePosition?.let(trashBounds::contains) == true
                            )
                            draggingObjectId.value = null
                            dragGhostOffset.value = null
                            dragPointerPagePosition.value = null
                            selectedObjectId.value = null
                        }
                    )
                    if (draggingObjectId.value == obj.id && dragGhostOffset.value != null) {
                        WorldSprite(
                            offset = dragGhostOffset.value!!,
                            origin = IntOffset(worldLeft, worldTop),
                            scale = scale,
                            sprite = obj.sprite,
                            opacity = 0.5f
                        )
                    }
                }
                TrashTarget(
                    isActive = isOverTrash,
                    modifier = Modifier.offset {
                        IntOffset(
                            trashLeft + trashSlideX.value.roundToInt(),
                            trashTop
                        )
                    }
                )
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
    origin: IntOffset = IntOffset.Zero,
    opacity: Float = 1f,
    objectId: Int? = null,
    onTap: ((Int) -> Unit)? = null,
    onDragStart: ((Int) -> Unit)? = null,
    onDragMove: ((Int, Offset, Offset) -> Unit)? = null,
    onDragEnd: ((Int, Offset) -> Unit)? = null
) {
    val density = LocalDensity.current
    val image = remember(sprite, scale) {
        scaled(
            sprite.bitmap,
            (sprite.width * scale).roundToInt().coerceAtLeast(1),
            (sprite.height * scale).roundToInt().coerceAtLeast(1)
        )
    }
    val currentOnTap = rememberUpdatedState(onTap)
    val currentOnDragStart = rememberUpdatedState(onDragStart)
    val currentOnDragMove = rememberUpdatedState(onDragMove)
    val currentOnDragEnd = rememberUpdatedState(onDragEnd)
    var modifier = Modifier
        .requiredSize(
            with(density) { image.width.toDp() },
            with(density) { image.height.toDp() }
        )
        .offset {
            IntOffset(
                origin.x + (offset.x * scale).roundToInt(),
                origin.y + (offset.y * scale).roundToInt()
            )
        }
        .alpha(opacity)

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
                    var started = false
                    val pointer = followPointer { change ->
                        if (!started) {
                            currentOnDragStart.value?.invoke(objectId)
                            started = true
                        }
                        accumulated += change.positionChange() / scale
                        val snapped = Offset(
                            accumulated.x.roundToInt().toFloat(),
                            accumulated.y.roundToInt().toFloat()
                        )
                        val delta = snapped - sent
                        currentOnDragMove.value?.invoke(objectId, delta, change.position)
                        if (delta != Offset.Zero) {
                            sent = snapped
                        }
                    }
                    currentOnDragEnd.value?.invoke(objectId, pointer.position)
                }
            }
        }
    }

    Canvas(modifier) {
        drawImage(image)
    }
}

@Composable
private fun TrashTarget(isActive: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val background = if (isActive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = if (isActive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(TrashSize)
            .background(background, shape)
            .border(2.dp, foreground, shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val strokeWidth = 2.dp.toPx()
            val canLeft = size.width * 0.28f
            val canTop = size.height * 0.25f
            val canWidth = size.width * 0.44f
            val canHeight = size.height * 0.58f
            drawRoundRect(
                color = foreground,
                topLeft = Offset(canLeft, canTop),
                size = Size(canWidth, canHeight),
                cornerRadius = CornerRadius(strokeWidth, strokeWidth),
                style = Stroke(strokeWidth)
            )
            drawLine(
                color = foreground,
                start = Offset(size.width * 0.2f, size.height * 0.18f),
                end = Offset(size.width * 0.8f, size.height * 0.18f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = foreground,
                start = Offset(size.width * 0.43f, size.height * 0.08f),
                end = Offset(size.width * 0.57f, size.height * 0.08f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
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
