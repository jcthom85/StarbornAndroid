package com.example.starborn.feature.exploration.presentation

import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState

/** Queued notices never compete with an activity, conversation, or modal choice. */
fun ExplorationUiState.hasModalScene(): Boolean =
    gladeBallAnimating || isLoading || forceBlackScreen || activeDialogue != null || cinematic != null || shopGreeting != null ||
        roomActionsVisible || roomObjectOffer != null || blockedPrompt != null || togglePrompt != null ||
        tuningPuzzle != null || isMenuOverlayVisible || skillTreeOverlay != null || partyMemberDetails != null ||
        levelUpPrompt != null || questDetail != null || isQuestLogVisible || isMapLegendVisible ||
        isMilestoneGalleryVisible || isCatchLogVisible || astraFacility != null || isAstraRecreationVisible ||
        isAstraNavConsoleVisible || isSimulationDeckVisible || isTapeDeckVisible || showBurgQuestAstraExitDialog

enum class ExplorationNotice { NONE, NARRATION, ANNOUNCEMENT, PROMPT }

fun explorationNotice(ui: ExplorationUiState, externalModal: Boolean = false): ExplorationNotice = when {
    externalModal || ui.hasModalScene() -> ExplorationNotice.NONE
    ui.narrationPrompt != null -> ExplorationNotice.NARRATION
    ui.eventAnnouncement != null -> ExplorationNotice.ANNOUNCEMENT
    ui.prompt != null -> ExplorationNotice.PROMPT
    else -> ExplorationNotice.NONE
}

/** Level gains wait until the current conversation or activity has finished. */
fun canPresentLevelUp(ui: ExplorationUiState, externalModal: Boolean = false): Boolean =
    !externalModal && !ui.copy(levelUpPrompt = null).hasModalScene()
