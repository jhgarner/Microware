package com.example.microware.games.store

import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Game
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Obj
import com.example.microware.game.model.Parameter
import com.example.microware.game.model.TagFilter
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.World
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteCodec
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.json.Json
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
        assertEquals(Trigger.Release(TagFilter(LogicTag(7))), legacyTrigger)

        val state = GameStoreState(
            games = persistentListOf(
                Game(
                    world = World(),
                    logics = persistentListOf(
                        Logic(Trigger.Release(TagFilter(LogicTag(7)))),
                        Logic(Trigger.Press(TagFilter(LogicTag(8)))),
                    ),
                ),
            ),
        )
        val stored = state.toStoredGames()
        val storedTriggers = stored.games.single().logics.map { it.trigger }

        assertEquals(
            listOf(
                GameStoredTrigger(type = "release", firstTags = setOf(7)),
                GameStoredTrigger(type = "press", firstTags = setOf(8)),
            ),
            storedTriggers,
        )
        assertEquals(
            listOf(Trigger.Release(TagFilter(LogicTag(7))), Trigger.Press(TagFilter(LogicTag(8)))),
            stored.toState().games.single().logics.map { it.trigger },
        )
    }

    @Test
    fun gamesSavedWithOneTagPerObjectLoadAsSingleTagSets() {
        val saved = """
            {"version":1,"games":[{"dbId":null,
              "world":{"objects":[
                {"offsetX":0.0,"offsetY":0.0,"sprite":$storedSprite,"tag":4},
                {"offsetX":0.0,"offsetY":0.0,"sprite":$storedSprite,"tag":null}]},
              "logics":[{"trigger":{"type":"collision","firstTag":4,"secondTag":5},
                "actions":[{"type":"moveTo","moverTag":4,"targetTag":5,"speed":3}]}]}]}
        """

        val game = Json.decodeFromString<StoredGames>(saved).toState().games.single()

        assertEquals(
            listOf(setOf(LogicTag(4)), emptySet()),
            game.world.objects.map { game.world.logicTagsFor(it.id) },
        )
        val logic = game.logics.single()
        assertEquals(
            Trigger.Collision(TagFilter(LogicTag(4)), TagFilter(LogicTag(5))),
            logic.trigger,
        )
        val action = logic.actions.single() as Action.MoveTo
        assertEquals(TagFilter(LogicTag(4)), action.mover)
        assertEquals(TagFilter(LogicTag(5)), action.target)
    }

    @Test
    fun objectsAndFiltersKeepEveryTagWhenSaved() {
        val obj = Obj(4, 4)
        val both = TagFilter(LogicTag(1), LogicTag(2))
        val state = GameStoreState(
            games = persistentListOf(
                Game(
                    world = World()
                        .addObject(obj)
                        .assignLogicTag(obj.id, LogicTag(1))
                        .assignLogicTag(obj.id, LogicTag(2)),
                    logics = persistentListOf(
                        Logic(Trigger.Press(both), listOf(Action.Move(both, Direction.N, speed = 1))),
                    ),
                ),
            ),
        )

        val encoded = Json.encodeToString(state.toStoredGames())
        val game = Json.decodeFromString<StoredGames>(encoded).toState().games.single()

        assertEquals(
            setOf(LogicTag(1), LogicTag(2)),
            game.world.logicTagsFor(game.world.objects.single().id),
        )
        assertEquals(Trigger.Press(both), game.logics.single().trigger)
        assertEquals(both, (game.logics.single().actions.single() as Action.Move).mover)
    }

    @Test
    fun separationAndItsObjectCountSurviveSaving() {
        val trigger = Trigger.Separation(
            TagFilter(LogicTag(1)),
            TagFilter(LogicTag(2)),
            firstTargetCount = 3,
        )
        val stored = Json.encodeToString(Logic(trigger, emptyList()).toStoredLogic())

        assertEquals(trigger, Json.decodeFromString<GameStoredLogic>(stored).toLogic().trigger)
    }

    @Test
    fun actionParametersSurviveSaving() {
        val logic = Logic(
            Trigger.Press(TagFilter(LogicTag(1))),
            listOf(Action.MoveTo(Parameter.Tapped, TagFilter(LogicTag(2)), speed = null)),
        )
        val stored = Json.encodeToString(logic.toStoredLogic())

        val action = Json.decodeFromString<GameStoredLogic>(stored).toLogic().actions.single() as Action.MoveTo

        assertEquals(Parameter.Tapped, action.mover)
        assertEquals(TagFilter(LogicTag(2)), action.target)
    }

    private val storedSprite = Json.encodeToString(SpriteCodec.encode(Sprite.empty(1, 1)))
}
