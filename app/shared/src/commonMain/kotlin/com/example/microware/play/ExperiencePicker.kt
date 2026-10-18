package com.example.microware.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawBackIcon
import com.example.microware.ui.icons.drawHomePlayIcon
import com.example.microware.ui.layout.insets

@Composable
fun PickExperience(
    experiences: List<PlayExperienceMetadata>,
    onBack: () -> Unit,
    loading: Boolean,
    pick: (PlayExperienceMetadata) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(
        start = insets.leftDp() + 16.dp,
        end = insets.rightDp() + 16.dp,
        top = insets.topDp() + 8.dp,
        bottom = insets.bottomDp(),
    )) {
        IconButton(description = "Back from play", modifier = Modifier.size(52.dp), onClick = onBack) {
            drawBackIcon()
        }
        if (experiences.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(120.dp).semantics { contentDescription = "No experiences to play yet" }) {
                    drawHomePlayIcon(0f)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(experiences, key = { _, experience -> experience.id }) { index, experience ->
                    IconButton(
                        description = "Play experience",
                        modifier = Modifier.fillMaxWidth().height(112.dp).semantics {
                            stateDescription = experience.gameCount?.let {
                                "Experience ${index + 1}, $it games"
                            } ?: "Experience ${index + 1}"
                        },
                        aspectRatio = 4f,
                        enabled = !loading && experience.gameCount != 0,
                        onClick = { pick(experience) },
                    ) {
                        drawImage(
                            experience.banner.sprite.bitmap,
                            dstSize = size.toIntSize(),
                            filterQuality = FilterQuality.None,
                            alpha = if (experience.gameCount == 0) .35f else 1f,
                        )
                    }
                }
            }
        }
    }
}
