package com.example.starborn.domain.leveling

import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameSessionState

/** Nova's crew progression rules. Version 1 is retained for existing prototype saves. */
object NovaBuildPrototype {
    const val VERSION = 1
    const val SCENARIO = "nova_build_prototype"
    const val GROUNDED = "nova_grounded_wire_v2"
    const val VENT = "nova_controlled_vent_v2"
    const val BREAKER = "nova_breaker_bolt_v2"
    const val SEAM = "nova_exposed_seam_v2"
    const val STEPS = "nova_quiet_steps_v2"
    const val SCATTER = "nova_scatter_charge_v2"
    const val START = "nova_hot_start_v2"
    const val MOVING = "nova_keep_moving_v2"
    const val BREAKER_SKILL = "nova_breaker_bolt"
    const val SCATTER_SKILL = "nova_scatter_charge"

    private fun node(id: String, name: String, cost: Int, previous: String?, description: String,
                     effect: SkillNodeEffect = SkillNodeEffect(type = "talent", subtype = id)) =
        SkillTreeNode(id, name, cost, listOf(0, cost - 1),
            listOfNotNull(previous) + if (cost == 4) listOf("ms_w4_mq20_complete") else emptyList(), effect, description)

    val tree = SkillTreeDefinition("nova", linkedMapOf(
        "Saboteur" to listOf(
            node(GROUNDED, "Grounded Wire", 1, null, "Arc Tether deals 20% more stability damage. HP damage and Shock are unchanged."),
            node(VENT, "Controlled Vent", 2, GROUNDED, "Cryo Vent power rises from 55 to 65. It applies Brittle for 2 turns, without freezing the target."),
            node(BREAKER, "Breaker Bolt", 3, VENT, "Physical shot at one enemy: 100 power, Agility scaling, cooldown 3. Deals 50% more stability damage; applies no status.", SkillNodeEffect(type = "active", subtype = BREAKER_SKILL)),
            node(SEAM, "Exposed Seam", 4, BREAKER, "Direct hits deal 15% more HP damage against Weak or Brittle targets. Having both does not double the bonus.")),
        "Improviser" to listOf(
            node(STEPS, "Quiet Steps", 1, null, "Permanent +1 Speed.", SkillNodeEffect(type = "buff", buffType = "speed", value = 1)),
            node(SCATTER, "Scatter Charge", 2, STEPS, "Physical damage to all enemies: 60 power, Agility scaling, cooldown 2. Applies no status.", SkillNodeEffect(type = "active", subtype = SCATTER_SKILL)),
            node(START, "Hot Start", 3, SCATTER, "Direct hits deal 15% more HP damage against targets above 75% HP."),
            node(MOVING, "Keep Moving", 4, START, "A direct-action kill grants Nova Shield for 1 personal turn. Once per action, at most twice per battle."))),
        useTierRequirements = false)

    val nodes = tree.branches.values.flatten().associateBy { it.id }
    val skills = listOf(
        Skill(BREAKER_SKILL, "Breaker Bolt", "nova", "active", 100, 3,
            "A focused physical shot dealing 50% more stability damage, without a status effect.",
            combatTags = listOf("physical"), scaling = "agility", statusApplications = emptyList(), targeting = "single_enemy"),
        Skill(SCATTER_SKILL, "Scatter Charge", "nova", "active", 60, 2,
            "Scatter a low-powered physical charge across the enemy formation.",
            combatTags = listOf("physical", "aoe"), scaling = "agility", statusApplications = emptyList(), targeting = "all_enemies"))

    fun activeSkills(purchases: Map<String, Int>): Set<String> = buildSet {
        if (BREAKER in purchases) add(BREAKER_SKILL)
        if (SCATTER in purchases) add(SCATTER_SKILL)
    }

    fun isEnabled(state: GameSessionState) = state.progressionPrototypeVersion == VERSION

    // Only new quest awards belong here. Existing boss AP remains in enemy rewards.
    val questAwards = linkedMapOf("w1_mq01" to 2, "w1_mq03" to 2, "w1_mq05" to 2,
        "w2_mq05" to 4, "w3_mq11" to 2, "w3_mq13" to 2, "w3_mq14" to 2, "w3_mq15" to 2,
        "w4_mq16" to 2, "w4_mq18" to 2, "w4_mq19" to 3, "w5_mq22" to 2, "w5_mq24" to 2,
        "w6_mq26" to 2, "w6_mq28" to 2, "w6_mq29" to 2,
        "w1_sq03" to 2, "w2_sq01" to 2, "w3_sq12" to 2, "w4_sq19" to 2, "w5_sq22" to 2, "w6_sq26" to 2)

    fun awardCompletedQuest(state: GameSessionState, questId: String): GameSessionState {
        val amount = questAwards[questId] ?: return state
        if (!isEnabled(state) || questId !in state.completedQuests || questId in state.progressionAwardClaims) return state
        return state.copy(playerAp = (state.playerAp.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            progressionAwardClaims = state.progressionAwardClaims + questId)
    }

    fun awardRecruitment(state: GameSessionState, member: String): GameSessionState {
        val claim = "recruit:$member"
        if (!isEnabled(state) || member !in setOf("orion", "gh0st") || claim in state.progressionAwardClaims) return state
        return state.copy(playerAp = (state.playerAp.toLong() + 2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), progressionAwardClaims = state.progressionAwardClaims + claim)
    }
}
