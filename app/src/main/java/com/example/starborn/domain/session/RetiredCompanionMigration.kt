package com.example.starborn.domain.session

/** Ollie remains an NPC. Keep owned legacy items and story progress, but retire his party state. */
internal fun GameSessionState.withoutRetiredCompanion(): GameSessionState {
    fun retired(id: String?) = id?.trim()?.equals("ollie", ignoreCase = true) == true
    val replacePlayer = retired(playerId)
    val remaining = partyMembers.filterNot(::retired).toMutableList()
    val replacement = if (replacePlayer) "nova" else playerId
    if ((replacePlayer || (partyMembers.isNotEmpty() && remaining.isEmpty())) && replacement != null && replacement !in remaining) {
        remaining.add(0, replacement)
    }
    return copy(
        playerId = replacement,
        playerLevel = if (replacePlayer) partyMemberLevels["nova"] ?: 1 else playerLevel,
        playerXp = if (replacePlayer) partyMemberXp["nova"] ?: 0 else playerXp,
        partyMembers = remaining,
        partyMemberLevels = partyMemberLevels.filterKeys { !retired(it) },
        partyMemberXp = partyMemberXp.filterKeys { !retired(it) },
        partyMemberHp = partyMemberHp.filterKeys { !retired(it) },
        unlockedWeapons = unlockedWeapons + equippedWeapons.filterKeys { retired(it) }.values,
        unlockedArmors = unlockedArmors + equippedArmors.filterKeys { retired(it) }.values,
        equippedWeapons = equippedWeapons.filterKeys { !retired(it) },
        equippedArmors = equippedArmors.filterKeys { !retired(it) },
        equippedItems = equippedItems.filterKeys { !retired(it.substringBefore(':')) },
        mealChefId = if (retired(mealChefId)) "nova" else mealChefId,
        battleCheckpoint = battleCheckpoint?.withoutRetiredCompanion()
    )
}
