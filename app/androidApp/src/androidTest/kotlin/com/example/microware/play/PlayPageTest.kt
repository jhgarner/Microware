package com.example.microware.play

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.runtime.mutableStateListOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.microware.community.DurableId
import com.example.microware.game.runtime.GameResult
import com.example.microware.game.runtime.Player
import com.example.microware.util.Id
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayPageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyExperienceCannotBeStarted() {
        composeRule.setContent {
            PlayPageTestContent(listOf(experienceWith()))
        }

        composeRule.onNodeWithContentDescription("Play experience")
            .assertIsNotEnabled()
    }

    @Test
    fun fourUnresolvedAttemptsEndRunAndReplayResetsIt() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PlayPageTestContent(listOf(experienceWith(unresolvedGame())))
        }
        composeRule.onAllNodesWithContentDescription("Play experience")[0].performClick()

        repeat(4) {
            composeRule.mainClock.advanceTimeBy(800L)
            composeRule.mainClock.advanceTimeBy(3_200L)
            composeRule.mainClock.advanceTimeBy(800L)
        }

        composeRule.onNodeWithTag("play-phase").assertStateDescription("game-over")
        composeRule.onNodeWithContentDescription("Replay run").performClick()
        advanceFrame()
        composeRule.onNodeWithTag("play-phase").assertStateDescription("ready")
        composeRule.onNodeWithTag("play-score").assertStateDescription("0")
        composeRule.onNodeWithTag("play-lives").assertStateDescription("4")
        composeRule.onNodeWithTag("play-round").assertStateDescription("1")
        composeRule.onNodeWithTag("play-speed").assertStateDescription("1.0")
    }

    @Test
    fun startWinScoresAtAttemptDeadline() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PlayPageTestContent(listOf(experienceWith(startWinGame())))
        }
        composeRule.onAllNodesWithContentDescription("Play experience")[0].performClick()
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.onNodeWithTag("play-phase").assertStateDescription("playing")

        composeRule.mainClock.advanceTimeBy(3_200L)
        composeRule.onNodeWithTag("play-phase").assertStateDescription("feedback")
        composeRule.onNodeWithTag("play-score").assertStateDescription("1")
    }

    @Test
    fun survivalGameWinsOnlyWhenRunnerAvoidsCollision() {
        val survived = Player(survivalGame()).apply { advance(3_000L) }
        val collided = Player(survivalGame(escapeHazard = false)).apply { advance(3_000L) }

        assertEquals(GameResult.Won, survived.result)
        assertEquals(GameResult.Lost, collided.result)
    }

    @Test
    fun pauseDoesNotConsumeAttemptTime() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PlayPageTestContent(listOf(experienceWith(unresolvedGame())))
        }
        composeRule.onNodeWithContentDescription("Play experience").performClick()
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.onNodeWithContentDescription("Pause run").performClick()
        advanceFrame()
        composeRule.onNodeWithTag("play-phase").assertStateDescription("paused")

        composeRule.mainClock.advanceTimeBy(10_000L)
        composeRule.onNodeWithContentDescription("Resume run").performClick()
        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.onNodeWithTag("play-phase").assertStateDescription("playing")
        composeRule.mainClock.advanceTimeBy(1_200L)
        composeRule.onNodeWithTag("play-phase").assertStateDescription("feedback")
    }

    @Test
    fun backgroundPausesRunAndForegroundReturnsToPauseOverlay() {
        composeRule.mainClock.autoAdvance = false
        val lifecycleOwner = TestLifecycleOwner()
        composeRule.runOnIdle { lifecycleOwner.resume() }
        composeRule.setContent {
            PlayPageTestContent(
                experiences = listOf(experienceWith(unresolvedGame())),
                lifecycleOwner = lifecycleOwner,
            )
        }
        composeRule.onNodeWithContentDescription("Play experience").performClick()
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.onNodeWithTag("play-phase").assertStateDescription("playing")

        composeRule.runOnIdle { lifecycleOwner.background() }
        advanceFrame()
        composeRule.onNodeWithTag("play-phase").assertStateDescription("paused")
        composeRule.mainClock.advanceTimeBy(10_000L)
        composeRule.runOnIdle { lifecycleOwner.foreground() }
        advanceFrame()
        composeRule.onNodeWithTag("play-phase").assertStateDescription("paused")

        composeRule.onNodeWithContentDescription("Resume run").performClick()
        advanceFrame()
        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.onNodeWithTag("play-phase").assertStateDescription("playing")
    }

    @Test
    fun systemBackReturnsFromPickerAndPausesAnActiveRun() {
        composeRule.mainClock.autoAdvance = false
        val backCount = AtomicInteger()
        composeRule.setContent {
            PlayPageTestContent(
                experiences = listOf(experienceWith(unresolvedGame())),
                onBack = { backCount.incrementAndGet() },
            )
        }
        Espresso.pressBack()
        advanceFrame()
        composeRule.waitForIdle()
        assertEquals(1, backCount.get())

        composeRule.onNodeWithContentDescription("Play experience").performClick()
        composeRule.mainClock.advanceTimeBy(800L)
        Espresso.pressBack()
        advanceFrame()
        composeRule.onNodeWithTag("play-phase").assertStateDescription("paused")
    }

    @Test
    fun tapAtRenderedGameCenterUsesWorldCoordinates() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PlayPageTestContent(listOf(experienceWith(tapWinGame())))
        }
        composeRule.onNodeWithContentDescription("Play experience").performClick()
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.onNodeWithTag("play-game").performTouchInput {
            click()
        }
        composeRule.mainClock.advanceTimeBy(3_200L)

        composeRule.onNodeWithTag("play-phase").assertStateDescription("feedback")
        composeRule.onNodeWithTag("play-score").assertStateDescription("1")
    }

    @Test
    fun publishingRoutesTheCommunityAndExperienceAndEnablesThatBannerForPlay() {
        val firstExperience = experienceWith()
        val targetExperience = experienceWith()
        val communities = listOf(
            communityWith(1L, firstExperience),
            communityWith(2L, targetExperience),
        )
        val publishedCommunity = AtomicReference<DurableId>()
        val publishedExperience = AtomicReference<Id>()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PublishToPlayTestContent(
                initialCommunities = communities,
                game = startWinGame(),
                onPublished = { communityCode, experienceId ->
                    publishedCommunity.set(communityCode)
                    publishedExperience.set(experienceId)
                },
            )
        }

        composeRule.onAllNodesWithContentDescription("Publish game to experience")[1]
            .performClick()
        advanceFrame()

        assertEquals(communities[1].code, publishedCommunity.get())
        assertEquals(targetExperience.id, publishedExperience.get())
        composeRule.onAllNodesWithContentDescription("Play experience")[0].assertIsNotEnabled()
        composeRule.onAllNodesWithContentDescription("Play experience")[1].assertIsEnabled()
        composeRule.onAllNodesWithContentDescription("Play experience")[1].performClick()
        advanceFrame()
        composeRule.onNodeWithTag("play-phase").assertStateDescription("ready")
    }

    @Test
    fun replayUsesTheLatestPublishedGameForTheSelectedExperience() {
        composeRule.mainClock.autoAdvance = false
        val oldGame = unresolvedGame()
        val experiences = mutableStateListOf(experienceWith(oldGame))
        composeRule.setContent {
            PlayPageTestContent(experiences.toList())
        }
        composeRule.onAllNodesWithContentDescription("Play experience")[0].performClick()
        repeat(4) {
            composeRule.mainClock.advanceTimeBy(800L)
            composeRule.mainClock.advanceTimeBy(3_200L)
            composeRule.mainClock.advanceTimeBy(800L)
        }
        composeRule.onNodeWithTag("play-phase").assertStateDescription("game-over")

        val updatedGame = startWinGame().copy(id = oldGame.id)
        composeRule.runOnIdle {
            experiences[0] = experiences[0].publish(updatedGame)
        }
        composeRule.onNodeWithContentDescription("Replay run").performClick()
        advanceFrame()
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.mainClock.advanceTimeBy(3_200L)

        composeRule.onNodeWithTag("play-phase").assertStateDescription("feedback")
        composeRule.onNodeWithTag("play-score").assertStateDescription("1")
    }

    @Test
    fun capturesPlayingPausedAndGameOverScreensForReview() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PlayPageTestContent(
                listOf(
                    experienceWith(tapWinGame()),
                    experienceWith(unresolvedGame()),
                ),
            )
        }
        composeRule.onAllNodesWithContentDescription("Play experience")[0].performClick()
        composeRule.mainClock.advanceTimeBy(800L)
        saveScreenshot("microware-playing")
        composeRule.onNodeWithContentDescription("Pause run").performClick()
        advanceFrame()
        saveScreenshot("microware-paused")
        composeRule.onNodeWithContentDescription("Exit run").performClick()
        advanceFrame()

        composeRule.onAllNodesWithContentDescription("Play experience")[1].performClick()
        repeat(4) {
            composeRule.mainClock.advanceTimeBy(800L)
            composeRule.mainClock.advanceTimeBy(3_200L)
            composeRule.mainClock.advanceTimeBy(800L)
        }
        composeRule.onNodeWithTag("play-phase").assertStateDescription("game-over")
        saveScreenshot("microware-game-over")
    }

    private fun saveScreenshot(name: String) {
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("Screenshot saved to ${file.absolutePath}")
    }

    private fun advanceFrame() {
        composeRule.mainClock.advanceTimeByFrame()
    }
}

private fun SemanticsNodeInteraction.assertStateDescription(value: String) {
    assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value))
}
