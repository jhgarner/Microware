package com.example.microware.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.microware.game.runtime.GameResult
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawWinIcon

@Composable
internal fun PlayHud(session: PlaySession) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(28.dp)) { drawWinIcon() }
            PlayNumber(
                session.score,
                Modifier.size(40.dp).testTag("play-score"),
                "Score ${session.score}",
            )
            Spacer(Modifier.weight(1f))
            Lives(session)
            Spacer(Modifier.weight(1f))
            IconButton(
                description = "Pause run",
                modifier = Modifier.size(52.dp),
                enabled = session.phase !is PlayPhase.Paused && session.phase != PlayPhase.GameOver,
                onClick = session::pause,
            ) { drawPauseIcon() }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlayNumber(
                session.round,
                Modifier.size(28.dp).testTag("play-round"),
                "Round ${session.round}",
            )
            Canvas(
                Modifier.size(28.dp).testTag("play-speed").semantics {
                    contentDescription = "Speed ${session.speed}"
                    stateDescription = session.speed.toString()
                },
            ) { drawSpeedIcon(arrowCount = session.speedTier + 1) }
        }
    }
}

@Composable
private fun Lives(session: PlaySession) {
    val phase = (session.phase as? PlayPhase.Paused)?.previous ?: session.phase
    val feedback = phase as? PlayPhase.Feedback
    Row(
        Modifier.testTag("play-lives").semantics {
            contentDescription = "${session.lives} lives remaining"
            stateDescription = session.lives.toString()
        },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(PlaySession.StartingLives) { index ->
            Canvas(Modifier.size(20.dp)) {
                drawLife(false)
                val disappearing = feedback?.result == GameResult.Lost && index == session.lives
                if (disappearing) scale(1f - session.phaseProgress) { drawLife(true) }
                else drawLife(index < session.lives)
            }
        }
    }
}
