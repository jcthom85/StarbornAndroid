package com.example.starborn.domain.leveling

import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameSessionState

/** Production crew builds. Version 1 preserves existing Nova/crew prototype saves. */
object CrewBuildPrototype {
    const val VERSION = NovaBuildPrototype.VERSION
    const val SCENARIO = "crew_build_prototype"
    val checkpointScenarios = mapOf("crew_build_early" to "early", "crew_build_middle" to "middle", "crew_build_late" to "late")
    const val COVER = "zeke_cover_v2"
    const val REINFORCED_HARNESS = "zeke_reinforced_harness_v2"
    const val STEADY_COVER = "zeke_steady_cover_v2"
    const val LAST_LINE = "zeke_last_line_v2"
    const val TRAINING_SESSION = "zeke_training_session_v2"
    const val FAULT_LINE = "zeke_fault_line_v2"
    const val OPENING = "zeke_opening_v2"
    const val CONTROLLED_DEMOLITION = "zeke_controlled_demolition_v2"
    const val FIELD_RESILIENCE = "orion_field_resilience_v2"
    const val FIELD_TREATMENT = "orion_field_treatment_v2"
    const val TRIAGE = "orion_triage_v2"
    const val AFTERCARE = "orion_aftercare_v2"
    const val CLEAR_FOCUS = "orion_clear_focus_v2"
    const val PRISM_LENS = "orion_prism_lens_v2"
    const val SAFE_EXPOSURE = "orion_safe_exposure_v2"
    const val STEADY_FIELD = "orion_steady_field_v2"
    const val SCOPE = "gh0st_scope_v2"
    const val STEADY_AIM = "gh0st_steady_aim_v2"
    const val EXECUTE = "gh0st_execute_v2"
    const val COOL_HEAD = "gh0st_cool_head_v2"
    const val QUICK_POSITION = "gh0st_quick_position_v2"
    const val SURGICAL_STRIKE = "gh0st_surgical_strike_v2"
    const val DISARM = "gh0st_disarm_v2"
    const val RECENTER = "gh0st_recenter_v2"
    const val COVER_SKILL = "zeke_cover"
    const val TREATMENT_SKILL = "orion_field_treatment"
    const val DISARM_SKILL = "gh0st_disarm"
    const val PATCH_SKILL = "orion_stasis_stitch"
    const val CORPORATE_INSIGHT = "corporate_insight"
    const val DATA_SHIELD = "data_shield"
    const val NOVA_LEGACY = "nova_legacy"
    const val ZEKE_UNSHACKLED = "zeke_unshackled"
    const val GH0ST_SOURCE_BALANCE = "gh0st_source_balance"
    const val ORION_ANCESTRAL_GRACE = "orion_ancestral_grace"
    val PASSIVE_SKILLS = setOf(
        CORPORATE_INSIGHT,
        DATA_SHIELD,
        NOVA_LEGACY,
        ZEKE_UNSHACKLED,
        GH0ST_SOURCE_BALANCE,
        ORION_ANCESTRAL_GRACE
    )
    const val OPENING_STATUS = "crew_opening"
    const val AFTERCARE_STATUS = "crew_aftercare"

    val zeke = SkillTreeDefinition("zeke", linkedMapOf(
        "Protector" to listOf(
            SkillTreeNode(COVER, "Cover", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "active", subtype = COVER_SKILL), "Unlock Cover: Grant Shield to one ally (or Zeke) for 2 turns, reducing incoming damage by 25%. (3-turn cooldown)"),
            SkillTreeNode(REINFORCED_HARNESS, "Reinforced Harness", 2, listOf(0, 1), listOf("zeke_cover_v2"), SkillNodeEffect(type = "buff", buffType = "vitality", value = 1), "Permanent +1 Vitality, raising Zeke's Max HP and resilience."),
            SkillTreeNode(STEADY_COVER, "Steady Cover", 3, listOf(0, 2), listOf("zeke_reinforced_harness_v2"), SkillNodeEffect(type = "talent", subtype = STEADY_COVER), "Cover also restores +20 Stability to its target."),
            SkillTreeNode(LAST_LINE, "Last Line", 4, listOf(0, 3), listOf("zeke_steady_cover_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = LAST_LINE), "Once per battle, Zeke can cast Cover even while on cooldown to protect an ally below 30% HP.")
        ),
        "Disruptor" to listOf(
            SkillTreeNode(TRAINING_SESSION, "Training Session", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "buff", buffType = "strength", value = 1), "Permanent +1 Strength, boosting Zeke's weapon strikes and physical skills."),
            SkillTreeNode(FAULT_LINE, "Fault Line", 2, listOf(0, 1), listOf("zeke_training_session_v2"), SkillNodeEffect(type = "talent", subtype = FAULT_LINE), "Shatter Blow deals +20% Stability damage."),
            SkillTreeNode(OPENING, "Opening", 3, listOf(0, 2), listOf("zeke_fault_line_v2"), SkillNodeEffect(type = "talent", subtype = OPENING), "When Shatter Blow inflicts Discord, mark the target for 2 turns; the next direct hit from an ally consumes the mark for +10% HP damage."),
            SkillTreeNode(CONTROLLED_DEMOLITION, "Controlled Demolition", 4, listOf(0, 3), listOf("zeke_opening_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = CONTROLLED_DEMOLITION), "Zeke's physical attacks deal +15% Stability damage against enemies afflicted with Discord.")
        )
    ), useTierRequirements = false)

    val orion = SkillTreeDefinition("orion", linkedMapOf(
        "Field Medic" to listOf(
            SkillTreeNode(FIELD_RESILIENCE, "Field Resilience", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "buff", buffType = "vitality", value = 1), "Permanent +1 Vitality, raising Orion's Max HP and durability."),
            SkillTreeNode(FIELD_TREATMENT, "Field Treatment", 2, listOf(0, 1), listOf("orion_field_resilience_v2"), SkillNodeEffect(type = "active", subtype = TREATMENT_SKILL), "Unlock Field Treatment: Restore 20% of one ally's Max HP directly. (4-turn cooldown)"),
            SkillTreeNode(TRIAGE, "Triage", 3, listOf(0, 2), listOf("orion_field_treatment_v2"), SkillNodeEffect(type = "talent", subtype = TRIAGE), "Field Treatment restores 25% Max HP (up from 20%) when healing an ally below 35% HP."),
            SkillTreeNode(AFTERCARE, "Aftercare", 4, listOf(0, 3), listOf("orion_triage_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = AFTERCARE), "Field Treatment also applies Aftercare for 2 turns, restoring 5% Max HP at the end of each turn (unless Regen is already active).")
        ),
        "Controller" to listOf(
            SkillTreeNode(CLEAR_FOCUS, "Clear Focus", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "buff", buffType = "focus", value = 1), "Permanent +1 Focus, boosting Orion's Source power, healing, and status accuracy."),
            SkillTreeNode(PRISM_LENS, "Prism Lens", 2, listOf(0, 1), listOf("orion_clear_focus_v2"), SkillNodeEffect(type = "talent", subtype = PRISM_LENS), "Prism Lance deals +20% Stability damage."),
            SkillTreeNode(SAFE_EXPOSURE, "Safe Exposure", 3, listOf(0, 2), listOf("orion_prism_lens_v2"), SkillNodeEffect(type = "talent", subtype = SAFE_EXPOSURE), "Prism Lance inflicts Discord (1 turn) on enemies that aren't already afflicted."),
            SkillTreeNode(STEADY_FIELD, "Steady Field", 4, listOf(0, 3), listOf("orion_safe_exposure_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = STEADY_FIELD), "Discord newly applied by Orion lasts +1 extra turn (up to 3 turns).")
        )
    ), useTierRequirements = false)

    val gh0st = SkillTreeDefinition("gh0st", linkedMapOf(
        "Precision" to listOf(
            SkillTreeNode(SCOPE, "Scope", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "talent", subtype = SCOPE), "Headshot deals +10% HP damage against enemies at or above 75% HP."),
            SkillTreeNode(STEADY_AIM, "Steady Aim", 2, listOf(0, 1), listOf("gh0st_scope_v2"), SkillNodeEffect(type = "talent", subtype = STEADY_AIM), "Headshot gains +5% Critical Chance."),
            SkillTreeNode(EXECUTE, "Execute", 3, listOf(0, 2), listOf("gh0st_steady_aim_v2"), SkillNodeEffect(type = "talent", subtype = EXECUTE), "Gh0st's single-target skills deal +15% HP damage against enemies at or below 30% HP."),
            SkillTreeNode(COOL_HEAD, "Cool Head", 4, listOf(0, 3), listOf("gh0st_execute_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = COOL_HEAD), "Landing a basic attack primes a Steady Shot, boosting Gh0st's next Headshot by +15% HP damage.")
        ),
        "Tactical" to listOf(
            SkillTreeNode(QUICK_POSITION, "Quick Position", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "buff", buffType = "agility", value = 1), "Permanent +1 Agility, improving turn speed and evasion."),
            SkillTreeNode(SURGICAL_STRIKE, "Surgical Strike", 2, listOf(0, 1), listOf("gh0st_quick_position_v2"), SkillNodeEffect(type = "talent", subtype = SURGICAL_STRIKE), "Gh0st's basic attacks deal +20% Stability damage."),
            SkillTreeNode(DISARM, "Disarm", 3, listOf(0, 2), listOf("gh0st_surgical_strike_v2"), SkillNodeEffect(type = "active", subtype = DISARM_SKILL), "Unlock Disarm: Agility-scaled physical shot (60 power) that inflicts Discord for 2 turns. (3-turn cooldown)"),
            SkillTreeNode(RECENTER, "Recenter", 4, listOf(0, 3), listOf("gh0st_disarm_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = RECENTER), "Once per battle, automatically shake off Stun or Stagger at turn start instead of losing the turn.")
        )
    ), useTierRequirements = false)

    val trees = listOf(NovaBuildPrototype.tree, zeke, orion, gh0st)
    val nodes = trees.flatMap { it.branches.values.flatten() }.associateBy { it.id }
    val owners = trees.flatMap { tree -> tree.branches.values.flatten().map { it.id to tree.character } }.toMap()
    val skills = NovaBuildPrototype.skills + listOf(
        Skill(COVER_SKILL, "Cover", "zeke", "support", 0, 3, "Grant Shield to one ally for 2 turns, reducing incoming damage by 25%.", combatTags = listOf("support"), statusApplications = emptyList(), targeting = "single_ally"),
        Skill(TREATMENT_SKILL, "Field Treatment", "orion", "heal", 0, 4, "Restore 20% of one ally's Max HP directly.", combatTags = listOf("heal"), statusApplications = emptyList(), targeting = "single_ally"),
        Skill(DISARM_SKILL, "Disarm", "gh0st", "active", 60, 3, "Agility-scaled physical shot that inflicts Discord for 2 turns.", combatTags = listOf("physical"), scaling = "agility", statusApplications = listOf("weak"), targeting = "single_enemy")
    )
    fun isEnabled(state: GameSessionState) = NovaBuildPrototype.isEnabled(state)
    fun activeSkills(purchases: Map<String, Int>): Set<String> = purchases.keys.mapNotNull { id ->
        nodes[id]?.effect?.takeIf { it.type == "active" }?.subtype
    }.toSet()
    fun ownedTalents(state: GameSessionState, owner: String): Set<String> =
        (if (isEnabled(state)) state.purchasedSkillNodes.keys.filter { owners[it] == owner }.toSet() else emptySet()) +
            buildSet {
                if (owner == "nova" && CORPORATE_INSIGHT in state.unlockedSkills) add(CORPORATE_INSIGHT)
                if (owner == "nova" && NOVA_LEGACY in state.unlockedSkills) add(NOVA_LEGACY)
                if (owner == "zeke" && DATA_SHIELD in state.unlockedSkills) add(DATA_SHIELD)
                if (owner == "zeke" && ZEKE_UNSHACKLED in state.unlockedSkills) add(ZEKE_UNSHACKLED)
                if (owner == "gh0st" && GH0ST_SOURCE_BALANCE in state.unlockedSkills) add(GH0ST_SOURCE_BALANCE)
                if (owner == "orion" && ORION_ANCESTRAL_GRACE in state.unlockedSkills) add(ORION_ANCESTRAL_GRACE)
            }
    fun coreSkills(state: GameSessionState): Set<String> =
        if (isEnabled(state) && "orion" in state.partyMembers) setOf(PATCH_SKILL) else emptySet()
    fun grantCore(state: GameSessionState) = state.copy(unlockedSkills = state.unlockedSkills + coreSkills(state))
    fun tune(skill: Skill, state: GameSessionState): Skill {
        if (!isEnabled(state)) return skill
        return when (skill.id) {
            "nova_cryo_vent" -> if (NovaBuildPrototype.VENT in state.purchasedSkillNodes) skill.copy(basePower = 65) else skill
            PATCH_SKILL -> skill.copy(name = "Field Patch", type = "heal", basePower = 18, cooldown = 3, targeting = "single_ally", combatTags = listOf("heal"), statusApplications = emptyList(), description = "Mend one ally with Focus-scaled healing and grant Shield for 1 turn.")
            "gh0st_headshot" -> skill.copy(description = "Fire a precision high-caliber shot at one enemy. Scales with Strength.")
            "orion_prism_lance" -> skill.copy(description = "Focus harmonic light through Orion's prism for Acid damage. Scales with Focus.")
            else -> skill
        }
    }
}
