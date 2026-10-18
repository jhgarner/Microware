package com.example.microware.core

import com.example.microware.game.model.Action
import com.example.microware.game.model.Trigger
import kotlinx.serialization.Serializable

object Model {
    @Serializable
    class Sprite(val width: Int, val height: Int, val pixels: ByteArray) {
        init {
            require(width <= 128)
            require(height <= 256)
            require(pixels.size <= 256 * 128)
        }
    }

    @Serializable
    class Offset(val x: Float, val y: Float)

    @Serializable
    class Obj(val offset: Offset, val sprite: Sprite, val tag: Int?)

    @Serializable
    class World(val objects: List<Obj>) {
        init {
            require(objects.size < 100)
        }
    }

    @Serializable
    class Logic(val trigger: Trigger, val actions: List<Action>) {
        init {
            require(actions.size < 50)
        }
    }

    @Serializable
    class Game(val id: Long?, val world: World, val logics: List<Logic>) {
        init {
            require(logics.size < 100)
        }
    }

    @Serializable
    class Experience(val id: Long, val banner: Sprite, val games: List<Game>) {
        init {
            validateBanner(banner.width, banner.height)
            require(games.size < 200)
        }
    }

    @Serializable
    class PublishableExperience(
        val community: CommunityMetadata,
        val experiences: List<ExperienceMetadata>,
    )

    @Serializable
    class ExperienceMetadata(
        val id: Long,
        val banner: Sprite,
    )

    @Serializable
    class Publish(
        val game: Game,
        val experience: Long,
    )

    @Serializable
    class Unpublish(
        val game: Long,
        val experience: Long,
    )

    @Serializable
    class CommunityMetadata(
        val id: Long,
        val code: Long,
        val icon: Sprite,
        val banner: Sprite,
    ) {
        init {
            validateAvatar(icon.width, icon.height)
            validateBanner(banner.width, banner.height)
        }
    }

    @Serializable
    class Community(
        val id: Long,
        val code: Long,
        val icon: Sprite,
        val banner: Sprite,
        val experiences: List<Experience>,
    ) {
        init {
            validateAvatar(icon.width, icon.height)
            validateBanner(banner.width, banner.height)
            require(experiences.size < 300)
        }
    }
}