package com.example.microware.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.example.microware.Palette
import com.example.microware.community.Community
import com.example.microware.community.DurableId
import com.example.microware.game.model.Game
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawCheckIcon
import com.example.microware.ui.icons.drawJoinCommunityIcon
import com.example.microware.ui.icons.drawPublishIcon
import com.example.microware.ui.layout.insets
import com.example.microware.util.Id

@Composable
fun PublishGame(
    communities: List<Community>,
    game: Game,
    onJoinCommunity: () -> Unit,
    onPublish: (DurableId, Id) -> Unit,
) {
    if (communities.all { it.experiences.isEmpty() }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            IconButton(
                description = if (communities.isEmpty()) "Join a community to publish a game" else "No experiences available for publishing",
                modifier = Modifier.size(96.dp),
                enabled = communities.isEmpty(),
                onClick = onJoinCommunity,
            ) {
                if (communities.isEmpty()) drawJoinCommunityIcon(it) else drawPublishIcon(it)
            }
        }
        return
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
        for (community in communities) {
            if (community.experiences.isEmpty()) continue
            item {
                Canvas(Modifier.size(40.dp)) {
                    drawImage(
                        community.icon.sprite.bitmap,
                        dstSize = size.toIntSize(),
                        filterQuality = FilterQuality.None,
                    )
                }
            }
            items(community.experiences) { experience ->
                val publishedGame = experience.games.firstOrNull { it.id == game.id }
                val published = publishedGame == game
                Box {
                    IconButton(
                        description = when {
                            published -> "Current game version published to experience"
                            publishedGame != null -> "Publish changes to experience"
                            else -> "Publish game to experience"
                        },
                        modifier = Modifier.fillMaxWidth().height(104.dp),
                        selected = published,
                        aspectRatio = 4f,
                        onClick = { onPublish(community.code, experience.id) },
                    ) {
                        drawImage(
                            experience.banner.sprite.bitmap,
                            dstSize = size.toIntSize(),
                            filterQuality = FilterQuality.None,
                        )
                    }
                    if (published) {
                        Canvas(Modifier.align(Alignment.TopEnd).padding(12.dp).size(28.dp)) {
                            drawCircle(Palette.Green.color)
                            drawCheckIcon()
                        }
                    }
                }
            }
        }
    }
}
