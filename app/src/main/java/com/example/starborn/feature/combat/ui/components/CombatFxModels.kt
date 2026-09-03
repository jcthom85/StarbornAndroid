package com.example.starborn.feature.combat.ui.components

data class DamageFxUi(
    val id: String,
    val targetId: String,
    val amount: Int,
    val element: String?,
    val critical: Boolean,
    val isWeakness: Boolean = false,
    val isBrokenBonus: Boolean = false,
    val isGuardBreak: Boolean = false
)

data class HealFxUi(
    val id: String,
    val targetId: String,
    val amount: Int
)

data class StatusFxUi(
    val id: String,
    val targetId: String,
    val statusId: String,
    val stacks: Int
)

data class KnockoutFxUi(
    val id: String,
    val targetId: String
)

data class SupportFxUi(
    val id: String,
    val actorId: String,
    val skillName: String,
    val targetIds: List<String>
)

data class TelegraphFxUi(
    val id: String,
    val actorId: String,
    val skillName: String,
    val targetIds: List<String>
)

data class AttackHitFxUi(
    val id: String,
    val targetId: String,
    val style: AttackFxStyle,
    val critical: Boolean
)

data class ShieldBreakFxUi(
    val id: String,
    val targetId: String
)

enum class AttackFxStyle {
    NOVA_LASER,
    ZEKE_PUNCH,
    ORION_JEWEL,
    GHOST_SLASH
}

enum class TargetFilter {
    ENEMY,
    ALLY,
    ANY
}
