package com.example.microware

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal const val GhostListAnimationDurationMillis = 300
internal const val GhostListDragGhostAlpha = .7f

private data class GhostListDrop<T : Any>(
    val request: Int,
    val item: T,
    val itemKey: Int,
    val startTopLeftInWindow: Offset,
    val sourceBounds: Rect?,
)

private const val GhostListTemporaryKeyBase = Int.MIN_VALUE
private const val GhostListEndKey = "ghost-list-end"

@Composable
internal fun <T : Identifiable<T>> GhostingLazyColumn(
    items: List<T>,
    dragController: DragController<T>,
    modifier: Modifier,
    listModifier: Modifier = Modifier.fillMaxSize(),
    belowList: @Composable () -> Unit = {},
    willBeDeleted: Boolean = false,
    onDrop: (Offer<T>, Int) -> T,
    ghostWidth: Dp? = null,
    ghostContent: (@Composable (T, Modifier) -> Unit)? = null,
    itemContent: @Composable (T, Modifier) -> Unit,
    draggedItemAlpha: (Boolean) -> Float = { 1f },
    contentPadding: PaddingValues = PaddingValues(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    insertionSlotHeight: Dp = 72.dp,
    scrollToTopRequest: Int = 0,
) {
    val offered = dragController.offer
    val listState = rememberLazyListState()
    val itemBounds = remember { mutableStateMapOf<Int, Rect>() }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }
    var dropAnimation by remember { mutableStateOf<GhostListDrop<T>?>(null) }
    var startedDropRequest by remember { mutableStateOf<Int?>(null) }
    var nextDropRequest by remember { mutableStateOf(0) }

    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0 && items.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Box(
        modifier = modifier.onGloballyPositioned {
            rootBounds = it.boundsInWindow()
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = listModifier,
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item ->
                        if (dropAnimation?.itemKey == item.id) {
                            GhostListTemporaryKeyBase + dropAnimation!!.request
                        } else {
                            item.id
                        }
                    },
                ) { index, item ->
                    val key = item.id
                    val itemModifier = Modifier
                        .onGloballyPositioned { itemBounds[key] = it.boundsInWindow() }

                    val animatedModifier = Modifier.animateItem(
                        fadeInSpec = null,
                        placementSpec = tween(durationMillis = GhostListAnimationDurationMillis),
                        fadeOutSpec = null,
                    )

                    GhostListSlot(
                        index = index,
                        dragController = dragController,
                        onDrop = { _, _ -> },
                        modifier = animatedModifier,
                    ) {
                        itemContent(item, itemModifier)
                    }
                }

                item(key = GhostListEndKey) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (items.isEmpty()) 200.dp else insertionSlotHeight)
                            .dropTarget(
                                dragController = dragController,
                                onGiven = { offer ->
//                                    handleDrop(offer, items.size, completion)
                                },
                            ),
                    )
                }
            }

            belowList()
        }
    }
}

@Composable
private fun <T> GhostListSlot(
    index: Int,
    dragController: DragController<T>,
    onDrop: (Offer<T>, Int) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .dropTarget(
                dragController = dragController,
                onGiven = { offer ->
                    val targetIndex = bounds?.let {
                        if (offer.offset.y < it.center.y) index else index + 1
                    } ?: index
                    onDrop(offer, targetIndex)
                },
            )
            .padding(vertical = 4.dp),
    ) {
        content()
    }
}
