package com.example.microware.editor.sprite

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PixelBrushTest {
    @Test
    fun onePixelRoundBrushCoversItsCenter() {
        assertEquals(
            listOf(Cell(4, 7)),
            PixelBrush().cellsAt(Cell(4, 7)),
        )
    }

    @Test
    fun squareBrushUsesExactOddAndEvenDiameters() {
        assertEquals(
            cellsIn(2..4, 3..5),
            PixelBrush(size = 3, shape = BrushShape.Square).cellsAt(Cell(3, 4)),
        )
        assertEquals(
            cellsIn(2..3, 3..4),
            PixelBrush(size = 2, shape = BrushShape.Square).cellsAt(Cell(3, 4)),
        )
    }

    @Test
    fun roundBrushFollowsPixelCenterDistance() {
        val cells = PixelBrush(size = 5).cellsAt(Cell(0, 0)).toSet()

        assertEquals(21, cells.size)
        assertTrue(Cell(-2, 0) in cells)
        assertFalse(Cell(-2, -2) in cells)
    }

    @Test
    fun evenBrushIsAnchoredTowardSmallerRowAndColumn() {
        assertEquals(
            setOf(Cell(9, 19), Cell(9, 20), Cell(10, 19), Cell(10, 20)),
            PixelBrush(size = 2, shape = BrushShape.Square).cellsAt(Cell(10, 20)).toSet(),
        )
    }

    @Test
    fun brushFootprintTranslatesWithItsCenter() {
        val brush = PixelBrush(size = 4, shape = BrushShape.Square)
        val origin = brush.cellsAt(Cell(0, 0)).toSet()
        val moved = brush.cellsAt(Cell(8, -3)).toSet()

        assertEquals(origin.map { Cell(it.row + 8, it.col - 3) }.toSet(), moved)
    }

    @Test
    fun sizeMustBePositive() {
        assertFailsWith<IllegalArgumentException> {
            PixelBrush(size = 0)
        }
    }

    private fun cellsIn(rows: IntRange, columns: IntRange): List<Cell> =
        rows.flatMap { row -> columns.map { col -> Cell(row, col) } }
}
