package com.example.microware.game.model

import com.example.microware.core.Model
import com.example.microware.util.Id
import com.example.microware.util.Identifiable

import com.example.microware.util.Identifiable.Companion.nextId
import kotlinx.serialization.Transient

data class Logic(
    val trigger: Trigger,
    val actions: List<Action> = listOf(),
    @Transient
    override val id: Id = nextId(),
) : Identifiable<Logic> {
    override val self = this
    override fun newId(id: Id): Logic = copy(id = id)

    fun toModel(): Model.Logic = Model.Logic(trigger = trigger, actions = actions)
}

fun Model.Logic.toUi(): Logic = Logic(trigger = trigger, actions = actions)
