package com.example.microware.play

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.microware.platform.SystemBackHandler

@Composable
fun PlayPage(experiences: List<Experience>, onBack: () -> Unit) {
    var picked by remember { mutableStateOf<Experience?>(null) }
    SystemBackHandler { if (picked != null) picked = null else onBack() }

    if (picked == null) {
        PickExperience(experiences) { picked = it }
    } else {
        Text("Yay")
    }

}