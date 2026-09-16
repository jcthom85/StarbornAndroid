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
 * Automated full playthrough and narrative review test for World 3 (The Spire).
 * Executes the complete World 3 main quest sequence (w3_mq11 - w3_mq15) while
 * auditing text, heist preparation, Upper City infiltration, and narrative continuity.
 */
class World3PlaythroughAuditTest {

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
    fun `play World 3 beginning to end and generate narrative audit report`() {
        val auditor = PlaythroughNarrativeAuditor(
            worldId = "world_3",
            worldTitle = "The Spire: Lower Wards, Underrail & Executive Penthouse",
            allRooms = rooms,
            allQuests = quests,
            allEvents = events,
            allDialogue = dialogue
        )

        // Starting state directly seeded from World 2 exit / World 3 start:
        val store = GameSessionStore().apply {
            restore(
                GameSessionState(
                    worldId = "world_3",
                    hubId = "hub_3_spire",
                    roomId = "spire_sewers_landing",
                    playerId = "nova",
                    partyMembers = listOf("nova", "zeke", "orion", "gh0st"),
                    unlockedSkills = setOf("nova_arc_tether", "nova_link"),
                    activeQuests = setOf("w3_mq11"),
                    trackedQuestId = "w3_mq11",
                    questStageById = mapOf("w3_mq11" to "secure_landing_zone"),
                    completedQuests = ((1..5).map { "w1_mq%02d".format(it) } + (1..5).map { "w2_mq%02d".format(it) }).toSet(),
                    completedMilestones = setOf("ms_w1_mq05_complete", "ms_w2_mq05_complete")
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

        // --- PLAYTHROUGH EXECUTION: WORLD 3 ---

        // 1. MQ11: Clear Landing & Safehouse
        combat(listOf("sewer_crawler"), "spire_sewers_landing")
        navigate("spire_vent_output")
        navigate("spire_zekes_apartment")
        talk("Zeke")

        assertTrue("w3_mq11 must be completed", store.state.value.completedQuests.contains("w3_mq11"))
        assertTrue("w3_mq12 must be active", store.state.value.activeQuests.contains("w3_mq12"))
        talk("Zeke")

        // 2. MQ12: Heist Prep
        doAction("w3_mq12_talk_jax")
        doAction("w3_mq12_map_patrols")
        doAction("w3_mq12_interrogate_guard")
        doAction("w3_mq12_copy_badges")
        doAction("w3_mq12_source_disguises")
        doAction("w3_mq12_hack_blueprints")
        doAction("w3_mq12_assemble_planning")

        assertTrue("w3_mq12 must be completed", store.state.value.completedQuests.contains("w3_mq12"))
        assertTrue("w3_mq13 must be active", store.state.value.activeQuests.contains("w3_mq13"))
        talk("Zeke")

        // 3. MQ13: Upper City Infiltration & Blend In
        doAction("w3_mq13_blend_in")
        doAction("w3_mq13_disable_sensors")
        doAction("w3_mq13_enter_lobby")

        assertTrue("w3_mq13 must be completed", store.state.value.completedQuests.contains("w3_mq13"))
        assertTrue("w3_mq14 must be active", store.state.value.activeQuests.contains("w3_mq14"))
        talk("Zeke")

        // 4. MQ14: Archive Vault & Claim The Lens
        navigate("spire_archive_vault")
        doAction("w3_mq14_read_containment_field")
        doAction("w3_mq14_read_prism_shutters")
        doAction("w3_mq14_trace_command_tethers")
        doAction("w3_mq14_solve_light_puzzle")
        talk("Zeke")

        assertTrue("w3_mq14 must be completed", store.state.value.completedQuests.contains("w3_mq14"))
        assertTrue("The Lens acquired", (store.state.value.inventory["the_lens"] ?: 0) >= 1)
        assertTrue("w3_mq15 must be active", store.state.value.activeQuests.contains("w3_mq15"))

        // 5. MQ15: Roof Escape & Boss Battle
        navigate("spire_landing_pad_roof")
        combat(listOf("aero_drone", "heavy_mech"), "spire_landing_pad_roof")
        combat(listOf("administrator_boss"), "spire_landing_pad_roof")
        doAction("w3_scan_shield_gap")
        doAction("w3_mq15_launch_astra")

        assertTrue("w3_mq15 must be completed", store.state.value.completedQuests.contains("w3_mq15"))
        assertTrue("Milestone ms_w3_mq15_complete set", store.state.value.completedMilestones.contains("ms_w3_mq15_complete"))
        assertEquals("foundry_slag_landing", store.state.value.roomId)

        // Export report
        val reportFile = File(root, "docs/playtest/reports/WORLD_3_PLAYTHROUGH_AUDIT.md")
        auditor.exportReportToFile(reportFile)
        println("Audited World 3 Playthrough report exported to: ${reportFile.absolutePath}")
    }

    private inline fun <reified T> readList(path: String): List<T> {
        val file = File(assets, path)
        assertTrue("Asset file $path must exist", file.exists())
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(file.readText()) ?: emptyList()
    }
}
