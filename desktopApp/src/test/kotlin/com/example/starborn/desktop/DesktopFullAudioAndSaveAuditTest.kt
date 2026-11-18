package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopFieldMenuContent
import com.example.starborn.desktop.ui.DesktopMenuTab
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopFullAudioAndSaveAuditTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAudioSettingsAndPersistence() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_audit_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            services.startNewGame()

            // 1. Audio Driver & Router Tests via AudioCommand
            services.audioDriver.execute(AudioCommand.Play(AudioCueType.MUSIC, "music_w1_homestead_explore", loop = true))
            services.audioDriver.execute(AudioCommand.Play(AudioCueType.UI, "sfx_ui_button_click", loop = false))
            listOf(
                "tinkering_wrench", "turn_start", "action_retreat", "sfx_button_toggle",
                "sfx_rest_chime", "sfx_bunk_light_on", "sfx_bunk_light_off",
                "sfx_loot_cache", "sfx_forage_gather", "sfx_datapad_read",
                "ui_title_start", "sfx_w3_shield_anchor"
            ).forEach { eventId ->
                assertFalse("UI event $eventId should resolve to a command", services.audioRouter.commandsForUi(eventId).isEmpty())
            }
            listOf(
                "retreat", "wpn_nova_resonance_shot", "wpn_zeke_body_impact",
                "wpn_gh0st_wire_slash", "wpn_orion_bowl_resonance"
            ).forEach { battleId ->
                assertFalse("Battle event $battleId should resolve to a command", services.audioRouter.commandsForBattle(battleId).isEmpty())
            }
            assertTrue(services.audioRouter.commandsForUi("undefined-event").isEmpty())
            services.audioDriver.setUserGain(AudioCueType.MUSIC, 0.75f)

            // 2. Save / Stasis Slot Tests
            services.sessionStore.setRoom("pit_jed_bunk")
            services.sessionStore.restore(services.sessionStore.state.value.copy(playerCredits = 250))
            val stateToSave = services.sessionStore.state.value

            val saveSuccess = services.saveManager.saveGame(1, stateToSave, "Jed's Bunk")
            assertTrue("Save to Slot 1 must succeed", saveSuccess)

            val meta = services.saveManager.getSlotMetadata(1)
            assertNotNull("Slot 1 metadata must not be null", meta)
            assertEquals("Jed's Bunk", meta?.roomTitle)
            assertEquals(250, meta?.credits)

            // Modify live session state
            services.sessionStore.restore(services.sessionStore.state.value.copy(playerCredits = 0, roomId = "pit_pod_row"))

            // Restore from Slot 1
            val restored = services.saveManager.loadGame(1)
            assertNotNull("Restored state from Slot 1 must not be null", restored)
            services.sessionStore.restore(restored!!)
            assertEquals("pit_jed_bunk", services.sessionStore.state.value.roomId)
            assertEquals(250, services.sessionStore.state.value.playerCredits)

            // 3. Render Settings & Stasis Tab Snapshot
            composeTestRule.setContent {
                DesktopFieldMenuContent(
                    services = services,
                    initialTab = DesktopMenuTab.SETTINGS,
                    currentRoomTitle = "Jed's Bunk",
                    onOpenFieldKit = {},
                    onReturnToTitle = {},
                    onDismiss = {}
                )
            }
            composeTestRule.waitForIdle()

            val artifactsDir = File(System.getProperty("user.home"), ".gemini/antigravity-cli/brain/03813eca-b59d-44ca-9f2d-90a0e5b8e32f")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_settings_tab.png"))

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
