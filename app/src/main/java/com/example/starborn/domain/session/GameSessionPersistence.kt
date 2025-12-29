package com.example.starborn.domain.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.example.starborn.datastore.GameSessionProto
import com.example.starborn.datastore.InventoryEntryProto
import com.example.starborn.datastore.QuestTaskListProto
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.inventory.ItemCatalog
import java.io.File
import java.util.Locale
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

private const val MAX_BACKUPS = 3

class GameSessionPersistence(context: Context) {

    private val appContext = context.applicationContext
    private val dataStoreFile = appContext.dataStoreFile(DATASTORE_FILE)
    private val autosaveFile = appContext.dataStoreFile(AUTOSAVE_FILE)
    private val quickSaveFile = appContext.dataStoreFile(QUICKSAVE_FILE)

    private val dataStore: DataStore<GameSessionProto> = dataStoreFor(appContext, dataStoreFile)
    private val autosaveStore: DataStore<GameSessionProto> = dataStoreFor(appContext, autosaveFile)
    private val quickSaveStore: DataStore<GameSessionProto> = dataStoreFor(appContext, quickSaveFile)

    val sessionFlow: Flow<GameSessionState> = dataStore.data.map { proto -> proto.toState() }

    suspend fun persist(state: GameSessionState) {
        val timestamp = System.currentTimeMillis()
        dataStore.updateWithBackup(dataStoreFile) { state.toProto(timestamp) }
    }

    suspend fun writeAutosave(state: GameSessionState) {
        val timestamp = System.currentTimeMillis()
        autosaveStore.updateWithBackup(autosaveFile) { state.toProto(timestamp) }
    }

    suspend fun readSlot(slot: Int): GameSessionState? = slotInfo(slot)?.state

    suspend fun readAutosave(): GameSessionState? = autosaveInfo()?.state

    suspend fun readQuickSave(): GameSessionState? = quickSaveInfo()?.state

    suspend fun writeSlot(slot: Int, state: GameSessionState) {
        val timestamp = System.currentTimeMillis()
        slotStore(slot).write { state.toProto(timestamp) }
    }

    suspend fun writeQuickSave(state: GameSessionState) {
        val timestamp = System.currentTimeMillis()
        quickSaveStore.updateWithBackup(quickSaveFile) { state.toProto(timestamp) }
    }

    suspend fun slotInfo(slot: Int): GameSessionSlotInfo? = slotStore(slot).read()

    suspend fun autosaveInfo(): GameSessionSlotInfo? {
        val proto = autosaveStore.data.first()
        if (proto == GameSessionProto.getDefaultInstance()) return null
        return GameSessionSlotInfo(proto.toState(), proto.lastSavedMs.takeIf { it > 0 })
    }

    suspend fun quickSaveInfo(): GameSessionSlotInfo? {
        val proto = quickSaveStore.data.first()
        if (proto == GameSessionProto.getDefaultInstance()) return null
        return GameSessionSlotInfo(proto.toState(), proto.lastSavedMs.takeIf { it > 0 })
    }

    suspend fun clearSlot(slot: Int) {
        slotStore(slot).clear()
    }

    suspend fun clearQuickSave() {
        quickSaveStore.updateData { GameSessionProto.getDefaultInstance() }
    }

    private fun slotStore(slot: Int): SlotStore = slotStoreFor(appContext, slot)

    companion object {
        private const val DATASTORE_FILE = "game_session.pb"
        private const val AUTOSAVE_FILE = "game_session_autosave.pb"
        private const val QUICKSAVE_FILE = "game_session_quicksave.pb"

        private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val sharedStores = java.util.concurrent.ConcurrentHashMap<String, DataStore<GameSessionProto>>()
        private val sharedSlotStores = java.util.concurrent.ConcurrentHashMap<String, SlotStore>()

        private fun dataStoreFor(context: Context, file: File): DataStore<GameSessionProto> {
            val key = file.absolutePath
            return sharedStores.getOrPut(key) {
                DataStoreFactory.create(
                    serializer = GameSessionSerializer,
                    corruptionHandler = ReplaceFileCorruptionHandler { GameSessionProto.getDefaultInstance() },
                    produceFile = { file },
                    scope = storeScope
                )
            }
        }

        private fun slotStoreFor(context: Context, slot: Int): SlotStore {
            val fileName = "game_session_slot$slot.pb"
            val file = context.dataStoreFile(fileName)
            val key = file.absolutePath
            return sharedSlotStores.getOrPut(key) {
                SlotStore(file)
            }
        }
    }
}

private data class SlotStore(
    val file: File,
    private val mutex: Mutex = Mutex()
) {
    suspend fun write(builder: () -> GameSessionProto) {
        mutex.withLock {
            val proto = builder()
            writeProto(file, proto)
            copyToBackup(file, proto)
        }
    }

    suspend fun read(): GameSessionSlotInfo? =
        mutex.withLock {
            if (!file.exists()) return null
            runCatching {
                file.inputStream().use { input ->
                    val proto = GameSessionSerializer.readFrom(input)
                    if (proto == GameSessionProto.getDefaultInstance()) null
                    else GameSessionSlotInfo(proto.toState(), proto.lastSavedMs.takeIf { it > 0 })
                }
            }.getOrNull()
        }

    suspend fun clear() {
        mutex.withLock {
            if (file.exists()) {
                file.delete()
            }
        }
    }
}

data class GameSessionSlotInfo(
    val state: GameSessionState,
    val savedAtMillis: Long?
)

private suspend fun DataStore<GameSessionProto>.updateWithBackup(
    file: File,
    transform: (GameSessionProto) -> GameSessionProto
) {
    val snapshot = updateData { transform(it) }
    copyToBackup(file, snapshot)
}

private fun copyToBackup(target: File, snapshot: GameSessionProto) {
    runCatching {
        val parent = target.parentFile ?: return
        if (!parent.exists()) parent.mkdirs()
        val stamp = System.currentTimeMillis()
        val backup = File(parent, "${target.name}.$stamp.bak")
        backup.outputStream().use { out ->
            snapshot.writeTo(out)
        }
        pruneBackups(target, parent)
    }
}

private fun pruneBackups(target: File, parent: File) {
    val prefix = "${target.name}."
    val backups = parent.listFiles { file ->
        file.name.startsWith(prefix) && file.name.endsWith(".bak")
    }?.sortedByDescending { it.lastModified() } ?: return
    backups.drop(MAX_BACKUPS).forEach { it.delete() }
}

@Throws(IOException::class)
private fun writeProto(target: File, proto: GameSessionProto) {
    val parent = target.parentFile
    if (parent != null && !parent.exists()) {
        parent.mkdirs()
    }
    val temp = File.createTempFile("${target.name}.tmp", null, parent)
    runCatching {
        temp.outputStream().use { output ->
            proto.writeTo(output)
        }
        if (!temp.renameTo(target)) {
            target.delete()
            if (!temp.renameTo(target)) {
                throw IOException("Unable to rename ${temp.absolutePath} to ${target.absolutePath}")
            }
        }
    }.onFailure {
        temp.delete()
        throw it
    }
}

private fun GameSessionProto.toState(): GameSessionState = GameSessionState(
    worldId = worldId.orEmpty().ifBlank { null },
    hubId = hubId.orEmpty().ifBlank { null },
    roomId = roomId.orEmpty().ifBlank { null },
    playerId = playerId.orEmpty().ifBlank { null },
    playerLevel = playerLevel,
    playerXp = playerXp,
    playerAp = playerAp,
    playerCredits = playerCredits,
    partyMemberXp = partyMemberXpMap,
    partyMemberLevels = partyMemberLevelsMap,
    partyMemberHp = partyMemberHpMap,
    trackedQuestId = trackedQuestId.orEmpty().ifBlank { null },
    activeQuests = activeQuestsList.toSet(),
    completedQuests = completedQuestsList.toSet(),
    failedQuests = failedQuestsList.toSet(),
    completedMilestones = completedMilestonesList.toSet(),
    milestoneHistory = milestoneHistoryList,
    learnedSchematics = learnedSchematicsList.toSet(),
    unlockedSkills = unlockedSkillsList.toSet(),
    unlockedWeapons = unlockedWeaponsList.toSet(),
    unlockedArmors = unlockedArmorsList.toSet(),
    partyMembers = partyMembersList,
    inventory = inventoryList.associate { entry -> entry.itemId to entry.quantity },
    equippedItems = equippedItemsMap,
    equippedWeapons = equippedWeaponsMap,
    equippedArmors = equippedArmorsMap,
    tutorialSeen = tutorialSeenList.toSet(),
    tutorialCompleted = tutorialCompletedList.toSet(),
    tutorialRoomsSeen = tutorialRoomsSeenList.toSet(),
    questStageById = questStageMap,
    questTasksCompleted = questTasksMap.mapValues { (_, listProto) ->
        listProto.completedTaskIdsList.toSet()
    },
    completedEvents = completedEventsList.toSet(),
    unlockedAreas = unlockedAreasList.toSet(),
    unlockedExits = unlockedExitsList.toSet()
)

private fun GameSessionState.toProto(savedAt: Long = System.currentTimeMillis()): GameSessionProto = GameSessionProto.newBuilder().apply {
    worldId = this@toProto.worldId.orEmpty()
    hubId = this@toProto.hubId.orEmpty()
    roomId = this@toProto.roomId.orEmpty()
    playerId = this@toProto.playerId.orEmpty()
    playerLevel = this@toProto.playerLevel
    playerXp = this@toProto.playerXp
    playerAp = this@toProto.playerAp
    playerCredits = this@toProto.playerCredits
    putAllPartyMemberXp(this@toProto.partyMemberXp)
    putAllPartyMemberLevels(this@toProto.partyMemberLevels)
    putAllPartyMemberHp(this@toProto.partyMemberHp)
    trackedQuestId = this@toProto.trackedQuestId.orEmpty()
    addAllActiveQuests(this@toProto.activeQuests)
    addAllCompletedQuests(this@toProto.completedQuests)
    clearFailedQuests()
    addAllFailedQuests(this@toProto.failedQuests)
    addAllCompletedMilestones(this@toProto.completedMilestones)
    clearMilestoneHistory()
    addAllMilestoneHistory(this@toProto.milestoneHistory)
    addAllLearnedSchematics(this@toProto.learnedSchematics)
    addAllUnlockedSkills(this@toProto.unlockedSkills)
    addAllUnlockedWeapons(this@toProto.unlockedWeapons)
    addAllUnlockedArmors(this@toProto.unlockedArmors)
    addAllPartyMembers(this@toProto.partyMembers)
    clearInventory()
    this@toProto.inventory.forEach { (itemId, quantity) ->
        addInventory(
            InventoryEntryProto.newBuilder()
                .setItemId(itemId)
                .setQuantity(quantity)
                .build()
        )
    }
    putAllEquippedItems(this@toProto.equippedItems)
    putAllEquippedWeapons(this@toProto.equippedWeapons)
    putAllEquippedArmors(this@toProto.equippedArmors)
    clearTutorialSeen()
    addAllTutorialSeen(this@toProto.tutorialSeen)
    clearTutorialCompleted()
    addAllTutorialCompleted(this@toProto.tutorialCompleted)
    clearTutorialRoomsSeen()
    addAllTutorialRoomsSeen(this@toProto.tutorialRoomsSeen)
    clearCompletedEvents()
    addAllCompletedEvents(this@toProto.completedEvents)
    clearUnlockedAreas()
    addAllUnlockedAreas(this@toProto.unlockedAreas)
    clearUnlockedExits()
    addAllUnlockedExits(this@toProto.unlockedExits)
    putAllQuestStage(this@toProto.questStageById)
    // Quest tasks use a map so we only persist non-empty sets.
    clearQuestTasks()
    this@toProto.questTasksCompleted.forEach { (questId, tasks) ->
        if (tasks.isNotEmpty()) {
            putQuestTasks(
                questId,
                QuestTaskListProto.newBuilder()
                    .addAllCompletedTaskIds(tasks)
                    .build()
            )
        }
    }
    lastSavedMs = savedAt
}.build()

fun GameSessionPersistence.importLegacySave(file: File, itemCatalog: ItemCatalog): GameSessionState? {
    if (!file.exists()) return null
    val root = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return null
    val gameState = root.optJSONObject("game_state")
    if (gameState == null) {
        return importSimpleSave(root, itemCatalog)
    }
    val characters = root.optJSONObject("characters") ?: JSONObject()

    fun resolveItemId(rawId: String): String {
        val normalizedKey = rawId.trim()
        return itemCatalog.findItem(normalizedKey)?.id
            ?: itemCatalog.findItem(normalizedKey.lowercase(Locale.getDefault()))?.id
            ?: normalizedKey
    }

    fun isWeaponItem(itemId: String): Boolean {
        val item = itemCatalog.findItem(itemId) ?: return false
        val normalizedType = item.type.trim().lowercase(Locale.getDefault())
        return normalizedType == "weapon" ||
            GearRules.isWeaponType(normalizedType) ||
            item.equipment?.slot?.equals("weapon", ignoreCase = true) == true
    }

    fun weaponOwnerFor(itemId: String): String? {
        val item = itemCatalog.findItem(itemId) ?: return null
        val weaponType = item.equipment?.weaponType?.trim()?.lowercase(Locale.getDefault())
        val fallbackType = item.type.trim().lowercase(Locale.getDefault())
        return GearRules.characterForWeaponType(weaponType ?: fallbackType)
    }

    val partyMembers = mutableListOf<String>()
    val partyArray = gameState.optJSONArray("party")
    if (partyArray != null) {
        for (i in 0 until partyArray.length()) {
            val id = partyArray.optString(i)
            if (id.isNotBlank()) partyMembers += id
        }
    }
    val playerId = partyMembers.firstOrNull()

    val inventoryJson = gameState.optJSONObject("inventory")
    val inventory = mutableMapOf<String, Int>()
    val unlockedWeapons = mutableSetOf<String>()
    val equippedWeapons = mutableMapOf<String, String>()
    inventoryJson?.keys()?.forEachRemaining { key ->
        val quantity = inventoryJson.optInt(key, 0)
        if (quantity > 0) {
            val itemId = resolveItemId(key)
            if (isWeaponItem(itemId)) {
                unlockedWeapons += itemId
            } else {
                val existing = inventory[itemId] ?: 0
                inventory[itemId] = existing + quantity
            }
        }
    }

    val partyLevels = mutableMapOf<String, Int>()
    val partyXp = mutableMapOf<String, Int>()
    val partyHp = mutableMapOf<String, Int>()
    val baseEquipment = mutableMapOf<String, String>()
    val scopedEquipment = mutableMapOf<String, String>()
    fun registerWeaponEquip(characterId: String?, weaponId: String) {
        unlockedWeapons += weaponId
        val ownerId = characterId?.trim()?.lowercase(Locale.getDefault()) ?: return
        if (!equippedWeapons.containsKey(ownerId)) {
            equippedWeapons[ownerId] = weaponId
        }
    }
    characters.keys().forEachRemaining { id ->
        val entry = characters.optJSONObject(id) ?: return@forEachRemaining
        partyLevels[id] = entry.optInt("level", 1)
        partyXp[id] = entry.optInt("xp", 0)
        partyHp[id] = entry.optInt("hp", 0)
        val equipment = entry.optJSONObject("equipment")
        equipment?.keys()?.forEachRemaining { slot ->
            val rawItemId = equipment.optString(slot)?.trim()
            val itemId = rawItemId?.takeIf { it.isNotBlank() }?.let { resolveItemId(it) } ?: return@forEachRemaining
            val normalizedSlot = slot.lowercase(Locale.getDefault())
            if (normalizedSlot == "weapon" || isWeaponItem(itemId)) {
                val ownerId = weaponOwnerFor(itemId) ?: id
                registerWeaponEquip(ownerId, itemId)
            } else {
                val scopedKey = "${id.lowercase(Locale.getDefault())}:$normalizedSlot"
                scopedEquipment[scopedKey] = itemId
                if (!inventory.containsKey(itemId)) {
                    inventory[itemId] = 1
                }
                if (playerId != null && playerId.equals(id, ignoreCase = true)) {
                    baseEquipment.putIfAbsent(normalizedSlot, itemId)
                }
            }
        }
    }

    val activeQuests = mutableSetOf<String>()
    val completedQuests = mutableSetOf<String>()
    val failedQuests = mutableSetOf<String>()
    val quests = gameState.optJSONObject("quests")
    var trackedQuestId: String? = quests?.optString("tracked")?.takeIf { it.isNotBlank() }
    val questArray = quests?.optJSONArray("quests")
    if (questArray != null) {
        for (i in 0 until questArray.length()) {
            val entry = questArray.optJSONObject(i) ?: continue
            val id = entry.optString("id").takeIf { it.isNotBlank() } ?: continue
            when (entry.optString("status").lowercase(Locale.getDefault())) {
                "complete", "completed", "done" -> completedQuests += id
                "active", "in_progress", "ongoing" -> activeQuests += id
                "failed", "fail" -> failedQuests += id
            }
        }
    }

    val completedMilestones = mutableSetOf<String>()
    val milestonesArray = gameState.optJSONArray("completed_milestones")
        ?: gameState.optJSONArray("milestones")
    if (milestonesArray != null) {
        for (i in 0 until milestonesArray.length()) {
            milestonesArray.optString(i)?.takeIf { it.isNotBlank() }?.let { completedMilestones += it }
        }
    }
    val milestonesObject = gameState.optJSONObject("milestones")
    milestonesObject?.keys()?.forEachRemaining { key ->
        val raw = milestonesObject.opt(key)
        val isComplete = when (raw) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> raw.equals("true", ignoreCase = true) || raw == "1"
            else -> false
        }
        if (isComplete && key.isNotBlank()) {
            completedMilestones += key
        }
    }

    val learnedSchematics = mutableSetOf<String>()
    val schematicsArray = gameState.optJSONArray("learned_schematics")
    if (schematicsArray != null) {
        for (i in 0 until schematicsArray.length()) {
            schematicsArray.optString(i)?.takeIf { it.isNotBlank() }?.let { learnedSchematics += it }
        }
    }

    val mapState = gameState.optJSONObject("map")
    val worldId = mapState?.optString("current_world_id")?.takeIf { it.isNotBlank() }
    val hubId = mapState?.optString("current_hub_id")?.takeIf { it.isNotBlank() }
    val roomId = mapState?.optString("current_room_id")?.takeIf { it.isNotBlank() }

    val credits = gameState.optInt("credits", 0)
    val playerLevel = playerId?.let { partyLevels[it] } ?: partyLevels.values.firstOrNull() ?: 1
    val playerXp = playerId?.let { partyXp[it] } ?: partyXp.values.firstOrNull() ?: 0

    val equipmentJson = gameState.optJSONObject("equipment")
    equipmentJson?.keys()?.forEachRemaining { slot ->
        val itemId = equipmentJson.optString(slot)?.trim()
        if (!itemId.isNullOrBlank()) {
            val resolvedId = resolveItemId(itemId)
            val normalizedSlot = slot.lowercase(Locale.getDefault())
            if (normalizedSlot == "weapon" || isWeaponItem(resolvedId)) {
                val ownerId = weaponOwnerFor(resolvedId) ?: playerId
                registerWeaponEquip(ownerId, resolvedId)
            } else {
                baseEquipment.putIfAbsent(normalizedSlot, resolvedId)
                if (!inventory.containsKey(resolvedId)) {
                    inventory[resolvedId] = 1
                }
            }
        }
    }

    if (baseEquipment.isNotEmpty() && partyMembers.isNotEmpty()) {
        partyMembers.mapNotNull { it.takeIf { id -> id.isNotBlank() } }
            .forEach { memberId ->
                val normalizedId = memberId.lowercase(Locale.getDefault())
                baseEquipment.forEach { (slot, itemId) ->
                    val scopedKey = "$normalizedId:$slot"
                    scopedEquipment.putIfAbsent(scopedKey, itemId)
                }
            }
    }
    val equippedItems = (baseEquipment + scopedEquipment).toMutableMap()

    return GameSessionState(
        worldId = worldId,
        hubId = hubId,
        roomId = roomId,
        playerId = playerId,
        playerLevel = playerLevel,
        playerXp = playerXp,
        playerCredits = credits,
        partyMembers = partyMembers,
        partyMemberLevels = partyLevels,
        partyMemberXp = partyXp,
        partyMemberHp = partyHp,
        inventory = inventory.filterValues { it > 0 },
        equippedItems = equippedItems,
        unlockedWeapons = unlockedWeapons,
        equippedWeapons = equippedWeapons,
        trackedQuestId = trackedQuestId,
        activeQuests = activeQuests,
        completedQuests = completedQuests,
        failedQuests = failedQuests,
        completedMilestones = completedMilestones,
        milestoneHistory = completedMilestones.toList(),
        learnedSchematics = learnedSchematics
    )
}

private fun importSimpleSave(root: JSONObject, itemCatalog: ItemCatalog): GameSessionState {
    fun resolveItemId(rawId: String): String {
        val normalizedKey = rawId.trim()
        return itemCatalog.findItem(normalizedKey)?.id
            ?: itemCatalog.findItem(normalizedKey.lowercase(Locale.getDefault()))?.id
            ?: normalizedKey
    }

    fun isWeaponItem(itemId: String): Boolean {
        val item = itemCatalog.findItem(itemId) ?: return false
        val normalizedType = item.type.trim().lowercase(Locale.getDefault())
        return normalizedType == "weapon" ||
            GearRules.isWeaponType(normalizedType) ||
            item.equipment?.slot?.equals("weapon", ignoreCase = true) == true
    }

    fun weaponOwnerFor(itemId: String): String? {
        val item = itemCatalog.findItem(itemId) ?: return null
        val weaponType = item.equipment?.weaponType?.trim()?.lowercase(Locale.getDefault())
        val fallbackType = item.type.trim().lowercase(Locale.getDefault())
        return GearRules.characterForWeaponType(weaponType ?: fallbackType)
    }

    val inventoryJson = root.optJSONObject("inventory")
    val inventory = mutableMapOf<String, Int>()
    val unlockedWeapons = mutableSetOf<String>()
    val equippedWeapons = mutableMapOf<String, String>()
    inventoryJson?.keys()?.forEachRemaining { key ->
        val quantity = inventoryJson.optInt(key, 0)
        if (quantity > 0) {
            val itemId = resolveItemId(key)
            if (isWeaponItem(itemId)) {
                unlockedWeapons += itemId
            } else {
                val existing = inventory[itemId] ?: 0
                inventory[itemId] = existing + quantity
            }
        }
    }
    val equipmentJson = root.optJSONObject("equipment")
    val baseEquipment = mutableMapOf<String, String>()
    fun registerWeaponEquip(characterId: String?, weaponId: String) {
        unlockedWeapons += weaponId
        val ownerId = characterId?.trim()?.lowercase(Locale.getDefault()) ?: return
        if (!equippedWeapons.containsKey(ownerId)) {
            equippedWeapons[ownerId] = weaponId
        }
    }
    equipmentJson?.keys()?.forEachRemaining { slot ->
        val id = equipmentJson.optString(slot)?.trim()
        if (!id.isNullOrBlank()) {
            val normalizedSlot = slot.lowercase(Locale.getDefault())
            val resolvedId = resolveItemId(id)
            if (normalizedSlot == "weapon" || isWeaponItem(resolvedId)) {
                val ownerId = weaponOwnerFor(resolvedId)
                registerWeaponEquip(ownerId, resolvedId)
            } else {
                baseEquipment[normalizedSlot] = resolvedId
                if (!inventory.containsKey(resolvedId)) {
                    inventory[resolvedId] = 1
                }
            }
        }
    }
    val partyMembers = mutableListOf<String>()
    val partyArray = root.optJSONArray("party")
    if (partyArray != null) {
        for (i in 0 until partyArray.length()) {
            val id = partyArray.optString(i)
            if (id.isNotBlank()) partyMembers += id
        }
    }
    val scopedEquipment = mutableMapOf<String, String>()
    if (baseEquipment.isNotEmpty() && partyMembers.isNotEmpty()) {
        partyMembers.mapNotNull { it.takeIf { id -> id.isNotBlank() } }
            .forEach { memberId ->
                val normalizedId = memberId.lowercase(Locale.getDefault())
                baseEquipment.forEach { (slot, itemId) ->
                    scopedEquipment["$normalizedId:$slot"] = itemId
                }
            }
    }
    val equipped = (baseEquipment + scopedEquipment).toMutableMap()
    val playerId = partyMembers.firstOrNull()
    return GameSessionState(
        playerId = playerId,
        partyMembers = partyMembers,
        playerCredits = root.optInt("credits", 0),
        inventory = inventory,
        equippedItems = equipped,
        unlockedWeapons = unlockedWeapons,
        equippedWeapons = equippedWeapons,
        playerLevel = 1,
        playerXp = 0
    )
}
