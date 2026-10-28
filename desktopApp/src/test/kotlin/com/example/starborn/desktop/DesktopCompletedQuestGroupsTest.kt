package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class DesktopCompletedQuestGroupsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun completedCampaignQuestsCanBeExpandedByWorldAndOpened() {
        val directory = Files.createTempDirectory("starborn-completed-groups").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            check(services.startDebugScenario("tut_npc_dialogue"))
            services.sessionStore.completeQuest("w1_sq01")
            services.sessionStore.completeQuest("w2_sq01")
            compose.setContent { DesktopStarbornTheme { DesktopExplorationTheme(services) {
                Box(Modifier.size(1024.dp,768.dp)) { DesktopRuntimeJournalContent(services) }
            } } }
            compose.waitUntil(15000) { !services.exploration.uiState.value.isLoading }
            compose.onAllNodesWithText("Completed", substring = false).onFirst().performClick()
            compose.onNodeWithText("World 1 (1) · Expand").performClick()
            compose.onNodeWithText("World 1 (1) · Collapse").assertExists()
            val title = requireNotNull(services.questRepository.questById("w1_sq01")).title
            compose.onAllNodesWithText(title).onFirst().performClick()
            compose.waitUntil(5000) { services.exploration.uiState.value.questDetail?.id == "w1_sq01" }
            compose.onNodeWithText("World 1 (1) · Collapse").performClick()
            compose.onNodeWithText("World 1 (1) · Expand").assertExists()
            compose.onNodeWithText("World 2 (1) · Expand").performClick()
            compose.onNodeWithText("World 2 (1) · Collapse").assertExists()
        } finally { services.close(); directory.deleteRecursively() }
    }
}
