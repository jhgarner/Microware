package com.example.microware

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.microware.platform.AppStorage
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Is this remotely secure? No. It'll work for now though since I'm just testing with friends and
// the security of this app is really not important right now. A better solution would use PassKeys.
data class User(val uuid: Uuid) {
    @OptIn(ExperimentalUuidApi::class)
    constructor() : this(Uuid.generateV4())

    companion object {
        private const val STORAGE_KEY = "User"
        fun loadOrCreate(): User {
            val uuidStr = AppStorage.read(STORAGE_KEY)
            if (uuidStr == null) {
                val user = User()
                AppStorage.write(STORAGE_KEY, user.uuid.toHexString())
                return user
            } else {
                return User(Uuid.parseHex(uuidStr))
            }
        }
    }
}

val LocalUser: ProvidableCompositionLocal<User> =
    staticCompositionLocalOf { throw RuntimeException("No User set") }
