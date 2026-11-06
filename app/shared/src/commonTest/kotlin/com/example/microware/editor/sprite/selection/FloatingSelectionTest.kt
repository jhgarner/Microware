package com.example.microware.editor.sprite.selection

import com.example.microware.Palette
import com.example.microware.editor.sprite.Cell
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FloatingSelectionTest {
    private val sprite = sprite(
        "RRG.",
        "RRG.",
        "YYYY",
    )

    private fun lift(left: Int, top: Int, right: Int, bottom: Int) =
        FloatingSelection.lift(sprite, SpriteBounds(left, top, right, bottom))!!

    @Test
    fun liftingDoesNotChangeTheSprite() {
        assertEquals(sprite, lift(0, 0, 2, 2).composite())
    }

    @Test
    fun movingLeavesTransparencyBehind() {
        assertEquals(
            sprite(
                ".RR.",
                ".RR.",
                "YYYY",
            ),
            lift(0, 0, 2, 2).movedBy(rows = 0, columns = 1).composite(),
        )
    }

    @Test
    fun movingBackRestoresTheOriginal() {
        val selection = lift(0, 0, 2, 2)
        val moved = selection.movedBy(rows = 0, columns = 1)
        assertEquals(sprite, moved.movedBy(rows = 0, columns = -1).composite())
    }

    @Test
    fun pixelsMovedOffTheSpriteComeBack() {
        val selection = lift(0, 0, 2, 2)
        val offscreen = selection.movedBy(rows = -1, columns = -1)
        assertEquals(
            sprite(
                "R.G.",
                "..G.",
                "YYYY",
            ),
            offscreen.composite(),
        )
        assertEquals(sprite, offscreen.movedBy(rows = 1, columns = 1).composite())
    }

    @Test
    fun copyingLeavesTheOriginalPixelsBehind() {
        assertEquals(
            sprite(
                "RRRR",
                "RRRR",
                "YYYY",
            ),
            lift(0, 0, 2, 2).copied().movedBy(rows = 0, columns = 2).composite(),
        )
    }

    @Test
    fun transparentPixelsShowTheReferenceBeneath() {
        val selection = lift(2, 0, 4, 2).copied().movedBy(rows = 0, columns = -1)
        assertEquals(
            sprite(
                "RGG.",
                "RGG.",
                "YYYY",
            ),
            selection.composite(),
        )
    }

    @Test
    fun rotatingKeepsTheSelectionCentered() {
        val rotated = lift(0, 0, 4, 2).rotatedClockwise()
        assertEquals(SpriteBounds(left = 1, top = -1, right = 3, bottom = 3), rotated.bounds)
        assertEquals(
            sprite(
                ".RR.",
                ".GG.",
                "YYYY",
            ),
            rotated.composite(),
        )
    }

    @Test
    fun rotatingBackRestoresTheOriginal() {
        val selection = lift(0, 0, 3, 2)
        assertEquals(selection.bounds, selection.rotatedClockwise().rotatedCounterClockwise().bounds)
        assertEquals(sprite, selection.rotatedClockwise().rotatedCounterClockwise().composite())
        assertEquals(
            sprite,
            selection.rotatedClockwise().rotatedClockwise().rotatedClockwise().rotatedClockwise().composite(),
        )
    }

    @Test
    fun flippingMirrorsInPlace() {
        assertEquals(
            sprite(
                "GRR.",
                "GRR.",
                "YYYY",
            ),
            lift(0, 0, 3, 2).flippedHorizontally().composite(),
        )
        assertEquals(
            sprite(
                "YYY.",
                "RRG.",
                "RRGY",
            ),
            lift(0, 0, 3, 3).flippedVertically().composite(),
        )
    }

    @Test
    fun liftingIsClippedToTheSprite() {
        val selection = FloatingSelection.lift(sprite, SpriteBounds(-2, 1, 2, 5))!!
        assertEquals(SpriteBounds(0, 1, 2, 3), selection.bounds)
        assertEquals(Cell(1, 0), selection.position)
    }

    @Test
    fun liftingOutsideTheSpriteSelectsNothing() {
        assertNull(FloatingSelection.lift(sprite, SpriteBounds(4, 0, 6, 2)))
    }

    @Test
    fun boundsSpanCellsInEitherDirection() {
        assertEquals(SpriteBounds(1, 2, 4, 6), boundsSpanning(Cell(5, 3), Cell(2, 1)))
        assertEquals(SpriteBounds(3, 5, 4, 6), boundsSpanning(Cell(5, 3), Cell(5, 3)))
    }
}

private fun sprite(vararg rows: String): Sprite {
    val colors = mapOf(
        '.' to Palette.Clear,
        'R' to Palette.Red,
        'G' to Palette.Green,
        'Y' to Palette.Yellow,
    )
    val width = rows.first().length
    return Sprite.pattern(width, rows.size) { colors.getValue(rows[it / width][it % width]) }
}
