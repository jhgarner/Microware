package com.example.microware.play

import com.example.microware.community.Banner
import com.example.microware.game.model.Game
import com.example.microware.util.Id
import com.example.microware.util.Identifiable
import com.example.microware.util.Identifiable.Companion.nextId

data class Experience(
    val dbId: Long,
    val banner: Banner,
    val games: List<Game>,
    override val id: Id = nextId(),
) : Identifiable<Experience> {
    override val self = this
    override fun newId(id: Id) = copy(id = id)
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

data class PlayExperienceMetadata(
    val id: Long,
    val communityId: Long,
    val banner: Banner,
    val gameCount: Int? = null,
)
