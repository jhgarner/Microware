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

class PlayerPointerTest {
    @Test
    fun draggingIntoAndOutOfAnObjectFiresEachContactTransitionOnce() {
        val tag = LogicTag(0)
        val player = playerWithObjects(
            logics = pointerWinAndLose(tag),
            positions = mapOf(tag to Offset.Zero),
        )

        player.press(Offset(2f, 2f))
        assertEquals(WinState.Playing, player.winState)

        player.movePointer(Offset(0.5f, 0.5f))
        assertEquals(WinState.Won, player.winState)

        player.winState = WinState.Playing
        player.movePointer(Offset(0.5f, 0.5f))
        assertEquals(WinState.Playing, player.winState)

        player.movePointer(null)
        assertEquals(WinState.Lost, player.winState)

        player.winState = WinState.Playing
        player.movePointer(Offset(0.5f, 0.5f))
        assertEquals(WinState.Won, player.winState)

        player.winState = WinState.Playing
        player.release()
        assertEquals(WinState.Lost, player.winState)

        player.winState = WinState.Playing
        player.release()
        assertEquals(WinState.Playing, player.winState)
    }

    @Test
    fun stationaryPointerTracksAnObjectMovingIntoAndOutOfContact() {
        val tag = LogicTag(0)
        val player = playerWithObjects(
            logics = pointerWinAndLose(tag),
            positions = mapOf(tag to Offset.Zero),
        )
        player.objects.getValue(tag).velocity = Offset(1f, 0f)

        player.press(Offset(2.5f, 0.5f))
        assertEquals(WinState.Playing, player.winState)

        player.step(2_000L)
        assertEquals(WinState.Won, player.winState)

        player.winState = WinState.Playing
        player.step(100L)
        assertEquals(WinState.Playing, player.winState)

        player.step(500L)
        assertEquals(WinState.Lost, player.winState)
    }

    @Test
    fun absentPointerDoesNotTriggerForObjectsUnderItsFormerPosition() {
        val tag = LogicTag(0)
        val player = playerWithObjects(
            logics = pointerWinAndLose(tag),
            positions = mapOf(tag to Offset.Zero),
        )

        player.press(Offset(0.5f, 0.5f))
        player.release()
        player.winState = WinState.Playing

        player.step(1_000L)

        assertEquals(WinState.Playing, player.winState)
    }

    @Test
    fun overlappingObjectsEachReceiveTheirOwnPressAndRelease() {
        val first = LogicTag(0)
        val second = LogicTag(1)
        val firstMover = LogicTag(2)
        val secondMover = LogicTag(3)
        val player = playerWithObjects(
            logics = listOf(
                Logic(Trigger.Press(first), listOf(Action.Move(firstMover, Direction.E, speed = 2))),
                Logic(Trigger.Press(second), listOf(Action.Move(secondMover, Direction.W, speed = 3))),
                Logic(Trigger.Release(first), listOf(Action.Move(firstMover, Direction.N, speed = 4))),
                Logic(Trigger.Release(second), listOf(Action.Move(secondMover, Direction.S, speed = 5))),
            ),
            positions = mapOf(
                first to Offset.Zero,
                second to Offset.Zero,
                firstMover to Offset(10f, 0f),
                secondMover to Offset(20f, 0f),
            ),
        )

        player.press(Offset(0.5f, 0.5f))
        assertEquals(Offset(2f, 0f), player.objects.getValue(firstMover).velocity)
        assertEquals(Offset(-3f, 0f), player.objects.getValue(secondMover).velocity)

        player.release()
        assertEquals(Offset(0f, -4f), player.objects.getValue(firstMover).velocity)
        assertEquals(Offset(0f, 5f), player.objects.getValue(secondMover).velocity)
    }

    @Test
    fun contactChangesAfterTheDeadlineCannotChangeTheResult() {
        val tag = LogicTag(0)
        val player = playerWithObjects(
            logics = pointerWinAndLose(tag),
            positions = mapOf(tag to Offset.Zero),
        )
        player.press(Offset(0.5f, 0.5f))
        player.step(Player.GameDurationMillis)

        player.movePointer(null)
        player.release()

        assertEquals(GameResult.Won, player.result)
        assertEquals(WinState.Won, player.winState)
    }

    private fun pointerWinAndLose(tag: LogicTag) = listOf(
        Logic(Trigger.Press(tag), listOf(Action.Win)),
        Logic(Trigger.Release(tag), listOf(Action.Lose)),
    )

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
