package com.example.microware.play

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.microware.community.DrawBanner


@Composable
fun PickExperience(experiences: List<Experience>, pick: (Experience) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
    ) {
        for (experience in experiences) {
            item {
                Box(modifier = Modifier.clickable { pick(experience) }) {
                    DrawBanner(experience.banner)
                }
            }
        }
    }
}