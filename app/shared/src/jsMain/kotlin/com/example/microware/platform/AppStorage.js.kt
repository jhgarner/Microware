package com.example.microware.platform

import kotlinx.browser.localStorage

actual fun initializeAppStorage(context: Any?) = Unit

actual object AppStorage {
    actual fun read(key: String): String? = localStorage.getItem(key)

    actual fun write(key: String, value: String) {
        localStorage.setItem(key, value)
    }
}
