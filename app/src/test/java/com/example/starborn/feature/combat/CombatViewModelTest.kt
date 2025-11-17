package com.example.starborn.feature.combat

import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.CombatOutcome
import com.example.starborn.domain.combat.CombatReward
import com.example.starborn.domain.combat.CombatSide
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.Combatant
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.domain.combat.StatusRegistry
import com.example.starborn.domain.combat.TurnSlot
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.domain.leveling.LevelingData
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.leveling.ProgressionData
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Resistances
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class CombatViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun consumeLevelUpSummariesClearsQueue() {
        val player = Player(
            id = "nova",
            name = "Nova",
            level = 1,
            xp = 0,
            hp = 120,
            strength = 10,
            vitality = 8,
            agility = 6,
            focus = 5,
            luck = 4,
            skills = listOf("nova_strike"),
            miniIconPath = "images/portraits/nova.png"
        )
        val enemy = Enemy(
            id = "scrap_bandit",
            name = "Scrap Bandit",
            tier = "normal",
            hp = 90,
            strength = 8,
            vitality = 6,
            agility = 5,
            focus = 3,
            luck = 2,
            speed = 4,
            element = "physical",
            resistances = Resistances(),
            abilities = emptyList(),
            flavor = "A scavenger looking for trouble.",
            xpReward = 40,
            creditReward = 20,
            drops = emptyList(),
            description = "An opportunistic raider.",
            portrait = "",
            sprite = emptyList(),
            attack = 12,
            apReward = 1
        )
        val skill = Skill(
            id = "nova_strike",
            name = "Nova Strike",
            character = player.id,
            type = "attack",
            basePower = 20,
            cooldown = 0,
            description = "A swift strike."
        )

        val worldAssets = mock<WorldAssetDataSource> {
            on { loadCharacters() } doReturn listOf(player)
            on { loadEnemies() } doReturn listOf(enemy)
            on { loadSkills() } doReturn listOf(skill)
        }

        val combatantPlayer = Combatant(
            id = player.id,
            name = player.name,
            side = CombatSide.PLAYER,
            stats = com.example.starborn.domain.combat.StatBlock(
                maxHp = player.hp,
                maxRp = player.hp,
                strength = player.strength,
                vitality = player.vitality,
                agility = player.agility,
                focus = player.focus,
                luck = player.luck,
                speed = player.agility
            ),
            skills = player.skills
        )
        val combatantEnemy = Combatant(
            id = enemy.id,
            name = enemy.name,
            side = CombatSide.ENEMY,
            stats = com.example.starborn.domain.combat.StatBlock(
                maxHp = enemy.hp,
                maxRp = enemy.vitality,
                strength = enemy.strength,
                vitality = enemy.vitality,
                agility = enemy.agility,
                focus = enemy.focus,
                luck = enemy.luck,
                speed = enemy.speed
            ),
            skills = enemy.abilities
        )

        val initialState = CombatState(
            turnOrder = listOf(TurnSlot(combatantPlayer.id, 10)),
            activeTurnIndex = 0,
            combatants = mapOf(
                combatantPlayer.id to CombatantState(combatantPlayer, combatantPlayer.stats.maxHp, combatantPlayer.stats.maxRp),
                combatantEnemy.id to CombatantState(combatantEnemy, combatantEnemy.stats.maxHp, combatantEnemy.stats.maxRp)
            ),
            outcome = CombatOutcome.Victory(CombatReward())
        )

        val combatEngine = mock<CombatEngine> {
            on { beginEncounter(any()) } doReturn initialState
        }

        val inventoryService = InventoryService(FakeItemCatalog()).apply { loadItems() }

        val themeRepository = mock<ThemeRepository> {
            on { getTheme(any()) } doReturn null
            on { getStyle(any()) } doReturn null
        }
        val environmentThemeManager = EnvironmentThemeManager(themeRepository)

        val viewModel = CombatViewModel(
            worldAssets = worldAssets,
            combatEngine = combatEngine,
            statusRegistry = StatusRegistry(),
            sessionStore = GameSessionStore(),
            inventoryService = inventoryService,
            levelingManager = LevelingManager(LevelingData(mapOf("1" to 0, "2" to 100))),
            progressionData = ProgressionData(),
            audioRouter = AudioRouter(AudioBindings()),
            themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager,
            enemyIds = listOf(enemy.id)
        )

        val field = CombatViewModel::class.java.getDeclaredField("pendingLevelUps")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val pending = field.get(viewModel) as MutableList<LevelUpSummary>
        pending += LevelUpSummary(
            characterId = player.id,
            characterName = player.name,
            levelsGained = 1,
            newLevel = 2,
            unlockedSkills = emptyList()
        )

        val first = viewModel.consumeLevelUpSummaries()
        assertEquals(1, first.size)

        val second = viewModel.consumeLevelUpSummaries()
        assertTrue(second.isEmpty())
    }

    private class FakeItemCatalog : ItemCatalog {
        override fun load() = Unit
        override fun findItem(idOrAlias: String): Item? = null
    }
}
