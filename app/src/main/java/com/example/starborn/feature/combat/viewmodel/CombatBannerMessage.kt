package com.example.starborn.feature.combat.viewmodel

enum class CombatBannerAccent {
    DEFAULT,
    MISS,
    HEAL,
    FIRE,
    ICE,
    SHOCK,
    POISON,
    RADIATION,
    PSYCHIC,
    VOID,
    PHYSICAL,
    NOVA,
    ZEKE,
    ORION,
    GHOST,
    ENEMY
}

enum class CombatBannerIcon {
    ATTACK,
    SKILL,
    ITEM,
    SNACK,
    GUARD,
    RETREAT,
    HEAL,
    STATUS,
    BURST,
    OUTCOME,
    MISS
}

enum class CombatBannerImportance {
    NORMAL,
    IMPORTANT
}

enum class CombatTextVerbosity {
    MINIMAL,
    STANDARD,
    VERBOSE
}

data class CombatBannerMessage(
    val id: String,
    val primary: String,
    val secondary: String? = null,
    val accent: CombatBannerAccent = CombatBannerAccent.DEFAULT,
    val icon: CombatBannerIcon? = null,
    val tags: List<String> = emptyList(),
    val importance: CombatBannerImportance = CombatBannerImportance.NORMAL
)

