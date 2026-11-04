package com.example.microware.game.runtime

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Trigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerTest {
    @Test
    fun startWinOnlyCompletesAsWonAtTheDeadline() {
        val player = Player(listOf(Logic(Trigger.Start, listOf(Action.Win))))

        player.step(Player.GameDurationMillis - 1)
        assertNull(player.result)

        player.step(1)

        assertEquals(GameResult.Won, player.result)
    }

    @Test
    fun unresolvedGameLosesAtTheDeadline() {
        val player = Player(emptyList())

        player.step(Player.GameDurationMillis)

        assertEquals(GameResult.Lost, player.result)
    }

    @Test
    fun laterLoseOverridesStartWinForASurvivalGame() {
        val tag = LogicTag(0)
        val player = playerWithObjects(
            logics = listOf(
                Logic(Trigger.Start, listOf(Action.Win)),
                Logic(Trigger.Release(tag), listOf(Action.Lose)),
            ),
            positions = mapOf(tag to Offset.Zero),
        )

        player.press(Offset(0.5f, 0.5f))
        player.release()
        player.step(Player.GameDurationMillis)

        assertEquals(GameResult.Lost, player.result)
    }

    @Test
    fun releaseAfterDeadlineCannotChangeTheFinalResult() {
        val tag = LogicTag(0)
        val player = playerWithObjects(
            logics = listOf(
                Logic(Trigger.Start, listOf(Action.Win)),
                Logic(Trigger.Release(tag), listOf(Action.Lose)),
            ),
            positions = mapOf(tag to Offset.Zero),
        )
        player.press(Offset(0.5f, 0.5f))
        player.step(Player.GameDurationMillis)

        player.release()

        assertEquals(GameResult.Won, player.result)
        assertEquals(WinState.Won, player.winState)
    }

    @Test
    fun pressAndReleaseRunOnlyTheirOwnActions() {
        val tag = LogicTag(0)
        val player = playerWithObjects(
            logics = listOf(
                Logic(Trigger.Press(tag), listOf(Action.Win)),
                Logic(Trigger.Release(tag), listOf(Action.Lose)),
            ),
            positions = mapOf(tag to Offset.Zero),
        )

        player.press(Offset(0.5f, 0.5f))
        assertEquals(WinState.Won, player.winState)

        player.release()
        assertEquals(WinState.Lost, player.winState)
    }

    @Test
    fun pressAfterDeadlineCannotChangeTheFinalResult() {
        val tag = LogicTag(0)
        val player = playerWithObjects(
            logics = listOf(Logic(Trigger.Press(tag), listOf(Action.Win))),
            positions = mapOf(tag to Offset.Zero),
        )
        player.step(Player.GameDurationMillis)

        player.press(Offset(0.5f, 0.5f))

        assertEquals(GameResult.Lost, player.result)
        assertEquals(WinState.Playing, player.winState)
    }

    @Test
    fun advanceRetainsFractionalSimulationMilliseconds() {
        val oneFrame = Player(emptyList())
        val splitFrames = Player(emptyList())

        oneFrame.advance(10, 1.15f)
        repeat(10) { splitFrames.advance(1, 1.15f) }

        assertEquals(11L, oneFrame.timeMs)
        assertEquals(oneFrame.timeMs, splitFrames.timeMs)
        oneFrame.advance(1, 1.15f)
        splitFrames.advance(1, 1.15f)
        assertEquals(oneFrame.timeMs, splitFrames.timeMs)
    }

    @Test
    fun movementAndTimerUseTheSameScaledTime() {
        val mover = LogicTag(0)
        val target = LogicTag(1)
        val player = playerWithObjects(
            logics = listOf(
                Logic(Trigger.Start, listOf(Action.MoveTo(mover, target, speed = 25))),
            ),
            positions = mapOf(
                mover to Offset.Zero,
                target to Offset(20f, 0f),
            ),
        )

        player.advance(delta = 500, speed = 2f)

        assertEquals(1_000L, player.timeMs)
        assertEquals(Offset(20f, 0f), player.objects.getValue(mover).offset)
        assertEquals(Offset.Zero, player.objects.getValue(mover).velocity)
        assertNull(player.objects.getValue(mover).destination)
    }

    @Test
    fun smallCollisionStepsCatchAnObjectCrossingBetweenLargeFrameEndpoints() {
        val mover = LogicTag(0)
        val obstacle = LogicTag(1)
        val player = playerWithObjects(
            logics = listOf(
                Logic(Trigger.Start, listOf(Action.Move(mover, Direction.E, speed = 100))),
                Logic(Trigger.Collision(mover, obstacle), listOf(Action.Win)),
            ),
            positions = mapOf(
                mover to Offset.Zero,
                obstacle to Offset(5f, 0f),
            ),
        )

        player.step(100)

        assertEquals(WinState.Won, player.winState)
    }

    @Test
    fun finalDeadlineCollisionCanSetTheResult() {
        val mover = LogicTag(0)
        val obstacle = LogicTag(1)
        val player = playerWithObjects(
            logics = listOf(
                Logic(Trigger.Start, listOf(Action.Lose)),
                Logic(Trigger.Start, listOf(Action.Move(mover, Direction.E, speed = 1))),
                Logic(Trigger.Collision(mover, obstacle), listOf(Action.Win)),
            ),
            positions = mapOf(
                mover to Offset.Zero,
                obstacle to Offset(3.999f, 0f),
            ),
        )

        player.step(Player.GameDurationMillis)

        assertEquals(GameResult.Won, player.result)
    }

    private fun playerWithObjects(
        logics: List<Logic>,
        positions: Map<LogicTag, Offset>,
    ): Player {
        val bitmap = ImageBitmap(1, 1)
        val objects = positions.mapValuesTo(mutableMapOf()) { (_, offset) ->
            ObjectState(bitmap, offset, Offset.Zero)
        }
        return Player(logics = logics, objects = objects)
    }
}
