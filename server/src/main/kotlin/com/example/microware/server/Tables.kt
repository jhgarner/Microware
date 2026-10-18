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
            index("joined", isUnique = true, community, user)
        }
    }

    object Experience : LongIdTable() {
        val banner = spriteReference("banner")
        val community = cascadeReference("community", Community).index()
        val index = integer("index")
    }

    object Game : LongIdTable() {
        val owner = cascadeReference("owner", User).index()
    }

    object ExperienceGameJoin : Table() {
        val experience = cascadeReference("experience", Experience).index()
        val game = cascadeReference("game", Game).index()

        init {
            index("joined", isUnique = true, experience, game)
        }
    }

    object Obj : LongIdTable() {
        val game = cascadeReference("game", Game).index()
        val index = integer("index")
        val sprite = spriteReference("sprite")
        val xOffset = float("xOffset")
        val yOffset = float("yOffset")
        val tag = integer("tag").nullable()
    }

    object Trigger : LongIdTable() {
        val game = cascadeReference("game", Game).index()
        val index = integer("index")
        val variant = integer("variant")
        val tapTarget = integer("tapTarget").nullable()
        val collisionFirstTarget = integer("collisionFirstTarget").nullable()
        val collisionSecondTarget = integer("collisionSecondTarget").nullable()
    }

    object Action : Table() {
        val trigger = cascadeReference("trigger", Trigger).index()
        val index = integer("index")
        val variant = integer("variant")
        val moveMover = integer("moveMover").nullable()
        val moveTarget = integer("moveTarget").nullable()
        val moveSpeed = integer("moveSpeed").nullable()
        val moveToMover = integer("moveToMover").nullable()
        val moveToTarget = integer("moveToTarget").nullable()
        val moveToSpeed = integer("moveToSpeed").nullable()
        val moveWithinMover = integer("moveWithinMover").nullable()
        val moveWithinTarget = integer("moveWithinTarget").nullable()
        val moveWithinSpeed = integer("moveWithinSpeed").nullable()
    }

    private fun Table.spriteReference(name: String): Column<EntityID<Long>> =
        reference(name, Sprite, onDelete = ReferenceOption.RESTRICT)

    private fun <T : Any> Table.cascadeReference(
        name: String,
        type: IdTable<T>,
    ): Column<EntityID<T>> = reference(name, type, onDelete = ReferenceOption.CASCADE)
}