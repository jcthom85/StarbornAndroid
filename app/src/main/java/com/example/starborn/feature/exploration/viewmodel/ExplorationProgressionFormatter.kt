package com.example.starborn.feature.exploration.viewmodel

import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.SkillNodeEffect
import com.example.starborn.domain.model.SkillTreeDefinition
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.feature.exploration.skilltree.evaluateNodeStatus
import com.example.starborn.feature.exploration.skilltree.isPlaceholderNode
import com.example.starborn.feature.exploration.skilltree.nodeColumn
import com.example.starborn.feature.exploration.skilltree.nodeRow
import com.example.starborn.feature.exploration.skilltree.skillTreeApInvested
import java.util.Locale
import kotlin.math.max

internal fun buildPartyStatusUi(
    sessionState: GameSessionState,
    charactersById: Map<String, Player>,
    levelingManager: LevelingManager,
    skillsById: Map<String, Skill>
): PartyStatusUi {
    if (charactersById.isEmpty()) return PartyStatusUi()
    val partyIds = when {
        sessionState.partyMembers.isNotEmpty() -> sessionState.partyMembers
        sessionState.playerId != null -> listOf(sessionState.playerId)
        else -> charactersById.keys.sorted()
    }
    val members = partyIds.mapNotNull { id ->
        val character = charactersById[id] ?: return@mapNotNull null
        val xp = sessionState.partyMemberXp[id] ?: sessionState.playerXp.takeIf { id == sessionState.playerId } ?: character.xp
        val level = sessionState.partyMemberLevels[id]
            ?: sessionState.playerLevel.takeIf { id == sessionState.playerId }
            ?: character.level
        val bounds = levelingManager.levelBounds(level)
        val startXp = bounds.first
        val nextXp = bounds.second
        val progress = if (nextXp == null || nextXp <= startXp) {
            1f
        } else {
            val span = max(nextXp - startXp, 1)
            ((xp - startXp).coerceAtLeast(0).toFloat() / span).coerceIn(0f, 1f)
        }
        val xpLabel = buildString {
            append(xp)
            append(" XP")
            if (nextXp != null) {
                val toNext = max(nextXp - xp, 0)
                append(" • ")
                append(toNext)
                append(" to next")
            }
        }
        val maxHp = character.hp.coerceAtLeast(0)
        val currentHp = (sessionState.partyMemberHp[id] ?: maxHp).coerceAtLeast(0)
        val hpLabel = if (maxHp > 0) "$currentHp / $maxHp HP" else null
        val hpProgress = if (maxHp > 0) currentHp.toFloat() / maxHp else null
        val unlockedSkills = sessionState.unlockedSkills
            .mapNotNull { skillId -> skillsById[skillId]?.takeIf { it.character == character.id }?.name }
            .sorted()
        PartyMemberStatusUi(
            id = character.id,
            name = character.name,
            level = level,
            xpProgress = progress,
            xpLabel = xpLabel,
            hpLabel = hpLabel,
            hpProgress = hpProgress,
            portraitPath = character.miniIconPath,
            unlockedSkills = unlockedSkills
        )
    }
    return PartyStatusUi(members = members)
}

internal fun buildProgressionSummaryUi(
    sessionState: GameSessionState,
    charactersById: Map<String, Player>,
    levelingManager: LevelingManager
): ProgressionSummaryUi {
    val primaryId = when {
        !sessionState.partyMembers.isNullOrEmpty() -> sessionState.partyMembers.first()
        sessionState.playerId != null -> sessionState.playerId
        else -> charactersById.keys.firstOrNull()
    } ?: return ProgressionSummaryUi()
    val character = charactersById[primaryId]
    val level = sessionState.partyMemberLevels[primaryId]
        ?: sessionState.playerLevel.takeIf { primaryId == sessionState.playerId }
        ?: character?.level ?: 1
    val xp = sessionState.partyMemberXp[primaryId]
        ?: sessionState.playerXp.takeIf { primaryId == sessionState.playerId }
        ?: character?.xp ?: 0
    val bounds = levelingManager.levelBounds(level)
    val startXp = bounds.first
    val nextXp = bounds.second
    val progress = if (nextXp == null || nextXp <= startXp) {
        1f
    } else {
        val span = max(nextXp - startXp, 1)
        ((xp - startXp).coerceAtLeast(0).toFloat() / span).coerceIn(0f, 1f)
    }
    val xpToNextLabel = nextXp?.let { next ->
        val remaining = max(next - xp, 0)
        "$remaining XP to next level"
    }
    val maxHp = character?.hp?.takeIf { it > 0 } ?: sessionState.partyMemberHp[primaryId] ?: 0
    val currentHp = sessionState.partyMemberHp[primaryId] ?: maxHp
    val hpLabel = if (maxHp > 0) "$currentHp / $maxHp HP" else null
    return ProgressionSummaryUi(
        playerLevel = level,
        xpLabel = "$xp XP",
        xpProgress = progress,
        xpToNextLabel = xpToNextLabel,
        actionPointLabel = "${sessionState.playerAp} AP",
        creditsLabel = "${sessionState.playerCredits} credits",
        hpLabel = hpLabel
    )
}

internal fun buildLevelUpPrompt(
    summary: LevelUpSummary,
    charactersById: Map<String, Player>,
    skillsById: Map<String, Skill>
): LevelUpPrompt {
    val portraitPath = charactersById[summary.characterId]?.miniIconPath
    val unlocks = summary.unlockedSkills.map { unlock ->
        val skill = skillsById[unlock.id]
        SkillUnlockUi(
            id = unlock.id,
            name = skill?.name ?: unlock.name,
            description = skill?.description
        )
    }
    return LevelUpPrompt(
        characterName = summary.characterName,
        characterId = summary.characterId,
        newLevel = summary.newLevel,
        levelsGained = summary.levelsGained,
        unlockedSkills = unlocks,
        portraitPath = portraitPath,
        statChanges = summary.statChanges.map { StatChangeUi(label = it.label, value = it.value) }
    )
}

internal fun buildSkillTreeOverlayUi(
    tree: SkillTreeDefinition,
    character: Player?,
    sessionState: GameSessionState
): SkillTreeOverlayUi? {
    val nodes = tree.branches.values.flatten()
    if (nodes.isEmpty()) return null
    val nodesById = nodes.associateBy { it.id }
    val unlockedForTree = sessionState.unlockedSkills.filter { nodesById.containsKey(it) }.toSet()
    val availableAp = sessionState.playerAp
    val apInvested = skillTreeApInvested(tree, unlockedForTree)
    val completedMilestones = sessionState.completedMilestones

    val branches = tree.branches.entries.mapNotNull { (title, branchNodes) ->
        val nodeUis = branchNodes.mapNotNull { node ->
            if (isPlaceholderNode(node)) return@mapNotNull null
            val status = evaluateNodeStatus(
                node = node,
                tree = tree,
                unlockedSkills = unlockedForTree,
                completedMilestones = completedMilestones,
                availableAp = availableAp,
                apInvested = apInvested
            )
            SkillTreeNodeUi(
                id = node.id,
                name = node.name,
                costAp = node.costAp,
                row = nodeRow(node),
                column = nodeColumn(node),
                status = status,
                description = formatSkillNodeDescription(node.effect),
                requirements = node.requires.orEmpty().mapNotNull { requirementId ->
                    val label = nodesById[requirementId]?.name ?: requirementId.humanizeId()
                    SkillTreeRequirementUi(id = requirementId, label = label)
                }
            )
        }
        if (nodeUis.isEmpty()) return@mapNotNull null
        SkillTreeBranchUi(
            id = title.lowercase(Locale.getDefault()).ifBlank { title },
            title = title,
            nodes = nodeUis
        )
    }

    if (branches.isEmpty()) return null

    return SkillTreeOverlayUi(
        characterId = character?.id ?: tree.character,
        characterName = character?.name ?: tree.character.humanizeId(),
        portraitPath = character?.miniIconPath,
        availableAp = availableAp,
        apInvested = apInvested,
        branches = branches
    )
}

private fun formatSkillNodeDescription(effect: SkillNodeEffect?): String? {
    effect ?: return null
    val description = when (effect.type?.lowercase(Locale.getDefault())) {
        "buff" -> {
            val target = effect.buffType?.humanizeId() ?: "stats"
            val value = effect.value ?: 0
            "Boosts $target by $value."
        }
        "damage" -> {
            val multiplier = effect.mult?.let { String.format(Locale.getDefault(), "%.1f", it) }
            if (multiplier != null) "Deals damage (x$multiplier)." else "Deals heavy damage."
        }
        "utility" -> {
            val subtype = effect.subtype?.humanizeId() ?: "utility"
            "Grants the $subtype ability."
        }
        "heal" -> {
            val amount = effect.value ?: 0
            "Restores $amount HP."
        }
        else -> null
    }
    return description?.ifBlank { null }
}

private fun String.humanizeId(): String =
    split('_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
