package com.example.microware.play

import com.example.microware.game.model.Game
import com.example.microware.game.model.World
import com.example.microware.game.runtime.GameResult
import com.example.microware.game.runtime.Player
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlaySessionTest {
    @Test
    fun readyCreatesFreshPlayerAndOnlyCurrentPlayerCanFinishAttempt() {
        val session = PlaySession(listOf(game()), Random(1))

        session.advance(599)

        assertEquals(PlayPhase.Ready, session.phase)
        assertEquals(599L, session.phaseElapsedMs)

        session.advance(1)
        val firstPlayer = (session.phase as PlayPhase.Playing).player
        session.finish(Player(emptyList()), GameResult.Won)

        assertSame(firstPlayer, (session.phase as PlayPhase.Playing).player)
        assertEquals(0, session.score)

        session.finish(firstPlayer, GameResult.Won)
        session.finish(firstPlayer, GameResult.Lost)

        assertEquals(PlayPhase.Feedback(GameResult.Won), session.phase)
        assertEquals(1, session.score)
        assertEquals(PlaySession.StartingLives, session.lives)
        session.advance(699)
        assertEquals(PlayPhase.Feedback(GameResult.Won), session.phase)

        session.advance(1)
        assertEquals(PlayPhase.Ready, session.phase)
        assertEquals(2, session.round)
        session.advance(600)

        val secondPlayer = (session.phase as PlayPhase.Playing).player
        assertNotSame(firstPlayer, secondPlayer)
    }

    @Test
    fun lostAttemptsEndTheSessionAfterTheFourthFeedback() {
        val session = PlaySession(listOf(game()), Random(2))

        repeat(PlaySession.StartingLives) { attempt ->
            val player = startAttempt(session)
            session.finish(player, GameResult.Lost)

            assertEquals(PlayPhase.Feedback(GameResult.Lost), session.phase)
            assertEquals(PlaySession.StartingLives - attempt - 1, session.lives)
            session.advance(700)

            if (attempt < PlaySession.StartingLives - 1) {
                assertEquals(PlayPhase.Ready, session.phase)
            }
        }

        assertEquals(PlayPhase.GameOver, session.phase)
        assertEquals(0, session.lives)
        session.advance(10_000)
        assertEquals(PlayPhase.GameOver, session.phase)
    }

    @Test
    fun pausePreservesTimedPhaseAndCanRecordAResultWhilePaused() {
        val session = PlaySession(listOf(game()), Random(3))
        session.advance(250)
        session.pause()

        assertEquals(PlayPhase.Paused(PlayPhase.Ready), session.phase)
        session.advance(1_000)
        assertEquals(250L, session.phaseElapsedMs)

        session.resume()
        session.advance(349)
        assertEquals(PlayPhase.Ready, session.phase)
        session.advance(1)
        val player = (session.phase as PlayPhase.Playing).player
        session.pause()
        session.finish(player, GameResult.Won)

        assertEquals(PlayPhase.Paused(PlayPhase.Feedback(GameResult.Won)), session.phase)
        session.advance(700)
        assertEquals(0L, session.phaseElapsedMs)

        session.resume()
        session.advance(699)
        assertEquals(PlayPhase.Feedback(GameResult.Won), session.phase)
        session.advance(1)
        assertEquals(PlayPhase.Ready, session.phase)
    }

    @Test
    fun seededShuffleBagUsesEverySnapshotGameAndAvoidsBoundaryRepeat() {
        val sourceGames = MutableList(4) { game() }
        val session = PlaySession(sourceGames, Random(42))
        val firstBagGames = mutableListOf(session.game)

        repeat(3) {
            finishAttempt(session, GameResult.Won)
            firstBagGames += session.game
        }

        finishAttempt(session, GameResult.Won)
        val nextBagGame = session.game

        assertEquals(sourceGames.toSet(), firstBagGames.toSet())
        assertNotEquals(firstBagGames.last(), nextBagGame)

        val appendedGame = game()
        sourceGames += appendedGame
        finishAttempt(session, GameResult.Won)

        assertTrue(session.game in sourceGames.dropLast(1))
        assertNotEquals(appendedGame, session.game)
    }

    @Test
    fun speedIncreasesEveryFiveCompletedAttemptsAndStopsAtMaximumTier() {
        val session = PlaySession(listOf(game()), Random(4))

        repeat(25) { completed ->
            val player = startAttempt(session)
            val result = if (completed < 3) GameResult.Lost else GameResult.Won
            session.finish(player, result)
            session.advance(700)

            if ((completed + 1) % 5 == 0 && completed < 20) {
                assertEquals(PlayPhase.SpeedUp, session.phase)
                assertEquals(1f + ((completed + 1) / 5) * 0.15f, session.speed)
                session.advance(700)
            }
        }

        assertEquals(1.6f, session.speed)
        assertEquals(22, session.score)
        assertEquals(1, session.lives)
        assertEquals(26, session.round)
        assertEquals(PlayPhase.Ready, session.phase)
    }

    private fun startAttempt(session: PlaySession): Player {
        session.advance(600)
        return (session.phase as PlayPhase.Playing).player
    }

    private fun finishAttempt(session: PlaySession, result: GameResult) {
        session.finish(startAttempt(session), result)
        session.advance(700)
        if (session.phase == PlayPhase.SpeedUp) session.advance(700)
    }

    private fun game() = Game(World(), persistentListOf())
}
