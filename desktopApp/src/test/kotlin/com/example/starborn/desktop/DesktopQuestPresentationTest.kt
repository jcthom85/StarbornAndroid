package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.audio.*
import com.example.starborn.feature.exploration.viewmodel.ExplorationEvent
import kotlinx.coroutines.flow.collect
import com.example.starborn.ui.events.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import javax.imageio.ImageIO

class DesktopQuestQueueTest {
    private fun detail(id: String, type: QuestBannerType = QuestBannerType.NEW) = UiEvent.ShowQuestDetail(id, type, "Original title $id", "Original summary", listOf("Original objective"))

    @Test fun importantEventsKeepOrderAndDeduplicatePendingQuestTypes() {
        val state = QuestPresentationState()
        state.accept(detail("a")); state.accept(detail("a")); state.accept(detail("a", QuestBannerType.COMPLETED)); state.accept(detail("b"))
        assertEquals(3, state.important.size)
        assertEquals(QuestBannerType.NEW, (state.important.first() as QuestPresentationState.Important.Detail).event.type)
        state.dismissImportant()
        assertEquals(QuestBannerType.COMPLETED, (state.important.first() as QuestPresentationState.Important.Detail).event.type)
    }

    @Test fun progressPreservesObjectivesRolesAndCountAndUsesTwoSecondDedupe() {
        val state = QuestPresentationState()
        val event = UiEvent.ShowQuestBanner(QuestBannerType.PROGRESS, "a", "Original title", listOf(QuestObjectiveStatus("o", "Original objective", true, QuestObjectiveRole.JUST_COMPLETED)), 4)
        state.accept(event, 0); state.accept(event, 1000); state.accept(event, 2001)
        assertEquals(listOf(event), state.updates.toList())
        state.accept(UiEvent.ShowQuestBanner(QuestBannerType.NEW, "a", "Original title"))
        assertTrue(state.important.isEmpty())
    }

    @Test fun summaryToastBadgeAndResetRemainSeparate() {
        val state = QuestPresentationState()
        val summary = UiEvent.ShowQuestSummary(listOf(QuestSummaryEntry("a", SummaryType.COMPLETED, "Original title")))
        state.accept(summary); state.accept(summary); state.accept(UiEvent.ShowToast("toast", "Original toast"))
        state.accept(UiEvent.JournalBadgeDelta(2)); assertEquals(2, state.journalBadge)
        state.readJournal(); assertEquals(0, state.journalBadge)
        assertEquals(1, state.important.size); assertEquals(1, state.toasts.size)
        state.clear(); assertTrue(state.important.isEmpty()); assertTrue(state.toasts.isEmpty())
    }

    @Test fun completingQuestDropsObsoleteUpdatesAndRejectsLateProgress() {
        val state = QuestPresentationState()
        state.accept(UiEvent.ShowQuestBanner(QuestBannerType.PROGRESS, "first", "Old objective"), 0)
        state.accept(UiEvent.ShowQuestBanner(QuestBannerType.PROGRESS, "second", "Other quest"), 0)
        state.accept(detail("first", QuestBannerType.COMPLETED))
        state.accept(UiEvent.ShowQuestBanner(QuestBannerType.PROGRESS, "first", "Late stale objective"), 5000)
        assertEquals(listOf("second"), state.updates.map { it.questId })
        assertEquals(1, state.important.size)
    }

    @Test fun staleQuestDismissDoesNotSkipNextCard() {
        val state = QuestPresentationState()
        state.accept(detail("first")); state.accept(detail("second"))
        val old = state.important.first()
        state.dismissImportant(old)
        state.dismissImportant(old)
        assertEquals("second", (state.important.single() as QuestPresentationState.Important.Detail).event.questId)
    }

    @Test fun eachVisibleCardStartsOnlyOnceUntilDismissed() {
        val state = QuestPresentationState()
        state.accept(detail("a", QuestBannerType.FAILED))
        val current = state.important.first()
        assertTrue(state.markStarted(current)); assertFalse(state.markStarted(current))
        state.dismissImportant(); state.accept(detail("a", QuestBannerType.FAILED))
        assertTrue(state.markStarted(state.important.first()))
    }
}

class DesktopQuestPresentationTest {
    @get:Rule val compose = createComposeRule()
    private class RecordingAudio : AudioDriver {
        val commands = CopyOnWriteArrayList<AudioCommand>()
        override fun execute(command: AudioCommand) { commands.add(command) }
        override fun setUserGain(type: AudioCueType, gain: Float) {}
        override fun release() {}
    }

    @Test @OptIn(ExperimentalTestApi::class)
    fun cardsDeferPreserveAuthoredCopyOpenDetailsAndDismissByKeyboard() {
        val directory = Files.createTempDirectory("starborn-quest-ui-").toFile()
        val audio = RecordingAudio()
        val services = DesktopAppServices(directory, audio)
        val presentations = QuestPresentationState()
        val blocked = mutableStateOf(true)
        val narrow = mutableStateOf(false)
        var opened: String? = null
        try {
            compose.setContent { DesktopStarbornTheme {
                Box(Modifier.size(if (narrow.value) 800.dp else 1280.dp, if (narrow.value) 600.dp else 720.dp)) {
                    DesktopQuestPresentation(services, presentations, blocked.value) { opened = it }
                }
            } }
            compose.runOnIdle {
                presentations.accept(UiEvent.ShowQuestDetail("first", QuestBannerType.NEW, "Original Quest Title", "Original authored summary", listOf("Original objective")))
                presentations.accept(UiEvent.ShowQuestDetail("second", QuestBannerType.COMPLETED, "Completed Quest Title", "Completed authored summary", listOf("Completed objective")))
            }
            compose.onNodeWithText("Original Quest Title").assertDoesNotExist()
            compose.runOnIdle { assertFalse(audio.commands.filterIsInstance<AudioCommand.Play>().any { it.cueId == "quest_new_stinger" }) }
            compose.runOnIdle { blocked.value = false }
            compose.onNodeWithText("New Quest").assertIsDisplayed()
            compose.onNodeWithText("Original authored summary").assertIsDisplayed()
            compose.onNodeWithText("Original objective").assertIsDisplayed()
            capture("quest-new")
            compose.runOnIdle { narrow.value = true; blocked.value = true }
            compose.runOnIdle { blocked.value = false }
            compose.waitUntil(5000) { audio.commands.filterIsInstance<AudioCommand.Play>().count { it.cueId == "quest_new_stinger" } == 1 }
            compose.onNodeWithText("Details").performClick()
            compose.runOnIdle { assertEquals("first", opened) }
            compose.onNodeWithText("Quest Completed").assertIsDisplayed()
            capture("quest-completed-narrow")
            compose.onRoot().performKeyInput { pressKey(Key.Escape) }
            compose.runOnIdle { assertEquals(emptyList<QuestPresentationState.Important>(), presentations.important.toList()) }
            compose.waitUntil(5000) { audio.commands.filterIsInstance<AudioCommand.Play>().any { it.cueId == "quest_complete_stinger" } }
            compose.runOnIdle { assertEquals(1, audio.commands.filterIsInstance<AudioCommand.Play>().count { it.cueId == "quest_new_stinger" }) }
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun updatesRenderTypedObjectiveRolesAndDismissWithoutImportantCard() {
        val directory = Files.createTempDirectory("starborn-quest-update-").toFile()
        val services = DesktopAppServices(directory, RecordingAudio())
        val presentations = QuestPresentationState()
        try {
            compose.mainClock.autoAdvance = false
            compose.setContent { DesktopStarbornTheme { Box(Modifier.size(1280.dp, 720.dp)) {
                DesktopQuestPresentation(services, presentations, false) {}
            } } }
            compose.runOnIdle { presentations.accept(UiEvent.ShowQuestBanner(QuestBannerType.PROGRESS, "a", "Authored quest", listOf(
                QuestObjectiveStatus("done", "Authored completed objective", true, QuestObjectiveRole.JUST_COMPLETED),
                QuestObjectiveStatus("next", "Authored next objective", false, QuestObjectiveRole.NEXT)), 2)) }
            compose.mainClock.advanceTimeBy(300)
            compose.onNodeWithText("Quest Updated").assertIsDisplayed()
            compose.onNodeWithText("Completed: Authored completed objective", substring = true).assertExists()
            compose.onNodeWithText("Next: Authored next objective", substring = true).assertExists()
            compose.onNodeWithText("+2 more objectives").assertExists()
            capture("quest-updated")
            compose.mainClock.advanceTimeBy(3500)
            compose.runOnIdle { assertTrue(presentations.updates.isEmpty()) }
            compose.runOnIdle { presentations.accept(UiEvent.ShowQuestBanner(QuestBannerType.PROGRESS, "b", "Another authored quest")) }
            compose.mainClock.advanceTimeBy(300)
            compose.onNodeWithContentDescription("Dismiss quest update").performClick()
            compose.mainClock.advanceTimeBy(250)
            compose.waitForIdle()
            compose.runOnIdle { assertTrue(presentations.updates.isEmpty()) }
            for (left in listOf(true, false)) {
                compose.runOnIdle { presentations.accept(UiEvent.ShowQuestBanner(QuestBannerType.PROGRESS,
                    "swipe-$left", "Swipe this update")) }
                compose.mainClock.advanceTimeBy(300)
                compose.onNodeWithText("Quest Updated").performTouchInput {
                    if (left) swipeLeft() else swipeRight()
                }
                compose.mainClock.advanceTimeBy(400)
                compose.runOnIdle { assertTrue("Swipe dismisses the update", presentations.updates.isEmpty()) }
            }
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun passiveNoticesWaitForDialogueAndResumeWithTheirFullReadingTime() {
        val directory = Files.createTempDirectory("starborn-notice-timers-").toFile()
        val services = DesktopAppServices(directory, RecordingAudio())
        val presentations = QuestPresentationState()
        val blocked = mutableStateOf(true)
        try {
            compose.mainClock.autoAdvance = false
            compose.setContent { DesktopStarbornTheme { Box(Modifier.size(1280.dp, 720.dp)) {
                DesktopQuestPresentation(services, presentations, blocked.value) {}
            } } }
            compose.runOnIdle {
                presentations.accept(UiEvent.ShowQuestBanner(QuestBannerType.PROGRESS, "a", "Deferred update"))
                presentations.accept(UiEvent.ShowToast("credits", "+25 Credits"))
            }
            compose.mainClock.advanceTimeBy(10000)
            compose.onNodeWithText("Deferred update").assertDoesNotExist()
            compose.onNodeWithText("+25 Credits").assertDoesNotExist()
            compose.runOnIdle { assertEquals(1, presentations.updates.size); assertEquals(1, presentations.toasts.size); blocked.value = false }
            compose.mainClock.advanceTimeBy(350)
            compose.onNodeWithText("Deferred update").assertIsDisplayed()
            compose.onNodeWithText("+25 Credits").assertDoesNotExist()
            compose.runOnIdle { blocked.value = true }
            compose.mainClock.advanceTimeBy(10000)
            compose.runOnIdle { assertEquals(1, presentations.updates.size); blocked.value = false }
            compose.mainClock.advanceTimeBy(350)
            compose.onNodeWithText("Deferred update").assertIsDisplayed()
            compose.mainClock.advanceTimeBy(3500)
            compose.onNodeWithText("+25 Credits").assertIsDisplayed()
            compose.runOnIdle { blocked.value = true }
            compose.mainClock.advanceTimeBy(10000)
            compose.runOnIdle { assertEquals(1, presentations.toasts.size); blocked.value = false }
            compose.mainClock.advanceTimeBy(500)
            compose.onNodeWithText("+25 Credits").assertIsDisplayed()
            compose.mainClock.advanceTimeBy(2000)
            compose.runOnIdle { assertTrue(presentations.toasts.isEmpty()) }
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun journalUsesSharedObjectiveStateAndExpandsQuestBackground() {
        val directory = Files.createTempDirectory("starborn-journal-parity-").toFile()
        val services = DesktopAppServices(directory, RecordingAudio())
        try {
            services.startNewGame()
            val quest = services.questRepository.questById("w1_mq01")!!
            val task = quest.stages.first().tasks.first()
            services.sessionStore.setQuestTaskCompleted(quest.id, task.id, true)
            compose.setContent { DesktopStarbornTheme { Box(Modifier.size(1280.dp, 800.dp)) {
                DesktopFieldMenuContent(services, initialTab = DesktopMenuTab.JOURNAL, onOpenFieldKit = {}, onReturnToTitle = {}, onDismiss = {})
            } } }
            compose.waitUntil(10_000) { services.exploration.uiState.value.questDetail?.id == quest.id }
            compose.onNodeWithText(quest.summary).assertDoesNotExist()
            compose.onNodeWithText("Quest background").performScrollTo().performClick()
            compose.onNodeWithText(quest.summary).assertExists()
            compose.onNodeWithContentDescription("Completed objective: ${task.text}").assertExists()
            compose.onNodeWithText("Rewards").assertExists()
            capture("journal-quest-details")
            compose.onNodeWithText("Hide quest background").performScrollTo().performClick()
            compose.onNodeWithText(quest.summary).assertDoesNotExist()
            // Fishing records are covered through the cargo terminal in DesktopAstraCatchLogTest.
            compose.onNodeWithText("Fishing").assertDoesNotExist()
            compose.onNodeWithText("History").performClick()
            compose.waitForIdle()
            compose.runOnIdle { assertNull(services.exploration.uiState.value.questDetail) }
        } finally { services.close(); directory.deleteRecursively() }
    }

    private fun capture(name: String) {
        val target = File("build/reports/desktop/screenshots/$name.png")
        target.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", target)
    }
}
