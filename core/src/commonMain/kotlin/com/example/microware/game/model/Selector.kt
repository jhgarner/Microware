package com.example.microware.game.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/** The objects an action applies to. */
@Serializable(with = SelectorSerializer::class)
sealed interface Selector

/** Selects objects picked out by the trigger that ran the action. */
@Serializable
enum class Parameter : Selector {
    /** The objects that were pressed or released. */
    Tapped,

    /** Every first target object touching the second target object. */
    FirstTargets,

    /** One of the [FirstTargets], picked each time the trigger fires. */
    RandomFirstTarget,

    /** The second target object that came into or left contact. */
    SecondTarget,

    /** The [FirstTargets] together with the [SecondTarget]. */
    AllTargets,

    /** One of [AllTargets], picked each time the trigger fires. */
    RandomTarget,
}

/**
 * The parameters worth offering for a trigger. A contact between identical filters has no sides
 * to tell apart, and there is only something to pick randomly from when several objects are needed.
 */
val Trigger.parameters: List<Parameter>
    get() = when (this) {
        Trigger.Start -> emptyList()
        is Trigger.Press, is Trigger.Release -> listOf(Parameter.Tapped)
        is Trigger.Contact -> {
            val severalNeeded = firstTargetCount > 1
            if (firstTarget == secondTarget) {
                listOfNotNull(Parameter.AllTargets, Parameter.RandomTarget.takeIf { severalNeeded })
            } else {
                listOfNotNull(
                    Parameter.FirstTargets,
                    Parameter.RandomFirstTarget.takeIf { severalNeeded },
                    Parameter.SecondTarget,
                )
            }
        }
    }

/** Parameters are encoded as their name, which keeps tag filters readable from before parameters existed. */
private object SelectorSerializer : KSerializer<Selector> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("com.example.microware.game.model.Selector", PolymorphicKind.SEALED)

    override fun serialize(encoder: Encoder, value: Selector) = when (value) {
        is TagFilter -> encoder.encodeSerializableValue(TagFilter.serializer(), value)
        is Parameter -> encoder.encodeSerializableValue(Parameter.serializer(), value)
    }

    override fun deserialize(decoder: Decoder): Selector {
        val json = (decoder as JsonDecoder).json
        val element = decoder.decodeJsonElement()
        val serializer = if (element is JsonPrimitive) Parameter.serializer() else TagFilter.serializer()
        return json.decodeFromJsonElement(serializer, element)
    }
}
