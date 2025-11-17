package com.example.starborn.data

import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.assets.CinematicSceneAsset
import com.example.starborn.data.assets.CinematicStepAsset
import com.example.starborn.domain.cinematic.CinematicScene
import com.example.starborn.domain.cinematic.CinematicStep
import com.example.starborn.domain.cinematic.CinematicStepType
import com.example.starborn.domain.model.EventAction
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.MilestoneDefinition
import com.example.starborn.domain.model.MilestoneExitUnlock
import com.example.starborn.domain.model.Quest
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DataIntegrityTest {

    private val moshi = MoshiProvider.instance
    private val allowedMissingQuestIds = setOf("q_mine_relay_scramble")

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
}

data class RoomSummary(
    val id: String
)

data class TutorialScriptSummary(
    val id: String?
)
