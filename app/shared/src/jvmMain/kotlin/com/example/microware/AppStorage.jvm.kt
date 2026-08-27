package com.example.microware

import java.util.prefs.Preferences

actual fun initializeAppStorage(context: Any?) = Unit

actual object AppStorage {
    private val preferences = Preferences.userRoot().node("com.example.microware")

    actual fun read(key: String): String? = preferences.get(key, null)

    actual fun write(key: String, value: String) {
        preferences.put(key, value)
    }
}
