package com.example.microware.community

import com.example.microware.game.sprite.Sprite

const val AvatarWidth = 32
const val AvatarHeight = 32

data class Avatar(val sprite: Sprite) {
    init {
        require(sprite.width == AvatarWidth && sprite.height == AvatarHeight)
    }
}
