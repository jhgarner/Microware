package com.example.microware.game.model
import com.example.microware.game.sprite.Sprite
import com.example.microware.util.Id
import com.example.microware.util.Identifiable

import androidx.compose.ui.geometry.Offset
import com.example.microware.util.Identifiable.Companion.nextId

data class Obj(
    val offset: Offset,
    val sprite: Sprite,
    override val id: Id = nextId(),
) : Identifiable<Obj> {
    constructor(width: Int, height: Int, offset: Offset = Offset.Zero) : this(
        offset,
        Sprite.empty(width, height)
    )

    override val self = this
    override fun newId(id: Id): Obj = copy(id = id)
}
