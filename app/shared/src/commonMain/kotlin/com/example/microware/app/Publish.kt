package com.example.microware.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.example.microware.core.Model
import com.example.microware.game.model.Game
import com.example.microware.game.sprite.Sprite.Companion.load
import com.example.microware.publishGame
import com.example.microware.publishableExperiences
import com.example.microware.publishedAt
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawLoseIcon
import com.example.microware.ui.icons.drawSaveIcon
import com.example.microware.ui.layout.insets
import com.example.microware.unpublishGame
import kotlinx.coroutines.launch

@Composable
fun PublishGame(
    game: Game,
    setGameDbId: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableStateOf(0) }
    var publishables by remember { mutableStateOf(listOf<Model.PublishableExperience>()) }
    var publishedAt by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(refresh) {
        publishables = publishableExperiences()
        if (game.dbId != null) {
            publishedAt = publishedAt(game.dbId).toSet()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = insets.leftDp() + 24.dp,
            end = insets.rightDp() + 24.dp,
            top = 24.dp,
            bottom = insets.bottomDp() + 72.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (publishable in publishables) {
            item {
                Canvas(Modifier.size(40.dp)) {
                    drawImage(
                        publishable.community.icon.load().bitmap,
                        dstSize = size.toIntSize(),
                        filterQuality = FilterQuality.None,
                    )
                }
            }
            items(items = publishable.experiences) { experience ->
                val published = publishedAt.contains(experience.id)
                Row {
                    Canvas(
                        modifier = Modifier.aspectRatio(4f).weight(1f),
                    ) {
                        drawImage(
                            experience.banner.load().bitmap,
                            dstSize = size.toIntSize(),
                            filterQuality = FilterQuality.None,
                        )
                    }
                    if (published) {
                        Column(modifier = Modifier.fillMaxHeight()) {
                            IconButton(
                                modifier = Modifier.weight(.5f).width(48.dp),
                                onClick = {
                                    scope.launch {
                                        publishGame(experience.id, game)
                                    }
                                },
                            ) {
                                drawLoseIcon()
                            }
                            IconButton(
                                modifier = Modifier.weight(.5f).width(48.dp),
                                onClick = {
                                    scope.launch {
                                        game.dbId?.let { unpublishGame(experience.id, it) }
                                    }
                                },
                            ) {
                                drawLoseIcon()
                            }
                        }
                    } else {
                        IconButton(
                            modifier = Modifier.fillMaxHeight(),
                            onClick = {
                                scope.launch {
                                    val gameId = publishGame(experience.id, game)
                                    if (gameId != game.dbId) {
                                        setGameDbId(gameId)
                                    }
                                }
                            },
                        ) {
                            drawSaveIcon()
                        }
                    }
                }
            }
        }
    }
}
