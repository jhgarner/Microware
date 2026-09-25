package com.example.microware.games.store
import com.example.microware.util.set
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Game
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Obj
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.World
import com.example.microware.game.sprite.SpriteCodec
import com.example.microware.game.sprite.StoredSprite
import com.example.microware.platform.AppStorage
import com.example.microware.util.BiMap
import com.example.microware.util.Id
import com.example.microware.util.Identifiable
import com.example.microware.util.Mut

import androidx.compose.ui.geometry.Offset
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val GamesStorageKey = "microware.games.v1"
private const val CurrentGamesVersion = 1

@Serializable
internal data class StoredGames(
    val version: Int = CurrentGamesVersion,
    val games: List<StoredGame> = emptyList(),
)

@Serializable
internal data class StoredGame(
    val world: GameStoredWorld,
    val logics: List<GameStoredLogic> = persistentListOf(),
)

@Serializable
internal data class GameStoredWorld(
    val background: StoredSprite,
    val objects: List<GameStoredObj> = emptyList(),
)

@Serializable
internal data class GameStoredObj(
    val offsetX: Float,
    val offsetY: Float,
    val sprite: StoredSprite,
    val tag: Int?,
)

@Serializable
internal data class GameStoredLogic(
    val trigger: GameStoredTrigger,
    val actions: List<GameStoredAction>,
)

@Serializable
internal data class GameStoredTrigger(
    val type: String,
    val firstTag: Int? = null,
    val secondTag: Int? = null,
)

@Serializable
internal data class GameStoredAction(
    val type: String,
    val moverTag: Int? = null,
    val targetTag: Int? = null,
    val direction: String? = null,
    val speed: Int? = null,
)

internal interface GameRepository {
    fun load(): StoredGames
    fun save(games: StoredGames)
}

internal object AppGameRepository : GameRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): StoredGames {
        val encoded = AppStorage.read(GamesStorageKey) ?: return StoredGames()
        return runCatching {
            json.decodeFromString<StoredGames>(encoded).also {
                require(it.version == CurrentGamesVersion)
            }
        }.getOrDefault(StoredGames())
    }

    override fun save(games: StoredGames) {
        AppStorage.write(GamesStorageKey, json.encodeToString(games))
    }
}

internal fun StoredGames.toState(): GameStoreState {
    val games = games.map(StoredGame::toGame)
    return GameStoreState(
        games = games.toPersistentList(),
    )
}

internal fun GameStoreState.toStoredGames(): StoredGames = StoredGames(
    games = games.map(Game::toStoredGame),
)

private fun Game.toStoredGame(): StoredGame = StoredGame(
    world = world.toStoredWorld(),
    logics = logics.map(Logic::toStoredLogic),
)

private fun StoredGame.toGame(): Game = Game(
    world = world.toWorld(),
    logics = logics.map(GameStoredLogic::toLogic).toPersistentList(),
)

private fun World.toStoredWorld(): GameStoredWorld = GameStoredWorld(
    background = SpriteCodec.encode(background),
    objects = objects.map { it.toStoredObj(logicTags) },
)

private fun GameStoredWorld.toWorld(): World {
    val logicTags = Mut(BiMap<LogicTag, Id>())
    return World(
        background = SpriteCodec.decode(background),
        objects = objects.map { it.toObj(logicTags) }.toPersistentList(),
        logicTags = logicTags.value
    )
}

private fun Obj.toStoredObj(tags: BiMap<LogicTag, Id>): GameStoredObj = GameStoredObj(
    offsetX = offset.x,
    offsetY = offset.y,
    sprite = SpriteCodec.encode(sprite),
    tag = tags.flip()[id]?.tag
)

private fun GameStoredObj.toObj(tags: Mut<BiMap<LogicTag, Id>>): Obj {
    val id = Identifiable.nextId()
    tag?.let { tags[LogicTag(it)] = id }
    return Obj(
        id = id,
        offset = Offset(offsetX, offsetY),
        sprite = SpriteCodec.decode(sprite),
    )
}

private fun Logic.toStoredLogic(): GameStoredLogic = GameStoredLogic(
    trigger = trigger.toStoredTrigger(),
    actions = actions.map(Action::toStoredAction),
)

private fun GameStoredLogic.toLogic(): Logic = Logic(
    trigger = trigger.toTrigger(),
    actions = actions.map(GameStoredAction::toAction),
)

private fun Trigger.toStoredTrigger(): GameStoredTrigger = when (this) {
    Trigger.Start -> GameStoredTrigger(type = "start")
    is Trigger.Tap -> GameStoredTrigger(type = "tap", firstTag = target.tag)
    is Trigger.Collision -> GameStoredTrigger(
        type = "collision",
        firstTag = firstTarget.tag,
        secondTag = secondTarget.tag,
    )
}

private fun GameStoredTrigger.toTrigger(): Trigger = when (type) {
    "start" -> Trigger.Start
    "tap" -> Trigger.Tap(LogicTag(requireNotNull(firstTag)))
    "collision" -> Trigger.Collision(
        firstTarget = LogicTag(requireNotNull(firstTag)),
        secondTarget = LogicTag(requireNotNull(secondTag)),
    )

    else -> error("Unknown trigger type: $type")
}

private fun Action.toStoredAction(): GameStoredAction = when (this) {
    is Action.Move -> GameStoredAction(
        type = "move",
        moverTag = mover.tag,
        direction = target.name,
        speed = speed,
    )

    is Action.MoveTo -> GameStoredAction(
        type = "moveTo",
        moverTag = mover.tag,
        targetTag = target.tag,
        speed = speed,
    )

    Action.Win -> GameStoredAction(type = "win")
    Action.Lose -> GameStoredAction(type = "lose")
}

private fun GameStoredAction.toAction(): Action = when (type) {
    "move" -> Action.Move(
        mover = LogicTag(requireNotNull(moverTag)),
        target = Direction.valueOf(requireNotNull(direction)),
        speed = requireNotNull(speed),
    )

    "moveTo" -> Action.MoveTo(
        mover = LogicTag(requireNotNull(moverTag)),
        target = LogicTag(requireNotNull(targetTag)),
        speed = speed,
    )

    "win" -> Action.Win
    "lose" -> Action.Lose
    else -> error("Unknown action type: $type")
}
