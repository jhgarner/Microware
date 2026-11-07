package com.example.microware.server

import com.example.microware.APP_VERSION
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun versionEndpoint() = testApplication {
        application { module() }

        val response = client.request("/version") { method = HttpMethod.Query }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(APP_VERSION, response.bodyAsText())
    }
}
