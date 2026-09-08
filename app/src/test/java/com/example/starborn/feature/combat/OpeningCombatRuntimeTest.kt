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
import kotlinx.coroutines.flow.MutableStateFlow
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

    @Test fun `combat rewards preserve a level earned under the previous curve`() {
        val session = GameSessionStore().apply {
            restore(GameSessionState(worldId = "world_1", roomId = "workshop_yard",
                playerId = "nova", partyMembers = listOf("nova"), playerLevel = 12,
                partyMemberLevels = mapOf("nova" to 12), playerXp = 4900,
                partyMemberXp = mapOf("nova" to 4900)))
        }
        val vm = createCombat(session)
        try {
            val method = CombatViewModel::class.java.getDeclaredMethod("applyVictoryRewards", CombatReward::class.java)
            method.isAccessible = true
            method.invoke(vm, CombatReward(xp = 10))
            assertEquals(12, session.state.value.playerLevel)
            assertEquals(12, session.state.value.partyMemberLevels["nova"])
            assertEquals(4910, session.state.value.playerXp)
        } finally { vm.viewModelScope.cancel() }
    }

    // Estimated World 4 main-route checkpoint, not a campaign-earned equipment
    // snapshot. Keep optional gear absent and exclude unearned level-12 skills.
    private fun world4Session(): GameSessionStore {
        val party = listOf("nova", "zeke", "orion", "gh0st")
        return GameSessionStore().apply {
            restore(GameSessionState(worldId = "world_4", playerId = "nova",
                partyMembers = party, playerLevel = 9, playerXp = 11670,
                partyMemberLevels = party.associateWith { 9 },
                partyMemberXp = party.associateWith { 11670 },
                unlockedSkills = setOf("nova_smoke_bomb", "nova_plasma_burst",
                    "zeke_bulwark_stance", "zeke_overload_fists",
                    "orion_nano_repair", "orion_disruption_pulse",
                    "gh0st_venom_edge", "gh0st_system_crash")))
        }
    }

    @Test fun `world four party exposes earned skill tiers against Titan Walker`() {
        val session = world4Session()
        val vm = createCombat(session, listOf("titan_walker_boss"))
        try {
            val party = requireNotNull(vm.combatState).combatants.values
                .filter { it.combatant.side == CombatSide.PLAYER }
            assertEquals(session.state.value.partyMembers.toSet(), party.map { it.combatant.id }.toSet())
            session.state.value.partyMembers.forEach { id ->
                val expected = session.state.value.unlockedSkills.filter { it.startsWith("${id}_") }.toSet() +
                    assets.loadCharacters().single { it.id == id }.skills
                assertEquals("Earned skills for $id", expected, vm.skillsForPlayer(id).map { it.id }.toSet())
            }
            assertEquals(listOf("titan_walker_boss"), vm.enemies.map { it.id })
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `world four Orion can execute his level nine command through production ATB`() {
        val vm = createCombat(world4Session(), listOf("titan_walker_boss"))
        try {
            val skill = vm.skillsForPlayer("orion").single { it.id == "orion_disruption_pulse" }
            repeat(80) {
                dispatcher.scheduler.advanceTimeBy(250)
                dispatcher.scheduler.runCurrent()
                vm.selectReadyPlayer("orion")
                if (vm.awaitingAction.value == "orion") {
                    assertTrue(vm.canUseSkill("orion", skill))
                    val before = vm.combatState
                    vm.useSkill(skill, listOf("titan_walker_boss"))
                    dispatcher.scheduler.runCurrent()
                    assertNotEquals(before, vm.combatState)
                    return
                }
            }
            fail("Orion never became selectable during 20 seconds of production ATB")
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `wounded authored support enemy heals through production ATB and targeting`() {
        val vm = createCombat(enemyIds = listOf("stalker_vine"))
        try {
            dispatcher.scheduler.runCurrent()
            val initial = requireNotNull(vm.combatState)
            val enemy = initial.combatants.getValue("stalker_vine")
            // Seed only the injury, not AI choices, targeting or turn execution.
            // Reflection keeps a test-only mutation hook out of the public API.
            val field = CombatViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val state = field.get(vm) as MutableStateFlow<CombatState?>
            state.value = initial.copy(combatants = initial.combatants +
                ("stalker_vine" to enemy.copy(hp = 10)))
            repeat(240) {
                dispatcher.scheduler.advanceTimeBy(250)
                dispatcher.scheduler.runCurrent()
                val healed = requireNotNull(vm.combatState).combatants.getValue("stalker_vine")
                if (healed.hp > 10) {
                    assertTrue("Healing must respect maximum HP", healed.hp <= healed.combatant.stats.maxHp)
                    assertTrue("Heal must enter cooldown", healed.activeCooldowns.getOrDefault("nature_heal", 0) > 0)
                    return
                }
            }
            fail("Wounded Stalker-Vine did not heal during 60 seconds of production ATB")
        } finally { vm.viewModelScope.cancel() }
    }

    private fun createCombat(initialSession: GameSessionStore? = null,
                             enemyIds: List<String> = listOf("faulted_loader")): CombatViewModel {
        val items = reader.readList<Item>("items.json").associateBy { it.id }
        check(items.isNotEmpty())
        val catalog = object : ItemCatalog {
            override fun load() = Unit
            override fun findItem(idOrAlias: String): Item? = items[idOrAlias]
        }
        val session = initialSession ?: GameSessionStore().apply {
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
            encounterCoordinator = EncounterCoordinator(), enemyIds = enemyIds,
            tutorialsEnabled = false, elapsedRealtime = { dispatcher.scheduler.currentTime }
        )
    }
}
