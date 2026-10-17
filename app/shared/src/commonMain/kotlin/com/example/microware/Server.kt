package com.example.microware

import com.example.microware.community.Avatar
import com.example.microware.community.Banner
import com.example.microware.community.DurableId
import com.example.microware.core.Model
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.query
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json

private val user: User = User.loadOrCreate()
private const val HOST = "10.0.2.2"

private val client = HttpClient() {
    install(ContentNegotiation) {
        json()
    }
}

suspend fun getCommunityMetadata(): List<Model.CommunityMetadata> = client.query {
    url { this.host = HOST; this.port = 8040; this.path("communityMetadata") }
    this.header("userUuid", user.uuid.toHexString())
}.body()

suspend fun joinCommunity(id: DurableId): HttpStatusCode = client.post {
    url { this.host = HOST; this.port = 8040; this.path("joinCommunity") }
    this.header("userUuid", user.uuid.toHexString())
    setBody(id.bits)
}.status

suspend fun createCommunity(): Long = client.post {
    url { this.host = HOST; this.port = 8040; this.path("createCommunity") }
    this.header("userUuid", user.uuid.toHexString())
}.body()

suspend fun updateCommunity(
    id: Long,
    code: DurableId,
    avatar: Avatar,
    banner: Banner,
): HttpStatusCode = client.patch {
    url { this.host = HOST; this.port = 8040; this.path("updateCommunity") }
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
