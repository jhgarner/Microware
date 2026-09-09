package com.example.microware

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal object LogicListDragSource
internal data class LogicListItemDragSource(val index: Int)

internal object LogicTrashDragSource
internal object LogicLibraryDragSource

internal data class LogicDropAnimation(
    val request: Int,
    val logic: Logic,
    val startTopLeftInWindow: Offset,
    val sourceBounds: Rect?,
    val completion: DropCompletion,
)

internal const val LogicDropAnimationDurationMillis = 300
internal const val LogicDragGhostAlpha = .7f

private const val TemporaryLogicKeyBase = Long.MIN_VALUE

@Composable
internal fun LogicTable(
    items: List<Logic>,
    dragController: DragController,
    from: Any,
    modifier: Modifier = Modifier,
    offered: Offer? = null,
    willBeDeleted: Boolean = false,
    onDrop: ((Offer, Int, DropCompletion) -> Unit)? = null,
    dropAnimation: LogicDropAnimation? = null,
    onItemBoundsChanged: ((Int, Rect) -> Unit)? = null,
) {
    val hasInsertionTargets = onDrop != null
    val density = LocalDensity.current
    val state = rememberLazyListState()
    var rootOriginInWindow by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier.onGloballyPositioned {
            rootOriginInWindow = it.positionInWindow()
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(
                horizontal = 8.dp,
                vertical = if (hasInsertionTargets) 4.dp else 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (hasInsertionTargets) 0.dp else 8.dp),
        ) {
            itemsIndexed(
                items = items,
                key = { _, logic ->
                    if (dropAnimation?.logic === logic) {
                        TemporaryLogicKeyBase + dropAnimation.request
                    } else {
                        logic.id.toLong()
                    }
                },
            ) { index, logic ->
                val isDropTarget = dropAnimation?.logic === logic
                val itemModifier = if (hasInsertionTargets) {
                    Modifier
                        .animateItem(
                            fadeInSpec = null,
                            placementSpec = if (isDropTarget) {
                                null
                            } else {
                                tween<IntOffset>(durationMillis = LogicDropAnimationDurationMillis)
                            },
                            fadeOutSpec = null,
                        )
                        .zIndex(if (isDropTarget) 2f else 0f)
                } else {
                    Modifier
                }
                val card = @Composable {
                    LogicCard(
                        logic = logic,
                        modifier = Modifier.fillMaxWidth(),
                        dragController = dragController,
                        from = if (from === LogicListDragSource) {
                            LogicListItemDragSource(index)
                        } else {
                            from
                        },
                        offered = offered,
                        willBeDeleted = willBeDeleted,
                        index = index,
                        dropAnimation = dropAnimation,
                        onBoundsChanged = onItemBoundsChanged,
                    )
                }
                if (onDrop != null) {
                    LogicListSlot(
                        index = index,
                        dragController = dragController,
                        offered = offered,
                        onDrop = onDrop,
                        modifier = itemModifier,
                        content = card,
                    )
                } else {
                    card()
                }
            }
            if (onDrop != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight()
                            .height(if (items.isEmpty()) 200.dp else 72.dp)
                            .dropTarget(
                                dragController = dragController,
                                offered = offered,
                                onGiven = { offer, completion ->
                                    completion.defer()
                                    onDrop(offer, items.size, completion)
                                },
                            ),
                    )
                }
            }
        }

        dropAnimation?.sourceBounds?.let { sourceBounds ->
            val width = with(density) { sourceBounds.width.toDp() }
            val alpha = remember(dropAnimation.request) { Animatable(1f) }
            LaunchedEffect(dropAnimation.request) {
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = LogicDropAnimationDurationMillis),
                )
            }
            LogicCard(
                logic = dropAnimation.logic,
                modifier = Modifier
                    .width(width)
                    .offset {
                        IntOffset(
                            (sourceBounds.left - rootOriginInWindow.x).roundToInt(),
                            (sourceBounds.top - rootOriginInWindow.y).roundToInt(),
                        )
                    }
                    .alpha(alpha.value)
                    .zIndex(-1f),
            )
        }
    }
}

@Composable
private fun LogicListSlot(
    index: Int,
    dragController: DragController,
    offered: Offer?,
    onDrop: (Offer, Int, DropCompletion) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .dropTarget(
                dragController = dragController,
                offered = offered,
                onGiven = { offer, completion ->
                    completion.defer()
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

@Composable
internal fun LogicCard(
    logic: Logic,
    modifier: Modifier = Modifier,
    dragController: DragController? = null,
    from: Any = Unit,
    offered: Offer? = null,
    willBeDeleted: Boolean = false,
    index: Int? = null,
    dropAnimation: LogicDropAnimation? = null,
    onBoundsChanged: ((Int, Rect) -> Unit)? = null,
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val source = offered?.from as? LogicListItemDragSource
    val isSource = source?.index == index && offered?.item === logic
    val isDropTarget = dropAnimation?.logic === logic
    val dropOffsetX = remember { Animatable(0f) }
    val dropOffsetY = remember { Animatable(0f) }
    val dropAlpha = remember { Animatable(1f) }
    var startedDropRequest by remember { mutableStateOf<Int?>(null) }
    val dropRequest = dropAnimation?.request

    LaunchedEffect(dropAnimation?.request, isDropTarget, bounds) {
        if (!isDropTarget) return@LaunchedEffect

        withFrameNanos { }
        val targetBounds = bounds ?: return@LaunchedEffect
        val animation = dropAnimation
        if (startedDropRequest == animation.request) return@LaunchedEffect
        val initialOffset = animation.startTopLeftInWindow - targetBounds.topLeft
        dropOffsetX.snapTo(initialOffset.x)
        dropOffsetY.snapTo(initialOffset.y)
        dropAlpha.snapTo(LogicDragGhostAlpha)
        startedDropRequest = animation.request
        animation.completion.complete()

        coroutineScope {
            launch {
                dropOffsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = LogicDropAnimationDurationMillis),
                )
            }
            launch {
                dropOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = LogicDropAnimationDurationMillis),
                )
            }
            launch {
                dropAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = LogicDropAnimationDurationMillis),
                )
            }
        }
    }

    val renderedDropAlpha = if (isDropTarget && startedDropRequest != dropRequest) {
        0f
    } else {
        dropAlpha.value
    }
    val opacity by animateFloatAsState(
        targetValue = if (isSource && willBeDeleted) .5f else 1f,
        label = "logic drag opacity",
    )

    var cardModifier = modifier
        .onGloballyPositioned {
            val newBounds = it.boundsInWindow()
            bounds = newBounds
            index?.let { itemIndex -> onBoundsChanged?.invoke(itemIndex, newBounds) }
        }
        .offset {
            IntOffset(
                dropOffsetX.value.roundToInt(),
                dropOffsetY.value.roundToInt(),
            )
        }
        .alpha(opacity * renderedDropAlpha)
        .zIndex(if (isDropTarget) 2f else 0f)
        .semantics { contentDescription = "Drag logic item" }

    if (dragController != null) {
        cardModifier = cardModifier.dragSource(dragController, from, logic)
    }

    Row(
        modifier = cardModifier
            .background(Palette.OffWhite.color)
            .border(2.dp, Palette.Black.color, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(40.dp).padding(5.dp)) {
            drawTrigger(logic.trigger)
        }
        Canvas(Modifier.size(28.dp).padding(4.dp)) {
            drawArrowIcon()
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                logic.actions.take(5).forEach { action ->
                    Canvas(Modifier.size(36.dp).padding(4.dp)) {
                        drawAction(action)
                    }
                }
            }
        }
    }
}

@Composable
internal fun LogicDragGhost(
    offer: Offer?,
    rootOriginInWindow: Offset,
    width: Dp,
) {
    val logic = offer?.item as? Logic ?: return
    LogicCard(
        logic = logic,
        modifier = Modifier
            .width(width)
            .offset {
                IntOffset(
                    (offer.offset.x - rootOriginInWindow.x - offer.localOffset.x).roundToInt(),
                    (offer.offset.y - rootOriginInWindow.y - offer.localOffset.y).roundToInt(),
                )
            }
            .alpha(LogicDragGhostAlpha)
            .zIndex(2f),
    )
}
