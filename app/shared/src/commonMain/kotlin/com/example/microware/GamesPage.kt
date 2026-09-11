package com.example.microware

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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

private object GameListDragSource

@Composable
internal fun GamesPage(
    games: List<Game>,
    canUndo: Boolean,
    onOpenGame: (Int) -> Unit,
    onCreateGame: () -> Int,
    onDeleteGame: (Int) -> Unit,
    onUndoDelete: () -> Unit,
    onMoveGame: (Int, Int) -> Unit,
) {
    val dragController = remember { DragController() }
    val offered = dragController.offer
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    val gameBounds = remember { mutableStateMapOf<Int, Rect>() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                rootOrigin = it.positionInWindow()
                dragController.setRootCoordinates(it)
            }
            .dragObserver(dragController),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = insets.leftDp(),
                    top = insets.topDp(),
                    end = insets.rightDp(),
                    bottom = insets.bottomDp(),
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (canUndo) {
                    IconButton(
                        description = "Undo delete",
                        padding = 0.dp,
                        buttonSize = 56.dp,
                        onClick = onUndoDelete,
                    ) { _ -> drawUndoIcon() }
                }
                GameCreateButton(
                    modifier = Modifier.weight(1f),
                    isDragging = offered != null,
                    dragController = dragController,
                    offered = offered,
                    onCreate = { onOpenGame(onCreateGame()) },
                    onDelete = onDeleteGame,
                )
            }

            GameGrid(
                games = games,
                dragController = dragController,
                offered = offered,
                onOpenGame = onOpenGame,
                onMoveGame = onMoveGame,
                gameBounds = gameBounds,
                onBoundsChanged = { gameId, bounds -> gameBounds[gameId] = bounds },
                modifier = Modifier.weight(1f),
            )
        }

        val draggedGame = (offered?.item as? Game)
            ?.takeUnless { offered.released }
        val sourceBounds = draggedGame?.let { gameBounds[it.id] }
        if (draggedGame != null && sourceBounds != null) {
            GameDragGhost(
                game = draggedGame,
                offer = offered,
                sourceBounds = sourceBounds,
                rootOrigin = rootOrigin,
            )
        }
    }
}

@Composable
private fun GameCreateButton(
    modifier: Modifier,
    isDragging: Boolean,
    dragController: DragController,
    offered: Offer?,
    onCreate: () -> Unit,
    onDelete: (Int) -> Unit,
) {
    val foreground = if (isDragging) Palette.Red.color else Palette.Navy.color
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(shape)
            .background(Palette.OffWhite.color)
            .clickable(enabled = !isDragging, onClick = onCreate)
            .dropTarget(
                dragController = dragController,
                offered = offered,
                onGiven = { offer, _ ->
                    (offer.item as? Game)?.let { onDelete(it.id) }
                },
            )
            .drawDashedBorder(foreground),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (isDragging) {
                TrashControls(foreground)
            } else {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    drawPlusIcon(foreground)
                }
            }
        }
    }
}

@Composable
private fun GameGrid(
    games: List<Game>,
    dragController: DragController,
    offered: Offer?,
    onOpenGame: (Int) -> Unit,
    onMoveGame: (Int, Int) -> Unit,
    gameBounds: Map<Int, Rect>,
    onBoundsChanged: (Int, Rect) -> Unit,
    modifier: Modifier,
) {
    val slotBounds = games
        .mapNotNull { gameBounds[it.id] }
        .sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
    val draggedGame = offered?.item as? Game
    var gridBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(
        draggedGame?.id,
        offered?.offset,
        offered?.released,
        games,
        slotBounds,
        gridBounds,
    ) {
        if (
            draggedGame == null ||
            offered.released ||
            gridBounds?.contains(offered.offset) != true
        ) {
            return@LaunchedEffect
        }

        val targetIndex = nearestGridSlot(slotBounds, offered.offset)
            ?: return@LaunchedEffect
        onMoveGame(draggedGame.id, targetIndex)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { gridBounds = it.boundsInWindow() }
            .dropTarget(
                dragController = dragController,
                offered = offered,
                onGiven = { offer, _ ->
                    (offer.item as? Game)?.let { draggedGame ->
                        nearestGridSlot(slotBounds, offer.offset)?.let { targetIndex ->
                            onMoveGame(draggedGame.id, targetIndex)
                        }
                    }
                },
            ),
        contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = games,
            key = { _, game -> game.id },
        ) { _, game ->
            GameGridItem(
                game = game,
                dragController = dragController,
                offered = offered,
                onOpen = { onOpenGame(game.id) },
                onDrop = { offer ->
                    (offer.item as? Game)?.let { draggedGame ->
                        val targetIndex = nearestGridSlot(slotBounds, offer.offset)
                        targetIndex?.let { onMoveGame(draggedGame.id, it) }
                    }
                },
                onBoundsChanged = { onBoundsChanged(game.id, it) },
                modifier = Modifier.animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec = tween<IntOffset>(durationMillis = 180),
                ),
            )
        }
    }
}

@Composable
private fun GameGridItem(
    game: Game,
    dragController: DragController,
    offered: Offer?,
    onOpen: () -> Unit,
    onDrop: (Offer) -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .onGloballyPositioned { onBoundsChanged(it.boundsInWindow()) }
            .background(Palette.OffWhite.color)
            .drawBorder(Palette.DarkGray.color)
            .dropTarget(
                dragController = dragController,
                offered = offered,
                onGiven = { offer, _ ->
                    onDrop(offer)
                },
            )
            .alpha(
                if (offered?.item is Game &&
                    offered.item.id == game.id &&
                    !offered.released
                ) {
                    0f
                } else {
                    1f
                }
            )
            .dragSource(dragController, GameListDragSource, game) {
                sequence {
                    expectTap()
                    onOpen()
                }
            }
            .semantics { contentDescription = "Open game" },
    ) {
        GamePreview(game)
    }
}

@Composable
private fun GameDragGhost(
    game: Game,
    offer: Offer,
    sourceBounds: Rect,
    rootOrigin: Offset,
) {
    val density = LocalDensity.current
    Box(
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
            .alpha(1f)
            .zIndex(2f)
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.OffWhite.color)
            .drawBorder(Palette.DarkGray.color),
    ) {
        GamePreview(game)
    }
}

@Composable
private fun GamePreview(game: Game) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val world = game.world
        val scale = min(
            size.width / world.background.width,
            size.height / world.background.height,
        )
        val worldWidth = (world.background.width * scale).roundToInt().coerceAtLeast(1)
        val worldHeight = (world.background.height * scale).roundToInt().coerceAtLeast(1)
        val origin = Offset(
            (size.width - worldWidth) / 2f,
            (size.height - worldHeight) / 2f,
        )

        drawImage(
            image = world.background.bitmap,
            dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
            dstSize = IntSize(worldWidth, worldHeight),
        )
        world.objects.forEach { obj ->
            val objectWidth = (obj.sprite.width * scale).roundToInt().coerceAtLeast(1)
            val objectHeight = (obj.sprite.height * scale).roundToInt().coerceAtLeast(1)
            drawImage(
                image = obj.sprite.bitmap,
                dstOffset = IntOffset(
                    (origin.x + obj.offset.x * scale).roundToInt(),
                    (origin.y + obj.offset.y * scale).roundToInt(),
                ),
                dstSize = IntSize(objectWidth, objectHeight),
            )
        }
    }
}

private fun Modifier.drawDashedBorder(color: Color): Modifier = drawBehind {
    val strokeWidth = 2.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(10.dp.toPx(), 7.dp.toPx()),
            ),
        ),
    )
}

private fun Modifier.drawBorder(
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

private fun DrawScope.drawPlusIcon(color: Color) {
    val stroke = 3.dp.toPx()
    drawLine(
        color = color,
        start = Offset(size.width * .2f, size.height * .5f),
        end = Offset(size.width * .8f, size.height * .5f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(size.width * .5f, size.height * .2f),
        end = Offset(size.width * .5f, size.height * .8f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawUndoIcon() {
    val stroke = 3.dp.toPx()
    val path = Path().apply {
        moveTo(size.width * .22f, size.height * .5f)
        cubicTo(
            size.width * .22f,
            size.height * .23f,
            size.width * .5f,
            size.height * .12f,
            size.width * .74f,
            size.height * .26f,
        )
        cubicTo(
            size.width * .97f,
            size.height * .4f,
            size.width * .9f,
            size.height * .72f,
            size.width * .62f,
            size.height * .8f,
        )
    }
    drawPath(
        path = path,
        color = Color.Black,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    val arrow = Path().apply {
        moveTo(size.width * .24f, size.height * .74f) // Tip pointing down
        lineTo(size.width * .44f, size.height * .50f) // Top-right corner
        lineTo(size.width * .04f, size.height * .50f) // Top-left corner
        close()
    }
    drawPath(arrow, Color.Black)
}
