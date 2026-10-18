package com.example.microware.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.microware.community.Banner
import com.example.microware.game.model.Game
import com.example.microware.game.model.fromModel
import com.example.microware.game.sprite.Sprite.Companion.load
import com.example.microware.getCommunityMetadata
import com.example.microware.getExperienceMetadatas
import com.example.microware.getExperiences
import com.example.microware.platform.SystemBackHandler
import kotlinx.coroutines.launch

private data class ExperienceRun(val experience: PlayExperienceMetadata, val session: PlaySession)

@Composable
fun PlayPage(onBack: () -> Unit) {
    var experiences by remember { mutableStateOf(emptyList<PlayExperienceMetadata>()) }
    LaunchedEffect(Unit) {
        experiences = getCommunityMetadata().flatMap { community ->
            getExperienceMetadatas(community.id).map { metadata ->
                PlayExperienceMetadata(
                    id = metadata.id,
                    communityId = community.id,
                    banner = Banner(metadata.banner.load()),
                )
            }
        }
    }

    PlayPage(experiences, onBack) { metadata ->
        getExperiences(metadata.communityId)
            .firstOrNull { it.id == metadata.id }
            ?.games
            ?.map { it.fromModel() }
            .orEmpty()
    }
}

@Composable
fun PlayPage(experiences: List<Experience>, onBack: () -> Unit) {
    val metadata = experiences.mapIndexed { index, experience ->
        PlayExperienceMetadata(
            id = index.toLong(),
            communityId = 0,
            banner = experience.banner,
            gameCount = experience.games.size,
        )
    }
    PlayPage(metadata, onBack) { selection ->
        experiences[selection.id.toInt()].games
    }
}

@Composable
private fun PlayPage(
    experiences: List<PlayExperienceMetadata>,
    onBack: () -> Unit,
    loadGames: suspend (PlayExperienceMetadata) -> List<Game>,
) {
    var run by remember { mutableStateOf<ExperienceRun?>(null) }
    var loadingExperience by remember { mutableStateOf<Long?>(null) }
    var emptyExperiences by remember(experiences) { mutableStateOf(emptySet<Long>()) }
    val scope = rememberCoroutineScope()
    val currentRun = run
    if (currentRun == null) {
        SystemBackHandler(onBack = onBack)
        PickExperience(
            experiences = experiences.map { metadata ->
                if (metadata.id in emptyExperiences) metadata.copy(gameCount = 0) else metadata
            },
            onBack = onBack,
            loading = loadingExperience != null,
        ) { selection ->
            loadingExperience = selection.id
            scope.launch {
                val games = loadGames(selection)
                loadingExperience = null
                if (games.isEmpty()) {
                    emptyExperiences = emptyExperiences + selection.id
                } else {
                    run = ExperienceRun(selection, PlaySession(games))
                }
            }
        }
    } else {
        PlayScreen(
            session = currentRun.session,
            onExit = { run = null },
            onReplay = {
                scope.launch {
                    val games = loadGames(currentRun.experience)
                    if (games.isNotEmpty()) {
                        run = currentRun.copy(session = PlaySession(games))
                    }
                }
            },
        )
    }
}
