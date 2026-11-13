package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

/** Exercise the favor through the same NPC choices, room controls and save slots as play. */
class CharlesFavorPublicTest {
    @get:Rule val compose = createComposeRule()
    private val asked = "ms_charles_analytics_favor_asked"
    private val audited = "ms_charles_public_queue_verified"
    private val completed = "ms_charles_analytics_favor_completed"

    private fun runWith(milestones: Set<String> = emptySet(), materials: Boolean = true, block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("charles-public-favor").toFile()
        val s = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(compose.runOnIdle { s.startDebugScenario("tut_npc_dialogue") })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            compose.runOnIdle { s.sessionStore.restore(s.sessionStore.state.value.copy(
                worldId = "world_5", hubId = "hub_9_orbital_ring", roomId = "orbital_customs_lounge",
                activeQuests = emptySet(), completedQuests = emptySet(), completedEvents = emptySet(),
                completedMilestones = milestones, questTasksCompleted = emptyMap(), roomStates = emptyMap(),
                inventory = if (materials) mapOf("circuit_board" to 2, "focusing_lens" to 1) else emptyMap()
            )) }
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == "orbital_customs_lounge" }
            repeat(25) { if (s.exploration.uiState.value.activeDialogue != null) {
                compose.runOnIdle { s.exploration.advanceDialogue() }; compose.waitForIdle()
            } }
            dismiss(s)
            s.promptManager.discardMatching { true }
            block(s)
        } finally { compose.runOnIdle { s.close() }; dir.deleteRecursively() }
    }
    private fun dismiss(s: DesktopAppServices) {
        while (s.exploration.uiState.value.narrationPrompt != null) {
            compose.runOnIdle { s.exploration.dismissNarration() }; compose.waitForIdle()
        }
    }
    private fun talk(s: DesktopAppServices) {
        compose.runOnIdle { s.exploration.onNpcInteraction("analyst_charles") }
        compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue?.line?.id == "analyst_charles_default_talk" }
    }
    private fun choice(s: DesktopAppServices, id: String) {
        assertTrue("Choice available: $id", s.exploration.uiState.value.dialogueChoices.any { it.id == id })
        compose.runOnIdle { s.exploration.onDialogueChoiceSelected(id) }; compose.waitForIdle()
    }
    private fun finishReply(s: DesktopAppServices) {
        repeat(10) {
            if (s.exploration.uiState.value.dialogueChoices.isNotEmpty()) return
            compose.runOnIdle { s.exploration.advanceDialogue() }; compose.waitForIdle()
        }
        fail("Charles reply did not return to choices")
    }
    private fun leave(s: DesktopAppServices) {
        choice(s,"analyst_charles_default_talk_leave")
        compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue == null }
    }
    private fun gate(s: DesktopAppServices) {
        val action = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.name == "biometric gates" }
        compose.runOnIdle { s.exploration.onActionSelected(action) }; compose.waitForIdle()
        compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
    }

    @Test fun preparedPlayerEarnsTrustBeforeTurningInCarriedMaterialsAndRewardCannotRepeatAfterReload() = runWith { s ->
        val before = s.sessionStore.state.value
        gate(s)
        assertFalse(audited in s.sessionStore.state.value.completedMilestones)
        dismiss(s)
        talk(s)
        assertFalse(s.exploration.uiState.value.dialogueChoices.any { it.id == "charles_opt_favor_turnin" })
        choice(s,"charles_opt_ethics")
        assertTrue(s.exploration.uiState.value.activeDialogue!!.line.text.contains("Thorne"))
        assertTrue(s.exploration.uiState.value.activeDialogue!!.line.text.contains("Vale"))
        finishReply(s)
        choice(s,"charles_opt_favor_offer"); finishReply(s)
        assertTrue(asked in s.sessionStore.state.value.completedMilestones)
        assertFalse(s.exploration.uiState.value.dialogueChoices.any { it.id == "charles_opt_favor_turnin" })
        leave(s)
        gate(s)
        assertTrue(s.exploration.uiState.value.narrationPrompt!!.message.contains("leaving passenger records sealed"))
        compose.waitUntil(5000) { audited in s.sessionStore.state.value.completedMilestones }
        assertEquals(before.inventory,s.sessionStore.state.value.inventory)
        assertEquals(before.playerXp,s.sessionStore.state.value.playerXp)
        dismiss(s)
        assertTrue(s.saveManager.saveGame(8,s.sessionStore.state.value))
        assertTrue(compose.runOnIdle { s.loadSlot(8) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == "orbital_customs_lounge" }
        dismiss(s)
        compose.waitUntil(5000) { s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().any { it.name == "biometric gates" && it.completedInspection } }
        talk(s); choice(s,"charles_opt_favor_turnin"); finishReply(s)
        compose.waitUntil(5000) { completed in s.sessionStore.state.value.completedMilestones }
        val rewarded = s.sessionStore.state.value
        assertEquals(0,rewarded.inventory["circuit_board"] ?: 0)
        assertEquals(0,rewarded.inventory["focusing_lens"] ?: 0)
        assertEquals(1,rewarded.inventory["charles_heuristic_slate"])
        assertEquals(before.playerXp + 400,rewarded.playerXp)
        assertEquals(before.playerCredits + 500,rewarded.playerCredits)
        assertFalse(s.exploration.uiState.value.dialogueChoices.any { it.id == "charles_opt_favor_turnin" || it.id == "charles_opt_favor_offer" })
        leave(s)
        assertTrue(s.saveManager.saveGame(8,rewarded))
        assertTrue(compose.runOnIdle { s.loadSlot(8) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
        dismiss(s)
        talk(s)
        assertFalse(s.exploration.uiState.value.dialogueChoices.any { it.id == "charles_opt_favor_turnin" })
        leave(s); gate(s); dismiss(s)
        assertEquals(rewarded.inventory,s.sessionStore.state.value.inventory)
        assertEquals(rewarded.playerXp,s.sessionStore.state.value.playerXp)
        assertEquals(rewarded.playerCredits,s.sessionStore.state.value.playerCredits)
    }

    @Test fun previouslyAcceptedFavorCanDoTheAuditWithoutMaterialsAndDoesNotGiveAnEarlyReward() = runWith(setOf(asked),false) { s ->
        val before=s.sessionStore.state.value
        talk(s); choice(s,"charles_opt_favor_status")
        assertTrue(s.exploration.uiState.value.activeDialogue!!.line.text.contains("biometric gates"))
        finishReply(s); leave(s); gate(s)
        compose.waitUntil(5000) { audited in s.sessionStore.state.value.completedMilestones }
        dismiss(s); talk(s)
        assertFalse(s.exploration.uiState.value.dialogueChoices.any { it.id == "charles_opt_favor_turnin" })
        assertEquals(before.inventory,s.sessionStore.state.value.inventory)
        assertEquals(before.playerXp,s.sessionStore.state.value.playerXp)
        assertEquals(before.playerCredits,s.sessionStore.state.value.playerCredits)
        assertTrue(s.sessionStore.state.value.activeQuests.isEmpty())
    }

    @Test fun outerRingUsesTheNewLoopAndDeepRingKeepsItsSurveillanceTrack() = runWith { s ->
        s.audioRouter.reset()
        val outer = s.audioRouter.commandsForRoom("hub_9_orbital_ring", "orbital_executive_dock")
            .filterIsInstance<com.example.starborn.domain.audio.AudioCommand.Play>()
            .single { it.type == com.example.starborn.domain.audio.AudioCueType.MUSIC }
        assertEquals("music_w5_orbital_intrigue", outer.cueId)
        assertTrue(outer.loop)
        assertTrue(outer.fadeMs > 0)
        val deep = s.audioRouter.commandsForRoom("hub_10_deep_ring", "deep_mainframe_nave")
            .filterIsInstance<com.example.starborn.domain.audio.AudioCommand.Play>()
            .single { it.type == com.example.starborn.domain.audio.AudioCueType.MUSIC }
        assertEquals("music_w5_security_hub", deep.cueId)
        assertTrue(s.audioCatalog.tracks.any { it.id == outer.cueId })
    }

    @Test fun oldCompletedFavorNeedsNoNewAuditAndCannotOfferOrPayAgain() = runWith(setOf(asked,completed)) { s ->
        val before=s.sessionStore.state.value
        assertTrue(s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.name == "biometric gates" }.completedInspection)
        gate(s); dismiss(s); talk(s)
        assertFalse(s.exploration.uiState.value.dialogueChoices.any { it.id.startsWith("charles_opt_favor_") })
        assertEquals(before.inventory,s.sessionStore.state.value.inventory)
        assertEquals(before.playerXp,s.sessionStore.state.value.playerXp)
        assertFalse(audited in s.sessionStore.state.value.completedMilestones)
    }
}
