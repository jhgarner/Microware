package com.example.microware.editor.sprite

enum class BrushShape {
    Round,
    Square,
}

data class PixelBrush(
    val size: Int = 1,
    val shape: BrushShape = BrushShape.Round,
) {
    init {
        require(size > 0)
    }

    private val offsets = run {
        val firstOffset = -(size / 2)
        val lastOffset = firstOffset + size - 1
        val footprintCenter = (firstOffset + lastOffset) / 2.0
        val radiusSquared = (size / 2.0) * (size / 2.0)

        (firstOffset..lastOffset).flatMap { rowOffset ->
            (firstOffset..lastOffset).mapNotNull { colOffset ->
                val isInsideBrush = when (shape) {
                    BrushShape.Round -> {
                        val rowDistance = rowOffset - footprintCenter
                        val colDistance = colOffset - footprintCenter
                        rowDistance * rowDistance + colDistance * colDistance <= radiusSquared
                    }

                    BrushShape.Square -> true
                }

                if (isInsideBrush) {
                    Cell(rowOffset, colOffset)
                } else {
                    null
                }
            }
        }
    }

    fun cellsAt(center: Cell): List<Cell> = offsets.map { offset ->
        Cell(center.row + offset.row, center.col + offset.col)
    }
}
