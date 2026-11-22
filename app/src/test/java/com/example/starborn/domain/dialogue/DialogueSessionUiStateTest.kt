package com.example.starborn.domain.dialogue

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.DialogueLine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueSessionUiStateTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val lines = reader.readList<DialogueLine>("dialogue.json")

    private fun newService() = DialogueService(
        dialogueLines = lines,
        conditionEvaluator = DialogueConditionEvaluator { true },
        triggerHandler = DialogueTriggerHandler { }
    )

    @Test
    fun pamTopicHubTracksVisitedRevisitedFavorAndExitOptions() {
        val service = newService()
        val session = service.startDialogueAt("pam_default_talk")
        assertNotNull(session)
        session!!

        assertTrue(session.isTopicHub())
        assertFalse(session.isCurrentLineRevisited())

        val choices = session.choices()
        val biscuits = choices.first { it.id == "pam_opt_biscuits" }
        val rooster = choices.first { it.id == "pam_opt_rooster" }
        val leave = choices.first { it.id == "pam_opt_leave" }

        assertTrue("Pam's biscuits option should highlight as favor/reward", session.hasFavorOrReward(biscuits))
        assertFalse("Banter topic should not highlight as favor", session.hasFavorOrReward(rooster))
        assertTrue("Leave option should be classified as exit", session.isExitOption(leave))

        // Ask about the rooster, advance back to pam_default_talk -> visited + revisit flag set
        val reply = session.choose(rooster.id)
        assertNotNull(reply)
        assertFalse(session.isCurrentLineRevisited())

        val backToHub = session.advance()
        assertNotNull(backToHub)
        assertTrue("Returning to hub greeting should skip typewriter replay", session.isCurrentLineRevisited())
        assertTrue("Asked topic should be marked visited", session.isOptionVisited(rooster.id))
        assertFalse("Unasked topic should not be marked visited", session.isOptionVisited(biscuits.id))
    }

    @Test
    fun storyQuizzesAndPetInteractionsAreNeverSpoiledWithFavorBadges() {
        val service = newService()

        val zeke = service.startDialogueAt("zeke_w1_mq02_override_choice")
        assertNotNull(zeke)
        zeke!!.choices().forEach { opt ->
            assertFalse("Quiz choice ${opt.id} must not be spoiled as favor/reward", zeke.hasFavorOrReward(opt))
        }

        val meeko = service.startDialogueAt("meeko_default_talk")
        assertNotNull(meeko)
        meeko!!.choices().forEach { opt ->
            assertFalse("Pet interaction ${opt.id} must not be spoiled as favor/reward", meeko.hasFavorOrReward(opt))
        }
    }
}
