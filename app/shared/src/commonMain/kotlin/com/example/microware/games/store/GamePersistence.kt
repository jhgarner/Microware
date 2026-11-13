package com.example.microware.games.store

import androidx.compose.ui.geometry.Offset
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Game
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Obj
import com.example.microware.game.model.Parameter
import com.example.microware.game.model.Selector
import com.example.microware.game.model.TagFilter
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.World
import com.example.microware.game.sprite.SpriteCodec
import com.example.microware.game.sprite.StoredSprite
import com.example.microware.platform.AppStorage
import com.example.microware.util.BiMap
import com.example.microware.util.Id
import com.example.microware.util.Identifiable
import com.example.microware.util.Mut
import com.example.microware.util.set
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val GamesStorageKey = "microware.games.v1"
private const val CurrentGamesVersion = 1

// The singular `tag` fields are only read: games saved before objects could carry
// several tags stored exactly one tag in each of them.
private fun storedTags(tags: Set<Int>, legacyTag: Int?): Set<LogicTag> =
    (tags + listOfNotNull(legacyTag)).map(::LogicTag).toSet()

private fun storedFilter(tags: Set<Int>, legacyTag: Int?) = TagFilter(storedTags(tags, legacyTag))

private fun Set<LogicTag>.toStoredTags(): Set<Int> = map(LogicTag::tag).toSet()

private fun TagFilter.toStoredTags(): Set<Int> = tags.toStoredTags()

private fun Selector.toStoredTags(): Set<Int> = (this as? TagFilter)?.toStoredTags().orEmpty()

private fun storedSelector(parameter: Parameter?, tags: Set<Int>, legacyTag: Int?): Selector =
    parameter ?: storedFilter(tags, legacyTag)

@Serializable
internal data class StoredGames(
    val version: Int = CurrentGamesVersion,
    val games: List<StoredGame> = emptyList(),
)

@Serializable
internal data class StoredGame(
    val dbId: Long?,
    val world: GameStoredWorld,
    val logics: List<GameStoredLogic> = persistentListOf(),
)

@Serializable
internal data class GameStoredWorld(
    val objects: List<GameStoredObj> = emptyList(),
)

@Serializable
internal data class GameStoredObj(
    val offsetX: Float,
    val offsetY: Float,
    val sprite: StoredSprite,
    val tags: Set<Int> = emptySet(),
    val tag: Int? = null,
)

@Serializable
internal data class GameStoredLogic(
    val trigger: GameStoredTrigger,
    val actions: List<GameStoredAction>,
)

@Serializable
internal data class GameStoredTrigger(
    val type: String,
    val firstTags: Set<Int> = emptySet(),
    val secondTags: Set<Int> = emptySet(),
    val firstTag: Int? = null,
    val secondTag: Int? = null,
    val firstCount: Int = 1,
)

@Serializable
internal data class GameStoredAction(
    val type: String,
    val moverTags: Set<Int> = emptySet(),
    val targetTags: Set<Int> = emptySet(),
    val moverTag: Int? = null,
    val targetTag: Int? = null,
    val moverParameter: Parameter? = null,
    val targetParameter: Parameter? = null,
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
    dbId = dbId,
    world = world.toStoredWorld(),
    logics = logics.map(Logic::toStoredLogic),
)

private fun StoredGame.toGame(): Game = Game(
    dbId = dbId,
    world = world.toWorld(),
    logics = logics.map(GameStoredLogic::toLogic).toPersistentList(),
)

private fun World.toStoredWorld(): GameStoredWorld = GameStoredWorld(
    objects = objects.map { it.toStoredObj(logicTags) },
)

private fun GameStoredWorld.toWorld(): World {
    val logicTags = Mut(BiMap<LogicTag, Id>())
    return World(
        objects = objects.map { it.toObj(logicTags) }.toPersistentList(),
        logicTags = logicTags.value,
    )
}

private fun Obj.toStoredObj(tags: BiMap<LogicTag, Id>): GameStoredObj = GameStoredObj(
    offsetX = offset.x,
    offsetY = offset.y,
    sprite = SpriteCodec.encode(sprite),
    tags = tags.flip()[id].orEmpty().toStoredTags(),
)

private fun GameStoredObj.toObj(tags: Mut<BiMap<LogicTag, Id>>): Obj {
    val id = Identifiable.nextId()
    storedTags(this.tags, tag).forEach { tags[it] = id }
    return Obj(
        id = id,
        offset = Offset(offsetX, offsetY),
        sprite = SpriteCodec.decode(sprite),
    )
}

internal fun Logic.toStoredLogic(): GameStoredLogic = GameStoredLogic(
    trigger = trigger.toStoredTrigger(),
    actions = actions.map(Action::toStoredAction),
)

internal fun GameStoredLogic.toLogic(): Logic = Logic(
    trigger = trigger.toTrigger(),
    actions = actions.map(GameStoredAction::toAction),
)

private fun Trigger.toStoredTrigger(): GameStoredTrigger = when (this) {
    Trigger.Start -> GameStoredTrigger(type = "start")
    is Trigger.Release -> GameStoredTrigger(type = "release", firstTags = target.toStoredTags())
    is Trigger.Press -> GameStoredTrigger(type = "press", firstTags = target.toStoredTags())
    is Trigger.Contact -> GameStoredTrigger(
        type = when (this) {
            is Trigger.Collision -> "collision"
            is Trigger.Separation -> "separation"
        },
        firstTags = firstTarget.toStoredTags(),
        secondTags = secondTarget.toStoredTags(),
        firstCount = firstTargetCount,
    )
}

private fun GameStoredTrigger.toTrigger(): Trigger = when (type) {
    "start" -> Trigger.Start
    "tap", "release" -> Trigger.Release(storedFilter(firstTags, firstTag))
    "press" -> Trigger.Press(storedFilter(firstTags, firstTag))
    "collision" -> Trigger.Collision(
        firstTarget = storedFilter(firstTags, firstTag),
        secondTarget = storedFilter(secondTags, secondTag),
        firstTargetCount = firstCount,
    )

    "separation" -> Trigger.Separation(
        firstTarget = storedFilter(firstTags, firstTag),
        secondTarget = storedFilter(secondTags, secondTag),
        firstTargetCount = firstCount,
    )

    else -> error("Unknown trigger type: $type")
}

private fun Action.toStoredAction(): GameStoredAction = when (this) {
    is Action.Move -> GameStoredAction(
        type = "move",
        moverTags = mover.toStoredTags(),
        moverParameter = mover as? Parameter,
        direction = target.name,
        speed = speed,
    )

    is Action.MoveTo -> GameStoredAction(
        type = "moveTo",
        moverTags = mover.toStoredTags(),
        moverParameter = mover as? Parameter,
        targetTags = target.toStoredTags(),
        targetParameter = target as? Parameter,
        speed = speed,
    )

    is Action.MoveWithin -> GameStoredAction(
        type = "moveWithin",
        moverTags = mover.toStoredTags(),
        moverParameter = mover as? Parameter,
        targetTags = target.toStoredTags(),
        targetParameter = target as? Parameter,
        speed = speed,
    )

    Action.Win -> GameStoredAction(type = "win")
    Action.Lose -> GameStoredAction(type = "lose")
}

private fun GameStoredAction.toAction(): Action = when (type) {
    "move" -> Action.Move(
        mover = storedSelector(moverParameter, moverTags, moverTag),
        target = Direction.valueOf(requireNotNull(direction)),
        speed = requireNotNull(speed),
    )

    "moveTo" -> Action.MoveTo(
        mover = storedSelector(moverParameter, moverTags, moverTag),
        target = storedSelector(targetParameter, targetTags, targetTag),
        speed = speed,
    )

    "moveWithin" -> Action.MoveWithin(
        mover = storedSelector(moverParameter, moverTags, moverTag),
        target = storedSelector(targetParameter, targetTags, targetTag),
        speed = speed,
    )

    "win" -> Action.Win
    "lose" -> Action.Lose
    else -> error("Unknown action type: $type")
}
