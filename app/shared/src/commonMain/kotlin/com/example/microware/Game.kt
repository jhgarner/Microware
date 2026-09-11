package com.example.microware

data class Game(
    val world: World,
    val logics: List<Logic>,
    val id: Int = 0,
)
