package com.example.microware

expect object AppStorage {
    fun read(key: String): String?
    fun write(key: String, value: String)
}

expect fun initializeAppStorage(context: Any? = null)
