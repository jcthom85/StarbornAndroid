package com.example.starborn.domain.session

import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameSessionStore {

    private val _state = MutableStateFlow(GameSessionState())
    val state: StateFlow<GameSessionState> = _state.asStateFlow()

    fun restore(state: GameSessionState) {
        val normalized = normalizeArmors(normalizeWeapons(normalizeEquipment(state)))
        if (_state.value == normalized) return
        _state.value = normalized
    }

    fun setWorld(worldId: String?) {
        _state.update { it.copy(worldId = worldId) }
    }

    fun setHub(hubId: String?) {
        _state.update { it.copy(hubId = hubId) }
    }

    fun setRoom(roomId: String?) {
        _state.update { it.copy(roomId = roomId) }
    }

    fun setPlayer(playerId: String?) {
        _state.update {
            val sanitizedId = playerId
            val updatedLevels = if (sanitizedId != null) {
                it.partyMemberLevels + (sanitizedId to it.playerLevel)
            } else it.partyMemberLevels
            val updatedXp = if (sanitizedId != null) {
                it.partyMemberXp + (sanitizedId to it.playerXp)
            } else it.partyMemberXp
            it.copy(
                playerId = sanitizedId,
                partyMemberLevels = updatedLevels,
                partyMemberXp = updatedXp
            )
        }
    }

    fun setPlayerLevel(level: Int) {
        if (level <= 0) return
        _state.update { state ->
            val playerId = state.playerId
            val updatedLevels = if (playerId != null) {
                state.partyMemberLevels + (playerId to level)
            } else state.partyMemberLevels
            state.copy(
                playerLevel = level,
                partyMemberLevels = updatedLevels
            )
        }
    }

    fun startQuest(questId: String, track: Boolean = false) {
        if (questId.isBlank()) return
        _state.update {
            it.copy(
                activeQuests = it.activeQuests + questId,
                failedQuests = it.failedQuests - questId,
                trackedQuestId = if (track || it.trackedQuestId.isNullOrBlank()) questId else it.trackedQuestId
            )
        }
    }

    fun completeQuest(questId: String) {
        _state.update {
            it.copy(
                activeQuests = it.activeQuests - questId,
                completedQuests = it.completedQuests + questId,
                failedQuests = it.failedQuests - questId,
                trackedQuestId = it.trackedQuestId?.takeUnless { tracked -> tracked.equals(questId, ignoreCase = true) }
            )
        }
    }

    fun setMilestone(milestoneId: String) {
        if (milestoneId.isBlank()) return
        _state.update {
            if (milestoneId in it.completedMilestones) {
                it
            } else {
                val history = (it.milestoneHistory + milestoneId).takeLast(20)
                it.copy(
                    completedMilestones = it.completedMilestones + milestoneId,
                    milestoneHistory = history
                )
            }
        }
    }

    fun clearMilestone(milestoneId: String) {
        _state.update { it.copy(completedMilestones = it.completedMilestones - milestoneId) }
    }

    fun addXp(amount: Int) {
        if (amount <= 0) return
        _state.update { state ->
            val playerId = state.playerId
            val newXp = state.playerXp + amount
            val updatedXp = if (playerId != null) {
                state.partyMemberXp + (playerId to newXp)
            } else state.partyMemberXp
            state.copy(
                playerXp = newXp,
                partyMemberXp = updatedXp
            )
        }
    }

    fun setPlayerXp(amount: Int) {
        if (amount < 0) return
        _state.update { state ->
            val playerId = state.playerId
            val updatedXp = if (playerId != null) {
                state.partyMemberXp + (playerId to amount)
            } else state.partyMemberXp
            state.copy(
                playerXp = amount,
                partyMemberXp = updatedXp
            )
        }
    }

    fun addAp(amount: Int) {
        if (amount <= 0) return
        _state.update { it.copy(playerAp = it.playerAp + amount) }
    }

    fun spendAp(amount: Int): Boolean {
        if (amount <= 0) return true
        var success = false
        _state.update { state ->
            if (state.playerAp >= amount) {
                success = true
                state.copy(playerAp = state.playerAp - amount)
            } else {
                state
            }
        }
        return success
    }

    fun addCredits(amount: Int) {
        if (amount <= 0) return
        _state.update { it.copy(playerCredits = it.playerCredits + amount) }
    }

    fun spendCredits(amount: Int): Boolean {
        if (amount <= 0) return true
        var success = false
        _state.update { state ->
            if (state.playerCredits >= amount) {
                success = true
                state.copy(playerCredits = state.playerCredits - amount)
            } else {
                state
            }
        }
        return success
    }

    fun learnSchematic(schematicId: String) {
        if (schematicId.isBlank()) return
        _state.update { current ->
            if (schematicId in current.learnedSchematics) current
            else current.copy(learnedSchematics = current.learnedSchematics + schematicId)
        }
    }

    fun unlockArea(areaId: String) {
        if (areaId.isBlank()) return
        _state.update { it.copy(unlockedAreas = it.unlockedAreas + areaId) }
    }

    fun unlockExit(roomId: String?, direction: String?) {
        val key = buildExitKey(roomId, direction) ?: return
        _state.update { it.copy(unlockedExits = it.unlockedExits + key) }
    }

    fun setTrackedQuest(questId: String?) {
        _state.update { it.copy(trackedQuestId = questId?.takeIf { id -> id.isNotBlank() }) }
    }

    fun failQuest(questId: String) {
        if (questId.isBlank()) return
        _state.update {
            val newTracked = it.trackedQuestId?.takeUnless { tracked -> tracked.equals(questId, ignoreCase = true) }
            it.copy(
                activeQuests = it.activeQuests - questId,
                failedQuests = it.failedQuests + questId,
                trackedQuestId = newTracked
            )
        }
    }

    fun clearFailedQuest(questId: String) {
        if (questId.isBlank()) return
        _state.update { it.copy(failedQuests = it.failedQuests - questId) }
    }

    fun forgetSchematic(schematicId: String) {
        if (schematicId.isBlank()) return
        _state.update { it.copy(learnedSchematics = it.learnedSchematics - schematicId) }
    }

    fun setEquippedItem(slotId: String, itemId: String?, characterId: String? = null) {
        if (slotId.isBlank()) return
        val normalizedSlot = slotId.trim().lowercase(Locale.getDefault())
        if (normalizedSlot == "weapon" || normalizedSlot == "armor") return
        val ownerId = characterId?.trim()?.lowercase(Locale.getDefault())
        _state.update { state ->
            val updated = state.equippedItems.toMutableMap()

            val normalizedItem = itemId?.trim()
            if (!normalizedItem.isNullOrBlank()) {
                // Ensure uniqueness: if another slot (or character) is holding this item, clear it.
                val targetKey = ownerId?.let { "$it:$normalizedSlot" } ?: normalizedSlot
                val duplicateKeys = updated.filterValues { it.equals(normalizedItem, ignoreCase = true) }
                    .keys
                    .filterNot { key -> key.equals(targetKey, ignoreCase = true) }
                duplicateKeys.forEach { updated.remove(it) }
            }

            if (ownerId != null) {
                val scopedKey = "$ownerId:$normalizedSlot"
                // Clear any lingering party-wide entry for this slot.
                updated.remove(normalizedSlot)
                if (normalizedItem.isNullOrBlank()) {
                    updated.remove(scopedKey)
                } else {
                    updated[scopedKey] = normalizedItem
                }
            } else {
                if (normalizedItem.isNullOrBlank()) updated.remove(normalizedSlot) else updated[normalizedSlot] = normalizedItem
            }
            state.copy(equippedItems = updated)
        }
    }

    fun unlockSkill(skillId: String) {
        if (skillId.isBlank()) return
        _state.update { it.copy(unlockedSkills = it.unlockedSkills + skillId) }
    }

    fun unlockWeapon(weaponId: String) {
        if (weaponId.isBlank()) return
        _state.update { it.copy(unlockedWeapons = it.unlockedWeapons + weaponId) }
    }

    fun unlockArmor(armorId: String) {
        if (armorId.isBlank()) return
        _state.update { it.copy(unlockedArmors = it.unlockedArmors + armorId) }
    }

    fun setEquippedWeapon(characterId: String, weaponId: String?) {
        if (characterId.isBlank()) return
        val normalizedId = characterId.trim().lowercase(Locale.getDefault())
        _state.update { state ->
            val updated = state.equippedWeapons.toMutableMap()
            val unlocked = state.unlockedWeapons.toMutableSet()
            val normalizedWeapon = weaponId?.trim()
            if (normalizedWeapon.isNullOrBlank()) {
                updated.remove(normalizedId)
            } else {
                updated[normalizedId] = normalizedWeapon
                unlocked.add(normalizedWeapon)
            }
            state.copy(
                equippedWeapons = updated,
                unlockedWeapons = unlocked
            )
        }
    }

    fun setEquippedArmor(characterId: String, armorId: String?) {
        if (characterId.isBlank()) return
        val normalizedId = characterId.trim().lowercase(Locale.getDefault())
        _state.update { state ->
            val updated = state.equippedArmors.toMutableMap()
            val unlocked = state.unlockedArmors.toMutableSet()
            val normalizedArmor = armorId?.trim()
            if (normalizedArmor.isNullOrBlank()) {
                updated.remove(normalizedId)
            } else {
                updated[normalizedId] = normalizedArmor
                unlocked.add(normalizedArmor)
            }
            state.copy(
                equippedArmors = updated,
                unlockedArmors = unlocked
            )
        }
    }

    fun setPartyMembers(ids: List<String>) {
        _state.update { state ->
            val distinct = ids.distinct()
            val filteredXp = state.partyMemberXp.filterKeys { it in distinct }
            val filteredLevels = state.partyMemberLevels.filterKeys { it in distinct }
            val seededXp = distinct.fold(filteredXp) { acc, id ->
                if (acc.containsKey(id)) acc else acc + (id to state.playerXp)
            }
            val seededLevels = distinct.fold(filteredLevels) { acc, id ->
                if (acc.containsKey(id)) acc else acc + (id to state.playerLevel)
            }
            val filteredHp = state.partyMemberHp.filterKeys { it in distinct }
            normalizeArmors(
                normalizeWeapons(
                    normalizeEquipment(
                        state.copy(
                            partyMembers = distinct,
                            partyMemberXp = seededXp,
                            partyMemberLevels = seededLevels,
                            partyMemberHp = filteredHp
                        )
                    )
                )
            )
        }
    }

    fun setInventory(entries: Map<String, Int>) {
        val normalized = entries.filterValues { it > 0 }
        _state.update { it.copy(inventory = normalized) }
    }

    fun markEventCompleted(eventId: String) {
        if (eventId.isBlank()) return
        _state.update { it.copy(completedEvents = it.completedEvents + eventId) }
    }

    fun clearEventCompletion(eventId: String) {
        if (eventId.isBlank()) return
        _state.update { it.copy(completedEvents = it.completedEvents - eventId) }
    }

    fun setQuestStage(questId: String, stageId: String?) {
        if (questId.isBlank()) return
        _state.update { state ->
            val updated = if (stageId.isNullOrBlank()) {
                state.questStageById - questId
            } else {
                state.questStageById + (questId to stageId)
            }
            state.copy(questStageById = updated)
        }
    }

    fun setQuestTasksCompleted(questId: String, taskIds: Set<String>) {
        if (questId.isBlank()) return
        val filtered = taskIds.filter { it.isNotBlank() }.toSet()
        _state.update { state ->
            val updated = if (filtered.isEmpty()) {
                state.questTasksCompleted - questId
            } else {
                state.questTasksCompleted + (questId to filtered)
            }
            state.copy(questTasksCompleted = updated)
        }
    }

    fun setQuestTaskCompleted(questId: String, taskId: String, completed: Boolean) {
        if (questId.isBlank() || taskId.isBlank()) return
        _state.update { state ->
            val current = state.questTasksCompleted[questId].orEmpty().toMutableSet()
            val changed = if (completed) current.add(taskId) else current.remove(taskId)
            if (!changed) {
                state
            } else {
                val updated = if (current.isEmpty()) {
                    state.questTasksCompleted - questId
                } else {
                    state.questTasksCompleted + (questId to current.toSet())
                }
                state.copy(questTasksCompleted = updated)
            }
        }
    }

    fun clearQuestTasks(questId: String) {
        if (questId.isBlank()) return
        _state.update { it.copy(questTasksCompleted = it.questTasksCompleted - questId) }
    }

    fun resetQuestProgress() {
        _state.update {
            it.copy(
                questStageById = emptyMap(),
                questTasksCompleted = emptyMap()
            )
        }
    }

    fun markTutorialSeen(id: String) {
        if (id.isBlank()) return
        _state.update { it.copy(tutorialSeen = it.tutorialSeen + id) }
    }

    fun markTutorialCompleted(id: String) {
        if (id.isBlank()) return
        _state.update {
            it.copy(
                tutorialSeen = it.tutorialSeen + id,
                tutorialCompleted = it.tutorialCompleted + id
            )
        }
    }

    fun markTutorialRoomVisited(roomId: String) {
        if (roomId.isBlank()) return
        _state.update { it.copy(tutorialRoomsSeen = it.tutorialRoomsSeen + roomId) }
    }

    fun resetTutorialProgress() {
        _state.update {
            it.copy(
                tutorialSeen = emptySet(),
                tutorialCompleted = emptySet(),
                tutorialRoomsSeen = emptySet()
            )
        }
    }

    fun addPartyMember(id: String) {
        if (id.isBlank()) return
        _state.update { state ->
            val updatedMembers = (state.partyMembers + id).distinct()
            val updatedXp = state.partyMemberXp + (id to state.partyMemberXp.getOrElse(id) { state.playerXp })
            val updatedLevels = state.partyMemberLevels + (id to state.partyMemberLevels.getOrElse(id) { state.playerLevel })
            state.copy(
                partyMembers = updatedMembers,
                partyMemberXp = updatedXp,
                partyMemberLevels = updatedLevels,
                partyMemberHp = state.partyMemberHp
            )
        }
    }

    fun removePartyMember(id: String) {
        if (id.isBlank()) return
        _state.update { state ->
            state.copy(
                partyMembers = state.partyMembers.filterNot { member -> member == id },
                partyMemberXp = state.partyMemberXp - id,
                partyMemberLevels = state.partyMemberLevels - id,
                partyMemberHp = state.partyMemberHp - id
            )
        }
    }

    fun setPartyMemberXp(id: String, amount: Int) {
        if (id.isBlank() || amount < 0) return
        _state.update { state ->
            val updatedMap = state.partyMemberXp + (id to amount)
            val updatedPlayerXp = if (id == state.playerId) amount else state.playerXp
            state.copy(
                partyMemberXp = updatedMap,
                playerXp = updatedPlayerXp
            )
        }
    }

    fun addPartyMemberXp(id: String, amount: Int) {
        if (id.isBlank() || amount <= 0) return
        _state.update { state ->
            val current = state.partyMemberXp[id] ?: state.playerXp
            val newAmount = (current + amount).coerceAtLeast(0)
            val updatedMap = state.partyMemberXp + (id to newAmount)
            val updatedPlayerXp = if (id == state.playerId) newAmount else state.playerXp
            state.copy(
                partyMemberXp = updatedMap,
                playerXp = updatedPlayerXp
            )
        }
    }

    fun setPartyMemberLevel(id: String, level: Int) {
        if (id.isBlank() || level <= 0) return
        _state.update { state ->
            val updatedLevels = state.partyMemberLevels + (id to level)
            val updatedPlayerLevel = if (id == state.playerId) level else state.playerLevel
            state.copy(
                partyMemberLevels = updatedLevels,
                playerLevel = updatedPlayerLevel
            )
        }
    }

    fun setPartyMemberHp(id: String, hp: Int) {
        if (id.isBlank()) return
        _state.update { state ->
            val clamped = hp.coerceAtLeast(0)
            val updated = state.partyMemberHp + (id to clamped)
            state.copy(partyMemberHp = updated)
        }
    }

    fun updatePartyVitals(snapshot: Map<String, Int>) {
        if (snapshot.isEmpty()) return
        _state.update { state ->
            var hpMap = state.partyMemberHp
            snapshot.forEach { (id, hp) ->
                hpMap = hpMap + (id to hp.coerceAtLeast(0))
            }
            state.copy(partyMemberHp = hpMap)
        }
    }

    private fun normalizeEquipment(state: GameSessionState): GameSessionState {
        val normalized = normalizeScopedEquipment(state)
        return if (normalized == state.equippedItems) state else state.copy(equippedItems = normalized)
    }

    private fun normalizeWeapons(state: GameSessionState): GameSessionState {
        val normalized = buildMap {
            state.equippedWeapons.forEach { (rawKey, rawValue) ->
                val characterId = rawKey.trim().lowercase(Locale.getDefault())
                val weaponId = rawValue.trim()
                if (characterId.isBlank() || weaponId.isBlank()) return@forEach
                put(characterId, weaponId)
            }
        }
        val unlocked = state.unlockedWeapons.toMutableSet()
        normalized.values.forEach { unlocked.add(it) }
        val updated = state.copy(
            equippedWeapons = normalized,
            unlockedWeapons = unlocked
        )
        return if (updated == state) state else updated
    }

    private fun normalizeArmors(state: GameSessionState): GameSessionState {
        val normalized = buildMap {
            state.equippedArmors.forEach { (rawKey, rawValue) ->
                val characterId = rawKey.trim().lowercase(Locale.getDefault())
                val armorId = rawValue.trim()
                if (characterId.isBlank() || armorId.isBlank()) return@forEach
                put(characterId, armorId)
            }
        }
        val unlocked = state.unlockedArmors.toMutableSet()
        normalized.values.forEach { unlocked.add(it) }
        val updated = state.copy(
            equippedArmors = normalized,
            unlockedArmors = unlocked
        )
        return if (updated == state) state else updated
    }

    private fun normalizeScopedEquipment(state: GameSessionState): Map<String, String> {
        val scoped = mutableMapOf<String, String>()
        val base = mutableMapOf<String, String>()

        state.equippedItems.forEach { (rawKey, rawValue) ->
            val value = rawValue.trim()
            val key = rawKey.trim()
            if (key.isBlank() || value.isBlank()) return@forEach
            if (key.contains(':')) {
                val owner = key.substringBefore(':').lowercase(Locale.getDefault())
                val slot = key.substringAfter(':').lowercase(Locale.getDefault())
                if (slot == "weapon" || slot == "armor") return@forEach
                if (owner.isNotBlank() && slot.isNotBlank()) {
                    scoped["$owner:$slot"] = value
                }
            } else {
                val slot = key.lowercase(Locale.getDefault())
                if (slot == "weapon" || slot == "armor") return@forEach
                base[slot] = value
            }
        }

        if (base.isEmpty()) {
            return scoped
        }

        val targets = state.partyMembers.ifEmpty { listOfNotNull(state.playerId) }
            .mapNotNull { it?.trim()?.lowercase(Locale.getDefault()) }
        if (targets.isEmpty() && scoped.isEmpty()) {
            return base
        }
        targets.forEach { id ->
            base.forEach { (slot, itemId) ->
                val scopedKey = "$id:$slot"
                scoped.putIfAbsent(scopedKey, itemId)
            }
        }
        return scoped
    }

    private fun buildExitKey(roomId: String?, direction: String?): String? {
        val normalizedRoom = roomId?.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault()) ?: return null
        val normalizedDirection = direction?.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault()) ?: return null
        return "$normalizedRoom$EXIT_KEY_SEPARATOR$normalizedDirection"
    }

    companion object {
        private const val EXIT_KEY_SEPARATOR = "::"
    }
}
