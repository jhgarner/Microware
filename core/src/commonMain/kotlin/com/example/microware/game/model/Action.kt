package com.example.microware.game.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Action {
    @Serializable
    class Move(val mover: TagFilter, val target: Direction, val speed: Int) : Action

    // If the speed is null, the mover will instantly teleport to its destination.
    @Serializable
    class MoveTo(val mover: TagFilter, val target: TagFilter, val speed: Int?) : Action

    @Serializable
    class MoveWithin(val mover: TagFilter, val target: TagFilter, val speed: Int?) : Action

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

    @Serializable
    data class Collision(val firstTarget: TagFilter, val secondTarget: TagFilter) : Trigger
}

@Serializable
enum class Direction { N, S, E, W }