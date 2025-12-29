package com.example.starborn.di

import android.content.Context
import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.CinematicAssetDataSource
import com.example.starborn.data.assets.CraftingAssetDataSource
import com.example.starborn.data.assets.DialogueAssetDataSource
import com.example.starborn.data.assets.EventAssetDataSource
import com.example.starborn.data.assets.FishingAssetDataSource
import com.example.starborn.data.assets.ItemAssetDataSource
import com.example.starborn.data.assets.MilestoneAssetDataSource
import com.example.starborn.data.assets.ThemeAssetDataSource
import com.example.starborn.data.assets.ThemeStyleAssetDataSource
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.ItemRepository
import com.example.starborn.data.assets.QuestAssetDataSource
import com.example.starborn.data.assets.ShopAssetDataSource
import com.example.starborn.data.repository.MilestoneRepository
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.data.repository.ShopRepository
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.audio.AudioCatalog
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.audio.VoiceoverController
import com.example.starborn.domain.cinematic.CinematicCoordinator
import com.example.starborn.domain.cinematic.CinematicPlaybackState
import com.example.starborn.domain.cinematic.CinematicService
import android.util.Log
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.EncounterCoordinator
import com.example.starborn.domain.combat.StatusRegistry
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueConditionEvaluator
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerParser
import com.example.starborn.domain.dialogue.DialogueTriggerHandler
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.MilestoneEffects
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionSlotInfo
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.fingerprint
import com.example.starborn.domain.session.importLegacySave
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.leveling.LevelingData
import com.example.starborn.domain.leveling.ProgressionData
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.fx.UiFxBus
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.milestone.MilestoneRuntimeManager
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.domain.tutorial.TutorialScriptRepository
import com.example.starborn.data.local.UserSettingsStore
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.ui.events.UiEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.Locale
import java.io.File
import kotlin.random.Random

class AppServices(context: Context) {
    private val appContext = context.applicationContext
    private val moshi = MoshiProvider.instance
    private val assetReader = AssetJsonReader(context, moshi)

    val worldDataSource = WorldAssetDataSource(assetReader)
    private val themeDataSource = ThemeAssetDataSource(assetReader)
    private val themeStyleDataSource = ThemeStyleAssetDataSource(assetReader)
    private val dialogueDataSource = DialogueAssetDataSource(assetReader)
    private val eventDataSource = EventAssetDataSource(assetReader)
    val itemRepository = ItemRepository(ItemAssetDataSource(assetReader))
    private val craftingDataSource = CraftingAssetDataSource(assetReader)
    private val cinematicDataSource = CinematicAssetDataSource(assetReader, moshi)
    private val shopDataSource = ShopAssetDataSource(assetReader)
    private val fishingDataSource = FishingAssetDataSource(assetReader)
    private val milestoneDataSource = MilestoneAssetDataSource(assetReader)
    val questRepository: QuestRepository = QuestRepository(QuestAssetDataSource(assetReader)).apply { load() }
    val shopRepository: ShopRepository = ShopRepository(shopDataSource).apply { load() }
    val milestoneRepository: MilestoneRepository = MilestoneRepository(milestoneDataSource).apply { load() }
    val themeRepository: ThemeRepository = ThemeRepository(
        themeDataSource,
        themeStyleDataSource
    ).apply { load() }
    val environmentThemeManager = EnvironmentThemeManager(themeRepository)

    val inventoryService = InventoryService(itemRepository).apply { loadItems() }
    val sessionStore = GameSessionStore()
    private val sessionPersistence = GameSessionPersistence(context)
    val craftingService = CraftingService(craftingDataSource, inventoryService, sessionStore)
    val events: List<GameEvent> = eventDataSource.loadEvents()
    val statusRegistry = StatusRegistry(worldDataSource.loadStatuses())
    val combatEngine = CombatEngine(statusRegistry = statusRegistry)
    val encounterCoordinator = EncounterCoordinator()
    val levelingManager = LevelingManager(worldDataSource.loadLevelingData() ?: LevelingData())
    val progressionData: ProgressionData = worldDataSource.loadProgressionData() ?: ProgressionData()
    val cinematicService = CinematicService(cinematicDataSource)
    val cinematicCoordinator = CinematicCoordinator(cinematicService)
    val cinematicState = cinematicCoordinator.state
    fun playCinematic(sceneId: String?, onComplete: () -> Unit = {}): Boolean =
        cinematicCoordinator.play(sceneId, onComplete)

    fun cinematicStateFlow() = cinematicCoordinator.state
    private val audioBindings: AudioBindings = assetReader.readObject<AudioBindings>("audio_bindings.json") ?: AudioBindings()
    private val audioCatalog: AudioCatalog = assetReader.readObject<AudioCatalog>("audio_catalog.json") ?: AudioCatalog()
    val audioCuePlayer = AudioCuePlayer(context)
    val audioRouter = AudioRouter(audioBindings, audioCatalog)
    val uiFxBus = UiFxBus()
    val uiEventBus = UiEventBus()
    private var dialogueTriggerListener: ((String) -> Boolean)? = null
    val fishingService = FishingService(fishingDataSource, inventoryService)
    val tutorialScripts = TutorialScriptRepository(assetReader)
    val userSettingsStore = UserSettingsStore(appContext)
    private val bootstrapCinematics: ArrayDeque<String> = ArrayDeque()
    private val bootstrapPlayerActions: ArrayDeque<String> = ArrayDeque()

    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val voiceoverController = VoiceoverController(
        audioRouter = audioRouter,
        dispatchCommands = audioCuePlayer::execute,
        scope = runtimeScope,
        dispatcher = Dispatchers.Main
    )
    private var autosaveJob: Job? = null
    private var lastAutosaveTimestamp: Long = 0L
    private var lastAutosaveFingerprint: String? = null

    companion object {
        private const val AUTOSAVE_INTERVAL_MS = 90_000L
        private const val AUTOSAVE_SLOT = 0
        private const val SAMPLE_SLOT_ID = -1
        private val SAMPLE_PARTY = setOf("nova", "zeke", "orion", "gh0st")
        private const val SAMPLE_WORLD_ID = "nova_prime"
        private const val SAMPLE_HUB_ID = "mining_colony"
        private const val SAMPLE_ROOM_ID = "market_2"
    }

    private val defaultArmorsByCharacter = mapOf(
        "nova" to "nova_flux_liner",
        "zeke" to "zeke_surge_harness",
        "orion" to "orion_channeler_mantle",
        "gh0st" to "gh0st_phaseweave_jacket",
        "ollie" to "basic_vest"
    )

    val promptManager = UIPromptManager()
    val dialogueService: DialogueService = DialogueService(
        dialogueDataSource.loadDialogue(),
        DialogueConditionEvaluator { condition ->
            isDialogueConditionMet(condition, sessionStore.state.value, inventoryService)
        },
        DialogueTriggerHandler { trigger ->
            val handled = dialogueTriggerListener?.invoke(trigger) == true
            if (!handled) {
                handleDialogueTrigger(trigger, sessionStore, questRuntimeManager, inventoryService)
            }
        }
    )
    val questRuntimeManager = QuestRuntimeManager(questRepository, sessionStore, runtimeScope, uiEventBus)
    val milestoneManager = MilestoneRuntimeManager(
        milestoneRepository,
        sessionStore,
        promptManager,
        runtimeScope,
        ::applyMilestoneEffects
    )
    val tutorialManager = TutorialRuntimeManager(sessionStore, promptManager, tutorialScripts, runtimeScope)

    init {
        inventoryService.addOnItemAddedListener { itemId, quantity ->
            if (quantity > 0) {
                handleWeaponUnlockFromItem(itemId, quantity)
                handleArmorUnlockFromItem(itemId, quantity)
            }
        }
        questRuntimeManager.addQuestCompletionListener { questId ->
            milestoneManager.onQuestCompleted(questId)
        }
        persistenceScope.launch {
            // Only hydrate from persisted session once on startup.
            // If the user starts a new game before the initial read completes, don't overwrite it.
            val stored = sessionPersistence.sessionFlow.first()
            if (sessionStore.state.value.needsFallbackImport()) {
                sessionStore.restore(stored)
                inventoryService.restore(stored.inventory)
                migrateLegacyWeapons(stored)
                migrateLegacyArmors(stored)
            }
        }
        persistenceScope.launch {
            sessionStore.state
                .drop(1)
                .distinctUntilChanged()
                .collect { state ->
                    sessionPersistence.persist(state)
                    scheduleAutosave(state)
                }
        }
        persistenceScope.launch {
            inventoryService.state.collect { entries ->
                val snapshot = entries.associate { it.item.id to it.quantity }.filterValues { it > 0 }
                if (snapshot != sessionStore.state.value.inventory) {
                    sessionStore.setInventory(snapshot)
                }
            }
        }
    }

    private fun applyMilestoneEffects(effects: MilestoneEffects) {
        effects.unlockAbilities.orEmpty().forEach { abilityId ->
            sessionStore.unlockSkill(abilityId)
        }
        effects.unlockAreas.orEmpty().forEach { areaId ->
            sessionStore.unlockArea(areaId)
        }
        effects.unlockExits.orEmpty().forEach { exit ->
            sessionStore.unlockExit(exit.roomId, exit.direction)
        }
    }

    private fun handleWeaponUnlockFromItem(itemId: String, quantity: Int) {
        if (quantity <= 0) return
        val item = itemRepository.findItem(itemId) ?: return
        if (!item.isWeaponItem()) return
        val weaponId = item.id
        sessionStore.unlockWeapon(weaponId)
        val ownerId = weaponOwnerFor(item)
        if (!ownerId.isNullOrBlank()) {
            val normalizedOwner = ownerId.lowercase(Locale.getDefault())
            val equipped = sessionStore.state.value.equippedWeapons[normalizedOwner]
            if (equipped.isNullOrBlank()) {
                sessionStore.setEquippedWeapon(ownerId, weaponId)
            }
        }
        inventoryService.removeItem(weaponId, quantity)
    }

    private fun handleArmorUnlockFromItem(itemId: String, quantity: Int) {
        if (quantity <= 0) return
        val item = itemRepository.findItem(itemId) ?: return
        if (!item.isArmorItem()) return
        sessionStore.unlockArmor(item.id)
        inventoryService.removeItem(item.id, quantity)
    }

    private fun migrateLegacyWeapons(state: GameSessionState) {
        val weaponIds = mutableSetOf<String>()
        val legacyEquipped = mutableMapOf<String, String>()

        state.inventory.keys.forEach { rawId ->
            canonicalWeaponId(rawId)?.let { weaponIds += it }
        }

        state.equippedItems.forEach { (key, value) ->
            val slot = if (key.contains(':')) key.substringAfter(':') else key
            if (!slot.equals("weapon", ignoreCase = true)) return@forEach
            val weaponId = canonicalWeaponId(value) ?: return@forEach
            weaponIds += weaponId
            val ownerId = if (key.contains(':')) key.substringBefore(':') else weaponOwnerFor(weaponId)
            if (!ownerId.isNullOrBlank()) {
                legacyEquipped[ownerId.lowercase(Locale.getDefault())] = weaponId
            }
        }

        state.equippedWeapons.forEach { (ownerId, weaponId) ->
            canonicalWeaponId(weaponId)?.let { weaponIds += it }
            if (ownerId.isNotBlank() && weaponId.isNotBlank()) {
                legacyEquipped.putIfAbsent(ownerId.lowercase(Locale.getDefault()), weaponId)
            }
        }

        weaponIds.forEach { sessionStore.unlockWeapon(it) }
        legacyEquipped.forEach { (ownerId, weaponId) ->
            val equipped = sessionStore.state.value.equippedWeapons[ownerId]
            if (equipped.isNullOrBlank()) {
                sessionStore.setEquippedWeapon(ownerId, weaponId)
            }
        }

        val inventorySnapshot = inventoryService.snapshot()
        inventorySnapshot.forEach { (id, qty) ->
            if (isWeaponItemId(id)) {
                inventoryService.removeItem(id, qty)
            }
        }
    }

    private fun migrateLegacyArmors(state: GameSessionState) {
        val armorIds = mutableSetOf<String>()
        val legacyEquipped = mutableMapOf<String, String>()

        state.inventory.keys.forEach { rawId ->
            canonicalArmorId(rawId)?.let { armorIds += it }
        }

        state.equippedItems.forEach { (key, value) ->
            val slot = if (key.contains(':')) key.substringAfter(':') else key
            if (!slot.equals("armor", ignoreCase = true)) return@forEach
            val armorId = canonicalArmorId(value) ?: return@forEach
            armorIds += armorId
            val ownerId = if (key.contains(':')) key.substringBefore(':') else null
            if (!ownerId.isNullOrBlank()) {
                legacyEquipped[ownerId.lowercase(Locale.getDefault())] = armorId
            }
        }

        state.equippedArmors.forEach { (ownerId, armorId) ->
            canonicalArmorId(armorId)?.let { armorIds += it }
            if (ownerId.isNotBlank() && armorId.isNotBlank()) {
                legacyEquipped.putIfAbsent(ownerId.lowercase(Locale.getDefault()), armorId)
            }
        }

        armorIds.forEach { sessionStore.unlockArmor(it) }
        legacyEquipped.forEach { (ownerId, armorId) ->
            val equipped = sessionStore.state.value.equippedArmors[ownerId]
            if (equipped.isNullOrBlank()) {
                sessionStore.setEquippedArmor(ownerId, armorId)
            }
        }

        val inventorySnapshot = inventoryService.snapshot()
        inventorySnapshot.forEach { (id, qty) ->
            if (isArmorItemId(id)) {
                inventoryService.removeItem(id, qty)
            }
        }
    }

    private fun seedDefaultWeaponsForCharacters(characterIds: List<String>) {
        if (characterIds.isEmpty()) return
        itemRepository.load()
        val weaponItems = itemRepository.allItems().filter { it.isWeaponItem() }
        if (weaponItems.isEmpty()) return
        val rng = Random(System.currentTimeMillis())
        characterIds
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { normalizedId ->
                val equipped = sessionStore.state.value.equippedWeapons[normalizedId]
                if (!equipped.isNullOrBlank()) {
                    sessionStore.unlockWeapon(equipped)
                    return@forEach
                }
                val chosen = pickRandomWeaponForCharacter(normalizedId, weaponItems, rng) ?: return@forEach
                sessionStore.setEquippedWeapon(normalizedId, chosen.id)
            }
    }

    private fun seedDefaultArmorsForCharacters(characterIds: List<String>) {
        if (characterIds.isEmpty()) return
        itemRepository.load()
        val armorItems = itemRepository.allItems().filter { it.isArmorItem() }
        if (armorItems.isEmpty()) return
        val rng = Random(System.currentTimeMillis())
        characterIds
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { normalizedId ->
                val equipped = sessionStore.state.value.equippedArmors[normalizedId]
                if (!equipped.isNullOrBlank()) {
                    sessionStore.unlockArmor(equipped)
                    return@forEach
                }
                val chosen = pickDefaultArmorForCharacter(normalizedId, armorItems, rng) ?: return@forEach
                sessionStore.setEquippedArmor(normalizedId, chosen.id)
            }
    }

    private fun unlockAllWeapons() {
        itemRepository.load()
        itemRepository.allItems()
            .filter { it.isWeaponItem() }
            .forEach { sessionStore.unlockWeapon(it.id) }
    }

    private fun unlockAllArmors() {
        itemRepository.load()
        itemRepository.allItems()
            .filter { it.isArmorItem() }
            .forEach { sessionStore.unlockArmor(it.id) }
    }

    private fun buildStartingWeaponState(
        characterIds: List<String>,
        unlockAll: Boolean
    ): Pair<Set<String>, Map<String, String>> {
        itemRepository.load()
        val unlocked = mutableSetOf<String>()
        val equipped = mutableMapOf<String, String>()
        val weaponItems = itemRepository.allItems().filter { it.isWeaponItem() }

        if (unlockAll) {
            weaponItems.forEach { unlocked += it.id }
        }

        val rng = Random(System.currentTimeMillis())
        characterIds
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { normalizedId ->
                val chosen = pickRandomWeaponForCharacter(normalizedId, weaponItems, rng) ?: return@forEach
                unlocked += chosen.id
                equipped.putIfAbsent(normalizedId, chosen.id)
            }

        return unlocked to equipped
    }

    private fun buildStartingArmorState(
        characterIds: List<String>,
        unlockAll: Boolean
    ): Pair<Set<String>, Map<String, String>> {
        itemRepository.load()
        val unlocked = mutableSetOf<String>()
        val equipped = mutableMapOf<String, String>()
        val armorItems = itemRepository.allItems().filter { it.isArmorItem() }

        if (unlockAll) {
            armorItems.forEach { unlocked += it.id }
        }

        if (armorItems.isEmpty()) return unlocked to equipped
        val rng = Random(System.currentTimeMillis())
        characterIds
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { normalizedId ->
                val chosen = pickDefaultArmorForCharacter(normalizedId, armorItems, rng) ?: return@forEach
                unlocked += chosen.id
                equipped.putIfAbsent(normalizedId, chosen.id)
            }

        return unlocked to equipped
    }

    private fun canonicalWeaponId(rawId: String): String? =
        itemRepository.findItem(rawId)?.takeIf { it.isWeaponItem() }?.id

    private fun isWeaponItemId(rawId: String): Boolean =
        itemRepository.findItem(rawId)?.let { it.isWeaponItem() } == true

    private fun canonicalArmorId(rawId: String): String? =
        itemRepository.findItem(rawId)?.takeIf { it.isArmorItem() }?.id

    private fun isArmorItemId(rawId: String): Boolean =
        itemRepository.findItem(rawId)?.let { it.isArmorItem() } == true

    private fun weaponOwnerFor(item: Item): String? {
        val weaponType = item.equipment?.weaponType?.trim()?.lowercase(Locale.getDefault())
        val fallbackType = item.type.trim().lowercase(Locale.getDefault())
        return GearRules.characterForWeaponType(weaponType ?: fallbackType)
    }

    private fun weaponOwnerFor(itemId: String): String? =
        itemRepository.findItem(itemId)?.let { weaponOwnerFor(it) }

    private fun weaponTypeFor(item: Item): String? {
        val equipmentType = item.equipment?.weaponType?.trim()?.lowercase(Locale.getDefault())
        if (!equipmentType.isNullOrBlank()) return equipmentType
        val normalizedType = item.type.trim().lowercase(Locale.getDefault())
        return normalizedType.takeIf { GearRules.isWeaponType(it) }
    }

    private fun armorTypeFor(item: Item): String? {
        val normalizedType = item.type.trim().lowercase(Locale.getDefault())
        return normalizedType.takeIf { GearRules.isArmorType(it) }
    }

    private fun pickRandomWeaponForCharacter(
        characterId: String,
        weapons: List<Item>,
        rng: Random
    ): Item? {
        if (weapons.isEmpty()) return null
        val expectedType = GearRules.allowedWeaponTypeFor(characterId)
        val matches = if (expectedType == null) weapons else weapons.filter {
            weaponTypeFor(it) == expectedType
        }
        val pool = if (matches.isNotEmpty()) matches else weapons
        return pool[rng.nextInt(pool.size)]
    }

    private fun pickRandomArmorForCharacter(
        characterId: String,
        armors: List<Item>,
        rng: Random
    ): Item? {
        if (armors.isEmpty()) return null
        val expectedType = GearRules.allowedArmorTypeFor(characterId)
        val matches = if (expectedType == null) armors else armors.filter {
            armorTypeFor(it) == expectedType
        }
        val pool = if (matches.isNotEmpty()) matches else armors
        return pool[rng.nextInt(pool.size)]
    }

    private fun pickDefaultArmorForCharacter(
        characterId: String,
        armors: List<Item>,
        rng: Random
    ): Item? {
        val normalizedId = characterId.trim().lowercase(Locale.getDefault())
        val expectedType = GearRules.allowedArmorTypeFor(normalizedId)
        val preferredId = defaultArmorsByCharacter[normalizedId]
        val preferred = preferredId?.let { id ->
            armors.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }
        if (preferred != null) {
            val preferredType = armorTypeFor(preferred)
            if (expectedType == null || preferredType == expectedType) {
                return preferred
            }
        }
        return pickRandomArmorForCharacter(normalizedId, armors, rng)
    }

    private fun Item.isWeaponItem(): Boolean {
        val normalizedType = type.trim().lowercase(Locale.getDefault())
        return normalizedType == "weapon" ||
            GearRules.isWeaponType(normalizedType) ||
            equipment?.slot?.equals("weapon", ignoreCase = true) == true
    }

    private fun Item.isArmorItem(): Boolean {
        val normalizedType = type.trim().lowercase(Locale.getDefault())
        return normalizedType == "armor" ||
            equipment?.slot?.equals("armor", ignoreCase = true) == true
    }

    suspend fun saveSlot(slot: Int) {
        sessionPersistence.writeSlot(slot, sessionStore.state.value)
    }

    suspend fun loadSlot(slot: Int): Boolean {
        val info = resolveSlotInfo(slot) ?: return false
        val state = info.state
        sessionStore.restore(state)
        inventoryService.restore(state.inventory)
        migrateLegacyWeapons(state)
        migrateLegacyArmors(state)
        resetAutosaveThrottle()
        return true
    }

    suspend fun clearSlot(slot: Int) {
        sessionPersistence.clearSlot(slot)
    }

    suspend fun slotState(slot: Int): GameSessionState? = slotInfo(slot)?.state

    suspend fun slotInfo(slot: Int): GameSessionSlotInfo? = resolveSlotInfo(slot)

    suspend fun loadAutosave(): Boolean {
        val info = sessionPersistence.autosaveInfo() ?: return false
        val state = info.state
        sessionStore.restore(state)
        inventoryService.restore(state.inventory)
        migrateLegacyWeapons(state)
        migrateLegacyArmors(state)
        recordAutosaveState(state)
        return true
    }

    suspend fun autosaveState(): GameSessionState? = sessionPersistence.autosaveInfo()?.state

    suspend fun autosaveInfo(): GameSessionSlotInfo? = sessionPersistence.autosaveInfo()

    suspend fun clearAutosave() {
        sessionPersistence.writeAutosave(GameSessionState())
        resetAutosaveThrottle()
    }

    suspend fun quickSave(): Boolean {
        sessionPersistence.writeQuickSave(sessionStore.state.value)
        return true
    }

    suspend fun loadQuickSave(): Boolean {
        val info = sessionPersistence.quickSaveInfo() ?: return false
        val state = info.state
        sessionStore.restore(state)
        inventoryService.restore(state.inventory)
        migrateLegacyWeapons(state)
        migrateLegacyArmors(state)
        recordAutosaveState(state)
        return true
    }

    suspend fun quickSaveInfo(): GameSessionSlotInfo? = sessionPersistence.quickSaveInfo()

    suspend fun clearQuickSave() {
        sessionPersistence.clearQuickSave()
    }

    fun syncInventoryFromSession() {
        inventoryService.restore(sessionStore.state.value.inventory)
    }

    suspend fun importLegacySave(file: File): Boolean {
        val imported = sessionPersistence.importLegacySave(file, itemRepository) ?: return false
        sessionStore.restore(imported)
        inventoryService.restore(imported.inventory)
        migrateLegacyWeapons(imported)
        migrateLegacyArmors(imported)
        return true
    }

    private suspend fun importLegacySlotFromAssets(slot: Int): Boolean {
        val assetName = "save$slot.json"
        if (!assetReader.assetExists(assetName)) return false
        val cacheFile = File(appContext.cacheDir, "legacy_slot_$slot.json")
        return try {
            appContext.assets.open(assetName).use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            val imported = sessionPersistence.importLegacySave(cacheFile, itemRepository) ?: return false
            sessionPersistence.writeSlot(slot, imported)
            true
        } catch (t: Throwable) {
            false
        } finally {
            cacheFile.delete()
        }
    }

    suspend fun resetSlotFromAssets(slot: Int): Boolean =
        importLegacySlotFromAssets(slot)

    private suspend fun resolveSlotInfo(slot: Int): GameSessionSlotInfo? {
        var info = sessionPersistence.slotInfo(slot)
        if ((info == null || info.state.needsFallbackImport()) && importLegacySlotFromAssets(slot)) {
            info = sessionPersistence.slotInfo(slot)
        }
        if (slot == SAMPLE_SLOT_ID && (info == null || !info.state.matchesSampleSeed())) {
            if (importLegacySlotFromAssets(slot)) {
                info = sessionPersistence.slotInfo(slot)
            }
        }
        return info
    }

    private fun GameSessionState.needsFallbackImport(): Boolean {
        val isPartyEmpty = playerId.isNullOrBlank() && partyMembers.isEmpty()
        val noProgress = worldId.isNullOrBlank() && hubId.isNullOrBlank() && roomId.isNullOrBlank()
        val noInventory = inventory.isEmpty() && playerCredits == 0
        return isPartyEmpty && noProgress && noInventory
    }

    private fun GameSessionState.matchesSampleSeed(): Boolean {
        if (partyMembers.size != SAMPLE_PARTY.size) return false
        if (!partyMembers.containsAll(SAMPLE_PARTY)) return false
        if (!playerId.isNullOrBlank() && playerId !in SAMPLE_PARTY) return false
        val worldMatches = worldId.isNullOrBlank() || worldId == SAMPLE_WORLD_ID
        val hubMatches = hubId.isNullOrBlank() || hubId == SAMPLE_HUB_ID
        val roomMatches = roomId.isNullOrBlank() || roomId == SAMPLE_ROOM_ID
        return worldMatches && hubMatches && roomMatches
    }

    fun startNewGame(debugFullInventory: Boolean = false): Boolean {
        return runCatching {
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            promptManager.dismissCurrent()
            tutorialManager.cancelAllScheduled()
            milestoneManager.clearHistory()

            val players = runCatching { worldDataSource.loadCharacters() }.getOrNull().orEmpty()
            val defaultPlayer = players.firstOrNull()
            val playerId = defaultPlayer?.id ?: SAMPLE_PARTY.firstOrNull()
            val baseLevel = defaultPlayer?.level ?: 1
            val baseXp = defaultPlayer?.xp ?: 0
            val startingRoomId = "town_9"
            val party = if (debugFullInventory) SAMPLE_PARTY.toList() else playerId?.let { listOf(it) } ?: emptyList()
            val rosterIds = players.map { it.id.trim() }.filter { it.isNotBlank() }.distinct()
            val weaponSeedIds = if (rosterIds.isNotEmpty()) rosterIds else party
            val (startingUnlockedWeapons, startingEquippedWeapons) = buildStartingWeaponState(
                characterIds = weaponSeedIds,
                unlockAll = debugFullInventory
            )
            val (startingUnlockedArmors, startingEquippedArmors) = buildStartingArmorState(
                characterIds = weaponSeedIds,
                unlockAll = debugFullInventory
            )
            val seedState = GameSessionState(
                worldId = "nova_prime",
                hubId = "mining_colony",
                roomId = startingRoomId,
                playerId = playerId,
                playerLevel = baseLevel,
                playerXp = baseXp,
                unlockedWeapons = startingUnlockedWeapons,
                unlockedArmors = startingUnlockedArmors,
                partyMembers = party,
                partyMemberLevels = party.associateWith { baseLevel },
                partyMemberXp = party.associateWith { baseXp },
                equippedWeapons = startingEquippedWeapons,
                equippedArmors = startingEquippedArmors
            )
            sessionStore.restore(seedState)
            sessionStore.resetTutorialProgress()
            sessionStore.resetQuestProgress()
            if (debugFullInventory) {
                val fullInventory = buildFullInventory()
                inventoryService.restore(fullInventory)
                sessionStore.setInventory(fullInventory)
                migrateLegacyWeapons(sessionStore.state.value)
                migrateLegacyArmors(sessionStore.state.value)
                unlockAllWeapons()
                unlockAllArmors()
                seedDefaultWeaponsForCharacters(weaponSeedIds)
                seedDefaultArmorsForCharacters(weaponSeedIds)
                sessionStore.addCredits(50_000)
                unlockAllSkillsForParty(party)
            } else {
                inventoryService.restore(emptyMap())
                seedDefaultWeaponsForCharacters(weaponSeedIds)
                seedDefaultArmorsForCharacters(weaponSeedIds)
                unlockStartingSkillsForParty(party, players, baseLevel)
            }
            questRuntimeManager.resetAll()
            resetAutosaveThrottle()

            val introSceneId = when {
                cinematicService.scene("intro_prologue") != null -> "intro_prologue"
                cinematicService.scene("new_game_intro") != null -> "new_game_intro"
                else -> null
            }
            introSceneId?.let { bootstrapCinematics.add(it) }
            bootstrapPlayerActions.add("new_game_spawn_player_and_fade")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start new game", err)
            false
        }
    }

    fun drainPendingCinematics(): List<String> =
        bootstrapCinematics.toList().also { bootstrapCinematics.clear() }

    fun drainPendingPlayerActions(): List<String> =
        bootstrapPlayerActions.toList().also { bootstrapPlayerActions.clear() }

    fun setDialogueTriggerListener(listener: ((String) -> Boolean)?) {
        dialogueTriggerListener = listener
    }

    private fun scheduleAutosave(state: GameSessionState) {
        val fingerprint = state.fingerprint()
        val now = System.currentTimeMillis()
        val elapsed = now - lastAutosaveTimestamp
        if (fingerprint == lastAutosaveFingerprint && elapsed < AUTOSAVE_INTERVAL_MS) {
            return
        }
        autosaveJob?.cancel()
        val delayMs = if (elapsed >= AUTOSAVE_INTERVAL_MS) 0L else AUTOSAVE_INTERVAL_MS - elapsed
        autosaveJob = persistenceScope.launch {
            if (delayMs > 0) delay(delayMs)
            sessionPersistence.writeAutosave(state)
            lastAutosaveTimestamp = System.currentTimeMillis()
            lastAutosaveFingerprint = fingerprint
        }
    }

    private fun recordAutosaveState(state: GameSessionState) {
        lastAutosaveFingerprint = state.fingerprint()
        lastAutosaveTimestamp = System.currentTimeMillis()
    }

    private fun resetAutosaveThrottle() {
        lastAutosaveFingerprint = null
        lastAutosaveTimestamp = 0L
    }

    private fun buildFullInventory(): Map<String, Int> {
        itemRepository.load()
        val items = itemRepository.allItems()
        if (items.isEmpty()) return emptyMap()
        val maxStack = 9
        val inventory = items.associate { item ->
            val qty = when {
                (item.equipment != null) -> 4
                item.type.equals("key_item", true) -> 1
                else -> maxStack
            }
            item.id to qty
        }.toMutableMap()

        listOf(
            "nova_laser_blaster",
            "nova_plasma_shotgun",
            "nova_rocket_launcher",
            "zeke_shock_fists",
            "zeke_quake_knuckles",
            "zeke_maul_gauntlet",
            "orion_prism_focus",
            "orion_halo_array",
            "orion_stasis_core",
            "gh0st_whisperblade",
            "gh0st_splitter_edge",
            "gh0st_void_fang",
            "zeke_bulwark_plate",
            "zeke_surge_harness",
            "zeke_brawler_rig",
            "orion_ward_coat",
            "orion_channeler_mantle",
            "orion_driftweave_cloak",
            "gh0st_nullguard_coat",
            "gh0st_phaseweave_jacket",
            "gh0st_killer_harness",
            "precision_sight",
            "stability_gyro",
            "vital_signet",
            "focus_conduit",
            "shadow_coil",
            "brutal_charm",
            "lucky_talisman",
            "reactive_plating",
            "pulse_citrus",
            "iron_biscuit",
            "mindfruit_slice",
            "rattle_pepper",
            "slipstream_mochi",
            "shadow_kelp",
            "ember_broth",
            "void_crisp"
        ).forEach { id -> inventory.putIfAbsent(id, 1) }

        return inventory
    }

    private fun unlockStartingSkillsForParty(
        party: List<String>,
        players: List<Player>,
        baseLevel: Int
    ) {
        if (party.isEmpty()) return
        val byId = players.associateBy { it.id }
        party.forEach { memberId ->
            byId[memberId]?.skills.orEmpty().forEach { skillId ->
                sessionStore.unlockSkill(skillId)
            }
            progressionData.levelUpSkills[memberId].orEmpty().forEach { (level, skillId) ->
                val resolvedLevel = level.toIntOrNull() ?: return@forEach
                if (resolvedLevel <= baseLevel) {
                    sessionStore.unlockSkill(skillId)
                }
            }
        }
    }

    private fun unlockAllSkillsForParty(party: List<String>) {
        if (party.isEmpty()) return
        val skills = runCatching { worldDataSource.loadSkills() }.getOrNull().orEmpty()
        skills.filter { skill -> party.contains(skill.character) }
            .forEach { skill -> sessionStore.unlockSkill(skill.id) }
    }
}

internal fun isDialogueConditionMet(
    condition: String?,
    state: GameSessionState,
    inventoryService: InventoryService
): Boolean {
    if (condition.isNullOrBlank()) return true
    val tokens = condition.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    for (token in tokens) {
        val parts = token.split(':', limit = 2)
        if (parts.isEmpty()) continue
        val type = parts[0].trim().lowercase()
        val value = parts.getOrNull(1)?.trim().orEmpty()
        val met = when (type) {
            "quest" -> value in state.activeQuests || value in state.completedQuests
            "quest_active" -> value in state.activeQuests
            "quest_completed" -> value in state.completedQuests
            "quest_not_started" -> value.isNotBlank() &&
                value !in state.activeQuests &&
                value !in state.completedQuests &&
                value !in state.failedQuests
            "quest_failed" -> value in state.failedQuests
            "quest_stage" -> {
                val (questId, stageId) = parseQuestStageCondition(value)
                questId != null && stageId != null &&
                    state.questStageById[questId]?.equals(stageId, ignoreCase = true) == true
            }
            "quest_stage_not" -> {
                val (questId, stageId) = parseQuestStageCondition(value)
                questId == null || stageId == null ||
                    state.questStageById[questId]?.equals(stageId, ignoreCase = true) != true
            }
            "milestone" -> value in state.completedMilestones
            "milestone_not_set" -> value !in state.completedMilestones
            "item" -> value.isNotBlank() && inventoryService.hasItem(value)
            "item_not" -> value.isNotBlank() && !inventoryService.hasItem(value)
            "event_completed" -> value in state.completedEvents
            "event_not_completed" -> value.isNotBlank() && value !in state.completedEvents
            "tutorial_completed" -> value in state.tutorialCompleted
            "tutorial_not_completed" -> value.isNotBlank() && value !in state.tutorialCompleted
            else -> true
        }
        if (!met) return false
    }
    return true
}

private fun parseQuestStageCondition(raw: String): Pair<String?, String?> {
    if (raw.isBlank()) return null to null
    val parts = raw.split(':', limit = 2)
    val questId = parts.getOrNull(0)?.trim().takeUnless { it.isNullOrEmpty() }
    val stageId = parts.getOrNull(1)?.trim().takeUnless { it.isNullOrEmpty() }
    return questId to stageId
}

internal fun handleDialogueTrigger(
    trigger: String,
    sessionStore: GameSessionStore,
    questRuntimeManager: QuestRuntimeManager,
    inventoryService: InventoryService? = null
) {
    val actions = DialogueTriggerParser.parse(trigger)
    if (actions.isEmpty()) return
    actions.forEach { action ->
        when (action.type.lowercase(Locale.getDefault())) {
            "start_quest" -> action.startQuest?.let {
                sessionStore.startQuest(it)
                questRuntimeManager.recordQuestStarted(it)
            }
            "complete_quest" -> action.completeQuest?.let {
                sessionStore.completeQuest(it)
                questRuntimeManager.markQuestCompleted(it)
                questRuntimeManager.recordQuestCompleted(it)
            }
            "fail_quest" -> action.questId?.let {
                sessionStore.failQuest(it)
                questRuntimeManager.markQuestFailed(it)
            }
            "set_milestone" -> action.milestone?.let { sessionStore.setMilestone(it) }
            "clear_milestone" -> action.milestone?.let { sessionStore.clearMilestone(it) }
            "track_quest" -> sessionStore.setTrackedQuest(action.questId)
            "untrack_quest" -> sessionStore.setTrackedQuest(null)
            "set_quest_task_done" -> {
                val questId = action.questId
                val taskId = action.taskId
                if (!questId.isNullOrBlank() && !taskId.isNullOrBlank()) {
                    questRuntimeManager.markTaskComplete(questId, taskId)
                }
            }
            "advance_quest_stage" -> {
                val questId = action.questId
                val stageId = action.toStageId
                if (!questId.isNullOrBlank() && !stageId.isNullOrBlank()) {
                    questRuntimeManager.setStage(questId, stageId)
                }
            }
            "give_xp" -> action.xp?.takeIf { it > 0 }?.let { sessionStore.addXp(it) }
            "give_reward" -> action.credits?.takeIf { it > 0 }?.let { sessionStore.addCredits(it) }
            "add_party_member" -> action.itemId?.let { sessionStore.addPartyMember(it) }
            "give_item" -> if (inventoryService != null) {
                val itemId = action.itemId ?: action.item
                val quantity = action.quantity ?: 1
                if (!itemId.isNullOrBlank() && quantity > 0) {
                    inventoryService.addItem(itemId, quantity)
                }
            }
            "take_item" -> if (inventoryService != null) {
                val itemId = action.itemId ?: action.item
                val quantity = action.quantity ?: 1
                if (!itemId.isNullOrBlank() && quantity > 0) {
                    inventoryService.removeItem(itemId, quantity)
                }
            }
        }
    }
}
