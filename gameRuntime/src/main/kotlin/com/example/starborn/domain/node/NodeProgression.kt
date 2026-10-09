package com.example.starborn.domain.node

import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.NodeRequirement
import com.example.starborn.domain.session.GameSessionState

enum class NodeVisibility { HIDDEN, REVEALED }

data class NodeAvailability(
    val visibility: NodeVisibility,
    val unlocked: Boolean,
    val visited: Boolean,
    val completed: Boolean,
    val canEnterFromHub: Boolean,
    val unmetRequirement: String? = null
)

class NodeProgressionEvaluator {
    fun evaluate(node: HubNode, state: GameSessionState): NodeAvailability {
        val initial = node.initialVisibility?.lowercase()
            ?: if (node.discovered) "unlocked" else "hidden"
        val requirementsMet = node.unlockConditions.all { requirementMet(it, state) }
        val visited = node.id in state.visitedNodes
        val explicitlyUnlocked = node.id in state.unlockedNodes
        val initiallyUnlocked = initial in setOf("unlocked", "visited")
        val revealed = visited || explicitlyUnlocked || node.id in state.revealedNodes || initial != "hidden"
        val unlocked = visited || explicitlyUnlocked || (initiallyUnlocked && requirementsMet) ||
            (node.unlockConditions.isNotEmpty() && requirementsMet)
        val canEnter = unlocked && (visited || !node.entryPolicy.equals("transition", ignoreCase = true))
        return NodeAvailability(
            visibility = if (revealed) NodeVisibility.REVEALED else NodeVisibility.HIDDEN,
            unlocked = unlocked,
            visited = visited,
            completed = node.id in state.completedNodes,
            canEnterFromHub = canEnter,
            unmetRequirement = if (requirementsMet) {
                null
            } else {
                node.lockedMessage?.takeIf { it.isNotBlank() }
                    ?: node.unlockConditions.firstOrNull { !requirementMet(it, state) }?.let(::describe)
            }
        )
    }

    private fun requirementMet(requirement: NodeRequirement, state: GameSessionState): Boolean {
        return when (requirement.type.lowercase()) {
            "milestone" -> requirement.id?.let { it in state.completedMilestones } == true
            "quest_completed" -> (requirement.questId ?: requirement.id)?.let { it in state.completedQuests } == true
            "quest_active" -> (requirement.questId ?: requirement.id)?.let { it in state.activeQuests } == true
            "quest_task_done" -> {
                val questId = requirement.questId
                val taskId = requirement.taskId
                questId != null && taskId != null && taskId in state.questTasksCompleted[questId].orEmpty()
            }
            "item" -> (requirement.itemId ?: requirement.id)?.let { it in state.inventory } == true
            "node_visited" -> requirement.id?.let { it in state.visitedNodes } == true
            "node_completed" -> requirement.id?.let { it in state.completedNodes } == true
            else -> false
        }
    }

    private fun describe(requirement: NodeRequirement): String = when (requirement.type.lowercase()) {
        "milestone" -> "Requires ${requirement.id.orEmpty().toLabel()}"
        "quest_completed" -> "Complete ${(requirement.questId ?: requirement.id).orEmpty().toLabel()}"
        "quest_active" -> "Requires ${(requirement.questId ?: requirement.id).orEmpty().toLabel()}"
        "quest_task_done" -> "Requires ${requirement.taskId.orEmpty().toLabel()}"
        "item" -> "Requires ${(requirement.itemId ?: requirement.id).orEmpty().toLabel()}"
        "node_visited" -> "Discover ${requirement.id.orEmpty().toLabel()}"
        "node_completed" -> "Complete ${requirement.id.orEmpty().toLabel()}"
        else -> "Locked"
    }

    private fun String.toLabel(): String = replace('_', ' ').trim().replaceFirstChar { it.uppercase() }
}
