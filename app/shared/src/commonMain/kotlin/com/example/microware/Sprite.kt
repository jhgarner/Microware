package com.example.microware

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

data class Sprite(val rows: PersistentList<PersistentList<Color>>) {
    val width = rows[0].size
    val height = rows.size
    val ratio = width.toFloat() / height

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
        data class Write(val row: Int, val col: Int, val color: Color) : Edits
    }

    companion object {
        fun empty(w: Int, h: Int): Sprite {
            return Sprite(List(h) { List(w) { Color.Transparent }.toPersistentList() }.toPersistentList())
        }
    }
}

