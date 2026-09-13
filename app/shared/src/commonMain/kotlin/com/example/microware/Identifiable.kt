package com.example.microware

import kotlinx.collections.immutable.PersistentList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

interface Identifiable<Self : Identifiable<Self>> {
    val self: Self
    val id: Int
    fun newId(id: Int): Self
    fun newId(): Self = newId(nextId())

    @OptIn(ExperimentalAtomicApi::class)
    companion object {
        private val nextId = AtomicInt(0)

        fun nextId(): Int = nextId.fetchAndAdd(1);
    }
}

fun <T : Identifiable<T>> PersistentList<T>.upserting(item: T): PersistentList<T> {
    val i = indexOfFirst { it.id == item.id }.takeIf { it >= 0 }
    return if (i != null) {
        replacingAt(i, item)
    } else {
        adding(item)
    }
}
