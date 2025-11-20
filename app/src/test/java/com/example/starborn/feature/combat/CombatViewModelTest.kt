package com.example.starborn.feature.combat

import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.CombatOutcome
import com.example.starborn.domain.combat.CombatReward
import com.example.starborn.domain.combat.CombatSetup
import com.example.starborn.domain.combat.CombatSide
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.EncounterCoordinator
import com.example.starborn.domain.combat.EncounterDescriptor
import com.example.starborn.domain.combat.EncounterEnemyInstance
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
import java.util.Locale
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

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
            itemCatalog = FakeItemCatalog(),
            levelingManager = LevelingManager(LevelingData(mapOf("1" to 0, "2" to 100))),
            progressionData = ProgressionData(),
            audioRouter = AudioRouter(AudioBindings()),
            themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager,
            encounterCoordinator = EncounterCoordinator(),
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

    @Test
    fun lootDropsAreAddedToInventoryOnVictory() {
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

        val expectedLoot = listOf(
            com.example.starborn.domain.combat.LootDrop("item_a", 2),
            com.example.starborn.domain.combat.LootDrop("item_b", 1)
        )
        val combatRewardWithLoot = CombatReward(
            xp = 100,
            ap = 10,
            credits = 50,
            drops = expectedLoot
        )

        val combatEngine = mock<CombatEngine>()
        val inventoryService = mock<InventoryService>()
        val gameSessionStore = GameSessionStore()

        val themeRepository = mock<ThemeRepository> {
            on { getTheme(any()) } doReturn null
            on { getStyle(any()) } doReturn null
        }
        val environmentThemeManager = EnvironmentThemeManager(themeRepository)

        val viewModel = CombatViewModel(
            worldAssets = worldAssets,
            combatEngine = combatEngine,
            statusRegistry = StatusRegistry(),
            sessionStore = gameSessionStore,
            inventoryService = inventoryService,
            itemCatalog = FakeItemCatalog(),
            levelingManager = LevelingManager(LevelingData(mapOf("1" to 0, "2" to 100))),
            progressionData = ProgressionData(),
            audioRouter = AudioRouter(AudioBindings()),
            themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager,
            encounterCoordinator = EncounterCoordinator(),
            enemyIds = listOf(enemy.id)
        )

        // Use reflection to call the private applyVictoryRewards method
        val method = CombatViewModel::class.java.getDeclaredMethod(
            "applyVictoryRewards",
            CombatReward::class.java
        )
        method.isAccessible = true
        method.invoke(viewModel, combatRewardWithLoot)

        // Verify that addItem was called for each loot drop
        expectedLoot.forEach { drop ->
            verify(inventoryService).addItem(drop.itemId, drop.quantity)
        }
    }

    @Test
    fun victoryRewardCanonicalizesLootIds() {
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
            id = "fume_bat",
            name = "Fume Bat",
            tier = "standard",
            hp = 20,
            strength = 4,
            vitality = 2,
            agility = 3,
            focus = 1,
            luck = 2,
            speed = 5,
            element = "fire",
            resistances = Resistances(),
            abilities = emptyList(),
            flavor = "",
            xpReward = 10,
            creditReward = 5,
            drops = listOf(
                com.example.starborn.domain.model.Drop(
                    id = "fiery pepper",
                    chance = 1.0,
                    quantity = 1
                )
            ),
            description = "",
            portrait = "",
            sprite = emptyList(),
            attack = 6,
            apReward = 0
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
                combatantPlayer.id to CombatantState(
                    combatantPlayer,
                    combatantPlayer.stats.maxHp,
                    combatantPlayer.stats.maxRp
                ),
                combatantEnemy.id to CombatantState(
                    combatantEnemy,
                    combatantEnemy.stats.maxHp,
                    combatantEnemy.stats.maxRp
                )
            )
        )

        val combatEngine = mock<CombatEngine> {
            on { beginEncounter(any()) } doReturn initialState
        }
        val inventoryService = InventoryService(SingleItemCatalog()).apply { loadItems() }
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
            itemCatalog = SingleItemCatalog(),
            levelingManager = LevelingManager(LevelingData(mapOf("1" to 0, "2" to 100))),
            progressionData = ProgressionData(),
            audioRouter = AudioRouter(AudioBindings()),
            themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager,
            encounterCoordinator = EncounterCoordinator(),
            enemyIds = listOf(enemy.id)
        )

        val method = CombatViewModel::class.java.getDeclaredMethod("victoryReward")
        method.isAccessible = true
        val reward = method.invoke(viewModel) as CombatReward

        assertTrue(reward.drops.any { it.itemId == "fiery_pepper" })
    }

    @Test
    fun victoryRewardSkipsDropsWhenChanceFails() {
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
            id = "fume_bat",
            name = "Fume Bat",
            tier = "standard",
            hp = 20,
            strength = 4,
            vitality = 2,
            agility = 3,
            focus = 1,
            luck = 2,
            speed = 5,
            element = "fire",
            resistances = Resistances(),
            abilities = emptyList(),
            flavor = "",
            xpReward = 10,
            creditReward = 5,
            drops = listOf(
                com.example.starborn.domain.model.Drop(
                    id = "fiery pepper",
                    chance = 0.0,
                    quantity = 1
                )
            ),
            description = "",
            portrait = "",
            sprite = emptyList(),
            attack = 6,
            apReward = 0
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
                combatantPlayer.id to CombatantState(
                    combatantPlayer,
                    combatantPlayer.stats.maxHp,
                    combatantPlayer.stats.maxRp
                ),
                combatantEnemy.id to CombatantState(
                    combatantEnemy,
                    combatantEnemy.stats.maxHp,
                    combatantEnemy.stats.maxRp
                )
            )
        )

        val combatEngine = mock<CombatEngine> {
            on { beginEncounter(any()) } doReturn initialState
        }
        val inventoryService = InventoryService(SingleItemCatalog()).apply { loadItems() }
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
            itemCatalog = SingleItemCatalog(),
            levelingManager = LevelingManager(LevelingData(mapOf("1" to 0, "2" to 100))),
            progressionData = ProgressionData(),
            audioRouter = AudioRouter(AudioBindings()),
            themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager,
            encounterCoordinator = EncounterCoordinator(),
            enemyIds = listOf(enemy.id)
        )

        val method = CombatViewModel::class.java.getDeclaredMethod("victoryReward")
        method.isAccessible = true
        val reward = method.invoke(viewModel) as CombatReward

        assertTrue(reward.drops.isEmpty())
    }

    @Test
    fun encounterExtraDropsAreGrantedToSpecificInstance() {
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
            id = "fume_bat",
            name = "Fume Bat",
            tier = "standard",
            hp = 20,
            strength = 4,
            vitality = 2,
            agility = 3,
            focus = 1,
            luck = 2,
            speed = 5,
            element = "fire",
            resistances = Resistances(),
            abilities = emptyList(),
            flavor = "",
            xpReward = 10,
            creditReward = 5,
            drops = emptyList(),
            description = "",
            portrait = "",
            sprite = emptyList(),
            attack = 6,
            apReward = 0
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
                combatantPlayer.id to CombatantState(
                    combatantPlayer,
                    combatantPlayer.stats.maxHp,
                    combatantPlayer.stats.maxRp
                ),
                combatantEnemy.id to CombatantState(
                    combatantEnemy,
                    combatantEnemy.stats.maxHp,
                    combatantEnemy.stats.maxRp
                )
            )
        )

        val combatEngine = mock<CombatEngine> {
            on { beginEncounter(any()) } doReturn initialState
        }
        val inventoryService = InventoryService(SingleItemCatalog()).apply { loadItems() }
        val themeRepository = mock<ThemeRepository> {
            on { getTheme(any()) } doReturn null
            on { getStyle(any()) } doReturn null
        }
        val environmentThemeManager = EnvironmentThemeManager(themeRepository)
        val encounterCoordinator = EncounterCoordinator().apply {
            setPendingEncounter(
                EncounterDescriptor(
                    enemies = listOf(
                        EncounterEnemyInstance(
                            enemyId = enemy.id,
                            extraDrops = listOf(
                                com.example.starborn.domain.model.Drop(
                                    id = "mine_keycard",
                                    chance = 1.0,
                                    quantity = 1
                                )
                            )
                        )
                    )
                )
            )
        }

        val viewModel = CombatViewModel(
            worldAssets = worldAssets,
            combatEngine = combatEngine,
            statusRegistry = StatusRegistry(),
            sessionStore = GameSessionStore(),
            inventoryService = inventoryService,
            itemCatalog = SingleItemCatalog(),
            levelingManager = LevelingManager(LevelingData(mapOf("1" to 0, "2" to 100))),
            progressionData = ProgressionData(),
            audioRouter = AudioRouter(AudioBindings()),
            themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager,
            encounterCoordinator = encounterCoordinator,
            enemyIds = listOf(enemy.id)
        )

        val method = CombatViewModel::class.java.getDeclaredMethod("victoryReward")
        method.isAccessible = true
        val reward = method.invoke(viewModel) as CombatReward

        assertTrue(reward.drops.any { it.itemId == "mine_keycard" })
    }

    @Test
    fun encounterOverrideDropsReplaceBaseTable() {
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
            id = "fume_bat",
            name = "Fume Bat",
            tier = "standard",
            hp = 20,
            strength = 4,
            vitality = 2,
            agility = 3,
            focus = 1,
            luck = 2,
            speed = 5,
            element = "fire",
            resistances = Resistances(),
            abilities = emptyList(),
            flavor = "",
            xpReward = 10,
            creditReward = 5,
            drops = listOf(
                com.example.starborn.domain.model.Drop(
                    id = "fiery pepper",
                    chance = 1.0,
                    quantity = 1
                )
            ),
            description = "",
            portrait = "",
            sprite = emptyList(),
            attack = 6,
            apReward = 0
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
                combatantPlayer.id to CombatantState(
                    combatantPlayer,
                    combatantPlayer.stats.maxHp,
                    combatantPlayer.stats.maxRp
                ),
                combatantEnemy.id to CombatantState(
                    combatantEnemy,
                    combatantEnemy.stats.maxHp,
                    combatantEnemy.stats.maxRp
                )
            )
        )

        val combatEngine = mock<CombatEngine> {
            on { beginEncounter(any()) } doReturn initialState
        }
        val inventoryService = InventoryService(SingleItemCatalog()).apply { loadItems() }
        val themeRepository = mock<ThemeRepository> {
            on { getTheme(any()) } doReturn null
            on { getStyle(any()) } doReturn null
        }
        val environmentThemeManager = EnvironmentThemeManager(themeRepository)
        val encounterCoordinator = EncounterCoordinator().apply {
            setPendingEncounter(
                EncounterDescriptor(
                    enemies = listOf(
                        EncounterEnemyInstance(
                            enemyId = enemy.id,
                            overrideDrops = listOf(
                                com.example.starborn.domain.model.Drop(
                                    id = "mine_keycard",
                                    chance = 1.0,
                                    quantity = 1
                                )
                            )
                        )
                    )
                )
            )
        }

        val viewModel = CombatViewModel(
            worldAssets = worldAssets,
            combatEngine = combatEngine,
            statusRegistry = StatusRegistry(),
            sessionStore = GameSessionStore(),
            inventoryService = inventoryService,
            itemCatalog = SingleItemCatalog(),
            levelingManager = LevelingManager(LevelingData(mapOf("1" to 0, "2" to 100))),
            progressionData = ProgressionData(),
            audioRouter = AudioRouter(AudioBindings()),
            themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager,
            encounterCoordinator = encounterCoordinator,
            enemyIds = listOf(enemy.id)
        )

        val method = CombatViewModel::class.java.getDeclaredMethod("victoryReward")
        method.isAccessible = true
        val reward = method.invoke(viewModel) as CombatReward

        assertTrue(reward.drops.any { it.itemId == "mine_keycard" })
        assertTrue(reward.drops.none { it.itemId == "fiery_pepper" })
    }

    @Test
    fun duplicateEnemyIdsCreateUniqueCombatantIdsAndCanonicalResults() {
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
            on { loadRooms() } doReturn emptyList()
            on { loadSkillNodes() } doReturn emptyMap()
        }

        var capturedSetup: CombatSetup? = null
        val combatEngine = mock<CombatEngine> {
            on { beginEncounter(any()) } doAnswer { invocation ->
                val setup = invocation.arguments[0] as CombatSetup
                capturedSetup = setup
                val combatants = setup.allCombatants.associate { combatant ->
                    combatant.id to CombatantState(
                        combatant = combatant,
                        hp = combatant.stats.maxHp,
                        rp = combatant.stats.maxRp
                    )
                }
                CombatState(
                    turnOrder = setup.allCombatants.mapIndexed { index, combatant ->
                        TurnSlot(combatant.id, setup.allCombatants.size - index)
                    },
                    activeTurnIndex = 0,
                    combatants = combatants
                )
            }
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
            itemCatalog = FakeItemCatalog(),
            levelingManager = LevelingManager(LevelingData(mapOf("1" to 0, "2" to 100))),
            progressionData = ProgressionData(),
            audioRouter = AudioRouter(AudioBindings()),
            themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager,
            encounterCoordinator = EncounterCoordinator(),
            enemyIds = listOf(enemy.id, enemy.id)
        )

        val encounterIds = viewModel.encounterEnemyIds
        assertEquals(listOf(enemy.id, enemy.id), encounterIds)

        val setupIds = capturedSetup?.enemyParty?.map { it.id }
        assertEquals(listOf("${enemy.id}#1", "${enemy.id}#2"), setupIds)
    }

    private class FakeItemCatalog : ItemCatalog {
        override fun load() = Unit
        override fun findItem(idOrAlias: String): Item? = null
    }

    private class SingleItemCatalog : ItemCatalog {
        override fun load() = Unit

        override fun findItem(idOrAlias: String): Item? {
            val key = idOrAlias.trim().lowercase(Locale.getDefault())
            return when (key) {
                "fiery_pepper", "fiery pepper" -> Item(
                    id = "fiery_pepper",
                    name = "Fiery Pepper",
                    type = "ingredient"
                )
                else -> null
            }
        }
    }
}
