package com.example.microware.community

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.example.microware.ui.components.IconButton

@Composable
fun Communities(
    communities: List<Community>,
    lookup: (DurableId) -> Community?,
    addCommunity: (Community) -> Unit,
) {
    if (communities.isEmpty()) {
        JoinCommunity { addCommunity(lookup(it) ?: return@JoinCommunity false).let { true } }
    } else {
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