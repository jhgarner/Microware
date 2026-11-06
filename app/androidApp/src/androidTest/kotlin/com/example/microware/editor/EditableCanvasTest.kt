package com.example.microware.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.example.microware.Palette
import com.example.microware.editor.sprite.EditableCanvas
import com.example.microware.game.sprite.Sprite
import com.example.microware.ui.layout.Insets
import com.example.microware.ui.layout.LocalInsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditableCanvasTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun drawingCanBeUndoneAndRedoneWithControlsInTheirOwnRow() {
        val original = Sprite.solid(1, 1, Palette.Clear)
        val sprite = mutableStateOf(original)
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalInsets provides Insets.new()) {
                    EditableCanvas(
                        sprite = sprite.value,
                        commit = { sprite.value = it },
                        resize = { resized, _ -> sprite.value = resized },
                        positionEditor = {},
                    )
                }
            }
        }

        val brushBounds = composeRule.onNodeWithContentDescription("Brush shape")
            .fetchSemanticsNode().boundsInRoot
        val penBounds = composeRule.onNodeWithContentDescription("Pen tool")
            .fetchSemanticsNode().boundsInRoot
        val undoBounds = composeRule.onNodeWithContentDescription("Undo")
            .fetchSemanticsNode().boundsInRoot
        val redoBounds = composeRule.onNodeWithContentDescription("Redo")
            .fetchSemanticsNode().boundsInRoot
        val logicPickerBounds = composeRule.onNodeWithContentDescription("Choose logic tag")
            .fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithContentDescription("Undo").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Redo").assertIsNotEnabled()
        assertTrue(brushBounds.top < penBounds.top)
        assertTrue(undoBounds.top > penBounds.bottom)
        assertEquals(undoBounds.center.y, redoBounds.center.y)
        assertEquals(undoBounds.center.y, logicPickerBounds.center.y)

        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val tapPosition = Offset(
            x = rootBounds.width / 2f,
            y = (brushBounds.top - rootBounds.top) / 2f,
        )
        composeRule.onRoot().performTouchInput { click(tapPosition) }

        val edited = Sprite.solid(1, 1, Palette.Navy)
        composeRule.runOnIdle { assertEquals(edited, sprite.value) }
        composeRule.onNodeWithContentDescription("Undo").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Redo").assertIsNotEnabled()

        composeRule.onNodeWithContentDescription("Undo").performClick()
        composeRule.runOnIdle { assertEquals(original, sprite.value) }
        composeRule.onNodeWithContentDescription("Undo").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Redo").assertIsEnabled()

        composeRule.onNodeWithContentDescription("Redo").performClick()
        composeRule.runOnIdle { assertEquals(edited, sprite.value) }
        composeRule.onNodeWithContentDescription("Undo").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Redo").assertIsNotEnabled()
    }

    @Test
    fun selectionCanBeMovedBackCopiedAndDeselected() {
        val original = Sprite.pattern(4, 4) { if (it == 0) Palette.Red else Palette.Clear }
        val sprite = mutableStateOf(original)
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalInsets provides Insets.new()) {
                    EditableCanvas(
                        sprite = sprite.value,
                        commit = { sprite.value = it },
                        resize = { resized, _ -> sprite.value = resized },
                        positionEditor = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Select tool").performClick()
        composeRule.onNodeWithContentDescription("Deselect").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Copy selection").assertIsNotEnabled()

        val canvasBottom = composeRule.onNodeWithContentDescription("Deselect")
            .fetchSemanticsNode().boundsInRoot.top
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val cellSize = minOf(rootBounds.width, canvasBottom - rootBounds.top) / 4f
        val spriteOrigin = Offset(
            x = (rootBounds.width - cellSize * 4f) / 2f,
            y = (canvasBottom - rootBounds.top - cellSize * 4f) / 2f,
        )
        fun cell(row: Int, col: Int) = spriteOrigin + Offset((col + .5f) * cellSize, (row + .5f) * cellSize)
        fun drag(from: Offset, to: Offset) {
            composeRule.onRoot().performTouchInput { swipe(from, to, durationMillis = 300) }
            composeRule.waitForIdle()
        }

        drag(cell(0, 0), cell(1, 1))
        composeRule.onNodeWithContentDescription("Deselect").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Copy selection").assertIsEnabled()
        composeRule.runOnIdle { assertEquals(original, sprite.value) }

        drag(cell(0, 0), cell(0, 2))
        val moved = Sprite.pattern(4, 4) { if (it == 2) Palette.Red else Palette.Clear }
        composeRule.runOnIdle { assertEquals(moved, sprite.value) }

        drag(cell(0, 2), cell(0, 0))
        composeRule.runOnIdle { assertEquals(original, sprite.value) }

        composeRule.onNodeWithContentDescription("Copy selection").performClick()
        drag(cell(0, 0), cell(0, 2))
        val copied = Sprite.pattern(4, 4) { if (it == 0 || it == 2) Palette.Red else Palette.Clear }
        composeRule.runOnIdle { assertEquals(copied, sprite.value) }

        composeRule.onNodeWithContentDescription("Deselect").performClick()
        composeRule.onNodeWithContentDescription("Deselect").assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(copied, sprite.value) }
    }
}
