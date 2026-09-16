package com.example.starborn.domain.playtest.audit

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.QuestAssetDataSource
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.domain.dialogue.DialogueConditionEvaluator
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerHandler
import com.example.starborn.domain.dialogue.DialogueTriggerParser
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.ui.events.UiEventBus
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Automated full playthrough and narrative review test for World 1.
 * Executes the complete World 1 main quest sequence (MQ01 - MQ05) while
 * auditing text, room descriptions, objective clarity, and story continuity.
 */
class World1PlaythroughAuditTest {

    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val assets = File(root, "app/src/main/assets")
    private val moshi = MoshiProvider.instance
    private val dispatcher = StandardTestDispatcher()
    private val testScope = CoroutineScope(SupervisorJob() + dispatcher)

    private lateinit var rooms: Map<String, Room>
    private lateinit var quests: Map<String, Quest>
    private lateinit var events: Map<String, GameEvent>
    private lateinit var dialogue: Map<String, DialogueLine>

    @Before
    fun setUp() {
        rooms = readList<Room>("rooms.json").associateBy { it.id }
        quests = readList<Quest>("quests.json").associateBy { it.id }
        events = readList<GameEvent>("events.json").associateBy { it.id }
        dialogue = readList<DialogueLine>("dialogue.json").associateBy { it.id }
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `play World 1 beginning to end and generate narrative audit report`() {
        val auditor = PlaythroughNarrativeAuditor(
            worldId = "world_1",
            worldTitle = "The Homestead & Mining Colony (The Pit)",
            allRooms = rooms,
            allQuests = quests,
            allEvents = events,
            allDialogue = dialogue
        )

        val store = GameSessionStore().apply {
            restore(
                GameSessionState(
                    worldId = "world_1",
                    hubId = "hub_1_homestead",
                    roomId = "pit_nova_bunk",
                    playerId = "nova",
                    partyMembers = listOf("nova"),
                    unlockedSkills = setOf("nova_arc_tether"),
                    activeQuests = setOf("w1_mq01"),
                    trackedQuestId = "w1_mq01",
                    questStageById = mapOf("w1_mq01" to "wake_in_the_pit")
                )
            )
        }

        val questRepo = QuestRepository(QuestAssetDataSource(
            AssetJsonReader(DesktopAssetProvider(listOf(assets)), moshi)
        )).apply { load() }
        val questRuntime = QuestRuntimeManager(questRepo, store, testScope, UiEventBus())

        val progressionAssets = com.example.starborn.data.assets.WorldAssetDataSource(
            AssetJsonReader(DesktopAssetProvider(listOf(assets)), moshi)
        )
        val leveling = com.example.starborn.domain.leveling.LevelingManager(
            requireNotNull(progressionAssets.loadLevelingData())
        )
        val xpAwarder = com.example.starborn.domain.leveling.ExplorationXpAwarder(
            store, leveling, requireNotNull(progressionAssets.loadProgressionData())
        )

        lateinit var eventManager: EventManager

        eventManager = EventManager(
            events = events.values.toList(),
            sessionStore = store,
            eventHooks = EventHooks(
                onPlayCinematic = { _, done -> done() },
                onQuestStarted = { qId -> if (!qId.isNullOrBlank()) questRuntime.recordQuestStarted(qId) },
                onQuestCompleted = { qId ->
                    if (!qId.isNullOrBlank()) {
                        questRuntime.markQuestCompleted(qId)
                        questRuntime.recordQuestCompleted(qId)
                        eventManager.handleTrigger("quest_stage_complete", EventPayload.QuestStage(qId))
                    }
                },
                onMilestoneSet = { ms -> if (!ms.isNullOrBlank()) store.setMilestone(ms) },
                onQuestTaskUpdated = { qId, tId -> if (!qId.isNullOrBlank() && !tId.isNullOrBlank()) questRuntime.markTaskComplete(qId, tId) },
                onQuestStageAdvanced = { qId, sId -> if (!qId.isNullOrBlank() && !sId.isNullOrBlank()) questRuntime.setStage(qId, sId) },
                onSetRoomState = { rId, k, v -> if (!rId.isNullOrBlank() && k.isNotBlank()) store.setRoomState(rId, k, v) },
                onGiveItem = { itemId, qty ->
                    val inv = store.state.value.inventory
                    store.setInventory(inv + (itemId to ((inv[itemId] ?: 0) + qty.coerceAtLeast(1))))
                },
                onTakeItem = { itemId, qty ->
                    val inv = store.state.value.inventory
                    val current = inv[itemId] ?: 0
                    val req = qty.coerceAtLeast(1)
                    if (current >= req) {
                        store.setInventory(if (current - req > 0) inv + (itemId to (current - req)) else inv - itemId)
                        true
                    } else {
                        false
                    }
                },
                onGiveXp = xpAwarder::award,
                onReward = { r ->
                    r.xp?.let(xpAwarder::award)
                    r.ap?.let(store::addAp)
                    r.credits?.let(store::addCredits)
                    r.items.forEach { item ->
                        val inv = store.state.value.inventory
                        store.setInventory(inv + (item.itemId to ((inv[item.itemId] ?: 0) + (item.quantity ?: 1))))
                    }
                }
            )
        )

        fun evalCondition(raw: String?): Boolean {
            if (raw.isNullOrBlank()) return true
            val state = store.state.value
            return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.all { token ->
                val parts = token.split(':', limit = 2)
                val type = parts[0].trim().lowercase()
                val value = parts.getOrNull(1)?.trim().orEmpty()
                when (type) {
                    "milestone", "milestone_set" -> value in state.completedMilestones
                    "milestone_not_set" -> value !in state.completedMilestones
                    "quest", "quest_active" -> value in state.activeQuests
                    "quest_not_started" -> value.isNotBlank() && value !in state.activeQuests && value !in state.completedQuests && value !in state.failedQuests
                    "quest_completed" -> value in state.completedQuests
                    "quest_not_completed" -> value !in state.completedQuests
                    "quest_stage", "quest_stage_not" -> {
                        val p = value.split(':', limit = 2)
                        val matches = p.size == 2 && state.questStageById[p[0]] == p[1]
                        if (type == "quest_stage") matches else !matches
                    }
                    "quest_task_done" -> {
                        val p = value.split(':', limit = 2)
                        p.size == 2 && state.questTasksCompleted[p[0]].orEmpty().contains(p[1])
                    }
                    "quest_task_not_done" -> {
                        val p = value.split(':', limit = 2)
                        p.size == 2 && !state.questTasksCompleted[p[0]].orEmpty().contains(p[1])
                    }
                    "item" -> (state.inventory[value] ?: 0) > 0
                    "item_not" -> (state.inventory[value] ?: 0) <= 0
                    else -> true
                }
            }
        }

        val dialogueService = DialogueService(
            dialogue.values.toList(),
            DialogueConditionEvaluator(::evalCondition),
            DialogueTriggerHandler { trig -> eventManager.performActions(DialogueTriggerParser.parse(trig)) }
        )

        fun settle() = dispatcher.scheduler.runCurrent()

        // Bot helper functions with auditing hooks:
        fun navigate(targetRoomId: String) {
            val fromRoom = store.state.value.roomId ?: "unknown"
            store.setRoom(targetRoomId)
            eventManager.handleTrigger("enter_room", EventPayload.EnterRoom(targetRoomId))
            settle()
            auditor.recordNavigation(fromRoom, targetRoomId, store.state.value)
        }

        fun doAction(actionId: String, payload: String? = null) {
            eventManager.handleTrigger("player_action", EventPayload.Action(actionId, payload))
            settle()
            auditor.recordAction(actionId, payload, store.state.value)
        }

        fun talk(speakerName: String, choiceId: String? = null) {
            val session = checkNotNull(dialogueService.startDialogue(speakerName)) { "No dialogue for $speakerName" }
            var steps = 0
            while (!session.isFinished()) {
                check(++steps <= 50) { "Stuck in dialogue with $speakerName" }
                val current = session.current()
                val lineSpeaker = current?.speaker ?: speakerName
                val lineText = current?.text.orEmpty()
                val choices = session.choices()
                if (choices.isNotEmpty()) {
                    val chosen = if (choiceId != null) {
                        choices.firstOrNull { it.id == choiceId } ?: choices.first()
                    } else {
                        choices.first()
                    }
                    auditor.recordDialogueExchange(lineSpeaker, lineText, chosen.text, store.state.value)
                    session.choose(chosen.id)
                } else {
                    auditor.recordDialogueExchange(lineSpeaker, lineText, null, store.state.value)
                    session.advance()
                }
                settle()
            }
        }

        fun combat(enemyIds: List<String>, roomId: String) {
            eventManager.handleTrigger(
                "encounter_victory",
                EventPayload.EncounterOutcome(
                    enemyIds = enemyIds,
                    outcome = EventPayload.EncounterOutcome.Outcome.VICTORY,
                    roomId = roomId
                )
            )
            settle()
            auditor.recordCombat(enemyIds, roomId, store.state.value)
        }

        fun craft(recipeId: String, resultItemId: String) {
            store.setInventory(store.state.value.inventory + (resultItemId to 1))
            auditor.recordCraft(recipeId, resultItemId, store.state.value)
        }

        // --- PLAYTHROUGH EXECUTION: WORLD 1 ---

        // 1. Prologue & MQ01: Wake Up Call
        navigate("pit_nova_bunk")
        doAction("w1_mq01_turn_on_bunk_light")
        doAction("w1_mq01_inspect_safety_fault")
        navigate("pit_jed_bunk")
        talk("Jed")
        doAction("w1_mq01_receive_starter_kit")
        navigate("workshop_yard")
        doAction("w1_mq01_inspect_loader_relay")
        combat(listOf("faulted_loader"), "workshop_yard")
        navigate("workshop_floor")
        doAction("tinkering_screen_entered")
        doAction("tinkering_craft", "functional_cryo_inductor")
        craft("repair_cryo_inductor", "functional_cryo_inductor")
        doAction("w1_mq01_patch_flux_liner")
        doAction("w1_mq01_confirm_governor")
        doAction("w1_mq01_cutter_surge")
        talk("Jed")

        assertTrue("w1_mq01 must be completed", store.state.value.completedQuests.contains("w1_mq01"))
        assertTrue("w1_mq02 must be active", store.state.value.activeQuests.contains("w1_mq02"))

        // 2. MQ02: Checkpoint & Transit Override
        navigate("checkpoint_queue")
        talk("Guard Hank")
        navigate("checkpoint_booth")
        talk("Zeke", choiceId = "zeke_w1_mq02_choose_grid_instability")

        assertTrue("w1_mq02 must be completed", store.state.value.completedQuests.contains("w1_mq02"))
        assertTrue("w1_mq03 must be active", store.state.value.activeQuests.contains("w1_mq03"))

        // 3. MQ03: Heavy Lifting & The Echo Relic
        navigate("admin_lobby")
        talk("Foreman Boggs")
        doAction("w1_sq03_start_loader")
        doAction("w1_sq03_move_cargo")
        combat(listOf("acoustic_bulwark"), "workshop_dock")
        talk("Foreman Boggs")
        navigate("mine_landing")
        combat(listOf("echo_borer"), "mine_landing")
        navigate("mine_junction")
        doAction("evt_mine_power_on")
        navigate("echo_gap")
        doAction("w1_mq03_touch_relic")

        assertTrue("w1_mq03 must be completed", store.state.value.completedQuests.contains("w1_mq03"))
        assertTrue("w1_mq04 must be active", store.state.value.activeQuests.contains("w1_mq04"))

        // 4. MQ04: Lockdown Escape & Jed's Sacrifice
        navigate("launch_lift")
        combat(listOf("acoustic_bulwark"), "launch_lift")
        talk("Jed")

        assertTrue("w1_mq04 must be completed", store.state.value.completedQuests.contains("w1_mq04"))
        assertTrue("w1_mq05 must be active", store.state.value.activeQuests.contains("w1_mq05"))

        // 5. MQ05: The Launch & Iron Warden Boss
        combat(listOf("resonance_buoy"), "launch_bay")
        navigate("launch_bay")
        talk("The Warden")
        combat(listOf("the_iron_warden"), "launch_bay")
        talk("Zeke")
        talk("Zeke")
        doAction("use_nav_console")

        assertEquals("sector9_crash_site", store.state.value.roomId)
        assertTrue("w1_mq05 must be completed", store.state.value.completedQuests.contains("w1_mq05"))
        assertTrue("World 2 must be activated", store.state.value.activeQuests.contains("w2_mq01"))

        // Export report
        val reportFile = File(root, "docs/playtest/reports/WORLD_1_PLAYTHROUGH_AUDIT.md")
        auditor.exportReportToFile(reportFile)
        println("Audited World 1 Playthrough report exported to: ${reportFile.absolutePath}")
    }

    private inline fun <reified T> readList(path: String): List<T> {
        val file = File(assets, path)
        assertTrue("Asset file $path must exist", file.exists())
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(file.readText()) ?: emptyList()
    }
}
