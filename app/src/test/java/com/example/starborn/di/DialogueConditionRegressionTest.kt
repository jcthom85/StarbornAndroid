package com.example.starborn.di

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.session.GameSessionState
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock

class DialogueConditionRegressionTest {
    private val inventory = InventoryService(mock<ItemCatalog>())
    private val lines: List<DialogueLine> by lazy {
        val root = if (File("src/main/assets").isDirectory) File("src/main/assets") else File("app/src/main/assets")
        requireNotNull(MoshiProvider.instance.adapter<List<DialogueLine>>(
            Types.newParameterizedType(List::class.java, DialogueLine::class.java)
        ).fromJson(File(root, "dialogue.json").readText()))
    }
    private fun service(state: GameSessionState, android: Boolean): DialogueService = DialogueService(
        lines, { condition -> if (android) isDialogueConditionMet(condition, state, inventory)
            else com.example.starborn.domain.dialogue.isDialogueConditionMet(condition, state, inventory) }, {}
    )

    @Test fun authoredTaskAndEvidenceConditionsUseRealStateOnBothPlatforms() {
        val before = GameSessionState(activeQuests = setOf("q"))
        val after = before.copy(questTasksCompleted = mapOf("q" to setOf("task")), completedMilestones = setOf("evidence"))
        for (android in listOf(false, true)) {
            fun met(raw: String, state: GameSessionState) = if (android) isDialogueConditionMet(raw, state, inventory)
                else com.example.starborn.domain.dialogue.isDialogueConditionMet(raw, state, inventory)
            assertFalse(met("quest_task_done:q:task", before))
            assertTrue(met("quest_task_not_done:q:task", before))
            assertTrue(met("quest_task_done:q:task,milestone_set:evidence", after))
            assertFalse(met("quest_task_not_done:q:task", after))
            assertFalse(met("milestone_set:evidence", before))
            assertTrue(met("quest_not_completed:q", before))
            assertFalse(met("quest_not_completed:q", before.copy(completedQuests = setOf("q"))))
            for (raw in listOf("quest_task_done:q", "quest_task_not_done:q:", "quest_task_done::task", "quest_not_completed:", "typo:evidence")) {
                assertFalse(raw, met(raw, after))
            }
        }
    }

    @Test fun botanistCannotTurnInUntilEverySpecimenIsScanned() {
        for (android in listOf(false, true)) for (count in 0..5) {
            val state = GameSessionState(activeQuests = setOf("w2_sq01"),
                completedQuests = setOf("w2_mq01", "w2_mq03"),
                completedMilestones = setOf("ms_w2_mq03_complete"),
                questTasksCompleted = mapOf("w2_sq01" to (1..count).map { "scan_flora_$it" }.toSet()))
            val session = service(state, android).startDialogue("Zeke")
            assertEquals("platform=$android scans=$count", count == 5,
                session?.current()?.id == "zeke_w2_sq01_turnin_1")
        }
    }

    @Test fun completedCrashIntroductionDoesNotReplay() {
        for (android in listOf(false, true)) {
            val initial = GameSessionState(activeQuests = setOf("w2_mq01"))
            assertEquals("zeke_w2_crash_1", service(initial, android).startDialogue("Zeke")?.current()?.id)
            val checked = initial.copy(questTasksCompleted = mapOf("w2_mq01" to setOf("check_on_zeke")))
            assertNotEquals("zeke_w2_crash_1", service(checked, android).startDialogue("Zeke")?.current()?.id)
        }
    }

    @Test fun orionOffersOptionalQuestAfterAstraInspection() {
        val state = GameSessionState(activeQuests = setOf("w2_mq05"),
            completedQuests = setOf("w2_mq01", "w2_mq03", "w2_mq04"),
            completedMilestones = setOf("ms_w2_mq03_complete", "ms_w2_mq04_complete", "ms_w2_gate_bypassed"),
            questTasksCompleted = mapOf("w2_mq05" to setOf("bypass_source_gate", "inspect_astra")))
        for (android in listOf(false, true)) {
            assertEquals("orion_w2_sq03_intro_1", service(state, android).startDialogue("Orion")?.current()?.id)
        }
    }

    @Test fun allAuthoredDialogueConditionsHaveAnImplementedVocabulary() {
        val supported = setOf("quest", "quest_active", "quest_completed", "quest_not_completed", "quest_not_started",
            "quest_failed", "quest_stage", "quest_stage_not", "quest_task_done", "quest_task_not_done", "room",
            "milestone", "milestone_set", "milestone_not_set", "item", "item_not", "event_completed", "event_not_completed",
            "tutorial_completed", "tutorial_not_completed")
        for (line in lines) for (raw in listOf(line.condition) + line.options.orEmpty().map { it.condition }) {
            raw.orEmpty().split(',').filter { it.isNotBlank() }.forEach { token ->
                assertTrue("${line.id}: unsupported $token", token.substringBefore(':').trim().lowercase() in supported)
            }
        }
    }
}
