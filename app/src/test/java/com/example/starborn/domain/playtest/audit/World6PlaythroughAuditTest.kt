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
 * Automated full playthrough and narrative review test for World 6: The Source (The Grand Finale).
 * Executes the complete World 6 main quest sequence (w6_mq26 - w6_mq30) from the Campfire
 * through companion nightmare rescues, the Echo Mines, the Memory Bridge, the Spire of Thought,
 * and the final confrontation against Ascended Vale and Ascended Silence.
 */
class World6PlaythroughAuditTest {

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
    fun `play World 6 beginning to end and generate narrative audit report`() {
        val auditor = PlaythroughNarrativeAuditor(
            worldId = "world_6",
            worldTitle = "The Source: Event Horizon & The Singularity (The Finale)",
            allRooms = rooms,
            allQuests = quests,
            allEvents = events,
            allDialogue = dialogue
        )

        // Starting state directly seeded from World 5 exit / World 6 entry:
        val store = GameSessionStore().apply {
            restore(
                GameSessionState(
                    worldId = "world_6",
                    hubId = "hub_11_event_horizon",
                    roomId = "source_campfire",
                    playerId = "nova",
                    partyMembers = listOf("nova", "zeke", "orion", "gh0st"),
                    unlockedSkills = setOf(
                        "nova_arc_tether",
                        "nova_link",
                        "orion_harmony",
                        "source_art_channel",
                        "source_art_construct",
                        "source_art_stasis"
                    ),
                    activeQuests = setOf("w6_mq26"),
                    trackedQuestId = "w6_mq26",
                    questStageById = mapOf("w6_mq26" to "nightmares"),
                    completedQuests = ((1..5).map { "w1_mq%02d".format(it) } +
                        (1..5).map { "w2_mq%02d".format(it) } +
                        (11..15).map { "w3_mq%02d".format(it) } +
                        (16..20).map { "w4_mq%02d".format(it) } +
                        (21..25).map { "w5_mq%02d".format(it) }).toSet(),
                    completedMilestones = setOf(
                        "ms_w1_mq05_complete",
                        "ms_w2_mq05_complete",
                        "ms_w3_mq15_complete",
                        "ms_w4_mq20_complete",
                        "ms_w5_mq25_complete",
                        "ms_w6_access_unlocked"
                    ),
                    inventory = mapOf(
                        "the_lens" to 1,
                        "the_anvil" to 1,
                        "anchor_relic" to 1
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

        // --- PLAYTHROUGH EXECUTION: WORLD 6 (THE FINALE) ---

        // 1. MQ26: Fractured Minds (Three Nightmares, Reassemble Team, The Key Relic)
        assertTrue(store.state.value.activeQuests.contains("w6_mq26"))
        navigate("source_campfire")

        // Nightmare 1: Zeke
        navigate("source_zeke_nightmare")
        combat(listOf("manager_projection"), "source_zeke_nightmare") // triggers w6_mq26_rescue_zeke
        talk("Zeke") // Speaks zeke_w6_nightmare_release

        // Nightmare 2: Gh0st
        navigate("source_gh0st_nightmare")
        combat(listOf("endless_war_echo"), "source_gh0st_nightmare") // triggers w6_mq26_rescue_gh0st
        talk("Gh0st") // Speaks gh0st_w6_nightmare_release

        // Nightmare 3: Orion
        navigate("source_orion_nightmare")
        combat(listOf("silent_shore_wraith"), "source_orion_nightmare") // triggers w6_mq26_rescue_orion
        talk("Orion") // Speaks orion_w6_nightmare_release

        // Reassembly at Campfire
        navigate("source_campfire")
        doAction("w6_mq26_reassemble") // awards Key relic, completes MQ26, warps to source_echo_mines, starts MQ27

        assertTrue("w6_mq26 completed", store.state.value.completedQuests.contains("w6_mq26"))
        assertTrue("Key relic acquired", (store.state.value.inventory["key_relic"] ?: 0) >= 1)
        assertTrue("w6_mq27 active", store.state.value.activeQuests.contains("w6_mq27"))
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 2. MQ27: The Echo of the Mines (Distorted Mines, Evade Manager, Elevator)
        navigate("source_echo_mines") // triggers w6_mq27_enter_mines
        doAction("w6_mq27_evade_manager")
        navigate("source_echo_elevator") // triggers w6_mq27_reach_elevator -> completes MQ27, warps to source_memory_bridge, starts MQ28

        assertTrue("w6_mq27 completed", store.state.value.completedQuests.contains("w6_mq27"))
        assertTrue("w6_mq28 active", store.state.value.activeQuests.contains("w6_mq28"))
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 3. MQ28: The Crossing (4 Memory Anchors, Build Bridge, Banter, Cross to Singularity)
        navigate("source_memory_bridge")
        doAction("w6_mq28_anchor_zeke")
        doAction("w6_mq28_anchor_ghost")
        doAction("w6_mq28_anchor_orion")
        doAction("w6_mq28_anchor_nova")
        doAction("w6_mq28_build_bridge")
        doAction("w6_mq28_final_banter")
        doAction("w6_mq28_reach_singularity") // completes MQ28, warps to source_memory_stair, starts MQ29

        assertTrue("w6_mq28 completed", store.state.value.completedQuests.contains("w6_mq28"))
        assertTrue("w6_mq29 active", store.state.value.activeQuests.contains("w6_mq29"))
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 4. MQ29: The Spire of Thought (Refuse Revisions, Climb Stair, Shadow Wave, Memory Guard, Center)
        navigate("source_memory_stair")
        doAction("w6_mq29_refuse_jed_revision")
        doAction("w6_mq29_refuse_astra_revision")
        doAction("w6_mq29_refuse_foundry_revision")
        doAction("w6_mq29_climb_stair")
        combat(listOf("source_shadow", "distorted_sentinel", "glitch_hound"), "source_memory_stair") // triggers w6_mq29_shadow_wave
        combat(listOf("memory_leak", "nightmare_guard"), "source_spire_arena") // triggers w6_mq29_memory_guard
        navigate("source_center") // triggers w6_mq29_reach_center -> completes MQ29, starts MQ30

        assertTrue("w6_mq29 completed", store.state.value.completedQuests.contains("w6_mq29"))
        assertTrue("w6_mq30 active", store.state.value.activeQuests.contains("w6_mq30"))
        talk("Zeke")
        talk("Orion")
        talk("Gh0st")

        // 5. MQ30: The Final Note (Confront Vale, Defeat Soloist, Shatter Silence, Tune the World)
        talk("Vale") // Triggers w6_mq30_confront_vale
        combat(listOf("ascended_vale"), "source_center") // triggers w6_mq30_defeat_vale
        combat(listOf("ascended_god"), "source_center") // triggers w6_mq30_defeat_god
        doAction("w6_mq30_tune_world") // unlocks source_art_tune_world, completes MQ30, sets ms_game_complete, warps to source_new_world

        assertTrue("w6_mq30 completed", store.state.value.completedQuests.contains("w6_mq30"))
        assertTrue("ms_w6_mq30_complete set", store.state.value.completedMilestones.contains("ms_w6_mq30_complete"))
        assertTrue("ms_game_complete set", store.state.value.completedMilestones.contains("ms_game_complete"))
        assertTrue("ms_credits_seen set", store.state.value.completedMilestones.contains("ms_credits_seen"))
        assertEquals("source_new_world", store.state.value.roomId)
        assertTrue("Tune World skill unlocked", store.state.value.unlockedSkills.contains("source_art_tune_world"))

        // Export report
        val reportFile = File(root, "docs/playtest/reports/WORLD_6_PLAYTHROUGH_AUDIT.md")
        auditor.exportReportToFile(reportFile)
        println("Audited World 6 Playthrough report exported to: ${reportFile.absolutePath}")
    }

    private inline fun <reified T> readList(path: String): List<T> {
        val file = File(assets, path)
        assertTrue("Asset file $path must exist", file.exists())
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(file.readText()) ?: emptyList()
    }
}
