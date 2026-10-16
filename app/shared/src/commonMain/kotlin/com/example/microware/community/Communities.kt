package com.example.microware.community

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.example.microware.core.Model
import com.example.microware.game.sprite.Sprite
import com.example.microware.getCommunityMetadata
import com.example.microware.joinCommunity
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.layout.insets
import io.ktor.http.HttpStatusCode

@Composable
fun Communities() {
    var loadingCommunities by remember { mutableStateOf<List<Model.CommunityMetadata>?>(null) }
    var counter by remember { mutableStateOf(0) }
    LaunchedEffect(counter) {
        loadingCommunities = getCommunityMetadata()
    }
    val communities = loadingCommunities ?: return

    if (communities.isEmpty()) {
        JoinCommunity { (joinCommunity(it) == HttpStatusCode.OK).also { counter++ } }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(
                start = insets.leftDp() + 32.dp,
                end = insets.rightDp() + 32.dp,
                top = insets.topDp() + 32.dp,
                bottom = insets.bottomDp() + 32.dp,
            ),
        ) {
            Text("Todo this page is still in progress")
            LazyVerticalGrid(GridCells.FixedSize(100.dp)) {
                for (community in communities) {
                    item {
                        val icon = community.icon
                        val sprite = Sprite.fromPackedBytes(icon.width, icon.height, icon.pixels)
                        IconButton(onClick = {}) {
                            drawImage(
                                image = sprite.bitmap,
                                dstSize = size.toIntSize(),
                                filterQuality = FilterQuality.None,
                            )
                        }
                    }
                }
            }
        }
    }
}