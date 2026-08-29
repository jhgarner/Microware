package com.example.microware

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.min

sealed interface Tool {
    fun toEdit(row: Int, col: Int): Sprite.Edits

    data class Pen(val color: Palette) : Tool {
        override fun toEdit(row: Int, col: Int): Sprite.Edits = Sprite.Edits.Write(row, col, color)
    }
}

private const val TransparentTileSizeDp = 16

@Composable
private fun ColorPickerButton(color: Palette, isSelected: Boolean, onClick: () -> Unit) {
    val clearBrush = if (color == Palette.Clear) {
        val tileSize = with(LocalDensity.current) { TransparentTileSizeDp.dp.roundToPx() }
        remember(tileSize) { tiled(scaled(transparent, tileSize, tileSize)) }
    } else {
        null
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .size(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (color == Palette.Clear) Color.Transparent else color.color,
            contentColor = color.color
        ),
        shape = RectangleShape,
        border = if (isSelected) BorderStroke(2.dp, Color.Black) else null,
        contentPadding = PaddingValues(0.dp)
    ) {
        if (clearBrush != null) {
            Box(modifier = Modifier.fillMaxSize().background(clearBrush))
        }
    }
}

private const val BackgroundTileSize = 128f

@Composable
fun CanvasTools(tool: Tool, pickTool: (Tool) -> Unit) {
    val scrollState = rememberScrollState()
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
        for (color in Palette.entries) {
            ColorPickerButton(color, (tool as? Tool.Pen)?.color == color) {
                pickTool(Tool.Pen(color))
            }
        }
    }
}

@Composable
fun EditableCanvas(
    sprite: Sprite,
    edit: (Sprite.Edits) -> Unit,
    modifier: Modifier = Modifier
) {
    val toolState: MutableState<Tool> = remember { mutableStateOf(Tool.Pen(Palette.Navy)) }
    Column(modifier = modifier.fillMaxSize()) {
        CanvasTools(toolState.value) { toolState.value = it }
        Content(toolState.value, sprite, edit)
    }
}

@Composable
private fun Content(tool: Tool, sprite: Sprite, edit: (Sprite.Edits) -> Unit) {
    val pan = remember { mutableStateOf(Offset(0f, 0f)) }
    val zoom = remember { mutableStateOf(1f) }
    Box(modifier = Modifier.fillMaxSize()) {
        BackgroundCanvas(pan.value, zoom.value, sprite)
        SpriteCanvas(tool, sprite, edit, pan, zoom)
    }
}

private fun fittedSpriteSize(containerSize: Size, sprite: Sprite): Size {
    val pixelSize = min(
        containerSize.width / sprite.width,
        containerSize.height / sprite.height
    )
    return Size(sprite.width * pixelSize, sprite.height * pixelSize)
}

@Composable
private fun BackgroundCanvas(pan: Offset, zoom: Float, sprite: Sprite) {
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
                            brush = tiled(
                                scaled(
                                    transparent,
                                    BackgroundTileSize.toInt(),
                                    BackgroundTileSize.toInt()
                                )
                            ),
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
    tool: Tool,
    sprite: Sprite,
    edit: (Sprite.Edits) -> Unit,
    pan: MutableState<Offset>,
    zoom: MutableState<Float>
) {
    var pan by pan
    var zoom by zoom
    val edit = rememberUpdatedState(edit)
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(tool) {
                racing {
                    sequence {
                        val visited = mutableSetOf<Pair<Int, Int>>()
                        val spriteSize = fittedSpriteSize(
                            Size(size.width.toFloat(), size.height.toFloat()),
                            sprite
                        )
                        val offsetInitial = Offset(
                            (size.width - spriteSize.width) / 2f,
                            (size.height - spriteSize.height) / 2f
                        )
                        val cellSize = spriteSize.width / sprite.width * zoom
                        followDrag {
                            val offset = it.position - pan - offsetInitial
                            val col = (offset.x / cellSize).toInt()
                            val row = (offset.y / cellSize).toInt()

                            if (sprite.inBounds(row, col) && !visited.contains(row to col)) {
                                visited.add(row to col)
                                edit.value(tool.toEdit(row, col))
                            }
                        }
                    }
                    sequence {
                        var lastOffset: Offset? = null
                        var lastSize: Float? = null
                        val spriteSize = fittedSpriteSize(
                            Size(size.width.toFloat(), size.height.toFloat()),
                            sprite
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
                            pan += (offset - lastOff) +
                                (pan + spriteOrigin - lastOff) * (zoomFactor - 1)
                            zoom *= zoomFactor
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
        val spriteSize = fittedSpriteSize(size, sprite)
        val offsetInitial = Offset(
            (size.width - spriteSize.width) / 2f,
            (size.height - spriteSize.height) / 2f
        )
        val exactCellSize = spriteSize.width / sprite.width * zoom

        for (row in 0 until sprite.height) {
            val top = floor(row * exactCellSize + pan.y + offsetInitial.y)
            val bottom = floor((row + 1) * exactCellSize + pan.y + offsetInitial.y)
            val h = bottom - top
            for (col in 0 until sprite.width) {
                val left = floor(col * exactCellSize + pan.x + offsetInitial.x)
                val right = floor((col + 1) * exactCellSize + pan.x + offsetInitial.x)
                val w = right - left
                drawRect(
                    color = sprite.get(row, col).color,
                    topLeft = Offset(left, top),
                    size = Size(w, h)
                )
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 400)
@Composable
private fun TestCanvas() {
    val sprite by remember { mutableStateOf(Sprite.empty(20, 20)) }
    EditableCanvas(sprite, sprite::update)
}
