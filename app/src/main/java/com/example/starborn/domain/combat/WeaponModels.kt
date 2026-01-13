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
    data class SingleTarget(
        val powerMultiplier: Double = 1.0,
        val element: String? = null
    ) : WeaponAttack

    data class AllEnemies(
        val powerMultiplier: Double = 0.7,
        val element: String? = null
    ) : WeaponAttack

    data class ChargedSplash(
        val chargeTurns: Int = 1,
        val powerMultiplier: Double = 1.8,
        val splashMultiplier: Double = 0.5,
        val element: String? = null
    ) : WeaponAttack
}

data class WeaponChargeState(
    val weaponItemId: String,
    val remainingTurns: Int
)
