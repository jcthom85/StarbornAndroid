package com.example.starborn.feature.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ShopDefinition
import com.example.starborn.domain.model.ShopGate
import com.example.starborn.domain.model.ShopDialogueLine
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.shop.ShopCatalog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

enum class ShopTab {
    BUY,
    SELL
}

data class ShopUiState(
    val isLoading: Boolean = true,
    val shopName: String = "",
    val portraitPath: String? = null,
    val credits: Int = 0,
    val itemsForSale: List<ShopItemUi> = emptyList(),
    val sellInventory: List<SellItemUi> = emptyList(),
    val unavailableMessage: String? = null,
    val activeTab: ShopTab = ShopTab.BUY,
    val smalltalkTopics: List<ShopDialogueTopicUi> = emptyList(),
    val conversationLog: List<ShopDialogueLineUi> = emptyList()
)

data class ShopItemUi(
    val id: String,
    val name: String,
    val description: String?,
    val price: Int,
    val canAfford: Boolean,
    val locked: Boolean,
    val lockedMessage: String?,
    val maxQuantity: Int,
    val rotating: Boolean = false
)

data class SellItemUi(
    val id: String,
    val name: String,
    val description: String?,
    val quantity: Int,
    val price: Int,
    val canSell: Boolean,
    val reason: String?
)

 data class ShopDialogueTopicUi(
    val id: String,
    val label: String,
    val voiceCue: String? = null
)

 data class ShopDialogueLineUi(
    val id: String,
    val speaker: String?,
    val text: String,
    val voiceCue: String?
)


class ShopViewModel(
    private val shopId: String,
    private val shopCatalog: ShopCatalog,
    private val itemCatalog: ItemCatalog,
    private val inventoryService: InventoryService,
    private val sessionStore: GameSessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var rotatingItems: Set<String> = emptySet()
    private var definition: ShopDefinition? = null
    private var latestSessionState: GameSessionState = sessionStore.state.value
    private var latestInventory: List<InventoryEntry> = inventoryService.state.value

    init {
        loadShop()
        observeSession()
        observeInventory()
    }

    private fun loadShop() {
        val shop = shopCatalog.shopById(shopId)
        if (shop == null) {
            _uiState.value = ShopUiState(
                isLoading = false,
                shopName = "Unknown Shop",
                unavailableMessage = "Shop data unavailable."
            )
            return
        }
        definition = shop
        val prefaceLog = shop.dialogue?.preface.orEmpty().mapIndexed { index, line ->
            line.toUi("${shop.id}_preface_$index")
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                shopName = shop.name,
                portraitPath = shop.portrait,
                credits = sessionStore.state.value.playerCredits,
                unavailableMessage = null,
                smalltalkTopics = shop.dialogue?.smalltalk.orEmpty().map { topic ->
                    ShopDialogueTopicUi(
                        id = topic.id,
                        label = topic.label,
                        voiceCue = topic.voiceCue
                    )
                },
                conversationLog = prefaceLog
            )
        }
        refreshState(sessionStore.state.value, inventoryService.state.value)
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionStore.state.collect { state ->
                refreshState(state, latestInventory)
            }
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryService.state.collect { entries ->
                refreshState(latestSessionState, entries)
            }
        }
    }

    private fun refreshState(session: GameSessionState, inventory: List<InventoryEntry>) {
        latestSessionState = session
        latestInventory = inventory
        val shop = definition ?: return
        val rotationSelection = selectRotatingItems(shop)
        rotatingItems = rotationSelection
        val items = buildItems(shop, session, rotationSelection)
        val sellItems = buildSellItems(shop, inventory)
        _uiState.update {
            it.copy(
                credits = session.playerCredits,
                itemsForSale = items,
                sellInventory = sellItems
            )
        }
    }

    private fun buildItems(shop: ShopDefinition, state: GameSessionState, rotating: Set<String>): List<ShopItemUi> {
        val orderedIds = (shop.sells.items + rotating).distinct()
        if (orderedIds.isEmpty()) return emptyList()
        return orderedIds.mapNotNull { idOrAlias ->
            val item = itemCatalog.findItem(idOrAlias) ?: return@mapNotNull null
            val gate = findGateForItem(shop, item)
            val locked = gate?.milestones?.any { it !in state.completedMilestones } == true
            val lockedMessage = if (locked) {
                val milestones = gate?.milestones.orEmpty().filter { it.isNotBlank() }
                if (milestones.isNotEmpty()) "Requires ${milestones.joinToString()}" else "Currently unavailable"
            } else null
            val price = priceFor(item, shop)
            ShopItemUi(
                id = item.id,
                name = item.name,
                description = item.description,
                price = price,
                canAfford = !locked && state.playerCredits >= price,
                locked = locked,
                lockedMessage = lockedMessage,
                maxQuantity = if (locked || price <= 0) 0 else state.playerCredits / price,
                rotating = rotating.contains(idOrAlias)
            )
        }
    }

    private fun buildSellItems(
        shop: ShopDefinition,
        inventory: List<InventoryEntry>
    ): List<SellItemUi> {
        if (inventory.isEmpty()) return emptyList()
        val markdown = shop.pricing?.buyMarkdown ?: DEFAULT_BUY_MARKDOWN
        return inventory.map { entry ->
            val item = entry.item
            val basePrice = max(item.value, item.buyPrice ?: 0)
            val price = max(1, (basePrice * markdown).roundToInt())
            val isBlacklisted = shop.buys?.blacklist?.any { it.equals(item.id, ignoreCase = true) || it.equals(item.name, ignoreCase = true) } == true
            val typeAllowed = shop.buys?.acceptTypes.isNullOrEmpty() || shop.buys?.acceptTypes?.any { it.equals(item.type, ignoreCase = true) } == true
            val canSell = !item.unsellable && !isBlacklisted && typeAllowed && price > 0
            val reason = when {
                item.unsellable -> "Cannot sell this item."
                isBlacklisted -> "Dealer won't take this."
                !typeAllowed -> "Dealer isn't buying this category."
                price <= 0 -> "No resale value."
                else -> null
            }
            SellItemUi(
                id = item.id,
                name = item.name,
                description = item.description,
                quantity = entry.quantity,
                price = price,
                canSell = canSell,
                reason = reason
            )
        }.sortedBy { it.name }
    }

    fun buyItem(itemId: String, quantity: Int) {
        if (quantity <= 0) {
            emitMessage("Choose at least one item to purchase.")
            return
        }
        val shop = definition ?: return
        val state = latestSessionState
        val item = itemCatalog.findItem(itemId)
        if (item == null) {
            emitMessage("That item is no longer available.")
            return
        }
        val gate = findGateForItem(shop, item)
        if (gate?.milestones?.any { it !in state.completedMilestones } == true) {
            emitMessage("${item.name} isn't available yet.")
            return
        }
        val price = priceFor(item, shop)
        val totalCost = price * quantity
        if (!sessionStore.spendCredits(totalCost)) {
            emitMessage("Not enough credits for that purchase.")
            return
        }
        inventoryService.addItem(item.id, quantity)
        val label = if (quantity == 1) item.name else "${item.name} x$quantity"
        emitMessage("Purchased $label for $totalCost credits.")
        refreshState(sessionStore.state.value, latestInventory)
    }

    private fun selectRotatingItems(shop: ShopDefinition): Set<String> {
        val pool = shop.sells.rotationPool.distinct()
        val count = shop.sells.rotationSize ?: 0
        if (pool.isEmpty() || count <= 0) return emptySet()
        if (pool.size <= count) return pool.toSet()
        val day = System.currentTimeMillis() / 86_400_000L
        val seedBase = (shop.sells.rotationSeed ?: shop.id).hashCode().toLong()
        val random = java.util.Random(seedBase + day)
        return pool.shuffled(random).take(count).toSet()
    }

    fun playSmalltalk(topicId: String) {
        val dialogue = definition?.dialogue ?: return
        val topic = dialogue.smalltalk.firstOrNull { it.id == topicId } ?: return
        if (topic.response.isEmpty()) {
            emitMessage("They have nothing else to discuss right now.")
            return
        }
        _uiState.update { state ->
            state.copy(
                conversationLog = topic.response.mapIndexed { index, line ->
                    line.toUi("${shopId}_${topic.id}_$index")
                }
            )
        }
    }

    fun sellItem(itemId: String, quantity: Int) {
        if (quantity <= 0) {
            emitMessage("Choose at least one item to sell.")
            return
        }
        val shop = definition ?: return
        val entry = latestInventory.firstOrNull { it.item.id == itemId }
        if (entry == null) {
            emitMessage("Nothing to sell.")
            return
        }
        if (quantity > entry.quantity) {
            emitMessage("You only have ${entry.quantity} in stock.")
            return
        }
        val sellUi = buildSellItems(shop, listOf(entry)).firstOrNull()
        if (sellUi == null || !sellUi.canSell) {
            emitMessage(sellUi?.reason ?: "Dealer won't buy that.")
            return
        }
        inventoryService.removeItem(itemId, quantity)
        val earnings = sellUi.price * quantity
        sessionStore.addCredits(earnings)
        val label = if (quantity == 1) entry.item.name else "${entry.item.name} x$quantity"
        emitMessage("Sold $label for $earnings credits.")
        refreshState(sessionStore.state.value, latestInventory)
    }

    fun switchTab(tab: ShopTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    private fun priceFor(item: Item, shop: ShopDefinition): Int {
        val base = (item.buyPrice ?: item.value).coerceAtLeast(1)
        val markup = shop.pricing?.sellMarkup ?: 1.0
        return max(1, (base * markup).roundToInt())
    }

    private fun findGateForItem(shop: ShopDefinition, item: Item): ShopGate? {
        val gates = shop.sells.gates
        if (gates.isEmpty()) return null
        return gates[item.id] ?: gates[item.name]
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }

    companion object {
        private const val DEFAULT_BUY_MARKDOWN = 0.35
    }
}

private fun ShopDialogueLine.toUi(id: String): ShopDialogueLineUi = ShopDialogueLineUi(
    id = id,
    speaker = speaker,
    text = text,
    voiceCue = voiceCue
)
