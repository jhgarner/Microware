package com.example.microware.editor.sprite

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import com.example.microware.Palette
import com.example.microware.game.sprite.MaxSpriteHeight
import com.example.microware.game.sprite.MaxSpriteWidth
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteBounds

internal class SpriteDraftFrame(
    val bitmap: ImageBitmap,
    val width: Int,
    val height: Int,
)

internal class MutableSpriteDraft(sprite: Sprite) {
    var width: Int = sprite.width
        private set
    var height: Int = sprite.height
        private set

    private var pixels = sprite.paletteCodes()
    private val paint = Paint().apply {
        blendMode = BlendMode.Src
    }

    private var bitmap = ImageBitmap(width, height)
    private var frameCanvas = Canvas(bitmap)
    private val frameState: MutableState<SpriteDraftFrame> = mutableStateOf(
        SpriteDraftFrame(bitmap, width, height),
    )

    val frame: SpriteDraftFrame
        get() = frameState.value

    init {
        rebuildBitmap()
    }

    fun get(row: Int, col: Int): Palette {
        require(inBounds(row, col))
        return Palette.fromCode(pixels[index(row, col)].toInt() and 0xFF)
    }

    fun inBounds(row: Int, col: Int): Boolean = row in 0..<height && col in 0..<width

    fun apply(edit: Sprite.Edits): Boolean {
        val changed = when (edit) {
            is Sprite.Edits.Write -> set(edit.row, edit.col, edit.color)
            is Sprite.Edits.WriteMany -> edit.edits.fold(false) { changed, write ->
                set(write.row, write.col, write.color) || changed
            }
        }
        if (changed) {
            publishFrame()
        }
        return changed
    }

    private fun set(row: Int, col: Int, color: Palette): Boolean {
        if (!inBounds(row, col)) return false
        val index = index(row, col)
        if ((pixels[index].toInt() and 0xFF) == color.code) return false

        pixels[index] = color.code.toByte()
        paint.color = color.color
        frameCanvas.drawRect(
            left = col.toFloat(),
            top = row.toFloat(),
            right = (col + 1).toFloat(),
            bottom = (row + 1).toFloat(),
            paint = paint,
        )
        return true
    }

    fun resize(bounds: SpriteBounds) {
        require(bounds.width <= MaxSpriteWidth)
        require(bounds.height <= MaxSpriteHeight)

        val resizedPixels = ByteArray(bounds.width * bounds.height) {
            val row = it / bounds.width
            val col = it % bounds.width
            val sourceRow = bounds.top + row
            val sourceCol = bounds.left + col
            if (inBounds(sourceRow, sourceCol)) {
                pixels[index(sourceRow, sourceCol)]
            } else {
                Palette.Clear.code.toByte()
            }
        }
        width = bounds.width
        height = bounds.height
        pixels = resizedPixels
        bitmap = ImageBitmap(width, height)
        frameCanvas = Canvas(bitmap)
        rebuildBitmap()
        publishFrame()
    }

    fun synchronize(sprite: Sprite) {
        if (toSprite() == sprite) return

        width = sprite.width
        height = sprite.height
        pixels = sprite.paletteCodes()
        bitmap = ImageBitmap(width, height)
        frameCanvas = Canvas(bitmap)
        rebuildBitmap()
        publishFrame()
    }

    fun toSprite(): Sprite = Sprite.fromPaletteCodes(width, height, pixels.copyOf())

    private fun index(row: Int, col: Int): Int = row * width + col

    private fun rebuildBitmap() {
        for (row in 0 until height) {
            for (col in 0 until width) {
                paint.color = Palette.fromCode(pixels[index(row, col)].toInt() and 0xFF).color
                frameCanvas.drawRect(
                    left = col.toFloat(),
                    top = row.toFloat(),
                    right = (col + 1).toFloat(),
                    bottom = (row + 1).toFloat(),
                    paint = paint,
                )
            }
        }
    }

    private fun publishFrame() {
        frameState.value = SpriteDraftFrame(bitmap, width, height)
    }
}
