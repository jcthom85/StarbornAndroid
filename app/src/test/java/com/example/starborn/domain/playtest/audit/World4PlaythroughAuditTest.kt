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
 * Automated full playthrough and narrative review test for World 4 (The Foundry).
 * Executes the complete World 4 main quest sequence (w4_mq16 - w4_mq20) while
 * auditing text, Project Phantom revelations, conveyor/assembly operations,
 * Anvil relic acquisition, and Commander Rylos / Titan Walker boss confrontation.
 */
class World4PlaythroughAuditTest {

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
    fun `play World 4 beginning to end and generate narrative audit report`() {
        val auditor = PlaythroughNarrativeAuditor(
            worldId = "world_4",
            worldTitle = "The Foundry: Slag Pits, Assembly Line & Titan Forge",
            allRooms = rooms,
            allQuests = quests,
            allEvents = events,
            allDialogue = dialogue
        )

        // Starting state directly seeded from World 3 exit / World 4 start:
        val store = GameSessionStore().apply {
            restore(
                GameSessionState(
                    worldId = "world_4",
                    hubId = "hub_7_slag_pits",
                    roomId = "foundry_slag_landing",
                    playerId = "nova",
                    partyMembers = listOf("nova", "zeke", "orion", "gh0st"),
                    unlockedSkills = setOf("nova_arc_tether", "nova_link", "orion_harmony", "source_art_channel"),
                    activeQuests = setOf("w4_mq16"),
                    trackedQuestId = "w4_mq16",
                    questStageById = mapOf("w4_mq16" to "secure_landing"),
                    completedQuests = ((1..5).map { "w1_mq%02d".format(it) } +
                        (1..5).map { "w2_mq%02d".format(it) } +
                        (11..15).map { "w3_mq%02d".format(it) }).toSet(),
                    completedMilestones = setOf(
                        "ms_w1_mq05_complete",
                        "ms_w2_mq05_complete",
                        "ms_w3_mq15_complete"
                    ),
                    inventory = mapOf("the_lens" to 1)
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
                onPartyMemberJoined = { memberId ->
                    store.addPartyMember(memberId)
                },
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

        // --- PLAYTHROUGH EXECUTION: WORLD 4 ---

        // 1. MQ16: Into the Fire (Landing, Slag River, Cooling Vents, Airlock)
        assertTrue(store.state.value.activeQuests.contains("w4_mq16"))
        doAction("w4_mq16_land_shelf")
        navigate("foundry_slag_river")
        combat(listOf("magma_drone"), "foundry_slag_river")
        doAction("w4_mq16_cross_slag_river")
        navigate("foundry_waste_intake")
        talk("Zeke")
        doAction("w4_mq16_hack_cooling_vents")
        navigate("foundry_service_airlock")
        doAction("w4_mq16_enter_airlock")

        assertTrue("w4_mq16 completed", store.state.value.completedQuests.contains("w4_mq16"))
        // 2. MQ17: Ghost in the Machine (Project Phantom Terminal & Springs Regroup)
        navigate("foundry_waste_intake")
        talk("Gh0st") // Triggers w4_mq17_access_phantom_terminal via nova_w4_phantom_prompt
        navigate("foundry_cooling_springs")
        talk("Gh0st") // Triggers w4_mq17_regroup_springs via gh0st_w4_springs_key (which warps to foundry_conveyor_belt)

        assertTrue("w4_mq17 completed", store.state.value.completedQuests.contains("w4_mq17"))
        assertTrue("w4_mq18 active", store.state.value.activeQuests.contains("w4_mq18"))
        assertEquals("foundry_conveyor_belt", store.state.value.roomId)
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 3. MQ18: The Assembly (Conveyors, Prototypes, Overload Matrix)
        talk("Gh0st") // Triggers w4_mq18_navigate_conveyors
        talk("Gh0st") // Triggers w4_mq18_defeat_prototypes
        combat(listOf("magma_drone"), "foundry_conveyor_belt")
        doAction("w4_mq18_overload_matrix")

        assertTrue("w4_mq18 completed", store.state.value.completedQuests.contains("w4_mq18"))
        assertTrue("w4_mq19 active", store.state.value.activeQuests.contains("w4_mq19"))
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 4. MQ19: The Anvil (Puzzle & Acquisition)
        navigate("foundry_forge_anvil")
        doAction("w4_mq19_read_pulse_board")
        doAction("w4_mq19_read_grease_marks")
        doAction("w4_mq19_throw_override_paddle")
        doAction("w4_mq19_starve_breath_pistons")
        talk("Orion") // Triggers w4_mq19_open_anvil_cradle
        talk("Nova") // Triggers w4_mq19_take_anvil

        assertTrue("w4_mq19 completed", store.state.value.completedQuests.contains("w4_mq19"))
        assertTrue("The Anvil acquired", (store.state.value.inventory["the_anvil"] ?: 0) >= 1)
        assertTrue("w4_mq20 active", store.state.value.activeQuests.contains("w4_mq20"))
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 5. MQ20: Meltdown (Confront Rylos, Titan Walker, Steal Engine & Arrays, Launch)
        talk("Rylos") // Triggers w4_mq20_confront_rylos
        combat(listOf("titan_walker_boss"), "foundry_forge_anvil")
        doAction("w4_mq20_steal_engine")
        doAction("w4_mq20_steal_arrays")
        doAction("w4_mq20_escape_launch")

        assertTrue("w4_mq20 completed", store.state.value.completedQuests.contains("w4_mq20"))
        assertTrue("ms_w5_access_unlocked set", store.state.value.completedMilestones.contains("ms_w5_access_unlocked"))
        assertEquals("orbital_executive_dock", store.state.value.roomId)

        // Export report
        val reportFile = File(root, "docs/playtest/reports/WORLD_4_PLAYTHROUGH_AUDIT.md")
        auditor.exportReportToFile(reportFile)
        println("Audited World 4 Playthrough report exported to: ${reportFile.absolutePath}")
    }

    private inline fun <reified T> readList(path: String): List<T> {
        val file = File(assets, path)
        assertTrue("Asset file $path must exist", file.exists())
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(file.readText()) ?: emptyList()
    }
}
