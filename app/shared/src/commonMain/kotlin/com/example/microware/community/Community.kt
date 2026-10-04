package com.example.microware.community

import com.example.microware.game.sprite.Sprite
import com.example.microware.play.Experience

data class Community(
    val code: DurableId,
    val icon: Sprite,
    val memberBanner: Sprite,
    val experiences: List<Experience>,
)
