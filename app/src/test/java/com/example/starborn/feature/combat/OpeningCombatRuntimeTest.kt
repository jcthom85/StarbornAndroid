package com.example.starborn.feature.combat

import androidx.lifecycle.viewModelScope
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.combat.*
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Attainable opening checkpoint: solo Nova, starting skills, no optional gear
 * equipped. This is not a campaign-earned reward snapshot or a balance audit.
 * Only presentation services are mocked; assets, mapping, ATB, commands and AI
 * are production code. Random outcomes are deliberately not win-rate assertions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OpeningCombatRuntimeTest {
    private val dispatcher = StandardTestDispatcher()
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val assets = WorldAssetDataSource(reader)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `opening fixture uses solo Nova and only her starting skill`() {
        val vm = createCombat()
        try {
            val state = requireNotNull(vm.combatState)
            val party = state.combatants.values.filter { it.combatant.side == CombatSide.PLAYER }
            assertEquals(listOf("nova"), party.map { it.combatant.id })
            assertEquals(setOf("nova_arc_tether"), vm.skillsForPlayer("nova").map { it.id }.toSet())
            val nova = assets.loadCharacters().single { it.id == "nova" }
            assertEquals(nova.strength, party.single().combatant.stats.strength)
            assertEquals(CombatFormulas.maxHp(nova.hp, nova.vitality), party.single().hp)
            assertTrue(vm.skillsForPlayer("zeke").isEmpty())
            assertEquals(listOf("faulted_loader"), vm.enemies.map { it.id })
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `production ATB lets the loader act without player input`() {
        val vm = createCombat()
        try {
            val initial = requireNotNull(vm.combatState)
            val initialHp = initial.combatants.getValue("nova").hp
            // A finite virtual-time window prevents an idle ATB loop hanging CI.
            repeat(240) {
                dispatcher.scheduler.advanceTimeBy(250)
                dispatcher.scheduler.runCurrent()
                if (requireNotNull(vm.combatState).combatants.getValue("nova").hp < initialHp) return
            }
            fail("The loader never damaged Nova during 60 seconds of production ATB")
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `starting skill command is accepted by production combat runtime`() {
        val vm = createCombat()
        try {
            val skill = vm.skillsForPlayer("nova").single()
            repeat(80) {
                dispatcher.scheduler.advanceTimeBy(250)
                dispatcher.scheduler.runCurrent()
                vm.selectReadyPlayer("nova")
                if (vm.awaitingAction.value == "nova") {
                    assertTrue(vm.canUseSkill("nova", skill))
                    val before = vm.combatState
                    vm.useSkill(skill, listOf("faulted_loader"))
                    dispatcher.scheduler.runCurrent()
                    assertNotEquals("A legal starting skill must change combat state", before, vm.combatState)
                    return
                }
            }
            fail("Nova never became selectable during 20 seconds of production ATB")
        } finally { vm.viewModelScope.cancel() }
    }

    private fun createCombat(): CombatViewModel {
        val items = reader.readList<Item>("items.json").associateBy { it.id }
        check(items.isNotEmpty())
        val catalog = object : ItemCatalog {
            override fun load() = Unit
            override fun findItem(idOrAlias: String): Item? = items[idOrAlias]
        }
        val session = GameSessionStore().apply {
            restore(GameSessionState(worldId = "world_1", roomId = "workshop_yard",
                playerId = "nova", partyMembers = listOf("nova"),
                unlockedSkills = setOf("nova_arc_tether")))
        }
        val registry = StatusRegistry(assets.loadStatuses())
        val themes = mock<ThemeRepository> {
            on { getTheme(any()) } doReturn null
            on { getStyle(any()) } doReturn null
        }
        return CombatViewModel(
            worldAssets = assets, combatEngine = CombatEngine(statusRegistry = registry),
            statusRegistry = registry, sessionStore = session,
            inventoryService = InventoryService(catalog).apply { loadItems() }, itemCatalog = catalog,
            levelingManager = LevelingManager(requireNotNull(assets.loadLevelingData())),
            progressionData = requireNotNull(assets.loadProgressionData()),
            audioRouter = AudioRouter(AudioBindings()), themeRepository = themes,
            environmentThemeManager = EnvironmentThemeManager(themes),
            encounterCoordinator = EncounterCoordinator(), enemyIds = listOf("faulted_loader"),
            tutorialsEnabled = false, elapsedRealtime = { dispatcher.scheduler.currentTime }
        )
    }
}
