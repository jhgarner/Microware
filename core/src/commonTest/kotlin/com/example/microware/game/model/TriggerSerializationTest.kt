package com.example.microware.game.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun togglingNeverEmptiesAFilter() {
        val filter = TagFilter(LogicTag(1))

        assertEquals(filter, filter.toggling(LogicTag(1)))
        assertEquals(TagFilter(LogicTag(1), LogicTag(2)), filter.toggling(LogicTag(2)))
        assertEquals(filter, filter.toggling(LogicTag(2)).toggling(LogicTag(2)))
    }
}
