package com.example.microware.server

import com.example.microware.core.Model
import com.example.microware.core.Model.SaveExperienceMetadata
import com.example.microware.core.Model.Sprite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
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
}
