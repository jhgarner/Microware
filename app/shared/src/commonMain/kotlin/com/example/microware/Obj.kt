package com.example.microware

import androidx.compose.ui.geometry.Offset

// Will contain more than just the Sprite in the future
data class Obj(val id: Int, val offset: Offset, val sprite: Sprite) {
    constructor(id: Int, width: Int, height: Int, offset: Offset = Offset.Zero) : this(
        id,
        offset,
        Sprite.empty(width, height)
    )
}
