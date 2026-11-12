package com.example.microware.game.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlin.jvm.JvmInline

/** Selects the objects that carry every one of [tags]. */
@Serializable
@JvmInline
value class TagFilter(
    @Serializable(with = TagSetSerializer::class)
    val tags: Set<LogicTag>,
) {
    constructor(vararg tags: LogicTag) : this(tags.toSet())

    init {
        require(tags.isNotEmpty())
    }

    fun matches(objectTags: Set<LogicTag>): Boolean = objectTags.containsAll(tags)

    /** Removing the last tag is ignored, so the filter always selects something. */
    fun toggling(tag: LogicTag): TagFilter = when {
        tag !in tags -> TagFilter(tags + tag)
        tags.size > 1 -> TagFilter(tags - tag)
        else -> this
    }
}

/** Filters used to hold exactly one tag, encoded as a bare tag rather than a list. */
private object TagSetSerializer :
    JsonTransformingSerializer<Set<LogicTag>>(SetSerializer(LogicTag.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        element as? JsonArray ?: JsonArray(listOf(element))
}
