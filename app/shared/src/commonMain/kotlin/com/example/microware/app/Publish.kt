package com.example.microware.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.microware.ui.icons.drawSaveIcon
import com.example.microware.ui.icons.drawTrashIcon
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
            publishedAt = runCatching { publishedAt(game.dbId).toSet() }.getOrElse { setOf() }
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
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val rowHeight = (maxWidth - 48.dp) / 4
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight),
                    ) {
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            drawImage(
                                experience.banner.load().bitmap,
                                dstSize = size.toIntSize(),
                                filterQuality = FilterQuality.None,
                            )
                        }
                        if (published) {
                            Column(
                                modifier = Modifier
                                    .width(48.dp)
                                    .fillMaxHeight(),
                            ) {
                                IconButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            publishGame(experience.id, game)
                                            refresh++
                                        }
                                    },
                                ) {
                                    drawSaveIcon()
                                }
                                IconButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            game.dbId?.let { unpublishGame(experience.id, it) }
                                            refresh++
                                        }
                                    },
                                ) {
                                    drawTrashIcon()
                                }
                            }
                        } else {
                            IconButton(
                                modifier = Modifier
                                    .width(48.dp)
                                    .fillMaxHeight(),
                                onClick = {
                                    scope.launch {
                                        val gameId = publishGame(experience.id, game)
                                        if (gameId != game.dbId) {
                                            setGameDbId(gameId)
                                        }
                                        refresh++
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
}
