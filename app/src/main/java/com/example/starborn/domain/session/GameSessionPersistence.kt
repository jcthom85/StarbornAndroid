package com.example.starborn.domain.session

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.example.starborn.datastore.GameSessionProto
import com.example.starborn.datastore.EnemyPartyStateProto
import com.example.starborn.datastore.InventoryEntryProto
import com.example.starborn.datastore.QuestTaskListProto
import com.example.starborn.datastore.RoomStateProto
import com.example.starborn.datastore.ArcadeProgressProto
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.movement.EnemyPartyRuntimeState
import java.io.File
import java.util.Locale
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import androidx.datastore.core.CorruptionException
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

class GameSessionPersistence(
    private val fileForName: (String) -> File
) {

    constructor(baseDir: File) : this({ name -> File(baseDir, name) })

    private val dataStoreFile = fileForName(DATASTORE_FILE)
    private val autosaveFile = fileForName(AUTOSAVE_FILE)
    private val quickSaveFile = fileForName(QUICKSAVE_FILE)

    private val dataStore: DataStore<GameSessionProto> = dataStoreFor(dataStoreFile)
    private val autosaveStore: DataStore<GameSessionProto> = dataStoreFor(autosaveFile)
    private val quickSaveStore: DataStore<GameSessionProto> = dataStoreFor(quickSaveFile)

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
        backupsFor(quickSaveFile).forEach { it.delete() }
    }

    suspend fun clearAutosave() {
        autosaveStore.updateData { GameSessionProto.getDefaultInstance() }
        backupsFor(autosaveFile).forEach { it.delete() }
    }

    private fun slotStore(slot: Int): SlotStore = slotStoreFor(fileForName("game_session_slot$slot.pb"))

    companion object {
        private const val DATASTORE_FILE = "game_session.pb"
        private const val AUTOSAVE_FILE = "game_session_autosave.pb"
        private const val QUICKSAVE_FILE = "game_session_quicksave.pb"

        private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val sharedStores = java.util.concurrent.ConcurrentHashMap<String, DataStore<GameSessionProto>>()
        private val sharedSlotStores = java.util.concurrent.ConcurrentHashMap<String, SlotStore>()

        private fun dataStoreFor(file: File): DataStore<GameSessionProto> {
            val key = file.absolutePath
            return sharedStores.getOrPut(key) {
                DataStoreFactory.create(
                    serializer = GameSessionSerializer,
                    corruptionHandler = ReplaceFileCorruptionHandler {
                        newestValidBackup(file) ?: run {
                            // Keep the damaged bytes for recovery while allowing a new game
                            // or an explicit clear to write this DataStore again.
                            val retained = File.createTempFile("${file.name}.", ".corrupt", file.parentFile)
                            file.copyTo(retained, overwrite = true)
                            GameSessionProto.getDefaultInstance()
                        }
                    },
                    produceFile = { file },
                    scope = storeScope
                )
            }
        }

        private fun slotStoreFor(file: File): SlotStore {
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
            try {
                file.inputStream().use { input ->
                    val proto = GameSessionSerializer.readFrom(input)
                    if (proto == GameSessionProto.getDefaultInstance()) null
                    else GameSessionSlotInfo(proto.toState(), proto.lastSavedMs.takeIf { it > 0 })
                }
            } catch (error: CorruptionException) {
                val recovered = newestValidBackup(file) ?: throw error
                writeProto(file, recovered)
                GameSessionSlotInfo(recovered.toState(), recovered.lastSavedMs.takeIf { it > 0 })
            }
        }

    suspend fun clear() {
        mutex.withLock {
            if (file.exists()) {
                file.delete()
            }
            backupsFor(file).forEach { it.delete() }
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
        val backup = File.createTempFile("${target.name}.${System.currentTimeMillis()}.", ".bak", parent)
        backup.outputStream().use { out ->
            snapshot.writeTo(out)
        }
        pruneBackups(target)
    }
}

private fun pruneBackups(target: File) {
    backupsFor(target).drop(MAX_BACKUPS).forEach { it.delete() }
}

private fun backupsFor(target: File): List<File> =
    target.parentFile?.listFiles { file ->
        file.name.startsWith("${target.name}.") && file.name.endsWith(".bak")
    }?.sortedByDescending { it.lastModified() }.orEmpty()

private fun newestValidBackup(target: File): GameSessionProto? {
    for (backup in backupsFor(target)) {
        val proto = try {
            backup.inputStream().use { GameSessionProto.parseFrom(it) }
        } catch (_: IOException) {
            continue
        }
        if (proto != GameSessionProto.getDefaultInstance()) return proto
    }
    return null
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
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
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
    unlockedExits = unlockedExitsList.toSet(),
    revealedNodes = revealedNodesList.toSet(),
    unlockedNodes = unlockedNodesList.toSet(),
    visitedNodes = visitedNodesList.toSet(),
    completedNodes = completedNodesList.toSet(),
    astraReturnWorldId = astraReturnWorldId.takeIf { it.isNotBlank() },
    astraReturnHubId = astraReturnHubId.takeIf { it.isNotBlank() },
    astraReturnRoomId = astraReturnRoomId.takeIf { it.isNotBlank() },
    arcadeProgress = arcadeProgressMap.mapValues { (_, progress) ->
        ArcadeCabinetProgress(
            discovered = progress.discovered,
            repaired = progress.repaired,
            installed = progress.installed,
            highScore = progress.highScore.coerceAtLeast(0),
            claimedTiers = progress.claimedTiersList.filter { it.isNotBlank() }.toSet(),
            playCount = progress.playCount.coerceAtLeast(0)
        )
    },
    roomStates = roomStatesMap
        .filterKeys { it.isNotBlank() }
        .mapValues { (_, stateProto) ->
            stateProto.statesMap.filterKeys { it.isNotBlank() }
        },
    enemyPartyStates = enemyPartyStatesMap.mapValues { (_, state) ->
        EnemyPartyRuntimeState(
            roomId = state.roomId,
            routeIndex = state.routeIndex,
            routeDirection = state.routeDirection,
            moveRemainingMs = state.moveRemainingMs,
            aggressionRemainingMs = state.aggressionRemainingMs.takeIf { state.hasAggressionTimer },
            retreatGraceRemainingMs = state.retreatGraceRemainingMs,
            defeated = state.defeated
        )
    },
    activeMealBuff = if (hasActiveMealBuff() && activeMealBuff.recipeId.isNotBlank()) {
        ActiveMealBuff(
            recipeId = activeMealBuff.recipeId,
            recipeName = activeMealBuff.recipeName,
            chefId = activeMealBuff.chefId.takeIf { it.isNotBlank() },
            remainingEncounters = activeMealBuff.remainingEncounters.coerceAtLeast(1),
            hpBonus = activeMealBuff.hpBonus,
            speedBonus = activeMealBuff.speedBonus,
            focusBonus = activeMealBuff.focusBonus,
            critBonus = activeMealBuff.critBonus,
            stabilityBonus = activeMealBuff.stabilityBonus,
            statusResistBonus = activeMealBuff.statusResistBonus
        )
    } else null
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
    clearRevealedNodes()
    addAllRevealedNodes(this@toProto.revealedNodes)
    clearUnlockedNodes()
    addAllUnlockedNodes(this@toProto.unlockedNodes)
    clearVisitedNodes()
    addAllVisitedNodes(this@toProto.visitedNodes)
    clearCompletedNodes()
    addAllCompletedNodes(this@toProto.completedNodes)
    astraReturnWorldId = this@toProto.astraReturnWorldId.orEmpty()
    astraReturnHubId = this@toProto.astraReturnHubId.orEmpty()
    astraReturnRoomId = this@toProto.astraReturnRoomId.orEmpty()
    clearArcadeProgress()
    this@toProto.arcadeProgress.forEach { (cabinetId, progress) ->
        if (cabinetId.isNotBlank()) {
            putArcadeProgress(
                cabinetId,
                ArcadeProgressProto.newBuilder()
                    .setDiscovered(progress.discovered)
                    .setRepaired(progress.repaired)
                    .setInstalled(progress.installed)
                    .setHighScore(progress.highScore.coerceAtLeast(0))
                    .addAllClaimedTiers(progress.claimedTiers.filter { it.isNotBlank() })
                    .setPlayCount(progress.playCount.coerceAtLeast(0))
                    .build()
            )
        }
    }
    clearRoomStates()
    this@toProto.roomStates.forEach { (roomId, states) ->
        val normalizedRoom = roomId.trim()
        if (normalizedRoom.isBlank()) return@forEach
        val filteredStates = states.filterKeys { it.isNotBlank() }
        if (filteredStates.isNotEmpty()) {
            putRoomStates(
                normalizedRoom,
                RoomStateProto.newBuilder()
                    .putAllStates(filteredStates)
                    .build()
            )
        }
    }
    clearEnemyPartyStates()
    this@toProto.enemyPartyStates.forEach { (partyId, state) ->
        if (partyId.isBlank()) return@forEach
        putEnemyPartyStates(
            partyId,
            EnemyPartyStateProto.newBuilder()
                .setRoomId(state.roomId)
                .setRouteIndex(state.routeIndex)
                .setRouteDirection(state.routeDirection)
                .setMoveRemainingMs(state.moveRemainingMs)
                .setAggressionRemainingMs(state.aggressionRemainingMs ?: 0)
                .setHasAggressionTimer(state.aggressionRemainingMs != null)
                .setRetreatGraceRemainingMs(state.retreatGraceRemainingMs)
                .setDefeated(state.defeated)
                .build()
        )
    }
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
    this@toProto.activeMealBuff?.let { buff ->
        activeMealBuff = com.example.starborn.datastore.ActiveMealBuffProto.newBuilder().apply {
            recipeId = buff.recipeId
            recipeName = buff.recipeName
            chefId = buff.chefId.orEmpty()
            remainingEncounters = buff.remainingEncounters
            hpBonus = buff.hpBonus
            speedBonus = buff.speedBonus
            focusBonus = buff.focusBonus
            critBonus = buff.critBonus
            stabilityBonus = buff.stabilityBonus
            statusResistBonus = buff.statusResistBonus
        }.build()
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
    val questStageById = mutableMapOf<String, String>()
    val questTasksCompleted = mutableMapOf<String, Set<String>>()
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
            val stage = entry.optString("stage").takeIf { it.isNotBlank() }
                ?: entry.optString("current_stage").takeIf { it.isNotBlank() }
            if (stage != null) {
                questStageById[id] = stage
            }
            val tasksArray = entry.optJSONArray("completed_tasks") ?: entry.optJSONArray("tasks")
            if (tasksArray != null) {
                val tasks = mutableSetOf<String>()
                for (j in 0 until tasksArray.length()) {
                    tasksArray.optString(j)?.takeIf { it.isNotBlank() }?.let { tasks += it }
                }
                if (tasks.isNotEmpty()) {
                    questTasksCompleted[id] = tasks
                }
            }
        }
    }
    val questStagesObj = gameState.optJSONObject("quest_stages") ?: quests?.optJSONObject("stages")
    questStagesObj?.keys()?.forEachRemaining { qId ->
        val stage = questStagesObj.optString(qId)
        if (stage.isNotBlank()) questStageById[qId] = stage
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

    val unlockedSkills = mutableSetOf<String>()
    characters.keys().forEachRemaining { id ->
        val entry = characters.optJSONObject(id) ?: return@forEachRemaining
        val skills = entry.optJSONArray("unlocked_abilities") ?: entry.optJSONArray("unlocked_skills")
        if (skills != null) {
            for (i in 0 until skills.length()) {
                skills.optString(i)?.takeIf { it.isNotBlank() }?.let { unlockedSkills += it }
            }
        }
    }
    val rootSkills = gameState.optJSONArray("unlocked_skills") ?: root.optJSONArray("unlocked_skills")
    if (rootSkills != null) {
        for (i in 0 until rootSkills.length()) {
            rootSkills.optString(i)?.takeIf { it.isNotBlank() }?.let { unlockedSkills += it }
        }
    }

    val unlockedExits = mutableSetOf<String>()
    val exitsArray = gameState.optJSONArray("unlocked_exits") ?: root.optJSONArray("unlocked_exits")
    if (exitsArray != null) {
        for (i in 0 until exitsArray.length()) {
            exitsArray.optString(i)?.takeIf { it.isNotBlank() }?.let { unlockedExits += it }
        }
    }
    val exitsObj = gameState.optJSONObject("unlocked_exits") ?: root.optJSONObject("unlocked_exits")
    exitsObj?.keys()?.forEachRemaining { key ->
        if (exitsObj.optBoolean(key, false) && key.isNotBlank()) {
            unlockedExits += key
        }
    }

    val roomStates = mutableMapOf<String, Map<String, Boolean>>()
    val roomStatesJson = gameState.optJSONObject("room_states") ?: root.optJSONObject("room_states")
    roomStatesJson?.keys()?.forEachRemaining { rId ->
        val stateObj = roomStatesJson.optJSONObject(rId)
        if (stateObj != null) {
            val flags = mutableMapOf<String, Boolean>()
            stateObj.keys().forEachRemaining { flagKey ->
                flags[flagKey] = stateObj.optBoolean(flagKey, false)
            }
            roomStates[rId] = flags
        }
    }

    val completedEvents = mutableSetOf<String>()
    val firedEventsArray = gameState.optJSONArray("fired_events") ?: gameState.optJSONArray("completed_events")
    if (firedEventsArray != null) {
        for (i in 0 until firedEventsArray.length()) {
            firedEventsArray.optString(i)?.takeIf { it.isNotBlank() }?.let { completedEvents += it }
        }
    }

    val tutorialSeen = mutableSetOf<String>()
    val tutorialCompleted = mutableSetOf<String>()
    val tutorialRoomsSeen = mutableSetOf<String>()
    val tutObj = root.optJSONObject("tutorials") ?: gameState.optJSONObject("tutorials")
    if (tutObj != null) {
        val seenArr = tutObj.optJSONArray("seen")
        if (seenArr != null) {
            for (i in 0 until seenArr.length()) {
                seenArr.optString(i)?.takeIf { it.isNotBlank() }?.let { tutorialSeen += it }
            }
        }
        val compArr = tutObj.optJSONArray("completed")
        if (compArr != null) {
            for (i in 0 until compArr.length()) {
                compArr.optString(i)?.takeIf { it.isNotBlank() }?.let { tutorialCompleted += it }
            }
        }
        val roomsArr = tutObj.optJSONArray("rooms_seen")
        if (roomsArr != null) {
            for (i in 0 until roomsArr.length()) {
                roomsArr.optString(i)?.takeIf { it.isNotBlank() }?.let { tutorialRoomsSeen += it }
            }
        }
    }

    val mapState = gameState.optJSONObject("map")
    val worldId = mapState?.optString("current_world_id")?.takeIf { it.isNotBlank() }
    val hubId = mapState?.optString("current_hub_id")?.takeIf { it.isNotBlank() }
    val roomId = mapState?.optString("current_room_id")?.takeIf { it.isNotBlank() }

    val unlockedAreas = mutableSetOf<String>()
    val unlockedNodes = mutableSetOf<String>()
    val routesObj = gameState.optJSONObject("routes")
    if (routesObj != null) {
        val worldsObj = routesObj.optJSONObject("worlds")
        worldsObj?.keys()?.forEachRemaining { if (worldsObj.optBoolean(it, false)) unlockedAreas += it }
        val hubsObj = routesObj.optJSONObject("hubs")
        hubsObj?.keys()?.forEachRemaining { if (hubsObj.optBoolean(it, false)) unlockedAreas += it }
        val nodesObj = routesObj.optJSONObject("nodes")
        nodesObj?.keys()?.forEachRemaining { if (nodesObj.optBoolean(it, false)) unlockedNodes += it }
    }
    val revealedNodes = mutableSetOf<String>()
    val visitedNodes = mutableSetOf<String>()
    val nodeMaps = mapState?.optJSONObject("node_maps")
    nodeMaps?.keys()?.forEachRemaining { nId ->
        revealedNodes += nId
        visitedNodes += nId
    }
    val currentNodeId = mapState?.optString("current_node_id")?.takeIf { it.isNotBlank() }
    if (currentNodeId != null) {
        revealedNodes += currentNodeId
        visitedNodes += currentNodeId
    }

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
        unlockedSkills = unlockedSkills,
        unlockedAreas = unlockedAreas,
        unlockedExits = unlockedExits,
        tutorialSeen = tutorialSeen,
        tutorialCompleted = tutorialCompleted,
        tutorialRoomsSeen = tutorialRoomsSeen,
        questStageById = questStageById,
        questTasksCompleted = questTasksCompleted,
        completedEvents = completedEvents,
        roomStates = roomStates,
        revealedNodes = revealedNodes,
        unlockedNodes = unlockedNodes,
        visitedNodes = visitedNodes,
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
    val unlockedExits = mutableSetOf<String>()
    val exitsArray = root.optJSONArray("unlocked_exits")
    if (exitsArray != null) {
        for (i in 0 until exitsArray.length()) {
            exitsArray.optString(i)?.takeIf { it.isNotBlank() }?.let { unlockedExits += it }
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
        unlockedExits = unlockedExits,
        playerLevel = 1,
        playerXp = 0
    )
}
