package com.example.microware.play

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import com.example.microware.game.model.Action
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.WorldHeight
import com.example.microware.game.model.WorldWidth
import com.example.microware.game.runtime.ObjectState
import com.example.microware.game.runtime.Player
import com.example.microware.game.runtime.WinState
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerPointerInputTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playGameTracksPointerEnteringLeavingAndLiftingOverAnObject() {
        composeRule.mainClock.autoAdvance = false
        val target = LogicTag(0)
        val player = Player(
            logics = listOf(
                Logic(Trigger.Press(target), listOf(Action.Win)),
                Logic(Trigger.Release(target), listOf(Action.Lose)),
            ),
            objects = mutableMapOf(
                target to ObjectState(
                    bitmap = ImageBitmap(20, 20),
                    offset = Offset(50f, 50f),
                    velocity = Offset.Zero,
                ),
            ),
        )
        composeRule.setContent {
            player.PlayGame(
                modifier = Modifier.testTag("pointer-game"),
                applyInsets = false,
            )
        }

        composeRule.mainClock.advanceTimeByFrame()
        val game = composeRule.onNodeWithTag("pointer-game")
        val canvasSize = game.fetchSemanticsNode().size
        val outside = canvasPosition(Offset(20f, 20f), canvasSize)
        val inside = canvasPosition(Offset(55f, 55f), canvasSize)

        game.performTouchInput { down(outside) }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(WinState.Playing, player.winState)

        game.performTouchInput { moveTo(inside) }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(WinState.Won, player.winState)

        game.performTouchInput { moveTo(outside) }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(WinState.Lost, player.winState)

        game.performTouchInput { moveTo(inside) }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(WinState.Won, player.winState)

        game.performTouchInput { up() }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(WinState.Lost, player.winState)
    }

    private fun canvasPosition(worldPosition: Offset, canvasSize: IntSize): Offset {
        val scale = min(
            canvasSize.width / WorldWidth.toFloat(),
            canvasSize.height / WorldHeight.toFloat(),
        )
        val origin = Offset(
            (canvasSize.width - WorldWidth * scale) / 2f,
            (canvasSize.height - WorldHeight * scale) / 2f,
        )
        return origin + worldPosition * scale
    }
}
