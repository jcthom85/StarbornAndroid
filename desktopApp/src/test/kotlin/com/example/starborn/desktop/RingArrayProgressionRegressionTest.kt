package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import kotlinx.coroutines.runBlocking

class RingArrayProgressionRegressionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun actualControllerCanFinishTheRingArrayAndWakeOrion() {
        val directory = Files.createTempDirectory("ring-array-regression").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(services.startDebugScenario("tut_npc_dialogue"))
            val runtime = services.exploration
            compose.waitUntil(15000) { !runtime.uiState.value.isLoading }
            val prior = services.sessionStore.state.value
            services.sessionStore.restore(prior.copy(roomId = "sector9_stasis_ring_array",
                activeQuests = setOf("w2_mq03"), questTasksCompleted = prior.questTasksCompleted - "w2_mq03",
                completedMilestones = prior.completedMilestones + setOf("ms_w2_murals_read", "ms_w2_stasis_pod_read", "ms_w2_stasis_overview_read", "ms_w2_coolant_stabilized")))
            compose.waitUntil(10000) { runtime.uiState.value.currentRoom?.id == "sector9_stasis_ring_array" }
            val console = runtime.uiState.value.actions.single { it.name == "ring console" }
            runtime.onActionSelected(console)
            compose.waitUntil(5000) { runtime.uiState.value.activeDialogue != null }
            assertEquals("w2_stasis_entry", runtime.uiState.value.activeDialogue?.line?.id)
            runtime.advanceDialogue()
            for (step in 1..3) {
                assertEquals("w2_stasis_$step", runtime.uiState.value.activeDialogue?.line?.id)
                runtime.onDialogueChoiceSelected("correct")
            }
            assertTrue("ms_w2_stasis_awake" in services.sessionStore.state.value.completedMilestones)
            assertTrue("align_stasis_rings" in services.sessionStore.state.value.questTasksCompleted["w2_mq03"].orEmpty())
            assertNull(runtime.uiState.value.activeDialogue)
        } finally { services.close(); directory.deleteRecursively() }
    }
}
