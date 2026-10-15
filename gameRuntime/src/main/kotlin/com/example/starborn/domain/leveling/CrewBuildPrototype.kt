package com.example.starborn.domain.leveling

import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameSessionState

/** All four reviewed builds. Version 1 preserves existing Nova prototype saves. */
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
    const val OPENING_STATUS = "crew_opening"
    const val AFTERCARE_STATUS = "crew_aftercare"

    val zeke = SkillTreeDefinition("zeke", linkedMapOf(
        "Protector" to listOf(
            SkillTreeNode(COVER, "Cover", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "active", subtype = COVER_SKILL), "New active: one living ally, including Zeke; cooldown 3 personal turns; applies Shield for 2 target turns (25% less incoming damage under the reviewed Shield rule); no heal, damage or interception."),
            SkillTreeNode(REINFORCED_HARNESS, "Reinforced Harness", 2, listOf(0, 1), listOf("zeke_cover_v2"), SkillNodeEffect(type = "buff", buffType = "vitality", value = 1), "Permanent +1 Vitality. Derived maximum HP and defense change through their shared formulas; no duplicate flat HP bonus."),
            SkillTreeNode(STEADY_COVER, "Steady Cover", 3, listOf(0, 2), listOf("zeke_reinforced_harness_v2"), SkillNodeEffect(type = "talent", subtype = STEADY_COVER), "Cover additionally restores 20 stability to its target, capped at that target current maximum. Does not heal HP, remove an existing skip effect or grant a turn."),
            SkillTreeNode(LAST_LINE, "Last Line", 4, listOf(0, 3), listOf("zeke_steady_cover_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = LAST_LINE), "Once per battle, Zeke may cast Cover while it is on cooldown if the selected living ally is below 30% HP at commitment. Spend a normal ATB action, apply ordinary Cover and Steady Cover if learned, and restart Cover's full 3-turn cooldown. Consume the emergency charge only for a valid cast that bypassed an active cooldown. No extra Guard, stronger Shield, heal, free action or stacked Shield copy.")
        ),
        "Disruptor" to listOf(
            SkillTreeNode(TRAINING_SESSION, "Training Session", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "buff", buffType = "strength", value = 1), "Permanent +1 Strength; derived Attack changes through the shared formula."),
            SkillTreeNode(FAULT_LINE, "Fault Line", 2, listOf(0, 1), listOf("zeke_training_session_v2"), SkillNodeEffect(type = "talent", subtype = FAULT_LINE), "Shatter Blow deals 20% more stability damage. Its HP damage, Weak duration and cooldown are unchanged."),
            SkillTreeNode(OPENING, "Opening", 3, listOf(0, 2), listOf("zeke_fault_line_v2"), SkillNodeEffect(type = "talent", subtype = OPENING), "When Shatter Blow successfully applies Weak, also mark that enemy for 2 target turns. The next direct damaging action from another crew member consumes the mark for +10% HP damage against that enemy. One mark per target; reapplication refreshes but does not stack; damage-over-time and Zeke own actions do not consume it."),
            SkillTreeNode(CONTROLLED_DEMOLITION, "Controlled Demolition", 4, listOf(0, 3), listOf("zeke_opening_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = CONTROLLED_DEMOLITION), "Zeke physical direct hits against an enemy with Weak deal 15% more stability damage. No HP damage bonus, immunity bypass or extra skipped turn.")
        )
    ), useTierRequirements = false)

    val orion = SkillTreeDefinition("orion", linkedMapOf(
        "Field Medic" to listOf(
            SkillTreeNode(FIELD_RESILIENCE, "Field Resilience", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "buff", buffType = "vitality", value = 1), "Permanent +1 Vitality; maximum HP and defense use the shared formulas."),
            SkillTreeNode(FIELD_TREATMENT, "Field Treatment", 2, listOf(0, 1), listOf("orion_field_resilience_v2"), SkillNodeEffect(type = "active", subtype = TREATMENT_SKILL), "New active: one living ally; cooldown 4 personal turns; restore 20% of target derived maximum HP, rounded down with minimum 1 and capped at missing HP. No Focus multiplier, resurrection or baseline recovery-over-time effect."),
            SkillTreeNode(TRIAGE, "Triage", 3, listOf(0, 2), listOf("orion_field_treatment_v2"), SkillNodeEffect(type = "talent", subtype = TRIAGE), "Field Treatment restores an additional 5% of target maximum HP when the target is below 35% HP before the cast, for 25% total. Calculate the combined amount once, round down once, and emit one direct-heal event per target."),
            SkillTreeNode(AFTERCARE, "Aftercare", 4, listOf(0, 3), listOf("orion_triage_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = AFTERCARE), "Field Treatment additionally applies Aftercare for 2 target turns: each turn-end restores floor(cast-time target maximum HP / 20), minimum 1, capped at missing HP. Aftercare and Regen share one recovery-over-time slot. Existing Regen has priority and retains its potency and remaining duration; this cast neither extends it nor queues Aftercare. Recasting Aftercare refreshes its own duration to 2 without stacking ticks; a later Regen replaces it. No resurrection, Focus multiplier or post-battle ticking.")
        ),
        "Controller" to listOf(
            SkillTreeNode(CLEAR_FOCUS, "Clear Focus", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "buff", buffType = "focus", value = 1), "Permanent +1 Focus, using the shared potency/accuracy/critical/resistance formulas exactly once."),
            SkillTreeNode(PRISM_LENS, "Prism Lens", 2, listOf(0, 1), listOf("orion_clear_focus_v2"), SkillNodeEffect(type = "talent", subtype = PRISM_LENS), "Prism Lance deals 20% more stability damage. Its HP damage, element and cooldown are unchanged."),
            SkillTreeNode(SAFE_EXPOSURE, "Safe Exposure", 3, listOf(0, 2), listOf("orion_prism_lens_v2"), SkillNodeEffect(type = "talent", subtype = SAFE_EXPOSURE), "Prism Lance applies Weak for 1 target turn only if the target did not have Weak before the action and is not immune. An existing Weak is neither extended nor replaced by this talent."),
            SkillTreeNode(STEADY_FIELD, "Steady Field", 4, listOf(0, 3), listOf("orion_safe_exposure_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = STEADY_FIELD), "Weak newly applied by Orion lasts 1 additional target turn, capped at 3. Does not change potency, extend existing effects retroactively, affect Frozen/Stun/Stagger or bypass immunity.")
        )
    ), useTierRequirements = false)

    val gh0st = SkillTreeDefinition("gh0st", linkedMapOf(
        "Precision" to listOf(
            SkillTreeNode(SCOPE, "Scope", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "talent", subtype = SCOPE), "Headshot deals 10% more HP damage against a target at or above 75% HP when the hit is evaluated."),
            SkillTreeNode(STEADY_AIM, "Steady Aim", 2, listOf(0, 1), listOf("gh0st_scope_v2"), SkillNodeEffect(type = "talent", subtype = STEADY_AIM), "Headshot gains 5 percentage points of critical chance, subject to the normal critical cap. No other action gains this bonus."),
            SkillTreeNode(EXECUTE, "Execute", 3, listOf(0, 2), listOf("gh0st_steady_aim_v2"), SkillNodeEffect(type = "talent", subtype = EXECUTE), "Gh0st single-target direct damaging skills deal 15% more HP damage to targets at or below 30% HP before the hit. No instant kill, boss exemption or damage-over-time bonus."),
            SkillTreeNode(COOL_HEAD, "Cool Head", 4, listOf(0, 3), listOf("gh0st_execute_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = COOL_HEAD), "Landing a basic attack gives Gh0st one steady-shot charge. His next valid Headshot consumes it for +15% direct HP damage, even if that Headshot misses. Charges do not stack and expire at battle end; invalid or canceled actions consume none. Headshot keeps base cooldown 2, with no talent cooldown refund, ATB grant or critical-hit recharge. The shared +25% talent-damage cap includes Scope and Execute.")
        ),
        "Tactical" to listOf(
            SkillTreeNode(QUICK_POSITION, "Quick Position", 1, listOf(0, 0), listOf(), SkillNodeEffect(type = "buff", buffType = "agility", value = 1), "Permanent +1 Agility; Speed/evasion use their shared formulas. No immediate ATB grant."),
            SkillTreeNode(SURGICAL_STRIKE, "Surgical Strike", 2, listOf(0, 1), listOf("gh0st_quick_position_v2"), SkillNodeEffect(type = "talent", subtype = SURGICAL_STRIKE), "Gh0st basic attack deals 20% more stability damage, with no HP damage or extra status bonus."),
            SkillTreeNode(DISARM, "Disarm", 3, listOf(0, 2), listOf("gh0st_surgical_strike_v2"), SkillNodeEffect(type = "active", subtype = DISARM_SKILL), "New active: one enemy; physical; base power 60; agility scaling; cooldown 3 personal turns; applies Weak for 2 target turns, respecting immunity. No Stun, silence or instant kill."),
            SkillTreeNode(RECENTER, "Recenter", 4, listOf(0, 3), listOf("gh0st_disarm_v2", "ms_w4_mq20_complete"), SkillNodeEffect(type = "talent", subtype = RECENTER), "Once per battle, prevent a scheduled-turn skip caused by Stun or Stagger on Gh0st, removing those two effects from him. Frozen and other unhandled skip effects remain; if any would still skip this turn, do not activate or consume Recenter. Other statuses remain; no bonus action, counterattack, damage or team-wide immunity.")
        )
    ), useTierRequirements = false)

    val trees = listOf(NovaBuildPrototype.tree, zeke, orion, gh0st)
    val nodes = trees.flatMap { it.branches.values.flatten() }.associateBy { it.id }
    val owners = trees.flatMap { tree -> tree.branches.values.flatten().map { it.id to tree.character } }.toMap()
    val skills = NovaBuildPrototype.skills + listOf(
        Skill(COVER_SKILL, "Cover", "zeke", "support", 0, 3, "Shield one living ally for 2 turns.", combatTags = listOf("support"), statusApplications = emptyList(), targeting = "single_ally"),
        Skill(TREATMENT_SKILL, "Field Treatment", "orion", "heal", 0, 4, "Restore 20% of one living ally maximum HP; no Focus multiplier.", combatTags = listOf("heal"), statusApplications = emptyList(), targeting = "single_ally"),
        Skill(DISARM_SKILL, "Disarm", "gh0st", "active", 60, 3, "Physical shot applying Weak for 2 turns.", combatTags = listOf("physical"), scaling = "agility", statusApplications = listOf("weak"), targeting = "single_enemy")
    )
    fun isEnabled(state: GameSessionState) = NovaBuildPrototype.isEnabled(state)
    fun activeSkills(purchases: Map<String, Int>): Set<String> = purchases.keys.mapNotNull { id ->
        nodes[id]?.effect?.takeIf { it.type == "active" }?.subtype
    }.toSet()
    fun ownedTalents(state: GameSessionState, owner: String): Set<String> =
        if (isEnabled(state)) state.purchasedSkillNodes.keys.filter { owners[it] == owner }.toSet() else emptySet()
    fun coreSkills(state: GameSessionState): Set<String> =
        if (isEnabled(state) && "orion" in state.partyMembers) setOf(PATCH_SKILL) else emptySet()
    fun grantCore(state: GameSessionState) = state.copy(unlockedSkills = state.unlockedSkills + coreSkills(state))
    fun tune(skill: Skill, state: GameSessionState): Skill {
        if (!isEnabled(state)) return skill
        return when (skill.id) {
            "nova_cryo_vent" -> if (NovaBuildPrototype.VENT in state.purchasedSkillNodes) skill.copy(basePower = 65) else skill
            PATCH_SKILL -> skill.copy(name = "Field Patch", type = "heal", basePower = 18, cooldown = 3, targeting = "single_ally", combatTags = listOf("heal"), statusApplications = emptyList(), description = "Treat one living ally: 18 base healing scaled by Focus, plus Shield for 1 turn. No resurrection.")
            "gh0st_headshot" -> skill.copy(description = "A precise physical shot. Power 95, Strength scaling, cooldown 2. Does not bypass armor.")
            "orion_prism_lance" -> skill.copy(description = "Focus harmonic light through a crystal. Acid damage, power 85, Focus scaling, cooldown 2.")
            else -> skill
        }
    }
}
