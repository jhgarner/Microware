package com.example.microware

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal const val GhostListAnimationDurationMillis = 300
internal const val GhostListDragGhostAlpha = .7f

private data class GhostListDrop<T : Any>(
    val request: Int,
    val item: T,
    val itemKey: Int,
    val startTopLeftInWindow: Offset,
    val sourceBounds: Rect?,
    val completion: DropCompletion,
)

private const val GhostListTemporaryKeyBase = Int.MIN_VALUE
private const val GhostListEndKey = "ghost-list-end"

@Composable
internal fun <T : Identifiable<T>> GhostingLazyColumn(
    items: List<T>,
    dragController: DragController<T>,
    from: Any,
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
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val listState = rememberLazyListState()
    val itemBounds = remember { mutableStateMapOf<Int, Rect>() }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }
    var dropAnimation by remember { mutableStateOf<GhostListDrop<T>?>(null) }
    var startedDropRequest by remember { mutableStateOf<Int?>(null) }
    val dropOffsetX = remember { Animatable(0f) }
    val dropOffsetY = remember { Animatable(0f) }
    val dropAlpha = remember { Animatable(1f) }
    val sourceFade = remember { Animatable(1f) }
    var nextDropRequest by remember { mutableStateOf(0) }

    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0 && items.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val currentDropAnimation by rememberUpdatedState(dropAnimation)

    DisposableEffect(Unit) {
        onDispose {
            currentDropAnimation?.completion?.complete()
        }
    }

    LaunchedEffect(dropAnimation?.request) {
        val animation = dropAnimation ?: return@LaunchedEffect
        val bounds = snapshotFlow { itemBounds[animation.itemKey] }
            .filterNotNull()
            .first()
        if (startedDropRequest == animation.request) return@LaunchedEffect

        withFrameNanos { }
        val initialOffset = animation.startTopLeftInWindow - bounds.topLeft
        dropOffsetX.snapTo(initialOffset.x)
        dropOffsetY.snapTo(initialOffset.y)
        dropAlpha.snapTo(GhostListDragGhostAlpha)
        sourceFade.snapTo(1f)
        startedDropRequest = animation.request
        animation.completion.complete()

        coroutineScope {
            launch {
                dropOffsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = GhostListAnimationDurationMillis),
                )
            }
            launch {
                dropOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = GhostListAnimationDurationMillis),
                )
            }
            launch {
                dropAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = GhostListAnimationDurationMillis),
                )
            }
            animation.sourceBounds?.let {
                launch {
                    sourceFade.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = GhostListAnimationDurationMillis),
                    )
                }
            }
        }

        if (dropAnimation?.request == animation.request) {
            dropAnimation = null
        }
    }

    fun handleDrop(
        offer: Offer<T>,
        targetIndex: Int,
        completion: DropCompletion,
    ) {
        completion.defer()
        val sourceItem = offer.item
        val sourceBounds = itemBounds[sourceItem.id]
        itemBounds.remove(sourceItem.id)
        onDrop(offer, targetIndex)

        nextDropRequest++
        dropAnimation = GhostListDrop(
            request = nextDropRequest,
            item = droppedItem,
            itemKey = droppedItem.id,
            startTopLeftInWindow = offer.offset - offer.localOffset,
            sourceBounds = sourceBounds,
            completion = completion,
        )
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
                    val isDropTarget = dropAnimation?.itemKey == key
                    val isSource = offered != null && offered.item === item
                    val targetRequest = dropAnimation?.request
                    val targetHasStarted = isDropTarget && startedDropRequest == targetRequest
                    val sourceAlpha = if (isSource) draggedItemAlpha(willBeDeleted) else 1f
                    val targetAlpha = if (isDropTarget) {
                        if (targetHasStarted) dropAlpha.value else 0f
                    } else {
                        1f
                    }
                    val itemModifier = Modifier
                        .onGloballyPositioned { itemBounds[key] = it.boundsInWindow() }
                        .alpha(sourceAlpha * targetAlpha)
                        .then(
                            if (targetHasStarted) {
                                Modifier.offset {
                                    IntOffset(
                                        dropOffsetX.value.roundToInt(),
                                        dropOffsetY.value.roundToInt(),
                                    )
                                }
                            } else {
                                Modifier
                            },
                        )
                        .zIndex(if (isDropTarget) 2f else 0f)
                        .then(
                            if (dropAnimation == null) {
                                Modifier.dragSource(
                                    dragController = dragController,
                                    from = from,
                                    item = item,
                                )
                            } else {
                                Modifier
                            },
                        )

                    val animatedModifier = if (offered?.item?.id != item.id) {
                        Modifier.animateItem(
                            fadeInSpec = null,
                            placementSpec = if (isDropTarget) {
                                null
                            } else {
                                tween(durationMillis = GhostListAnimationDurationMillis)
                            },
                            fadeOutSpec = null,
                        )
                    } else {
                        Modifier
                    }

                    GhostListSlot(
                        index = index,
                        dragController = dragController,
                        onDrop = ::handleDrop,
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
                                onGiven = { offer, completion ->
                                    handleDrop(offer, items.size, completion)
                                },
                            ),
                    )
                }
            }

            belowList()
        }

        val ghostItem = offered?.item
        if (ghostItem != null && ghostContent != null) {
            val sourceBounds = itemBounds[itemKey(ghostItem)]
                ?: dropAnimation
                    ?.takeIf { it.itemKey == itemKey(ghostItem) }
                    ?.sourceBounds
            val horizontalPadding = contentPadding.calculateLeftPadding(layoutDirection) +
                    contentPadding.calculateRightPadding(layoutDirection)
            val width = ghostWidth
                ?: sourceBounds?.let { with(density) { it.width.toDp() } }
                ?: rootBounds?.let { with(density) { it.width.toDp() } - horizontalPadding }
            ghostContent(
                ghostItem,
                Modifier
                    .then(width?.let { Modifier.width(it) } ?: Modifier)
                    .offset {
                        IntOffset(
                            (offered.offset.x - (rootBounds?.left ?: 0f) - offered.localOffset.x)
                                .roundToInt(),
                            (offered.offset.y - (rootBounds?.top ?: 0f) - offered.localOffset.y)
                                .roundToInt(),
                        )
                    }
                    .alpha(GhostListDragGhostAlpha)
                    .zIndex(2f),
            )
        }

        dropAnimation?.sourceBounds?.let { sourceBounds ->
            val width = with(density) { sourceBounds.width.toDp() }
            ghostContent?.invoke(
                dropAnimation!!.item,
                Modifier
                    .width(width)
                    .offset {
                        IntOffset(
                            (sourceBounds.left - (rootBounds?.left ?: 0f)).roundToInt(),
                            (sourceBounds.top - (rootBounds?.top ?: 0f)).roundToInt(),
                        )
                    }
                    .alpha(sourceFade.value)
                    .zIndex(-1f),
            )
        }
    }
}

@Composable
private fun <T> GhostListSlot(
    index: Int,
    dragController: DragController<T>,
    onDrop: (Offer<T>, Int, DropCompletion) -> Unit,
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
                onGiven = { offer, completion ->
                    val targetIndex = bounds?.let {
                        if (offer.offset.y < it.center.y) index else index + 1
                    } ?: index
                    onDrop(offer, targetIndex, completion)
                },
            )
            .padding(vertical = 4.dp),
    ) {
        content()
    }
}
