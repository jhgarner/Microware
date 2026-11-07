package com.example.microware.minified

import com.example.microware.core.Model
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf
import org.junit.Test

/**
 * Ktor looks up serializers for request and response bodies by reflection, which R8 can break.
 * Run against the minified build with `-PminifiedTests`.
 */
class ServerPayloadSerializersTest {
    @Test
    fun everyServerPayloadHasASerializer() {
        serverPayloadTypes.forEach { Json.serializersModule.serializer(it) }
    }
}

private val serverPayloadTypes = listOf(
    typeOf<String>(),
    typeOf<Long>(),
    typeOf<List<Long>>(),
    typeOf<List<Model.Sprite>>(),
    typeOf<List<Model.ExperienceMetadata>>(),
    typeOf<List<Model.Experience>>(),
    typeOf<List<Model.PublishableExperience>>(),
    typeOf<List<Model.CommunityMetadata>>(),
    typeOf<Model.SaveExperienceMetadata>(),
    typeOf<Model.Publish>(),
    typeOf<Model.Unpublish>(),
    typeOf<Model.CommunityMetadata>(),
)
