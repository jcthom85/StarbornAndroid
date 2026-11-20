package com.example.starborn.domain.combat

import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.ItemEffect
import com.example.starborn.domain.model.Skill
import java.util.Locale

/**
 * 3-layer data-driven combat action audio resolver covering all player skills,
 * enemy abilities, basic attacks, Defend, and combat items/snacks.
 *
 * Layer 1: Explicit signature skill overrides for hero abilities & iconic Source Arts.
 * Layer 2: Role + element + token classifier inspecting Skill metadata (`type`, `basePower`,
 *          `combatTags`, `statusApplications`, and skill ID tokens).
 * Layer 3: Fallback to character weapon SFX (players) or enemy body-type SFX (enemies).
 */
object CombatAbilityAudioResolver {

    val SIGNATURE_SKILL_CUES: Map<String, String> = mapOf(
        // Nova
        "nova_arc_tether" to "skill_shock_arc",
        "nova_cryo_vent" to "skill_cryo_freeze",
        "nova_plasma_burst" to "skill_plasma_blast",
        "nova_blast_wave" to "skill_shockwave_slam",
        "nova_pulse_grenade" to "skill_plasma_blast",
        "nova_rend_round" to "skill_precision_shot",
        "nova_smoke_bomb" to "skill_smoke_cloak",
        "nova_hydraulic_kick" to "wpn_zeke_body_impact",
        "slide_kick" to "wpn_zeke_body_impact",
        "nova_cheap_shot" to "wpn_zeke_body_impact",
        "scrap_shot" to "wpn_nova_resonance_shot",
        "nova_link" to "skill_heal",
        "source_art_scan" to "skill_target_scan",
        "source_art_stasis" to "skill_cryo_freeze",
        "source_art_construct" to "skill_source_harmonic",
        "source_art_tune_world" to "skill_source_harmonic",
        // Zeke
        "zeke_overload_fists" to "skill_shock_arc",
        "zeke_quake_slam" to "skill_shockwave_slam",
        "zeke_shatter_blow" to "wpn_zeke_body_impact",
        "zeke_bulwark_stance" to "skill_shield_up",
        "zeke_synergy_pitch" to "skill_shield_up",
        "zeke_guardian_covenant" to "skill_shield_up",
        "zeke_signal_jammer" to "skill_psionic_silence",
        // Orion
        "orion_prism_lance" to "skill_acid_burn",
        "orion_stasis_field" to "skill_cryo_freeze",
        "orion_sunbeam" to "skill_plasma_blast",
        "orion_nano_repair" to "skill_heal",
        "orion_stasis_stitch" to "skill_heal",
        "orion_ancestral_grace" to "skill_heal",
        "orion_disruption_pulse" to "wpn_orion_bowl_resonance",
        // GH0ST
        "gh0st_headshot" to "skill_precision_shot",
        "gh0st_venom_edge" to "skill_acid_burn",
        "gh0st_system_crash" to "skill_source_harmonic",
        "gh0st_shadow_flurry" to "wpn_gh0st_wire_slash",
        "gh0st_phase_counter" to "wpn_gh0st_wire_slash",
        "gh0st_target_lock" to "skill_target_scan",
        "gh0st_intercept" to "skill_shield_up",
        "gh0st_harden" to "skill_shield_up",
        // Ollie
        "slingshot" to "wpn_zeke_body_impact"
    )

    fun resolveSkillCue(
        sourceId: String,
        skillId: String?,
        skill: Skill?,
        enemy: Enemy?
    ): String {
        val normalizedSkillId = skillId?.trim()?.lowercase(Locale.ROOT)
        if (!normalizedSkillId.isNullOrEmpty()) {
            // Layer 1: Explicit signature skill override
            SIGNATURE_SKILL_CUES[normalizedSkillId]?.let { return it }

            // Layer 2: Data-driven role + element + token classifier
            classifySkillCue(normalizedSkillId, skill)?.let { return it }
        }

        // Layer 3: Character weapon or enemy body-type fallback
        return resolveBasicAttackCue(sourceId, enemy)
    }

    fun resolveBasicAttackCue(sourceId: String, enemy: Enemy?): String {
        val base = sourceId.substringBefore('#')
        val segment = base.substringAfterLast(':').substringAfterLast('/')
        val normalized = segment.lowercase(Locale.ROOT).removePrefix("player_")
        return when {
            normalized.startsWith("nova") -> "wpn_nova_laser"
            normalized.startsWith("zeke") -> "wpn_zeke_punch"
            normalized.startsWith("orion") -> "wpn_orion_resonance"
            normalized.startsWith("gh0st") || normalized.startsWith("ghost") -> "wpn_gh0st_slash"
            normalized.startsWith("ollie") -> "wpn_zeke_body_impact"
            else -> resolveEnemyBodyCue(normalized, enemy)
        }
    }

    fun resolveItemCue(itemId: String, effect: ItemEffect?): String {
        val normalizedId = itemId.trim().lowercase(Locale.ROOT)
        val tokens = normalizedId.split('_').toSet()
        val effectType = effect?.type?.trim()?.lowercase(Locale.ROOT)
        val status = effect?.status?.trim()?.lowercase(Locale.ROOT)
        val isDamage = (effect?.damage ?: 0) > 0 || effectType == "damage"
        if (isDamage) {
            return when {
                tokens.any { it in setOf("cryo", "frost", "freeze", "ice", "stasis") } -> "skill_cryo_freeze"
                tokens.any { it in setOf("emp", "shock", "arc", "stun", "volt") } -> "skill_shock_arc"
                tokens.any { it in setOf("acid", "corrosive", "gas", "venom", "toxic") } -> "skill_acid_burn"
                tokens.any { it in setOf("smoke", "cloak") } -> "skill_smoke_cloak"
                else -> "skill_plasma_blast"
            }
        }
        val isHeal = (effect?.restoreHp ?: 0) > 0 ||
            effectType in setOf("heal", "revive", "restore") ||
            status == "regen"
        if (isHeal) {
            return "skill_heal"
        }
        if (status in setOf("shield", "guard", "braced", "invulnerable") ||
            tokens.any { it in setOf("shield", "barrier", "guard") }
        ) {
            return "skill_shield_up"
        }
        if (tokens.any { it in setOf("smoke", "cloak") }) {
            return "skill_smoke_cloak"
        }
        return "skill_power_up"
    }

    fun resolveDefendCue(): String = "skill_shield_up"

    fun shouldVaryPitch(battleKey: String): Boolean {
        if (battleKey in setOf("skill_heal", "skill_source_harmonic", "skill_target_scan")) {
            return false
        }
        return battleKey.startsWith("wpn_") ||
            battleKey.startsWith("enemy_attack_") ||
            battleKey.startsWith("skill_") ||
            battleKey == "shield_block" ||
            battleKey == "shield_break"
    }

    internal fun classifySkillCue(sid: String, skill: Skill?): String? {
        val tokens = sid.split('_').toSet()
        val stype = skill?.type?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val power = skill?.basePower ?: 0
        val tags = skill?.combatTags.orEmpty().map { it.trim().lowercase(Locale.ROOT) }.toSet()
        val statuses = skill?.statusApplications.orEmpty().map { it.trim().lowercase(Locale.ROOT) }.toSet()

        fun hasToken(vararg candidates: String): Boolean = candidates.any { it in tokens }

        // 1. Heal / repair / mend / weld
        if (stype == "heal" || "heal" in tags || "regen" in statuses ||
            hasToken("repair", "mend", "weld", "heal")
        ) {
            return "skill_heal"
        }

        // 2. Smoke / stealth / evasion / burrow / mist
        if ("stealth" in tags || "evasion" in tags ||
            hasToken("smoke", "cloak", "evasive", "burrow", "mist")
        ) {
            return "skill_smoke_cloak"
        }

        // 3. Biological roar / howl
        if (hasToken("roar", "howl")) {
            return "enemy_screech"
        }

        // 4. Non-damaging tactical / stance / overclock / frequency / recovery shifts
        if (power == 0 && hasToken(
                "overheat", "overclock", "frequency", "alignment", "cooling",
                "vent", "vents", "expose", "exposed", "exposure", "recover",
                "recovery", "deploy", "recalibration", "split", "reset", "cooldown"
            )
        ) {
            return "skill_power_up"
        }

        // 5. Defensive shields / guards / barriers / shelters / carapace / wards
        val hasDefensiveStatus = statuses.any { st ->
            st.contains("guard") || st.contains("shield") || st.contains("invulnerable") ||
                st.contains("shelter") || st.contains("carapace") || st.contains("ward") ||
                st.contains("barrier")
        }
        if ((power == 0 || "defend" in tags || "defense" in tags || "defensive" in tags) &&
            (hasDefensiveStatus || hasToken("shield", "barrier", "guard", "shelter", "carapace", "ward", "cover", "lock"))
        ) {
            return "skill_shield_up"
        }

        // 6. Optical scans / target lock / marks / official stamps & orders
        val hasMarkStatus = "target_lock" in statuses || "marked" in statuses || statuses.any { it.contains("mark") }
        if ((power <= 40 && hasMarkStatus) ||
            hasToken("scan", "designate", "mark", "stamp") ||
            sid in setOf("source_review_order", "source_war_order", "gh0st_target_lock")
        ) {
            return "skill_target_scan"
        }

        // 7. Drone deployment / attacks
        if (hasToken("drone")) {
            return "enemy_attack_drone"
        }

        // 8. Psionic / silence / compliance / guilt / glitch / sonic disruption
        if ("psionic" in tags || "silence" in statuses || "confuse" in statuses ||
            hasToken("silence", "guilt", "compliance", "happiness", "chorus", "glitch", "shriek", "accuse", "murmur", "verdict")
        ) {
            return "skill_psionic_silence"
        }

        // 9. Cryo / freeze / stasis / quench
        if ("freeze" in tags || "freeze" in statuses || "brittle" in statuses ||
            hasToken("cryo", "stasis", "quench", "frost")
        ) {
            return "skill_cryo_freeze"
        }

        // 10. Shock / electric / arc / EMP / ion / lightning / tether
        if ("shock" in tags || "shock" in statuses || "short" in statuses ||
            hasToken("shock", "arc", "spark", "static", "emp", "ion", "lightning", "tether") ||
            sid in setOf("stun_pulse", "spire_quarantine_pulse", "spire_interception")
        ) {
            return "skill_shock_arc"
        }

        // 11. Seismic slams / stomps / crushes / heavy hammers (before fire so molten_slam hits heavy)
        if ("earthquake" in tags || "crush" in tags || "force" in tags ||
            hasToken("stomp", "slam", "hammer", "quake", "crush", "pillars")
        ) {
            return "skill_shockwave_slam"
        }

        // 12. Continuous flamethrower sweeps & cones
        if (hasToken("flame") || sid == "spire_heat_sweep") {
            return "enemy_attack_flame"
        }

        // 13. Plasma / explosive / thermal / fire bursts (before erosion so plasma torch_cut plays plasma)
        if ("plasma" in tags || "explosive" in tags || "burn" in tags || "fire" in tags ||
            "meltdown" in statuses || "burn" in statuses ||
            hasToken("flashbang", "salvo", "barrage") ||
            (hasToken("grenade") && !hasToken("gas") && "erosion" !in tags)
        ) {
            return "skill_plasma_blast"
        }

        // 14. Spore clouds
        if (hasToken("spore")) {
            return "enemy_attack_spore"
        }

        // 15. Acid / corrosive / toxic / erosion / gas
        if ("acid" in tags || "erosion" in tags || "erosion" in statuses ||
            hasToken("acid", "dissolve", "toxic", "corrosive", "exhale", "spill", "gas")
        ) {
            return "skill_acid_burn"
        }

        // 16. Source / Lattice / Reality harmonic waves
        if ("source" in tags || hasToken("lattice", "reality") ||
            sid in setOf("resonance_beam", "source_memory_copy", "source_memory_command")
        ) {
            return "skill_source_harmonic"
        }

        // 17. Precision shots / beams / volleys / snipes / lasers
        if ("laser" in tags || "pierce" in tags ||
            hasToken("headshot", "shot", "beam", "volley", "execution", "fire", "burst", "bolt", "purge", "judgment", "frequency", "fracture")
        ) {
            return "skill_precision_shot"
        }

        // 18. Remaining zero-power buffs / stances
        if (power == 0 && skill != null) {
            return "skill_power_up"
        }

        // 19. Explicit physical bite / slash hints before generic enemy body fallback
        if (hasToken("bite", "gnaw")) {
            return "enemy_attack_bite"
        }
        if (hasToken("slash", "claw", "swipe", "cut")) {
            return "enemy_attack_slash"
        }

        return null
    }

    private fun resolveEnemyBodyCue(normalized: String, enemy: Enemy?): String {
        val tags = enemy?.tags.orEmpty().map { it.lowercase(Locale.ROOT) }
        val element = enemy?.element?.lowercase(Locale.ROOT)

        return when {
            "massive" in tags || "industrial" in tags || normalized.contains("loader") -> "enemy_attack_slam"
            "robotic" in tags || "machine" in tags || "mech" in tags -> {
                if ("flying" in tags || "drone" in tags) "enemy_attack_drone" else "robot_stomp"
            }
            "crawler" in tags || "beast" in tags || normalized.contains("hound") || normalized.contains("borer") -> {
                if (normalized.contains("borer") || normalized.contains("crawler")) "enemy_attack_bite" else "enemy_attack_slash"
            }
            "flora" in tags || normalized.contains("spitter") || normalized.contains("vine") -> "enemy_attack_spore"
            element == "acid" -> "enemy_attack_acid"
            element == "fire" || element == "burn" || "foundry" in tags -> "enemy_attack_flame"
            "source" in tags || "shadow" in tags || "nightmare" in tags || "digital" in tags -> "enemy_attack_void"
            "human" in tags || "dominion" in tags -> "enemy_attack_gunshot"
            "biological" in tags -> "enemy_screech"
            else -> "enemy_attack_slash"
        }
    }
}
