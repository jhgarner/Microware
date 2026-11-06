package com.example.microware.editor.sprite.selection

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import com.example.microware.Palette
import com.example.microware.game.sprite.render

/**
 * An immutable rectangle of pixels. Unlike a sprite, it has no size limit, so a tall
 * selection can be rotated into a wide one.
 */
internal class PixelBlock private constructor(
    val width: Int,
    val height: Int,
    private val pixels: List<Palette>,
) {
    operator fun get(row: Int, col: Int): Palette = pixels[row * width + col]

    fun getOrNull(row: Int, col: Int): Palette? =
        if (row in 0..<height && col in 0..<width) this[row, col] else null

    fun rotatedClockwise() = of(width = height, height = width) { row, col ->
        this[height - 1 - col, row]
    }

    fun rotatedCounterClockwise() = of(width = height, height = width) { row, col ->
        this[col, width - 1 - row]
    }

    fun flippedHorizontally() = of(width, height) { row, col -> this[row, width - 1 - col] }

    fun flippedVertically() = of(width, height) { row, col -> this[height - 1 - row, col] }

    val bitmap: ImageBitmap by lazy {
        val paint = Paint()
        render(width, height) {
            pixels.forEachIndexed { index, color ->
                paint.color = color.color
                val col = (index % width).toFloat()
                val row = (index / width).toFloat()
                drawRect(col, row, col + 1, row + 1, paint)
            }
        }
    }

    companion object {
        fun of(width: Int, height: Int, pixel: (row: Int, col: Int) -> Palette) =
            PixelBlock(width, height, List(width * height) { pixel(it / width, it % width) })
    }
}
