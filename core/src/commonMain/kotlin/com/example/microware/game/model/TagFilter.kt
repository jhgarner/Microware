package com.example.microware.game.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlin.jvm.JvmInline

/** Selects the objects that carry every one of [tags]. An empty filter has no tags chosen yet, leaving whatever uses it incomplete. */
@Serializable
@JvmInline
value class TagFilter(
    @Serializable(with = TagSetSerializer::class)
    val tags: Set<LogicTag>,
) : Selector {
    constructor(vararg tags: LogicTag) : this(tags.toSet())

    val isEmpty: Boolean get() = tags.isEmpty()

    fun matches(objectTags: Set<LogicTag>): Boolean = objectTags.containsAll(tags)

    fun toggling(tag: LogicTag): TagFilter = TagFilter(if (tag in tags) tags - tag else tags + tag)

    companion object {
        val Empty = TagFilter(emptySet())
    }
}

/** Filters used to hold exactly one tag, encoded as a bare tag rather than a list. */
private object TagSetSerializer :
    JsonTransformingSerializer<Set<LogicTag>>(SetSerializer(LogicTag.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        element as? JsonArray ?: JsonArray(listOf(element))
}
