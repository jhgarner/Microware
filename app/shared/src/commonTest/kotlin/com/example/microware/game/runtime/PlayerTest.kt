package com.example.microware.game.runtime

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.TagFilter
import com.example.microware.game.model.Trigger
import kotlin.random.Random
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
                Logic(Trigger.Release(TagFilter(tag)), listOf(Action.Lose)),
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
                Logic(Trigger.Release(TagFilter(tag)), listOf(Action.Lose)),
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
                Logic(Trigger.Press(TagFilter(tag)), listOf(Action.Win)),
                Logic(Trigger.Release(TagFilter(tag)), listOf(Action.Lose)),
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
            logics = listOf(Logic(Trigger.Press(TagFilter(tag)), listOf(Action.Win))),
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
                Logic(Trigger.Start, listOf(Action.MoveTo(TagFilter(mover), TagFilter(target), speed = 25))),
            ),
            positions = mapOf(
                mover to Offset.Zero,
                target to Offset(20f, 0f),
            ),
        )

        player.advance(delta = 500, speed = 2f)

        assertEquals(1_000L, player.timeMs)
        assertEquals(Offset(20f, 0f), player.tagged(mover).offset)
        assertEquals(Offset.Zero, player.tagged(mover).velocity)
        assertNull(player.tagged(mover).destination)
    }

    @Test
    fun smallCollisionStepsCatchAnObjectCrossingBetweenLargeFrameEndpoints() {
        val mover = LogicTag(0)
        val obstacle = LogicTag(1)
        val player = playerWithObjects(
            logics = listOf(
                Logic(Trigger.Start, listOf(Action.Move(TagFilter(mover), Direction.E, speed = 100))),
                Logic(Trigger.Collision(TagFilter(mover), TagFilter(obstacle)), listOf(Action.Win)),
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
                Logic(Trigger.Start, listOf(Action.Move(TagFilter(mover), Direction.E, speed = 1))),
                Logic(Trigger.Collision(TagFilter(mover), TagFilter(obstacle)), listOf(Action.Win)),
            ),
            positions = mapOf(
                mover to Offset.Zero,
                obstacle to Offset(3.999f, 0f),
            ),
        )

        player.step(Player.GameDurationMillis)

        assertEquals(GameResult.Won, player.result)
    }

    @Test
    fun moveAppliesToEveryObjectCarryingAllOfTheFilterTags() {
        val red = LogicTag(0)
        val round = LogicTag(1)
        val redAndRound = taggedObject(red, round)
        val alsoRedAndRound = taggedObject(red, round)
        val onlyRed = taggedObject(red)
        val untagged = taggedObject()

        Player(
            logics = listOf(
                Logic(Trigger.Start, listOf(Action.Move(TagFilter(red, round), Direction.E, speed = 5))),
            ),
            objects = listOf(redAndRound, alsoRedAndRound, onlyRed, untagged),
        )

        assertEquals(Offset(5f, 0f), redAndRound.velocity)
        assertEquals(Offset(5f, 0f), alsoRedAndRound.velocity)
        assertEquals(Offset.Zero, onlyRed.velocity)
        assertEquals(Offset.Zero, untagged.velocity)
    }

    @Test
    fun eachMoverPicksItsTargetAtRandomAmongTheMatchingObjects() {
        val mover = LogicTag(0)
        val target = LogicTag(1)
        val movers = List(40) { taggedObject(mover) }
        val targets = listOf(
            taggedObject(target, at = Offset(10f, 0f)),
            taggedObject(target, at = Offset(0f, 10f)),
        )

        Player(
            logics = listOf(
                Logic(
                    Trigger.Start,
                    listOf(Action.MoveTo(TagFilter(mover), TagFilter(target), speed = null)),
                ),
            ),
            objects = movers + targets,
            random = Random(1),
        )

        assertEquals(targets.map { it.offset }.toSet(), movers.map { it.offset }.toSet())
    }

    @Test
    fun collisionFiresForAnyPairOfObjectsMatchingTheTwoFilters() {
        val ball = LogicTag(0)
        val firstBall = taggedObject(ball)
        val secondBall = taggedObject(ball, at = Offset(5f, 0f))
        val player = Player(
            logics = listOf(
                Logic(Trigger.Collision(TagFilter(ball), TagFilter(ball)), listOf(Action.Win)),
            ),
            objects = listOf(firstBall, secondBall),
        )

        player.step(4)
        assertEquals(WinState.Playing, player.winState)

        firstBall.offset = Offset(4.5f, 0f)
        player.step(4)
        assertEquals(WinState.Won, player.winState)
    }

    @Test
    fun collisionWaitsForTheRequiredNumberOfObjectsToTouchTheSameTarget() {
        val triangle = LogicTag(0)
        val circle = LogicTag(1)
        val firstTriangle = taggedObject(triangle, at = Offset(10f, 0f))
        val secondTriangle = taggedObject(triangle, at = Offset(20f, 0f))
        val player = Player(
            logics = listOf(
                Logic(
                    Trigger.Collision(TagFilter(triangle), TagFilter(circle), firstTargetCount = 2),
                    listOf(Action.Win),
                ),
            ),
            objects = listOf(firstTriangle, secondTriangle, taggedObject(circle)),
        )

        firstTriangle.offset = Offset(.5f, 0f)
        player.step(4)
        assertEquals(WinState.Playing, player.winState)

        secondTriangle.offset = Offset(-.5f, 0f)
        player.step(4)
        assertEquals(WinState.Won, player.winState)
    }

    @Test
    fun collisionFiresOnceWhenFurtherObjectsJoinAfterTheCountIsMet() {
        val triangle = LogicTag(0)
        val circle = LogicTag(1)
        val latecomer = taggedObject(triangle, at = Offset(10f, 0f))
        val player = Player(
            logics = listOf(
                Logic(Trigger.Collision(TagFilter(triangle), TagFilter(circle)), listOf(Action.Win)),
            ),
            objects = listOf(taggedObject(triangle), latecomer, taggedObject(circle)),
        )
        player.step(4)
        assertEquals(WinState.Won, player.winState)

        player.winState = WinState.Playing
        latecomer.offset = Offset(.5f, 0f)
        player.step(4)

        assertEquals(WinState.Playing, player.winState)
    }

    @Test
    fun separationFiresWhenATargetDropsBelowTheRequiredCount() {
        val triangle = LogicTag(0)
        val circle = LogicTag(1)
        val firstTriangle = taggedObject(triangle)
        val secondTriangle = taggedObject(triangle, at = Offset(.5f, 0f))
        val thirdTriangle = taggedObject(triangle, at = Offset(-.5f, 0f))
        val player = Player(
            logics = listOf(
                Logic(
                    Trigger.Separation(TagFilter(triangle), TagFilter(circle), firstTargetCount = 2),
                    listOf(Action.Lose),
                ),
            ),
            objects = listOf(firstTriangle, secondTriangle, thirdTriangle, taggedObject(circle)),
        )
        player.step(4)

        thirdTriangle.offset = Offset(10f, 0f)
        player.step(4)
        assertEquals(WinState.Playing, player.winState)

        secondTriangle.offset = Offset(20f, 0f)
        player.step(4)
        assertEquals(WinState.Lost, player.winState)

        player.winState = WinState.Playing
        firstTriangle.offset = Offset(30f, 0f)
        player.step(4)
        assertEquals(WinState.Playing, player.winState)
    }

    @Test
    fun separationNeedsTheCountToHaveBeenMetFirst() {
        val triangle = LogicTag(0)
        val circle = LogicTag(1)
        val onlyTriangle = taggedObject(triangle)
        val player = Player(
            logics = listOf(
                Logic(
                    Trigger.Separation(TagFilter(triangle), TagFilter(circle), firstTargetCount = 2),
                    listOf(Action.Lose),
                ),
            ),
            objects = listOf(onlyTriangle, taggedObject(circle)),
        )
        player.step(4)

        onlyTriangle.offset = Offset(10f, 0f)
        player.step(4)

        assertEquals(WinState.Playing, player.winState)
    }

    @Test
    fun collisionCountsObjectsPerTargetRatherThanAcrossTargets() {
        val triangle = LogicTag(0)
        val circle = LogicTag(1)
        val player = Player(
            logics = listOf(
                Logic(
                    Trigger.Collision(TagFilter(triangle), TagFilter(circle), firstTargetCount = 2),
                    listOf(Action.Win),
                ),
            ),
            objects = listOf(
                taggedObject(triangle, at = Offset(.5f, 0f)),
                taggedObject(circle),
                taggedObject(triangle, at = Offset(10.5f, 0f)),
                taggedObject(circle, at = Offset(10f, 0f)),
            ),
        )

        player.step(4)

        assertEquals(WinState.Playing, player.winState)
    }

    @Test
    fun collisionRunsItsActionsOncePerTargetTouchingEnoughObjects() {
        val triangle = LogicTag(0)
        val circle = LogicTag(1)
        val bystander = LogicTag(2)
        var moves = 0
        val countingRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(until: Int): Int = 0.also { moves++ }
        }
        val player = Player(
            logics = listOf(
                Logic(
                    Trigger.Collision(TagFilter(triangle), TagFilter(circle), firstTargetCount = 2),
                    listOf(Action.MoveTo(TagFilter(bystander), TagFilter(bystander), speed = null)),
                ),
            ),
            objects = listOf(
                taggedObject(circle),
                taggedObject(circle, at = Offset(.5f, 0f)),
                taggedObject(triangle, at = Offset(.2f, .5f)),
                taggedObject(triangle, at = Offset(.3f, .5f)),
                taggedObject(triangle, at = Offset(.4f, .5f)),
                taggedObject(bystander, at = Offset(50f, 50f)),
            ),
            random = countingRandom,
        )

        player.step(4)

        assertEquals(2, moves)
    }

    @Test
    fun pressOnlyTriggersForObjectsCarryingAllOfTheFilterTags() {
        val red = LogicTag(0)
        val round = LogicTag(1)
        val player = Player(
            logics = listOf(Logic(Trigger.Press(TagFilter(red, round)), listOf(Action.Win))),
            objects = listOf(
                taggedObject(red),
                taggedObject(red, round, at = Offset(5f, 0f)),
            ),
        )

        player.press(Offset(0.5f, 0.5f))
        assertEquals(WinState.Playing, player.winState)

        player.movePointer(Offset(5.5f, 0.5f))
        assertEquals(WinState.Won, player.winState)
    }

    private fun taggedObject(vararg tags: LogicTag, at: Offset = Offset.Zero) =
        ObjectState(ImageBitmap(1, 1), at, tags.toSet())

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
