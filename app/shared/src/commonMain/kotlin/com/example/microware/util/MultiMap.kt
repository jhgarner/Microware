package com.example.microware.util

class MultiMap<Key, Value> private constructor(val map: Map<Key, Set<Value>>) :
    Map<Key, Set<Value>> by map {
    constructor() : this(mapOf())

    fun add(key: Key, value: Value): MultiMap<Key, Value> {
        val set = map[key] ?: setOf()
        val newSet = set + value
        return MultiMap(map + (key to newSet))
    }

    fun remove(key: Key, value: Value): MultiMap<Key, Value> {
        val list = map[key] ?: return this
        val newList = list - value
        if (newList.isEmpty()) return MultiMap(map - key)
        return MultiMap(map + (key to newList))
    }
}