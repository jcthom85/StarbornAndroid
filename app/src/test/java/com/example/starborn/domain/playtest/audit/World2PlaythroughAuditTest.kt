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
 * Automated full playthrough and narrative review test for World 2 (Sector 9 Uncharted Wilds).
 * Executes the complete World 2 main quest sequence (w2_mq01 - w2_mq05) while
 * auditing text, room descriptions, objective clarity, and party recruitment continuity.
 */
class World2PlaythroughAuditTest {

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
    fun `play World 2 beginning to end and generate narrative audit report`() {
        val auditor = PlaythroughNarrativeAuditor(
            worldId = "world_2",
            worldTitle = "Sector 9 Uncharted Wilds & Ancient Spire Base",
            allRooms = rooms,
            allQuests = quests,
            allEvents = events,
            allDialogue = dialogue
        )

        // Starting state directly seeded from World 1 exit / World 2 start:
        val store = GameSessionStore().apply {
            restore(
                GameSessionState(
                    worldId = "world_2",
                    hubId = "hub_2_sector9",
                    roomId = "sector9_crash_site",
                    playerId = "nova",
                    partyMembers = listOf("nova", "zeke"),
                    unlockedSkills = setOf("nova_arc_tether"),
                    activeQuests = setOf("w2_mq01"),
                    trackedQuestId = "w2_mq01",
                    questStageById = mapOf("w2_mq01" to "assess_crash_site"),
                    completedQuests = (1..5).map { "w1_mq%02d".format(it) }.toSet(),
                    completedMilestones = setOf("ms_w1_mq05_complete")
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

        // --- PLAYTHROUGH EXECUTION: WORLD 2 ---

        // 1. MQ01: Assess Crash Site & Stabilize Zeke
        talk("Zeke")
        doAction("w2_mq01_examine_pod")
        doAction("w2_mq01_stabilize_zeke")
        navigate("sector9_landing_stream")

        assertTrue("w2_mq01 must be completed", store.state.value.completedQuests.contains("w2_mq01"))
        assertTrue("w2_mq02 must be active", store.state.value.activeQuests.contains("w2_mq02"))

        // 2. MQ02: The Signal & Temple Gate
        talk("Zeke")
        navigate("sector9_canopy")
        combat(listOf("echo_borer"), "sector9_canopy")
        navigate("sector9_temple_gate")
        doAction("w2_mq02_use_chime")

        assertTrue("w2_mq02 must be completed", store.state.value.completedQuests.contains("w2_mq02"))
        assertTrue("w2_mq03 must be active", store.state.value.activeQuests.contains("w2_mq03"))

        // 3. MQ03: Sleeping Giant (Orion Awakened)
        doAction("w2_mq03_inspect_murals")
        navigate("sector9_stasis_chamber")
        doAction("w2_mq03_inspect_pod")
        doAction("w2_mq03_read_mural_overview")
        doAction("w2_mq03_stabilize_coolant")
        doAction("w2_mq03_align_complete")

        assertTrue("w2_mq03 must be completed", store.state.value.completedQuests.contains("w2_mq03"))
        assertTrue("Orion must join party", store.state.value.partyMembers.contains("orion"))
        assertTrue("w2_mq04 must be active", store.state.value.activeQuests.contains("w2_mq04"))
        talk("Zeke")

        // 4. MQ04: The Hunter (Defeat Source Beast & Recruit Gh0st)
        navigate("sector9_canopy_ridge")
        doAction("w2_mq04_confront")
        combat(listOf("the_beast"), "sector9_canopy_ridge")
        doAction("w2_mq04_anchor_drill")

        assertTrue("w2_mq04 must be completed", store.state.value.completedQuests.contains("w2_mq04"))
        assertTrue("Gh0st must join party", store.state.value.partyMembers.contains("gh0st"))
        assertTrue("w2_mq05 must be active", store.state.value.activeQuests.contains("w2_mq05"))
        talk("Zeke")
        talk("Orion")

        // 5. MQ05: Liftoff to Spire
        navigate("sector9_source_gate")
        doAction("w2_mq05_stabilize_horn")
        doAction("w2_mq05_ground_cup")
        doAction("w2_mq05_read_pressure_gauge")
        doAction("w2_mq05_overload_breakers")
        doAction("w2_mq05_bypass_gate")
        doAction("w2_mq05_inspect_astra")
        doAction("w2_mq05_collect_conduits")
        doAction("w2_mq05_reboot")
        doAction("w2_mq05_launch")

        assertTrue("w2_mq05 must be completed", store.state.value.completedQuests.contains("w2_mq05"))
        assertTrue("World 3 Spire unlock milestone achieved", store.state.value.completedMilestones.contains("ms_w2_mq05_complete"))
        assertEquals("spire_sewers_landing", store.state.value.roomId)

        // Export report
        val reportFile = File(root, "docs/playtest/reports/WORLD_2_PLAYTHROUGH_AUDIT.md")
        auditor.exportReportToFile(reportFile)
        println("Audited World 2 Playthrough report exported to: ${reportFile.absolutePath}")
    }

    private inline fun <reified T> readList(path: String): List<T> {
        val file = File(assets, path)
        assertTrue("Asset file $path must exist", file.exists())
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(file.readText()) ?: emptyList()
    }
}
