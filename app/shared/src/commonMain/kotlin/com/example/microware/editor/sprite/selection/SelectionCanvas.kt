package com.example.microware.editor.sprite.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.editor.sprite.Cell
import com.example.microware.editor.sprite.SpriteGeometry
import com.example.microware.editor.sprite.drawBoundsFrame
import com.example.microware.editor.sprite.drawPixels
import com.example.microware.editor.sprite.followPinchZoom
import com.example.microware.editor.sprite.handleAt
import com.example.microware.editor.sprite.moved
import com.example.microware.editor.sprite.spriteGeometry
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteBounds
import com.example.microware.ui.gestures.racing
import com.example.microware.ui.gestures.sequence
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
internal fun SelectionCanvas(
    sprite: Sprite,
    selectionState: MutableState<FloatingSelection?>,
    panState: MutableState<Offset>,
    zoomState: MutableState<Float>,
    commit: (Sprite) -> Unit,
) {
    var selection by selectionState
    var pendingBounds by remember { mutableStateOf<SpriteBounds?>(null) }
    val currentSprite by rememberUpdatedState(sprite)
    val currentCommit by rememberUpdatedState(commit)
    val frameColor by animateColorAsState(
        if (selection?.isCopied == true) Palette.Yellow.color else Color.White,
    )
    val imagePaint = remember {
        Paint().apply { filterQuality = FilterQuality.None }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(sprite.width, sprite.height) {
                fun viewport() = Size(size.width.toFloat(), size.height.toFloat())
                fun geometry() = spriteGeometry(viewport(), sprite, panState.value, zoomState.value)

                fun reselect(bounds: SpriteBounds) {
                    selection = FloatingSelection.lift(selection?.composite() ?: currentSprite, bounds)
                    pendingBounds = null
                }

                racing {
                    sequence {
                        val initial = expectDown()
                        val startBounds = selection?.bounds ?: unexpected(initial)
                        val startGeometry = geometry()
                        val handle = startGeometry.handleAt(initial.position, startBounds, 20.dp.toPx())
                            ?: unexpected(initial)
                        var bounds = startBounds
                        followDrag(initial) { change ->
                            change.consume()
                            val delta = startGeometry.cellDelta(initial.position, change.position)
                            bounds = startBounds.moved(handle, delta.col, delta.row)
                            pendingBounds = bounds
                        }
                        reselect(bounds)
                    }
                    sequence {
                        val initial = expectDown()
                        val startGeometry = geometry()
                        val start = selection
                            ?.takeIf { startGeometry.cellAt(initial.position) in it.bounds }
                            ?: unexpected(initial)
                        var moved = start
                        followDrag(initial) { change ->
                            change.consume()
                            val delta = startGeometry.cellDelta(initial.position, change.position)
                            moved = start.movedBy(delta.row, delta.col)
                            selection = moved
                        }
                        currentCommit(moved.composite())
                    }
                    sequence {
                        val initial = expectDown()
                        val startGeometry = geometry()
                        val anchor = startGeometry.cellAt(initial.position)
                        var bounds = boundsSpanning(anchor, anchor)
                        followDrag(initial) { change ->
                            change.consume()
                            bounds = boundsSpanning(anchor, startGeometry.cellAt(change.position))
                            pendingBounds = bounds
                        }
                        reselect(bounds)
                    }
                    sequence {
                        followPinchZoom(panState, zoomState, spriteGeometry(viewport(), sprite).origin)
                    }
                }
            },
    ) {
        val geometry = spriteGeometry(size, sprite, panState.value, zoomState.value)
        when (val floating = selection) {
            null -> drawPixels(sprite.bitmap, geometry, imagePaint)
            else -> {
                drawPixels(floating.reference.bitmap, geometry, imagePaint)
                val spriteTopLeft = geometry.position(0, 0)
                val spriteBottomRight = geometry.position(sprite.width, sprite.height)
                clipRect(
                    left = floor(spriteTopLeft.x),
                    top = floor(spriteTopLeft.y),
                    right = floor(spriteBottomRight.x),
                    bottom = floor(spriteBottomRight.y),
                ) {
                    drawPixels(floating.pixels.bitmap, geometry, imagePaint, floating.position)
                }
            }
        }
        (pendingBounds ?: selection?.bounds)?.let { drawBoundsFrame(it, geometry, frameColor) }
    }
}

/** How many whole rows and columns a finger travelled, rounded to the nearest cell. */
private fun SpriteGeometry.cellDelta(start: Offset, end: Offset): Cell {
    val delta = end - start
    return Cell(row = (delta.y / cellSize).roundToInt(), col = (delta.x / cellSize).roundToInt())
}
