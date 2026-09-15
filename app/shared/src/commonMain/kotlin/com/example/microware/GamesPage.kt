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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.min
import kotlin.math.roundToInt

private object GameListDragSource

private data class DeletedGame(
    val game: Game,
    val index: Int,
)

@Composable
internal fun GamesPage(
    games: List<Game>,
    selectGame: (Int) -> Unit,
    handler: (GamesEvent) -> Unit,
) {
    val dragController = remember { DragController<Game>() }
    val offered = dragController.offer
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var deletedGames by remember { mutableStateOf(persistentListOf<DeletedGame>()) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.positionInWindow() }
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
                deletedGames.lastOrNull()?.let { lastDeleted ->
                    IconButton(
                        description = "Undo delete",
                        padding = 0.dp,
                        buttonSize = 56.dp,
                        onClick = {
                            deletedGames = deletedGames.removingAt(deletedGames.size - 1)
                            handler { it.addingAt(lastDeleted.index, lastDeleted.game) }
                        },
                    ) { _ -> drawUndoIcon() }
                }
                GameCreateButton(
                    dragController = dragController,
                    onCreate = {
                        val id = Identifiable.nextId()
                        handler { it.adding(Game(World(), persistentListOf(), id)) }
                        selectGame(id)
                    },
                    onDelete = { id ->
                        handler { games ->
                            val i = games.indexOfFirst { it.id == id }
                            deletedGames = deletedGames.adding(DeletedGame(games[i], i))
                            games.removingAt(i)
                        }
                    },
                )
            }

            GameGrid(
                games = games,
                dragController = dragController,
                onOpenGame = selectGame,
                onMoveGame = { gameId, targetIndex ->
                    handler { games ->
                        val sourceIndex = games.indexOfFirst { it.id == gameId }
                        val game = games[sourceIndex]
                        games.removingAt(sourceIndex).addingAt(targetIndex, game)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }

        val draggedGame = offered?.item
        if (draggedGame != null) {
            GameDragGhost(
                game = draggedGame,
                offer = offered,
                rootOrigin = rootOrigin,
            )
        }
    }
}

@Composable
private fun RowScope.GameCreateButton(
    dragController: DragController<Game>,
    onCreate: () -> Unit,
    onDelete: (Int) -> Unit,
) {
    val isDragging = dragController.offer != null
    val foreground = if (isDragging) Palette.Red.color else Palette.Navy.color
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .clip(shape)
            .background(Palette.OffWhite.color)
            .clickable(enabled = !isDragging, onClick = onCreate)
            .dropTarget(
                dragController = dragController,
                onGiven = { onDelete(it.item.id) },
            )
            .drawDashedBorder(foreground),
        contentAlignment = Alignment.Center,
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
    dragController: DragController<Game>,
    onOpenGame: (Int) -> Unit,
    onMoveGame: (Int, Int) -> Unit,
    modifier: Modifier,
) {
    val gameBounds = remember { mutableStateMapOf<Int, Rect>() }
    val slotBounds = games
        .mapNotNull { gameBounds[it.id] }
        .sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
    var gridBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(
        dragController.offer?.item?.id,
        dragController.offer?.offset,
        games,
        slotBounds,
        gridBounds,
    ) {
        val offer = dragController.offer
        val draggedGame = offer?.item
        if (
            draggedGame == null ||
            gridBounds?.contains(offer.offset) != true
        ) {
            return@LaunchedEffect
        }

        val targetIndex = nearestGridSlot(slotBounds, offer.offset)
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
                onGiven = { offer ->
                    nearestGridSlot(slotBounds, offer.offset)?.let { targetIndex ->
                        onMoveGame(offer.item.id, targetIndex)
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
                onOpen = { onOpenGame(game.id) },
                onDrop = { offer ->
                    val targetIndex = nearestGridSlot(slotBounds, offer.offset)
                    targetIndex?.let { onMoveGame(offer.item.id, it) }
                },
                onBoundsChanged = { gameBounds[game.id] = it },
                modifier = Modifier.animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec = tween(durationMillis = 180),
                ),
            )
        }
    }
}

@Composable
private fun GameGridItem(
    game: Game,
    dragController: DragController<Game>,
    onOpen: () -> Unit,
    onDrop: (Offer<Game>) -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier,
) {
    val offer = dragController.offer
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .alpha(
                if (offer?.item != null &&
                    offer.item.id == game.id
                ) {
                    0f
                } else {
                    1f
                }
            )
            .aspectRatio(1f)
            .clip(shape)
            .onGloballyPositioned { onBoundsChanged(it.boundsInWindow()) }
            .background(Palette.OffWhite.color)
            .drawBorder(Palette.DarkGray.color)
            .dropTarget(
                dragController = dragController,
                onGiven = { onDrop(it) },
            )
            .dragSource(dragController, GameListDragSource, game)
            .pointerInput(onOpen) {
                racing {
                    sequence {
                        expectTap()
                        onOpen()
                    }
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
    offer: Offer<Game>,
    rootOrigin: Offset,
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (offer.offset.x - offer.localOffset.x).roundToInt(),
                    (offer.offset.y - offer.localOffset.y).roundToInt(),
                )
            }
            .size(
                width = with(density) { offer.size.width.toDp() },
                height = offer.size.height.toDp(),
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
