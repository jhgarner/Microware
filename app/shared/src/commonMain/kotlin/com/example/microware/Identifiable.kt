package com.example.microware

import kotlinx.collections.immutable.PersistentList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.jvm.JvmInline

@JvmInline
value class Id(val value: Int)

interface Identifiable<Self : Identifiable<Self>> {
    val self: Self
    val id: Id
    fun newId(id: Id): Self
    fun newId(): Self = newId(nextId())

    @OptIn(ExperimentalAtomicApi::class)
    companion object {
        private val nextId = AtomicInt(0)

        fun nextId(): Id = Id(nextId.fetchAndAdd(1));
    }
}

fun <T : Identifiable<T>> List<T>.byId(id: Id): T? = firstOrNull { it.id == id }

fun <T : Identifiable<T>> PersistentList<T>.upserting(item: T): PersistentList<T> {
    val i = indexOfFirst { it.id == item.id }.takeIf { it >= 0 }
    return if (i != null) {
        replacingAt(i, item)
    } else {
        adding(item)
    }
}
