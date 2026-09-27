package com.example.microware.editor.world
import com.example.microware.game.events.WorldEvent
import com.example.microware.game.model.Obj
import com.example.microware.game.model.World
import com.example.microware.game.sprite.Palette
import com.example.microware.game.sprite.Sprite
import com.example.microware.ui.components.Grid
import com.example.microware.ui.components.HandleSize
import com.example.microware.ui.components.TrashGap
import com.example.microware.ui.gestures.dragSource
import com.example.microware.ui.layout.insets
import com.example.microware.util.toDp
import com.example.microware.util.toPx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.min
import kotlin.math.roundToInt

private val ObjectOrderCardHeight = 88.dp
private val ObjectOrderCardShape = RoundedCornerShape(8.dp)

@Composable
internal fun ObjectOrderEditor(
    world: World,
    handler: (WorldEvent) -> Unit,
    state: WorldEditorState,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val topFirstObjects = world.objects.asReversed()

    var rootBounds by remember { mutableStateOf<Rect?>(null) }
    Box(
        modifier = Modifier.fillMaxSize()
            .onGloballyPositioned { rootBounds = it.boundsInWindow() }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = insets.leftDp(),
                    top = insets.topDp(),
                    end = insets.rightDp(),
                ),
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Grid(
                    items = topFirstObjects,
                    itemSize = DpSize(maxWidth - 16.dp, 72.dp + 16.dp),
                    dragSource = state.dragController,
                    dragTarget = state.dragController,
                    onMove = { id, targetIndex ->
                        handler { world ->
                            val topFirst = world.objects.asReversed()
                            val sourceIndex = topFirst.indexOfFirst { it.id == id }
                            val game = topFirst[sourceIndex]
                            val mutTopFirst = topFirst.toMutableList()
                            mutTopFirst.removeAt(sourceIndex)
                            mutTopFirst.add(targetIndex, game)
                            world.copy(objects = mutTopFirst.reversed().toPersistentList())
                        }
                    },
                ) {
                    ObjectOrderCard(
                        obj = it,
                        world = world,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ObjectOrderCardHeight),
                    )
                }
            }
            Spacer(
                modifier = Modifier.height(
                    HandleSize + TrashGap + insets.bottomDp(),
                ),
            )
        }
        state.dragController.offer?.let { offer ->
            Box(
                modifier = Modifier
                    .size(offer.size.width.toDp(), offer.size.height.toDp())
                    .offset {
                        (offer.offset - (rootBounds?.topLeft
                            ?: Offset.Zero) - offer.localOffset).round()
                    }
            ) {
                ObjectOrderCard(offer.item, world, Modifier)
            }
        }
    }
}

@Composable
private fun ObjectOrderCard(
    obj: Obj,
    world: World,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.OffWhite.color)
            .border(2.dp, Palette.DarkGray.color, ObjectOrderCardShape)
//            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = "Reorder object" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            world.logicTagFor(obj.id)?.let { tag ->
                Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                    with(tag) { draw() }
                }
            }
        }
        Canvas(Modifier.size(72.dp)) {
            drawSpriteFit(obj.sprite)
        }
    }
}

private fun DrawScope.drawSpriteFit(sprite: Sprite) {
    val scale = min(
        size.width / sprite.width,
        size.height / sprite.height,
    )
    val width = (sprite.width * scale).roundToInt().coerceAtLeast(1)
    val height = (sprite.height * scale).roundToInt().coerceAtLeast(1)
    drawImage(
        image = sprite.bitmap,
        dstOffset = IntOffset(
            ((size.width - width) / 2f).roundToInt(),
            ((size.height - height) / 2f).roundToInt(),
        ),
        dstSize = IntSize(width, height),
        filterQuality = FilterQuality.None,
    )
}

internal fun DrawScope.drawObjectOrderIcon() {
    val stroke = 2.dp.toPx()
    val layers = listOf(
        Palette.Cyan.color to .24f,
        Palette.Orange.color to .44f,
        Palette.Green.color to .64f,
    )
    layers.forEach { (color, top) ->
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * .2f, size.height * top),
            size = androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 2f),
        )
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(size.width * .2f, size.height * top),
            size = androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 2f),
            style = Stroke(stroke),
        )
    }
}
