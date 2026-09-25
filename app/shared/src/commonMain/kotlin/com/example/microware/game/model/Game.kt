package com.example.microware.game.model
import com.example.microware.util.Id
import com.example.microware.util.Identifiable

import com.example.microware.util.Identifiable.Companion.nextId
import kotlinx.collections.immutable.PersistentList
import kotlinx.serialization.Transient

data class Game(
    val world: World,
    val logics: PersistentList<Logic>,
    @Transient
    override val id: Id = nextId(),
) : Identifiable<Game> {
    override val self = this
    override fun newId(id: Id): Game = copy(id = id)
}
