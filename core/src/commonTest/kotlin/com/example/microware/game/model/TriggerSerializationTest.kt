package com.example.microware.game.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TriggerSerializationTest {
    @Test
    fun decodesLegacyTapSerialNameAsRelease() {
        val encoded = """{"type":"com.example.microware.game.model.Trigger.Tap","target":{"tag":7}}"""

        assertEquals(
            Trigger.Release(TagFilter(LogicTag(7))),
            Json.decodeFromString<Trigger>(encoded),
        )
    }

    @Test
    fun decodesLegacySingleTagActionAsAOneTagFilter() {
        val encoded =
            """{"type":"com.example.microware.game.model.Action.MoveTo","mover":{"tag":1},"target":{"tag":2},"speed":null}"""

        val action = Json.decodeFromString<Action>(encoded) as Action.MoveTo

        assertEquals(TagFilter(LogicTag(1)), action.mover)
        assertEquals(TagFilter(LogicTag(2)), action.target)
    }

    @Test
    fun multiTagTriggerRoundTrips() {
        val trigger: Trigger = Trigger.Collision(
            TagFilter(LogicTag(7), LogicTag(9)),
            TagFilter(LogicTag(3)),
        )

        assertEquals(trigger, Json.decodeFromString<Trigger>(Json.encodeToString(trigger)))
    }

    @Test
    fun collisionsSavedBeforeObjectCountsNeedOneObject() {
        val encoded =
            """{"type":"com.example.microware.game.model.Trigger.Collision","firstTarget":{"tag":1},"secondTarget":{"tag":2}}"""

        assertEquals(1, (Json.decodeFromString<Trigger>(encoded) as Trigger.Collision).firstTargetCount)
    }

    @Test
    fun separationAndItsObjectCountRoundTrip() {
        val trigger: Trigger = Trigger.Separation(
            TagFilter(LogicTag(1)),
            TagFilter(LogicTag(2)),
            firstTargetCount = 3,
        )

        assertEquals(trigger, Json.decodeFromString<Trigger>(Json.encodeToString(trigger)))
    }

    @Test
    fun actionsRoundTripParametersAlongsideTagFilters() {
        val action: Action = Action.MoveTo(Parameter.RandomFirstTarget, TagFilter(LogicTag(2)), speed = 4)

        val decoded = Json.decodeFromString<Action>(Json.encodeToString(action)) as Action.MoveTo

        assertEquals(Parameter.RandomFirstTarget, decoded.mover)
        assertEquals(TagFilter(LogicTag(2)), decoded.target)
    }

    @Test
    fun contactsOfferParametersForTheSidesThatCanBeToldApart() {
        val first = TagFilter(LogicTag(1))
        val second = TagFilter(LogicTag(2))

        assertEquals(emptyList(), Trigger.Start.parameters)
        assertEquals(listOf(Parameter.Tapped), Trigger.Release(first).parameters)
        assertEquals(
            listOf(Parameter.FirstTargets, Parameter.SecondTarget),
            Trigger.Collision(first, second).parameters,
        )
        assertEquals(listOf(Parameter.AllTargets), Trigger.Collision(first, first).parameters)
        assertEquals(
            listOf(Parameter.FirstTargets, Parameter.RandomFirstTarget, Parameter.SecondTarget),
            Trigger.Separation(first, second, firstTargetCount = 2).parameters,
        )
        assertEquals(
            listOf(Parameter.AllTargets, Parameter.RandomTarget),
            Trigger.Collision(first, first, firstTargetCount = 2).parameters,
        )
    }

    @Test
    fun togglingTheLastTagEmptiesAFilter() {
        val filter = TagFilter(LogicTag(1))

        assertEquals(TagFilter.Empty, filter.toggling(LogicTag(1)))
        assertEquals(TagFilter(LogicTag(1), LogicTag(2)), filter.toggling(LogicTag(2)))
        assertEquals(filter, filter.toggling(LogicTag(2)).toggling(LogicTag(2)))
    }

    @Test
    fun emptyFiltersRoundTrip() {
        val action: Action = Action.MoveTo(TagFilter.Empty, Parameter.SecondTarget, speed = null)

        val decoded = Json.decodeFromString<Action>(Json.encodeToString(action)) as Action.MoveTo

        assertEquals(TagFilter.Empty, decoded.mover)
    }

    @Test
    fun emptyFiltersLeaveTriggersAndActionsIncomplete() {
        val tagged = TagFilter(LogicTag(1))

        assertTrue(Trigger.Start.isComplete)
        assertTrue(Trigger.Collision(tagged, tagged).isComplete)
        assertFalse(Trigger.Collision(tagged, TagFilter.Empty).isComplete)
        assertFalse(Trigger.Press(TagFilter.Empty).isComplete)
        assertTrue(Action.MoveTo(Parameter.Tapped, tagged, speed = 1).isComplete)
        assertFalse(Action.MoveTo(Parameter.Tapped, TagFilter.Empty, speed = 1).isComplete)
        assertFalse(Action.Move(TagFilter.Empty, Direction.N, speed = 1).isComplete)
        assertTrue(Action.Win.isComplete)
    }
}
