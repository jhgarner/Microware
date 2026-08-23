package com.example.microware

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

data class Sprite(val rows: PersistentList<PersistentList<Palette>>) {
    val width = rows[0].size
    val height = rows.size

    fun get(row: Int, col: Int) = rows[row][col]

    fun inBounds(row: Int, col: Int) = row in 0..<height && col in 0..<width

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
            return Sprite(List(h) { List(w) { Palette.Clear }.toPersistentList() }.toPersistentList())
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


