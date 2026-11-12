package com.example.microware.util

import kotlin.test.Test
import kotlin.test.assertEquals

class BiMapTest {
    @Test
    fun pairsAreVisibleFromBothSides() {
        val map = BiMap<String, Int>().add("a", 1).add("a", 2).add("b", 1)

        assertEquals(mapOf("a" to setOf(1, 2), "b" to setOf(1)), map.toMap())
        assertEquals(mapOf(1 to setOf("a", "b"), 2 to setOf("a")), map.flip().toMap())
    }

    @Test
    fun removingAPairLeavesTheOthersAndDropsEmptiedKeys() {
        val map = BiMap<String, Int>().add("a", 1).add("a", 2).add("b", 1)

        val removed = map.remove("a", 1).remove("b", 1)

        assertEquals(mapOf("a" to setOf(2)), removed.toMap())
        assertEquals(mapOf(2 to setOf("a")), removed.flip().toMap())
    }

    @Test
    fun removingAnAbsentPairChangesNothing() {
        val map = BiMap<String, Int>().add("a", 1)

        assertEquals(map.toMap(), map.remove("a", 2).remove("c", 1).toMap())
    }
}
