package com.example.microware.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.microware.ui.gestures.DragSource
import com.example.microware.ui.gestures.DragTarget
import com.example.microware.ui.gestures.Offer
import com.example.microware.ui.gestures.dragSource
import com.example.microware.ui.gestures.dropTarget
import com.example.microware.util.Id
import com.example.microware.util.Identifiable

private val GridNavigationHeight = 56.dp

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
    var gridBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var currentPage by remember(sourceId) { mutableIntStateOf(0) }
    var previousButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var nextButtonBounds by remember { mutableStateOf<Rect?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = ((maxWidth + 8.dp) / (itemSize.width + 24.dp))
            .toInt()
            .coerceAtLeast(1)
        val rows = ((maxHeight - GridNavigationHeight) / (itemSize.height + 16.dp))
            .toInt()
            .coerceAtLeast(1)
        val pageCapacity = columns * rows
        val pageCount = ((items.size + pageCapacity - 1) / pageCapacity).coerceAtLeast(1)
        val page = currentPage.coerceIn(0, pageCount - 1)
        val pageStart = page * pageCapacity
        val visibleItems = items.drop(pageStart).take(pageCapacity)
        val slotBounds = visibleItems
            .mapNotNull { item -> itemBounds[item.id] }
            .sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
        val canGoToPreviousPage = page > 0
        val canGoToNextPage = page < pageCount - 1
        val offer = dragTarget.offer
        val offerOffset = offer?.offset
        val isOverPreviousButton = offerOffset != null &&
                previousButtonBounds?.contains(offerOffset) == true
        val isOverNextButton = offerOffset != null &&
                nextButtonBounds?.contains(offerOffset) == true
        val hoveredDirection = when {
            isOverPreviousButton && canGoToPreviousPage -> GridPageDirection.Previous
            isOverNextButton && canGoToNextPage -> GridPageDirection.Next
            else -> null
        }
        val isOverNavigation = isOverPreviousButton || isOverNextButton
        val navigationProgress = remember(sourceId) { Animatable(0f) }

        LaunchedEffect(pageCount) {
            currentPage = page
        }

        LaunchedEffect(hoveredDirection, offer?.item?.id, pageCount) {
            navigationProgress.snapTo(0f)
            val direction = hoveredDirection ?: return@LaunchedEffect

            while (true) {
                navigationProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1_000),
                )
                val nextPage = currentPage + direction.pageDelta
                if (nextPage !in 0 until pageCount) break

                currentPage = nextPage
                navigationProgress.snapTo(0f)
            }

            navigationProgress.snapTo(0f)
        }

        LaunchedEffect(
            offer?.item?.id,
            offer?.offset,
            offer?.from,
            dragTarget.hoveredDropTarget?.to,
            items,
            slotBounds,
            pageStart,
            isOverNavigation,
            gridDropTarget,
        ) {
            val draggedItem = offer?.item ?: return@LaunchedEffect
            val from = offer.from as? GridDragSource ?: return@LaunchedEffect
            if (from.sourceId != sourceId || isOverNavigation) return@LaunchedEffect

            val targetIndex = if (dragTarget.hoveredDropTarget?.to == gridDropTarget) {
                nearestGridSlot(slotBounds, offer.offset)?.let(pageStart::plus)
                    ?: return@LaunchedEffect
            } else {
                from.index
            }
            onMove(draggedItem.id, targetIndex)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .dropTarget(
                        dragController = dragTarget,
                        to = gridDropTarget,
                        onGiven = { newOffer ->
                            if (!newOffer.originatesFromGrid(sourceId)) {
                                onDrop(
                                    newOffer,
                                    pageStart + nearestGridInsertionIndex(slotBounds, newOffer.offset),
                                )
                            }
                        },
                    )
                    .onGloballyPositioned { gridBounds = it },
            ) {
                for (item in visibleItems) {
                    key(item.id) {
                        itemOffsets[item.id]?.let { offset ->
                            val animatedOffset = animateIntOffsetAsState(
                                offset.round(),
                                animationSpec = tween(durationMillis = 180),
                            )
                            GridItem(
                                item = item,
                                offer = offer,
                                sourceId = sourceId,
                                modifier = Modifier.size(itemSize)
                                    .offset { animatedOffset.value },
                                preview = preview,
                            )
                        }
                    }
                }

                Column {
                    visibleItems.withIndex().chunked(columns).forEach { row ->
                        Row(
                            modifier = Modifier.height(itemSize.height + 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { (index, item) ->
                                val itemIndex = pageStart + index
                                Box(
                                    modifier = Modifier.padding(8.dp)
                                        .size(itemSize)
                                        .dragSource(dragSource, GridDragSource(sourceId, itemIndex), item)
                                        .suggestDrag()
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

            Row(modifier = Modifier.fillMaxWidth().height(GridNavigationHeight)) {
                GridNavigationButton(
                    direction = GridPageDirection.Previous,
                    enabled = canGoToPreviousPage,
                    progress = if (hoveredDirection == GridPageDirection.Previous) {
                        navigationProgress.value
                    } else {
                        0f
                    },
                    onClick = { currentPage = page - 1 },
                    modifier = Modifier.weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { previousButtonBounds = it.boundsInWindow() },
                )
                GridNavigationButton(
                    direction = GridPageDirection.Next,
                    enabled = canGoToNextPage,
                    progress = if (hoveredDirection == GridPageDirection.Next) {
                        navigationProgress.value
                    } else {
                        0f
                    },
                    onClick = { currentPage = page + 1 },
                    modifier = Modifier.weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { nextButtonBounds = it.boundsInWindow() },
                )
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
