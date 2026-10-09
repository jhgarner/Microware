package com.example.microware.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.microware.platform.SystemBackHandler
import com.example.microware.util.Id

private data class ExperienceRun(val experienceId: Id, val session: PlaySession)

@Composable
fun PlayPage(experiences: List<Experience>, onBack: () -> Unit) {
    var run by remember { mutableStateOf<ExperienceRun?>(null) }
    val currentRun = run
    if (currentRun == null) {
        SystemBackHandler(onBack = onBack)
        PickExperience(experiences, onBack = onBack) {
            run = ExperienceRun(it.id, PlaySession(it.games))
        }
    } else {
        PlayScreen(
            session = currentRun.session,
            onExit = { run = null },
            onReplay = {
                val experience = experiences.first { it.id == currentRun.experienceId }
                run = currentRun.copy(session = PlaySession(experience.games))
            },
        )
    }
}
