package com.example.microware.game.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Action {
    @Serializable
    class Move(val mover: Selector, val target: Direction, val speed: Int) : Action

    // If the speed is null, the mover will instantly teleport to its destination.
    @Serializable
    class MoveTo(val mover: Selector, val target: Selector, val speed: Int?) : Action

    @Serializable
    class MoveWithin(val mover: Selector, val target: Selector, val speed: Int?) : Action

    @Serializable
    object Win : Action

    @Serializable
    object Lose : Action
}

@Serializable
sealed interface Trigger {
    @Serializable
    object Start : Trigger

    @Serializable
    @SerialName("com.example.microware.game.model.Trigger.Tap")
    data class Release(val target: TagFilter) : Trigger

    @Serializable
    data class Press(val target: TagFilter) : Trigger

    /**
     * An object matching [secondTarget] is in contact while it touches at least
     * [firstTargetCount] objects matching [firstTarget].
     */
    @Serializable
    sealed interface Contact : Trigger {
        val firstTarget: TagFilter
        val secondTarget: TagFilter
        val firstTargetCount: Int
    }

    /** Fires once for each object that comes into contact. */
    @Serializable
    data class Collision(
        override val firstTarget: TagFilter,
        override val secondTarget: TagFilter,
        override val firstTargetCount: Int = 1,
    ) : Contact

    /** Fires once for each object that stops being in contact. */
    @Serializable
    data class Separation(
        override val firstTarget: TagFilter,
        override val secondTarget: TagFilter,
        override val firstTargetCount: Int = 1,
    ) : Contact
}

@Serializable
enum class Direction { N, S, E, W }