package com.example.microware.game.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface Action {
    @Serializable
    class Move(val mover: LogicTag, val target: Direction, val speed: Int) : Action

    // If the speed is null, the mover will instantly teleport to its destination.
    @Serializable
    class MoveTo(val mover: LogicTag, val target: LogicTag, val speed: Int?) : Action

    @Serializable
    class MoveWithin(val mover: LogicTag, val target: LogicTag, val speed: Int?) : Action

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
    data class Tap(val target: LogicTag) : Trigger

    @Serializable
    data class Collision(val firstTarget: LogicTag, val secondTarget: LogicTag) : Trigger
}

@Serializable
enum class Direction { N, S, E, W }