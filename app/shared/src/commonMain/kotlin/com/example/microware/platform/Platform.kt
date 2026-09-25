package com.example.microware.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform