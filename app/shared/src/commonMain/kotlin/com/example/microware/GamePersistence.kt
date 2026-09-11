package com.example.microware

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val GamesStorageKey = "microware.games.v1"
private const val CurrentGamesVersion = 1

@Serializable
internal data class StoredGames(
    val version: Int = CurrentGamesVersion,
    val nextGameId: Int = 1,
    val games: List<StoredGame> = emptyList(),
)

@Serializable
internal data class StoredGame(
    val id: Int,
    val world: GameStoredWorld,
    val logics: List<GameStoredLogic> = emptyList(),
)

@Serializable
internal data class GameStoredWorld(
    val background: StoredSprite,
    val objects: List<GameStoredObj> = emptyList(),
    val logicTags: List<GameStoredLogicTag> = emptyList(),
)

@Serializable
internal data class GameStoredLogicTag(
    val tag: Int,
    val objectId: Int,
)

@Serializable
internal data class GameStoredObj(
    val id: Int,
    val offsetX: Float,
    val offsetY: Float,
    val sprite: StoredSprite,
)

@Serializable
internal data class GameStoredLogic(
    val id: Int,
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
    val minimumNextId = (games.maxOfOrNull { it.id } ?: 0) + 1
    return GameStoreState(
        games = games,
        nextGameId = maxOf(nextGameId, minimumNextId),
    )
}

internal fun GameStoreState.toStoredGames(): StoredGames = StoredGames(
    nextGameId = nextGameId,
    games = games.map(Game::toStoredGame),
)

private fun Game.toStoredGame(): StoredGame = StoredGame(
    id = id,
    world = world.toStoredWorld(),
    logics = logics.map(Logic::toStoredLogic),
)

private fun StoredGame.toGame(): Game = Game(
    id = id,
    world = world.toWorld(),
    logics = logics.map(GameStoredLogic::toLogic),
)

private fun World.toStoredWorld(): GameStoredWorld = GameStoredWorld(
    background = SpriteCodec.encode(background),
    objects = objects.map(Obj::toStoredObj),
    logicTags = logicTags.map { (tag, objectId) ->
        GameStoredLogicTag(tag = tag.tag, objectId = objectId)
    },
)

private fun GameStoredWorld.toWorld(): World = World(
    background = SpriteCodec.decode(background),
    objects = objects.map(GameStoredObj::toObj),
    logicTags = logicTags.associate { storedTag ->
        LogicTag(storedTag.tag) to storedTag.objectId
    },
)

private fun Obj.toStoredObj(): GameStoredObj = GameStoredObj(
    id = id,
    offsetX = offset.x,
    offsetY = offset.y,
    sprite = SpriteCodec.encode(sprite),
)

private fun GameStoredObj.toObj(): Obj = Obj(
    id = id,
    offset = Offset(offsetX, offsetY),
    sprite = SpriteCodec.decode(sprite),
)

private fun Logic.toStoredLogic(): GameStoredLogic = GameStoredLogic(
    id = id,
    trigger = trigger.toStoredTrigger(),
    actions = actions.map(Action::toStoredAction),
)

private fun GameStoredLogic.toLogic(): Logic = Logic(
    id = id,
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
