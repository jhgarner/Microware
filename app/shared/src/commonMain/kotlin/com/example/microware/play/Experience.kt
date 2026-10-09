package com.example.microware.play

import com.example.microware.community.Banner
import com.example.microware.game.model.Game
import com.example.microware.util.Id
import com.example.microware.util.Identifiable.Companion.nextId

data class Experience(
    val banner: Banner,
    val games: List<Game>,
    val id: Id = nextId(),
) {
    fun publish(game: Game): Experience {
        return copy(
            games = if (games.any { it.id == game.id }) {
                games.map { published -> if (published.id == game.id) game else published }
            } else {
                games + game
            },
        )
    }
}
