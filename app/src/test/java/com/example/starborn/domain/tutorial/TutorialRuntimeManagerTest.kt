package com.example.starborn.domain.tutorial

import com.example.starborn.domain.prompt.TutorialPrompt
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
        assertEquals("bag_basics", prompt?.entry?.key)

        promptManager.dismissCurrent()
        advanceUntilIdle()

        assertTrue("Completion callback should run after final step", completed)
        val state = sessionStore.state.value
        assertTrue(state.tutorialSeen.contains("bag_basics"))
    }
}
