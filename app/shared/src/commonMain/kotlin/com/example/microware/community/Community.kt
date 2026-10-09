package com.example.microware.community

import com.example.microware.game.model.Game
import com.example.microware.play.Experience
import com.example.microware.util.Id

data class Community(
    val code: DurableId,
    val icon: Avatar,
    val memberBanner: Banner,
    val experiences: List<Experience>,
) {
    fun publish(experienceId: Id, game: Game): Community = copy(
        experiences = experiences.map { experience ->
            if (experience.id == experienceId) experience.publish(game) else experience
        },
    )
}
