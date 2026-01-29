package com.example.starborn.domain.combat

/**
 * Central configuration for Enemy AI scoring logic.
 *
 * This data class replaces hardcoded "magic numbers" in the AI decision tree,
 * allowing for easier balancing, difficulty scaling, and dynamic adjustments.
 */
data class CombatAiWeights(
    // --- Damage & Kill Secure ---
    val weaknessHighBonus: Double = 18.0,      // Bonus when affinity >= 1.5x
    val weaknessModerateBonus: Double = 10.0,  // Bonus when affinity > 1.0x
    val resistancePenalty: Double = -12.0,     // Penalty when affinity <= 0.5x
    val executeHighBonus: Double = 18.0,       // Target HP <= 25%
    val executeModerateBonus: Double = 10.0,   // Target HP <= 45%

    // --- Status Effects (Control & Debuffs) ---
    val statusStun: Double = 38.0,             // Stun, Stagger
    val statusBlind: Double = 28.0,
    val statusJammed: Double = 26.0,
    val statusDoT: Double = 26.0,              // Meltdown, Erosion, Bleed
    val statusDebuff: Double = 22.0,           // Weak, Brittle, Exposed, Short
    val statusBuffDefense: Double = 24.0,      // Shield, Guard, Regen
    val statusInvulnerable: Double = 28.0,
    val statusDefault: Double = 18.0,
    val statusAlreadyAppliedMultiplier: Double = 0.25, // Score multiplier if target already has status

    // --- Healing ---
    val healBaseScore: Double = 45.0,          // Fallback score if skill has no base power
    val healMissingHpMultiplier: Double = 22.0, // Bonus scaled by % missing HP

    // --- Guard Breaking ---
    val guardBreakBlocking: Double = 42.0,     // Target is actively blocking/shielded
    val guardBreakHighStability: Double = 18.0, // Stability > 70%
    val guardBreakMedStability: Double = 10.0,  // Stability > 50%

    // --- Summoning ---
    val summonBase: Double = 30.0,
    val summonEarlyRoundBonus: Double = 18.0,  // Rounds 1-2
    val summonLateRoundBonus: Double = 6.0,    // Rounds 3+
    val summonLowAllyBonus: Double = 20.0,     // Allies < 3

    // --- Defensive Actions ---
    val defendLowHpBonus: Double = 22.0,       // HP < 25%
    val defendMedHpBonus: Double = 18.0,       // HP < 40%
    val defendHighHpBonus: Double = 14.0,      // HP < 60%
    val defendExistingGuardPenalty: Double = -8.0,
    val anticipationDefendBonus: Double = 40.0, // Bonus for Elites/Bosses when player is charging

    // --- General / Penalties ---
    val cooldownPenaltyPerTurn: Double = 3.0,
    val diversityRepeatPenalty: Double = 7.0,      // Penalty per previous use in history
    val diversityConsecutivePenalty: Double = 10.0,// Penalty if used immediately prior
    val basicAttackBaseConstant: Double = 35.0,
    val basicAttackRepeatPenalty: Double = 6.0
)
