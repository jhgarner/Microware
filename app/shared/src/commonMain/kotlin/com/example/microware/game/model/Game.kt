package com.example.microware.game.model

import com.example.microware.core.Model
import com.example.microware.game.sprite.Sprite.Companion.load
import com.example.microware.util.BiMap
import com.example.microware.util.Id
import com.example.microware.util.Identifiable
import com.example.microware.util.Identifiable.Companion.nextId
import com.example.microware.util.toModel
import com.example.microware.util.toUi
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Transient

data class Game(
    val world: World,
    val logics: PersistentList<Logic>,
    val dbId: Long? = null,
    @Transient
    override val id: Id = nextId(),
) : Identifiable<Game> {
    override val self = this
    override fun newId(id: Id): Game = copy(id = id)

    fun toModel(): Model.Game = Model.Game(
        id = dbId,
        world = Model.World(
            objects = world.objects.map {
                Model.Obj(
                    offset = it.offset.toModel(),
                    sprite = it.sprite.toModel(),
                    tag = world.logicTags.flip()[it.id]?.tag,
                )
            },
        ),
        logics = logics.map { it.toModel() },
    )
}

fun Model.Game.fromModel(): Game {
    var logicTags = BiMap<LogicTag, Id>()
    val objects = world.objects.map { obj ->
        val id = nextId()
        obj.tag?.let { logicTags = logicTags.add(LogicTag(it), id) }
        Obj(offset = obj.offset.toUi(), sprite = obj.sprite.load(), id = id)
    }
    return Game(
        world = World(
            objects = objects.toPersistentList(),
            logicTags = logicTags,
        ),
        logics = logics.map { it.toUi() }.toPersistentList(),
    )
}
