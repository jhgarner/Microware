package com.example.microware.util

class BiMap<L, R> private constructor(
    private val lToR: MultiMap<L, R>,
    private val rToL: MultiMap<R, L>,
) : Map<L, Set<R>> by lToR {
    constructor() : this(MultiMap(), MultiMap())

    fun add(l: L, r: R): BiMap<L, R> = BiMap(lToR.add(l, r), rToL.add(r, l))

    fun remove(l: L, r: R): BiMap<L, R> = BiMap(lToR.remove(l, r), rToL.remove(r, l))

    fun flip(): BiMap<R, L> = BiMap(rToL, lToR)
}

data class Mut<T>(var value: T)

operator fun <L, R> Mut<BiMap<L, R>>.set(l: L, r: R) {
    value = value.add(l, r)
}
