package com.example.microware.server

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.lessEq


object Tables {
    object User : LongIdTable() {
        val lower = long("lower")
        val upper = long("upper")

        init {
            index("uuid", isUnique = true, lower, upper)
        }
    }

    object Sprite : LongIdTable() {
        val width = integer("width").check { it lessEq 128 }
        val height = integer("height").check { it lessEq 256 }
        val pixels = binary("pixels", 128 * 256)
    }

    object Community : LongIdTable() {
        val banner = spriteReference("banner")
        val avatar = spriteReference("avatar")
        val durableId = long("durableId").uniqueIndex()
        val owner = cascadeReference("owner", User).index()
    }

    object CommunityUserJoin : Table() {
        val community = cascadeReference("community", Community)
        val user = cascadeReference("user", User)

        init {
            index("CUjoined", isUnique = true, community, user)
        }
    }

    object Experience : LongIdTable() {
        val banner = spriteReference("banner")
        val community = cascadeReference("community", Community).index()
        val index = integer("index")
        val playable = bool("playable").default(true)
        val publishable = bool("publishable").default(true)
    }

    object Game : LongIdTable() {
        val owner = cascadeReference("owner", User).index()
    }

    object ExperienceGameJoin : Table() {
        val experience = cascadeReference("experience", Experience).index()
        val game = cascadeReference("game", Game).index()

        init {
            index("EGjoined", isUnique = true, experience, game)
        }
    }

    object Obj : LongIdTable() {
        val game = cascadeReference("game", Game).index()
        val index = integer("index")
        val sprite = spriteReference("sprite")
        val xOffset = float("xOffset")
        val yOffset = float("yOffset")
        val tags = tagSet("tags")
    }

    object Trigger : LongIdTable() {
        val game = cascadeReference("game", Game).index()
        val index = integer("index")
        val variant = integer("variant")
        val tapTargetTags = tagSet("tapTargetTags")
        val collisionFirstTargetTags = tagSet("collisionFirstTargetTags")
        val collisionSecondTargetTags = tagSet("collisionSecondTargetTags")
        val collisionFirstTargetCount = integer("collisionFirstTargetCount").default(1)
    }

    object Action : Table() {
        val trigger = cascadeReference("trigger", Trigger).index()
        val index = integer("index")
        val variant = integer("variant")
        val moveMoverTags = tagSet("moveMoverTags")
        val moveTarget = integer("moveTarget").nullable()
        val moveSpeed = integer("moveSpeed").nullable()
        val moveToMoverTags = tagSet("moveToMoverTags")
        val moveToTargetTags = tagSet("moveToTargetTags")
        val moveToSpeed = integer("moveToSpeed").nullable()
        val moveWithinMoverTags = tagSet("moveWithinMoverTags")
        val moveWithinTargetTags = tagSet("moveWithinTargetTags")
        val moveWithinSpeed = integer("moveWithinSpeed").nullable()
    }

    private fun Table.tagSet(name: String): Column<List<Int>> =
        array<Int>(name).default(emptyList())

    private fun Table.spriteReference(name: String): Column<EntityID<Long>> =
        reference(name, Sprite, onDelete = ReferenceOption.RESTRICT)

    private fun <T : Any> Table.cascadeReference(
        name: String,
        type: IdTable<T>,
    ): Column<EntityID<T>> = reference(name, type, onDelete = ReferenceOption.CASCADE)
}
