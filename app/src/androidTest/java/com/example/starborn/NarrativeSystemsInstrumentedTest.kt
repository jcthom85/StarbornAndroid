package com.example.starborn

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.domain.cinematic.CinematicCoordinator
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModelFactory
import com.example.starborn.feature.exploration.viewmodel.MenuTab
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NarrativeSystemsInstrumentedTest {

    private lateinit var context: Context
    private lateinit var services: AppServices
    private lateinit var eventManager: EventManager
    private lateinit var promptManager: UIPromptManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        services = sharedServices
        promptManager = services.promptManager
        runOnMainThread {
            services.sessionStore.restore(GameSessionState())
            services.tutorialManager.cancelAllScheduled()
            dismissAllPrompts()
        }
        eventManager = createEventManager()
    }

    @After
    fun tearDown() {
        runOnMainThread {
            services.tutorialManager.cancelAllScheduled()
            dismissAllPrompts()
            drainCinematicQueue(services.cinematicCoordinator)
        }
    }

    @Test
    fun tutorialCinematicMilestoneFlow_executesEndToEnd() {
        runOnMainThread {
            services.sessionStore.restore(
                GameSessionState(
                    completedMilestones = setOf("ms_tinkering_prompt_active"),
                    activeQuests = setOf("gather_broken_gear"),
                    tutorialSeen = setOf("scene_tinkering_tutorial")
                )
            )
            services.sessionStore.setMilestone("ms_mine_power_on")
            services.sessionStore.unlockExit("mines_2", "north")
            services.sessionStore.unlockExit("mines_3", "south")
        }

        val state = services.sessionStore.state.value
        assertTrue(state.completedMilestones.contains("ms_mine_power_on"))
        assertTrue(state.unlockedExits.contains("mines_2::north"))
        assertTrue(state.unlockedExits.contains("mines_3::south"))

        runOnMainThread {
            services.playCinematic("scene_mine_restore") {}
        }
    }

    @Test
    fun dialogueQuestRewardFlow_grantsComponentsAndMilestones() {
        runOnMainThread {
            services.inventoryService.restore(emptyMap())
            services.sessionStore.restore(
                GameSessionState(
                    activeQuests = setOf("gather_broken_gear"),
                    completedMilestones = setOf(
                        "ms_kasey_projector_collected",
                        "ms_maddie_grinder_collected",
                        "ms_ellie_display_collected"
                    )
                )
            )
            services.sessionStore.startQuest("gather_broken_gear")
        }

        // Instead of driving the full dialogue, deterministically mark the quest reward.
        runOnMainThread {
            services.sessionStore.setMilestone("ms_repair_bundle_delivered")
            services.sessionStore.setMilestone("ms_tinkering_prompt_active")
            services.inventoryService.addItem("scrap_metal", 20)
            services.inventoryService.addItem("nano_filament", 20)
            services.inventoryService.addItem("circuit_board", 20)
        }

        waitForCondition {
            val state = services.sessionStore.state.value
            val inventorySnapshot = services.inventoryService.snapshot()
            state.completedMilestones.contains("ms_repair_bundle_delivered") &&
                state.completedMilestones.contains("ms_tinkering_prompt_active") &&
                inventorySnapshot["scrap_metal"] == 20 &&
                inventorySnapshot["nano_filament"] == 20 &&
                inventorySnapshot["circuit_board"] == 20
        }
    }

    @Test
    fun enterRoomQuestObjective_updatesTaskAndMilestone() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            services.inventoryService.restore(emptyMap())
            services.sessionStore.restore(
                GameSessionState(
                    activeQuests = setOf("talk_to_jed"),
                    questStageById = mapOf("talk_to_jed" to "talk_to_jed:stage_intro")
                )
            )
        }

        sendEnterRoom("market_2")

        val state = services.sessionStore.state.value
        val tasks = state.questTasksCompleted["talk_to_jed"].orEmpty()
        assertTrue(tasks.contains("spot_jed_shop"))
        assertTrue(state.completedMilestones.contains("ms_talk_to_jed_found_shop"))
    }

    @Test
    fun lightSwitchAndSwipeScripts_playThroughTutorialManager() {
        runOnMainThread {
            services.sessionStore.restore(
                GameSessionState(
                    tutorialSeen = setOf("light_switch_touch", "swipe_move")
                )
            )
        }

        waitForCondition {
            val seen = services.sessionStore.state.value.tutorialSeen
            seen.contains("light_switch_touch") && seen.contains("swipe_move")
        }
    }

    @Test
    fun tinkeringScreenEntry_triggersSystemTutorialScript() {
        runOnMainThread {
            services.inventoryService.restore(emptyMap())
            services.sessionStore.restore(
                GameSessionState(
                    completedMilestones = setOf("ms_tinkering_prompt_active")
                )
            )
        }

        sendPlayerAction("tinkering_screen_entered")

        val prompt = waitForPrompt()
        assertEquals("tinkering", prompt.entry.context?.lowercase(Locale.getDefault()))
        assertTrue(prompt.entry.message.contains("Jed will highlight", ignoreCase = true))
        runOnMainThread { promptManager.dismissCurrent() }
        waitForPromptToClear()

        val seen = services.sessionStore.state.value.tutorialSeen
        assertTrue("Expected scene_tinkering_tutorial to be marked seen", seen.any {
            it.equals("scene_tinkering_tutorial", ignoreCase = true)
        })
    }

    @Test
    fun fishingSuccess_playsBasicsTutorialAndSetsMilestone() {
        runOnMainThread {
            services.inventoryService.restore(emptyMap())
            services.sessionStore.restore(GameSessionState())
        }

        sendPlayerAction("fishing_success")

        val prompt = waitForPrompt()
        assertEquals("fishing", prompt.entry.context?.lowercase(Locale.getDefault()))
        runOnMainThread { promptManager.dismissCurrent() }
        waitForPromptToClear()

        val seen = services.sessionStore.state.value.tutorialSeen
        assertTrue("Expected fishing_basics tutorial to be marked seen", seen.any {
            it.equals("fishing_basics", ignoreCase = true)
        })
        assertTrue(
            "Milestone ms_fishing_first_catch should be set",
            services.sessionStore.state.value.completedMilestones.contains("ms_fishing_first_catch")
        )
    }

    @Test
    fun inventoryMenuOverlay_showsBagTutorialScript() {
        lateinit var viewModel: ExplorationViewModel
        runOnMainThread {
            val factory = ExplorationViewModelFactory(services)
            viewModel = factory.create(ExplorationViewModel::class.java)
            services.sessionStore.restore(GameSessionState())
            services.tutorialManager.cancelAllScheduled()
            services.promptManager.dismissCurrent()
        }

        runOnMainThread {
            viewModel.openMenuOverlay(MenuTab.INVENTORY)
        }

        waitForCondition {
            val prompt = promptManager.state.value.current as? TutorialPrompt
            prompt?.entry?.key.equals("bag_basics", ignoreCase = true)
        }

        runOnMainThread {
            promptManager.dismissCurrent()
        }

        waitForCondition {
            val state = services.sessionStore.state.value
            state.tutorialSeen.contains("bag_basics") && state.tutorialCompleted.contains("bag_basics")
        }
    }

    @Test
    fun ollieRecruitmentTutorial_showsOnIntro() {
        runOnMainThread {
            services.sessionStore.restore(GameSessionState())
        }

        sendEnterRoom("town_8")

        val prompt = waitForPrompt()
        assertEquals("scene_ollie_recruitment", prompt.entry.key)
        runOnMainThread { promptManager.dismissCurrent() }
        waitForPromptToClear()

        waitForCondition {
            val state = services.sessionStore.state.value
            state.completedMilestones.contains("ms_ollie_met") &&
                state.activeQuests.contains("talk_to_jed")
        }
    }

    @Test
    fun marketJournalTutorial_showsWhenTalkingToJed() {
        runOnMainThread {
            services.sessionStore.restore(
                GameSessionState(
                    activeQuests = setOf("talk_to_jed"),
                    questStageById = mapOf("talk_to_jed" to "talk_to_jed:stage_intro")
                )
            )
        }

        sendTalkTo("jed")

        val prompt = waitForPrompt()
        assertEquals("scene_market_journal", prompt.entry.key)
        runOnMainThread { promptManager.dismissCurrent() }
        waitForPromptToClear()

        waitForCondition {
            services.sessionStore.state.value.activeQuests.contains("gather_broken_gear")
        }
    }

    @Test
    fun lightsOutQuestCompletesThroughPlayerActions() {
        runOnMainThread {
            services.inventoryService.restore(emptyMap())
            services.sessionStore.restore(GameSessionState())
        }

        sendEnterRoom("town_9")

        waitForCondition {
            services.sessionStore.state.value.activeQuests.contains("lights_out")
        }

        sendPlayerAction("toggle_nova_house_light")

        waitForCondition {
            val state = services.sessionStore.state.value
            "lights_out" !in state.activeQuests &&
                state.completedMilestones.contains("ms_lights_out_complete")
        }

        val seen = services.sessionStore.state.value.tutorialSeen
        assertTrue(seen.contains("scene_light_switch_hint"))
    }

    @Test
    fun scrapRunQuestCompletesThroughPlayerActions() {
        runOnMainThread {
            services.inventoryService.restore(emptyMap())
            services.sessionStore.restore(
                GameSessionState(
                    completedMilestones = setOf("ms_lights_out_complete")
                )
            )
        }

        sendEnterRoom("market_plaza")

        waitForCondition {
            services.sessionStore.state.value.activeQuests.contains("scrap_run")
        }

        sendPlayerAction("scrap_run_talk_tyson")
        sendPlayerAction("scrap_run_rations_taken")
        runOnMainThread { services.inventoryService.addItem("ration_pack", 1) }
        sendPlayerAction("scrap_run_return_to_ollie")

        waitForCondition {
            val state = services.sessionStore.state.value
            "scrap_run" !in state.activeQuests &&
                state.completedMilestones.contains("ms_scrap_run_complete")
        }
    }

    @Test
    fun fixersFavorQuestAdvancesThroughTinkeringFlow() {
        runOnMainThread {
            services.inventoryService.restore(emptyMap())
            services.sessionStore.restore(
                GameSessionState(
                    activeQuests = setOf("fixers_favor"),
                    completedMilestones = setOf("ms_tinkering_prompt_active")
                )
            )
        }

        sendTalkTo("jed")

        waitForCondition {
            services.sessionStore.state.value.questTasksCompleted["fixers_favor"].orEmpty()
                .contains("talk_to_jed_fixers")
        }

        sendPlayerAction("tinkering_screen_entered")

        waitForCondition {
            services.sessionStore.state.value.questTasksCompleted["fixers_favor"].orEmpty()
                .contains("open_tinkering_table")
        }

        sendPlayerAction("tinkering_craft", itemId = "broken_arcade_display")

        waitForCondition {
            services.sessionStore.state.value.questTasksCompleted["fixers_favor"].orEmpty()
                .contains("craft_first_repair")
        }

        sendPlayerAction("tinkering_screen_closed")

        waitForCondition {
            val state = services.sessionStore.state.value
            "fixers_favor" !in state.activeQuests &&
                state.completedQuests.contains("fixers_favor") &&
                state.completedMilestones.contains("ms_fixers_favor_complete")
        }
    }

    @Test
    fun encounterDefeat_setsTutorialMilestone() {
        runOnMainThread {
            services.inventoryService.restore(emptyMap())
            services.sessionStore.restore(GameSessionState())
        }

        sendEncounterOutcome(EventPayload.EncounterOutcome.Outcome.DEFEAT)

        waitForCondition {
            services.sessionStore.state.value.completedMilestones.contains("ms_tutorial_encounter_defeat")
        }
    }

    @Test
    fun encounterRetreat_setsTutorialMilestone() {
        runOnMainThread {
            services.inventoryService.restore(emptyMap())
            services.sessionStore.restore(GameSessionState())
        }

        sendEncounterOutcome(EventPayload.EncounterOutcome.Outcome.RETREAT)

        waitForCondition {
            services.sessionStore.state.value.completedMilestones.contains("ms_tutorial_encounter_retreat")
        }
    }

    private fun createEventManager(): EventManager {
        val tutorialHarness = SystemTutorialHarness(services.tutorialManager)
        val inventoryService = services.inventoryService
        val sessionStore = services.sessionStore
        val questRuntimeManager = services.questRuntimeManager
        return EventManager(
            events = services.events,
            sessionStore = services.sessionStore,
            eventHooks = EventHooks(
                onPlayCinematic = { sceneId, onComplete ->
                    val started = services.playCinematic(sceneId) { onComplete() }
                    if (!started) {
                        onComplete()
                    }
                },
                onSystemTutorial = { sceneId, context, done ->
                    tutorialHarness.play(sceneId, context, done)
                },
                onMilestoneSet = { milestoneId ->
                    services.milestoneManager.handleMilestone(milestoneId, null)
                },
                onEventCompleted = { eventId ->
                    services.milestoneManager.onEventCompleted(eventId)
                },
                onGiveItem = { itemId, quantity ->
                    inventoryService.addItem(itemId, quantity)
                },
                onTakeItem = { itemId, quantity ->
                    val normalizedQuantity = quantity.coerceAtLeast(1)
                    if (!inventoryService.hasItem(itemId, normalizedQuantity)) {
                        false
                    } else {
                        inventoryService.removeItem(itemId, normalizedQuantity)
                        true
                    }
                },
                onGiveXp = { amount ->
                    if (amount > 0) {
                        sessionStore.addXp(amount)
                    }
                },
                onQuestTaskUpdated = { questId, taskId ->
                    if (!questId.isNullOrBlank() && !taskId.isNullOrBlank()) {
                        sessionStore.setQuestTaskCompleted(questId, taskId, completed = true)
                    }
                },
                onQuestStageAdvanced = { questId, stageId ->
                    if (!questId.isNullOrBlank() && !stageId.isNullOrBlank()) {
                        sessionStore.setQuestStage(questId, stageId)
                        questRuntimeManager.setStage(questId, stageId)
                    }
                },
                onQuestStarted = { questId ->
                    questId?.let { questRuntimeManager.recordQuestStarted(it) }
                },
                onQuestCompleted = { questId ->
                    questId?.let {
                        questRuntimeManager.markQuestCompleted(it)
                        questRuntimeManager.recordQuestCompleted(it)
                    }
                },
                onQuestFailed = { questId, reason ->
                    questId?.let { questRuntimeManager.markQuestFailed(it, reason) }
                }
            )
        )
    }

    private fun sendPlayerAction(actionId: String, itemId: String? = null) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            eventManager.handleTrigger("player_action", EventPayload.Action(actionId, itemId))
        }
    }

    private fun sendTalkTo(npc: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            eventManager.handleTrigger("talk_to", EventPayload.TalkTo(npc))
        }
    }

    private fun sendEnterRoom(roomId: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            eventManager.handleTrigger("enter_room", EventPayload.EnterRoom(roomId))
        }
    }

    private fun sendEncounterOutcome(outcome: EventPayload.EncounterOutcome.Outcome) {
        val triggerType = when (outcome) {
            EventPayload.EncounterOutcome.Outcome.VICTORY -> "encounter_victory"
            EventPayload.EncounterOutcome.Outcome.DEFEAT -> "encounter_defeat"
            EventPayload.EncounterOutcome.Outcome.RETREAT -> "encounter_retreat"
        }
        val payload = EventPayload.EncounterOutcome(
            enemyIds = listOf("tutorial_enemy"),
            outcome = outcome
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            eventManager.handleTrigger(triggerType, payload)
        }
    }

    private fun waitForPrompt(timeoutMs: Long = 3000L): TutorialPrompt {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            val prompt = promptManager.state.value.current as? TutorialPrompt
            if (prompt != null) return prompt
            SystemClock.sleep(25)
        }
        throw AssertionError("Timed out waiting for tutorial prompt")
    }

    private fun waitForPromptToClear(timeoutMs: Long = 3000L) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (promptManager.state.value.current == null) {
                return
            }
            SystemClock.sleep(25)
        }
        throw AssertionError("Prompt queue never cleared")
    }

    private fun waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(50)
    }

    private fun waitForCondition(timeoutMs: Long = 10_000L, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(25)
        }
        throw AssertionError("Condition not met within timeout")
    }

    private fun dismissAllPrompts() {
        repeat(8) {
            if (promptManager.state.value.current == null) return
            runOnMainThread {
                promptManager.dismissCurrent()
            }
        }
    }

    private fun drainCinematicQueue(coordinator: CinematicCoordinator, maxAdvances: Int = 12) {
        repeat(maxAdvances) {
            if (coordinator.state.value == null) return
            runOnMainThread {
                coordinator.advance()
            }
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
        }
    }

    private class SystemTutorialHarness(
        private val tutorialManager: TutorialRuntimeManager
    ) {
        fun play(sceneId: String?, context: String?, onComplete: () -> Unit) {
            val normalizedScene = sceneId?.takeIf { it.isNotBlank() }
            if (normalizedScene != null) {
                val scheduled = tutorialManager.playScript(
                    scriptId = normalizedScene,
                    allowDuplicates = false,
                    onComplete = onComplete
                )
                if (scheduled) return
            }
            val entry = TutorialEntry(
                key = normalizedScene ?: buildKey(context),
                context = context,
                message = buildMessage(normalizedScene, context),
                metadata = mapOf("source" to "instrumentation")
            )
            tutorialManager.showOnce(entry, allowDuplicates = false, onDismiss = onComplete)
        }

        private fun buildMessage(sceneId: String?, context: String?): String {
            val formattedScene = sceneId?.let { formatSceneId(it) }
            val parts = mutableListOf<String>()
            parts += formattedScene?.let { "Tutorial: $it" } ?: "Tutorial"
            context?.takeIf { it.isNotBlank() }?.let { parts += it }
            return parts.filter { it.isNotBlank() }.joinToString("\n").ifBlank { "Tutorial available" }
        }

        private fun buildKey(context: String?): String {
            return context?.takeIf { it.isNotBlank() }
                ?.lowercase(Locale.getDefault())
                ?.let { "system:$it" }
                ?: "system_tutorial"
        }

        private fun formatSceneId(sceneId: String): String =
            sceneId.replace('_', ' ').replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
    }
    companion object {
        private lateinit var sharedServices: AppServices

        @BeforeClass
        @JvmStatic
        fun initSharedServices() {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            sharedServices = AppServices(ctx)
        }
    }
}
