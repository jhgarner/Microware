package com.example.microware.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Loaded<out T>(val value: T) : Loadable<T>
}

fun <T> Loadable<T>.getOrElse(default: T): T = when (this) {
    Loadable.Loading -> default
    is Loadable.Loaded -> value
}

inline fun <T, R> Loadable<T>.map(transform: (T) -> R): Loadable<R> = when (this) {
    Loadable.Loading -> Loadable.Loading
    is Loadable.Loaded -> Loadable.Loaded(transform(value))
}

/** Starts as [Loadable.Loading] and keeps the last loaded value while reloading for new [keys]. */
@Composable
fun <T> produceLoadable(vararg keys: Any?, load: suspend () -> T): Loadable<T> =
    produceState<Loadable<T>>(Loadable.Loading, *keys) { value = Loadable.Loaded(load()) }.value
