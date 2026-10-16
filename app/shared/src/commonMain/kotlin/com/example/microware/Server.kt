package com.example.microware

import com.example.microware.community.DurableId
import com.example.microware.core.Model
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.path

private val URL = Url("localhost:8040")

private val user: User = User.loadOrCreate()

private val client = HttpClient()

suspend fun getCommunityMetadata(): List<Model.CommunityMetadata> = client.get {
    url { this.host = "localhost"; this.port = 8040; this.path("communityMetadata") }
    this.header("userUuid", user)
}.body()

suspend fun joinCommunity(id: DurableId): HttpStatusCode = client.post {
    url { this.host = "localhost"; this.port = 8040; this.path("joinCommunity") }
    this.header("userUuid", user)
}.status
