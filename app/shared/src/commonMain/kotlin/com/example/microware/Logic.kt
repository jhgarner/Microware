package com.example.microware

import com.example.microware.Identifiable.Companion.nextId
import kotlinx.serialization.Transient

data class Logic(
    val trigger: Trigger,
    val actions: List<Action> = listOf(),
    @Transient
    override val id: Id = nextId(),
) : Identifiable<Logic> {
    override val self = this
    override fun newId(id: Id): Logic = copy(id = id)
}

sealed interface Action {
    class Move(val mover: LogicTag, val target: Direction, val speed: Int) : Action

    // If the speed is null, the mover will instantly teleport to the target.
    class MoveTo(val mover: LogicTag, val target: LogicTag, val speed: Int?) : Action

    object Win : Action
    object Lose : Action
}

sealed interface Trigger {
    object Start : Trigger
    data class Tap(val target: LogicTag) : Trigger
    data class Collision(val firstTarget: LogicTag, val secondTarget: LogicTag) : Trigger
}

enum class Direction { N, S, E, W }
