package com.example.starborn.data

import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.assets.CinematicSceneAsset
import com.example.starborn.data.assets.CinematicStepAsset
import com.example.starborn.domain.cinematic.CinematicScene
import com.example.starborn.domain.cinematic.CinematicStep
import com.example.starborn.domain.cinematic.CinematicStepType
import com.example.starborn.domain.model.EventAction
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.MilestoneDefinition
import com.example.starborn.domain.model.MilestoneExitUnlock
import com.example.starborn.domain.model.NodeTransition
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.TuningPuzzle
import com.example.starborn.data.local.Theme
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataIntegrityTest {

    private val moshi = MoshiProvider.instance
    private val allowedMissingQuestIds = setOf(
        "q_mine_relay_scramble",
        "gather_broken_gear",
        "talk_to_jed",
        "fixers_favor",
        "lights_out",
        "checking_the_fuse",
        "scrap_run",
        "tutorial_smoke"
    )

    @Test
    fun runtimeThemesUseRgbaArrays() {
        val themes = readMap("src/main/assets/themes.json", String::class.java, Theme::class.java)

        assertTrue("Runtime themes should not be empty", themes.isNotEmpty())
        themes.forEach { (id, theme) ->
            listOf(theme.bg, theme.fg, theme.border, theme.accent).forEach { color ->
                assertEquals("$id colors must have four RGBA components", 4, color.size)
                assertTrue("$id colors must be normalized", color.all { it in 0f..1f })
            }
        }
    }

    @Test
    fun runtimeRoomsDeserializeWithRequiredFields() {
        val rooms = readList("src/main/assets/rooms.json", Room::class.java)

        assertTrue("Runtime rooms should not be empty", rooms.isNotEmpty())
        assertEquals("Room ids must be unique", rooms.size, rooms.map { it.id }.distinct().size)
    }

    @Test
    fun dialogueVoiceProfilesCoverAuthoredSpeakers() {
        val dialogueText = File("src/main/assets/dialogue.json").readText()
        val speakers = Regex("\"speaker\"\\s*:\\s*\"([^\"]+)\"")
            .findAll(dialogueText)
            .map { it.groupValues[1] }
            .toSet()
        val profiles = readObject("src/main/assets/dialogue_voice_profiles.json", DialogueVoiceProfilesSummary::class.java)
        val validProfiles = setOf("female", "male", "none")
        val missing = speakers - profiles.profiles.keys
        val invalid = profiles.profiles.filterValues { it !in validProfiles }

        assertTrue("Dialogue voice profiles should cover all speakers (missing: $missing)", missing.isEmpty())
        assertTrue("Dialogue voice profiles should use valid values (invalid: $invalid)", invalid.isEmpty())
    }

    @Test
    fun worldTwoRoomsFormReciprocalNavigationGraphs() {
        val rooms = readList("src/main/assets/rooms.json", Room::class.java)
            .filter { it.id.startsWith("sector9_") }
            .associateBy { it.id }
        val opposite = mapOf(
            "north" to "south",
            "south" to "north",
            "east" to "west",
            "west" to "east",
            "northeast" to "southwest",
            "southwest" to "northeast",
            "northwest" to "southeast",
            "southeast" to "northwest"
        )

        assertEquals("World 2 should expose all authored rooms", 91, rooms.size)
        rooms.values.forEach { room ->
            assertTrue("${room.id} should not be an isolated scene", room.connections.isNotEmpty())
            room.connections.forEach { (direction, targetId) ->
                val target = rooms[targetId]
                assertTrue("${room.id} points to missing World 2 room $targetId", target != null)
                val reverse = opposite[direction]
                assertEquals("$room.id -> $targetId must be reciprocal", room.id, target?.connections?.get(reverse))
            }
        }
    }

    @Test
    fun tuningPuzzleRoomActionsReferenceValidDefinitionsAndEvents() {
        val rooms = readList("src/main/assets/rooms.json", Room::class.java)
        val puzzles = readList("src/main/assets/tuning_puzzles.json", TuningPuzzle::class.java)
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
        val puzzleIds = puzzles.map { it.id }.toSet()
        val eventIds = events.map { it.id }.toSet()
        val roomReferences = rooms.flatMap { room ->
            room.actions.mapNotNull { action ->
                val type = action["type"] as? String
                val puzzleId = action["puzzle_id"] as? String
                if (type.equals("tuning_puzzle", ignoreCase = true)) {
                    room.id to puzzleId.orEmpty()
                } else {
                    null
                }
            }
        }
        val missingPuzzles = roomReferences
            .filter { (_, puzzleId) -> puzzleId !in puzzleIds }
            .map { (roomId, puzzleId) -> "$roomId -> $puzzleId" }
        val missingEvents = puzzles
            .filter { it.successEvent !in eventIds }
            .map { "${it.id} -> ${it.successEvent}" }

        assertTrue("Tuning puzzle room actions should reference valid puzzle ids: $missingPuzzles", missingPuzzles.isEmpty())
        assertTrue("Tuning puzzle success events should exist: $missingEvents", missingEvents.isEmpty())
    }

    @Test
    fun forgePuzzleSliceUsesEnvironmentalActions() {
        val rooms = readList("src/main/assets/rooms.json", Room::class.java)
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
        val eventIds = events.map { it.id }.toSet()
        val forgeRooms = rooms.filter {
            it.id == "foundry_forge_anvil" ||
                it.id == "foundry_forge_control_alcove" ||
                it.id.startsWith("foundry_conveyor_belt_scale_") ||
                it.id.startsWith("foundry_forge_scale_")
        }
        val requiredForgeMilestones = listOf(
            "ms_w4_pulse_board_read",
            "ms_w4_grease_route_read",
            "ms_w4_paddle_thrown",
            "ms_w4_pistons_starved"
        )
        val puzzleLabels = forgeRooms.flatMap { room ->
            room.actions.mapNotNull { action ->
                val label = action["name"] as? String
                if (label?.contains("puzzle", ignoreCase = true) == true) "${room.id}:$label" else null
            }
        }
        val forgeTuningReferences = forgeRooms.flatMap { room ->
            room.actions.mapNotNull { action ->
                val type = action["type"] as? String
                val puzzleId = action["puzzle_id"] as? String
                if (type.equals("tuning_puzzle", ignoreCase = true) || puzzleId == "forge_conveyor_timing") {
                    "${room.id}:${puzzleId.orEmpty()}"
                } else {
                    null
                }
            }
        }
        val missingEvents = forgeRooms.flatMap { room ->
            room.actions.mapNotNull { action ->
                val eventId = action["action_event"] as? String
                if (!eventId.isNullOrBlank() && eventId !in eventIds) "${room.id}:$eventId" else null
            }
        }
        val openCradleActions = forgeRooms.flatMap { room ->
            room.actions.filter { action -> action["action_event"] == "w4_mq19_open_anvil_cradle" }
                .map { action -> room.id to action }
        }
        val cradleActionsMissingRequirements = openCradleActions.mapNotNull { (roomId, action) ->
            @Suppress("UNCHECKED_CAST")
            val requires = action["requires_milestones"] as? List<String>
            if (requires == requiredForgeMilestones) null else "$roomId:${action["name"]}"
        }
        val openCradleEvent = events.firstOrNull { it.id == "w4_mq19_open_anvil_cradle" }
        val openCradleEventMilestones = openCradleEvent?.conditions
            ?.filter { it.type == "milestone_set" }
            ?.mapNotNull { it.milestone }
            .orEmpty()

        assertTrue("Forge puzzle labels should use in-world nouns, not 'puzzle': $puzzleLabels", puzzleLabels.isEmpty())
        assertTrue("Forge puzzle slice should not use tuning puzzle actions: $forgeTuningReferences", forgeTuningReferences.isEmpty())
        assertTrue("Forge puzzle action events should exist: $missingEvents", missingEvents.isEmpty())
        assertTrue("Forge cradle actions should require the environmental chain: $cradleActionsMissingRequirements", cradleActionsMissingRequirements.isEmpty())
        assertTrue("Forge open-cradle event should require the environmental chain.", openCradleEventMilestones.containsAll(requiredForgeMilestones))
    }

    @Test
    fun worldThreePrismGalleryUsesEnvironmentalActions() {
        val rooms = readList("src/main/assets/rooms.json", Room::class.java)
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
        val prismRooms = rooms.filter { it.id == "spire_prism_gallery" || it.id == "spire_archive_vault" }
        val requiredMilestones = listOf(
            "ms_w3_containment_field_read",
            "ms_w3_prism_shutters_read",
            "ms_w3_command_tethers_traced"
        )
        val puzzleLabels = prismRooms.flatMap { room ->
            room.actions.mapNotNull { action ->
                val label = action["name"] as? String
                if (label?.contains("light puzzle", ignoreCase = true) == true) "${room.id}:$label" else null
            }
        }
        val tuningReferences = prismRooms.flatMap { room ->
            room.actions.mapNotNull { action ->
                val type = action["type"] as? String
                val puzzleId = action["puzzle_id"] as? String
                if (type.equals("tuning_puzzle", ignoreCase = true) || puzzleId == "prism_gallery_light") {
                    "${room.id}:${puzzleId.orEmpty()}"
                } else {
                    null
                }
            }
        }
        val solveEventMilestones = events.firstOrNull { it.id == "w3_mq14_solve_light_puzzle" }?.conditions
            ?.filter { it.type == "milestone_set" }
            ?.mapNotNull { it.milestone }
            .orEmpty()

        assertTrue("Prism Gallery should not expose a literal light puzzle label: $puzzleLabels", puzzleLabels.isEmpty())
        assertTrue("Prism Gallery should not use tuning puzzle actions: $tuningReferences", tuningReferences.isEmpty())
        assertTrue("Prism resolution should require the environmental reads.", solveEventMilestones.containsAll(requiredMilestones))
    }

    @Test
    fun lateWorldPuzzleBeatsUseWorldSystems() {
        val rooms = readList("src/main/assets/rooms.json", Room::class.java)
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
        val roomsById = rooms.associateBy { it.id }
        val eventsById = events.associateBy { it.id }
        fun requires(roomId: String, actionEvent: String): List<String> {
            val action = roomsById[roomId]?.actions.orEmpty()
                .firstOrNull { it["action_event"] == actionEvent }
            @Suppress("UNCHECKED_CAST")
            return action?.get("requires_milestones") as? List<String> ?: emptyList()
        }
        fun conditionMilestones(eventId: String): List<String> =
            eventsById[eventId]?.conditions.orEmpty()
                .filter { it.type == "milestone_set" }
                .mapNotNull { it.milestone }

        val w5Text = File("src/main/assets/quests.json").readText() +
            File("src/main/assets/dialogue.json").readText() +
            File("src/main/assets/events.json").readText()
        val w6BridgeMilestones = listOf(
            "ms_w6_bridge_zeke_anchor",
            "ms_w6_bridge_ghost_anchor",
            "ms_w6_bridge_orion_anchor",
            "ms_w6_bridge_nova_anchor"
        )
        val w6StairMilestones = listOf(
            "ms_w6_refused_jed_revision",
            "ms_w6_refused_astra_revision",
            "ms_w6_refused_foundry_revision"
        )

        assertTrue("W5 player-facing text should say server route, not server maze.", !w5Text.contains("server maze", ignoreCase = true))
        assertTrue("W5 quest flavor should not call the route a maze.", !w5Text.contains("So is the maze.", ignoreCase = true))
        assertTrue("W6 bridge should require separate memory anchors.", requires("source_memory_bridge", "w6_mq28_build_bridge") == w6BridgeMilestones)
        assertTrue("W6 bridge event should require separate memory anchors.", conditionMilestones("w6_mq28_build_bridge").containsAll(w6BridgeMilestones))
        assertTrue("W6 stair should require refusing Vale's revisions.", requires("source_memory_stair", "w6_mq29_climb_stair") == w6StairMilestones)
        assertTrue("W6 stair event should require refusing Vale's revisions.", conditionMilestones("w6_mq29_climb_stair").containsAll(w6StairMilestones))
    }

    @Test
    fun worldTwoPuzzleBeatsUseEnvironmentalChains() {
        val rooms = readList("src/main/assets/rooms.json", Room::class.java)
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
        val eventIds = events.map { it.id }.toSet()
        val roomsById = rooms.associateBy { it.id }
        val eventsById = events.associateBy { it.id }
        val stasisMilestones = listOf(
            "ms_w2_murals_read",
            "ms_w2_stasis_pod_read",
            "ms_w2_stasis_overview_read",
            "ms_w2_coolant_stabilized"
        )
        val sourceMilestones = listOf(
            "ms_w2_source_horn_stabilized",
            "ms_w2_source_cup_grounded",
            "ms_w2_source_pressure_mapped",
            "ms_w2_source_breakers_starved"
        )
        val crystalMilestones = listOf(
            "ms_w2_crystal_west_seated",
            "ms_w2_crystal_east_seated",
            "ms_w2_crystal_north_seated"
        )
        fun requires(roomId: String, actionEvent: String): List<String> {
            val action = roomsById[roomId]?.actions.orEmpty()
                .firstOrNull { it["action_event"] == actionEvent }
            @Suppress("UNCHECKED_CAST")
            return action?.get("requires_milestones") as? List<String> ?: emptyList()
        }
        fun conditionMilestones(eventId: String): List<String> =
            eventsById[eventId]?.conditions.orEmpty()
                .filter { it.type == "milestone_set" }
                .mapNotNull { it.milestone }

        val w2PuzzleEvents = listOf(
            "w2_mq03_align_complete",
            "w2_mq05_bypass_gate",
            "w2_sq04_complete",
            "w2_mq03_read_mural_overview",
            "w2_mq03_stabilize_coolant",
            "w2_mq05_stabilize_horn",
            "w2_mq05_ground_cup",
            "w2_mq05_read_pressure_gauge",
            "w2_mq05_overload_breakers"
        )
        val missingEvents = w2PuzzleEvents.filterNot { it in eventIds }
        val dialogueText = File("src/main/assets/dialogue.json").readText()
        val questText = File("src/main/assets/quests.json").readText()

        assertTrue("W2 puzzle action events should exist: $missingEvents", missingEvents.isEmpty())
        assertTrue("Stasis ring console should require environmental readings.", requires("sector9_stasis_ring_array", "w2_mq03_align_complete") == stasisMilestones)
        assertTrue("Stasis completion event should require environmental readings.", conditionMilestones("w2_mq03_align_complete").containsAll(stasisMilestones))
        assertTrue("Source acoustic lock should require hardware stabilization.", requires("sector9_source_gate", "w2_mq05_bypass_gate") == sourceMilestones)
        assertTrue("Source Gate completion event should require hardware stabilization.", conditionMilestones("w2_mq05_bypass_gate").containsAll(sourceMilestones))
        assertTrue("Ancient Echoes mural actions should require crystal voices.", requires("sector9_hall_of_echoes", "w2_sq04_complete") == crystalMilestones)
        assertTrue("Ancient Echoes completion event should require crystal voices.", conditionMilestones("w2_sq04_complete").containsAll(crystalMilestones))
        assertTrue("W2 dialogue should not expose old chord-answer menu copy.", !dialogueText.contains("Broadcast C# Minor 7") && !dialogueText.contains("Tune resonator to C# Minor 7"))
        assertTrue("W2 quest text should not ask players to solve the stasis code.", !questText.contains("Solve the stasis pod ring alignment code."))
    }

    @Test
    fun restStopRoomActionsReferenceValidEvents() {
        val rooms = readList("src/main/assets/rooms.json", Room::class.java)
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
        val eventIds = events.map { it.id }.toSet()
        val missingEvents = rooms.flatMap { room ->
            room.actions.mapNotNull { action ->
                val type = action["type"] as? String
                val restEvent = action["rest_event"] as? String
                if (type.equals("rest_stop", ignoreCase = true) && !restEvent.isNullOrBlank() && restEvent !in eventIds) {
                    "${room.id} -> $restEvent"
                } else {
                    null
                }
            }
        }

        assertTrue("Rest stop actions should reference valid rest events: $missingEvents", missingEvents.isEmpty())
    }

    @Test
    fun worldThreeHasBuiltoutRoomGraph() {
        val rooms = readList("src/main/assets/rooms.json", Room::class.java)
        val roomIds = rooms.map { it.id }.toSet()
        val worldThreeRooms = rooms.filter { it.id.startsWith("spire_") }
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
            .map { it.id }
            .toSet()
        val nodes = readList("src/main/assets/hub_nodes.json", HubNode::class.java)
            .filter { it.hubId in setOf("hub_5_lower_city", "hub_6_upper_city") }

        assertTrue("World 3 should be built out beyond the initial skeleton", worldThreeRooms.size >= 32)

        val missingNodeRooms = nodes.flatMap { node ->
            node.rooms.filterNot { it in roomIds }.map { "${node.id}:$it" }
        }
        val missingActionEvents = worldThreeRooms.flatMap { room ->
            room.actions.mapNotNull { action ->
                val eventId = action["action_event"] as? String
                if (!eventId.isNullOrBlank() && eventId !in events) "${room.id}:$eventId" else null
            }
        }

        assertTrue("World 3 hub nodes should reference valid rooms (missing: $missingNodeRooms)", missingNodeRooms.isEmpty())
        assertTrue("World 3 room actions should reference valid events (missing: $missingActionEvents)", missingActionEvents.isEmpty())
    }

    @Test
    fun playCinematicActionsReferenceExistingScenes() {
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
        val cinematics = readCinematics()
        val sceneIds = cinematics.map { it.id }.toSet()
        val missing = mutableListOf<String>()

        events.forEach { event ->
            traverseActions(event.actions) { action ->
                if (action.type.equals("play_cinematic", ignoreCase = true) ||
                    action.type.equals("trigger_cutscene", ignoreCase = true)
                ) {
                    val id = action.sceneId
                    if (!id.isNullOrBlank() && id !in sceneIds) {
                        missing += "${event.id}:${id}"
                    }
                }
            }
        }

        assertTrue(
            "All cinematic actions should reference scenes (missing: $missing)",
            missing.isEmpty()
        )
    }

    @Test
    fun milestoneExitUnlocksReferenceValidRooms() {
        val milestones = readList("src/main/assets/milestones.json", MilestoneDefinition::class.java)
        val rooms = readList("src/main/assets/rooms.json", RoomSummary::class.java)
            .associateBy { it.id }
        val invalid = mutableListOf<String>()

        milestones.forEach { milestone ->
            val exits = milestone.effects?.unlockExits.orEmpty()
            exits.forEach { unlock ->
                if (!isUnlockValid(unlock, rooms)) {
                    invalid += "${milestone.id}:${unlock.roomId}:${unlock.direction}"
                }
            }
        }

        assertTrue(
            "Milestone unlock exits should reference known rooms/directions (invalid: $invalid)",
            invalid.isEmpty()
        )
    }

    @Test
    fun systemTutorialActionsReferenceScripts() {
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
        val tutorialScripts = readList("src/main/assets/tutorial_scripts.json", TutorialScriptSummary::class.java)
            .mapNotNull { it.id?.takeIf { id -> id.isNotBlank() } }
            .toSet()
        val missing = mutableListOf<String>()

        events.forEach { event ->
            traverseActions(event.actions) { action ->
                if (action.type.equals("system_tutorial", ignoreCase = true)) {
                    val sceneId = action.sceneId
                    if (!sceneId.isNullOrBlank() && sceneId !in tutorialScripts) {
                        missing += "${event.id}:${sceneId}"
                    }
                }
            }
        }

        assertTrue(
            "All system_tutorial actions should map to tutorial_scripts entries (missing: $missing)",
            missing.isEmpty()
        )
    }

    @Test
    fun questReferencesAreValid() {
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
        val quests = readList("src/main/assets/quests.json", Quest::class.java)
        val questIds = quests.map { it.id }.toSet()
        val stagesByQuest = quests.associate { quest ->
            quest.id to quest.stages.map { it.id }.toSet()
        }
        val tasksByQuest = quests.associate { quest ->
            quest.id to quest.stages.flatMap { stage -> stage.tasks.map { it.id } }.toSet()
        }
        val missing = mutableListOf<String>()

        fun validateQuest(source: String, questId: String?) {
            val id = questId?.takeIf { it.isNotBlank() } ?: return
            if (id in allowedMissingQuestIds) return
            if (id !in questIds) {
                missing += "$source quest:$id"
            }
        }

        fun validateStage(source: String, questId: String?, stageId: String?) {
            val q = questId?.takeIf { it.isNotBlank() } ?: return
            if (q !in questIds && q in allowedMissingQuestIds) return
            val stage = stageId?.takeIf { it.isNotBlank() } ?: return
            val stages = stagesByQuest[q]
            if (stages == null || stage !in stages) {
                missing += "$source stage:$q::$stage"
            }
        }

        fun validateTask(source: String, questId: String?, taskId: String?) {
            val q = questId?.takeIf { it.isNotBlank() } ?: return
            if (q !in questIds && q in allowedMissingQuestIds) return
            val task = taskId?.takeIf { it.isNotBlank() } ?: return
            val tasks = tasksByQuest[q]
            if (tasks == null || task !in tasks) {
                missing += "$source task:$q::$task"
            }
        }

        events.forEach { event ->
            traverseActions(event.actions) { action ->
                validateQuest("${event.id}:${action.type}", action.questId)
                validateQuest("${event.id}:${action.type}:start", action.startQuest)
                validateQuest("${event.id}:${action.type}:complete", action.completeQuest)
                validateStage("${event.id}:${action.type}", action.questId, action.toStageId)
                validateTask("${event.id}:${action.type}", action.questId, action.taskId)
            }
            event.conditions.forEach { condition ->
                validateQuest("${event.id}:condition:${condition.type}", condition.questId)
                validateStage("${event.id}:condition:${condition.type}", condition.questId, condition.stageId)
                validateTask("${event.id}:condition:${condition.type}", condition.questId, condition.taskId)
            }
        }

        assertTrue(
            "Quest/stage/task references must match quests.json (missing: $missing)",
            missing.isEmpty()
        )
    }

    @Test
    fun questTutorialIdsAndSystemTutorialActionsReferenceScripts() {
        val quests = readList("src/main/assets/quests.json", Quest::class.java)
        val events = readList("src/main/assets/events.json", GameEvent::class.java)
        val tutorialScripts = readList(
            "src/main/assets/tutorial_scripts.json",
            TutorialScriptSummary::class.java
        )
            .mapNotNull { it.id?.takeIf { id -> id.isNotBlank() } }
            .toSet()
        val missing = mutableListOf<String>()

        quests.forEach { quest ->
            quest.stages.forEach { stage ->
                stage.tasks.forEach { task ->
                    val tutorialId = task.tutorialId?.takeIf { it.isNotBlank() } ?: return@forEach
                    if (tutorialId !in tutorialScripts) {
                        missing += "${quest.id}:${stage.id}:${task.id} -> $tutorialId"
                    }
                }
            }
        }

        events.forEach { event ->
            traverseActions(event.actions) { action ->
                if (action.type.equals("system_tutorial", ignoreCase = true)) {
                    val sceneId = action.sceneId?.takeIf { it.isNotBlank() } ?: return@traverseActions
                    if (sceneId !in tutorialScripts) {
                        missing += "${event.id}:${sceneId}"
                    }
                }
            }
        }

        assertTrue(
            "Quest tutorial_id + system_tutorial scene_id entries must reference tutorial_scripts (missing: $missing)",
            missing.isEmpty()
        )
    }

    @Test
    fun hubOneNodesUsePublicThresholdRoomsAsEntries() {
        val expectedEntries = mapOf(
            "pit" to "pit_L1_landing",
            "workshop" to "workshop_yard",
            "med_bay" to "medbay_hall",
            "trade_row" to "trade_entrance",
            "admin_gate" to "checkpoint_queue"
        )
        val nodes = readList("src/main/assets/hub_nodes.json", HubNode::class.java)
            .filter { it.hubId == "hub_1_homestead" }
            .associateBy { it.id }
        val roomIds = readList("src/main/assets/rooms.json", RoomSummary::class.java)
            .map { it.id }
            .toSet()

        assertEquals(expectedEntries.keys, nodes.keys)
        expectedEntries.forEach { (nodeId, entryRoomId) ->
            val node = requireNotNull(nodes[nodeId]) { "Missing Hub 1 node $nodeId" }
            assertEquals("$nodeId should enter through its public threshold", entryRoomId, node.entryRoom)
            assertTrue("$nodeId entry room must belong to the node", entryRoomId in node.rooms)
            assertTrue("$nodeId entry room must exist", entryRoomId in roomIds)
            assertEquals("$nodeId should list its entry room first", entryRoomId, node.rooms.firstOrNull())
        }
    }

    @Test
    fun worldThreeHubStartsMatchStoryEntryPoints() {
        val nodes = readList("src/main/assets/hub_nodes.json", HubNode::class.java)
            .filter { it.hubId in setOf("hub_5_lower_city", "hub_6_upper_city") }
            .associateBy { it.id }

        val sewers = requireNotNull(nodes["spire_sewers"]) { "Missing World 3 Sewers node" }
        assertEquals("spire_sewers_landing", sewers.entryRoom)
        assertEquals("unlocked", sewers.initialVisibility)
        assertEquals("hub", sewers.entryPolicy)

        val ventOutput = requireNotNull(nodes["spire_vent_output"]) { "Missing World 3 Vent Output node" }
        assertEquals("hidden", ventOutput.initialVisibility)
        assertEquals("transition", ventOutput.entryPolicy)

        val laundry = requireNotNull(nodes["spire_laundry"]) { "Missing World 3 Laundry Service node" }
        assertEquals("spire_laundry_service", laundry.entryRoom)
        assertEquals("unlocked", laundry.initialVisibility)
        assertEquals("hub", laundry.entryPolicy)
    }

    @Test
    fun allHubNodeEntriesExistAndBelongToTheirNodes() {
        val nodes = readList("src/main/assets/hub_nodes.json", HubNode::class.java)
        val roomIds = readList("src/main/assets/rooms.json", RoomSummary::class.java)
            .map { it.id }
            .toSet()

        nodes.forEach { node ->
            assertTrue("${node.id} entry room must exist", node.entryRoom in roomIds)
            assertTrue("${node.id} entry room must belong to the node", node.entryRoom in node.rooms)
        }
    }

    @Test
    fun roomsHaveOneNodeOwnerAndCrossNodeExitsHaveTransitions() {
        val nodes = readList("src/main/assets/hub_nodes.json", HubNode::class.java)
        val rooms = readList("src/main/assets/rooms.json", Room::class.java).associateBy { it.id }
        val transitions = readList("src/main/assets/node_transitions.json", NodeTransition::class.java)
        val owners = mutableMapOf<String, String>()
        val duplicateOwners = mutableListOf<String>()

        nodes.forEach { node ->
            node.rooms.forEach { roomId ->
                val previous = owners.put(roomId, node.id)
                if (previous != null) duplicateOwners += "$roomId:$previous:${node.id}"
            }
        }
        assertTrue("Rooms must belong to only one node: $duplicateOwners", duplicateOwners.isEmpty())
        assertEquals("Transition ids must be unique", transitions.size, transitions.map { it.id }.distinct().size)

        val transitionByEdge = transitions.associateBy { "${it.fromRoom}::${it.direction.lowercase()}" }
        val missing = mutableListOf<String>()
        owners.forEach { (roomId, fromNode) ->
            rooms[roomId]?.connections.orEmpty().forEach { (direction, targetRoom) ->
                val toNode = owners[targetRoom]
                if (toNode != null && toNode != fromNode) {
                    val transition = transitionByEdge["$roomId::${direction.lowercase()}"]
                    if (transition?.fromNode != fromNode || transition.toNode != toNode || transition.toRoom != targetRoom) {
                        missing += "$roomId::$direction->$targetRoom"
                    }
                }
            }
        }
        assertTrue("Cross-node room exits require explicit node transitions: $missing", missing.isEmpty())
    }

    @Test
    fun shopItemsAndMilestonesAreValid() {
        val items = readList("src/main/assets/items.json", ItemSummary::class.java)
            .map { it.id }
            .toSet()
        val milestones = readList("src/main/assets/milestones.json", MilestoneDefinition::class.java)
            .map { it.id }
            .toSet()

        val allMilestones = milestones + setOf("MET_MECHANIC")

        val shops = readMap("src/main/assets/shops.json", String::class.java, ShopRawSummary::class.java)
        val missingItems = mutableListOf<String>()
        val missingMilestones = mutableListOf<String>()

        shops.forEach { (shopId, shop) ->
            shop.sells.items.forEach { itemId ->
                if (itemId !in items) {
                    missingItems += "$shopId:$itemId"
                }
            }
            shop.sells.gates.forEach { (itemId, gate) ->
                gate.milestones.forEach { milestoneId ->
                    if (milestoneId !in allMilestones) {
                        missingMilestones += "$shopId:$itemId:$milestoneId"
                    }
                }
            }
        }

        assertTrue("Shop items must exist in items.json (missing: $missingItems)", missingItems.isEmpty())
        assertTrue("Shop gates must exist in milestones.json (missing: $missingMilestones)", missingMilestones.isEmpty())
    }

    private fun isUnlockValid(unlock: MilestoneExitUnlock, rooms: Map<String, RoomSummary>): Boolean {
        val roomId = unlock.roomId?.takeIf { it.isNotBlank() } ?: return false
        val direction = unlock.direction?.takeIf { it.isNotBlank() } ?: return false
        val room = rooms[roomId] ?: return false
        val normalizedDir = direction.lowercase()
        val allowedDirections = setOf("north", "south", "east", "west")
        return room != null && normalizedDir in allowedDirections
    }

    private fun traverseActions(
        actions: List<EventAction>,
        block: (EventAction) -> Unit
    ) {
        val stack = ArrayDeque<EventAction>()
        stack.addAll(actions)
        while (stack.isNotEmpty()) {
            val action = stack.removeFirst()
            block(action)
            action.onComplete?.let { stack.addAll(it) }
            action.`do`?.let { stack.addAll(it) }
            action.elseDo?.let { stack.addAll(it) }
        }
    }

    private fun readCinematics(): List<CinematicScene> {
        val file = File("src/main/assets/cinematics.json")
        require(file.exists()) { "cinematics.json not found" }
        val adapter = moshi.adapter<List<CinematicSceneAsset>>(
            Types.newParameterizedType(List::class.java, CinematicSceneAsset::class.java)
        )
        val assets = requireNotNull(adapter.fromJson(file.readText())) { "Failed to parse cinematics" }
        return assets.map { asset ->
            CinematicScene(
                id = asset.id.orEmpty(),
                title = asset.title,
                steps = asset.steps.orEmpty().mapNotNull { step -> step?.toDomainStep() }
            )
        }
    }

    private fun CinematicStepAsset.toDomainStep(): CinematicStep? {
        val stepText = text ?: line ?: return null
        val type = CinematicStepType.fromRaw(this.type)
        return CinematicStep(
            type = type,
            speaker = speaker,
            text = stepText,
            durationSeconds = durationSeconds
        )
    }

    private fun <T> readList(path: String, clazz: Class<T>): List<T> {
        val file = File(path)
        require(file.exists()) { "$path not found" }
        val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, clazz))
        return requireNotNull(adapter.fromJson(file.readText())) {
            "Failed to parse $path"
        }
    }

    private fun <K, V> readMap(path: String, keyClazz: Class<K>, valClazz: Class<V>): Map<K, V> {
        val file = File(path)
        require(file.exists()) { "$path not found" }
        val mapType = Types.newParameterizedType(Map::class.java, keyClazz, valClazz)
        val adapter = moshi.adapter<Map<K, V>>(mapType)
        return requireNotNull(adapter.fromJson(file.readText())) {
            "Failed to parse $path"
        }
    }

    private fun <T> readObject(path: String, clazz: Class<T>): T {
        val file = File(path)
        require(file.exists()) { "$path not found" }
        val adapter = moshi.adapter(clazz)
        return requireNotNull(adapter.fromJson(file.readText())) {
            "Failed to parse $path"
        }
    }
}

data class RoomSummary(
    val id: String
)

data class TutorialScriptSummary(
    val id: String?
)

data class DialogueVoiceProfilesSummary(
    val default: String = "male",
    val profiles: Map<String, String> = emptyMap()
)

data class ItemSummary(
    val id: String
)

data class ShopRawSummary(
    val sells: ShopSellsSummary = ShopSellsSummary()
)

data class ShopSellsSummary(
    val items: List<String> = emptyList(),
    val gates: Map<String, ShopGateSummary> = emptyMap()
)

data class ShopGateSummary(
    val milestones: List<String> = emptyList()
)
