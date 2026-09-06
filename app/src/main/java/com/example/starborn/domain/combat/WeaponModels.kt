package com.example.starborn.domain.combat

data class CombatWeapon(
    val itemId: String,
    val name: String,
    val weaponType: String,
    val attack: WeaponAttack,
    val minDamage: Int = 0,
    val maxDamage: Int = 0,
    val statusOnHit: String? = null,
    val statusChance: Double = 0.0
)

sealed interface WeaponAttack {
    val powerMultiplier: Double
    val element: String?

    data class SingleTarget(
        override val powerMultiplier: Double = 1.0,
        override val element: String? = null
    ) : WeaponAttack

    data class AllEnemies(
        override val powerMultiplier: Double = 0.7,
        override val element: String? = null
    ) : WeaponAttack

    data class ChargedSplash(
        val chargeTurns: Int = 1,
        override val powerMultiplier: Double = 1.8,
        val splashMultiplier: Double = 0.5,
        override val element: String? = null
    ) : WeaponAttack
}

data class WeaponChargeState(
    val weaponItemId: String,
    val remainingTurns: Int
)
