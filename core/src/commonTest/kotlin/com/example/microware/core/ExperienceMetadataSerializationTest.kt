package com.example.microware.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExperienceMetadataSerializationTest {
    @Test
    fun legacyMetadataEnablesPlayingAndPublishing() {
        val metadata = Json.decodeFromString<Model.ExperienceMetadata>(
            """{"id":1,"banner":{"width":128,"height":32,"pixels":[]}}""",
        )

        assertTrue(metadata.playable)
        assertTrue(metadata.publishable)
    }

    @Test
    fun legacySaveEnablesPlayingAndPublishing() {
        val metadata = Json.decodeFromString<Model.SaveExperienceMetadata>(
            """{"id":null,"communityId":1,"banner":{"width":128,"height":32,"pixels":[]}}""",
        )

        assertTrue(metadata.playable)
        assertTrue(metadata.publishable)
    }

    @Test
    fun disabledFlagsSurviveSerialization() {
        val banner = Model.Sprite(128, 32, ByteArray(128 * 32))
        val metadata = Model.ExperienceMetadata(1, banner, playable = false, publishable = false)
        val save = Model.SaveExperienceMetadata(null, 1, banner, playable = false, publishable = false)

        val decodedMetadata = Json.decodeFromString<Model.ExperienceMetadata>(Json.encodeToString(metadata))
        val decodedSave = Json.decodeFromString<Model.SaveExperienceMetadata>(Json.encodeToString(save))

        assertFalse(decodedMetadata.playable)
        assertFalse(decodedMetadata.publishable)
        assertFalse(decodedSave.playable)
        assertFalse(decodedSave.publishable)
    }
}
