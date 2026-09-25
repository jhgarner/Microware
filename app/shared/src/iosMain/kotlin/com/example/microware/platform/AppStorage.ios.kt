package com.example.microware.platform

import platform.Foundation.NSUserDefaults

actual fun initializeAppStorage(context: Any?) = Unit

actual object AppStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun read(key: String): String? = defaults.stringForKey(key)

    actual fun write(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
