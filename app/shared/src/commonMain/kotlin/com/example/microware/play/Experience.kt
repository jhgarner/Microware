package com.example.microware.play

import com.example.microware.community.Banner
import com.example.microware.game.model.Game

data class Experience(
    val banner: Banner,
    val games: List<Game>,
)