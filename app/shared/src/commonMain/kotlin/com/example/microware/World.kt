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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val dragController = DragController<Obj>()
    val mutableWillBeDeleted = mutableStateOf(false)
    var worldOriginInWindow by mutableStateOf(Offset.Zero)
    var worldScale by mutableStateOf(1f)
}

@Composable
internal fun rememberWorldEditorState(): WorldEditorState = remember { WorldEditorState() }

data class World(
    val background: Sprite,
    val objects: List<Obj>,
    val logicTags: BiMap<LogicTag, Id> = BiMap(),
) {
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

    fun addObject(obj: Obj): World = copy(objects = objects + obj.newId())
    fun upsertObject(obj: Obj): World {
        return objects.indexOfFirst { obj.id == it.id }.takeIf { it >= 0 }?.let {
            updateObject(obj)
        } ?: addObject(obj)
    }

    fun logicTagFor(objectId: Id): LogicTag? = logicTags.flip()[objectId]

    fun assignLogicTag(objectId: Id, logicTag: LogicTag?): World =
        copy(
            logicTags = if (logicTag != null) logicTags.add(
                logicTag,
                objectId
            ) else logicTags.remove(objectId)
        )

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

    fun deleteObject(id: Id): World = copy(
        objects = objects.filterNot { it.id == id },
        logicTags = logicTags.remove(id),
    )
}

@Composable
internal fun WorldEditorContent(
    world: World,
    handler: (WorldEvent) -> Unit,
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
                    onGiven = { offer ->
                        val localOffset =
                            (offer.offset - state.worldOriginInWindow - offer.localOffset) / state.worldScale
                        val obj = world.clamped(offer.obj.copy(offset = localOffset))
                        handler { world ->
                            if (offer.from is World) {
                                world.updateObject(obj)
                            } else {
                                world.addObject(obj)
                            }
                        }
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
                        state.worldOriginInWindow = coordinates.positionInWindow()
                        state.worldScale = scale
                    }
                    .clipToBounds()
            ) {
                WorldSprite(
                    obj = Obj(id = Id(-1), sprite = world.background, offset = Offset.Zero),
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
                    obj = obj.copy(offset = (offset - state.worldOriginInWindow - localOffset) / state.worldScale),
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
    dragSource: DragSource<Obj>?,
    world: World,
    handler: (WorldEvent) -> Unit,
) {
    WorldEditToolbar(
        dragTarget = state.dragController,
        dragSource = dragSource,
        world = world,
        handler = handler,
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
    dragController: DragController<Obj>? = null,
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
