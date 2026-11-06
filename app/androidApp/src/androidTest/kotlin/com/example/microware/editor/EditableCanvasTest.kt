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
}
