package com.example.microware.game.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class TriggerSerializationTest {
    @Test
    fun decodesLegacyTapSerialNameAsRelease() {
        val encoded = """{"type":"com.example.microware.game.model.Trigger.Tap","target":{"tag":7}}"""

        assertEquals(Trigger.Release(LogicTag(7)), Json.decodeFromString<Trigger>(encoded))
    }

    @Test
    fun pressTriggerRoundTrips() {
        val trigger: Trigger = Trigger.Press(LogicTag(7))

        assertEquals(trigger, Json.decodeFromString<Trigger>(Json.encodeToString(trigger)))
    }
}
