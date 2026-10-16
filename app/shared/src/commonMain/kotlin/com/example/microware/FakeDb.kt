package com.example.microware

import androidx.compose.ui.geometry.Offset
import com.example.microware.community.Avatar
import com.example.microware.community.Banner
import com.example.microware.community.Community
import com.example.microware.community.DurableId
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Game
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Obj
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.World
import com.example.microware.game.sprite.Sprite
import com.example.microware.play.Experience
import com.example.microware.util.BiMap
import com.example.microware.util.Id
import com.example.microware.util.Mut
import com.example.microware.util.set
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class FakeDb private constructor(
    private val users: MutableMap<Long, User> = mutableMapOf(),
    private val games: MutableMap<Long, DbGame> = mutableMapOf(),
    private val communities: MutableMap<Long, DbCommunity> = mutableMapOf(),
    private val communityMembers: MutableSet<Pair<Long, Long>> = mutableSetOf(),
    private val experiences: MutableMap<Long, DbExperience> = mutableMapOf(),
    private val experiencesGamesJoin: MutableSet<DbExperienceGame> = mutableSetOf(),
    private val sprites: MutableMap<Long, DbSprite> = mutableMapOf(),
    private val objects: MutableMap<Long, DbObj> = mutableMapOf(),
    private val triggers: MutableMap<Long, DbTrigger> = mutableMapOf(),
    private val actions: MutableList<DbAction> = mutableListOf(),
    private val counter: AtomicInt = AtomicInt(0),
) {
    companion object {
        fun empty() = FakeDb()
    }

    fun claimUser(user: User) {
        if (lookupUser(user) == null) {
            users[nextDatabaseId()] = user
        }
    }

    fun createCommunity(user: User, durableId: DurableId): Community? {
        val user = lookupUser(user) ?: return null
        val avatar = Avatar(Sprite.empty(32, 32))
        val banner = Banner(Sprite.empty(128, 32))

        val community = DbCommunity(
            avatar = fromSprite(avatar.sprite),
            banner = fromSprite(banner.sprite),
            durableId = durableId.bits,
            owner = user,
        )
        val communityId = nextDatabaseId()
        communityMembers.add(communityId to user)
        communities[communityId] = community
        return Community(communityId, durableId, avatar, banner, listOf())
    }

    fun updateCommunityAvatar(user: User, communityId: Long, avatar: Avatar) {
        val user = lookupUser(user) ?: return
        communities[communityId]?.let {
            if (it.owner != user) return
            val avatar = fromSprite(avatar.sprite)
            communities[communityId] = it.copy(avatar = avatar)
        }
    }

    fun updateCommunityBanner(user: User, communityId: Long, banner: Banner) {
        val user = lookupUser(user) ?: return
        communities[communityId]?.let {
            if (it.owner != user) return
            val banner = fromSprite(banner.sprite)
            communities[communityId] = it.copy(banner = banner)
        }
    }

    fun getJoinedCommunities(user: User): List<Community> {
        val user = lookupUser(user) ?: return listOf()
        return communityMembers
            .filter { it.second == user }
            .distinctBy { it.first }
            .mapNotNull { (id, _) -> communities[id]?.let { id to it } }
            .map { (id, dbCom) -> toCommunity(id, dbCom) }
    }

    fun joinCommunity(community: DurableId, user: User) {
        val user = lookupUser(user) ?: return
        val community = communities.entries.find { it.value.durableId == community.bits } ?: return
        communityMembers.add(community.key to user)
    }

    fun publishGame(user: User, game: Game, experience: Long) {
        val user = lookupUser(user) ?: return
        val experience = experiences[experience] ?: return
        val communityId = experience.community
        if (!communityMembers.contains(communityId to user)) return
        fromGame(user, game)
    }

    private fun lookupUser(user: User): Long? = users.entries.find { it.value == user }?.key

    private fun toWorld(gameId: Long): World {
        val logicTags = Mut(BiMap<LogicTag, Id>())
        val worldObjects = objects.values
            .filter { it.game == gameId }
            .sortedBy { it.index }
            .map { it.toObj(sprites.getValue(it.sprite), logicTags) }

        return World(
            objects = worldObjects.toPersistentList(),
            logicTags = logicTags.value,
        )
    }

    private fun toLogics(gameId: Long): PersistentList<Logic> = triggers.entries
        .filter { it.value.game == gameId }
        .sortedBy { it.value.index }
        .map { (triggerId, trigger) ->
            Logic(
                trigger.toTrigger(),
                actions
                    .filter { it.trigger == triggerId }
                    .sortedBy { it.index }
                    .map { it.toAction() },
            )
        }
        .toPersistentList()

    private fun toGame(id: Long, game: DbGame): Game = Game(
        dbId = id,
        world = toWorld(id),
        logics = toLogics(id),
    )

    private fun toExperience(id: Long, experience: DbExperience): Experience = Experience(
        dbId = id,
        banner = Banner(sprites.getValue(experience.banner).toSprite()),
        games = experiencesGamesJoin
            .filter { it.experience == id }
            .sortedBy { it.index }
            .map { toGame(it.game, games.getValue(it.game)) },
    )

    private fun toCommunity(id: Long, community: DbCommunity): Community = Community(
        dbId = id,
        code = DurableId(community.durableId),
        icon = Avatar(sprites.getValue(community.avatar).toSprite()),
        memberBanner = Banner(sprites.getValue(community.banner).toSprite()),
        experiences = experiences.entries
            .filter { it.value.community == id }
            .sortedBy { it.value.index }
            .map { (experienceId, experience) -> toExperience(experienceId, experience) },
    )

    private fun fromGame(userId: Long, game: Game): Long {
        val gameId = game.dbId ?: nextDatabaseId()

        actions.removeAll { triggers[it.trigger]?.game == gameId }
        val oldTriggerIds = triggers
            .filterValues { it.game == gameId }
            .keys
        oldTriggerIds.forEach(triggers::remove)
        objects.filterValues { it.game == gameId }.keys.forEach(objects::remove)

        games[gameId] = DbGame(user = userId)
        fromWorld(gameId, game.world)
        fromLogics(gameId, game.logics)
        return gameId
    }

    private fun fromWorld(gameId: Long, world: World) {
        world.objects.forEachIndexed { index, obj ->
            val spriteId = fromSprite(obj.sprite)
            val tag = world.logicTagFor(obj.id)?.tag
            objects[nextDatabaseId()] = obj.toDbObj(gameId, index, spriteId, tag)
        }
    }

    private fun fromLogics(gameId: Long, logics: PersistentList<Logic>) {
        logics.forEachIndexed { index, logic ->
            val triggerId = nextDatabaseId()
            triggers[triggerId] = fromTrigger(gameId, index, logic.trigger)
            logic.actions.forEachIndexed { actionIndex, action ->
                actions += fromAction(triggerId, actionIndex, action)
            }
        }
    }

    private fun fromSprite(sprite: Sprite): Long {
        val spriteId = nextDatabaseId()
        sprites[spriteId] = DbSprite(
            width = sprite.width,
            height = sprite.height,
            pixels = sprite.packedBytes(),
        )
        return spriteId
    }

    private fun nextDatabaseId(): Long = counter.fetchAndAdd(1).toLong()
}

private class DbSprite(
    val width: Int,
    val height: Int,
    val pixels: ByteArray,
) {
    fun toSprite(): Sprite = Sprite.fromPackedBytes(width, height, pixels)
}

private class DbTrigger(
    val game: Long,
    val index: Int,
    val variant: Int,
    val tapTarget: Int? = null,
    val collisionFirstTarget: Int? = null,
    val collisionSecondTarget: Int? = null,
) {
    fun toTrigger(): Trigger = when (this.variant) {
        0 -> Trigger.Start
        1 -> Trigger.Tap(LogicTag(tapTarget ?: 0))
        2 -> Trigger.Collision(
            LogicTag(collisionFirstTarget ?: 0),
            LogicTag(collisionSecondTarget ?: 0),
        )

        else -> Trigger.Start
    }
}

private fun fromTrigger(game: Long, index: Int, trigger: Trigger): DbTrigger = when (trigger) {
    Trigger.Start -> DbTrigger(game, index, variant = 0)
    is Trigger.Tap -> DbTrigger(game, index, variant = 1, tapTarget = trigger.target.tag)
    is Trigger.Collision -> DbTrigger(
        game, index,
        variant = 2,
        collisionFirstTarget = trigger.firstTarget.tag,
        collisionSecondTarget = trigger.secondTarget.tag,
    )
}

private class DbAction(
    val trigger: Long,
    val index: Int,
    val variant: Int,
    val moveMover: Int? = null,
    val moveTarget: Int? = null,
    val moveSpeed: Int? = null,
    val moveToMover: Int? = null,
    val moveToTarget: Int? = null,
    val moveToSpeed: Int? = null,
    val moveWithinMover: Int? = null,
    val moveWithinTarget: Int? = null,
    val moveWithinSpeed: Int? = null,
) {
    fun toAction(): Action = when (variant) {
        0 -> Action.Win
        1 -> Action.Lose
        2 -> Action.Move(
            LogicTag(moveMover ?: 0),
            Direction.entries[moveTarget ?: 0],
            moveSpeed ?: 0,
        )

        3 -> Action.MoveTo(
            LogicTag(moveToMover ?: 0),
            LogicTag(moveToTarget ?: 0),
            moveToSpeed,
        )

        4 -> Action.MoveWithin(
            LogicTag(moveWithinMover ?: 0),
            LogicTag(moveWithinTarget ?: 0),
            moveWithinSpeed,
        )

        else -> Action.Win
    }
}

private fun fromAction(trigger: Long, index: Int, action: Action): DbAction =
    when (action) {
        Action.Win -> DbAction(trigger, index, variant = 0)
        Action.Lose -> DbAction(trigger, index, variant = 1)
        is Action.Move -> DbAction(
            trigger, index,
            variant = 2,
            moveMover = action.mover.tag,
            moveTarget = action.target.ordinal,
            moveSpeed = action.speed,
        )

        is Action.MoveTo -> DbAction(
            trigger, index,
            variant = 3,
            moveToMover = action.mover.tag,
            moveToTarget = action.target.tag,
            moveToSpeed = action.speed,
        )

        is Action.MoveWithin -> DbAction(
            trigger, index,
            variant = 4,
            moveWithinMover = action.mover.tag,
            moveWithinTarget = action.target.tag,
            moveWithinSpeed = action.speed,
        )
    }

private data class DbGame(
    val user: Long,
)

private data class DbObj(
    val game: Long,
    val index: Int,
    val sprite: Long,
    val xOffset: Float,
    val yOffset: Float,
    val tag: Int?,
) {
    fun toObj(sprite: DbSprite, logicTags: Mut<BiMap<LogicTag, Id>>): Obj {
        val obj = Obj(
            offset = Offset(xOffset, yOffset),
            sprite = sprite.toSprite(),
        )
        tag?.let { logicTags[LogicTag(it)] = obj.id }
        return obj
    }
}

private fun Obj.toDbObj(game: Long, index: Int, sprite: Long, tag: Int?): DbObj = DbObj(
    game = game,
    index = index,
    sprite = sprite,
    xOffset = offset.x,
    yOffset = offset.y,
    tag = tag,
)

private data class DbExperience(
    val banner: Long,
    val community: Long,
    val index: Int,
)

private data class DbExperienceGame(
    val experience: Long,
    val game: Long,
    val index: Int,
)

private data class DbCommunity(
    val banner: Long,
    val avatar: Long,
    val durableId: Long,
    val owner: Long,
)
