package com.example.starborn.domain.tutorial

import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.prompt.ItemGrantedPrompt
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.tutorial.TutorialScript
import com.example.starborn.domain.tutorial.TutorialScriptStep
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TutorialRuntimeManagerTest {

    private val dispatcher = StandardTestDispatcher()

    @Test fun shippedDelayedEventScriptsKeepOrderForFastAndSlowReaders() = runTest(dispatcher) {
        val scripts = TutorialScriptRepository(com.example.starborn.data.assets.AssetJsonReader(
            com.example.starborn.core.platform.DesktopAssetProvider(),
            com.example.starborn.core.MoshiProvider.instance
        ))
        for (scriptId in listOf("world2_debuffs", "link_unlock")) {
            for (slowReader in listOf(false, true)) {
                val prompts = UIPromptManager()
                val manager = TutorialRuntimeManager(GameSessionStore(), prompts, scripts, backgroundScope)
                val script = requireNotNull(scripts.script(scriptId))
                assertEquals(2, script.steps.size)
                val waitMs = requireNotNull(script.steps[1].delayMs)
                var completions = 0
                manager.playScript(scriptId) { completions++ }
                testScheduler.runCurrent()
                assertEquals("${scriptId}_step_0", prompts.state.value.current?.id)
                if (slowReader) {
                    testScheduler.advanceTimeBy(waitMs + 1)
                    testScheduler.runCurrent()
                    assertEquals("${scriptId}_step_0", prompts.state.value.current?.id)
                }
                prompts.dismissCurrent()
                if (!slowReader) {
                    assertNull(prompts.state.value.current)
                    testScheduler.advanceTimeBy(waitMs + 1)
                    testScheduler.runCurrent()
                }
                assertEquals("${scriptId}_step_1", prompts.state.value.current?.id)
                assertEquals(0, completions)
                prompts.dismissCurrent()
                assertEquals(1, completions)
                assertTrue(manager.hasCompleted(scriptId))
                assertNull(prompts.state.value.current)
            }
        }
    }

    @Test fun cancellingTutorialsPreservesRewardsAndDoesNotCompleteSkippedSteps() = runTest(dispatcher) {
        val store = GameSessionStore()
        val prompts = UIPromptManager()
        val manager = TutorialRuntimeManager(store, prompts, null, backgroundScope)
        var completions = 0
        manager.showOnce("active", "Active", onDismiss = { completions++ })
        manager.showOnce("queued", "Queued", onDismiss = { completions++ })
        val reward = ItemGrantedPrompt("Medkit", 1)
        prompts.enqueue(reward)
        manager.showOnce("delayed", "Delayed", delayMs = 100)
        manager.cancelAllTutorials()
        testScheduler.advanceTimeBy(200)
        testScheduler.runCurrent()
        assertEquals(reward, prompts.state.value.current)
        assertTrue(prompts.state.value.queue.isEmpty())
        assertEquals(0, completions)
        assertTrue(store.state.value.tutorialCompleted.isEmpty())
        prompts.dismissCurrent()
        assertNull(prompts.state.value.current)
        manager.showOnce("active", "Can return after re-enabling")
        assertEquals("active", prompts.state.value.current?.id)
    }

    @Test fun cancellingQueuedTutorialsPreservesActiveReward() = runTest(dispatcher) {
        val prompts = UIPromptManager()
        val manager = TutorialRuntimeManager(GameSessionStore(), prompts, null, backgroundScope)
        val reward = ItemGrantedPrompt("Medkit", 1)
        prompts.enqueue(reward)
        manager.showOnce("queued", "Queued")
        manager.cancelAllTutorials()
        assertEquals(reward, prompts.state.value.current)
        prompts.dismissCurrent()
        assertNull(prompts.state.value.current)
    }

    @Test fun completingScriptDiscardsQueuedStepsButKeepsUnrelatedPrompt() = runTest(dispatcher) {
        val store = GameSessionStore()
        val prompts = UIPromptManager()
        val manager = TutorialRuntimeManager(store, prompts, null, backgroundScope)
        var unseenDismissals = 0
        manager.enqueue(TutorialEntry(key = "first", context = null, message = "First", metadata = mapOf("script_id" to "bag")))
        manager.enqueue(TutorialEntry(key = "second", context = null, message = "Second", metadata = mapOf("script_id" to "bag")),
            onDismiss = { unseenDismissals++ })
        manager.enqueue(TutorialEntry(key = "other", context = null, message = "Other"))
        manager.completeAndDismiss("BAG")
        assertEquals("other", prompts.state.value.current?.id)
        assertTrue(prompts.state.value.queue.isEmpty())
        assertEquals(0, unseenDismissals)
        assertTrue(manager.hasCompleted("bag"))
    }

    @Test fun completingQueuedTutorialDoesNotDismissUnrelatedCurrentPrompt() = runTest(dispatcher) {
        val store = GameSessionStore()
        val prompts = UIPromptManager()
        val manager = TutorialRuntimeManager(store, prompts, null, backgroundScope)
        manager.enqueue(TutorialEntry(key = "other", context = null, message = "Keep reading"))
        manager.enqueue(TutorialEntry(key = "finished", context = null, message = "Obsolete"))
        manager.markCompleted("finished")
        assertEquals("other", prompts.state.value.current?.id)
        assertTrue(prompts.state.value.queue.isEmpty())
        prompts.dismissCurrent()
        manager.showOnce("finished", "Must not return")
        assertNull(prompts.state.value.current)
    }

    @Test
    fun showOnceEnqueuesSinglePrompt() = runTest(dispatcher) {
        val sessionStore = GameSessionStore()
        val promptManager = UIPromptManager()
        val manager = TutorialRuntimeManager(sessionStore, promptManager, scripts = null, scope = backgroundScope)

        manager.showOnce(
            entry = TutorialEntry(
                key = "light_switch",
                context = "Nova's House",
                message = "Tap the light switch to brighten the room."
            )
        )
        advanceUntilIdle()

        manager.showOnce(
            entry = TutorialEntry(
                key = "light_switch",
                context = "Nova's House",
                message = "Tap the light switch to brighten the room."
            )
        )
        advanceUntilIdle()

        val prompt = promptManager.state.value.current
        assertEquals("light_switch", prompt?.id)
        assertEquals(0, promptManager.state.value.queue.size)

        manager.markCompleted("light_switch")
        manager.showOnce(
            entry = TutorialEntry(
                key = "light_switch",
                context = "Nova's House",
                message = "Tap the light switch to brighten the room."
            )
        )
        advanceUntilIdle()

        assertNull(promptManager.state.value.queue.firstOrNull { it.id == "light_switch" })
    }

    @Test
    fun showOnceInvokesDismissCallback() = runTest(dispatcher) {
        val sessionStore = GameSessionStore()
        val promptManager = UIPromptManager()
        val manager = TutorialRuntimeManager(sessionStore, promptManager, scripts = null, scope = backgroundScope)
        var dismissed = false

        manager.showOnce(
            entry = TutorialEntry(
                key = "swipe_hint",
                context = "Movement",
                message = "Swipe to explore."
            ),
            onDismiss = { dismissed = true }
        )
        advanceUntilIdle()

        assertTrue(promptManager.state.value.current is com.example.starborn.domain.prompt.TutorialPrompt)
        assertTrue(!dismissed)

        promptManager.dismissCurrent()
        assertTrue(dismissed)
    }

    @Test
    fun runtimeStateReflectsPersistedSessionData() = runTest(dispatcher) {
        val sessionStore = GameSessionStore()
        sessionStore.markTutorialSeen("light_switch")
        sessionStore.markTutorialCompleted("light_switch")
        sessionStore.markTutorialRoomVisited("town_9")
        val promptManager = UIPromptManager()

        val manager = TutorialRuntimeManager(sessionStore, promptManager, scripts = null, scope = backgroundScope)

        val state = manager.runtimeState.first { it.seen.contains("light_switch") }
        assertTrue(state.seen.contains("light_switch"))
        assertTrue(state.completed.contains("light_switch"))
        assertTrue(state.roomsVisited.contains("town_9"))
    }

    @Test
    fun playScriptQueuesStepsAndInvokesCompletion() = runTest(dispatcher) {
        val sessionStore = GameSessionStore()
        val promptManager = UIPromptManager()
        var completed = false
        val scripts = mock<TutorialScriptRepository>()
        whenever(scripts.script("bag_basics")).thenReturn(
            TutorialScript(
                id = "bag_basics",
                steps = listOf(
                    TutorialScriptStep(
                        key = "bag_basics_intro",
                        message = "Open your bag via the overlay.",
                        context = "Inventory"
                    ),
                    TutorialScriptStep(
                        message = "Use filters to inspect key items.",
                        context = "Inventory"
                    )
                )
            )
        )
        val manager = TutorialRuntimeManager(sessionStore, promptManager, scripts, scope = backgroundScope)

        val scheduled = manager.playScript("bag_basics") { completed = true }
        assertTrue(scheduled)
        advanceUntilIdle()

        var prompt = promptManager.state.value.current as? TutorialPrompt
        assertEquals("bag_basics_intro", prompt?.entry?.key)

        promptManager.dismissCurrent()
        advanceUntilIdle()

        prompt = promptManager.state.value.current as? TutorialPrompt
        assertEquals("bag_basics_step_1", prompt?.entry?.key)

        promptManager.dismissCurrent()
        advanceUntilIdle()

        assertTrue("Completion callback should run after final step", completed)
        val state = sessionStore.state.value
        assertTrue(state.tutorialSeen.contains("bag_basics"))
        assertTrue(state.tutorialCompleted.contains("bag_basics_intro"))
        assertTrue(state.tutorialCompleted.contains("bag_basics_step_1"))
        assertTrue(state.tutorialCompleted.contains("bag_basics"))

        assertTrue(manager.playScript("bag_basics"))
        advanceUntilIdle()
        assertNull(promptManager.state.value.current)
    }
}
