package com.example.microware.server

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
import io.ktor.server.routing.post
import io.ktor.server.routing.query
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.uuid.Uuid

fun main() {
    Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
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
            val metadata = Dao.getCommunityMetadata(Uuid.parseHex(userUuidString))
            call.respond(metadata)
        }
        post("/joinCommunity") {
            val userUuidString = call.request.header("userUuid")
            if (userUuidString == null) {
                call.respondText("No user provided")
                return@post
            }
            val communityDurableId = call.receive<Long>()
            try {
                Dao.joinCommunity(Uuid.parseHex(userUuidString), communityDurableId)
                call.respond(HttpStatusCode.OK)
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}