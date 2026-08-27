package com.example.microware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
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

    fun addObject(obj: Obj): World = copy(objects = objects + obj)

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
    finishObjectDrag: (Int, Offset, Boolean) -> Unit,
    createObject: (Int, Int) -> Int,
    spawnObject: (Obj) -> Unit
) {
    var selectedObjectId by remember { mutableStateOf<Int?>(null) }
    var editingObjectId by remember { mutableStateOf<Int?>(null) }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    var draggingObjectId by remember { mutableStateOf<Int?>(null) }
    var draggingTrashObject by remember { mutableStateOf<Obj?>(null) }
    var dragGhostOffset by remember { mutableStateOf<Offset?>(null) }
    var dragPointerRootPosition by remember { mutableStateOf<Offset?>(null) }
    var showingCreateDialog by remember { mutableStateOf(false) }
    var editorRootPosition by remember { mutableStateOf(Offset.Zero) }
    var worldOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var worldScale by remember { mutableStateOf(1f) }
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    val objectBeingEdited = world.objects.firstOrNull { it.id == editingObjectId }
    var trash by remember { mutableStateOf(Trash()) }
    if (draggingTrashObject != null) {
        trash = trash.close()
    } else if (dragPointerRootPosition != null) {
        trash = if (trashBounds?.contains(dragPointerRootPosition!!) == true) {
            trash.hover()
        } else {
            trash.open()
        }
    } else if (trash.state !is TrashState.DrawerOpen) {
        trash = trash.close()
    }

    if (objectBeingEdited != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { editingObjectId = null },
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    editorRootPosition = coordinates.localToRoot(Offset.Zero)
                }
        ) {
            val density = LocalDensity.current
            val availableWidth = with(density) { maxWidth.roundToPx() }

            Column(modifier = Modifier.fillMaxSize()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val availableHeight = with(density) { maxHeight.roundToPx() }
                    val fitScale = min(
                        availableWidth.toFloat() / world.background.width,
                        availableHeight.toFloat() / world.background.height
                    )
                    val scale = if (fitScale >= 1f) floor(fitScale) else fitScale
                    val worldWidth = (world.background.width * scale).roundToInt()
                    val worldHeight = (world.background.height * scale).roundToInt()
                    val worldLeft = ((availableWidth - worldWidth) / 2).coerceAtLeast(0)
                    val worldTop = ((availableHeight - worldHeight) / 2).coerceAtLeast(0)

                    if (scale > 0f) {
                        Box(
                            modifier = Modifier
                                .requiredSize(
                                    with(density) { worldWidth.toDp() },
                                    with(density) { worldHeight.toDp() }
                                )
                                .offset { IntOffset(worldLeft, worldTop) }
                                .onGloballyPositioned { coordinates ->
                                    worldOriginInRoot = coordinates.localToRoot(Offset.Zero)
                                    worldScale = scale
                                }
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
                                onTap = { selectedObjectId = it },
                                onDragStart = { id ->
                                    world.objects.firstOrNull { it.id == id }?.let { obj ->
                                        draggingObjectId = id
                                        dragGhostOffset = obj.offset
                                        dragPointerRootPosition = null
                                        selectedObjectId = null
                                    }
                                },
                                onDragMove = { id, delta, pointerPosition ->
                                    val currentObject = world.objects.firstOrNull { it.id == id }
                                    if (currentObject != null) {
                                        dragGhostOffset = (
                                                dragGhostOffset ?: currentObject.offset
                                                ) + delta
                                        dragPointerRootPosition = editorRootPosition + Offset(
                                            worldLeft + currentObject.offset.x * scale + pointerPosition.x,
                                            worldTop + currentObject.offset.y * scale + pointerPosition.y
                                        )
                                    }
                                },
                                onDragEnd = { id, pointerPosition ->
                                    val currentObject = world.objects.firstOrNull { it.id == id }
                                    val ghostOffset = dragGhostOffset ?: currentObject?.offset
                                    val pointerRootPosition = currentObject?.let {
                                        editorRootPosition + Offset(
                                            worldLeft + it.offset.x * scale + pointerPosition.x,
                                            worldTop + it.offset.y * scale + pointerPosition.y
                                        )
                                    }

                                    val overTrash =
                                        pointerRootPosition?.let { trashBounds?.contains(it) == true } == true
                                    if (overTrash) {
                                        trash = trash.discard(currentObject)
                                    }
                                    finishObjectDrag(id, ghostOffset ?: Offset.Zero, overTrash)
                                    draggingObjectId = null
                                    dragGhostOffset = null
                                    dragPointerRootPosition = null
                                    selectedObjectId = null
                                }
                            )
                            if (draggingObjectId == obj.id && dragGhostOffset != null) {
                                WorldSprite(
                                    offset = dragGhostOffset!!,
                                    origin = IntOffset(worldLeft, worldTop),
                                    scale = scale,
                                    sprite = obj.sprite,
                                    opacity = 0.5f
                                )
                            }
                        }
                        val trashObject = draggingTrashObject
                        if (trashObject != null && dragGhostOffset != null) {
                            WorldSprite(
                                offset = dragGhostOffset!!,
                                origin = IntOffset(worldLeft, worldTop),
                                scale = scale,
                                sprite = trashObject.sprite,
                                opacity = 0.5f
                            )
                        }
                    }

                    val selectedObject = world.objects.firstOrNull { it.id == selectedObjectId }
                    if (selectedObject != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(selectedObject.id) {
                                    racing {
                                        sequence {
                                            expectTap()
                                            selectedObjectId = null
                                        }
                                    }
                                }
                        )
                        val objectLeft = worldLeft + (selectedObject.offset.x * scale).roundToInt()
                        val objectTop = worldTop + (selectedObject.offset.y * scale).roundToInt()
                        val objectWidth = (selectedObject.sprite.width * scale).roundToInt()
                        val menuGap = with(density) { 4.dp.roundToPx() }
                        val menuLeft = (
                                objectLeft + (objectWidth - menuSize.width) / 2
                                ).coerceIn(0, (availableWidth - menuSize.width).coerceAtLeast(0))
                        val menuTop = (objectTop - menuSize.height - menuGap).coerceAtLeast(0)
                        ObjMenu(
                            obj = selectedObject,
                            onEdit = {
                                editingObjectId = selectedObject.id
                                selectedObjectId = null
                            },
                            modifier = Modifier
                                .offset { IntOffset(menuLeft, menuTop) }
                                .alpha(if (menuSize == IntSize.Zero) 0f else 1f)
                                .onSizeChanged { menuSize = it }
                        )
                    }

                    if (showingCreateDialog) {
                        CreateObjectDialog(
                            onDismiss = { showingCreateDialog = false },
                            onCreate = { width, height ->
                                val id = createObject(width, height)
                                showingCreateDialog = false
                                editingObjectId = id
                            }
                        )
                    }
                }
                TrashControls(
                    trash = trash,
                    onCreate = { showingCreateDialog = true },
                    onTrashClick = {
                        trash = if (trash.state is TrashState.DrawerOpen) {
                            trash.close()
                        } else {
                            trash.openDrawer()
                        }
                    },
                    onTrashBoundsChanged = { trashBounds = it },
                    modifier = Modifier
                )
            }

            TrashDrawer(
                trash = trash,
                handleLeft = (trashBounds?.left ?: -500f) - editorRootPosition.x,
                onItemDragStart = { obj, pointerRootPosition ->
                    draggingTrashObject = obj
                    dragGhostOffset = (pointerRootPosition - worldOriginInRoot) / worldScale -
                            Offset(obj.sprite.width / 2f, obj.sprite.height / 2f)
                    dragPointerRootPosition = null
                    trash = trash.close()
                    selectedObjectId = null
                },
                onItemDragMove = { pointerRootPosition ->
                    draggingTrashObject?.let { obj ->
                        dragGhostOffset = (pointerRootPosition - worldOriginInRoot) / worldScale -
                                Offset(obj.sprite.width / 2f, obj.sprite.height / 2f)
                    }
                },
                onItemDragEnd = { pointerRootPosition ->
                    draggingTrashObject?.let { obj ->
                        val offset = (pointerRootPosition - worldOriginInRoot) / worldScale -
                                Offset(obj.sprite.width / 2f, obj.sprite.height / 2f)
                        trash = trash.retrieve(obj)
                        spawnObject(obj.copy(offset = offset))
                    }
                    draggingTrashObject = null
                    dragGhostOffset = null
                    dragPointerRootPosition = null
                    selectedObjectId = null
                },
                modifier = Modifier.fillMaxSize()
            )
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
                    val pointer = followDrag { change ->
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
private fun ObjMenu(obj: Obj, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Button(onClick = onEdit) {
            Text("Edit ${obj.id}")
        }
    }
}

@Composable
private fun CreateObjectDialog(
    onDismiss: () -> Unit,
    onCreate: (Int, Int) -> Unit
) {
    var widthText by remember { mutableStateOf("20") }
    var heightText by remember { mutableStateOf("20") }
    val width = widthText.toIntOrNull()
    val height = heightText.toIntOrNull()
    val canCreate = width != null && width > 0 && height != null && height > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create object") },
        text = {
            Column {
                OutlinedTextField(
                    value = widthText,
                    onValueChange = { value ->
                        widthText = value.filter { it.isDigit() }
                    },
                    label = { Text("Width") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { value ->
                        heightText = value.filter { it.isDigit() }
                    },
                    label = { Text("Height") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (width != null && height != null && width > 0 && height > 0) {
                        onCreate(width, height)
                    }
                },
                enabled = canCreate
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
