package com.example.microware

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

const val WorldWidth = MaxSpriteWidth
const val WorldHeight = MaxSpriteHeight

internal class WorldEditorState {
    val dragController = DragController()
    val objectBeingEdited = mutableStateOf<Obj?>(null)
    val mutableTrash = mutableStateOf(Trash())
    val mutableWillBeDeleted = mutableStateOf(false)
    var worldOriginInParent by mutableStateOf(Offset.Zero)
    var worldScale by mutableStateOf(1f)
}

@Composable
internal fun rememberWorldEditorState(): WorldEditorState = remember { WorldEditorState() }

data class World(
    val background: Sprite,
    val objects: List<Obj>,
    val logicTags: Map<LogicTag, Int> = emptyMap(),
) {
    init {
        require(logicTags.values.toSet().size == logicTags.size) {
            "An object cannot have more than one logic tag"
        }
        require(logicTags.values.all { objectId -> objects.any { it.id == objectId } }) {
            "Logic tags must refer to objects in the world"
        }
    }

    constructor() : this(Sprite.empty(WorldWidth, WorldHeight), emptyList())

    fun updateObject(newObj: Obj): World {
        return copy(
            objects = objects.map { obj ->
                if (obj.id != newObj.id) {
                    obj
                } else {
                    val maxX = (background.width - newObj.sprite.width).coerceAtLeast(0).toFloat()
                    val maxY = (background.height - newObj.sprite.height).coerceAtLeast(0).toFloat()
                    newObj.copy(
                        offset = Offset(
                            (newObj.offset.x).coerceIn(0f, maxX),
                            (newObj.offset.y).coerceIn(0f, maxY)
                        )
                    )
                }
            }
        )
    }

    fun addObject(obj: Obj): World = copy(objects = objects + obj.copy(id = nextId()))
    fun nextId(): Int = (objects.maxOfOrNull { it.id } ?: 0) + 1

    fun logicTagFor(objectId: Int): LogicTag? {
        return logicTags.entries.firstOrNull { it.value == objectId }?.key
    }

    fun assignLogicTag(objectId: Int, logicTag: LogicTag?): World {
        if (objects.none { it.id == objectId }) return this

        val newLogicTags = logicTags
            .filterValues { it != objectId }
            .toMutableMap()
        if (logicTag == null) {
            return copy(logicTags = newLogicTags)
        }

        newLogicTags[logicTag] = objectId
        return copy(logicTags = newLogicTags)
    }

    fun clamped(obj: Obj): Obj {
        val maxX = (background.width - obj.sprite.width).coerceAtLeast(0).toFloat()
        val maxY = (background.height - obj.sprite.height).coerceAtLeast(0).toFloat()
        return obj.copy(
            offset = Offset(
                obj.offset.x.coerceIn(0f, maxX),
                obj.offset.y.coerceIn(0f, maxY)
            )
        )
    }

    fun deleteObject(id: Int): World = copy(
        objects = objects.filterNot { it.id == id },
        logicTags = logicTags.filterValues { it != id },
    )

    fun reorderObject(id: Int, targetIndex: Int): World {
        val sourceIndex = objects.indexOfFirst { it.id == id }
        if (sourceIndex < 0 || objects.size < 2) return this

        val destinationIndex = if (targetIndex > sourceIndex) {
            targetIndex - 1
        } else {
            targetIndex
        }
        val insertionIndex = destinationIndex.coerceIn(0, objects.lastIndex)
        if (sourceIndex == insertionIndex) return this

        val reordered = objects.toMutableList()
        val obj = reordered.removeAt(sourceIndex)
        reordered.add(insertionIndex, obj)
        return copy(objects = reordered)
    }
}

@Composable
fun WorldEditor(
    world: World,
    onWorldChange: (World) -> Unit,
    library: MutableState<Library>,
) {
    val state = rememberWorldEditorState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { state.dragController.setRootCoordinates(it) }
            .dragObserver(state.dragController)
    ) {
        WorldEditorContent(world = world, onWorldChange = onWorldChange, state = state)
        WorldEditorToolbar(
            state = state,
            world = world,
            onWorldChange = onWorldChange,
            library = library,
        )
    }
}

@Composable
internal fun WorldEditorContent(
    world: World,
    onWorldChange: (World) -> Unit,
    state: WorldEditorState,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val offer = state.dragController.offer
    val toolbarHeight = HandleSize + TrashGap

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(start = insets.leftDp(), end = insets.rightDp(), top = insets.topDp())
                .fillMaxWidth()
                .weight(1f)
                .zIndex(if (offer != null) 1f else 0f)
                .dropTarget(
                    dragController = state.dragController,
                    offered = offer,
                    onGiven = { offer, _ ->
                        val localOffset =
                            (offer.offset - state.worldOriginInParent - offer.localOffset) / state.worldScale
                        val obj = world.clamped(offer.obj.copy(offset = localOffset))
                        onWorldChange(if (offer.from is World) {
                            world.updateObject(obj)
                        } else {
                            world.addObject(obj)
                        })
                    },
                )
        ) {
            val availableWidth = maxWidth.roundToPx()
            val availableHeight = maxHeight.roundToPx()
            val fitScale = min(
                availableWidth.toFloat() / world.background.width,
                availableHeight.toFloat() / world.background.height
            )
            val scale = if (fitScale >= 1f) floor(fitScale) else fitScale
            val worldWidth = (world.background.width * scale).roundToInt()
            val worldHeight = (world.background.height * scale).roundToInt()
            val worldLeft = ((availableWidth - worldWidth) / 2).coerceAtLeast(0)
            val worldTop = ((availableHeight - worldHeight) / 2).coerceAtLeast(0)

            Box(
                modifier = Modifier
                    .requiredSize(worldWidth.toDp(), worldHeight.toDp())
                    .offset { IntOffset(worldLeft, worldTop) }
                    .onGloballyPositioned { coordinates ->
                        state.worldOriginInParent = coordinates.positionInWindow()
                        state.worldScale = scale
                    }
                    .clipToBounds()
            ) {
                WorldSprite(
                    obj = Obj(id = 0, sprite = world.background, offset = Offset.Zero),
                    origin = IntOffset.Zero,
                    scale = scale,
                    from = Unit,
                )
            }
            for (obj in world.objects) {
                WorldSprite(
                    obj = obj,
                    origin = IntOffset(worldLeft, worldTop),
                    scale = scale,
                    opacity = if (offer?.obj?.id == obj.id && state.mutableWillBeDeleted.value) .5f else 1f,
                    from = world,
                    dragController = state.dragController,
                )
            }

            offer?.apply {
                WorldSprite(
                    obj = obj.copy(offset = (offset - state.worldOriginInParent - localOffset) / state.worldScale),
                    origin = IntOffset(worldLeft, worldTop),
                    scale = scale,
                    from = Unit,
                    opacity = 0.5f
                )
            }
        }
        Spacer(modifier = Modifier.height(toolbarHeight + insets.bottomDp()))
    }
}

@Composable
internal fun BoxWithConstraintsScope.WorldEditorToolbar(
    state: WorldEditorState,
    world: World,
    onWorldChange: (World) -> Unit,
    library: MutableState<Library>,
    isVisible: Boolean = true,
) {
    WorldEditToolbar(
        offered = state.dragController.offer,
        dragController = state.dragController,
        isVisible = isVisible,
        settableTrash = state.mutableTrash,
        settableLibrary = library,
        world = world,
        onWorldChange = onWorldChange,
        settableObjectEdited = state.objectBeingEdited,
        willBeDeleted = state.mutableWillBeDeleted,
    )
}

@Composable
private fun WorldSprite(
    obj: Obj,
    scale: Float,
    origin: IntOffset = IntOffset.Zero,
    opacity: Float = 1f,
    from: Any,
    dragController: DragController? = null,
) {
    val density = LocalDensity.current
    val animatedOpacity by animateFloatAsState(
        targetValue = opacity,
        animationSpec = tween(durationMillis = 120),
        label = "sprite opacity"
    )
    val image = remember(obj.sprite, scale) {
        scaled(
            obj.sprite.bitmap,
            (obj.sprite.width * scale).roundToInt().coerceAtLeast(1),
            (obj.sprite.height * scale).roundToInt().coerceAtLeast(1)
        )
    }
    val isFullyTransparent = obj.sprite.isFullyTransparent()
    val layout = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val modifier = Modifier
        .requiredSize(
            with(density) { image.width.toDp() },
            with(density) { image.height.toDp() }
        )
        .offset {
            IntOffset(
                origin.x + (obj.offset.x * scale).roundToInt(),
                origin.y + (obj.offset.y * scale).roundToInt()
            )
        }
        .onGloballyPositioned { layout.value = it }
        .alpha(animatedOpacity)
        .then(dragController?.let { Modifier.dragSource(it, from, obj) } ?: Modifier)

    Canvas(modifier) {
        drawImage(image)
        if (isFullyTransparent) {
            drawRect(
                color = Palette.Black.color,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun CreateObjectForm(
    maxWidth: Int,
    maxHeight: Int,
    onCreate: (Int, Int) -> Unit,
) {
    var width by remember(maxWidth) { mutableStateOf(20.coerceIn(1, maxWidth)) }
    var height by remember(maxHeight) { mutableStateOf(20.coerceIn(1, maxHeight)) }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("Create sprite")
        IntegerSliderControl(
            dimension = IntegerSliderDimension.Width,
            value = width,
            maxValue = maxWidth,
            onValueChange = { it?.let { newWidth -> width = newWidth } },
        )
        IntegerSliderControl(
            dimension = IntegerSliderDimension.Height,
            value = height,
            maxValue = maxHeight,
            onValueChange = { it?.let { newHeight -> height = newHeight } },
        )
        Button(
            onClick = { onCreate(width, height) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Create")
        }
    }
}
