package com.example.starborn.feature.exploration.skilltree

import com.example.starborn.domain.model.SkillTreeDefinition
import com.example.starborn.domain.model.SkillTreeNode
import kotlin.math.max

data class SkillNodeStatus(
    val unlocked: Boolean,
    val canPurchase: Boolean,
    val hasEnoughAp: Boolean,
    val meetsTierRequirement: Boolean,
    val unmetRequirements: List<String>,
    val requiredApForTier: Int,
    val apInvested: Int
)

fun skillTreeNodes(tree: SkillTreeDefinition): List<SkillTreeNode> =
    tree.branches.values.flatten()

fun skillTreeApInvested(tree: SkillTreeDefinition, unlockedSkills: Set<String>): Int =
    skillTreeNodes(tree)
        .filter { unlockedSkills.contains(it.id) }
        .sumOf { node -> max(0, node.costAp) }

fun requiredApForRow(row: Int): Int = row.coerceAtLeast(0) * 5

fun nodeRow(node: SkillTreeNode): Int = node.pos?.getOrNull(1) ?: 0

fun nodeColumn(node: SkillTreeNode): Int = node.pos?.getOrNull(0) ?: 0

fun nodeGridPosition(node: SkillTreeNode): Pair<Int, Int>? {
    val row = node.pos?.getOrNull(1) ?: return null
    val col = node.pos?.getOrNull(0) ?: return null
    return row to col
}

fun isPlaceholderNode(node: SkillTreeNode): Boolean {
    if (node.id.isBlank() || node.id == "-") return true
    if (node.name.isBlank()) return true
    return false
}

private fun unmetRequirements(
    node: SkillTreeNode,
    unlocked: Set<String>,
    completedMilestones: Set<String>
): List<String> = node.requires.orEmpty().filterNot { req ->
    unlocked.contains(req) || completedMilestones.contains(req)
}

fun evaluateNodeStatus(
    node: SkillTreeNode,
    tree: SkillTreeDefinition,
    unlockedSkills: Set<String>,
    completedMilestones: Set<String>,
    availableAp: Int,
    apInvested: Int = skillTreeApInvested(tree, unlockedSkills)
): SkillNodeStatus {
    val unlocked = unlockedSkills.contains(node.id)
    val requiredForTier = requiredApForRow(nodeRow(node))
    val meetsTier = apInvested >= requiredForTier
    val unmet = unmetRequirements(node, unlockedSkills, completedMilestones)
    val hasAp = availableAp >= max(0, node.costAp)
    val canPurchase = !unlocked && hasAp && meetsTier && unmet.isEmpty()
    return SkillNodeStatus(
        unlocked = unlocked,
        canPurchase = canPurchase,
        hasEnoughAp = hasAp,
        meetsTierRequirement = meetsTier,
        unmetRequirements = unmet,
        requiredApForTier = requiredForTier,
        apInvested = apInvested
    )
}
