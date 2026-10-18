package com.example.microware

import com.example.microware.community.Avatar
import com.example.microware.community.Banner
import com.example.microware.community.DurableId
import com.example.microware.core.Model
import com.example.microware.game.model.Game
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.query
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json

private val user: User = User.loadOrCreate()
private const val HOST = "jhgarner.tplinkdns.com"

private val client = HttpClient() {
    install(ContentNegotiation) {
        json()
    }
}

private fun URLBuilder.server(path: String) {
    protocol = URLProtocol.HTTPS
    host = HOST
    this.port = 6375
    path(path)
}

suspend fun getVersion(): String = client.query {
    url { server("version") }
}.body()

suspend fun getBanners(): List<Model.Sprite> = client.query {
    url { server("banners") }
    this.header("userUuid", user.uuid.toHexString())
}.body()

suspend fun getExperienceMetadatas(communityId: Long): List<Model.ExperienceMetadata> =
    client.query {
        url { server("experienceMetadatas") }
        this.header("userUuid", user.uuid.toHexString())
        contentType(ContentType.Application.Json)
        setBody(communityId)
    }.body()

suspend fun getExperiences(communityId: Long): List<Model.Experience> = client.query {
    url { server("experiences") }
    this.header("userUuid", user.uuid.toHexString())
    contentType(ContentType.Application.Json)
    setBody(communityId)
}.body()

suspend fun saveExperienceMetadata(
    id: Long?,
    communityId: Long,
    banner: Banner,
): Long = client.post {
    url { server("experienceMetadata") }
    this.header("userUuid", user.uuid.toHexString())
    contentType(ContentType.Application.Json)
    setBody(Model.SaveExperienceMetadata(id, communityId, banner.sprite.toModel()))
}.body()

suspend fun publishGame(experienceId: Long, game: Game): Long = client.post {
    url { server("publish") }
    this.header("userUuid", user.uuid.toHexString())
    contentType(ContentType.Application.Json)
    setBody(Model.Publish(game.toModel(), experienceId))
}.body()

suspend fun publishableExperiences(): List<Model.PublishableExperience> = client.query {
    url { server("publishableExperiences") }
    this.header("userUuid", user.uuid.toHexString())
}.body()

suspend fun publishedAt(gameId: Long): List<Long> = client.query {
    url { server("publishedAt") }
    this.header("userUuid", user.uuid.toHexString())
    contentType(ContentType.Application.Json)
    setBody(gameId)
}.body()

suspend fun unpublishGame(experienceId: Long, gameId: Long): HttpStatusCode = client.delete {
    url { server("publish") }
    this.header("userUuid", user.uuid.toHexString())
    contentType(ContentType.Application.Json)
    setBody(Model.Unpublish(gameId, experienceId))
}.status

suspend fun getCommunityMetadata(): List<Model.CommunityMetadata> = client.query {
    url { server("communityMetadata") }
    this.header("userUuid", user.uuid.toHexString())
}.body()

suspend fun joinCommunity(id: DurableId): HttpStatusCode = client.post {
    url { server("joinCommunity") }
    this.header("userUuid", user.uuid.toHexString())
    setBody(id.bits)
}.status

suspend fun createCommunity(): Long = client.post {
    url { server("createCommunity") }
    this.header("userUuid", user.uuid.toHexString())
}.body()

suspend fun updateCommunity(
    id: Long,
    code: DurableId,
    avatar: Avatar,
    banner: Banner,
): HttpStatusCode = client.patch {
    url { server("updateCommunity") }
    this.header("userUuid", user.uuid.toHexString())
    contentType(ContentType.Application.Json)
    setBody(
        Model.CommunityMetadata(
            id,
            code.bits,
            avatar.sprite.toModel(),
            banner.sprite.toModel(),
        ),
    )
}.status
