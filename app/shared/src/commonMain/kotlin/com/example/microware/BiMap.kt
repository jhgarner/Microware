package com.example.microware

class BiMap<L, R> private constructor(
    private val lToR: Map<L, R> = mapOf(),
    private val rToL: Map<R, L> = mapOf(),
) : Map<L, R> by lToR {
    constructor() : this(mapOf(), mapOf())

    fun add(l: L, r: R): BiMap<L, R> {
        val lToR = rToL[r]?.let { lToR - it } ?: lToR
        val rToL = lToR[l]?.let { rToL - it } ?: rToL
        return BiMap(lToR + (l to r), rToL + (r to l))
    }

    fun remove(r: R): BiMap<L, R> = BiMap(lToR, rToL - r)

    fun flip(): BiMap<R, L> = BiMap(rToL, lToR)
}

data class Mut<T>(var value: T)

operator fun <L, R> Mut<BiMap<L, R>>.set(l: L, r: R) {
    value = value.add(l, r)
}
