package com.example.microware.game.sprite

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import com.example.microware.Palette
import com.example.microware.core.Model

private class PackedPixels private constructor(
    private val bytes: ByteArray,
) {
    fun get(index: Int): Int {
        val bitOffset = index * BITS_PER_PIXEL
        val byteIndex = bitOffset / 8
        val bitShift = bitOffset % 8
        val packed = (bytes[byteIndex].toInt() and 0xFF) or
                ((bytes.getOrNull(byteIndex + 1)?.toInt()?.and(0xFF) ?: 0) shl 8)
        return (packed ushr bitShift) and PALETTE_MASK
    }

    fun copyBytes(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean {
        return this === other || (other is PackedPixels && bytes.contentEquals(other.bytes))
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    companion object {
        fun fromPaletteCodes(codes: ByteArray): PackedPixels {
            val bytes = ByteArray((codes.size * BITS_PER_PIXEL + 7) / 8)
            for (index in codes.indices) {
                val code = codes[index].toInt() and 0xFF
                val bitOffset = index * BITS_PER_PIXEL
                val byteIndex = bitOffset / 8
                val bitShift = bitOffset % 8
                val packed = code shl bitShift
                bytes[byteIndex] = (bytes[byteIndex].toInt() or packed).toByte()
                if (bitShift > 3) {
                    bytes[byteIndex + 1] = (
                            bytes[byteIndex + 1].toInt() or (packed ushr 8)
                            ).toByte()
                }
            }
            return PackedPixels(bytes)
        }

        fun fromBytes(bytes: ByteArray, pixelCount: Int): PackedPixels {
            require(pixelCount > 0)
            require(bytes.size == (pixelCount * BITS_PER_PIXEL + 7) / 8)
            val pixels = PackedPixels(bytes.copyOf())
            return pixels
        }

        private const val BITS_PER_PIXEL = 5
        private const val PALETTE_MASK = (1 shl BITS_PER_PIXEL) - 1
    }
}

class Sprite private constructor(
    val width: Int,
    val height: Int,
    private val pixels: PackedPixels,
) {
    init {
        require(width in 1..MaxSpriteWidth)
        require(height in 1..MaxSpriteHeight)
    }

    fun toModel(): Model.Sprite = Model.Sprite(width, height, packedBytes())

    fun isFullyTransparent(): Boolean {
        for (index in 0 until width * height) {
            if (pixels.get(index) != Palette.Clear.code) return false
        }
        return true
    }

    val bitmap: ImageBitmap by lazy {
        val paint = Paint()
        render(width, height) {
            for (index in 0 until width * height) {
                paint.color = Palette.fromCode(pixels.get(index)).color
                val col = index % width
                val row = index / width
                drawRect(
                    left = col.toFloat(),
                    top = row.toFloat(),
                    right = (col + 1).toFloat(),
                    bottom = (row + 1).toFloat(),
                    paint = paint,
                )
            }
        }
    }

    internal fun packedBytes(): ByteArray = pixels.copyBytes()

    internal fun paletteCodes(): ByteArray = ByteArray(width * height) { index ->
        pixels.get(index).toByte()
    }

    sealed interface Edits {
        data class Write(val row: Int, val col: Int, val color: Palette) : Edits
        data class WriteMany(val edits: List<Write>) : Edits
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Sprite &&
                width == other.width &&
                height == other.height &&
                pixels == other.pixels)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.hashCode()
        return result
    }

    companion object {
        internal fun fromPaletteCodes(width: Int, height: Int, codes: ByteArray): Sprite {
            require(codes.size == width * height)
            return Sprite(width, height, PackedPixels.fromPaletteCodes(codes))
        }

        internal fun fromPackedBytes(width: Int, height: Int, bytes: ByteArray): Sprite {
            return Sprite(width, height, PackedPixels.fromBytes(bytes, width * height))
        }

        fun empty(w: Int, h: Int): Sprite {
            return solid(w, h, Palette.Clear)
        }

        fun solid(w: Int, h: Int, color: Palette): Sprite {
            return fromPaletteCodes(w, h, ByteArray(w * h) { color.code.toByte() })
        }

        fun pattern(w: Int, h: Int, draw: (Int) -> Palette): Sprite {
            return fromPaletteCodes(w, h, ByteArray(w * h) { draw(it).code.toByte() })
        }
    }
}

const val MaxSpriteWidth = 128
const val MaxSpriteHeight = 256

data class SpriteBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(right > left)
        require(bottom > top)
    }

    val width: Int get() = right - left
    val height: Int get() = bottom - top

    companion object {
        fun original(sprite: Sprite) = SpriteBounds(
            left = 0,
            top = 0,
            right = sprite.width,
            bottom = sprite.height,
        )
    }
}
