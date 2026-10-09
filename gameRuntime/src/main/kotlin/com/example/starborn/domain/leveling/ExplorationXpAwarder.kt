package com.example.starborn.domain.leveling

import com.example.starborn.domain.session.GameSessionStore

/** Gives each current party member the full exploration award, as combat does. */
class ExplorationXpAwarder(
    private val store: GameSessionStore,
    private val leveling: LevelingManager,
    private val progression: ProgressionData
) {
    fun award(amount: Int) {
        if (amount <= 0) return
        val before = store.state.value
        val recipients = (before.partyMembers + listOfNotNull(before.playerId)).filter { it.isNotBlank() }.distinct()
        if (recipients.isEmpty()) {
            store.addXp(amount)
            return
        }
        recipients.forEach { playerId ->
            // Snapshot fallbacks before any writes so a missing companion entry
            // cannot inherit the lead's newly increased XP and receive it twice.
            val previousXp = before.partyMemberXp[playerId] ?: before.playerXp
            val previousLevel = before.partyMemberLevels[playerId] ?: before.playerLevel
            val newXp = previousXp + amount
            store.setPartyMemberXp(playerId, newXp)
            val newLevel = maxOf(previousLevel, leveling.levelForXp(newXp))
            store.setPartyMemberLevel(playerId, newLevel)
            progression.levelUpSkills[playerId].orEmpty().forEach skill@{ (level, skillId) ->
                val threshold = level.toIntOrNull() ?: return@skill
                if (threshold in 1..newLevel) store.unlockSkill(skillId)
            }
        }
    }
}
