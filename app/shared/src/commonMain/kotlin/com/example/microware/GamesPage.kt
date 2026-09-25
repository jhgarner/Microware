package com.example.microware

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.min
import kotlin.math.roundToInt

private object GamesTrashDragSource

@Composable
internal fun GamesPage(
    games: List<Game>,
    selectGame: (Id) -> Unit,
    handler: (GamesEvent) -> Unit,
) {
    val dragController = remember { DragController<Game>() }
    val offered = dragController.offer
    var wouldDelete by remember { mutableStateOf(false) }
    val (trashDrawerOpen, setTrashDrawerOpen) = remember { mutableStateOf(false) }.apply {
        if (offered != null) value = false
    }

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
            Box(Modifier.weight(1f)) {
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
                    onDrop = { offer, targetIndex ->
                        val restoredGame = offer.item.newId()
                        handler { it.addingAt(targetIndex, restoredGame) }
                    },
                    itemSize = DpSize(128.dp, 256.dp),
                    preview = { GamePreview(it) },
                )
            }
            Spacer(Modifier.height(HandleSize + TrashGap))
        }

        TrashHandle(
            dragTarget = dragController,
            dragSource = dragController,
            trashSourceId = GamesTrashDragSource,
            activeEdge = if (trashDrawerOpen) HandleEdge.Left else null,
            setExpanded = { setTrashDrawerOpen(it == HandleEdge.Left) },
            willBeDeleted = { wouldDelete = it },
            onDelete = { game ->
                handler { games ->
                    val index = games.indexOfFirst { it.id == game.id }
                    if (index >= 0) games.removingAt(index) else games
                }
            },
            garbagePanel = { source, trash ->
                GamesTrashPanel(
                    games = trash,
                    dragSource = source,
                    dragTarget = dragController,
                )
            },
        )
        GameEditorHandle(
            dragController = dragController,
            onCreate = {
                val game = Game(World(), persistentListOf())
                handler { it.adding(game) }
                selectGame(game.id)
            },
            onDrop = { offer ->
                val game = if (offer.originatesFromGrid(GamesTrashDragSource)) {
                    offer.item.newId().also { restored -> handler { it.adding(restored) } }
                } else {
                    offer.item
                }
                selectGame(game.id)
            },
        )

        ButtonHandle(HandleType.Library)

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
private fun BoxWithConstraintsScope.GameEditorHandle(
    dragController: DragController<Game>,
    onCreate: () -> Unit,
    onDrop: (Offer<Game>) -> Unit,
) {
    val isDragging = dragController.offer != null
    var isHovered by remember { mutableStateOf(false) }

    ButtonHandle(
        type = if (isDragging) HandleType.Edit else HandleType.Create,
        highlight = isHovered,
        state = if (isDragging) {
            DrawerButtonState.Dragging
        } else {
            DrawerButtonState.Waiting(onCreate)
        },
        modifier = Modifier.dropTarget(
            dragController = dragController,
            onGiven = onDrop,
            onEnter = { isHovered = true },
            onExit = { isHovered = false },
            onEnd = { isHovered = false },
        ),
        presentation = HandlePresentation.FloatingAction,
    )
}

@Composable
private fun GamesTrashPanel(
    games: List<Game>,
    dragSource: DragSource<Game>,
    dragTarget: DragTarget<Game>,
) {
    Grid(
        items = games,
        dragSource = dragSource,
        dragTarget = dragTarget,
        sourceId = GamesTrashDragSource,
        onMove = { _, _ -> },
        itemSize = DpSize(96.dp, 192.dp),
        preview = { GamePreview(it) },
    )
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
            .zIndex(0.5f)
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
