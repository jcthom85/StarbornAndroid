package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.model.ItemEffect
import com.example.starborn.domain.model.Skill
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CombatAbilityAudioResolverTest {

    private val moshi = MoshiProvider.instance

    private fun repoRoot(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (dir.parentFile != null && !File(dir, "app/src/main/assets/skills.json").exists()) {
            dir = dir.parentFile
        }
        assertTrue("Could not locate repo root from ${System.getProperty("user.dir")}", File(dir, "app/src/main/assets/skills.json").exists())
        return dir
    }

    private fun loadSkills(relativePath: String): List<Skill> {
        val file = File(repoRoot(), relativePath)
        val type = Types.newParameterizedType(List::class.java, Skill::class.java)
        val adapter = moshi.adapter<List<Skill>>(type)
        return adapter.fromJson(file.readText()).orEmpty()
    }

    private fun loadBattleBindings(): Map<String, String> {
        val file = File(repoRoot(), "app/src/main/assets/audio_bindings.json")
        val bindings = moshi.adapter(AudioBindings::class.java).fromJson(file.readText())
        assertNotNull(bindings)
        return bindings!!.battle
    }

    private fun loadCatalogIds(): Set<String> {
        val file = File(repoRoot(), "app/src/main/assets/audio_catalog.json")
        val raw = moshi.adapter(Map::class.java).fromJson(file.readText()) as Map<*, *>
        @Suppress("UNCHECKED_CAST")
        val cues = raw["cues"] as List<Map<String, Any?>>
        return cues.mapNotNull { it["id"] as? String }.toSet()
    }

    @Test
    fun everyAuthoredSkillResolvesToBoundCatalogedRawCue() {
        val skills = loadSkills("app/src/main/assets/skills.json") +
            loadSkills("app/src/main/assets/combat_prototypes/skills.json")
        assertTrue("Expected 200+ authored skills", skills.size >= 200)

        val battleBindings = loadBattleBindings()
        val catalogIds = loadCatalogIds()
        val rawDir = File(repoRoot(), "app/src/main/res/raw")

        for (skill in skills) {
            if (skill.type.equals("passive", ignoreCase = true)) continue
            val sourceId = if (skill.character in setOf("nova", "zeke", "orion", "gh0st", "ollie")) {
                skill.character
            } else {
                "enemy_generic"
            }
            val battleKey = CombatAbilityAudioResolver.resolveSkillCue(
                sourceId = sourceId,
                skillId = skill.id,
                skill = skill,
                enemy = null
            )
            val cueId = battleBindings[battleKey]
            assertNotNull("Skill '${skill.id}' resolved to unbound battle key '$battleKey'", cueId)
            assertTrue("Battle key '$battleKey' -> cue '$cueId' missing from audio_catalog.json", cueId in catalogIds)
            val mp3 = File(rawDir, "$cueId.mp3")
            val ogg = File(rawDir, "$cueId.ogg")
            val wav = File(rawDir, "$cueId.wav")
            assertTrue("Raw audio file missing for cue '$cueId' (skill '${skill.id}')", mp3.exists() || ogg.exists() || wav.exists())
        }
    }

    @Test
    fun signatureHeroAndBossAbilitiesMapToIntendedCues() {
        val skillsById = (loadSkills("app/src/main/assets/skills.json") +
            loadSkills("app/src/main/assets/combat_prototypes/skills.json")).associateBy { it.id }

        fun cueOf(sourceId: String, skillId: String): String =
            CombatAbilityAudioResolver.resolveSkillCue(sourceId, skillId, skillsById[skillId], null)

        // Player signature skills
        assertEquals("skill_shock_arc", cueOf("nova", "nova_arc_tether"))
        assertEquals("skill_cryo_freeze", cueOf("nova", "nova_cryo_vent"))
        assertEquals("skill_plasma_blast", cueOf("nova", "nova_plasma_burst"))
        assertEquals("skill_shockwave_slam", cueOf("nova", "nova_blast_wave"))
        assertEquals("skill_smoke_cloak", cueOf("nova", "nova_smoke_bomb"))
        assertEquals("skill_precision_shot", cueOf("nova", "nova_rend_round"))
        assertEquals("skill_heal", cueOf("nova", "nova_link"))
        assertEquals("skill_target_scan", cueOf("nova", "source_art_scan"))
        assertEquals("skill_cryo_freeze", cueOf("nova", "source_art_stasis"))
        assertEquals("skill_source_harmonic", cueOf("nova", "source_art_tune_world"))
        assertEquals("skill_shield_up", cueOf("zeke", "zeke_bulwark_stance"))
        assertEquals("skill_shock_arc", cueOf("zeke", "zeke_overload_fists"))
        assertEquals("skill_shockwave_slam", cueOf("zeke", "zeke_quake_slam"))
        assertEquals("skill_psionic_silence", cueOf("zeke", "zeke_signal_jammer"))
        assertEquals("skill_acid_burn", cueOf("orion", "orion_prism_lance"))
        assertEquals("skill_heal", cueOf("orion", "orion_nano_repair"))
        assertEquals("skill_cryo_freeze", cueOf("orion", "orion_stasis_field"))
        assertEquals("skill_plasma_blast", cueOf("orion", "orion_sunbeam"))
        assertEquals("skill_precision_shot", cueOf("gh0st", "gh0st_headshot"))
        assertEquals("skill_acid_burn", cueOf("gh0st", "gh0st_venom_edge"))
        assertEquals("skill_source_harmonic", cueOf("gh0st", "gh0st_system_crash"))
        assertEquals("skill_target_scan", cueOf("gh0st", "gh0st_target_lock"))
        assertEquals("skill_shield_up", cueOf("gh0st", "gh0st_harden"))

        // Enemy abilities across W1-W6
        assertEquals("skill_shield_up", cueOf("the_iron_warden", "iron_warden_riot_shield"))
        assertEquals("skill_power_up", cueOf("the_iron_warden", "iron_warden_overclock"))
        assertEquals("skill_acid_burn", cueOf("the_iron_warden", "iron_warden_gas_grenade"))
        assertEquals("enemy_attack_drone", cueOf("the_iron_warden", "iron_warden_drone_summon"))
        assertEquals("skill_heal", cueOf("enemy", "nature_heal"))
        assertEquals("skill_heal", cueOf("enemy", "field_weld"))
        assertEquals("skill_cryo_freeze", cueOf("enemy", "foundry_quench_vent"))
        assertEquals("skill_shockwave_slam", cueOf("enemy", "foundry_champion_hammer"))
        assertEquals("skill_plasma_blast", cueOf("enemy", "foundry_champion_heated"))
        assertEquals("skill_plasma_blast", cueOf("enemy", "torch_cut"))
        assertEquals("skill_precision_shot", cueOf("enemy", "ring_joint_execution"))
        assertEquals("skill_target_scan", cueOf("enemy", "ring_designate"))
        assertEquals("skill_psionic_silence", cueOf("ascended_vale", "source_great_silence"))
        assertEquals("skill_source_harmonic", cueOf("enemy", "spire_emergency_lattice"))
    }

    @Test
    fun combatItemsSnacksDefendAndPitchVariationWorkCleanly() {
        assertEquals("skill_heal", CombatAbilityAudioResolver.resolveItemCue("medkit", ItemEffect(restoreHp = 60)))
        assertEquals("skill_plasma_blast", CombatAbilityAudioResolver.resolveItemCue("pulse_grenade", ItemEffect(damage = 55)))
        assertEquals("skill_cryo_freeze", CombatAbilityAudioResolver.resolveItemCue("cryo_grenade", ItemEffect(damage = 40)))
        assertEquals("skill_shock_arc", CombatAbilityAudioResolver.resolveItemCue("emp_charge", ItemEffect(damage = 45)))
        assertEquals("skill_power_up", CombatAbilityAudioResolver.resolveItemCue("black_coffee", ItemEffect(type = "buff")))
        assertEquals("skill_shield_up", CombatAbilityAudioResolver.resolveDefendCue())

        assertTrue(CombatAbilityAudioResolver.shouldVaryPitch("skill_shock_arc"))
        assertTrue(CombatAbilityAudioResolver.shouldVaryPitch("wpn_nova_laser"))
        assertFalse(CombatAbilityAudioResolver.shouldVaryPitch("skill_heal"))
        assertFalse(CombatAbilityAudioResolver.shouldVaryPitch("skill_source_harmonic"))
        assertFalse(CombatAbilityAudioResolver.shouldVaryPitch("skill_target_scan"))
    }
}
