package com.example.microware.editor.world
import com.example.microware.ui.gestures.obj
import com.example.microware.game.events.WorldEvent
import com.example.microware.game.model.Obj
import com.example.microware.game.model.World
import com.example.microware.game.sprite.Palette
import com.example.microware.game.sprite.scaled
import com.example.microware.ui.components.HandleSize
import com.example.microware.ui.components.TrashGap
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.gestures.dragSource
import com.example.microware.ui.gestures.dropTarget
import com.example.microware.ui.layout.density
import com.example.microware.ui.layout.insets
import com.example.microware.util.Id
import com.example.microware.util.roundToPx
import com.example.microware.util.toDp
import com.example.microware.util.toPx

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
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

internal class WorldEditorState {
    val dragController = DragController<Obj>()
    val mutableWillBeDeleted = mutableStateOf(false)
    var worldOriginInWindow by mutableStateOf(Offset.Zero)
    var worldScale by mutableStateOf(1f)
}

@Composable
internal fun rememberWorldEditorState(): WorldEditorState = remember { WorldEditorState() }

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
