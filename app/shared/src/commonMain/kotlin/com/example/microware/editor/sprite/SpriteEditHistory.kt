package com.example.microware.editor.sprite

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteBounds
import kotlin.collections.ArrayDeque

internal data class SpriteEditChange(
    val sprite: Sprite,
    val resize: SpriteResizeChange? = null,
)

internal data class SpriteResizeChange(
    val bounds: SpriteBounds,
    val offset: Offset,
)

internal class SpriteEditHistory(initial: Sprite) {
    private data class Edit(
        val before: SpriteEditChange,
        val after: SpriteEditChange,
    )

    private val undoStack = ArrayDeque<Edit>()
    private val redoStack = ArrayDeque<Edit>()
    private val currentState = mutableStateOf(initial)
    private val canUndoState = mutableStateOf(false)
    private val canRedoState = mutableStateOf(false)

    val current: Sprite get() = currentState.value
    val canUndo: Boolean get() = canUndoState.value
    val canRedo: Boolean get() = canRedoState.value

    fun record(
        sprite: Sprite,
        bounds: SpriteBounds? = null,
        offset: Offset = Offset.Zero,
    ): Boolean {
        val translated = bounds?.let { it.left != 0 || it.top != 0 } == true
        if (sprite == current && !translated) return false

        val beforeResize = bounds?.let {
            SpriteResizeChange(
                bounds = SpriteBounds(-it.left, -it.top, current.width - it.left, current.height - it.top),
                offset = offset,
            )
        }
        val afterResize = bounds?.let {
            SpriteResizeChange(it, offset + Offset(it.left.toFloat(), it.top.toFloat()))
        }
        undoStack.addLast(
            Edit(SpriteEditChange(current, beforeResize), SpriteEditChange(sprite, afterResize)),
        )
        redoStack.clear()
        currentState.value = sprite
        updateAvailability()
        return true
    }

    fun undo(): SpriteEditChange {
        val edit = undoStack.removeLast()
        redoStack.addLast(edit)
        currentState.value = edit.before.sprite
        updateAvailability()

        return edit.before
    }

    fun redo(): SpriteEditChange {
        val edit = redoStack.removeLast()
        undoStack.addLast(edit)
        currentState.value = edit.after.sprite
        updateAvailability()
        return edit.after
    }

    fun synchronize(sprite: Sprite) {
        if (sprite == current) return

        undoStack.clear()
        redoStack.clear()
        currentState.value = sprite
        updateAvailability()
    }

    private fun updateAvailability() {
        canUndoState.value = undoStack.isNotEmpty()
        canRedoState.value = redoStack.isNotEmpty()
    }
}
