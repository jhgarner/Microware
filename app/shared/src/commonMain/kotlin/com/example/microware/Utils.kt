package com.example.microware

inline fun <reified Ex : Exception> untilThrows(cond: () -> Unit) {
    while (true) {
        try {
            cond()
        } catch (ex: Ex) {
            return
        }
    }
}