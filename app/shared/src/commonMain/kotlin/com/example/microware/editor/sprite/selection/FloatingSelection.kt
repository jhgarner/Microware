package com.example.microware.editor.sprite.selection

import com.example.microware.Palette
import com.example.microware.editor.sprite.Cell
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteBounds
import kotlin.math.max
import kotlin.math.min

/**
 * Selected pixels floating as a layer above [reference], the sprite as it looks without them.
 *
 * Lifting pixels leaves a transparent hole in the reference, so moving them uncovers
 * nothing. Copying fills that hole with the pixels themselves, so moving them leaves a
 * copy behind. Either way, the sprite is always the [composite] of the two layers.
 */
internal data class FloatingSelection(
    val reference: Sprite,
    val pixels: PixelBlock,
    val position: Cell,
    val isCopied: Boolean = false,
) {
    val bounds: SpriteBounds
        get() = SpriteBounds(
            left = position.col,
            top = position.row,
            right = position.col + pixels.width,
            bottom = position.row + pixels.height,
        )

    fun composite(): Sprite = spriteOf(reference.width, reference.height) { row, col ->
        pixels.getOrNull(row - position.row, col - position.col)
            ?.takeUnless { it == Palette.Clear }
            ?: reference[row, col]
    }

    fun movedBy(rows: Int, columns: Int) =
        copy(position = Cell(position.row + rows, position.col + columns))

    fun rotatedClockwise() = rotatedAboutCenter(pixels.rotatedClockwise())

    fun rotatedCounterClockwise() = rotatedAboutCenter(pixels.rotatedCounterClockwise())

    fun flippedHorizontally() = copy(pixels = pixels.flippedHorizontally())

    fun flippedVertically() = copy(pixels = pixels.flippedVertically())

    fun copied() = copy(reference = composite(), isCopied = true)

    private fun rotatedAboutCenter(rotated: PixelBlock) = copy(
        pixels = rotated,
        position = Cell(
            row = position.row + (pixels.height - rotated.height) / 2,
            col = position.col + (pixels.width - rotated.width) / 2,
        ),
    )

    companion object {
        /** Lifts the part of [bounds] that lies on [sprite], or nothing if they don't overlap. */
        fun lift(sprite: Sprite, bounds: SpriteBounds): FloatingSelection? {
            val lifted = bounds.intersect(SpriteBounds.original(sprite)) ?: return null
            return FloatingSelection(
                reference = spriteOf(sprite.width, sprite.height) { row, col ->
                    if (Cell(row, col) in lifted) Palette.Clear else sprite[row, col]
                },
                pixels = PixelBlock.of(lifted.width, lifted.height) { row, col ->
                    sprite[lifted.top + row, lifted.left + col]
                },
                position = Cell(lifted.top, lifted.left),
            )
        }
    }
}

internal operator fun SpriteBounds.contains(cell: Cell) =
    cell.row in top..<bottom && cell.col in left..<right

/** The smallest bounds covering both cells. */
internal fun boundsSpanning(start: Cell, end: Cell) = SpriteBounds(
    left = min(start.col, end.col),
    top = min(start.row, end.row),
    right = max(start.col, end.col) + 1,
    bottom = max(start.row, end.row) + 1,
)

private fun SpriteBounds.intersect(other: SpriteBounds): SpriteBounds? {
    val left = max(left, other.left)
    val top = max(top, other.top)
    val right = min(right, other.right)
    val bottom = min(bottom, other.bottom)
    return if (left < right && top < bottom) SpriteBounds(left, top, right, bottom) else null
}

private fun spriteOf(width: Int, height: Int, pixel: (row: Int, col: Int) -> Palette) =
    Sprite.pattern(width, height) { pixel(it / width, it % width) }
