package com.example.microware.editor.sprite

import androidx.compose.ui.geometry.Offset
import com.example.microware.Palette
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpriteEditHistoryTest {
    @Test
    fun recordingAfterUndoStartsANewBranch() {
        val original = Sprite.solid(1, 1, Palette.Clear)
        val first = Sprite.solid(1, 1, Palette.Red)
        val second = Sprite.solid(1, 1, Palette.Green)
        val branch = Sprite.solid(1, 1, Palette.Cyan)
        val history = SpriteEditHistory(original)

        history.record(first)
        history.record(second)
        assertEquals(first, history.undo().sprite)
        assertTrue(history.canRedo)

        assertTrue(history.record(branch))

        assertEquals(branch, history.current)
        assertFalse(history.canRedo)
        assertEquals(first, history.undo().sprite)
        assertEquals(original, history.undo().sprite)
    }

    @Test
    fun noOpRecordPreservesRedoAndTranslatedBoundsCanRecordSameSprite() {
        val original = Sprite.solid(1, 1, Palette.Clear)
        val edited = Sprite.solid(1, 1, Palette.Red)
        val history = SpriteEditHistory(original)

        history.record(edited)
        history.undo()

        assertFalse(history.record(original))
        assertTrue(history.canRedo)
        assertEquals(edited, history.redo().sprite)

        val translatedBounds = SpriteBounds(-1, 0, 0, 1)
        assertTrue(history.record(edited, translatedBounds))
        assertTrue(history.canUndo)
    }

    @Test
    fun undoResizeRestoresCroppedPixelsAndReturnsInverseBounds() {
        val colors = listOf(
            Palette.Red, Palette.Green, Palette.Cyan,
            Palette.Orange, Palette.Yellow, Palette.Purple,
        )
        val original = Sprite.pattern(3, 2) { colors[it] }
        val cropped = Sprite.pattern(2, 2) { index -> colors[index + 1 + (index / 2)] }
        val bounds = SpriteBounds(1, 0, 3, 2)
        val history = SpriteEditHistory(original)

        history.record(cropped, bounds)

        val undo = history.undo()
        assertEquals(original, undo.sprite)
        assertEquals(SpriteBounds(-1, 0, 2, 2), undo.resize?.bounds)
        assertEquals(original, history.current)

        val redo = history.redo()
        assertEquals(cropped, redo.sprite)
        assertEquals(bounds, redo.resize?.bounds)
    }

    @Test
    fun resizeUndoRestoresOriginalOffsetAndRedoReturnsTranslatedOffset() {
        val original = Sprite.solid(1, 1, Palette.Red)
        val resized = Sprite.solid(3, 4, Palette.Red)
        val originalOffset = Offset(10f, 20f)
        val bounds = SpriteBounds(-1, 2, 2, 6)
        val history = SpriteEditHistory(original)

        history.record(resized, bounds, originalOffset)

        val undo = history.undo()
        assertEquals(original, undo.sprite)
        assertEquals(originalOffset, undo.resize?.offset)

        val redo = history.redo()
        assertEquals(resized, redo.sprite)
        assertEquals(Offset(9f, 22f), redo.resize?.offset)
    }

    @Test
    fun synchronizingExternalSpriteClearsHistory() {
        val original = Sprite.solid(1, 1, Palette.Clear)
        val edited = Sprite.solid(1, 1, Palette.Red)
        val external = Sprite.solid(2, 1, Palette.Green)
        val history = SpriteEditHistory(original)

        history.record(edited)
        history.undo()
        history.synchronize(original)
        assertTrue(history.canRedo)

        history.synchronize(external)

        assertEquals(external, history.current)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }
}
