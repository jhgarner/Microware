package com.example.microware.game.runtime

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.TagFilter
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
        player.tagged(tag).velocity = Offset(1f, 0f)

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
                Logic(Trigger.Press(TagFilter(first)), listOf(Action.Move(TagFilter(firstMover), Direction.E, speed = 2))),
                Logic(Trigger.Press(TagFilter(second)), listOf(Action.Move(TagFilter(secondMover), Direction.W, speed = 3))),
                Logic(Trigger.Release(TagFilter(first)), listOf(Action.Move(TagFilter(firstMover), Direction.N, speed = 4))),
                Logic(Trigger.Release(TagFilter(second)), listOf(Action.Move(TagFilter(secondMover), Direction.S, speed = 5))),
            ),
            positions = mapOf(
                first to Offset.Zero,
                second to Offset.Zero,
                firstMover to Offset(10f, 0f),
                secondMover to Offset(20f, 0f),
            ),
        )

        player.press(Offset(0.5f, 0.5f))
        assertEquals(Offset(2f, 0f), player.tagged(firstMover).velocity)
        assertEquals(Offset(-3f, 0f), player.tagged(secondMover).velocity)

        player.release()
        assertEquals(Offset(0f, -4f), player.tagged(firstMover).velocity)
        assertEquals(Offset(0f, 5f), player.tagged(secondMover).velocity)
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
        Logic(Trigger.Press(TagFilter(tag)), listOf(Action.Win)),
        Logic(Trigger.Release(TagFilter(tag)), listOf(Action.Lose)),
    )

    private fun playerWithObjects(
        logics: List<Logic>,
        positions: Map<LogicTag, Offset>,
    ): Player {
        val bitmap = ImageBitmap(1, 1)
        val objects = positions.map { (tag, offset) -> ObjectState(bitmap, offset, setOf(tag)) }
        return Player(logics = logics, objects = objects)
    }

    private fun Player.tagged(tag: LogicTag): ObjectState = objects.single { tag in it.tags }
}
