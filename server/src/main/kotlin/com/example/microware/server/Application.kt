package com.example.microware.server

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
import io.ktor.server.response.respondText
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.query
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

fun main() {
    prepareDb()
    embeddedServer(Netty, port = 8040, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

private fun prepareDb() {
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
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

}

fun Application.module() {
    install(ContentNegotiation) { json() }
    routing {
        query("/communityMetadata") {
            val userUuidString = call.request.header("userUuid")
            if (userUuidString == null) {
                call.respondText("No user provided")
                return@query
            }
            val metadata = transaction {
                Dao.getCommunityMetadata(Uuid.parseHex(userUuidString))
            }
            call.respond(metadata)
        }
        post("/createCommunity") {
            val userUuidString = call.request.header("userUuid")
            if (userUuidString == null) {
                call.respondText("No user provided")
                return@post
            }
            val communityId = transaction {
                Dao.createCommunity(Uuid.parseHex(userUuidString))
            }
            call.respond(communityId.value)
        }
        this.patch("/updateCommunity") {
            val userUuidString = call.request.header("userUuid")
            if (userUuidString == null) {
                call.respondText("No user provided")
                return@patch
            }
            val metadata = call.receive<Model.CommunityMetadata>()
            val uuid = Uuid.parseHex(userUuidString)
            transaction {
                Dao.updateCommunityAvatar(uuid, metadata.id, metadata.icon)
                Dao.updateCommunityBanner(uuid, metadata.id, metadata.banner)
                Dao.updateDurableId(uuid, metadata.id, metadata.code)
            }
            call.respond(HttpStatusCode.OK, Unit)
        }

        post("/joinCommunity") {
            val userUuidString = call.request.header("userUuid")
            if (userUuidString == null) {
                call.respondText("No user provided")
                return@post
            }
            val communityDurableId = call.receive<Long>()
            transaction {
                Dao.joinCommunity(Uuid.parseHex(userUuidString), communityDurableId)
            }
            call.respond(HttpStatusCode.OK, Unit)
        }
    }
}