package com.example.microware

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round

private data class GridDragSource(
    val sourceId: Any,
    val index: Int,
)

private data class GridDropTarget(val sourceId: Any)

internal fun Offer<*>.originatesFromGrid(sourceId: Any): Boolean =
    (from as? GridDragSource)?.sourceId == sourceId

@Composable
fun <T : Identifiable<T>> Grid(
    items: List<T>,
    itemSize: DpSize,
    dragSource: DragSource<T>,
    dragTarget: DragTarget<T>,
    onMove: (Id, Int) -> Unit,
    sourceId: Any = Unit,
    onDrop: (Offer<T>, Int) -> Unit = { _, _ -> },
    preview: @Composable (T) -> Unit,
) {
    val gridDropTarget = remember(sourceId) { GridDropTarget(sourceId) }
    val itemBounds = remember { mutableStateMapOf<Id, Rect>() }
    val itemOffsets = remember { mutableStateMapOf<Id, Offset>() }
    val slotBounds = items
        .mapNotNull { itemBounds[it.id] }
        .sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
    var gridBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(
        dragTarget.offer?.item?.id,
        dragTarget.offer?.offset,
        dragTarget.offer?.from,
        dragTarget.hoveredDropTarget?.to,
        items,
        slotBounds,
        gridDropTarget,
    ) {
        val offer = dragTarget.offer
        val draggedItem = offer?.item ?: return@LaunchedEffect
        val from = offer.from as? GridDragSource ?: return@LaunchedEffect
        if (from.sourceId != sourceId) return@LaunchedEffect

        val targetIndex = if (dragTarget.hoveredDropTarget?.to == gridDropTarget) {
            nearestGridSlot(slotBounds, offer.offset) ?: return@LaunchedEffect
        } else {
            from.index
        }
        onMove(draggedItem.id, targetIndex)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { gridBounds = it }
            .dropTarget(
                dragController = dragTarget,
                to = gridDropTarget,
                onGiven = { offer ->
                    if (!offer.originatesFromGrid(sourceId)) {
                        onDrop(
                            offer,
                            nearestGridInsertionIndex(slotBounds, offer.offset),
                        )
                    }
                },
            )
    ) {
        for (item in items) {
            key(item.id) {
                itemOffsets[item.id]?.let { offset ->
                    val animatedOffset = animateIntOffsetAsState(
                        offset.round(),
                        animationSpec = tween(durationMillis = 180)
                    )
                    GridItem(
                        item = item,
                        offer = dragTarget.offer,
                        sourceId = sourceId,
                        modifier = Modifier.size(itemSize)
                            .offset { animatedOffset.value },
                        preview = preview,
                    )
                }
            }
        }
        Column {
            val columns = (this@BoxWithConstraints.maxWidth / (itemSize.width + 16.dp))
                .toInt()
                .coerceAtLeast(1)
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
                                .dragSource(dragSource, GridDragSource(sourceId, index), item)
                                .onGloballyPositioned { layout ->
                                    itemBounds[item.id] = layout.boundsInWindow()
                                    gridBounds?.let { root ->
                                        itemOffsets[item.id] =
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
    sourceId: Any,
    modifier: Modifier,
    preview: @Composable (T) -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val isDraggedItem = offer?.let {
        it.item.id == item.id && it.originatesFromGrid(sourceId)
    } == true
    Box(
        modifier = modifier
            .alpha(if (isDraggedItem) 0f else 1f)
            .clip(shape),
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

private fun nearestGridInsertionIndex(
    slotBounds: List<Rect>,
    pointer: Offset,
): Int {
    val slotIndex = nearestGridSlot(slotBounds, pointer) ?: return 0
    val slot = slotBounds[slotIndex]
    val afterSlot = pointer.y > slot.center.y ||
        (pointer.y == slot.center.y && pointer.x > slot.center.x)
    return slotIndex + if (afterSlot) 1 else 0
}
