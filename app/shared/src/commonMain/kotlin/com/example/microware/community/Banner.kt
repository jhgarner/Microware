package com.example.microware.community

import com.example.microware.Palette
import com.example.microware.core.validateBanner
import com.example.microware.game.sprite.Sprite

data class Banner(val sprite: Sprite) {
    init {
        validateBanner(sprite.width, sprite.height)
    }
}

fun defaultBanner() = Banner(Sprite.solid(128, 32, Palette.Forest))
