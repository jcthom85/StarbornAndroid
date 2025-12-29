package com.example.starborn.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.combat.CombatFormulas
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemUseController
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.session.GameSessionStore
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService,
    private val sessionStore: GameSessionStore,
    playerRoster: List<Player>
) : ViewModel() {

    val entries: StateFlow<List<InventoryEntry>> = inventoryService.state
        .map { list -> list.filterNot { entry -> entry.item.isWeaponItem() || entry.item.isArmorItem() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = inventoryService.state.value.filterNot { it.item.isWeaponItem() || it.item.isArmorItem() }
        )
    val equippedItems: StateFlow<Map<String, String>> = sessionStore.state
        .map { it.equippedItems }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sessionStore.state.value.equippedItems
        )
    val unlockedWeapons: StateFlow<Set<String>> = sessionStore.state
        .map { it.unlockedWeapons }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sessionStore.state.value.unlockedWeapons
        )
    val equippedWeapons: StateFlow<Map<String, String>> = sessionStore.state
        .map { it.equippedWeapons }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sessionStore.state.value.equippedWeapons
        )
    val unlockedArmors: StateFlow<Set<String>> = sessionStore.state
        .map { it.unlockedArmors }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sessionStore.state.value.unlockedArmors
        )
    val equippedArmors: StateFlow<Map<String, String>> = sessionStore.state
        .map { it.equippedArmors }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sessionStore.state.value.equippedArmors
        )

    private val charactersById: Map<String, Player> = playerRoster.associateBy { it.id }
    private val itemUseController = ItemUseController(
        inventoryService = inventoryService,
        craftingService = craftingService,
        sessionStore = sessionStore,
        charactersProvider = { charactersById }
    )

    init {
        inventoryService.loadItems()
        // Prime inventory from the current session immediately so the Gear tab
        // has data on first render before the flow collector runs.
        val initialInventory = sessionStore.state.value.inventory
        if (inventoryService.snapshot() != initialInventory) {
            inventoryService.restore(initialInventory)
        }
        viewModelScope.launch {
            sessionStore.state
                .map { it.inventory }
                .distinctUntilChanged()
                .collect { inventory ->
                    if (inventoryService.snapshot() != inventory) {
                        inventoryService.restore(inventory)
                    }
                }
        }
    }

    val partyMembers: StateFlow<List<PartyMemberStatus>> = sessionStore.state
        .map { state ->
            val party = state.partyMembers.ifEmpty { listOfNotNull(state.playerId ?: charactersById.keys.firstOrNull()) }
            party.filterNotNull().map { id ->
                val character = charactersById[id]
                val baseMaxHp = character?.let { CombatFormulas.maxHp(it.hp, it.vitality) } ?: 0
                val currentHp = state.partyMemberHp[id] ?: baseMaxHp
                PartyMemberStatus(
                    id = id,
                    name = character?.name ?: id,
                    hp = currentHp,
                    maxHp = maxOf(baseMaxHp, currentHp),
                    portraitPath = character?.miniIconPath
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun useItem(itemId: String, targetId: String? = null) {
        viewModelScope.launch {
            when (val outcome = itemUseController.useItem(itemId, targetId)) {
                is ItemUseController.Result.Success -> _messages.emit(outcome.message)
                is ItemUseController.Result.Failure -> _messages.emit(outcome.message)
            }
            sessionStore.setInventory(inventoryService.snapshot())
        }
    }

    fun syncFromSession() {
        val sessionInventory = sessionStore.state.value.inventory
        if (sessionInventory.isNotEmpty()) {
            inventoryService.restore(sessionInventory)
        } else {
            sessionStore.setInventory(inventoryService.snapshot())
        }
    }

    fun equipItem(slotId: String, itemId: String?, characterId: String? = null) {
        val normalizedSlot = slotId.trim().lowercase(Locale.getDefault())
        if (normalizedSlot.isBlank() || normalizedSlot !in GearRules.equipSlots) return
        if (normalizedSlot == "armor") {
            if (characterId != null) {
                equipArmor(characterId, itemId)
            }
            return
        }
        if (itemId.isNullOrBlank()) {
            sessionStore.setEquippedItem(normalizedSlot, null, characterId)
            return
        }
        val entry = entries.value.firstOrNull { it.item.id == itemId } ?: return
        val normalizedType = entry.item.type.trim().lowercase(Locale.getDefault())
        val equipment = entry.item.equipment ?: run {
            val slotFromType = when {
                GearRules.equipSlots.contains(normalizedType) -> normalizedType
                GearRules.isWeaponType(normalizedType) -> "weapon"
                else -> null
            } ?: return
            Equipment(
                slot = slotFromType,
                weaponType = normalizedType.takeIf { GearRules.isWeaponType(it) }
            )
        }
        if (!GearRules.matchesSlot(equipment, normalizedSlot, characterId, entry.item.type)) return
        sessionStore.setEquippedItem(normalizedSlot, entry.item.id, characterId)
    }

    fun equipMod(slotId: String, itemId: String?, characterId: String? = null) {
        val normalizedSlot = slotId.trim().lowercase(Locale.getDefault())
        if (normalizedSlot.isBlank()) return
        if (itemId.isNullOrBlank()) {
            sessionStore.setEquippedItem(normalizedSlot, null, characterId)
            return
        }
        val entry = entries.value.firstOrNull { it.item.id == itemId } ?: return
        val normalizedType = entry.item.type.trim().lowercase(Locale.getDefault())
        val isMod = normalizedType == "mod" || entry.item.equipment?.slot?.equals("mod", true) == true
        if (!isMod) return
        sessionStore.setEquippedItem(normalizedSlot, entry.item.id, characterId)
    }

    fun weaponItem(id: String): Item? = inventoryService.itemDetail(id)

    fun armorItem(id: String): Item? = inventoryService.itemDetail(id)

    fun equipWeapon(characterId: String, weaponId: String?) {
        val normalizedCharacter = characterId.trim().lowercase(Locale.getDefault())
        if (normalizedCharacter.isBlank()) return
        if (weaponId.isNullOrBlank()) {
            sessionStore.setEquippedWeapon(normalizedCharacter, null)
            return
        }
        val unlocked = unlockedWeapons.value
        val resolvedWeaponId = unlocked.firstOrNull { it.equals(weaponId, ignoreCase = true) } ?: return
        val item = inventoryService.itemDetail(resolvedWeaponId) ?: return
        if (!isWeaponForCharacter(item, normalizedCharacter)) return
        sessionStore.setEquippedWeapon(normalizedCharacter, resolvedWeaponId)
    }

    fun equipArmor(characterId: String, armorId: String?) {
        val normalizedCharacter = characterId.trim().lowercase(Locale.getDefault())
        if (normalizedCharacter.isBlank()) return
        if (armorId.isNullOrBlank()) {
            sessionStore.setEquippedArmor(normalizedCharacter, null)
            return
        }
        val unlocked = unlockedArmors.value
        val resolvedArmorId = unlocked.firstOrNull { it.equals(armorId, ignoreCase = true) } ?: return
        val item = inventoryService.itemDetail(resolvedArmorId) ?: return
        if (!item.isArmorItem()) return
        if (!isArmorForCharacter(item, normalizedCharacter)) return
        sessionStore.setEquippedArmor(normalizedCharacter, resolvedArmorId)
    }

    private fun isWeaponForCharacter(item: Item, characterId: String): Boolean {
        val expectedType = GearRules.allowedWeaponTypeFor(characterId) ?: return true
        val weaponType = item.equipment?.weaponType
            ?.trim()
            ?.lowercase(Locale.getDefault())
            ?: item.type.trim().lowercase(Locale.getDefault())
        return weaponType == expectedType
    }

    private fun isArmorForCharacter(item: Item, characterId: String): Boolean {
        val expectedType = GearRules.allowedArmorTypeFor(characterId) ?: return true
        val armorType = item.type.trim().lowercase(Locale.getDefault())
        if (!GearRules.isArmorType(armorType)) return false
        return armorType == expectedType
    }
}

class InventoryViewModelFactory(
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService,
    private val sessionStore: GameSessionStore,
    private val playerRoster: List<Player>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            return InventoryViewModel(inventoryService, craftingService, sessionStore, playerRoster) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class PartyMemberStatus(
    val id: String,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val portraitPath: String?
)

private fun com.example.starborn.domain.model.Item.isWeaponItem(): Boolean {
    val normalizedType = type.trim().lowercase(Locale.getDefault())
    return normalizedType == "weapon" ||
        GearRules.isWeaponType(normalizedType) ||
        equipment?.slot?.equals("weapon", ignoreCase = true) == true
}

private fun com.example.starborn.domain.model.Item.isArmorItem(): Boolean {
    val normalizedType = type.trim().lowercase(Locale.getDefault())
    return normalizedType == "armor" ||
        equipment?.slot?.equals("armor", ignoreCase = true) == true
}
