package com.example.microware

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round

private data class GridDragSource(val index: Int)
private object GridDropTarget

@Composable
fun <T : Identifiable<T>> Grid(
    items: List<T>,
    itemSize: DpSize,
    dragSource: DragSource<T>,
    dragTarget: DragTarget<T>,
    onMove: (Id, Int) -> Unit,
    preview: @Composable (T) -> Unit,
) {
    val gameBounds = remember { mutableStateMapOf<Id, Rect>() }
    val offsetInRoot = remember { mutableStateMapOf<Id, Offset>() }
    val slotBounds = items
        .mapNotNull { gameBounds[it.id] }
        .sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
    var gridBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(
        dragTarget.offer?.item?.id,
        dragTarget.offer?.offset,
        dragTarget.offer?.from,
        dragTarget.hoveredDropTarget?.to,
        items,
        slotBounds,
        gridBounds,
    ) {
        val offer = dragTarget.offer
        val target = dragTarget.hoveredDropTarget?.to
        val draggedGame = offer?.item ?: return@LaunchedEffect
        val from = offer.from as? GridDragSource ?: return@LaunchedEffect
        if (target !is GridDropTarget) {
            onMove(draggedGame.id, from.index)
            return@LaunchedEffect
        }

        val targetIndex = nearestGridSlot(slotBounds, offer.offset)
            ?: return@LaunchedEffect
        onMove(draggedGame.id, targetIndex)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .onGloballyPositioned { gridBounds = it }
            .dropTarget(
                dragController = dragTarget,
                to = GridDropTarget,
            )
    ) {
        for (item in items) {
            key(item.id) {
                offsetInRoot[item.id]?.let { offset ->
                    val animatedOffset = animateIntOffsetAsState(
                        offset.round(),
                        animationSpec = tween(durationMillis = 180)
                    )
                    GridItem(
                        item = item,
                        offer = dragTarget.offer,
                        modifier = Modifier.padding(8.dp).size(itemSize)
                            .offset { animatedOffset.value },
                        preview = preview,
                    )
                }
            }
        }
        Column(modifier = Modifier) {
            val columns = (this@BoxWithConstraints.maxWidth / itemSize.width).toInt()
            items.withIndex().chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier
                        .height(itemSize.height + 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { (index, item) ->
                        Box(
                            modifier = Modifier.padding(8.dp)
                                .size(itemSize)
                                .dragSource(dragSource, GridDragSource(index), item)
                                .onGloballyPositioned { layout ->
                                    gameBounds[item.id] = layout.boundsInWindow()
                                    gridBounds?.let { root ->
                                        offsetInRoot[item.id] =
                                            root.windowToLocal(layout.positionInWindow())
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T : Identifiable<T>> GridItem(
    item: T,
    offer: Offer<T>?,
    modifier: Modifier,
    preview: @Composable (T) -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .alpha(
                if (offer?.item != null &&
                    offer.item.id == item.id
                ) {
                    0f
                } else {
                    1f
                }
            )
            .clip(shape)
            .background(Palette.OffWhite.color)
            .drawBorder(Palette.DarkGray.color)
            .semantics { contentDescription = "Open game" },
    ) {
        preview(item)
    }
}

fun Modifier.drawBorder(
    color: Color,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
        size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        style = Stroke(width = 2.dp.toPx()),
    )
}

private fun nearestGridSlot(
    slotBounds: List<Rect>,
    pointer: Offset,
): Int? = slotBounds.indices.minByOrNull { index ->
    val center = slotBounds[index].center
    val dx = center.x - pointer.x
    val dy = center.y - pointer.y
    dx * dx + dy * dy
}
