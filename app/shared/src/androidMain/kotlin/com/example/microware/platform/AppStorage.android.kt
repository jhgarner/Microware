package com.example.microware.platform

import android.content.Context

private var applicationContext: Context? = null

actual fun initializeAppStorage(context: Any?) {
    applicationContext = (context as? Context)?.applicationContext
}

actual object AppStorage {
    private val preferences
        get() = applicationContext?.getSharedPreferences(
            "microware",
            Context.MODE_PRIVATE
        )

    actual fun read(key: String): String? = preferences?.getString(key, null)

    actual fun write(key: String, value: String) {
        preferences?.edit()?.putString(key, value)?.apply()
    }
}
