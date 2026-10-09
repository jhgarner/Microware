package com.example.microware.community

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.layout.insets

@Composable
fun Communities(
    communities: List<Community>,
    lookup: (DurableId) -> Community?,
    addCommunity: (Community) -> Unit,
) {
    if (communities.isEmpty()) {
        JoinCommunity { addCommunity(lookup(it) ?: return@JoinCommunity false).let { true } }
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
                        val sprite = community.icon.sprite
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