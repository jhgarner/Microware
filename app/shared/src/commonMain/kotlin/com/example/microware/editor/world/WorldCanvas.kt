package com.example.microware.editor.world

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.example.microware.game.events.WorldEvent
import com.example.microware.game.model.Obj
import com.example.microware.game.model.World
import com.example.microware.game.model.WorldHeight
import com.example.microware.game.model.WorldWidth
import com.example.microware.ui.components.HandleSize
import com.example.microware.ui.components.TrashGap
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.gestures.dropTarget
import com.example.microware.ui.gestures.obj
import com.example.microware.ui.layout.insets
import com.example.microware.util.roundToPx
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
                .onGloballyPositioned { state.worldOriginInWindow = it.positionInWindow() }
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
                availableWidth.toFloat() / WorldWidth,
                availableHeight.toFloat() / WorldHeight
            )
            val scale = if (fitScale >= 1f) floor(fitScale) else fitScale
            state.worldScale = scale
            val worldWidth = (WorldWidth * scale).roundToInt()
            val worldHeight = (WorldHeight * scale).roundToInt()
            val worldLeft = ((availableWidth - worldWidth) / 2).coerceAtLeast(0)
            val worldTop = ((availableHeight - worldHeight) / 2).coerceAtLeast(0)

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
