package com.example.starborn.feature.mainmenu

import com.example.starborn.domain.session.GameSessionState

/** Booth-only fixtures. These grants never change campaign progression or enemy balance. */
object BurgQuestDemo {
    val party = listOf("nova", "zeke", "orion", "gh0st")
    val weapons = mapOf("nova" to "nova_laser_blaster", "zeke" to "zeke_shock_fists",
        "orion" to "orion_prism_focus", "gh0st" to "gh0st_whisperblade")
    val armor = mapOf("nova" to "nova_flux_liner", "zeke" to "zeke_surge_harness",
        "orion" to "orion_channeler_mantle", "gh0st" to "gh0st_phaseweave_jacket")
    val snacks = mapOf("nova" to "starbar_crunch", "zeke" to "mineral_trail_mix",
        "orion" to "comet_gummies", "gh0st" to "void_jerky")
    val skills = setOf("nova_arc_tether", "nova_link", "zeke_shatter_blow", "zeke_overload_fists",
        "orion_prism_lance", "orion_nano_repair", "gh0st_headshot", "gh0st_venom_edge")

    fun enemies(id: String): List<String> = when (id) {
        "burgfest_combat" -> listOf("siren_skimmer", "spore_spitter")
        "burgfest_boss" -> listOf("titan_walker_boss")
        else -> emptyList()
    }

    fun level(id: String): Int = if (id == "burgfest_boss") 8 else 5

    fun briefing(id: String): String = when (id) {
        "burgfest_combat" -> "Defeat the Siren Skimmer and Spore-Spitter with all four crew members. Tap a ready portrait, then choose an attack and target. Try Arc Tether or Shatter Blow. Use Link, Nano Repair or a medkit when the crew is hurt. Take your time choosing actions."
        "burgfest_boss" -> "Defeat Titan Walker. Shock skills help against its armor. After a barrage or stomp, watch for Vent Exposure: use that recovery turn to attack or heal. Your crew has fixed gear and limited medkits; this is the harder showcase."
        "burgfest_astra" -> "Start in the common room. Tap a restored arcade cabinet and play a round, then return to the ship. Optional: head west to the cargo bay, tap Astra workbench and try the stocked Cryo-Inductor recipe. Use Finish demo whenever you are ready."
        else -> "Meet Nova in the campaign opening. Follow the dialogue and quest prompts at your own pace. This is a story preview, not a promise of combat within five minutes. Use Finish demo when you have seen enough."
    }

    fun curate(state: GameSessionState, id: String, xp: Int): GameSessionState {
        require(id in setOf("burgfest_combat", "burgfest_boss", "burgfest_astra"))
        val stock = weapons.values.associateWith { 1 } + armor.values.associateWith { 1 } +
            snacks.values.associateWith { 3 } + mapOf("medkit" to if (id == "burgfest_boss") 8 else 4)
        return state.copy(
            partyMembers = party, playerId = "nova", playerLevel = level(id), playerXp = xp,
            partyMemberLevels = party.associateWith { level(id) }, partyMemberXp = party.associateWith { xp },
            partyMemberHp = emptyMap(), playerAp = 0, playerCredits = 0,
            inventory = stock, equippedWeapons = weapons, equippedArmors = armor,
            equippedItems = snacks.mapKeys { "${it.key}:snack" },
            unlockedWeapons = weapons.values.toSet(), unlockedArmors = armor.values.toSet(),
            unlockedSkills = skills, activeMealBuff = null,
            activeQuests = emptySet(), trackedQuestId = null, questStageById = emptyMap(),
            pendingBattleJson = "", battleCheckpoint = null, pendingEventCinematics = emptySet()
        )
    }
}
