package com.example.microware.play

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.microware.game.model.Game
import com.example.microware.game.runtime.GameResult
import com.example.microware.game.runtime.Player
import kotlin.random.Random

sealed interface PlayPhase {
    data object Ready : PlayPhase
    data class Playing(val player: Player) : PlayPhase
    data class Feedback(val result: GameResult) : PlayPhase
    data object SpeedUp : PlayPhase
    data class Paused(val previous: PlayPhase) : PlayPhase
    data object GameOver : PlayPhase
}

class PlaySession(games: List<Game>, private val random: Random = Random.Default) {
    val games = games.toList()
    private val bag = mutableListOf<Int>()
    private var lastGameIndex: Int? = null
    private var completed = 0

    init {
        require(games.isNotEmpty())
    }

    var game by mutableStateOf(nextGame())
        private set
    var phase by mutableStateOf<PlayPhase>(PlayPhase.Ready)
        private set
    var phaseElapsedMs by mutableStateOf(0L)
        private set
    var score by mutableStateOf(0)
        private set
    var lives by mutableStateOf(StartingLives)
        private set
    var round by mutableStateOf(1)
        private set
    var speedTier by mutableStateOf(0)
        private set

    val speed: Float
        get() = 1f + speedTier * .15f

    val phaseProgress: Float
        get() {
            val shownPhase = (phase as? PlayPhase.Paused)?.previous ?: phase
            return shownPhase.durationMillis?.let { phaseElapsedMs.toFloat() / it } ?: 0f
        }

    fun advance(delta: Long) {
        val duration = phase.durationMillis ?: return
        phaseElapsedMs = (phaseElapsedMs + delta).coerceAtMost(duration)
        if (phaseElapsedMs < duration) return

        when (phase) {
            PlayPhase.Ready -> transition(PlayPhase.Playing(Player(game)))
            is PlayPhase.Feedback -> when {
                lives == 0 -> transition(PlayPhase.GameOver)
                completed / RoundsPerSpeedUp > speedTier && speedTier < MaximumSpeedTier -> {
                    speedTier++
                    transition(PlayPhase.SpeedUp)
                }
                else -> nextRound()
            }
            PlayPhase.SpeedUp -> nextRound()
            else -> Unit
        }
    }

    fun finish(player: Player, result: GameResult) {
        val paused = phase as? PlayPhase.Paused
        val playing = (paused?.previous ?: phase) as? PlayPhase.Playing ?: return
        if (playing.player !== player) return

        completed++
        if (result == GameResult.Won) score++ else lives--
        val feedback = PlayPhase.Feedback(result)
        transition(if (paused != null) PlayPhase.Paused(feedback) else feedback)
    }

    fun pause() {
        if (phase !is PlayPhase.Paused && phase != PlayPhase.GameOver) {
            phase = PlayPhase.Paused(phase)
        }
    }

    fun resume() {
        (phase as? PlayPhase.Paused)?.let { phase = it.previous }
    }

    private fun nextRound() {
        round++
        game = nextGame()
        transition(PlayPhase.Ready)
    }

    private fun transition(next: PlayPhase) {
        phaseElapsedMs = 0L
        phase = next
    }

    private fun nextGame(): Game {
        if (bag.isEmpty()) {
            bag.addAll(games.indices.shuffled(random))
            if (bag.size > 1 && bag.first() == lastGameIndex) {
                val first = bag[0]
                bag[0] = bag[1]
                bag[1] = first
            }
        }
        val index = bag.removeAt(0)
        lastGameIndex = index
        return games[index]
    }

    companion object {
        const val StartingLives = 4
        private const val RoundsPerSpeedUp = 5
        private const val MaximumSpeedTier = 4
    }
}

internal val PlayPhase.durationMillis: Long?
    get() = when (this) {
        PlayPhase.Ready -> 600L
        is PlayPhase.Feedback, PlayPhase.SpeedUp -> 700L
        else -> null
    }

internal val PlayPhase.description: String
    get() = when (this) {
        PlayPhase.Ready -> "ready"
        is PlayPhase.Playing -> "playing"
        is PlayPhase.Feedback -> "feedback"
        PlayPhase.SpeedUp -> "speed-up"
        is PlayPhase.Paused -> "paused"
        PlayPhase.GameOver -> "game-over"
    }
