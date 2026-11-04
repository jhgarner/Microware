package com.example.microware.games.store

import com.example.microware.game.model.Game
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.World
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

class GamePersistenceTest {
    @Test
    fun legacyTapDecodesAsReleaseAndNewTriggersPersistByType() {
        val legacyGames = StoredGames(
            games = listOf(
                StoredGame(
                    dbId = null,
                    world = GameStoredWorld(),
                    logics = listOf(
                        GameStoredLogic(GameStoredTrigger(type = "tap", firstTag = 7), actions = emptyList()),
                    ),
                ),
            ),
        )

        val legacyTrigger = legacyGames.toState().games.single().logics.single().trigger
        assertEquals(Trigger.Release(LogicTag(7)), legacyTrigger)

        val state = GameStoreState(
            games = persistentListOf(
                Game(
                    world = World(),
                    logics = persistentListOf(
                        Logic(Trigger.Release(LogicTag(7))),
                        Logic(Trigger.Press(LogicTag(8))),
                    ),
                ),
            ),
        )
        val stored = state.toStoredGames()
        val storedTriggers = stored.games.single().logics.map { it.trigger }

        assertEquals(
            listOf(
                GameStoredTrigger(type = "release", firstTag = 7),
                GameStoredTrigger(type = "press", firstTag = 8),
            ),
            storedTriggers,
        )
        assertEquals(
            listOf(Trigger.Release(LogicTag(7)), Trigger.Press(LogicTag(8))),
            stored.toState().games.single().logics.map { it.trigger },
        )
    }
}
