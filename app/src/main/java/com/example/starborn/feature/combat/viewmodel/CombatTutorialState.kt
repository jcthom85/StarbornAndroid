package com.example.starborn.feature.combat.viewmodel

const val COMBAT_LOADER_TUTORIAL_ID = "combat_basics_loader"
const val COMBAT_BASICS_TUTORIAL_ID = "combat_basics_guard_break"
const val COMBAT_TUTORIAL_SKILL_ID = "nova_hydraulic_kick"
const val COMBAT_LOADER_SKILL_ID = "nova_arc_tether"

enum class CombatTutorialType {
    LOADER_WEAKNESS,
    BULWARK_GUARD_BREAK
}

enum class CombatTutorialStep {
    BRIEF,
    SELECT_NOVA_ATTACK,
    CHOOSE_ATTACK,
    TARGET_BASIC_ATTACK,
    AWAIT_BASIC_RESULT,
    BLOCKED_EXPLANATION,
    SELECT_NOVA_SKILL,
    CHOOSE_SKILLS,
    CHOOSE_HYDRAULIC_KICK,
    TARGET_HYDRAULIC_KICK,
    AWAIT_SHIELD_BREAK,
    SUCCESS
}

data class CombatTutorialState(
    val step: CombatTutorialStep,
    val targetId: String,
    val paused: Boolean = true,
    val tutorialType: CombatTutorialType = CombatTutorialType.LOADER_WEAKNESS
) {
    val showsModal: Boolean
        get() = step == CombatTutorialStep.BRIEF ||
            step == CombatTutorialStep.BLOCKED_EXPLANATION ||
            step == CombatTutorialStep.SUCCESS

    val canSkip: Boolean
        get() = false

    val expectedSkillId: String
        get() = if (tutorialType == CombatTutorialType.LOADER_WEAKNESS) {
            COMBAT_LOADER_SKILL_ID
        } else {
            COMBAT_TUTORIAL_SKILL_ID
        }
}
