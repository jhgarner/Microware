package com.example.microware

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
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

data class Sprite(val rows: PersistentList<PersistentList<Palette>>) {
    val width = rows[0].size
    val height = rows.size

    fun get(row: Int, col: Int) = rows[row][col]

    fun inBounds(row: Int, col: Int) = row in 0..<height && col in 0..<width

    val bitmap: ImageBitmap by lazy {
        val paint = Paint()
        render(width, height) {
            for ((rowIndex, row) in rows.withIndex()) {
                for ((colIndex, palette) in row.withIndex()) {
                    paint.color = palette.color
                    drawRect(
                        left = colIndex.toFloat(),
                        top = rowIndex.toFloat(),
                        right = (colIndex + 1).toFloat(),
                        bottom = (rowIndex + 1).toFloat(),
                        paint = paint
                    )
                }
            }
        }
    }

    fun update(edit: Edits): Sprite {
        return when (edit) {
            is Edits.Write -> Sprite(
                rows.replacingAt(
                    edit.row,
                    rows[edit.row].replacingAt(edit.col, edit.color)
                )
            )
        }
    }

    sealed interface Edits {
        data class Write(val row: Int, val col: Int, val color: Palette) : Edits
    }

    companion object {
        fun empty(w: Int, h: Int): Sprite {
            return solid(w, h, Palette.Clear)
        }

        fun solid(w: Int, h: Int, color: Palette): Sprite {
            return Sprite(List(h) { List(w) { color }.toPersistentList() }.toPersistentList())
        }
    }
}

// Copied from the Pico8 color palette, but can be replaced or extended with something else if needed.
enum class Palette(val color: Color) {
    Black(Color(0xFF000000)),
    Navy(Color(0xFF1D2B53)),
    Maroon(Color(0xFF7E2553)),
    Forest(Color(0xFF008751)),
    Rust(Color(0xFFAB5236)),
    DarkGray(Color(0xFF5F574F)),
    LightGray(Color(0xFFC2C3C7)),
    OffWhite(Color(0xFFFFF1E8)),
    Red(Color(0xFFFF004D)),
    Orange(Color(0xFFFFA300)),
    Yellow(Color(0xFFFFEC27)),
    Green(Color(0xFF00E436)),
    Cyan(Color(0xFF29ADFF)),
    Purple(Color(0xFF83769C)),
    Pink(Color(0xFFFF77A8)),
    Tan(Color(0xFFFFCCAA)),
    Clear(Color(0x00000000)),
}

@Composable
fun RenderSprite(offset: Offset, scale: Int, sprite: Sprite) {
    val image = remember { scaled(sprite.bitmap, sprite.width * scale, sprite.height * scale) }
    println("${image.width} by ${image.height}")
    Canvas(
        modifier = Modifier.requiredSize(sprite.width.dp * scale, sprite.height.dp * scale)
            .offset(offset.x.dp, offset.y.dp)
    ) {
        drawImage(image)
    }
}
