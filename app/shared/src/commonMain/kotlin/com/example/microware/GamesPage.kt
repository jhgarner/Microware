package com.example.microware

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.min
import kotlin.math.roundToInt


private data class DeletedGame(
    val game: Game,
    val index: Int,
)

@Composable
internal fun GamesPage(
    games: List<Game>,
    selectGame: (Id) -> Unit,
    handler: (GamesEvent) -> Unit,
) {
    val dragController = remember { DragController<Game>() }
    val offered = dragController.offer
    var deletedGames by remember { mutableStateOf(persistentListOf<DeletedGame>()) }
    var wouldDelete by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
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
                GameDeleteButton(
                    dragController,
                    onUndo = deletedGames.lastOrNull()?.let { lastDeleted ->
                        {
                            deletedGames = deletedGames.removingAt(deletedGames.size - 1)
                            handler { it.addingAt(lastDeleted.index, lastDeleted.game) }
                        }
                    },
                    onDelete = { id ->
                        handler { games ->
                            val i = games.indexOfFirst { it.id == id }
                            deletedGames = deletedGames.adding(DeletedGame(games[i], i))
                            games.removingAt(i)
                        }
                    },
                    onWouldDelete = { wouldDelete = it }
                )
                GameCreateButton(
                    dragController = dragController,
                    onCreate = {
                        val id = Identifiable.nextId()
                        handler { it.adding(Game(World(), persistentListOf(), id)) }
                        selectGame(id)
                    },
                    onEdit = { selectGame(it) },
                )
            }

            Grid(
                items = games,
                dragTarget = dragController,
                dragSource = dragController,
                onMove = { gameId, targetIndex ->
                    handler { games ->
                        val sourceIndex = games.indexOfFirst { it.id == gameId }
                        val game = games[sourceIndex]
                        games.removingAt(sourceIndex).addingAt(targetIndex, game)
                    }
                },
                itemSize = DpSize(128.dp, 256.dp),
                preview = { GamePreview(it) }
            )
        }

        val draggedGame = offered?.item
        if (draggedGame != null) {
            val alpha by animateFloatAsState(
                if (wouldDelete) .5f else 1f,
                tween(durationMillis = 180)
            )
            GameDragGhost(
                game = draggedGame,
                offer = offered,
                alpha = alpha
            )
        }
    }
}

@Composable
private fun RowScope.GameCreateButton(
    dragController: DragController<Game>,
    onCreate: () -> Unit,
    onEdit: (Id) -> Unit,
) {
    val isDragging = dragController.offer != null
    var foreground by remember { mutableStateOf(Palette.Navy) }
    val foregroundColor by animateColorAsState(
        foreground.color,
        tween(durationMillis = 180)
    )
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
                onGiven = { onEdit(it.item.id) },
                onEnter = { foreground = Palette.Cyan },
                onExit = { foreground = Palette.Navy },
                onEnd = { foreground = Palette.Navy },
            )
            .drawDashedBorder(foregroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            CreateTarget(foregroundColor, isDragging)
        }
    }
}

@Composable
private fun GameDeleteButton(
    dragController: DragController<Game>,
    onUndo: (() -> Unit)?,
    onDelete: (Id) -> Unit,
    onWouldDelete: (Boolean) -> Unit,
) {
    val isDragging = dragController.offer != null
    val defaultBackground = if (onUndo == null) Palette.LightGray else Palette.Black
    var dragBackground by remember { mutableStateOf<Palette?>(null) }
    val foreground = dragBackground ?: defaultBackground
    val foregroundColor by animateColorAsState(
        foreground.color,
        tween(delayMillis = 80, durationMillis = 180)
    )
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .height(64.dp)
            .width(64.dp)
            .clip(shape)
            .background(Palette.OffWhite.color)
            .clickable(enabled = !isDragging && onUndo != null, onClick = { onUndo?.invoke() })
            .dropTarget(
                dragController = dragController,
                onGiven = { onDelete(it.item.id) },
                onStart = { dragBackground = Palette.Maroon },
                onEnter = { onWouldDelete(true); dragBackground = Palette.Red },
                onExit = { onWouldDelete(false); dragBackground = Palette.Maroon },
                onEnd = { onWouldDelete(false); dragBackground = null },
            )
            .drawDashedBorder(foregroundColor),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            isDragging,
            enter = fadeIn(tween(durationMillis = 180)),
            exit = fadeOut(tween(durationMillis = 180))
        ) {
            TrashControls(foregroundColor)
        }
        AnimatedVisibility(
            !isDragging,
            enter = fadeIn(tween(durationMillis = 180)),
            exit = fadeOut(tween(durationMillis = 180))
        ) {
            Canvas(modifier = Modifier.size(42.dp)) { drawUndoIcon(foregroundColor) }
        }
    }
}

@Composable
private fun GameDragGhost(
    game: Game,
    offer: Offer<Game>,
    alpha: Float,
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (offer.offset.x - offer.localOffset.x).roundToInt(),
                    (offer.offset.y - offer.localOffset.y).roundToInt(),
                )
            }
            .size(
                width = offer.size.width.toDp(),
                height = offer.size.height.toDp(),
            )
            .alpha(alpha)
            .zIndex(2f)
    ) {
        GamePreview(game)
    }
}

@Composable
private fun GamePreview(game: Game) {
    Canvas(
        modifier = Modifier.fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.OffWhite.color)
            .drawBorder(Palette.DarkGray.color)
    ) {
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

private fun DrawScope.drawUndoIcon(color: Color) {
    val stroke = 2.dp.toPx()
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
        color = color,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    val arrow = Path().apply {
        moveTo(size.width * .22f, size.height * .70f) // Tip pointing down
        lineTo(size.width * .34f, size.height * .50f) // Top-right corner
        lineTo(size.width * .10f, size.height * .50f) // Top-left corner
        close()
    }
    drawPath(arrow, color)
}
