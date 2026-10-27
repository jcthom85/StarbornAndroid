package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.prompt.*
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.feature.exploration.viewmodel.MenuTab
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopJedPopupFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun jedBunkWorkshopRepairAndQuestHandoffKeepOneForegroundAtATime() {
        val directory = Files.createTempDirectory("starborn-jed-popups-").toFile()
        val s = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(true) }
            assertTrue(s.startDebugScenario("tut_npc_dialogue"))
            val base = s.sessionStore.state.value
            s.sessionStore.restore(base.copy(roomId = "pit_jed_bunk", activeQuests = setOf("w1_mq01"),
                trackedQuestId = "w1_mq01", completedQuests = emptySet(),
                completedMilestones = setOf("ms_w1_mq01_safety_fault_inspected"), completedEvents = emptySet(),
                questStageById = mapOf("w1_mq01" to "report_to_jed"),
                questTasksCompleted = mapOf("w1_mq01" to setOf("turn_on_bunk_light", "inspect_safety_fault", "leave_sleeping_level", "find_jed")),
                inventory = emptyMap(), unlockedWeapons = emptySet(), unlockedArmors = emptySet(),
                equippedWeapons = emptyMap(), equippedArmors = emptyMap(), tutorialCompleted = emptySet(), tutorialSeen = emptySet()))
            s.inventoryService.restore(emptyMap())
            val runtime = s.exploration
            compose.waitUntil(10000) { runtime.uiState.value.currentRoom?.id == "pit_jed_bunk" && !runtime.uiState.value.isLoading }
            s.questPresentations.clear()
            s.promptManager.discardMatching { true }
            compose.setContent { DesktopStarbornTheme(s) {
                Box(Modifier.size(1280.dp, 800.dp)) { DesktopExplorationScreen(s, {}, {}, {}, {}, {}, {}) }
            } }
            fun capture(name: String) {
                val file = File("build/reports/jed-popup-flow/$name.png").apply { parentFile.mkdirs() }
                ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", file)
            }
            fun dialogue(expected: String) {
                compose.waitUntil(5000) { runtime.uiState.value.activeDialogue?.line?.id == expected }
                assertEquals(ExplorationNotice.NONE, explorationNotice(runtime.uiState.value))
                capture(expected)
                var lines = 0
                while (runtime.uiState.value.activeDialogue != null && lines++ < 20) {
                    assertEquals(ExplorationNotice.NONE, explorationNotice(runtime.uiState.value))
                    compose.runOnIdle { runtime.advanceDialogue() }
                }
                assertTrue("Dialogue must finish", lines < 20)
            }
            // Acknowledge notices one at a time; never discard the rewards or tutorials under test.
            fun notices() {
                var steps = 0
                while (steps++ < 50) {
                    compose.waitForIdle()
                    val ui = runtime.uiState.value
                    when {
                        ui.activeDialogue != null -> error("Unexpected conversation ${ui.activeDialogue?.line?.id}")
                        ui.levelUpPrompt != null -> compose.runOnIdle { runtime.dismissLevelUpPrompt() }
                        ui.narrationPrompt != null -> compose.runOnIdle { runtime.dismissNarration() }
                        ui.eventAnnouncement != null -> compose.runOnIdle { runtime.dismissEventAnnouncement() }
                        ui.prompt != null -> compose.runOnIdle { runtime.dismissPrompt(ui.prompt?.id) }
                        s.questPresentations.important.isNotEmpty() -> compose.runOnIdle { s.questPresentations.dismissImportant() }
                        else -> return
                    }
                }
                error("Notices did not drain")
            }
            compose.runOnIdle { runtime.onNpcInteraction("jed") }
            dialogue("jed_w1_mq01_intro_1")
            assertTrue("ms_w1_mq01_jed_talked" in s.sessionStore.state.value.completedMilestones)
            val pistolId = s.itemRepository.findItem("mining_pistol")!!.id
            assertTrue("Starter pistol must be owned: ${s.sessionStore.state.value.unlockedWeapons}",
                pistolId in s.sessionStore.state.value.unlockedWeapons || s.inventoryService.hasItem(pistolId, 1))
            assertEquals(1, s.inventoryService.snapshot()["cryo_inductor"])
            assertEquals(2, s.inventoryService.snapshot()["scrap_metal"])
            compose.waitUntil(5000) { runtime.uiState.value.prompt != null }
            assertTrue("Rewards should precede the equipment offer", runtime.uiState.value.prompt is ItemGrantedPrompt)
            capture("starter-kit-notices")
            notices()
            s.sessionStore.setMilestone("ms_w1_mq01_loader_cleared")
            s.sessionStore.setRoom("workshop_floor")
            compose.waitUntil(10000) { runtime.uiState.value.currentRoom?.id == "workshop_floor" }
            dialogue("jed_w1_mq01_workshop_brief_1")
            compose.waitUntil(5000) { runtime.uiState.value.isTinkeringTutorialActive }
            notices()
            compose.runOnIdle { runtime.openMenuOverlay(MenuTab.FIELD_KIT, showTutorial = false) }
            compose.waitUntil(5000) { runtime.uiState.value.isMenuOverlayVisible }
            assertNull(runtime.uiState.value.activeDialogue)
            // The crafting UI owns recipe execution; this is its real result callback.
            s.inventoryService.addItem("functional_cryo_inductor", 1)
            compose.runOnIdle { runtime.onTinkeringCrafted("functional_cryo_inductor") }
            compose.waitUntil(5000) { !runtime.uiState.value.isMenuOverlayVisible }
            dialogue("jed_w1_mq01_repair_done_1")
            assertTrue("ms_w1_mq01_test_briefed" in s.sessionStore.state.value.completedMilestones)
            notices()
            fun action(event: String) {
                compose.runOnIdle { runtime.onActionSelected(GenericAction(event, "generic", actionEvent = event)) }
                compose.waitForIdle()
            }
            action("w1_mq01_patch_flux_liner"); notices()
            action("w1_mq01_confirm_governor"); notices()
            action("w1_mq01_cutter_surge")
            compose.waitUntil(5000) { runtime.uiState.value.cinematic != null }
            assertEquals(ExplorationNotice.NONE, explorationNotice(runtime.uiState.value))
            capture("cutter-surge")
            compose.runOnIdle { runtime.skipCinematic() }
            dialogue("jed_w1_mq02_handoff_1")
            assertTrue("w1_mq01" in s.sessionStore.state.value.completedQuests)
            assertTrue("w1_mq02" in s.sessionStore.state.value.activeQuests)
            assertEquals("w1_mq02", s.sessionStore.state.value.trackedQuestId)
            notices()
            assertNull(runtime.uiState.value.activeDialogue)
            assertNull(runtime.uiState.value.cinematic)
            assertFalse("Dismissing tutorials must not open menus", runtime.uiState.value.isMenuOverlayVisible)
            assertNull(runtime.uiState.value.skillTreeOverlay)
            assertTrue("All quest cards must be acknowledged", s.questPresentations.important.isEmpty())
            assertTrue("Completed quest must not replay old objective banners",
                s.questPresentations.updates.none { it.questId == "w1_mq01" })
            capture("after-wake-up-call")
        } finally { s.close(); directory.deleteRecursively() }
    }
}
