package com.example.starborn.feature.enemy

import java.util.Locale

enum class EnemyPresentationTier {
    STANDARD,
    ELITE,
    BOSS
}

fun enemyPresentationTier(rawTier: String?): EnemyPresentationTier {
    val normalized = rawTier
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.replace("_", "")
        ?.replace("-", "")
        ?.replace(" ", "")
        .orEmpty()
    return when (normalized) {
        "boss" -> EnemyPresentationTier.BOSS
        "elite", "miniboss" -> EnemyPresentationTier.ELITE
        else -> EnemyPresentationTier.STANDARD
    }
}

fun combatEnemySpriteScale(rawTier: String?): Float = when (enemyPresentationTier(rawTier)) {
    EnemyPresentationTier.STANDARD -> 1.35f
    EnemyPresentationTier.ELITE -> 1.50f
    EnemyPresentationTier.BOSS -> 1.65f
}

fun explorationEnemySpriteScale(tier: EnemyPresentationTier): Float = when (tier) {
    EnemyPresentationTier.STANDARD -> 1.00f
    EnemyPresentationTier.ELITE -> 1.08f
    EnemyPresentationTier.BOSS -> 1.18f
}
