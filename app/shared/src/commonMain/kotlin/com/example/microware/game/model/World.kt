package com.example.microware.game.model

import androidx.compose.ui.geometry.Offset
import com.example.microware.game.sprite.MaxSpriteHeight
import com.example.microware.game.sprite.MaxSpriteWidth
import com.example.microware.util.BiMap
import com.example.microware.util.Id
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

const val WorldWidth = MaxSpriteWidth
const val WorldHeight = MaxSpriteHeight

data class World(
    val objects: PersistentList<Obj> = persistentListOf(),
    val logicTags: BiMap<LogicTag, Id> = BiMap(),
) {
    fun updateObject(newObj: Obj): World {
        return copy(
            objects = objects.map { obj ->
                if (obj.id != newObj.id) {
                    obj
                } else {
                    val maxX = (WorldWidth - newObj.sprite.width).coerceAtLeast(0).toFloat()
                    val maxY = (WorldHeight - newObj.sprite.height).coerceAtLeast(0).toFloat()
                    newObj.copy(
                        offset = Offset(
                            (newObj.offset.x).coerceIn(0f, maxX),
                            (newObj.offset.y).coerceIn(0f, maxY),
                        ),
                    )
                }
            }.toPersistentList(),
        )
    }

    fun addObject(obj: Obj): World = copy(objects = objects.adding(obj.newId()))
    fun upsertObject(obj: Obj): World {
        return objects.indexOfFirst { obj.id == it.id }.takeIf { it >= 0 }?.let {
            updateObject(obj)
        } ?: addObject(obj)
    }

    fun logicTagFor(objectId: Id): LogicTag? = logicTags.flip()[objectId]

    fun assignLogicTag(objectId: Id, logicTag: LogicTag?): World =
        copy(
            logicTags = if (logicTag != null) logicTags.add(
                logicTag,
                objectId,
            ) else logicTags.remove(objectId),
        )

    fun clamped(obj: Obj): Obj {
        val maxX = (WorldWidth - obj.sprite.width).coerceAtLeast(0).toFloat()
        val maxY = (WorldHeight - obj.sprite.height).coerceAtLeast(0).toFloat()
        return obj.copy(
            offset = Offset(
                obj.offset.x.coerceIn(0f, maxX),
                obj.offset.y.coerceIn(0f, maxY),
            ),
        )
    }
}

