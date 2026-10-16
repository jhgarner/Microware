package com.example.microware.community

import com.example.microware.core.validateAvatar
import com.example.microware.game.sprite.Sprite

data class Avatar(val sprite: Sprite) {
    init {
        validateAvatar(sprite.width, sprite.height)
    }
}
