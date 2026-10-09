package com.example.starborn.feature.exploration

import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.feature.exploration.viewmodel.SystemTutorialCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemTutorialCoordinatorTest {
    @Test fun disablingDuringDelaySkipsHintAndReleasesEventOnce() = runTest {
        val prompts = UIPromptManager()
        val store = GameSessionStore()
        val runtime = TutorialRuntimeManager(store, prompts, null, backgroundScope)
        var enabled = true
        var continuations = 0
        val coordinator = SystemTutorialCoordinator(runtime, backgroundScope) { enabled }
        assertTrue(coordinator.play("delayed_scene", "Room", 100) { continuations++ })
        testScheduler.runCurrent()
        enabled = false
        testScheduler.advanceTimeBy(101)
        testScheduler.runCurrent()
        assertNull(prompts.state.value.current)
        assertEquals(1, continuations)
        assertTrue(store.state.value.tutorialSeen.isEmpty())
        assertTrue(store.state.value.tutorialCompleted.isEmpty())
    }

    @Test fun enabledDelayedHintWaitsForDismissalBeforeContinuing() = runTest {
        val prompts = UIPromptManager()
        val runtime = TutorialRuntimeManager(GameSessionStore(), prompts, null, backgroundScope)
        var continuations = 0
        val coordinator = SystemTutorialCoordinator(runtime, backgroundScope)
        coordinator.play("delayed_scene", "Room", 100) { continuations++ }
        testScheduler.runCurrent()
        assertNull(prompts.state.value.current)
        testScheduler.advanceTimeBy(101)
        testScheduler.runCurrent()
        assertEquals("delayed_scene", prompts.state.value.current?.id)
        assertEquals(0, continuations)
        prompts.dismissCurrent()
        assertEquals(1, continuations)
    }
}
