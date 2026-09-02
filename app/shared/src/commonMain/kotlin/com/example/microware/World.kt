package com.example.microware

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
}

@Composable
fun WorldEditor(
    mutableWorld: MutableState<World>,
    library: MutableState<Library>,
) {
    var world by mutableWorld
    val mutableOffer = remember { mutableStateOf<Offer?>(null) }
    var offer by mutableOffer
    var worldOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var worldScale by remember { mutableStateOf(1f) }
    val objectBeingEdited = remember { mutableStateOf<Obj?>(null) }
    val mutableTrash = remember { mutableStateOf(Trash()) }
    val mutableWillBeDeleted = remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val toolbarHeight = HandleSize + TrashGap

        var worldOriginInParent by remember { mutableStateOf(Offset.Zero) }
        Column(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(
                modifier = Modifier
                    .padding(start = insets.leftDp(), end = insets.rightDp(), top = insets.topDp())
                    .fillMaxWidth()
                    .weight(1f)
                    .zIndex(if (offer != null) 1f else 0f)
                    .dropTarget(
                        offered = offer,
                        onGiven = {
                            val localOffset =
                                (it.offset - worldOriginInParent - it.localOffset) / worldScale
                            val obj = world.clamped(it.obj.copy(offset = localOffset))
                            world = if (it.from is World) {
                                world.updateObject(obj)
                            } else {
                                world.addObject(obj)
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
                            worldOriginInParent = coordinates.positionInWindow()
                            worldOriginInRoot = coordinates.localToRoot(Offset.Zero)
                            worldScale = scale
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
                        opacity = if (offer?.obj?.id == obj.id && mutableWillBeDeleted.value) 0.5f else 1f,
                        from = world,
                        settableOffer = mutableOffer,
                    )
                }

                offer?.apply {
                    WorldSprite(
                        obj = obj.copy(offset = (offset - worldOriginInParent - localOffset) / scale),
                        origin = IntOffset(worldLeft, worldTop),
                        scale = scale,
                        from = Unit,
                        opacity = 0.5f
                    )
                }
            }
            Spacer(modifier = Modifier.height(toolbarHeight + insets.bottomDp()))
        }

        WorldEditToolbar(
            settableOffer = mutableOffer,
            settableTrash = mutableTrash,
            settableLibrary = library,
            settableWorld = mutableWorld,
            settableObjectEdited = objectBeingEdited,
            willBeDeleted = mutableWillBeDeleted,
        )
    }
}

@Composable
private fun WorldSprite(
    obj: Obj,
    scale: Float,
    origin: IntOffset = IntOffset.Zero,
    opacity: Float = 1f,
    from: Any,
    settableOffer: MutableState<Offer?>? = null,
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
    val isFullyTransparent = obj.sprite.rows.all { row ->
        row.all { it == Palette.Clear }
    }
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
        .then(settableOffer?.let { Modifier.dropSource(it, from, obj) } ?: Modifier)

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
            onValueChange = { width = it },
        )
        IntegerSliderControl(
            dimension = IntegerSliderDimension.Height,
            value = height,
            maxValue = maxHeight,
            onValueChange = { height = it },
        )
        Button(
            onClick = { onCreate(width, height) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Create")
        }
    }
}
