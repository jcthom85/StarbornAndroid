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
import com.example.starborn.domain.session.migrateOpeningNarrativeState
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
import com.example.starborn.domain.movement.EnemyPartyRuntimeState
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.domain.tutorial.TutorialScriptRepository
import com.example.starborn.data.local.UserSettingsStore
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.domain.telemetry.LocalPlaytestTelemetry
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
    val playtestTelemetry = LocalPlaytestTelemetry(File(appContext.noBackupFilesDir, "playtest")).apply {
        startSession("app_launch")
    }
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
    val audioCuePlayer = AudioCuePlayer(context, preloadCueIds = audioCatalog.cues.map { it.id })
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

    private val defaultWeaponsByCharacter = mapOf(
        "nova" to "nova_laser_blaster",
        "zeke" to "zeke_shock_fists",
        "orion" to "orion_prism_focus",
        "gh0st" to "gh0st_whisperblade"
    )

    private val defaultSnacksByCharacter = mapOf(
        "nova" to "starbar_crunch",
        "zeke" to "mineral_trail_mix",
        "orion" to "comet_gummies",
        "gh0st" to "void_jerky"
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
                handleDialogueTrigger(
                    trigger = trigger,
                    sessionStore = sessionStore,
                    questRuntimeManager = questRuntimeManager,
                    inventoryService = inventoryService,
                    onMilestoneSet = { milestone ->
                        milestoneManager.handleMilestone(milestone, null)
                        milestoneManager.applyEffectsFor(milestone)
                    }
                )
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
                sessionStore.restore(stored.migrateOpeningNarrativeState())
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
        val ownerId = armorOwnerFor(item)
        if (!ownerId.isNullOrBlank()) {
            val normalizedOwner = ownerId.lowercase(Locale.getDefault())
            val equipped = sessionStore.state.value.equippedArmors[normalizedOwner]
            if (equipped.isNullOrBlank()) {
                sessionStore.setEquippedArmor(ownerId, item.id)
            }
        }
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

    private fun equipNovaStarterGear() {
        sessionStore.unlockWeapon("mining_pistol")
        sessionStore.setEquippedWeapon("nova", "mining_pistol")
        sessionStore.unlockArmor("nova_flux_liner")
        sessionStore.setEquippedArmor("nova", "nova_flux_liner")
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
                if (normalizedId == "nova") return@forEach
                val equipped = sessionStore.state.value.equippedWeapons[normalizedId]
                if (!equipped.isNullOrBlank()) {
                    sessionStore.unlockWeapon(equipped)
                    return@forEach
                }
                val chosen = pickDefaultWeaponForCharacter(normalizedId, weaponItems, rng) ?: return@forEach
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
                if (normalizedId == "nova") return@forEach
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
                if (!unlockAll && normalizedId == "nova") return@forEach
                val chosen = pickDefaultWeaponForCharacter(normalizedId, weaponItems, rng) ?: return@forEach
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
                if (!unlockAll && normalizedId == "nova") return@forEach
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

    private fun armorOwnerFor(item: Item): String? {
        val armorType = item.type.trim().lowercase(Locale.getDefault())
        return GearRules.characterForArmorType(armorType)
    }

    private fun armorOwnerFor(itemId: String): String? =
        itemRepository.findItem(itemId)?.let { armorOwnerFor(it) }

    private fun pickDefaultWeaponForCharacter(
        characterId: String,
        weapons: List<Item>,
        rng: Random
    ): Item? {
        val normalizedId = characterId.trim().lowercase(Locale.getDefault())
        val expectedType = GearRules.allowedWeaponTypeFor(normalizedId)
        val preferredId = defaultWeaponsByCharacter[normalizedId]
        val preferred = preferredId?.let { id ->
            weapons.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }
        if (preferred != null) {
            val preferredType = weaponTypeFor(preferred)
            if (expectedType == null || preferredType == expectedType) {
                return preferred
            }
        }
        return pickRandomWeaponForCharacter(normalizedId, weapons, rng)
    }

    private fun buildStartingSnackState(
        characterIds: List<String>
    ): Map<String, String> {
        val equipped = mutableMapOf<String, String>()
        characterIds
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { normalizedId ->
                val snackId = defaultSnacksByCharacter[normalizedId]
                if (snackId != null) {
                    equipped["$normalizedId:snack"] = snackId
                }
            }
        return equipped
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
        sessionStore.restore(state.migrateOpeningNarrativeState())
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
        sessionStore.restore(state.migrateOpeningNarrativeState())
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
        sessionStore.restore(state.migrateOpeningNarrativeState())
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
        sessionStore.restore(imported.migrateOpeningNarrativeState())
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
            val startingRoomId = "pit_nova_bunk"
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
            val startingEquippedItems = if (debugFullInventory) buildStartingSnackState(weaponSeedIds) else emptyMap()
            val seedState = GameSessionState(
                worldId = "world_1",
                hubId = "hub_1_homestead",
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
                equippedArmors = startingEquippedArmors,
                equippedItems = startingEquippedItems
            )
            sessionStore.restore(seedState.migrateOpeningNarrativeState())
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
            playtestTelemetry.startSession(if (debugFullInventory) "debug_full_inventory" else "new_game")
            sessionStore.startQuest("w1_mq01", track = true)
            sessionStore.setQuestStage("w1_mq01", "wake_in_the_pit")
            playtestTelemetry.questStarted("w1_mq01")
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

    fun startDebugScenario(id: String): Boolean = when (id) {
        "story_w1_start" -> startNewGame()
        "full_inventory" -> startNewGame(debugFullInventory = true)
        "first_combat" -> startNewGameAtFirstCombat()
        "enemy_party" -> startNewGameAtEnemyPartyCombat()
        "presence_stress" -> startNewGameAtPresenceStress()
        "party_sizes" -> startNewGameAtEnemyPartySizes()
        "hub_qa_w1_rest" -> startNewGameAtHubQaW1Rest()
        "hub_qa_w2_cookfire" -> startNewGameAtHubQaW2Cookfire()
        "hub_qa_w3_tuning" -> startNewGameAtHubQaW3Tuning()
        "hub_qa_w4_tuning" -> startNewGameAtHubQaW4Tuning()
        "hub_qa_w6_source" -> startNewGameAtHubQaW6Source()
        "room_items" -> startNewGameAtRoomItems()
        "scavenger" -> startNewGameAtScavengerStash()
        "heavy_lifting" -> startNewGameAtHeavyLifting()
        "lift_shaft" -> startNewGameAtLiftShaft()
        "weather_lab" -> startNewGameAtWeatherLab()
        "checkpoint" -> startNewGameAtCheckpoint()
        "deep_mine" -> startNewGameAtDeepMine()
        "dynamic_patrol" -> startNewGameAtDynamicPatrol()
        "red_alert" -> startNewGameAtRedAlert()
        "launch" -> startNewGameAtLaunch()
        "w2_crash_start" -> startNewGameAtW2CrashStart()
        "w2_temple_gate" -> startNewGameAtW2TempleGate()
        "w2_stasis_chamber" -> startNewGameAtW2StasisChamber()
        "w2_hunter_canopy" -> startNewGameAtW2HunterCanopy()
        "w2_source_gate" -> startNewGameAtW2SourceGate()
        "w2_astra_repair" -> startNewGameAtW2AstraRepair()
        "w3_sewers_entry" -> startNewGameAtW3SewersEntry()
        "w3_safehouse_plan" -> startNewGameAtW3SafehousePlan()
        "w3_checkpoint_infiltration" -> startNewGameAtW3CheckpointInfiltration()
        "w3_lens_archive" -> startNewGameAtW3LensArchive()
        "w3_lockdown_escape" -> startNewGameAtW3LockdownEscape()
        "w4_foundry_start" -> startNewGameAtW4FoundryStart()
        "w4_phantom_records" -> startNewGameAtW4PhantomRecords()
        "w4_assembly_floor" -> startNewGameAtW4AssemblyFloor()
        "w4_anvil_forge" -> startNewGameAtW4AnvilForge()
        "w4_meltdown_escape" -> startNewGameAtW4MeltdownEscape()
        "w5_docking_procedure" -> startNewGameAtW5DockingProcedure()
        "w5_zero_g" -> startNewGameAtW5ZeroG()
        "w5_core_firewall" -> startNewGameAtW5CoreFirewall()
        "w5_anchor_chamber" -> startNewGameAtW5AnchorChamber()
        "w5_critical_mass" -> startNewGameAtW5CriticalMass()
        "w6_fractured_minds" -> startNewGameAtW6FracturedMinds()
        "w6_echo_mines" -> startNewGameAtW6EchoMines()
        "w6_crossing" -> startNewGameAtW6Crossing()
        "w6_spire" -> startNewGameAtW6Spire()
        "w6_finale" -> startNewGameAtW6Finale()
        "fun_w3_corporate_espionage" -> startNewGameAtFunW3CorporateEspionage()
        "fun_w4_quality_control" -> startNewGameAtFunW4QualityControl()
        "fun_w5_ghost_shell" -> startNewGameAtFunW5GhostInTheShell()
        "fun_w6_hr_record" -> startNewGameAtFunW6HrRecord()
        "node_progression_w1" -> startNewGameAtWorld1NodeProgression()
        "node_progression_w2" -> startNewGameAtWorld2NodeProgression()
        "astra_access" -> startNewGameAtAstraAccess()
        "astra_home" -> startNewGameAboardAstra()
        else -> if (id.startsWith("hub_")) startNewGameAtDebugHub(id) else false
    }

    private fun startNewGameAtHubQaW1Rest(): Boolean = runCatching {
        if (!startNewGame(debugFullInventory = true)) return false
        clearDebugBootstrap()
        sessionStore.setWorld("world_1")
        sessionStore.setHub("hub_1_homestead")
        sessionStore.setRoom("pit_nova_bunk")
        sessionStore.visitNode("pit")
        sessionStore.completeQuest("w1_mq01")
        sessionStore.setMilestone("ms_w1_mq01_complete")
        sessionStore.setRoomState("pit_nova_bunk", "light_on", true)
        sessionStore.setRoomState("pit_nova_bunk", "dark", false)
        sessionStore.markTutorialCompleted("swipe_move")
        sessionStore.markTutorialCompleted("movement")
        true
    }.getOrElse { false }

    private fun startNewGameAtHubQaW2Cookfire(): Boolean = runCatching {
        if (!prepareWorld2DebugState(completedW2Quests = listOf("w2_mq01"))) return false
        sessionStore.startQuest("w2_mq02", track = true)
        sessionStore.setQuestStage("w2_mq02", "follow_stream")
        sessionStore.setWorld("world_2")
        sessionStore.setHub("hub_3_sector9")
        sessionStore.setRoom("sector9_stream_falls")
        visitDebugNodes("sector9_landing", "sector9_stream_path")
        true
    }.getOrElse { false }

    private fun startNewGameAtHubQaW3Tuning(): Boolean = runCatching {
        if (!prepareWorld3DebugState(completedW3Quests = listOf("w3_mq11", "w3_mq12", "w3_mq13"))) return false
        sessionStore.startQuest("w3_mq14", track = true)
        sessionStore.setQuestStage("w3_mq14", "archive_infiltration")
        sessionStore.setQuestTaskCompleted("w3_mq14", "enter_archive", true)
        sessionStore.setMilestone("ms_w3_archive_entered")
        sessionStore.setWorld("world_3")
        sessionStore.setHub("hub_6_upper_city")
        sessionStore.setRoom("spire_prism_gallery")
        visitDebugNodes("spire_laundry", "spire_skypark", "spire_archive")
        true
    }.getOrElse { false }

    private fun startNewGameAtHubQaW4Tuning(): Boolean = runCatching {
        if (!startNewGameAtW4AnvilForge()) return false
        sessionStore.setRoom("foundry_forge_anvil")
        true
    }.getOrElse { false }

    private fun startNewGameAtHubQaW6Source(): Boolean = runCatching {
        if (!prepareWorld6DebugState()) return false
        sessionStore.startQuest("w6_mq26", track = true)
        sessionStore.setQuestStage("w6_mq26", "nightmares")
        sessionStore.setQuestTasksCompleted(
            questId = "w6_mq26",
            taskIds = setOf("rescue_zeke", "rescue_gh0st", "rescue_orion")
        )
        sessionStore.setWorld("world_6")
        sessionStore.setHub("hub_11_event_horizon")
        sessionStore.setRoom("source_campfire")
        visitDebugNodes("source_campfire_node")
        true
    }.getOrElse { false }

    private fun startNewGameAtWorld1NodeProgression(): Boolean = runCatching {
        if (!startNewGame(debugFullInventory = true)) return false
        clearDebugBootstrap()
        sessionStore.setWorld("world_1")
        sessionStore.setHub("hub_1_homestead")
        sessionStore.setRoom("pit_L1_landing")
        sessionStore.visitNode("pit")
        true
    }.getOrElse { false }

    private fun startNewGameAtWorld2NodeProgression(): Boolean = runCatching {
        if (!startNewGame(debugFullInventory = true)) return false
        clearDebugBootstrap()
        (1..5).forEach { number ->
            val questId = "w1_mq0$number"
            sessionStore.completeQuest(questId)
            sessionStore.setMilestone("ms_${questId}_complete")
        }
        sessionStore.setPartyMembers(listOf("nova", "zeke"))
        sessionStore.startQuest("w2_mq01", track = true)
        sessionStore.setQuestStage("w2_mq01", "assess_crash_site")
        sessionStore.setWorld("world_2")
        sessionStore.setHub("hub_3_sector9")
        sessionStore.setRoom("sector9_crash_site")
        sessionStore.visitNode("sector9_landing")
        true
    }.getOrElse { false }

    private fun startNewGameAtAstraAccess(): Boolean = runCatching {
        if (!prepareAstraDebugState()) return false
        true
    }.getOrElse { false }

    private fun startNewGameAtW2CrashStart(): Boolean = runCatching {
        if (!prepareWorld2DebugState()) return false
        sessionStore.startQuest("w2_mq01", track = true)
        sessionStore.setQuestStage("w2_mq01", "assess_crash_site")
        sessionStore.setWorld("world_2")
        sessionStore.setHub("hub_3_sector9")
        sessionStore.setRoom("sector9_crash_site")
        visitDebugNodes("sector9_landing")
        true
    }.getOrElse { false }

    private fun startNewGameAtW2TempleGate(): Boolean = runCatching {
        if (!prepareWorld2DebugState(completedW2Quests = listOf("w2_mq01"))) return false
        sessionStore.startQuest("w2_mq02", track = true)
        sessionStore.setQuestStage("w2_mq02", "reach_temple_gate")
        sessionStore.setQuestTasksCompleted("w2_mq02", setOf("clear_canopy_path"))
        sessionStore.setWorld("world_2")
        sessionStore.setHub("hub_3_sector9")
        sessionStore.setRoom("sector9_temple_gate")
        visitDebugNodes("sector9_landing", "razor_vine_path", "canopy_ridge", "temple_gate")
        true
    }.getOrElse { false }

    private fun startNewGameAtW2StasisChamber(): Boolean = runCatching {
        if (!prepareWorld2DebugState(completedW2Quests = listOf("w2_mq01", "w2_mq02"))) return false
        sessionStore.startQuest("w2_mq03", track = true)
        sessionStore.setQuestStage("w2_mq03", "override_stasis")
        sessionStore.setQuestTasksCompleted("w2_mq03", setOf("inspect_murals"))
        sessionStore.setWorld("world_2")
        sessionStore.setHub("hub_4_facility")
        sessionStore.setRoom("sector9_stasis_chamber")
        visitDebugNodes("hall_of_echoes", "stasis_chamber")
        true
    }.getOrElse { false }

    private fun startNewGameAtW2HunterCanopy(): Boolean = runCatching {
        if (!prepareWorld2DebugState(completedW2Quests = listOf("w2_mq01", "w2_mq02", "w2_mq03"))) return false
        sessionStore.setPartyMembers(listOf("nova", "zeke", "orion"))
        sessionStore.setPlayerLevel(12)
        listOf("nova", "zeke", "orion").forEach { memberId ->
            sessionStore.setPartyMemberLevel(memberId, 12)
        }
        sessionStore.setMilestone("ms_debug_w2_anchor_flow")
        sessionStore.startQuest("w2_mq04", track = true)
        sessionStore.setQuestStage("w2_mq04", "canopy_ridge")
        sessionStore.setWorld("world_2")
        sessionStore.setHub("hub_3_sector9")
        sessionStore.setRoom("sector9_canopy_ridge")
        visitDebugNodes("sector9_landing", "razor_vine_path", "canopy_ridge", "temple_gate")
        true
    }.getOrElse { false }

    private fun startNewGameAtW2SourceGate(): Boolean = runCatching {
        if (!prepareWorld2DebugState(completedW2Quests = listOf("w2_mq01", "w2_mq02", "w2_mq03", "w2_mq04"))) return false
        sessionStore.setPartyMembers(listOf("nova", "zeke", "orion", "gh0st"))
        sessionStore.startQuest("w2_mq05", track = true)
        sessionStore.setQuestStage("w2_mq05", "repair_astra")
        sessionStore.setWorld("world_2")
        sessionStore.setHub("hub_4_facility")
        sessionStore.setRoom("sector9_source_gate")
        visitDebugNodes("hall_of_echoes", "stasis_chamber", "source_gate")
        true
    }.getOrElse { false }

    private fun startNewGameAtW2AstraRepair(): Boolean = runCatching {
        if (!prepareWorld2DebugState(completedW2Quests = listOf("w2_mq01", "w2_mq02", "w2_mq03", "w2_mq04"))) return false
        sessionStore.setPartyMembers(listOf("nova", "zeke", "orion", "gh0st"))
        sessionStore.startQuest("w2_mq05", track = true)
        sessionStore.setQuestStage("w2_mq05", "repair_astra")
        sessionStore.setQuestTasksCompleted("w2_mq05", setOf("bypass_source_gate"))
        sessionStore.setWorld("world_2")
        sessionStore.setHub("hub_4_facility")
        sessionStore.setRoom("sector9_hangar_bay")
        visitDebugNodes("hall_of_echoes", "stasis_chamber", "source_gate", "hangar_bay")
        true
    }.getOrElse { false }

    private fun startNewGameAtW3SewersEntry(): Boolean = runCatching {
        if (!prepareAstraDebugState()) return false
        true
    }.getOrElse { false }

    private fun startNewGameAtW3SafehousePlan(): Boolean = runCatching {
        if (!prepareWorld3DebugState(completedW3Quests = listOf("w3_mq11"))) return false
        sessionStore.startQuest("w3_mq12", track = true)
        sessionStore.setQuestStage("w3_mq12", "gather_intel")
        sessionStore.setWorld("world_3")
        sessionStore.setHub("hub_5_lower_city")
        sessionStore.setRoom("spire_zekes_apartment")
        visitDebugNodes("spire_sewers", "spire_vent_output", "spire_the_static", "spire_night_market", "spire_transit_plaza")
        true
    }.getOrElse { false }

    private fun startNewGameAtW3CheckpointInfiltration(): Boolean = runCatching {
        if (!prepareWorld3DebugState(completedW3Quests = listOf("w3_mq11", "w3_mq12"))) return false
        sessionStore.startQuest("w3_mq13", track = true)
        sessionStore.setQuestStage("w3_mq13", "gain_access")
        sessionStore.setWorld("world_3")
        sessionStore.setHub("hub_6_upper_city")
        sessionStore.setRoom("spire_laundry_service")
        visitDebugNodes("spire_laundry", "spire_skypark")
        true
    }.getOrElse { false }

    private fun startNewGameAtW3LensArchive(): Boolean = runCatching {
        if (!prepareWorld3DebugState(completedW3Quests = listOf("w3_mq11", "w3_mq12", "w3_mq13"))) return false
        sessionStore.startQuest("w3_mq14", track = true)
        sessionStore.setQuestStage("w3_mq14", "archive_infiltration")
        sessionStore.setWorld("world_3")
        sessionStore.setHub("hub_6_upper_city")
        sessionStore.setRoom("spire_archive_vault")
        visitDebugNodes("spire_laundry", "spire_skypark", "spire_archive")
        true
    }.getOrElse { false }

    private fun startNewGameAtW3LockdownEscape(): Boolean = runCatching {
        if (!prepareWorld3DebugState(completedW3Quests = listOf("w3_mq11", "w3_mq12", "w3_mq13", "w3_mq14"))) return false
        inventoryService.addItem("the_lens", 1)
        sessionStore.unlockSkill("source_art_scan")
        sessionStore.startQuest("w3_mq15", track = true)
        sessionStore.setQuestStage("w3_mq15", "escape_lockdown")
        sessionStore.setWorld("world_3")
        sessionStore.setHub("hub_6_upper_city")
        sessionStore.setRoom("spire_prism_gallery")
        visitDebugNodes("spire_laundry", "spire_skypark", "spire_archive", "spire_landing_pad")
        true
    }.getOrElse { false }

    private fun startNewGameAboardAstra(): Boolean = runCatching {
        if (!prepareAstraDebugState()) return false
        sessionStore.setAstraReturnLocation("world_3", "hub_5_lower_city", "spire_vent_output")
        sessionStore.setWorld("world_astra")
        sessionStore.setHub("hub_astra")
        sessionStore.setRoom("astra_bridge")
        sessionStore.visitNode("astra_bridge_node")
        true
    }.getOrElse { false }

    private fun startNewGameAtW4FoundryStart(): Boolean = runCatching {
        if (!prepareWorld4DebugState()) return false
        sessionStore.startQuest("w4_mq16", track = true)
        sessionStore.setQuestStage("w4_mq16", "secure_landing")
        sessionStore.setWorld("world_4")
        sessionStore.setHub("hub_7_slag_pits")
        sessionStore.setRoom("foundry_slag_landing")
        visitDebugNodes("foundry_obsidian_shelf")
        true
    }.getOrElse { false }

    private fun startNewGameAtW4PhantomRecords(): Boolean = runCatching {
        if (!prepareWorld4DebugState(completedW4Quests = listOf("w4_mq16"))) return false
        inventoryService.addItem("heat_liner", 1)
        sessionStore.setMilestone("ms_w4_landing_secured")
        sessionStore.setMilestone("ms_w4_slag_crossed")
        sessionStore.setMilestone("ms_w4_cooling_vents_hacked")
        sessionStore.startQuest("w4_mq17", track = true)
        sessionStore.setQuestStage("w4_mq17", "phantom_records")
        sessionStore.setWorld("world_4")
        sessionStore.setHub("hub_7_slag_pits")
        sessionStore.setRoom("foundry_waste_intake")
        visitDebugNodes(
            "foundry_obsidian_shelf",
            "foundry_slag_river",
            "foundry_cooling_springs",
            "foundry_waste_intake",
            "foundry_service_airlock"
        )
        true
    }.getOrElse { false }

    private fun startNewGameAtW4AssemblyFloor(): Boolean = runCatching {
        if (!prepareWorld4DebugState(completedW4Quests = listOf("w4_mq16", "w4_mq17"))) return false
        inventoryService.addItem("heat_liner", 1)
        inventoryService.addItem("gh0st_override_key", 1)
        sessionStore.unlockSkill("gh0st_phase_counter")
        sessionStore.setMilestone("ms_w4_landing_secured")
        sessionStore.setMilestone("ms_w4_slag_crossed")
        sessionStore.setMilestone("ms_w4_cooling_vents_hacked")
        sessionStore.setMilestone("ms_w4_phantom_records_found")
        sessionStore.setMilestone("ms_w4_springs_regrouped")
        sessionStore.startQuest("w4_mq18", track = true)
        sessionStore.setQuestStage("w4_mq18", "production_floor")
        sessionStore.setWorld("world_4")
        sessionStore.setHub("hub_8_assembly_line")
        sessionStore.setRoom("foundry_conveyor_belt")
        visitDebugNodes("foundry_conveyor_belt")
        true
    }.getOrElse { false }

    private fun startNewGameAtW4AnvilForge(): Boolean = runCatching {
        if (!prepareWorld4DebugState(completedW4Quests = listOf("w4_mq16", "w4_mq17", "w4_mq18"))) return false
        inventoryService.addItem("heat_liner", 1)
        inventoryService.addItem("gh0st_override_key", 1)
        sessionStore.unlockSkill("gh0st_phase_counter")
        sessionStore.setMilestone("ms_w4_landing_secured")
        sessionStore.setMilestone("ms_w4_slag_crossed")
        sessionStore.setMilestone("ms_w4_cooling_vents_hacked")
        sessionStore.setMilestone("ms_w4_phantom_records_found")
        sessionStore.setMilestone("ms_w4_springs_regrouped")
        sessionStore.setMilestone("ms_w4_conveyors_timed")
        sessionStore.setMilestone("ms_w4_prototypes_defeated")
        sessionStore.setMilestone("ms_w4_matrix_overloaded")
        sessionStore.startQuest("w4_mq19", track = true)
        sessionStore.setQuestStage("w4_mq19", "forge_access")
        sessionStore.setWorld("world_4")
        sessionStore.setHub("hub_8_assembly_line")
        sessionStore.setRoom("foundry_forge_anvil")
        visitDebugNodes("foundry_conveyor_belt", "foundry_conditioning", "foundry_forge", "foundry_power_core")
        true
    }.getOrElse { false }

    private fun startNewGameAtW4MeltdownEscape(): Boolean = runCatching {
        if (!prepareWorld4DebugState(completedW4Quests = listOf("w4_mq16", "w4_mq17", "w4_mq18", "w4_mq19"))) return false
        inventoryService.addItem("heat_liner", 1)
        inventoryService.addItem("gh0st_override_key", 1)
        inventoryService.addItem("the_anvil", 1)
        sessionStore.unlockSkill("gh0st_phase_counter")
        sessionStore.unlockSkill("source_art_construct")
        sessionStore.setMilestone("ms_w4_landing_secured")
        sessionStore.setMilestone("ms_w4_slag_crossed")
        sessionStore.setMilestone("ms_w4_cooling_vents_hacked")
        sessionStore.setMilestone("ms_w4_phantom_records_found")
        sessionStore.setMilestone("ms_w4_springs_regrouped")
        sessionStore.setMilestone("ms_w4_conveyors_timed")
        sessionStore.setMilestone("ms_w4_prototypes_defeated")
        sessionStore.setMilestone("ms_w4_matrix_overloaded")
        sessionStore.setMilestone("ms_w4_conveyor_puzzle_solved")
        sessionStore.setMilestone("ms_w4_anvil_claimed")
        sessionStore.setMilestone("ms_w4_rylos_confronted")
        sessionStore.setMilestone("ms_w4_titan_defeated")
        sessionStore.startQuest("w4_mq20", track = true)
        sessionStore.setQuestStage("w4_mq20", "steal_and_escape")
        sessionStore.setQuestTasksCompleted("w4_mq20", setOf("confront_rylos", "defeat_titan_walker"))
        sessionStore.setWorld("world_4")
        sessionStore.setHub("hub_8_assembly_line")
        sessionStore.setRoom("foundry_power_core")
        visitDebugNodes(
            "foundry_conveyor_belt",
            "foundry_conditioning",
            "foundry_forge",
            "foundry_power_core",
            "foundry_titan_dock"
        )
        true
    }.getOrElse { false }

    private fun startNewGameAtW5DockingProcedure(): Boolean = runCatching {
        if (!prepareWorld5DebugState()) return false
        sessionStore.startQuest("w5_mq21", track = true)
        sessionStore.setQuestStage("w5_mq21", "approach")
        sessionStore.setWorld("world_5")
        sessionStore.setHub("hub_9_orbital_ring")
        sessionStore.setRoom("orbital_executive_dock")
        visitDebugNodes("orbital_executive_dock")
        true
    }.getOrElse { false }

    private fun startNewGameAtW5ZeroG(): Boolean = runCatching {
        if (!prepareWorld5DebugState(completedW5Quests = listOf("w5_mq21"))) return false
        sessionStore.setMilestone("ms_w5_halo_approached")
        sessionStore.setMilestone("ms_w5_fighter_screen_broken")
        sessionStore.setMilestone("ms_w5_forced_dock")
        sessionStore.setMilestone("ms_w5_airlock_hacked")
        sessionStore.startQuest("w5_mq22", track = true)
        sessionStore.setQuestStage("w5_mq22", "ring_crossing")
        sessionStore.setWorld("world_5")
        sessionStore.setHub("hub_9_orbital_ring")
        sessionStore.setRoom("orbital_solarium")
        visitDebugNodes("orbital_executive_dock", "orbital_grand_concourse", "orbital_solarium", "orbital_security_hub")
        true
    }.getOrElse { false }

    private fun startNewGameAtW5CoreFirewall(): Boolean = runCatching {
        if (!prepareWorld5DebugState(completedW5Quests = listOf("w5_mq21", "w5_mq22"))) return false
        inventoryService.addItem("grav_boots", 1)
        sessionStore.setMilestone("ms_w5_solarium_crossed")
        sessionStore.setMilestone("ms_w5_security_hub_cleared")
        sessionStore.setMilestone("ms_w5_shaft_traversed")
        sessionStore.setMilestone("ms_w5_mainframe_accessed")
        sessionStore.setMilestone("ms_w5_thorne_found")
        sessionStore.startQuest("w5_mq23", track = true)
        sessionStore.setQuestStage("w5_mq23", "firewall_maze")
        sessionStore.setWorld("world_5")
        sessionStore.setHub("hub_10_deep_ring")
        sessionStore.setRoom("orbital_server_farm")
        sessionStore.setRoomState("orbital_server_farm", debugEncounterClearedStateKey("void_turret", "void_turret"), true)
        sessionStore.setRoomState("deep_mainframe_nave", debugEncounterClearedStateKey("void_turret"), true)
        sessionStore.setRoomState("deep_firewall_alpha", debugEncounterClearedStateKey("void_turret"), true)
        sessionStore.setRoomState("deep_firewall_beta", debugEncounterClearedStateKey("void_turret"), true)
        visitDebugNodes("deep_server_farm")
        true
    }.getOrElse { false }

    private fun startNewGameAtW5AnchorChamber(): Boolean = runCatching {
        if (!prepareWorld5DebugState(completedW5Quests = listOf("w5_mq21", "w5_mq22", "w5_mq23"))) return false
        inventoryService.addItem("grav_boots", 1)
        sessionStore.setMilestone("ms_w5_server_maze_mapped")
        sessionStore.setMilestone("ms_w5_firewall_alpha_down")
        sessionStore.setMilestone("ms_w5_firewall_beta_down")
        sessionStore.setMilestone("ms_w5_firewall_gamma_down")
        sessionStore.startQuest("w5_mq24", track = true)
        sessionStore.setQuestStage("w5_mq24", "reunion")
        sessionStore.setWorld("world_5")
        sessionStore.setHub("hub_10_deep_ring")
        sessionStore.setRoom("deep_anchor_chamber")
        visitDebugNodes("deep_server_farm", "deep_anchor_chamber")
        true
    }.getOrElse { false }

    private fun startNewGameAtW5CriticalMass(): Boolean = runCatching {
        if (!prepareWorld5DebugState(completedW5Quests = listOf("w5_mq21", "w5_mq22", "w5_mq23", "w5_mq24"))) return false
        inventoryService.addItem("grav_boots", 1)
        inventoryService.addItem("anchor_relic", 1)
        sessionStore.unlockSkill("source_art_stasis")
        sessionStore.setMilestone("ms_w5_anchor_chamber_entered")
        sessionStore.setMilestone("ms_w5_elara_found")
        sessionStore.setMilestone("ms_w5_anchor_taken")
        sessionStore.startQuest("w5_mq25", track = true)
        sessionStore.setQuestStage("w5_mq25", "soloist")
        sessionStore.setWorld("world_5")
        sessionStore.setHub("hub_10_deep_ring")
        sessionStore.setRoom("deep_throne_room")
        visitDebugNodes("deep_server_farm", "deep_anchor_chamber", "deep_throne_room")
        true
    }.getOrElse { false }

    private fun startNewGameAtW6FracturedMinds(): Boolean = runCatching {
        if (!prepareWorld6DebugState()) return false
        sessionStore.startQuest("w6_mq26", track = true)
        sessionStore.setQuestStage("w6_mq26", "nightmares")
        sessionStore.setWorld("world_6")
        sessionStore.setHub("hub_11_event_horizon")
        sessionStore.setRoom("source_campfire")
        visitDebugNodes("source_campfire_node")
        true
    }.getOrElse { false }

    private fun startNewGameAtW6EchoMines(): Boolean = runCatching {
        if (!prepareWorld6DebugState(completedW6Quests = listOf("w6_mq26"))) return false
        inventoryService.addItem("key_relic", 1)
        sessionStore.setMilestone("ms_w6_mq26_complete")
        sessionStore.startQuest("w6_mq27", track = true)
        sessionStore.setQuestStage("w6_mq27", "distorted_mines")
        sessionStore.setWorld("world_6")
        sessionStore.setHub("hub_11_event_horizon")
        sessionStore.setRoom("source_echo_mines")
        visitDebugNodes(
            "source_campfire_node",
            "source_zeke_nightmare_node",
            "source_gh0st_nightmare_node",
            "source_orion_nightmare_node",
            "source_echo_mines_node"
        )
        true
    }.getOrElse { false }

    private fun startNewGameAtW6Crossing(): Boolean = runCatching {
        if (!prepareWorld6DebugState(completedW6Quests = listOf("w6_mq26", "w6_mq27"))) return false
        inventoryService.addItem("key_relic", 1)
        sessionStore.setMilestone("ms_w6_mq26_complete")
        sessionStore.setMilestone("ms_w6_mq27_complete")
        sessionStore.startQuest("w6_mq28", track = true)
        sessionStore.setQuestStage("w6_mq28", "memory_bridge")
        sessionStore.setWorld("world_6")
        sessionStore.setHub("hub_11_event_horizon")
        sessionStore.setRoom("source_memory_bridge")
        visitDebugNodes("source_echo_mines_node", "source_memory_bridge_node")
        true
    }.getOrElse { false }

    private fun startNewGameAtW6Spire(): Boolean = runCatching {
        if (!prepareWorld6DebugState(completedW6Quests = listOf("w6_mq26", "w6_mq27", "w6_mq28"))) return false
        inventoryService.addItem("key_relic", 1)
        sessionStore.setMilestone("ms_w6_mq26_complete")
        sessionStore.setMilestone("ms_w6_mq27_complete")
        sessionStore.setMilestone("ms_w6_mq28_complete")
        sessionStore.startQuest("w6_mq29", track = true)
        sessionStore.setQuestStage("w6_mq29", "memory_ascent")
        sessionStore.setWorld("world_6")
        sessionStore.setHub("hub_12_singularity")
        sessionStore.setRoom("source_memory_stair")
        visitDebugNodes("source_memory_stair_node")
        true
    }.getOrElse { false }

    private fun startNewGameAtW6Finale(): Boolean = runCatching {
        if (!prepareWorld6DebugState(completedW6Quests = listOf("w6_mq26", "w6_mq27", "w6_mq28", "w6_mq29"))) return false
        inventoryService.addItem("key_relic", 1)
        sessionStore.setMilestone("ms_w6_mq26_complete")
        sessionStore.setMilestone("ms_w6_mq27_complete")
        sessionStore.setMilestone("ms_w6_mq28_complete")
        sessionStore.setMilestone("ms_w6_mq29_complete")
        sessionStore.startQuest("w6_mq30", track = true)
        sessionStore.setQuestStage("w6_mq30", "soloist")
        sessionStore.setWorld("world_6")
        sessionStore.setHub("hub_12_singularity")
        sessionStore.setRoom("source_center")
        visitDebugNodes("source_memory_stair_node", "source_spire_thought_node", "source_center_node")
        true
    }.getOrElse { false }

    private fun startNewGameAtFunW3CorporateEspionage(): Boolean = runCatching {
        if (!prepareWorld3DebugState(completedW3Quests = listOf("w3_mq11", "w3_mq12", "w3_mq13"))) return false
        sessionStore.setWorld("world_3")
        sessionStore.setHub("hub_6_upper_city")
        sessionStore.setRoom("spire_exec_lounge_bar")
        visitDebugNodes("spire_laundry", "spire_skypark", "spire_archive")
        true
    }.getOrElse { false }

    private fun startNewGameAtFunW4QualityControl(): Boolean = runCatching {
        if (!prepareWorld4DebugState(completedW4Quests = listOf("w4_mq16", "w4_mq17", "w4_mq18"))) return false
        sessionStore.setWorld("world_4")
        sessionStore.setHub("hub_8_assembly_line")
        sessionStore.setRoom("foundry_reject_bay")
        visitDebugNodes("foundry_conveyor_belt", "foundry_conditioning")
        true
    }.getOrElse { false }

    private fun startNewGameAtFunW5GhostInTheShell(): Boolean = runCatching {
        if (!prepareWorld5DebugState(completedW5Quests = listOf("w5_mq21", "w5_mq22", "w5_mq23"))) return false
        sessionStore.setWorld("world_5")
        sessionStore.setHub("hub_10_deep_ring")
        sessionStore.setRoom("orbital_server_farm")
        sessionStore.setRoomState("orbital_server_farm", debugEncounterClearedStateKey("void_turret", "void_turret"), true)
        visitDebugNodes("deep_server_farm")
        true
    }.getOrElse { false }

    private fun startNewGameAtFunW6HrRecord(): Boolean = runCatching {
        if (!prepareWorld6DebugState(completedW6Quests = listOf("w6_mq26"))) return false
        sessionStore.setWorld("world_6")
        sessionStore.setHub("hub_11_event_horizon")
        sessionStore.setRoom("source_zeke_nightmare")
        visitDebugNodes("source_campfire_node", "source_zeke_nightmare_node")
        true
    }.getOrElse { false }

    private fun prepareWorld2DebugState(completedW2Quests: List<String> = emptyList()): Boolean {
        if (!startNewGame(debugFullInventory = true)) return false
        clearDebugBootstrap()
        completeDebugQuests("w1_mq01", "w1_mq02", "w1_mq03", "w1_mq04", "w1_mq05")
        completeDebugQuests(*completedW2Quests.toTypedArray())
        sessionStore.setPartyMembers(listOf("nova", "zeke"))
        sessionStore.markTutorialCompleted("swipe_move")
        sessionStore.markTutorialCompleted("movement")
        return true
    }

    private fun prepareWorld3DebugState(completedW3Quests: List<String> = emptyList()): Boolean {
        if (!prepareAstraDebugState()) return false
        completeDebugQuests(*completedW3Quests.toTypedArray())
        sessionStore.setPartyMembers(listOf("nova", "zeke", "orion", "gh0st"))
        sessionStore.setAstraReturnLocation("world_3", "hub_5_lower_city", "spire_vent_output")
        return true
    }

    private fun prepareWorld4DebugState(completedW4Quests: List<String> = emptyList()): Boolean {
        if (!prepareWorld3DebugState(completedW3Quests = listOf("w3_mq11", "w3_mq12", "w3_mq13", "w3_mq14", "w3_mq15"))) return false
        completeDebugQuests(*completedW4Quests.toTypedArray())
        inventoryService.addItem("the_lens", 1)
        sessionStore.unlockSkill("source_art_scan")
        sessionStore.setPartyMembers(listOf("nova", "zeke", "orion", "gh0st"))
        sessionStore.setMilestone("ms_w4_access_unlocked")
        sessionStore.setAstraReturnLocation("world_4", "hub_7_slag_pits", "foundry_slag_landing")
        sessionStore.markTutorialCompleted("swipe_move")
        sessionStore.markTutorialCompleted("movement")
        return true
    }

    private fun prepareWorld5DebugState(completedW5Quests: List<String> = emptyList()): Boolean {
        if (!prepareWorld4DebugState(completedW4Quests = listOf("w4_mq16", "w4_mq17", "w4_mq18", "w4_mq19", "w4_mq20"))) return false
        completeDebugQuests(*completedW5Quests.toTypedArray())
        inventoryService.addItem("deep_core_engine", 1)
        inventoryService.addItem("phase_cutter_arrays", 1)
        inventoryService.addItem("the_anvil", 1)
        inventoryService.addItem("gh0st_override_key", 1)
        sessionStore.unlockSkill("gh0st_phase_counter")
        sessionStore.unlockSkill("source_art_construct")
        sessionStore.setPartyMembers(listOf("nova", "zeke", "orion", "gh0st"))
        sessionStore.setMilestone("ms_w5_access_unlocked")
        sessionStore.setAstraReturnLocation("world_5", "hub_9_orbital_ring", "orbital_executive_dock")
        return true
    }

    private fun prepareWorld6DebugState(completedW6Quests: List<String> = emptyList()): Boolean {
        if (!prepareWorld5DebugState(completedW5Quests = listOf("w5_mq21", "w5_mq22", "w5_mq23", "w5_mq24", "w5_mq25"))) return false
        completeDebugQuests(*completedW6Quests.toTypedArray())
        inventoryService.addItem("anchor_relic", 1)
        sessionStore.unlockSkill("source_art_stasis")
        sessionStore.setPartyMembers(listOf("nova", "zeke", "orion", "gh0st"))
        sessionStore.setMilestone("ms_w6_access_unlocked")
        sessionStore.setAstraReturnLocation("world_6", "hub_11_event_horizon", "source_campfire")
        return true
    }

    private fun completeDebugQuests(vararg questIds: String) {
        questIds.forEach { questId ->
            sessionStore.completeQuest(questId)
            sessionStore.setMilestone("ms_${questId}_complete")
        }
    }

    private fun visitDebugNodes(vararg nodeIds: String) {
        nodeIds.forEach(sessionStore::visitNode)
    }

    private fun prepareAstraDebugState(): Boolean {
        if (!startNewGame(debugFullInventory = true)) return false
        clearDebugBootstrap()
        completeDebugQuests(*listOf(
            "w1_mq01", "w1_mq02", "w1_mq03", "w1_mq04", "w1_mq05",
            "w2_mq01", "w2_mq02", "w2_mq03", "w2_mq04", "w2_mq05"
        ).toTypedArray())
        sessionStore.setPartyMembers(listOf("nova", "zeke", "orion", "gh0st"))
        sessionStore.startQuest("w3_mq11", track = true)
        sessionStore.setQuestStage("w3_mq11", "secure_landing_zone")
        sessionStore.setWorld("world_3")
        sessionStore.setHub("hub_5_lower_city")
        sessionStore.setRoom("spire_sewers_landing")
        sessionStore.visitNode("spire_sewers")
        return true
    }

    private fun clearDebugBootstrap() {
        bootstrapCinematics.clear()
        bootstrapPlayerActions.clear()
    }

    private fun startNewGameAtDebugHub(hubId: String): Boolean = runCatching {
        val hubs = worldDataSource.loadHubs()
        val hub = hubs.firstOrNull { it.id == hubId } ?: return false
        if (!startNewGame(debugFullInventory = true)) return false
        bootstrapCinematics.clear()
        bootstrapPlayerActions.clear()

        val mainQuests = questRepository.allQuests()
            .filter { it.id.matches(Regex("w\\d+_mq\\d+")) }
            .sortedWith(compareBy(
                { it.id.substringBefore("_mq").removePrefix("w").toIntOrNull() ?: 0 },
                { it.id.substringAfter("_mq").toIntOrNull() ?: Int.MAX_VALUE }
            ))
        val targetQuest = mainQuests.firstOrNull { it.hubId == hubId }
        if (targetQuest != null) {
            mainQuests.takeWhile { it.id != targetQuest.id }.forEach { quest ->
                sessionStore.completeQuest(quest.id)
                sessionStore.setMilestone("ms_${quest.id}_complete")
            }
            sessionStore.startQuest(targetQuest.id, track = true)
            sessionStore.setQuestStage(targetQuest.id, targetQuest.stages.firstOrNull()?.id)
        }

        sessionStore.setWorld(hub.worldId)
        sessionStore.setHub(hub.id)
        sessionStore.setRoom(null)
        worldDataSource.loadHubNodes()
            .filter { it.hubId == hub.id }
            .forEach { sessionStore.visitNode(it.id) }
        sessionStore.markTutorialCompleted("swipe_move")
        sessionStore.markTutorialCompleted("movement")
        true
    }.getOrElse { err ->
        Log.e("AppServices", "Failed to start debug hub $hubId.", err)
        false
    }

    fun startNewGameAtFirstCombat(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = true)) return false
            val inventory = sessionStore.state.value.inventory.toMutableMap()
            inventory["mine_access_badge"] = (inventory["mine_access_badge"] ?: 0).coerceAtLeast(1)
            inventory["functional_cryo_inductor"] = (inventory["functional_cryo_inductor"] ?: 0).coerceAtLeast(1)
            inventory["nova_flux_liner"] = (inventory["nova_flux_liner"] ?: 0).coerceAtLeast(1)
            inventoryService.restore(inventory)
            sessionStore.setInventory(inventory)
            sessionStore.completeQuest("w1_mq01")
            sessionStore.completeQuest("w1_mq02")
            sessionStore.setMilestone("ms_w1_mq01_complete")
            sessionStore.setMilestone("ms_w1_mq02_complete")
            sessionStore.startQuest("w1_mq03", track = true)
            sessionStore.setQuestStage("w1_mq03", "deep_mine_descent")
            sessionStore.setQuestTasksCompleted(
                questId = "w1_mq03",
                taskIds = setOf("enter_logistics_sector", "talk_to_bogs", "use_deep_elevator")
            )
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_2_logistics")
            sessionStore.setRoom("mine_landing")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug first-combat game.", err)
            false
        }
    }

    fun startNewGameAtEnemyPartyCombat(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = true)) return false
            val inventory = sessionStore.state.value.inventory.toMutableMap()
            inventory["mine_access_badge"] = (inventory["mine_access_badge"] ?: 0).coerceAtLeast(1)
            inventory["tuning_fork"] = (inventory["tuning_fork"] ?: 0).coerceAtLeast(1)
            inventory["ghost_signal_cell"] = (inventory["ghost_signal_cell"] ?: 0).coerceAtLeast(1)
            inventoryService.restore(inventory)
            sessionStore.setInventory(inventory)
            listOf("w1_mq01", "w1_mq02", "w1_mq03", "w1_mq04").forEach(sessionStore::completeQuest)
            listOf(
                "ms_w1_mq01_complete",
                "ms_w1_mq02_complete",
                "ms_w1_mq03_complete",
                "ms_w1_mq04_complete"
            ).forEach(sessionStore::setMilestone)
            sessionStore.startQuest("w1_mq05", track = true)
            sessionStore.setQuestStage("w1_mq05", "reach_pod_bay")
            sessionStore.setQuestTasksCompleted(
                questId = "w1_mq05",
                taskIds = emptySet()
            )
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_2_logistics")
            sessionStore.setRoom("launch_checkpoint")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug enemy-party combat game.", err)
            false
        }
    }

    fun startNewGameAtPresenceStress(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = false)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            equipNovaStarterGear()
            sessionStore.completeQuest("w1_mq01")
            sessionStore.setMilestone("ms_w1_mq01_complete")
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_2_logistics")
            sessionStore.setRoom("debug_presence_stress")
            sessionStore.unlockSkill("nova_hydraulic_kick")
            sessionStore.setPlayerLevel(3)
            sessionStore.setPlayerXp(250)
            sessionStore.setPartyMemberLevel("nova", 3)
            sessionStore.setPartyMemberXp("nova", 250)
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug presence-stress game.", err)
            false
        }
    }

    fun startNewGameAtEnemyPartySizes(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = false)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            equipNovaStarterGear()
            sessionStore.completeQuest("w1_mq01")
            sessionStore.setMilestone("ms_w1_mq01_complete")
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_2_logistics")
            sessionStore.setRoom("debug_enemy_party_sizes")
            sessionStore.unlockSkill("nova_hydraulic_kick")
            sessionStore.setPlayerLevel(3)
            sessionStore.setPlayerXp(250)
            sessionStore.setPartyMemberLevel("nova", 3)
            sessionStore.setPartyMemberXp("nova", 250)
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug enemy-party-size game.", err)
            false
        }
    }

    fun startNewGameAtHub2(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = true)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            sessionStore.completeQuest("w1_mq01")
            sessionStore.completeQuest("w1_mq02")
            sessionStore.setMilestone("ms_w1_mq01_complete")
            sessionStore.setMilestone("ms_w1_mq02_complete")
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_2_logistics")
            sessionStore.setRoom("admin_lobby")
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug Hub 2 game.", err)
            false
        }
    }

    fun startNewGameAtHub1(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = true)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_1_homestead")
            sessionStore.setRoom(null)
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug Hub 1 game.", err)
            false
        }
    }

    fun startNewGameAtRoomItems(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = true)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            sessionStore.completeQuest("w1_mq01")
            sessionStore.setMilestone("ms_w1_mq01_complete")
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_1_homestead")
            sessionStore.setRoom("medbay_storage")
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug room-items game.", err)
            false
        }
    }

    fun startNewGameAtScavengerStash(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = false)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            equipNovaStarterGear()
            sessionStore.completeQuest("w1_mq01")
            sessionStore.setMilestone("ms_w1_mq01_complete")
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_1_homestead")
            sessionStore.setRoom("trade_scrapper")
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug Scavenger's Stash game.", err)
            false
        }
    }

    fun startNewGameAtHeavyLifting(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = false)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            equipNovaStarterGear()
            sessionStore.completeQuest("w1_mq01")
            sessionStore.setMilestone("ms_w1_mq01_complete")
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_1_homestead")
            sessionStore.setRoom("workshop_dock")
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug Heavy Lifting game.", err)
            false
        }
    }

    fun startNewGameAtLiftShaft(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = false)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            equipNovaStarterGear()
            sessionStore.completeQuest("w1_mq01")
            sessionStore.setMilestone("ms_w1_mq01_complete")
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_1_homestead")
            sessionStore.setRoom("pit_shaft")
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug Lift Shaft game.", err)
            false
        }
    }

    fun startNewGameAtWeatherLab(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = false)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_1_homestead")
            sessionStore.setRoom("weather_lab")
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug Weather Lab game.", err)
            false
        }
    }

    fun startNewGameAtCheckpoint(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = false)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            equipNovaStarterGear()
            sessionStore.completeQuest("w1_mq01")
            sessionStore.setMilestone("ms_w1_mq01_complete")
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_1_homestead")
            sessionStore.setRoom("checkpoint_queue")
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug checkpoint game.", err)
            false
        }
    }

    fun startNewGameAtDeepMine(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = true)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            val inventory = sessionStore.state.value.inventory.toMutableMap()
            inventory["mine_access_badge"] = (inventory["mine_access_badge"] ?: 0).coerceAtLeast(1)
            inventory["functional_cryo_inductor"] = (inventory["functional_cryo_inductor"] ?: 0).coerceAtLeast(1)
            inventory["nova_flux_liner"] = (inventory["nova_flux_liner"] ?: 0).coerceAtLeast(1)
            inventoryService.restore(inventory)
            sessionStore.setInventory(inventory)
            listOf("w1_mq01", "w1_mq02", "w1_sq03").forEach(sessionStore::completeQuest)
            listOf(
                "ms_w1_mq01_complete",
                "ms_w1_mq02_complete",
                "ms_w1_sq03_started",
                "ms_w1_guardbreak_trained"
            ).forEach(sessionStore::setMilestone)
            sessionStore.unlockSkill("nova_hydraulic_kick")
            sessionStore.startQuest("w1_mq03", track = true)
            sessionStore.setQuestStage("w1_mq03", "sector_four_assignment")
            sessionStore.setQuestTasksCompleted(
                questId = "w1_mq03",
                taskIds = setOf(
                    "enter_logistics_sector",
                    "clear_first_mine_encounter",
                    "break_riot_guard"
                )
            )
            sessionStore.setRoomState("mine_alpha", debugEncounterClearedStateKey("echo_borer"), true)
            sessionStore.setRoomState("mine_checkpoint", debugEncounterClearedStateKey("acoustic_bulwark"), true)
            sessionStore.setRoomState("mine_conveyor", debugEncounterClearedStateKey("pressure_hauler"), true)
            sessionStore.setEnemyPartyStates(
                mapOf(
                    "w1_deep_mine_pressure_patrol" to deepMinePressurePatrolState(defeated = true)
                )
            )
            sessionStore.setPlayerLevel(3)
            sessionStore.setPlayerXp(250)
            sessionStore.setPartyMembers(listOf("nova", "zeke"))
            sessionStore.setPartyMemberLevel("nova", 3)
            sessionStore.setPartyMemberXp("nova", 250)
            sessionStore.setPartyMemberLevel("zeke", 3)
            sessionStore.setPartyMemberXp("zeke", 250)
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_2_logistics")
            sessionStore.setRoom("admin_lobby")
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug Deep Mine game.", err)
            false
        }
    }

    fun startNewGameAtDynamicPatrol(): Boolean {
        return runCatching {
            if (!startNewGameAtDeepMine()) return false
            sessionStore.setEnemyPartyStates(
                mapOf(
                    "w1_deep_mine_pressure_patrol" to deepMinePressurePatrolState(defeated = false)
                )
            )
            sessionStore.setRoom("mine_conveyor")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug dynamic patrol game.", err)
            false
        }
    }

    private fun deepMinePressurePatrolState(defeated: Boolean) = EnemyPartyRuntimeState(
        roomId = "mine_conveyor",
        routeIndex = 0,
        moveRemainingMs = 25_000L,
        defeated = defeated
    )

    fun startNewGameAtRedAlert(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = true)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            val inventory = sessionStore.state.value.inventory.toMutableMap()
            inventory["mine_access_badge"] = (inventory["mine_access_badge"] ?: 0).coerceAtLeast(1)
            inventory["tuning_fork"] = (inventory["tuning_fork"] ?: 0).coerceAtLeast(1)
            inventoryService.restore(inventory)
            sessionStore.setInventory(inventory)
            listOf("w1_mq01", "w1_mq02", "w1_sq03", "w1_mq03").forEach(sessionStore::completeQuest)
            listOf(
                "ms_w1_mq01_complete",
                "ms_w1_mq02_complete",
                "ms_w1_sq03_started",
                "ms_w1_guardbreak_trained",
                "ms_w1_mq03_complete"
            ).forEach(sessionStore::setMilestone)
            sessionStore.unlockSkill("nova_hydraulic_kick")
            sessionStore.unlockSkill("nova_blast_wave")
            sessionStore.startQuest("w1_mq04", track = true)
            sessionStore.setQuestStage("w1_mq04", "cargo_lift_sacrifice")
            sessionStore.setQuestTasksCompleted(
                questId = "w1_mq04",
                taskIds = setOf(
                    "survive_lockdown_broadcast",
                    "clear_escape_gauntlet"
                )
            )
            sessionStore.setRoomState("launch_access", debugEncounterClearedStateKey("resonance_buoy"), true)
            sessionStore.setRoomState("launch_lift", debugEncounterClearedStateKey("acoustic_bulwark"), true)
            sessionStore.setPlayerLevel(4)
            sessionStore.setPlayerXp(450)
            sessionStore.setPartyMembers(listOf("nova", "zeke"))
            sessionStore.setPartyMemberLevel("nova", 4)
            sessionStore.setPartyMemberXp("nova", 450)
            sessionStore.setPartyMemberLevel("zeke", 4)
            sessionStore.setPartyMemberXp("zeke", 450)
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_2_logistics")
            sessionStore.setRoom("echo_exit")
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug Red Alert game.", err)
            false
        }
    }

    fun startNewGameAtLaunch(): Boolean {
        return runCatching {
            if (!startNewGame(debugFullInventory = true)) return false
            bootstrapCinematics.clear()
            bootstrapPlayerActions.clear()
            val inventory = sessionStore.state.value.inventory.toMutableMap()
            inventory["mine_access_badge"] = (inventory["mine_access_badge"] ?: 0).coerceAtLeast(1)
            inventory["tuning_fork"] = (inventory["tuning_fork"] ?: 0).coerceAtLeast(1)
            inventory["ghost_signal_cell"] = (inventory["ghost_signal_cell"] ?: 0).coerceAtLeast(1)
            inventoryService.restore(inventory)
            sessionStore.setInventory(inventory)
            listOf("w1_mq01", "w1_mq02", "w1_sq03", "w1_mq03", "w1_mq04").forEach(sessionStore::completeQuest)
            listOf(
                "ms_w1_mq01_complete",
                "ms_w1_mq02_complete",
                "ms_w1_sq03_started",
                "ms_w1_guardbreak_trained",
                "ms_w1_mq03_complete",
                "ms_w1_mq04_complete"
            ).forEach(sessionStore::setMilestone)
            sessionStore.unlockSkill("nova_hydraulic_kick")
            sessionStore.unlockSkill("nova_blast_wave")
            sessionStore.startQuest("w1_mq05", track = true)
            sessionStore.setQuestStage("w1_mq05", "launch_pod")
            sessionStore.setQuestTasksCompleted(
                questId = "w1_mq05",
                taskIds = setOf(
                    "fight_through_launch_access",
                    "reach_pod_bay",
                    "survive_warden_intro",
                    "break_warden_guard",
                    "defeat_the_warden"
                )
            )
            sessionStore.setRoomState("launch_checkpoint", debugEncounterClearedStateKey("dominion_dampener", "resonance_buoy"), true)
            sessionStore.setRoomState("launch_bay", debugEncounterClearedStateKey("the_iron_warden"), true)
            sessionStore.setRoomState("launch_bay", "warden_defeated", true)
            sessionStore.setPlayerLevel(5)
            sessionStore.setPlayerXp(700)
            sessionStore.setPartyMembers(listOf("nova", "zeke"))
            sessionStore.setPartyMemberLevel("nova", 5)
            sessionStore.setPartyMemberXp("nova", 700)
            sessionStore.setPartyMemberLevel("zeke", 5)
            sessionStore.setPartyMemberXp("zeke", 700)
            sessionStore.setWorld("world_1")
            sessionStore.setHub("hub_2_logistics")
            sessionStore.setRoom("launch_bay")
            sessionStore.markTutorialCompleted("swipe_move")
            sessionStore.markTutorialCompleted("movement")
            true
        }.getOrElse { err ->
            Log.e("AppServices", "Failed to start debug Launch game.", err)
            false
        }
    }

    private fun debugEncounterClearedStateKey(vararg enemyIds: String): String =
        "encounter_cleared:" + enemyIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .sorted()
            .joinToString("|")

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
    inventoryService: InventoryService? = null,
    onMilestoneSet: ((String) -> Unit)? = null
) {
    val actions = DialogueTriggerParser.parse(trigger)
    if (actions.isEmpty()) return
    actions.forEach { action ->
        when (action.type.lowercase(Locale.getDefault())) {
            "start_quest" -> action.startQuest?.let {
                sessionStore.startQuest(it, track = true)
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
            "set_milestone" -> action.milestone?.let {
                sessionStore.setMilestone(it)
                onMilestoneSet?.invoke(it)
            }
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
