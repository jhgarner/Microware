package com.example.microware.editor.sprite
import com.example.microware.editor.logic.LogicTagPicker
import com.example.microware.game.model.LogicTag
import com.example.microware.game.sprite.Palette
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteBounds
import com.example.microware.game.sprite.scaled
import com.example.microware.game.sprite.tiled
import com.example.microware.game.sprite.transparent
import com.example.microware.ui.gestures.racing
import com.example.microware.ui.gestures.sequence

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

private const val BackgroundTileSize = 128f

private data class Cell(val row: Int, val col: Int)

private fun cellsBetween(start: Cell, end: Cell): List<Cell> {
    val cells = mutableListOf<Cell>()
    var row = start.row
    var col = start.col
    val rowStep = if (row < end.row) 1 else -1
    val colStep = if (col < end.col) 1 else -1
    val rowDistance = -abs(end.row - row)
    val colDistance = abs(end.col - col)
    var error = colDistance + rowDistance

    while (true) {
        cells += Cell(row, col)
        if (row == end.row && col == end.col) {
            break
        }

        val doubledError = 2 * error
        if (doubledError >= rowDistance) {
            error += rowDistance
            col += colStep
        }
        if (doubledError <= colDistance) {
            error += colDistance
            row += rowStep
        }
    }
    return cells
}

@Composable
fun EditableCanvas(
    sprite: Sprite,
    commit: (Sprite) -> Unit,
    resize: (SpriteBounds) -> Unit,
    modifier: Modifier = Modifier,
    editorKey: Any = Unit,
    logicTag: LogicTag? = null,
    onLogicTagChange: (LogicTag?) -> Unit = {},
    positionEditor: (@Composable () -> Unit)? = null,
) {
    var selectedTool by remember(editorKey) { mutableStateOf(SpriteEditorTool.Pen) }
    var penColor by remember(editorKey) { mutableStateOf(Palette.Navy) }
    val boundsState = remember(editorKey) { mutableStateOf(SpriteBounds.original(sprite)) }
    val panState = remember(editorKey) { mutableStateOf(Offset.Zero) }
    val zoomState = remember(editorKey) { mutableStateOf(1f) }
    val viewportSizeState = remember(editorKey) { mutableStateOf(IntSize.Zero) }
    val draft = remember(editorKey) { MutableSpriteDraft(sprite) }
    LaunchedEffect(sprite) {
        draft.synchronize(sprite)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SpriteToolPicker(
                selectedTool = selectedTool,
                onSelectTool = { selectedTool = it },
                showPositionTool = positionEditor != null,
            )
            LogicTagPicker(
                selectedTag = logicTag,
                onSelectTag = onLogicTagChange,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
        when (selectedTool) {
            SpriteEditorTool.Pen -> PenOptions(penColor) { penColor = it }
            SpriteEditorTool.Resize -> ResizeOptions(
                canReset = boundsState.value != SpriteBounds.original(sprite),
                onReset = { boundsState.value = SpriteBounds.original(sprite) },
                onConfirm = {
                    val committedBounds = boundsState.value
                    rebaseViewportForResize(
                        sprite = sprite,
                        bounds = committedBounds,
                        viewportSize = viewportSizeState.value,
                        panState = panState,
                        zoomState = zoomState,
                    )
                    resize(committedBounds)
                    boundsState.value = SpriteBounds(
                        left = 0,
                        top = 0,
                        right = committedBounds.width,
                        bottom = committedBounds.height,
                    )
                },
            )
            SpriteEditorTool.Position -> positionEditor?.invoke()
        }
        if (selectedTool != SpriteEditorTool.Position) {
            Content(
                selectedTool = selectedTool,
                penColor = penColor,
                sprite = sprite,
                draft = draft,
                boundsState = boundsState,
                panState = panState,
                zoomState = zoomState,
                viewportSizeState = viewportSizeState,
                commit = commit,
            )
        }
    }
}

@Composable
private fun Content(
    selectedTool: SpriteEditorTool,
    penColor: Palette,
    sprite: Sprite,
    draft: MutableSpriteDraft,
    boundsState: MutableState<SpriteBounds>,
    panState: MutableState<Offset>,
    zoomState: MutableState<Float>,
    viewportSizeState: MutableState<IntSize>,
    commit: (Sprite) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSizeState.value = it },
    ) {
        BackgroundCanvas(
            pan = panState.value,
            zoom = zoomState.value,
            sprite = sprite,
            cropToSprite = selectedTool == SpriteEditorTool.Pen,
        )
        when (selectedTool) {
            SpriteEditorTool.Pen -> SpriteCanvas(
                penColor = penColor,
                draft = draft,
                commit = commit,
                panState = panState,
                zoomState = zoomState,
            )
            SpriteEditorTool.Resize -> SpriteResizeCanvas(
                sprite,
                boundsState,
                panState,
                zoomState,
            )
            SpriteEditorTool.Position -> Unit
        }
    }
}

private fun rebaseViewportForResize(
    sprite: Sprite,
    bounds: SpriteBounds,
    viewportSize: IntSize,
    panState: MutableState<Offset>,
    zoomState: MutableState<Float>,
) {
    if (viewportSize == IntSize.Zero) return

    val viewport = Size(viewportSize.width.toFloat(), viewportSize.height.toFloat())
    val oldFittedSize = fittedSpriteSize(viewport, sprite)
    val oldOrigin = Offset(
        (viewport.width - oldFittedSize.width) / 2f,
        (viewport.height - oldFittedSize.height) / 2f,
    ) + panState.value
    val cellSize = oldFittedSize.width / sprite.width * zoomState.value
    val committedOrigin = oldOrigin + Offset(
        bounds.left * cellSize,
        bounds.top * cellSize,
    )

    val newFittedSize = fittedSize(
        containerSize = viewport,
        width = bounds.width,
        height = bounds.height,
    )
    val newBaseCellSize = newFittedSize.width / bounds.width
    zoomState.value = cellSize / newBaseCellSize
    panState.value = committedOrigin - Offset(
        (viewport.width - newFittedSize.width) / 2f,
        (viewport.height - newFittedSize.height) / 2f,
    )
}

internal fun fittedSpriteSize(containerSize: Size, sprite: Sprite): Size {
    return fittedSize(containerSize, sprite.width, sprite.height)
}

private fun fittedSize(containerSize: Size, width: Int, height: Int): Size {
    val pixelSize = min(
        containerSize.width / width,
        containerSize.height / height,
    )
    return Size(width * pixelSize, height * pixelSize)
}

@Composable
private fun BackgroundCanvas(
    pan: Offset,
    zoom: Float,
    sprite: Sprite,
    cropToSprite: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tileScroll")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = BackgroundTileSize,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tileScrollOffset"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val backgroundBrush = tiled(
            scaled(transparent, BackgroundTileSize.toInt(), BackgroundTileSize.toInt())
        )
        if (!cropToSprite) {
            translate(-offset, offset) {
                drawRect(
                    brush = backgroundBrush,
                    topLeft = Offset(-BackgroundTileSize, -BackgroundTileSize),
                    size = Size(
                        size.width + BackgroundTileSize * 2f,
                        size.height + BackgroundTileSize * 2f,
                    ),
                )
            }
            return@Canvas
        }

        val spriteSize = fittedSpriteSize(size, sprite)
        val canvasOrigin = Offset(
            (size.width - spriteSize.width) / 2f,
            (size.height - spriteSize.height) / 2f
        ) + pan
        val left = floor(canvasOrigin.x)
        val top = floor(canvasOrigin.y)
        val right = floor(canvasOrigin.x + spriteSize.width * zoom)
        val bottom = floor(canvasOrigin.y + spriteSize.height * zoom)

        clipRect(left = left, top = top, right = right, bottom = bottom) {
            // Draw in the grid's unscaled coordinate space, then use the grid's
            // exact screen transform. The animation is the only relative motion.
            translate(canvasOrigin.x, canvasOrigin.y) {
                scale(zoom, pivot = Offset.Zero) {
                    translate(-offset, offset) {
                        drawRect(
                            brush = backgroundBrush,
                            // The rect moves by at most one tile, so one tile of
                            // local-space overscan keeps it covered by the clip.
                            topLeft = Offset(
                                -BackgroundTileSize,
                                -BackgroundTileSize
                            ),
                            size = Size(
                                spriteSize.width + BackgroundTileSize * 2f,
                                spriteSize.height + BackgroundTileSize * 2f
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpriteCanvas(
    penColor: Palette,
    draft: MutableSpriteDraft,
    commit: (Sprite) -> Unit,
    panState: MutableState<Offset>,
    zoomState: MutableState<Float>
) {
    var pan by panState
    var zoom by zoomState
    val commit = rememberUpdatedState(commit)
    val imagePaint = remember {
        Paint().apply { filterQuality = FilterQuality.None }
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(penColor) {
                fun cellAt(position: Offset): Cell {
                    val frame = draft.frame
                    val spriteSize = fittedSize(
                        Size(size.width.toFloat(), size.height.toFloat()),
                        frame.width,
                        frame.height,
                    )
                    val offsetInitial = Offset(
                        (size.width - spriteSize.width) / 2f,
                        (size.height - spriteSize.height) / 2f
                    )
                    val cellSize = spriteSize.width / frame.width * zoomState.value
                    return Cell(
                        row = floor((position.y - panState.value.y - offsetInitial.y) / cellSize).toInt(),
                        col = floor((position.x - panState.value.x - offsetInitial.x) / cellSize).toInt()
                    )
                }

                racing {
                    sequence {
                        val cell = cellAt(expectTap().position)
                        if (draft.inBounds(cell.row, cell.col)) {
                            draft.apply(Sprite.Edits.Write(cell.row, cell.col, penColor))
                            commit.value(draft.toSprite())
                        }
                    }
                    sequence {
                        val visited = mutableSetOf<Cell>()
                        var previousCell: Cell? = null
                        followDrag { change ->
                            val currentCell = cellAt(change.position)
                            previousCell?.let { previous ->
                                val edits = cellsBetween(previous, currentCell).mapNotNull { cell ->
                                    if (draft.inBounds(cell.row, cell.col) && visited.add(cell)) {
                                        Sprite.Edits.Write(cell.row, cell.col, penColor)
                                    } else {
                                        null
                                    }
                                }
                                if (edits.isNotEmpty()) {
                                    draft.apply(Sprite.Edits.WriteMany(edits))
                                }
                            }
                            previousCell = currentCell
                        }
                        commit.value(draft.toSprite())
                    }
                    sequence {
                        var lastOffset: Offset? = null
                        var lastSize: Float? = null
                        val frame = draft.frame
                        val spriteSize = fittedSize(
                            Size(size.width.toFloat(), size.height.toFloat()),
                            frame.width,
                            frame.height,
                        )
                        val spriteOrigin = Offset(
                            (size.width - spriteSize.width) / 2f,
                            (size.height - spriteSize.height) / 2f
                        )
                        followPinch { offset, size ->
                            val lastOff = lastOffset ?: offset
                            val lastSiz = lastSize ?: size
                            val zoomFactor = size / lastSiz
                            // Anchor zoom at focal point: keep content under fingers stationary
                            panState.value += (offset - lastOff) +
                                (panState.value + spriteOrigin - lastOff) * (zoomFactor - 1)
                            zoomState.value *= zoomFactor
                            lastOffset = offset
                            lastSize = size
                        }
                    }
                }
            }
    ) {
        // Use exact (non-floored) cell size for math, then snap edges to pixels.
        // This avoids banding/gaps from flooring the cell size and from
        // fractional pan offsets. Each cell's edges are computed as
        // floor(col * exactCellSize + pan) so adjacent cells share an edge.
        val frame = draft.frame
        val spriteSize = fittedSize(size, frame.width, frame.height)
        val offsetInitial = Offset(
            (size.width - spriteSize.width) / 2f,
            (size.height - spriteSize.height) / 2f
        )
        val exactCellSize = spriteSize.width / frame.width * zoom

        val left = floor(offsetInitial.x + pan.x)
        val top = floor(offsetInitial.y + pan.y)
        val right = floor(offsetInitial.x + pan.x + frame.width * exactCellSize)
        val bottom = floor(offsetInitial.y + pan.y + frame.height * exactCellSize)
        drawIntoCanvas { canvas ->
            canvas.drawImageRect(
                image = frame.bitmap,
                srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                srcSize = androidx.compose.ui.unit.IntSize(frame.width, frame.height),
                dstOffset = androidx.compose.ui.unit.IntOffset(
                    left.toInt(),
                    top.toInt(),
                ),
                dstSize = androidx.compose.ui.unit.IntSize(
                    (right - left).toInt().coerceAtLeast(1),
                    (bottom - top).toInt().coerceAtLeast(1),
                ),
                paint = imagePaint,
            )
        }
    }
}

@Preview(widthDp = 400, heightDp = 400)
@Composable
private fun TestCanvas() {
    val sprite by remember { mutableStateOf(Sprite.empty(20, 20)) }
    EditableCanvas(sprite, {}, {})
}
