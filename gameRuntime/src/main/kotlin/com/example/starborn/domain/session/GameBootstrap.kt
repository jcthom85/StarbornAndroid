package com.example.starborn.domain.session

import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.leveling.ProgressionData
import java.util.Locale
import kotlin.random.Random

/** The Android opening and NG+ policy; platform services only apply side effects and persistence. */
object GameBootstrap {
    const val startingQuest = "w1_mq01"
    const val startingStage = "wake_in_the_pit"
    val defaultWeapons = mapOf("nova" to "nova_laser_blaster", "zeke" to "zeke_shock_fists", "orion" to "orion_prism_focus", "gh0st" to "gh0st_whisperblade")
    val defaultArmors = mapOf("nova" to "nova_flux_liner", "zeke" to "zeke_surge_harness", "orion" to "orion_channeler_mantle", "gh0st" to "gh0st_phaseweave_jacket")
    val defaultSnacks = mapOf("nova" to "starbar_crunch", "zeke" to "mineral_trail_mix", "orion" to "comet_gummies", "gh0st" to "void_jerky")

    fun startingSkills(party: List<String>, players: List<Player>, progression: ProgressionData, level: Int,
        allSkills: List<Skill> = emptyList(), unlockAll: Boolean = false): Set<String> =
        if (unlockAll) allSkills.filter { it.character in party }.map { it.id }.toSet()
        else party.flatMap { member ->
            players.firstOrNull { it.id == member }?.skills.orEmpty() +
                progression.levelUpSkills[member].orEmpty().filterKeys { (it.toIntOrNull() ?: Int.MAX_VALUE) <= level }.values
        }.toSet()

    fun chooseWeapon(character: String, items: List<Item>, random: Random): Item? = choose(character, items, defaultWeapons, GearRules.allowedWeaponTypeFor(character), random, true)
    fun chooseArmor(character: String, items: List<Item>, random: Random): Item? = choose(character, items, defaultArmors, GearRules.allowedArmorTypeFor(character), random, false)
    private fun choose(character: String, items: List<Item>, defaults: Map<String, String>, expected: String?, random: Random, weapon: Boolean): Item? {
        if (items.isEmpty()) return null
        fun type(item: Item) = (if (weapon) item.equipment?.weaponType else null)?.trim()?.lowercase(Locale.ROOT) ?: item.type.trim().lowercase(Locale.ROOT)
        items.firstOrNull { it.id.equals(defaults[character.trim().lowercase(Locale.ROOT)], true) && (expected == null || type(it) == expected) }?.let { return it }
        val matching = if (expected == null) items else items.filter { type(it) == expected }
        val pool = matching.ifEmpty { items }
        return pool[random.nextInt(pool.size)]
    }

    fun startingWeapons(characters: List<String>, items: List<Item>, unlockAll: Boolean, random: Random = Random.Default): Pair<Set<String>, Map<String, String>> =
        gear(characters, items.filter { it.type.equals("weapon", true) || GearRules.isWeaponType(it.type) || it.equipment?.slot.equals("weapon", true) }, unlockAll, random, true)
    fun startingArmors(characters: List<String>, items: List<Item>, unlockAll: Boolean, random: Random = Random.Default): Pair<Set<String>, Map<String, String>> =
        gear(characters, items.filter { it.type.equals("armor", true) || it.equipment?.slot.equals("armor", true) }, unlockAll, random, false)
    private fun gear(characters: List<String>, items: List<Item>, unlockAll: Boolean, random: Random, weapon: Boolean): Pair<Set<String>, Map<String, String>> {
        val equipped = characters.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() && (unlockAll || it != "nova") }.distinct().mapNotNull { id ->
            (if (weapon) chooseWeapon(id, items, random) else chooseArmor(id, items, random))?.let { id to it.id }
        }.toMap()
        return (if (unlockAll) items.map { it.id }.toSet() else equipped.values.toSet()) to equipped
    }

    fun newGame(players: List<Player>, items: List<Item>, debug: Boolean = false): GameSessionState {
        val player = players.firstOrNull()
        val playerId = player?.id ?: "nova"
        val party = if (debug) listOf("nova", "zeke", "orion", "gh0st") else listOf(playerId)
        val roster = players.map { it.id }.ifEmpty { party }
        val (weapons, equippedWeapons) = startingWeapons(roster, items, debug)
        val (armors, equippedArmors) = startingArmors(roster, items, debug)
        return GameSessionState(worldId = "world_1", hubId = "hub_1_homestead", roomId = "pit_nova_bunk", playerId = playerId,
            playerLevel = player?.level ?: 1, playerXp = player?.xp ?: 0, partyMembers = party,
            partyMemberLevels = party.associateWith { player?.level ?: 1 }, partyMemberXp = party.associateWith { player?.xp ?: 0 },
            unlockedWeapons = weapons, unlockedArmors = armors, equippedWeapons = equippedWeapons, equippedArmors = equippedArmors,
            equippedItems = if (debug) roster.mapNotNull { id -> defaultSnacks[id]?.let { "$id:snack" to it } }.toMap() else emptyMap())
    }

    fun newGamePlus(players: List<Player>, items: List<Item>, previous: GameSessionState): GameSessionState {
        val base = newGame(players, items)
        val party = previous.partyMembers.ifEmpty { base.partyMembers }
        val (weapons, equippedWeapons) = startingWeapons(party, items, false)
        val (armors, equippedArmors) = startingArmors(party, items, false)
        return base.copy(partyMembers = party, playerLevel = previous.playerLevel.coerceAtLeast(base.playerLevel), playerXp = previous.playerXp,
            playerCredits = previous.playerCredits.coerceAtLeast(5000), inventory = previous.inventory,
            partyMemberLevels = previous.partyMemberLevels.ifEmpty { party.associateWith { base.playerLevel } },
            partyMemberXp = previous.partyMemberXp.ifEmpty { party.associateWith { base.playerXp } },
            unlockedWeapons = previous.unlockedWeapons + weapons, unlockedArmors = previous.unlockedArmors + armors,
            equippedWeapons = previous.equippedWeapons.ifEmpty { equippedWeapons }, equippedArmors = previous.equippedArmors.ifEmpty { equippedArmors },
            equippedItems = previous.equippedItems, completedMilestones = setOf("ms_master_protocol_active"))
    }
}
