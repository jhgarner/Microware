package com.example.microware.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.microware.app.PublishGame
import com.example.microware.community.Banner
import com.example.microware.community.BannerHeight
import com.example.microware.community.BannerWidth
import com.example.microware.community.Avatar
import com.example.microware.community.Community
import com.example.microware.community.DurableId
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Game
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Obj
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.World
import com.example.microware.game.sprite.Sprite
import com.example.microware.Palette
import com.example.microware.ui.layout.Insets
import com.example.microware.ui.layout.LocalInsets
import com.example.microware.util.Id
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.collections.immutable.persistentListOf

internal fun experienceWith(vararg games: Game) = Experience(
    banner = Banner(
        Sprite.pattern(BannerWidth, BannerHeight) { index ->
            val x = index % BannerWidth / 8
            val y = index / BannerWidth / 8
            if ((x + y) % 2 == 0) Palette.Cyan else Palette.Navy
        },
    ),
    games = games.toList(),
)

internal fun startWinGame() = Game(
    world = World(),
    logics = persistentListOf(Logic(Trigger.Start, listOf(Action.Win))),
)

internal fun unresolvedGame() = Game(
    world = World(),
    logics = persistentListOf(),
)

internal fun tapWinGame(): Game {
    val tag = LogicTag(0)
    val untaggedWorld = World().addObject(
        Obj(
            offset = Offset(58f, 122f),
            sprite = Sprite.solid(12, 12, Palette.Yellow),
        ),
    )
    val world = untaggedWorld.assignLogicTag(untaggedWorld.objects.single().id, tag)
    return Game(
        world = world,
        logics = persistentListOf(Logic(Trigger.Tap(tag), listOf(Action.Win))),
    )
}

internal fun survivalGame(escapeHazard: Boolean = true): Game {
    val hazardTag = LogicTag(0)
    val runnerTag = LogicTag(1)
    val untaggedWorld = World()
        .addObject(Obj(Offset.Zero, Sprite.solid(8, 8, Palette.Red)))
        .addObject(Obj(Offset(16f, 0f), Sprite.solid(8, 8, Palette.Green)))
    val world = untaggedWorld
        .assignLogicTag(untaggedWorld.objects[0].id, hazardTag)
        .assignLogicTag(untaggedWorld.objects[1].id, runnerTag)

    return Game(
        world = world,
        logics = persistentListOf(
            Logic(
                Trigger.Start,
                listOf(
                    Action.Win,
                    Action.Move(
                        runnerTag,
                        if (escapeHazard) Direction.E else Direction.W,
                        speed = 8,
                    ),
                ),
            ),
            Logic(Trigger.Collision(hazardTag, runnerTag), listOf(Action.Lose)),
        ),
    )
}

internal fun communityWith(code: Long, experience: Experience) = Community(
    code = DurableId(code),
    icon = Avatar(Sprite.solid(32, 32, Palette.Purple)),
    memberBanner = experience.banner,
    experiences = listOf(experience),
)

@Composable
internal fun PlayPageTestContent(
    experiences: List<Experience>,
    onBack: () -> Unit = {},
    lifecycleOwner: LifecycleOwner? = null,
) {
    MaterialTheme {
        CompositionLocalProvider(LocalInsets provides Insets.new()) {
            Box(Modifier.fillMaxSize().background(Palette.OffWhite.color)) {
                if (lifecycleOwner == null) {
                    PlayPage(experiences, onBack)
                } else {
                    CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                        PlayPage(experiences, onBack)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PublishToPlayTestContent(
    initialCommunities: List<Community>,
    game: Game,
    onPublished: (DurableId, Id) -> Unit,
) {
    var communities by remember { mutableStateOf(initialCommunities) }
    var showingPlay by remember { mutableStateOf(false) }

    MaterialTheme {
        CompositionLocalProvider(LocalInsets provides Insets.new()) {
            Box(Modifier.fillMaxSize().background(Palette.OffWhite.color)) {
                if (showingPlay) {
                    PlayPage(communities.flatMap { it.experiences }, onBack = { showingPlay = false })
                } else {
                    PublishGame(
                        communities = communities,
                        game = game,
                        onJoinCommunity = {},
                        onPublish = { communityCode, experienceId ->
                            onPublished(communityCode, experienceId)
                            communities = communities.map { community ->
                                if (community.code == communityCode) {
                                    community.publish(experienceId, game)
                                } else {
                                    community
                                }
                            }
                            showingPlay = true
                        },
                    )
                }
            }
        }
    }
}

internal class TestLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle = registry

    fun resume() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun background() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun foreground() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
}
