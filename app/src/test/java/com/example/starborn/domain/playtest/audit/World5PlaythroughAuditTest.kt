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
 * Automated full playthrough and narrative review test for World 5 (The Void / Dominion Orbital Ring).
 * Executes the complete World 5 main quest sequence (w5_mq21 - w5_mq25) while auditing
 * text, Orbital Ring systems, zero-g transit, Thorne confrontation, Elara & Anchor relic recovery,
 * and Administrator Vale's Soloist coup.
 */
class World5PlaythroughAuditTest {

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
    fun `play World 5 beginning to end and generate narrative audit report`() {
        val auditor = PlaythroughNarrativeAuditor(
            worldId = "world_5",
            worldTitle = "The Void: Dominion Orbital Ring",
            allRooms = rooms,
            allQuests = quests,
            allEvents = events,
            allDialogue = dialogue
        )

        // Starting state directly seeded from World 4 exit / World 5 start:
        val store = GameSessionStore().apply {
            restore(
                GameSessionState(
                    worldId = "world_5",
                    hubId = "hub_9_orbital_ring",
                    roomId = "orbital_executive_dock",
                    playerId = "nova",
                    partyMembers = listOf("nova", "zeke", "orion", "gh0st"),
                    unlockedSkills = setOf(
                        "nova_arc_tether",
                        "nova_link",
                        "orion_harmony",
                        "source_art_channel",
                        "source_art_construct"
                    ),
                    activeQuests = setOf("w5_mq21"),
                    trackedQuestId = "w5_mq21",
                    questStageById = mapOf("w5_mq21" to "approach"),
                    completedQuests = ((1..5).map { "w1_mq%02d".format(it) } +
                        (1..5).map { "w2_mq%02d".format(it) } +
                        (11..15).map { "w3_mq%02d".format(it) } +
                        (16..20).map { "w4_mq%02d".format(it) }).toSet(),
                    completedMilestones = setOf(
                        "ms_w1_mq05_complete",
                        "ms_w2_mq05_complete",
                        "ms_w3_mq15_complete",
                        "ms_w4_mq20_complete",
                        "ms_w5_access_unlocked"
                    ),
                    inventory = mapOf(
                        "the_lens" to 1,
                        "the_anvil" to 1
                    )
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

        // --- PLAYTHROUGH EXECUTION: WORLD 5 ---

        // 1. MQ21: Docking Procedure (Approach Halo, Fighter Screen, Force Dock, Hack Airlock)
        assertTrue(store.state.value.activeQuests.contains("w5_mq21"))
        navigate("orbital_executive_dock") // triggers w5_mq21_arrive_halo (approach_halo)
        combat(listOf("orbital_fighter"), "orbital_executive_dock") // triggers w5_mq21_fighters_down
        doAction("w5_mq21_force_dock")
        talk("Gh0st") // Triggers w5_mq21_hack_airlock

        assertTrue("w5_mq21 completed", store.state.value.completedQuests.contains("w5_mq21"))
        assertTrue("w5_mq22 active", store.state.value.activeQuests.contains("w5_mq22"))
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 2. MQ22: Zero G (Solarium, Security Hub, Service Shaft, Server Farm, Find Thorne)
        navigate("orbital_solarium")
        doAction("w5_mq22_cross_solarium")
        navigate("orbital_security_hub")
        combat(listOf("compliance_officer", "null_g_drone"), "orbital_security_hub") // triggers w5_mq22_clear_security
        navigate("orbital_service_shaft")
        doAction("w5_mq22_traverse_shaft")
        navigate("orbital_server_farm")
        doAction("w5_mq22_access_mainframe")
        talk("Thorne") // Triggers w5_mq22_find_thorne

        assertTrue("w5_mq22 completed", store.state.value.completedQuests.contains("w5_mq22"))
        assertTrue("w5_mq23 active", store.state.value.activeQuests.contains("w5_mq23"))
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 3. MQ23: The Core Approach (Firewall Maze, Alpha/Beta/Gamma, Hard-Light Construct)
        talk("Thorne") // Advises Nova on 3 firewalls
        doAction("w5_mq23_navigate_maze")
        doAction("w5_mq23_firewall_alpha")
        doAction("w5_mq23_firewall_beta")
        doAction("w5_mq23_firewall_gamma")
        combat(listOf("firewall_construct"), "orbital_server_farm") // triggers w5_mq23_defeat_construct

        assertTrue("w5_mq23 completed", store.state.value.completedQuests.contains("w5_mq23"))
        assertTrue("w5_mq24 active", store.state.value.activeQuests.contains("w5_mq24"))
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 4. MQ24: The Anchor (Anchor Chamber, Elara Reunion, Claim Relic & Stasis)
        navigate("deep_anchor_chamber") // triggers w5_mq24_enter_chamber
        talk("Elara") // Triggers w5_mq24_find_elara
        doAction("w5_mq24_take_anchor")

        assertTrue("w5_mq24 completed", store.state.value.completedQuests.contains("w5_mq24"))
        assertTrue("Anchor Relic acquired", (store.state.value.inventory["anchor_relic"] ?: 0) >= 1)
        assertTrue("Stasis skill unlocked", store.state.value.unlockedSkills.contains("source_art_stasis"))
        assertTrue("w5_mq25 active", store.state.value.activeQuests.contains("w5_mq25"))
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 5. MQ25: Critical Mass (Throne Room, Vale's Soloist Coup, Avatar Boss, Reality Breach)
        navigate("deep_throne_room") // triggers w5_mq25_reach_throne
        talk("Vale") // Triggers w5_mq25_soloist
        combat(listOf("compliance_avatar"), "deep_throne_room") // triggers w5_mq25_defeat_avatar
        navigate("deep_tear")
        doAction("w5_mq25_enter_tear")

        assertTrue("w5_mq25 completed", store.state.value.completedQuests.contains("w5_mq25"))
        assertTrue("ms_w6_access_unlocked set", store.state.value.completedMilestones.contains("ms_w6_access_unlocked"))
        assertEquals("source_campfire", store.state.value.roomId)
        assertTrue("w6_mq26 active", store.state.value.activeQuests.contains("w6_mq26"))

        // Export report
        val reportFile = File(root, "docs/playtest/reports/WORLD_5_PLAYTHROUGH_AUDIT.md")
        auditor.exportReportToFile(reportFile)
        println("Audited World 5 Playthrough report exported to: ${reportFile.absolutePath}")
    }

    private inline fun <reified T> readList(path: String): List<T> {
        val file = File(assets, path)
        assertTrue("Asset file $path must exist", file.exists())
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(file.readText()) ?: emptyList()
    }
}
