package com.example.microware.server

import com.example.microware.core.Model
import com.example.microware.core.Model.SaveExperienceMetadata
import com.example.microware.core.Model.Sprite
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Parameter
import com.example.microware.game.model.TagFilter
import com.example.microware.game.model.Trigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class DaoTest {
    @Test
    fun experienceFlagsArePersistedAndControlPlayAndPublishing() {
        connectToDatabase()
        transaction {
            SchemaUtils.create(
                Tables.Sprite,
                Tables.User,
                Tables.Community,
                Tables.CommunityUserJoin,
                Tables.Experience,
                Tables.ExperienceGameJoin,
                Tables.Game,
                Tables.Trigger,
                Tables.Action,
                Tables.Obj,
            )

            val user = Uuid.random()
            val communityId = Dao.createCommunity(user).value
            val defaultFlags = Dao.saveExperienceMetadata(
                user,
                SaveExperienceMetadata(null, communityId, banner()),
            ).value
            val unavailableForPlay = Dao.saveExperienceMetadata(
                user,
                SaveExperienceMetadata(
                    null,
                    communityId,
                    banner(),
                    playable = false,
                    publishable = true,
                ),
            ).value
            val unavailableForPublish = Dao.saveExperienceMetadata(
                user,
                SaveExperienceMetadata(
                    null,
                    communityId,
                    banner(),
                    playable = true,
                    publishable = false,
                ),
            ).value

            val metadata = Dao.getExperienceMetadatas(user, communityId).associateBy { it.id }
            assertTrue(metadata.getValue(defaultFlags).playable)
            assertTrue(metadata.getValue(defaultFlags).publishable)
            assertFalse(metadata.getValue(unavailableForPlay).playable)
            assertTrue(metadata.getValue(unavailableForPlay).publishable)
            assertTrue(metadata.getValue(unavailableForPublish).playable)
            assertFalse(metadata.getValue(unavailableForPublish).publishable)

            assertEquals(
                listOf(defaultFlags, unavailableForPublish),
                Dao.experiences(user, communityId).map { it.id },
            )
            assertEquals(
                setOf(defaultFlags, unavailableForPlay),
                Dao.publishableExperiences(user).flatMap { it.experiences }.map { it.id }.toSet(),
            )
            assertFailsWith<IllegalArgumentException> {
                Dao.publishGame(user, unavailableForPublish, emptyGame())
            }

            val gameId = Dao.publishGame(user, unavailableForPlay, emptyGame()).value
            Dao.saveExperienceMetadata(
                user,
                SaveExperienceMetadata(
                    unavailableForPlay,
                    communityId,
                    banner(),
                    playable = false,
                    publishable = false,
                ),
            )
            Dao.unpublishGame(user, unavailableForPlay, gameId)
            assertTrue(Dao.publishedAt(user, gameId).isEmpty())
            assertEquals(setOf(defaultFlags), Dao.publishableExperiences(user)
                .flatMap { it.experiences }
                .map { it.id }
                .toSet())
        }
    }

    @Test
    fun existingExperienceRowsGainEnabledFlagsDuringMigration() {
        connectToDatabase()
        transaction {
            SchemaUtils.create(Tables.Sprite, Tables.User, Tables.Community, Tables.CommunityUserJoin)

            val user = Uuid.random()
            val communityId = Dao.createCommunity(user).value
            val bannerId = Tables.Community
                .select(Tables.Community.banner)
                .where { Tables.Community.id eq communityId }
                .first()[Tables.Community.banner]

            SchemaUtils.create(LegacyExperience)
            LegacyExperience.insert {
                it[community] = communityId
                it[banner] = bannerId.value
                it[index] = 0
            }

            migrateExperienceFlags()
            migrateExperienceFlags()
            val migrated = Tables.Experience.select(Tables.Experience.playable, Tables.Experience.publishable).first()
            assertTrue(migrated[Tables.Experience.playable])
            assertTrue(migrated[Tables.Experience.publishable])

            Tables.Experience.update {
                it[playable] = false
                it[publishable] = false
            }
            migrateExperienceFlags()
            val afterRestart = Tables.Experience.select(Tables.Experience.playable, Tables.Experience.publishable).first()
            assertFalse(afterRestart[Tables.Experience.playable])
            assertFalse(afterRestart[Tables.Experience.publishable])
        }
    }

    @Test
    fun gamesKeepEveryTagOfTheirObjectsAndFilters() {
        connectToDatabase()
        transaction {
            SchemaUtils.create(*gameTables)
            val user = Uuid.random()
            val communityId = Dao.createCommunity(user).value
            val experienceId = Dao.saveExperienceMetadata(
                user,
                SaveExperienceMetadata(null, communityId, banner()),
            ).value
            val both = TagFilter(LogicTag(1), LogicTag(2))
            val game = Model.Game(
                id = null,
                world = Model.World(
                    listOf(
                        Model.Obj(Model.Offset(0f, 0f), sprite(), tags = setOf(1, 2)),
                        Model.Obj(Model.Offset(4f, 0f), sprite()),
                    ),
                ),
                logics = listOf(
                    Model.Logic(
                        Trigger.Separation(both, TagFilter(LogicTag(3)), firstTargetCount = 4),
                        listOf(Action.MoveTo(both, Parameter.SecondTarget, speed = null)),
                    ),
                ),
            )

            Dao.publishGame(user, experienceId, game)

            val stored = Dao.experiences(user, communityId).single().games.single()
            assertEquals(listOf(setOf(1, 2), emptySet()), stored.world.objects.map { it.tags })
            assertEquals(game.logics.single().trigger, stored.logics.single().trigger)
            val action = stored.logics.single().actions.single() as Action.MoveTo
            assertEquals(both, action.mover)
            assertEquals(Parameter.SecondTarget, action.target)
        }
    }

    @Test
    fun gamesKeepTheirEmptyFilters() {
        connectToDatabase()
        transaction {
            SchemaUtils.create(*gameTables)
            val user = Uuid.random()
            val communityId = Dao.createCommunity(user).value
            val experienceId = Dao.saveExperienceMetadata(
                user,
                SaveExperienceMetadata(null, communityId, banner()),
            ).value
            val game = Model.Game(
                id = null,
                world = Model.World(emptyList()),
                logics = listOf(
                    Model.Logic(
                        Trigger.Collision(TagFilter.Empty, tagFilter(1)),
                        listOf(
                            Action.MoveTo(TagFilter.Empty, TagFilter.Empty, speed = null),
                            Action.Move(TagFilter.Empty, Direction.N, speed = 3),
                        ),
                    ),
                ),
            )

            Dao.publishGame(user, experienceId, game)

            val stored = Dao.experiences(user, communityId).single().games.single().logics.single()
            assertEquals(game.logics.single().trigger, stored.trigger)
            val moveTo = stored.actions[0] as Action.MoveTo
            assertEquals(TagFilter.Empty, moveTo.mover)
            assertEquals(TagFilter.Empty, moveTo.target)
            assertEquals(TagFilter.Empty, (stored.actions[1] as Action.Move).mover)
        }
    }

    @Test
    fun singleTagRowsBecomeOneTagSetsDuringMigration() {
        connectToDatabase()
        transaction {
            SchemaUtils.create(
                Tables.Sprite,
                Tables.User,
                Tables.Community,
                Tables.CommunityUserJoin,
                Tables.Experience,
                Tables.ExperienceGameJoin,
                Tables.Game,
                LegacyObj,
                LegacyTrigger,
                LegacyAction,
            )
            val user = Uuid.random()
            val communityId = Dao.createCommunity(user).value
            val experienceId = Dao.saveExperienceMetadata(
                user,
                SaveExperienceMetadata(null, communityId, banner()),
            ).value
            val gameId = Dao.publishGame(user, experienceId, emptyGame()).value
            val spriteId = Tables.Sprite.select(Tables.Sprite.id).first()[Tables.Sprite.id].value
            listOf(5, null).forEachIndexed { objectIndex, legacyTag ->
                LegacyObj.insert {
                    it[game] = gameId
                    it[index] = objectIndex
                    it[sprite] = spriteId
                    it[xOffset] = 0f
                    it[yOffset] = 0f
                    it[tag] = legacyTag
                }
            }
            val collisionId = LegacyTrigger.insertAndGetId {
                it[game] = gameId
                it[index] = 0
                it[variant] = 2
                it[collisionFirstTarget] = 5
                it[collisionSecondTarget] = 6
            }
            LegacyTrigger.insert {
                it[game] = gameId
                it[index] = 1
                it[variant] = 3
                it[tapTarget] = 7
            }
            LegacyAction.insert {
                it[trigger] = collisionId.value
                it[index] = 0
                it[variant] = 2
                it[moveMover] = 5
                it[moveTarget] = 1
                it[moveSpeed] = 9
            }
            LegacyAction.insert {
                it[trigger] = collisionId.value
                it[index] = 1
                it[variant] = 3
                it[moveToMover] = 5
                it[moveToTarget] = 6
            }
            LegacyAction.insert {
                it[trigger] = collisionId.value
                it[index] = 2
                it[variant] = 4
                it[moveWithinMover] = 6
                it[moveWithinTarget] = 5
            }

            migrateTagSets()
            migrateTagSets()
            migrateAddedColumns()
            migrateAddedColumns()

            val migrated = Dao.experiences(user, communityId).single().games.single()
            assertEquals(listOf(setOf(5), emptySet()), migrated.world.objects.map { it.tags })
            val (collision, press) = migrated.logics
            assertEquals(Trigger.Collision(tagFilter(5), tagFilter(6)), collision.trigger)
            assertEquals(Trigger.Press(tagFilter(7)), press.trigger)
            val (move, moveTo, moveWithin) = collision.actions
            assertEquals(tagFilter(5), (move as Action.Move).mover)
            assertEquals(tagFilter(5), (moveTo as Action.MoveTo).mover)
            assertEquals(tagFilter(6), moveTo.target)
            assertEquals(tagFilter(6), (moveWithin as Action.MoveWithin).mover)
            assertEquals(tagFilter(5), moveWithin.target)

            Dao.publishGame(
                user,
                experienceId,
                Model.Game(
                    gameId,
                    Model.World(listOf(Model.Obj(Model.Offset(0f, 0f), sprite(), setOf(1, 2)))),
                    emptyList(),
                ),
            )
            migrateTagSets()
            val republished = Dao.experiences(user, communityId).single().games.single()
            assertEquals(listOf(setOf(1, 2)), republished.world.objects.map { it.tags })
        }
    }

    private val gameTables = arrayOf(
        Tables.Sprite,
        Tables.User,
        Tables.Community,
        Tables.CommunityUserJoin,
        Tables.Experience,
        Tables.ExperienceGameJoin,
        Tables.Game,
        Tables.Trigger,
        Tables.Action,
        Tables.Obj,
    )

    private fun tagFilter(tag: Int) = TagFilter(LogicTag(tag))

    private fun sprite() = Sprite(1, 1, ByteArray(1))

    private fun connectToDatabase() {
        Database.connect(
            "jdbc:h2:mem:server-${Uuid.random()};DB_CLOSE_DELAY=-1;MODE=MYSQL",
            driver = "org.h2.Driver",
        )
    }

    private fun banner() = Sprite(128, 32, ByteArray(128 * 32))

    private fun emptyGame() = Model.Game(null, Model.World(emptyList()), emptyList())

    private object LegacyExperience : LongIdTable("Experience") {
        val banner = long("banner")
        val community = long("community")
        val index = integer("index")
    }

    private object LegacyObj : LongIdTable("Obj") {
        val game = long("game")
        val index = integer("index")
        val sprite = long("sprite")
        val xOffset = float("xOffset")
        val yOffset = float("yOffset")
        val tag = integer("tag").nullable()
    }

    private object LegacyTrigger : LongIdTable("Trigger") {
        val game = long("game")
        val index = integer("index")
        val variant = integer("variant")
        val tapTarget = integer("tapTarget").nullable()
        val collisionFirstTarget = integer("collisionFirstTarget").nullable()
        val collisionSecondTarget = integer("collisionSecondTarget").nullable()
    }

    private object LegacyAction : Table("Action") {
        val trigger = long("trigger")
        val index = integer("index")
        val variant = integer("variant")
        val moveMover = integer("moveMover").nullable()
        val moveTarget = integer("moveTarget").nullable()
        val moveSpeed = integer("moveSpeed").nullable()
        val moveToMover = integer("moveToMover").nullable()
        val moveToTarget = integer("moveToTarget").nullable()
        val moveToSpeed = integer("moveToSpeed").nullable()
        val moveWithinMover = integer("moveWithinMover").nullable()
        val moveWithinTarget = integer("moveWithinTarget").nullable()
        val moveWithinSpeed = integer("moveWithinSpeed").nullable()
    }
}
