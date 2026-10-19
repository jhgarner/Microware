package com.example.microware.server

import com.example.microware.community.DurableId
import com.example.microware.core.Model.CommunityMetadata
import com.example.microware.core.Model.Experience
import com.example.microware.core.Model.ExperienceMetadata
import com.example.microware.core.Model.Game
import com.example.microware.core.Model.Logic
import com.example.microware.core.Model.Obj
import com.example.microware.core.Model.Offset
import com.example.microware.core.Model.PublishableExperience
import com.example.microware.core.Model.SaveExperienceMetadata
import com.example.microware.core.Model.Sprite
import com.example.microware.core.Model.World
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Trigger
import org.jetbrains.exposed.v1.core.Alias
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid

object Dao {
    fun withUser(userUuid: Uuid): EntityID<Long> = Tables.User
        .select(Tables.User.id)
        .where { isUser(userUuid) }
        .map { it[Tables.User.id] }
        .firstOrNull()
        ?: createUser(userUuid)

    fun createUser(uuid: Uuid): EntityID<Long> = uuid.toLongs { upperBits, lowerBits ->
        Tables.User.insertAndGetId {
            it[upper] = upperBits
            it[lower] = lowerBits
        }
    }

    fun createCommunity(userUuid: Uuid): EntityID<Long> {
        val userId = withUser(userUuid)
        val avatarSprite = Sprite(32, 32, ByteArray(640))
        val bannerSprite = Sprite(128, 32, ByteArray(2560))
        val communityId = Tables.Community.insertAndGetId {
            it[owner] = userId
            it[durableId] = DurableId().bits
            it[avatar] = writeSprite(avatarSprite)
            it[banner] = writeSprite(bannerSprite)
        }
        Tables.CommunityUserJoin.insert {
            it[user] = userId
            it[community] = communityId
        }
        return communityId
    }

    fun updateCommunityAvatar(userUuid: Uuid, communityId: Long, newAvatar: Sprite) {
        val userId = withUser(userUuid)
        val oldAvatar = Tables.Community.select(Tables.Community.avatar)
            .where((Tables.Community.id eq communityId) and (Tables.Community.owner eq userId))
            .first()[Tables.Community.avatar]
        Tables.Community.update(where = { (Tables.Community.id eq communityId) }) {
            it[avatar] = writeSprite(newAvatar)
        }
        Tables.Sprite.deleteWhere { id eq oldAvatar }
    }

    fun updateCommunityBanner(userUuid: Uuid, communityId: Long, newBanner: Sprite) {
        val userId = withUser(userUuid)
        val oldBanner = Tables.Community.select(Tables.Community.banner)
            .where((Tables.Community.id eq communityId) and (Tables.Community.owner eq userId))
            .first()[Tables.Community.banner]
        Tables.Community.update(where = { (Tables.Community.id eq communityId) }) {
            it[banner] = writeSprite(newBanner)
        }
        Tables.Sprite.deleteWhere { id eq oldBanner }
    }

    fun updateDurableId(userUuid: Uuid, communityId: Long, newDurableId: Long) {
        val userId = withUser(userUuid)
        Tables.Community.update(where = { (Tables.Community.id eq communityId) and (Tables.Community.owner eq userId) }) {
            it[durableId] = newDurableId
        }
    }

    fun getBanners(userUuid: Uuid): List<Sprite> {
        val userId = withUser(userUuid)
        val bannerTable = Tables.Sprite.alias("banners")
        return (Tables.Community innerJoin Tables.CommunityUserJoin)
            .join(
                otherTable = bannerTable,
                joinType = JoinType.INNER,
                onColumn = Tables.Community.banner,
                otherColumn = bannerTable[Tables.Sprite.id],
            )
            .select(
                bannerTable[Tables.Sprite.width],
                bannerTable[Tables.Sprite.height],
                bannerTable[Tables.Sprite.pixels],
            )
            .where(Tables.CommunityUserJoin.user eq userId)
            .map { it.toSprite(bannerTable) }
            .toList()
    }

    fun getCommunityMetadata(userUuid: Uuid): List<CommunityMetadata> {
        val user = withUser(userUuid)
        val bannerTable = Tables.Sprite.alias("banners")
        val avatarTable = Tables.Sprite.alias("avatars")
        return (Tables.Community innerJoin Tables.CommunityUserJoin)
            .join(
                otherTable = bannerTable,
                joinType = JoinType.INNER,
                onColumn = Tables.Community.banner,
                otherColumn = bannerTable[Tables.Sprite.id],
            )
            .join(
                otherTable = avatarTable,
                joinType = JoinType.INNER,
                onColumn = Tables.Community.avatar,
                otherColumn = avatarTable[Tables.Sprite.id],
            )
            .select(
                Tables.Community.id,
                Tables.Community.durableId,
                avatarTable[Tables.Sprite.width],
                avatarTable[Tables.Sprite.height],
                avatarTable[Tables.Sprite.pixels],
                bannerTable[Tables.Sprite.width],
                bannerTable[Tables.Sprite.height],
                bannerTable[Tables.Sprite.pixels],
            )
            .where(Tables.CommunityUserJoin.user eq user)
            .map {
                CommunityMetadata(
                    id = it[Tables.Community.id].value,
                    code = it[Tables.Community.durableId],
                    icon = it.toSprite(avatarTable),
                    banner = it.toSprite(bannerTable),
                )
            }
    }

    fun joinCommunity(userUuid: Uuid, communityDurableId: Long): EntityID<Long> {
        val userId = withUser(userUuid)
        val communityId = Tables.Community.select(Tables.Community.id)
            .where(Tables.Community.durableId eq communityDurableId)
            .first()[Tables.Community.id]
        Tables.CommunityUserJoin.insertIgnore {
            it[user] = userId
            it[community] = communityId
        }
        return communityId
    }

    fun experiences(userUuid: Uuid, communityId: Long): List<Experience> {
        val userId = withUser(userUuid)
        val experienceRows =
            (Tables.Community innerJoin Tables.Experience innerJoin Tables.CommunityUserJoin leftJoin Tables.ExperienceGameJoin)
                .join(
                    otherTable = Tables.Game,
                    joinType = JoinType.LEFT,
                    onColumn = Tables.ExperienceGameJoin.game,
                    otherColumn = Tables.Game.id,
                )
                .selectAll()
                .where { Tables.CommunityUserJoin.user eq userId }
                .andWhere { Tables.Community.id eq communityId }
                .orderBy(
                    Tables.Experience.index to SortOrder.ASC,
                )
                .toList()

        if (experienceRows.isEmpty()) return emptyList()

        val gameIds = experienceRows
            .mapNotNull { it.getOrNull(Tables.Game.id) }
            .distinct()
        val bannerIds = experienceRows
            .map { it[Tables.Experience.banner] }
            .distinct()

        val bannersById = Tables.Sprite
            .selectAll()
            .where { Tables.Sprite.id inList bannerIds }
            .associate { row -> row[Tables.Sprite.id].value to row.toSprite() }

        val objectsByGame = if (gameIds.isEmpty()) {
            emptyMap()
        } else {
            (Tables.Obj innerJoin Tables.Sprite)
                .selectAll()
                .where { Tables.Obj.game inList gameIds }
                .orderBy(Tables.Obj.game to SortOrder.ASC, Tables.Obj.index to SortOrder.ASC)
                .map { row ->
                    row[Tables.Obj.game].value to Obj(
                        offset = Offset(row[Tables.Obj.xOffset], row[Tables.Obj.yOffset]),
                        sprite = row.toSprite(),
                        tag = row[Tables.Obj.tag] ?: row[Tables.Obj.index],
                    )
                }
                .groupBy({ it.first }, { it.second })
        }

        val triggers = if (gameIds.isEmpty()) {
            emptyList()
        } else {
            Tables.Trigger
                .selectAll()
                .where { Tables.Trigger.game inList gameIds }
                .orderBy(
                    Tables.Trigger.game to SortOrder.ASC,
                    Tables.Trigger.index to SortOrder.ASC,
                )
                .map { row ->
                    StoredTrigger(
                        id = row[Tables.Trigger.id],
                        gameId = row[Tables.Trigger.game].value,
                        trigger = row.toTrigger(),
                    )
                }
        }
        val actionsByTrigger = if (triggers.isEmpty()) {
            emptyMap()
        } else {
            Tables.Action
                .selectAll()
                .where { Tables.Action.trigger inList triggers.map { it.id } }
                .orderBy(
                    Tables.Action.trigger to SortOrder.ASC,
                    Tables.Action.index to SortOrder.ASC,
                )
                .map { row -> row[Tables.Action.trigger] to row.toAction() }
                .groupBy({ it.first }, { it.second })
        }
        val logicsByGame = triggers
            .groupBy { it.gameId }
            .mapValues { (_, gameTriggers) ->
                gameTriggers.map { trigger ->
                    Logic(trigger.trigger, actionsByTrigger[trigger.id].orEmpty())
                }
            }

        val gamesById = gameIds.associate { gameId ->
            gameId.value to Game(
                id = gameId.value,
                world = World(objectsByGame[gameId.value].orEmpty()),
                logics = logicsByGame[gameId.value].orEmpty(),
            )
        }

        return experienceRows
            .groupBy { it[Tables.Experience.id].value }
            .map { (experienceId, rows) ->
                val firstRow = rows.first()
                Experience(
                    id = experienceId,
                    banner = bannersById[firstRow[Tables.Experience.banner].value]
                        ?: Sprite(10, 10, ByteArray(100)),
                    games = rows
                        .mapNotNull { row -> row.getOrNull(Tables.Game.id) }
                        .distinct()
                        .mapNotNull { gamesById[it.value] },
                )
            }
    }

    fun getSprite(spriteId: EntityID<Long>): Sprite = Tables.Sprite
        .selectAll()
        .where { Tables.Sprite.id eq spriteId }
        .map { it.toSprite() }
        .firstOrNull()
        ?: Sprite(10, 10, ByteArray(100))

    fun publishGame(uuid: Uuid, experienceId: Long, game: Game): EntityID<Long> {
        val userId = withUser(uuid)
        val isUserMember =
            !(Tables.Experience innerJoin Tables.Community innerJoin Tables.CommunityUserJoin)
                .select(Tables.CommunityUserJoin.user)
                .where { Tables.CommunityUserJoin.user eq userId }
                .andWhere { Tables.Experience.id eq experienceId }
                .empty()
        require(isUserMember)
        val gameId = writeGame(userId, game)
        Tables.ExperienceGameJoin.insertIgnore {
            it[experience] = experienceId
            it[this.game] = gameId
        }
        return gameId
    }

    fun publishedAt(uuid: Uuid, gameId: Long): List<EntityID<Long>> {
        val userId = withUser(uuid)
        val owner = Tables.Game
            .select(Tables.Game.owner)
            .where(Tables.Game.id eq gameId)
            .firstOrNull()?.get(Tables.Game.owner)
        if (owner != userId) return listOf()
        return Tables.ExperienceGameJoin
            .select(Tables.ExperienceGameJoin.experience)
            .where(Tables.ExperienceGameJoin.game eq gameId)
            .map { it[Tables.ExperienceGameJoin.experience] }
    }

    fun getExperienceMetadatas(uuid: Uuid, communityId: Long): List<ExperienceMetadata> {
        val userId = withUser(uuid)
        val isUserMember = !Tables.CommunityUserJoin
            .select(Tables.CommunityUserJoin.user)
            .where { Tables.CommunityUserJoin.user eq userId }
            .andWhere { Tables.CommunityUserJoin.community eq communityId }
            .empty()
        require(isUserMember)
        return (Tables.Community innerJoin Tables.CommunityUserJoin innerJoin Tables.Experience)
            .join(
                onColumn = Tables.Experience.banner,
                otherTable = Tables.Sprite,
                otherColumn = Tables.Sprite.id,
                joinType = JoinType.INNER,
            )
            .select(
                Tables.Experience.id,
                Tables.Experience.community,
                Tables.Sprite.pixels,
                Tables.Sprite.width,
                Tables.Sprite.height,
            )
            .where(Tables.Community.id eq communityId)
            .andWhere { Tables.CommunityUserJoin.user eq userId }
            .orderBy(Tables.Experience.index to SortOrder.ASC)
            .map {
                ExperienceMetadata(
                    id = it[Tables.Experience.id].value,
                    banner = it.toSprite(),
                )
            }
    }

    fun saveExperienceMetadata(
        uuid: Uuid,
        metadata: SaveExperienceMetadata,
    ): EntityID<Long> {
        val userId = withUser(uuid)
        val communityId = EntityID(metadata.communityId, Tables.Community)
        val isCommunityMember = !Tables.CommunityUserJoin
            .select(Tables.CommunityUserJoin.user)
            .where { Tables.CommunityUserJoin.user eq userId }
            .andWhere { Tables.CommunityUserJoin.community eq communityId }
            .empty()
        require(isCommunityMember)

        val newBanner = writeSprite(metadata.banner)
        val experienceId = metadata.id?.let { id ->
            val oldBanner = Tables.Experience
                .select(Tables.Experience.banner)
                .where { (Tables.Experience.id eq id) and (Tables.Experience.community eq communityId) }
                .first()[Tables.Experience.banner]
            Tables.Experience.update(
                where = { (Tables.Experience.id eq id) and (Tables.Experience.community eq communityId) },
            ) {
                it[banner] = newBanner
            }
            Tables.Sprite.deleteWhere { Tables.Sprite.id eq oldBanner }
            EntityID(id, Tables.Experience)
        } ?: run {
            val nextIndex = Tables.Experience
                .select(Tables.Experience.index)
                .where { Tables.Experience.community eq communityId }
                .orderBy(Tables.Experience.index to SortOrder.DESC)
                .firstOrNull()
                ?.get(Tables.Experience.index)
                ?.plus(1)
                ?: 0
            Tables.Experience.insertAndGetId {
                it[community] = communityId
                it[index] = nextIndex
                it[banner] = newBanner
            }
        }
        return experienceId
    }

    fun publishableExperiences(uuid: Uuid): List<PublishableExperience> {
        val communities = getCommunityMetadata(uuid)
        val experiencesByCommunityId = (Tables.Community innerJoin Tables.Experience)
            .join(
                onColumn = Tables.Experience.banner,
                otherTable = Tables.Sprite,
                otherColumn = Tables.Sprite.id,
                joinType = JoinType.INNER,
            )
            .select(
                Tables.Experience.id,
                Tables.Experience.community,
                Tables.Sprite.pixels,
                Tables.Sprite.width,
                Tables.Sprite.height,
            )
            .where(Tables.Community.id inList communities.map { it.id })
            .groupBy { it[Tables.Experience.community].value }
            .mapValues { entries ->
                entries.value.map {
                    ExperienceMetadata(id = it[Tables.Experience.id].value, banner = it.toSprite())
                }
            }
        return communities
            .map {
                PublishableExperience(
                    it,
                    experiencesByCommunityId[it.id] ?: listOf(),
                )
            }
            .filter { it.experiences.isNotEmpty() }
    }

    fun unpublishGame(uuid: Uuid, experienceId: Long, gameId: Long) {
        val userId = withUser(uuid)
        val owner = Tables.Game
            .select(Tables.Game.owner)
            .where(Tables.Game.id eq gameId)
            .first()[Tables.Game.owner]
        require(owner == userId)
        Tables.ExperienceGameJoin.deleteWhere { (game eq gameId) and (experience eq experienceId) }
    }

    private fun writeSprite(sprite: Sprite): EntityID<Long> = Tables.Sprite.insertAndGetId {
        it[width] = sprite.width
        it[height] = sprite.height
        it[pixels] = sprite.pixels
    }

    private fun writeGame(userId: EntityID<Long>, game: Game): EntityID<Long> {
        val gameId = game.id
        if (gameId != null) {
            val dbGame = Tables.Game.select(Tables.Game.owner)
                .where(Tables.Game.id.eq(gameId))
                .firstOrNull()
            if (dbGame != null && userId == dbGame[Tables.Game.owner]) {
                Tables.Obj.deleteWhere { this.game eq gameId }
                Tables.Trigger.deleteWhere { this.game eq gameId }
                val gameEntityId = EntityID(gameId, Tables.Game)
                game.world.objects.forEachIndexed { i, obj -> writeObj(gameEntityId, i, obj) }
                game.logics.forEachIndexed { i, logic -> writeLogic(gameEntityId, i, logic) }
                return gameEntityId
            }
        }
        return createGame(userId, game)
    }

    private fun createGame(userId: EntityID<Long>, game: Game): EntityID<Long> {
        val gameId = Tables.Game.insertAndGetId {
            it[owner] = userId
        }
        game.world.objects.forEachIndexed { i, obj -> writeObj(gameId, i, obj) }
        game.logics.forEachIndexed { i, logic -> writeLogic(gameId, i, logic) }
        return gameId
    }

    private fun writeObj(gameId: EntityID<Long>, i: Int, obj: Obj) = Tables.Obj.insert {
        it[game] = gameId
        it[index] = i
        it[sprite] = writeSprite(obj.sprite)
        it[xOffset] = obj.offset.x
        it[yOffset] = obj.offset.y
        it[tag] = obj.tag
    }

    private fun writeLogic(gameId: EntityID<Long>, i: Int, logic: Logic) {
        val triggerId = Tables.Trigger.insertAndGetId {
            it[game] = gameId
            it[index] = i
            it[variant] = when (val trigger = logic.trigger) {
                Trigger.Start -> 0
                is Trigger.Tap -> {
                    it[tapTarget] = trigger.target.tag
                    1
                }

                is Trigger.Collision -> {
                    it[collisionFirstTarget] = trigger.firstTarget.tag
                    it[collisionSecondTarget] = trigger.secondTarget.tag
                    2
                }
            }
        }
        logic.actions.forEachIndexed { actionIndex, action ->
            Tables.Action.insert {
                it[trigger] = triggerId
                it[index] = actionIndex
                it[variant] = when (action) {
                    Action.Win -> 0
                    Action.Lose -> 1
                    is Action.Move -> {
                        it[moveMover] = action.mover.tag
                        it[moveTarget] = action.target.ordinal
                        it[moveSpeed] = action.speed
                        2
                    }

                    is Action.MoveTo -> {
                        it[moveToMover] = action.mover.tag
                        it[moveToTarget] = action.target.tag
                        it[moveToSpeed] = action.speed
                        3
                    }

                    is Action.MoveWithin -> {
                        it[moveWithinMover] = action.mover.tag
                        it[moveWithinTarget] = action.target.tag
                        it[moveWithinSpeed] = action.speed
                        4
                    }
                }
            }
        }
    }

    private fun isUser(uuid: Uuid): Op<Boolean> = uuid.toLongs { upper, lower ->
        (Tables.User.upper eq upper) and (Tables.User.lower eq lower)
    }
}

private data class StoredTrigger(
    val id: EntityID<Long>,
    val gameId: Long,
    val trigger: Trigger,
)

private fun ResultRow.toSprite(table: Alias<Tables.Sprite>) = Sprite(
    width = this[table[Tables.Sprite.width]],
    height = this[table[Tables.Sprite.height]],
    pixels = this[table[Tables.Sprite.pixels]],
)

private fun ResultRow.toSprite() = Sprite(
    width = this[Tables.Sprite.width],
    height = this[Tables.Sprite.height],
    pixels = this[Tables.Sprite.pixels],
)

private fun ResultRow.toTrigger(): Trigger = when (this[Tables.Trigger.variant]) {
    0 -> Trigger.Start
    1 -> Trigger.Tap(LogicTag(this[Tables.Trigger.tapTarget]!!))
    2 -> Trigger.Collision(
        firstTarget = LogicTag(this[Tables.Trigger.collisionFirstTarget]!!),
        secondTarget = LogicTag(this[Tables.Trigger.collisionSecondTarget]!!),
    )

    else -> error("Unknown trigger variant")
}

private fun ResultRow.toAction(): Action = when (this[Tables.Action.variant]) {
    0 -> Action.Win
    1 -> Action.Lose
    2 -> Action.Move(
        mover = LogicTag(this[Tables.Action.moveMover]!!),
        target = Direction.entries[this[Tables.Action.moveTarget]!!],
        speed = this[Tables.Action.moveSpeed]!!,
    )

    3 -> Action.MoveTo(
        mover = LogicTag(this[Tables.Action.moveToMover]!!),
        target = LogicTag(this[Tables.Action.moveToTarget]!!),
        speed = this[Tables.Action.moveToSpeed],
    )

    4 -> Action.MoveWithin(
        mover = LogicTag(this[Tables.Action.moveWithinMover]!!),
        target = LogicTag(this[Tables.Action.moveWithinTarget]!!),
        speed = this[Tables.Action.moveWithinSpeed],
    )

    else -> error("Unknown action variant")
}
