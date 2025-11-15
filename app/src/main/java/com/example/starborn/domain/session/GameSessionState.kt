package com.example.starborn.domain.session

import java.security.MessageDigest
import java.util.Locale
import kotlin.text.Charsets

data class GameSessionState(
    val worldId: String? = null,
    val hubId: String? = null,
    val roomId: String? = null,
    val playerId: String? = null,
    val playerLevel: Int = 1,
    val playerXp: Int = 0,
    val playerAp: Int = 0,
    val playerCredits: Int = 0,
    val partyMemberXp: Map<String, Int> = emptyMap(),
    val partyMemberLevels: Map<String, Int> = emptyMap(),
    val partyMemberHp: Map<String, Int> = emptyMap(),
    val partyMemberRp: Map<String, Int> = emptyMap(),
    val trackedQuestId: String? = null,
    val activeQuests: Set<String> = emptySet(),
    val completedQuests: Set<String> = emptySet(),
    val failedQuests: Set<String> = emptySet(),
    val completedMilestones: Set<String> = emptySet(),
    val milestoneHistory: List<String> = emptyList(),
    val learnedSchematics: Set<String> = emptySet(),
    val unlockedSkills: Set<String> = emptySet(),
    val unlockedAreas: Set<String> = emptySet(),
    val unlockedExits: Set<String> = emptySet(),
    val partyMembers: List<String> = emptyList(),
    val inventory: Map<String, Int> = emptyMap(),
    val equippedItems: Map<String, String> = emptyMap(),
    val tutorialSeen: Set<String> = emptySet(),
    val tutorialCompleted: Set<String> = emptySet(),
    val tutorialRoomsSeen: Set<String> = emptySet(),
    val questStageById: Map<String, String> = emptyMap(),
    val questTasksCompleted: Map<String, Set<String>> = emptyMap(),
    val completedEvents: Set<String> = emptySet(),
    val resonance: Int = 0,
    val resonanceMin: Int = 0,
    val resonanceMax: Int = 100,
    val resonanceStartBase: Int = 0
)

fun GameSessionState.fingerprint(): String {
    val normalizedLocale = Locale.US
    val builder = StringBuilder()
    builder.append(worldId.orEmpty()).append('|')
    builder.append(hubId.orEmpty()).append('|')
    builder.append(roomId.orEmpty()).append('|')
    builder.append(playerId.orEmpty()).append('|')
    builder.append(playerLevel).append('|').append(playerXp).append('|').append(playerCredits).append('|')
    builder.append(trackedQuestId.orEmpty()).append('|')
    activeQuests.map { it.lowercase(normalizedLocale) }.sorted().forEach {
        builder.append("AQ:").append(it).append('|')
    }
    completedQuests.map { it.lowercase(normalizedLocale) }.sorted().forEach {
        builder.append("CQ:").append(it).append('|')
    }
    questStageById.entries.sortedBy { it.key.lowercase(normalizedLocale) }.forEach { (questId, stageId) ->
        builder.append("QS:").append(questId.lowercase(normalizedLocale)).append('=')
            .append(stageId.lowercase(normalizedLocale)).append('|')
    }
    questTasksCompleted.entries.sortedBy { it.key.lowercase(normalizedLocale) }.forEach { (questId, tasks) ->
        tasks.map { it.lowercase(normalizedLocale) }.sorted().forEach { taskId ->
            builder.append("QT:").append(questId.lowercase(normalizedLocale)).append(':').append(taskId).append('|')
        }
    }
    inventory.entries.sortedBy { it.key.lowercase(normalizedLocale) }.forEach { (itemId, qty) ->
        builder.append("INV:").append(itemId.lowercase(normalizedLocale)).append('=').append(qty).append('|')
    }
    unlockedAreas.map { it.lowercase(normalizedLocale) }.sorted().forEach {
        builder.append("AREA:").append(it).append('|')
    }
    unlockedExits.map { it.lowercase(normalizedLocale) }.sorted().forEach {
        builder.append("EXIT:").append(it).append('|')
    }
    completedEvents.map { it.lowercase(normalizedLocale) }.sorted().forEach {
        builder.append("EVT:").append(it).append('|')
    }
    val digest = MessageDigest.getInstance("SHA-1")
    val hash = digest.digest(builder.toString().toByteArray(Charsets.UTF_8))
    return hash.joinToString(separator = "") { byte -> "%02x".format(normalizedLocale, byte) }
}
