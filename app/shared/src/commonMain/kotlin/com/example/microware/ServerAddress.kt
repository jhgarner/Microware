package com.example.microware

import io.ktor.http.URLProtocol

enum class ServerAddress(val protocol: URLProtocol, val host: String, val port: Int) {
    Production(URLProtocol.HTTPS, "jhgarner.tplinkdns.com", 6375),
    Test(URLProtocol.HTTP, "10.100.0.1", 8040),
}

var serverAddress: ServerAddress = ServerAddress.Production
