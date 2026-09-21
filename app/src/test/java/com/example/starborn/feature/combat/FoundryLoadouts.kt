package com.example.starborn.feature.combat

import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.leveling.LevelingManager

/** Attainable loadout brackets, not a measured campaign inventory distribution. */
internal object FoundryLoadouts {
    val party = listOf("nova", "zeke", "orion", "gh0st")
    val starterWeapons = mapOf("nova" to "mining_pistol", "zeke" to "zeke_shock_fists",
        "orion" to "orion_prism_focus", "gh0st" to "gh0st_whisperblade")
    val starterArmor = mapOf("nova" to "nova_flux_liner", "zeke" to "zeke_surge_harness",
        "orion" to "orion_channeler_mantle", "gh0st" to "gh0st_phaseweave_jacket")
    val purchasedWeapons = mapOf("nova" to "nova_plasma_shotgun", "zeke" to "zeke_quake_knuckles",
        "orion" to "orion_halo_array", "gh0st" to "gh0st_splitter_edge")
    val purchasedArmor = mapOf("nova" to "nova_starline_jacket", "zeke" to "zeke_brawler_rig",
        "orion" to "orion_driftweave_cloak", "gh0st" to "gh0st_killer_harness")

    fun session(name: String, assets: WorldAssetDataSource, xp: Int = 11000): GameSessionState {
        require(xp >= 0)
        val level = LevelingManager(requireNotNull(assets.loadLevelingData())).levelForXp(xp)
        if (name.endsWith("_ap")) {
            val base = name.removeSuffix("_ap")
            require(base in setOf("weak", "purchased"))
            return session(base, assets, xp).let { it.copy(
                unlockedSkills = it.unlockedSkills + setOf("nova_quiet_steps", "gh0st_scope"),
                playerAp = 0 // Administrator's two shared AP spent on two legal root nodes.
            ) }
        }
        require(name in setOf("bare", "starter", "weak", "purchased"))
        val progressed = name in setOf("weak", "purchased")
        val skills = assets.loadCharacters().filter { it.id in party }.flatMap { it.skills }.toMutableSet()
        if (progressed) assets.loadProgressionData()!!.levelUpSkills.filterKeys { it in party }.values.forEach { tiers ->
            skills.addAll(tiers.filterKeys { it.toInt() <= level }.values)
        }
        return GameSessionState(worldId = "world_4", hubId = "hub_7_slag_pits", roomId = "foundry_waste_intake",
            playerId = "nova", partyMembers = party, playerLevel = level, playerXp = xp,
            partyMemberLevels = party.associateWith { level }, partyMemberXp = party.associateWith { xp },
            equippedWeapons = when(name) { "bare" -> emptyMap(); "purchased" -> purchasedWeapons; else -> starterWeapons },
            equippedArmors = when(name) { "bare" -> emptyMap(); "purchased" -> purchasedArmor; else -> starterArmor },
            unlockedSkills = skills,
            inventory = if (progressed) mapOf("medkit" to 3) else emptyMap(),
            completedMilestones = if (progressed) setOf("ms_w1_mq03_complete", "ms_w1_mq05_complete", "ms_w4_access_unlocked") else emptySet())
    }
}
