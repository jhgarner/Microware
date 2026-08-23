package com.example.microware

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform