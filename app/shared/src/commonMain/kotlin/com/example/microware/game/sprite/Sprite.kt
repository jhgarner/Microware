package com.example.microware.game.sprite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.dp
import com.example.microware.editor.sprite.MutableSpriteDraft

private class PackedPixels private constructor(
    private val bytes: ByteArray,
) {
    fun get(index: Int): Int {
        val bitOffset = index * BitsPerPixel
        val byteIndex = bitOffset / 8
        val bitShift = bitOffset % 8
        val packed = (bytes[byteIndex].toInt() and 0xFF) or
                ((bytes.getOrNull(byteIndex + 1)?.toInt()?.and(0xFF) ?: 0) shl 8)
        return (packed ushr bitShift) and PaletteMask
    }

    fun copyBytes(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean {
        return this === other || (other is PackedPixels && bytes.contentEquals(other.bytes))
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    companion object {
        fun fromPaletteCodes(codes: ByteArray): PackedPixels {
            val bytes = ByteArray((codes.size * BitsPerPixel + 7) / 8)
            for (index in codes.indices) {
                val code = codes[index].toInt() and 0xFF
                Palette.fromCode(code)
                val bitOffset = index * BitsPerPixel
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
            require(bytes.size == (pixelCount * BitsPerPixel + 7) / 8)
            val pixels = PackedPixels(bytes.copyOf())
            for (index in 0 until pixelCount) {
                Palette.fromCode(pixels.get(index))
            }
            return pixels
        }

        private const val BitsPerPixel = 5
        private const val PaletteMask = (1 shl BitsPerPixel) - 1
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

    fun get(row: Int, col: Int): Palette = Palette.fromCode(pixels.get(row * width + col))

    fun inBounds(row: Int, col: Int) = row in 0..<height && col in 0..<width

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

    fun update(edit: Edits): Sprite {
        val draft = MutableSpriteDraft(this)
        draft.apply(edit)
        return draft.toSprite()
    }

    fun resized(bounds: SpriteBounds): Sprite {
        val draft = MutableSpriteDraft(this)
        draft.resize(bounds)
        return draft.toSprite()
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
        fun fromRows(rows: List<List<Palette>>): Sprite {
            require(rows.isNotEmpty())
            val width = rows.first().size
            require(width > 0)
            require(rows.all { it.size == width })
            val codes = ByteArray(width * rows.size)
            rows.forEachIndexed { row, values ->
                values.forEachIndexed { col, palette ->
                    codes[row * width + col] = palette.code.toByte()
                }
            }
            return fromPaletteCodes(width, rows.size, codes)
        }

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

// Copied from the Pico8 color palette, but can be replaced or extended with something else if needed.
enum class Palette(val code: Int, val color: Color) {
    Black(0, Color(0xFF000000)),
    Navy(1, Color(0xFF1D2B53)),
    Maroon(2, Color(0xFF7E2553)),
    Forest(3, Color(0xFF008751)),
    Rust(4, Color(0xFFAB5236)),
    DarkGray(5, Color(0xFF5F574F)),
    LightGray(6, Color(0xFFC2C3C7)),
    OffWhite(7, Color(0xFFFFF1E8)),
    Red(8, Color(0xFFFF004D)),
    Orange(9, Color(0xFFFFA300)),
    Yellow(10, Color(0xFFFFEC27)),
    Green(11, Color(0xFF00E436)),
    Cyan(12, Color(0xFF29ADFF)),
    Purple(13, Color(0xFF83769C)),
    Pink(14, Color(0xFFFF77A8)),
    Tan(15, Color(0xFFFFCCAA)),
    Clear(16, Color(0x00000000));

    companion object {
        fun fromCode(code: Int): Palette = entries.firstOrNull { it.code == code }
            ?: error("Unknown palette code: $code")
    }

}

@Composable
fun RenderSprite(offset: Offset, scale: Int, sprite: Sprite) {
    val image = remember(sprite, scale) {
        scaled(sprite.bitmap, sprite.width * scale, sprite.height * scale)
    }
    println("${image.width} by ${image.height}")
    Canvas(
        modifier = Modifier.requiredSize(sprite.width.dp * scale, sprite.height.dp * scale)
            .offset(offset.x.dp, offset.y.dp),
    ) {
        drawImage(image)
    }
}
