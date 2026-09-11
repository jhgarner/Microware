package com.example.microware

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.min
import kotlin.math.roundToInt

private val ObjectOrderCardHeight = 88.dp
private val ObjectOrderCardShape = RoundedCornerShape(8.dp)

@Composable
internal fun ObjectOrderEditor(
    world: World,
    onWorldChange: (World) -> Unit,
    state: WorldEditorState,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val offer = state.dragController.offer
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    val objectBounds = remember { mutableStateMapOf<Int, Rect>() }
    val draggedObject = offer?.item as? Obj
    val sourceBounds = draggedObject?.let { objectBounds[it.id] }

    BoxWithConstraints(
        modifier = modifier
            .onGloballyPositioned { rootOrigin = it.positionInWindow() }
            .zIndex(if (offer != null) 1f else 0f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = insets.leftDp(),
                    top = insets.topDp(),
                    end = insets.rightDp(),
                ),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .dropTarget(
                        dragController = state.dragController,
                        offered = offer,
                        onGiven = { droppedOffer, _ ->
                            reorderObjectAtEnd(
                                world = world,
                                offer = droppedOffer,
                                onWorldChange = onWorldChange,
                            )
                        },
                    ),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                itemsIndexed(
                    items = world.objects,
                    key = { _, obj -> obj.id },
                ) { index, obj ->
                    ObjectOrderSlot(
                        world = world,
                        obj = obj,
                        index = index,
                        offer = offer,
                        dragController = state.dragController,
                        onWorldChange = onWorldChange,
                        onBoundsChanged = { bounds -> objectBounds[obj.id] = bounds },
                        modifier = Modifier.animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = tween<IntOffset>(durationMillis = 180),
                        ),
                    )
                }
                item(key = "object-order-end") {
                    ObjectOrderInsertionSlot(
                        dragController = state.dragController,
                        offer = offer,
                        onGiven = { droppedOffer, _ ->
                            reorderObjectAtEnd(
                                world = world,
                                offer = droppedOffer,
                                onWorldChange = onWorldChange,
                            )
                        },
                    )
                }
            }
            Spacer(
                modifier = Modifier.height(
                    HandleSize + TrashGap + insets.bottomDp(),
                ),
            )
        }

        if (draggedObject != null && !offer.released && sourceBounds != null) {
            ObjectOrderDragGhost(
                obj = draggedObject,
                world = world,
                offer = offer,
                sourceBounds = sourceBounds,
                rootOrigin = rootOrigin,
            )
        }
    }
}

@Composable
private fun ObjectOrderSlot(
    world: World,
    obj: Obj,
    index: Int,
    offer: Offer?,
    dragController: DragController,
    onWorldChange: (World) -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier,
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .dropTarget(
                dragController = dragController,
                offered = offer,
                onGiven = { droppedOffer, _ ->
                    val droppedObject = droppedOffer.item as? Obj ?: return@dropTarget
                    if (droppedOffer.from !is World) return@dropTarget
                    val targetIndex = bounds?.let {
                        if (droppedOffer.offset.y < it.center.y) index else index + 1
                    } ?: index
                    onWorldChange(world.reorderObject(droppedObject.id, targetIndex))
                },
            )
            .padding(vertical = 4.dp),
    ) {
        ObjectOrderCard(
            obj = obj,
            world = world,
            offer = offer,
            dragController = dragController,
            onBoundsChanged = onBoundsChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(ObjectOrderCardHeight),
        )
    }
}

@Composable
private fun ObjectOrderInsertionSlot(
    dragController: DragController,
    offer: Offer?,
    onGiven: (Offer, DropCompletion) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ObjectOrderCardHeight)
            .dropTarget(
                dragController = dragController,
                offered = offer,
                onGiven = onGiven,
            ),
    )
}

@Composable
private fun ObjectOrderCard(
    obj: Obj,
    world: World,
    offer: Offer?,
    dragController: DragController? = null,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = ObjectOrderCardShape
    val isDragged = offer?.item is Obj &&
            offer.item.id == obj.id &&
            !offer.released
    var cardModifier = modifier
        .onGloballyPositioned { onBoundsChanged?.invoke(it.boundsInWindow()) }
        .clip(shape)
        .background(Palette.OffWhite.color)
        .border(2.dp, Palette.DarkGray.color, shape)
        .alpha(if (isDragged) 0f else 1f)
        .semantics { contentDescription = "Reorder object" }

    if (dragController != null) {
        cardModifier = cardModifier.dragSource(dragController, world, obj)
    }

    Row(
        modifier = cardModifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            world.logicTagFor(obj.id)?.let { tag ->
                Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                    with(tag) { draw() }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Canvas(Modifier.size(72.dp)) {
            drawSpriteFit(obj.sprite)
        }
    }
}

@Composable
private fun ObjectOrderDragGhost(
    obj: Obj,
    world: World,
    offer: Offer,
    sourceBounds: Rect,
    rootOrigin: Offset,
) {
    val density = LocalDensity.current
    ObjectOrderCard(
        obj = obj,
        world = world,
        offer = null,
        modifier = Modifier
            .offset {
                IntOffset(
                    (offer.offset.x - rootOrigin.x - offer.localOffset.x).roundToInt(),
                    (offer.offset.y - rootOrigin.y - offer.localOffset.y).roundToInt(),
                )
            }
            .size(
                width = with(density) { sourceBounds.width.toDp() },
                height = with(density) { sourceBounds.height.toDp() },
            )
            .zIndex(2f),
    )
}

private fun reorderObjectAtEnd(
    world: World,
    offer: Offer,
    onWorldChange: (World) -> Unit,
) {
    val obj = offer.item as? Obj ?: return
    if (offer.from !is World) return
    onWorldChange(world.reorderObject(obj.id, world.objects.size))
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
