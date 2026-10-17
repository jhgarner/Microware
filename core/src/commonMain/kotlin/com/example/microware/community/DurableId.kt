package com.example.microware.community

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random

/// A 64bit ID that's mostly monotonically increasing. It's like a UUID, but making it smaller makes
// it easier to share.
/// [------------Unix Time in ms-------------------][count-][-rand-]
/// 0000000000000000000000000000000000000000000000000000000000000000
data class DurableId(val bits: Long) {
    constructor() : this(buildNewBits())

    init {
        require(bits != 0L)
    }

    @OptIn(ExperimentalAtomicApi::class)
    companion object {
        private val counter = AtomicInt(0)

        fun nextCount(): Int = counter.fetchAndAdd(1);

        fun buildNewBits(): Long {
            return Random.nextLong()
//            val nowMs = Clock.System.now().toEpochMilliseconds()
//            val count = (nextCount() and 0xFF).toLong()
//            val rand = Random.nextBits(8).toLong()
//            return (nowMs shl 16) or (count shl 8) or rand
        }
    }
}