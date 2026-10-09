package com.example.microware.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.microware.Palette
import com.example.microware.game.runtime.GameResult
import com.example.microware.platform.SystemBackHandler
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawBackIcon
import com.example.microware.ui.icons.drawLoseIcon
import com.example.microware.ui.icons.drawPlayIcon
import com.example.microware.ui.icons.drawTapIcon
import com.example.microware.ui.icons.drawWinIcon
import com.example.microware.ui.layout.insets

@Composable
internal fun PlayScreen(session: PlaySession, onExit: () -> Unit, onReplay: () -> Unit) {
    val phase = session.phase
    val shownPhase = (phase as? PlayPhase.Paused)?.previous ?: phase
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { session.pause() }
    SystemBackHandler {
        when (phase) {
            is PlayPhase.Paused -> session.resume()
            PlayPhase.GameOver -> onExit()
            else -> session.pause()
        }
    }

    LaunchedEffect(session, phase) {
        if (phase.durationMillis == null) return@LaunchedEffect
        var previousFrame = withFrameMillis { it }
        while (session.phase == phase) {
            val frame = withFrameMillis { it }
            session.advance(frame - previousFrame)
            previousFrame = frame
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = insets.leftDp() + 12.dp,
                end = insets.rightDp() + 12.dp,
                top = insets.topDp() + 8.dp,
                bottom = insets.bottomDp() + 12.dp,
            )
            .testTag("play-phase")
            .semantics { stateDescription = phase.description },
    ) {
        PlayHud(session)
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            when (shownPhase) {
                is PlayPhase.Playing -> shownPhase.player.PlayGame(
                    active = phase is PlayPhase.Playing,
                    speed = session.speed,
                    modifier = Modifier.testTag("play-game"),
                    applyInsets = false,
                    onFinished = { session.finish(shownPhase.player, it) },
                )
                PlayPhase.Ready -> PhaseArtwork {
                    drawTapIcon(session.phaseProgress)
                }
                is PlayPhase.Feedback -> PhaseArtwork {
                    if (shownPhase.result == GameResult.Won) {
                        drawCircle(Palette.Green.color, size.minDimension * .48f)
                        drawWinIcon(session.phaseProgress)
                    } else drawLoseIcon()
                }
                PlayPhase.SpeedUp -> PhaseArtwork { drawSpeedIcon(session.phaseProgress) }
                PlayPhase.GameOver -> RunFinished(session.score, onReplay, onExit)
                is PlayPhase.Paused -> Unit
            }
            if (phase is PlayPhase.Paused) {
                PauseOverlay(onResume = session::resume, onExit = onExit)
            }
        }
    }
}

@Composable
private fun PhaseArtwork(size: Dp = 160.dp, draw: DrawScope.() -> Unit) {
    Canvas(Modifier.size(size), onDraw = draw)
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onExit: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Palette.OffWhite.color.copy(alpha = .95f)),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            IconButton(description = "Resume run", modifier = Modifier.size(88.dp), onClick = onResume) {
                drawPlayIcon()
            }
            IconButton(description = "Exit run", modifier = Modifier.size(88.dp), onClick = onExit) {
                drawBackIcon()
            }
        }
    }
}

@Composable
private fun RunFinished(score: Int, onReplay: () -> Unit, onExit: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artworkSize = minOf(160.dp, maxHeight / 3)
        val numberSize = minOf(120.dp, maxHeight / 4)
        val buttonSize = minOf(80.dp, maxHeight / 4, maxWidth / 3)
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            PhaseArtwork(artworkSize) { drawWinIcon() }
            PlayNumber(score, Modifier.size(numberSize), description = "Final score $score")
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(description = "Replay run", modifier = Modifier.size(buttonSize), onClick = onReplay) {
                    drawReplayIcon(it)
                }
                IconButton(description = "Back to experiences", modifier = Modifier.size(buttonSize), onClick = onExit) {
                    drawBackIcon()
                }
            }
        }
    }
}
