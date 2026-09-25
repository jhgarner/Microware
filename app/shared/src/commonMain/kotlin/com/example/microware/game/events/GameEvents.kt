package com.example.microware.game.events
import com.example.microware.game.model.Game
import com.example.microware.game.model.Logic
import com.example.microware.game.model.Obj
import com.example.microware.game.model.World
import com.example.microware.util.Id

import kotlinx.collections.immutable.PersistentList

fun interface GamesEvent {
    fun modifyGames(games: PersistentList<Game>): PersistentList<Game>

    companion object {
        fun GameEvents.forGame(i: Int): GamesEvent = GamesEvent { games ->
            games.replacingAt(i, modifyGame(games[i]))
        }
    }
}

fun interface GameEvents {
    fun modifyGame(game: Game): Game
}

fun interface WorldEvent : GameEvents {
    fun modifyWorld(world: World): World

    override fun modifyGame(game: Game): Game = game.copy(world = modifyWorld(game.world))
}

fun interface ObjEvent : WorldEvent {
    fun modifyObj(world: World): Obj

    override fun modifyWorld(world: World): World = world.upsertObject(modifyObj(world))
}

fun interface LogicsEvent : GameEvents {
    fun modifyLogics(logics: PersistentList<Logic>): PersistentList<Logic>

    override fun modifyGame(game: Game): Game = game.copy(logics = modifyLogics(game.logics))

    companion object {
    }
}

fun interface LogicEvent {
    fun modifyLogic(logic: Logic): Logic

    fun forId(id: Id): LogicsEvent = LogicsEvent { logics ->
        val i = logics.indexOfFirst { it.id == id }
        logics.replacingAt(i, modifyLogic(logics[i]))
    }

    fun forLogic(i: Int): LogicsEvent = LogicsEvent { logics ->
        logics.replacingAt(i, modifyLogic(logics[i]))
    }
}
