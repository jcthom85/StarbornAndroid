package com.example.starborn.di

import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class DialogueTriggerHandlingTest {

    @Test
    fun setMilestoneTriggerRunsMilestoneCallback() {
        val sessionStore = GameSessionStore()
        val questRuntimeManager = mock<QuestRuntimeManager>()
        var callbackWasCalled = false

        handleDialogueTrigger(
            trigger = "set_milestone:ms_dialogue_unlock",
            sessionStore = sessionStore,
            questRuntimeManager = questRuntimeManager,
            onMilestoneSet = { milestone ->
                if (milestone == "ms_dialogue_unlock") {
                    callbackWasCalled = true
                    sessionStore.unlockSkill("nova_hydraulic_kick")
                }
            }
        )

        val state = sessionStore.state.value
        assertTrue(state.completedMilestones.contains("ms_dialogue_unlock"))
        assertTrue(callbackWasCalled)
        assertTrue(state.unlockedSkills.contains("nova_hydraulic_kick"))
    }
}
