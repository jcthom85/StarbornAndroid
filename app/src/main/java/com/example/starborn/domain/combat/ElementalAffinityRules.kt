package com.example.starborn.domain.combat

import com.example.starborn.domain.model.Resistances

enum class AffinityTier(val code: Int, val multiplier: Double) {
    WEAKNESS(-100, 2.0),
    VANTAGE(-50, 1.5),
    NEUTRAL(0, 1.0),
    RESIST(50, 0.5),
    IMMUNE(100, 0.0)
}

object ElementalAffinityRules {
    private val physicalWeakTags = setOf("soft", "unarmored")
    private val physicalResistTags = setOf("armored", "plated")

    private val shockWeakTags = setOf("robotic", "electronic")
    private val shockResistTags = setOf("insulated")

    private val burnWeakTags = setOf("biological")
    private val burnResistTags = setOf("heat_shielded")

    private val freezeWeakTags = setOf("overheated", "fragile")
    private val freezeResistTags = setOf("high_internal_heat")

    private val acidWeakTags = setOf("industrial", "plated")
    private val acidResistTags = setOf("acid_proof", "corrosion_resistant")

    private val sourceWeakTags = setOf("hardened_tech", "source_beast")
    private val sourceResistTags = setOf("source_shielded")

    fun tierForValue(value: Int): AffinityTier = when (value) {
        AffinityTier.WEAKNESS.code -> AffinityTier.WEAKNESS
        AffinityTier.VANTAGE.code -> AffinityTier.VANTAGE
        AffinityTier.NEUTRAL.code -> AffinityTier.NEUTRAL
        AffinityTier.RESIST.code -> AffinityTier.RESIST
        AffinityTier.IMMUNE.code -> AffinityTier.IMMUNE
        else -> when {
            value <= -75 -> AffinityTier.WEAKNESS
            value <= -25 -> AffinityTier.VANTAGE
            value >= 75 -> AffinityTier.IMMUNE
            value >= 25 -> AffinityTier.RESIST
            else -> AffinityTier.NEUTRAL
        }
    }

    fun fromTags(tags: Collection<String>): ResistanceProfile {
        val normalized = tags.mapNotNull { tag ->
            tag.trim().lowercase().takeIf { it.isNotBlank() }
        }.toSet()
        return ResistanceProfile(
            physical = resolveTier(normalized, physicalWeakTags, physicalResistTags),
            shock = resolveTier(normalized, shockWeakTags, shockResistTags),
            burn = resolveTier(normalized, burnWeakTags, burnResistTags),
            freeze = resolveTier(normalized, freezeWeakTags, freezeResistTags),
            acid = resolveTier(normalized, acidWeakTags, acidResistTags),
            source = resolveTier(normalized, sourceWeakTags, sourceResistTags)
        )
    }

    fun applyOverrides(base: ResistanceProfile, overrides: Resistances?): ResistanceProfile {
        if (overrides == null) return base
        return base.copy(
            burn = overrides.burn ?: base.burn,
            freeze = overrides.freeze ?: base.freeze,
            shock = overrides.shock ?: base.shock,
            acid = overrides.acid ?: base.acid,
            source = overrides.source ?: base.source,
            physical = overrides.physical ?: base.physical
        )
    }

    private fun resolveTier(
        tags: Set<String>,
        weakTags: Set<String>,
        resistTags: Set<String>,
        immuneTags: Set<String> = emptySet()
    ): Int = when {
        tags.any(immuneTags::contains) -> AffinityTier.IMMUNE.code
        tags.any(weakTags::contains) -> AffinityTier.WEAKNESS.code
        tags.any(resistTags::contains) -> AffinityTier.RESIST.code
        else -> AffinityTier.NEUTRAL.code
    }
}
