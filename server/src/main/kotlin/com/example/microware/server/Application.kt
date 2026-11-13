package com.example.microware.server

import com.example.microware.APP_VERSION
import com.example.microware.core.Model
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.query
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

fun main() {
    prepareDb()
    embeddedServer(Netty, port = 8040, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

private fun prepareDb() {
    Database.connect(
        "jdbc:h2:/var/db/microware/microwaredb;MODE=MYSQL",
        driver = "org.h2.Driver",
    )
    transaction {
        SchemaUtils.create(Tables.Sprite)
        SchemaUtils.create(Tables.User)
        SchemaUtils.create(Tables.Community)
        SchemaUtils.create(Tables.CommunityUserJoin)
        SchemaUtils.create(Tables.Experience)
        SchemaUtils.create(Tables.ExperienceGameJoin)
        SchemaUtils.create(Tables.Game)
        SchemaUtils.create(Tables.Trigger)
        SchemaUtils.create(Tables.Action)
        SchemaUtils.create(Tables.Obj)
    }
    migrateExperienceFlags()
    migrateTagSets()
    migrateAddedColumns()
}

internal fun migrateExperienceFlags() {
    transaction {
        exec("ALTER TABLE EXPERIENCE ADD COLUMN IF NOT EXISTS PLAYABLE BOOLEAN DEFAULT TRUE NOT NULL")
        exec("ALTER TABLE EXPERIENCE ADD COLUMN IF NOT EXISTS PUBLISHABLE BOOLEAN DEFAULT TRUE NOT NULL")
    }
}

private val legacyTagColumns = mapOf(
    Tables.Obj.tags to "tag",
    Tables.Trigger.tapTargetTags to "tapTarget",
    Tables.Trigger.collisionFirstTargetTags to "collisionFirstTarget",
    Tables.Trigger.collisionSecondTargetTags to "collisionSecondTarget",
    Tables.Action.moveMoverTags to "moveMover",
    Tables.Action.moveToMoverTags to "moveToMover",
    Tables.Action.moveToTargetTags to "moveToTarget",
    Tables.Action.moveWithinMoverTags to "moveWithinMover",
    Tables.Action.moveWithinTargetTags to "moveWithinTarget",
)

/**
 * Objects, triggers and actions used to reference a single tag each. Databases that predate tag
 * sets gain the tag set columns, seeded from the single tag columns. Those are left in place but
 * are no longer read or written.
 */
internal fun migrateTagSets() {
    transaction {
        for ((tagSet, legacyName) in legacyTagColumns) {
            if (hasColumn(tagSet.table, tagSet.name)) continue

            tagSet.createStatement().forEach { exec(it) }
            if (hasColumn(tagSet.table, legacyName)) {
                val legacyTag = db.identifierManager.quoteIdentifierWhenWrongCaseOrNecessary(legacyName)
                exec(
                    """
                    UPDATE ${identity(tagSet.table)} SET ${identity(tagSet)} = ARRAY[$legacyTag]
                    WHERE $legacyTag IS NOT NULL
                    """.trimIndent(),
                )
            }
        }
    }
}

private val addedColumns = listOf(
    Tables.Trigger.collisionFirstTargetCount,
    Tables.Action.moveMoverParameter,
    Tables.Action.moveToMoverParameter,
    Tables.Action.moveToTargetParameter,
    Tables.Action.moveWithinMoverParameter,
    Tables.Action.moveWithinTargetParameter,
)

/**
 * Databases that predate collision counts and action parameters gain their columns. Existing
 * collisions need one object and existing actions keep selecting by tag.
 */
internal fun migrateAddedColumns() {
    transaction {
        addedColumns
            .filterNot { hasColumn(it.table, it.name) }
            .flatMap { it.createStatement() }
            .forEach { exec(it) }
    }
}

private fun JdbcTransaction.hasColumn(table: Table, name: String): Boolean = exec(
    """
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE UPPER(TABLE_NAME) = UPPER('${table.tableName}') AND UPPER(COLUMN_NAME) = UPPER('$name')
    """.trimIndent(),
) { it.next() } == true

fun Application.module() {
    install(ContentNegotiation) { json() }
    routing {
        query("/version") {
            call.respond(APP_VERSION)
        }
        post("/publish") {
            val uuid = withUser()
            val publish = call.receive<Model.Publish>()
            val gameId = transaction {
                Dao.publishGame(uuid, publish.experience, publish.game)
            }
            call.respond(gameId.value)
        }
        delete("/publish") {
            val uuid = withUser()
            val unpublish = call.receive<Model.Unpublish>()
            transaction {
                Dao.unpublishGame(uuid, unpublish.experience, unpublish.game)
            }
        }
        query("/publishedAt") {
            val uuid = withUser()
            val gameId = call.receive<Long>()
            val publishedAt = transaction {
                Dao.publishedAt(uuid, gameId)
            }.map { it.value }
            call.respond(publishedAt)
        }
        query("/publishableExperiences") {
            val uuid = withUser()
            val publishableExperiences = transaction {
                Dao.publishableExperiences(uuid)
            }
            call.respond(publishableExperiences)
        }
        query("/experienceMetadatas") {
            val uuid = withUser()
            val communityId = call.receive<Long>()
            val experienceMetadatas = transaction {
                Dao.getExperienceMetadatas(uuid, communityId)
            }
            call.respond(experienceMetadatas)
        }
        query("/experiences") {
            val uuid = withUser()
            val communityId = call.receive<Long>()
            val experiences = transaction {
                Dao.experiences(uuid, communityId)
            }
            call.respond(experiences)
        }
        post("/experienceMetadata") {
            val uuid = withUser()
            val metadata = call.receive<Model.SaveExperienceMetadata>()
            val experienceId = transaction {
                Dao.saveExperienceMetadata(uuid, metadata)
            }
            call.respond(experienceId.value)
        }
        query("/banners") {
            val uuid = withUser()
            val banners = transaction {
                Dao.getBanners(uuid)
            }
            call.respond(banners)
        }
        query("/communityMetadata") {
            val uuid = withUser()
            val metadata = transaction {
                Dao.getCommunityMetadata(uuid)
            }
            call.respond(metadata)
        }
        post("/createCommunity") {
            val uuid = withUser()
            val communityId = transaction {
                Dao.createCommunity(uuid)
            }
            call.respond(communityId.value)
        }
        this.patch("/updateCommunity") {
            val uuid = withUser()
            val metadata = call.receive<Model.CommunityMetadata>()
            transaction {
                Dao.updateCommunityAvatar(uuid, metadata.id, metadata.icon)
                Dao.updateCommunityBanner(uuid, metadata.id, metadata.banner)
                Dao.updateDurableId(uuid, metadata.id, metadata.code)
            }
            call.respond(HttpStatusCode.OK, Unit)
        }

        post("/joinCommunity") {
            val user = withUser()
            val communityDurableId = call.receive<Long>()
            transaction {
                Dao.joinCommunity(user, communityDurableId)
            }
            call.respond(HttpStatusCode.OK, Unit)
        }
    }
}

private suspend fun RoutingContext.withUser(): Uuid {
    val uuid = call.request.header("userUuid") ?: throw Exception("No user provided")
    return Uuid.parseHex(uuid)
}
