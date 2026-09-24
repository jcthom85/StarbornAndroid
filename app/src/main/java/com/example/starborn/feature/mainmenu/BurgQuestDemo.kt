package com.example.starborn.feature.mainmenu

import com.example.starborn.domain.session.GameSessionState

/** Booth-only fixtures. These grants never change campaign progression or enemy balance. */
object BurgQuestDemo {
    const val farewell = "Thanks for spending a little time with the crew. Want to know where their adventure goes next? Come chat with us at the booth."
    const val combatIntroduction = "Meet Nova, Zeke, Orion and Gh0st. Help them through a short skirmish, then visit their home aboard the Astra.\n\n1. Tap a ready crew portrait.\n2. Choose an attack or skill, then its target.\n3. Use a medkit or healing skill when someone needs help."

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
        "burgfest_combat" -> "$combatIntroduction\n\nTry Arc Tether or Shatter Blow. Link and Nano Repair help the crew recover. Take your time choosing actions. Use Demo → Finish demo whenever you like."
        "burgfest_boss" -> "Defeat Titan Walker. Shock skills help against its armor. After a barrage or stomp, watch for Vent Exposure: use that recovery turn to attack or heal. Your crew has fixed gear and limited medkits; this is the harder showcase."
        "burgfest_astra" -> "Make yourself at home. Talk to Orion or Gh0st in the common room, then choose something to try:\n\n• Play a round of Deep Mine Asteroid Drill at the common room cabinet.\n• Or head west to the cargo bay, tap Astra workbench and craft the stocked Cryo-Inductor recipe.\n\nNothing is required. Use Demo → Finish demo whenever you are ready."
        else -> "Meet Nova in the campaign opening. Follow the dialogue and quest prompts at your own pace. This is a reading-focused story preview; reaching combat can take longer than ten minutes. Use Demo → Finish demo when you have seen enough."
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
