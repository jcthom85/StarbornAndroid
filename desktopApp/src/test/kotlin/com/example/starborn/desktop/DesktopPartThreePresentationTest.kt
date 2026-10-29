package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.prompt.ItemGrantedPrompt
import com.example.starborn.feature.exploration.viewmodel.DialogueUi
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopPartThreePresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun radioCallsAndConsumedItemsUseTheirOwnPortraitAndLabels() {
        val directory = Files.createTempDirectory("starborn-part3-presentation-").toFile()
        val s = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            val calls = listOf("w1_comms_exit", "w1_comms_access", "w1_comms_lift")
            calls.forEach { id ->
                val session = s.dialogueService.startDialogueAt(id)!!
                assertEquals("Zeke", session.current()?.speaker)
                assertEquals("images/npcs/comms_radio.png", session.current()?.portrait)
                assertTrue(session.current()!!.scriptedOnly)
                session.advance()
                assertTrue(session.isFinished())
            }
            val line = s.dialogueService.startDialogueAt(calls.first())!!.current()!!
            var radio by mutableStateOf(true)
            compose.setContent { DesktopStarbornTheme(s) {
                Box(Modifier.size(900.dp, 440.dp), contentAlignment = Alignment.Center) {
                    if (radio) DesktopAuthoredDialogueOverlay(s, DialogueUi(line, line.portrait, null), emptyList(),
                        onAdvance = {}, onChoice = {}, onPlayVoice = {}, revealAllRequest = 1)
                    else DesktopFieldMenuTheme(s) {
                        DesktopItemGrantedBanner(s, ItemGrantedPrompt("Functional Cryo-Inductor", 1,
                            itemId = "functional_cryo_inductor", category = "consumed",
                            description = "Spent during calibration. Removed from inventory."), onDismiss = {})
                    }
                }
            } }
            compose.onNodeWithText("Zeke").assertIsDisplayed()
            compose.waitUntil(5000) { compose.onAllNodesWithText(line.text).fetchSemanticsNodes().isNotEmpty() }
            val output = File("build/reports/part3-presentation").apply { mkdirs() }
            ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", File(output, "zeke-radio.png"))
            compose.runOnIdle { radio = false }
            compose.onNodeWithText("ITEM USED").assertIsDisplayed()
            compose.onNodeWithText("ITEM ACQUIRED").assertDoesNotExist()
            compose.onNodeWithText("Spent during calibration. Removed from inventory.").assertIsDisplayed()
            ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", File(output, "inductor-used.png"))
        } finally { s.close(); directory.deleteRecursively() }
    }
}
