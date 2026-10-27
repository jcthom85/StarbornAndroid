package com.example.starborn.feature.exploration

import com.example.starborn.domain.prompt.ItemGrantedPrompt
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.feature.exploration.viewmodel.*
import org.junit.Assert.*
import org.junit.Test

class ExplorationNoticePolicyTest {
    private val ui = ExplorationUiState(isLoading = false, forceBlackScreen = false,
        narrationPrompt = NarrationPrompt("Inspection", true), prompt = ItemGrantedPrompt("Reward", 1))

    @Test fun inspectionPrecedesRewardAndModalScenesHideBoth() {
        assertEquals(ExplorationNotice.NARRATION, explorationNotice(ui))
        assertEquals(ExplorationNotice.PROMPT, explorationNotice(ui.copy(narrationPrompt = null)))
        assertEquals(ExplorationNotice.NONE, explorationNotice(ui, externalModal = true))
        listOf(ui.copy(roomActionsVisible = true), ui.copy(isMenuOverlayVisible = true),
            ui.copy(forceBlackScreen = true), ui.copy(isLoading = true), ui.copy(isTapeDeckVisible = true)).forEach {
            assertEquals(ExplorationNotice.NONE, explorationNotice(it))
            assertFalse(canPresentLevelUp(it))
        }
        assertTrue(canPresentLevelUp(ui))
    }
}
