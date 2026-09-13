package com.example.microware

import com.example.microware.Identifiable.Companion.nextId
import kotlinx.collections.immutable.PersistentList
import kotlinx.serialization.Transient

data class Game(
    val world: World,
    val logics: PersistentList<Logic>,
    @Transient
    override val id: Int = nextId(),
) : Identifiable<Game> {
    override val self = this
    override fun newId(id: Int): Game = copy(id = id)
}
