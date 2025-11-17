package com.example.starborn.domain.milestone

import com.example.starborn.data.repository.MilestoneRepository
import com.example.starborn.domain.model.MilestoneDefinition
import com.example.starborn.domain.model.MilestoneEffects
import com.example.starborn.domain.model.MilestoneExitUnlock
import com.example.starborn.domain.model.MilestoneTrigger
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.session.GameSessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class MilestoneRuntimeManagerTest {

    @Test
    fun unlockExitEffectUpdatesSessionStore() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val sessionStore = GameSessionStore()
        val promptManager = UIPromptManager()
        val definition = MilestoneDefinition(
            id = "ms_unlock_exit",
            trigger = MilestoneTrigger(type = "event", eventId = "evt_unlock"),
            effects = MilestoneEffects(
                unlockExits = listOf(MilestoneExitUnlock(roomId = "room_a", direction = "north"))
            )
        )
        val repository = mock<MilestoneRepository> {
            on { definitionsForTrigger("event", "evt_unlock") } doReturn listOf(definition)
            on { milestoneById(any()) } doReturn definition
        }

        val manager = MilestoneRuntimeManager(
            repository = repository,
            sessionStore = sessionStore,
            promptManager = promptManager,
            scope = scope,
            applyEffects = { effects ->
                effects.unlockAreas.orEmpty().forEach { sessionStore.unlockArea(it) }
                effects.unlockAbilities.orEmpty().forEach { sessionStore.unlockSkill(it) }
                effects.unlockExits.orEmpty().forEach { exit ->
                    sessionStore.unlockExit(exit.roomId, exit.direction)
                }
            }
        )

        manager.onEventCompleted("evt_unlock")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(sessionStore.state.value.unlockedExits.contains("room_a::north"))
        scope.cancel()
    }
}
