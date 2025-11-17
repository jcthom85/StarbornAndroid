package com.example.starborn.feature.exploration

import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.MilestoneRepository
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.data.repository.ShopRepository
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.core.DispatcherProvider
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.audio.VoiceoverController
import com.example.starborn.domain.cinematic.CinematicCoordinator
import com.example.starborn.domain.cinematic.CinematicService
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.BlockedDirection
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.QuestStage
import com.example.starborn.domain.model.QuestTask
import com.example.starborn.domain.milestone.MilestoneRuntimeManager
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.GameSaveRepository
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.data.local.UserSettings
import com.example.starborn.data.local.UserSettingsStore
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.FullMapUiState
import com.example.starborn.feature.exploration.viewmodel.MinimapUiState
import com.example.starborn.feature.exploration.viewmodel.MenuTab
import com.example.starborn.ui.events.UiEventBus
import com.example.starborn.domain.prompt.TutorialPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var scope: CoroutineScope

    @Before
    fun setupDispatcher() {
        Dispatchers.setMain(dispatcher)
        scope = CoroutineScope(dispatcher)
    }

    @After
    fun tearDownDispatcher() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun enqueueLevelUpsPromotesSequentially() {
        val viewModel = createViewModel()

        val characterNova = Player(
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
            skills = emptyList(),
            miniIconPath = "images/portraits/nova.png"
        )
        val characterZeke = characterNova.copy(id = "zeke", name = "Zeke")

        ExplorationViewModel::class.java.getDeclaredField("charactersById").apply {
            isAccessible = true
            set(viewModel, mapOf(characterNova.id to characterNova, characterZeke.id to characterZeke))
        }
        ExplorationViewModel::class.java.getDeclaredField("skillsById").apply {
            isAccessible = true
            set(viewModel, emptyMap<String, Skill>())
        }

        val method = ExplorationViewModel::class.java.getDeclaredMethod(
            "enqueueLevelUps",
            List::class.java
        )
        method.isAccessible = true

        val first = LevelUpSummary(
            characterId = "nova",
            characterName = "Nova",
            levelsGained = 1,
            newLevel = 2,
            unlockedSkills = emptyList()
        )
        val second = LevelUpSummary(
            characterId = "zeke",
            characterName = "Zeke",
            levelsGained = 2,
            newLevel = 4,
            unlockedSkills = emptyList()
        )

        method.invoke(viewModel, listOf(first, second))

        val initialPrompt = viewModel.uiState.value.levelUpPrompt
        assertNotNull(initialPrompt)
        assertEquals("nova", initialPrompt?.characterId)

        viewModel.dismissLevelUpPrompt()
        val followUpPrompt = viewModel.uiState.value.levelUpPrompt
        assertNotNull(followUpPrompt)
        assertEquals("zeke", followUpPrompt?.characterId)

        viewModel.dismissLevelUpPrompt()
        assertNull(viewModel.uiState.value.levelUpPrompt)
    }

    @Test
    fun openingInventoryTabQueuesBagTutorial() {
        val viewModel = createViewModel()
        val promptManager = getPrivateField<UIPromptManager>(viewModel, "promptManager")
        val sessionStore = getPrivateField<GameSessionStore>(viewModel, "sessionStore")

        viewModel.openMenuOverlay(MenuTab.INVENTORY)
        dispatcher.scheduler.advanceUntilIdle()

        val currentPrompt = promptManager.state.value.current as? TutorialPrompt
        assertNotNull(currentPrompt)
        assertEquals("bag_basics", currentPrompt?.entry?.key)

        promptManager.dismissCurrent()
        dispatcher.scheduler.advanceUntilIdle()

        val state = sessionStore.state.value
        assertTrue(state.tutorialSeen.contains("bag_basics"))
        assertTrue(state.tutorialCompleted.contains("bag_basics"))
    }

    @Test
    fun fullMapCellsCoverEntireNodeAndPreserveDiscoveryState() {
        val viewModel = createViewModel()
        val roomA = testRoom(
            id = "room_a",
            connections = mapOf("east" to "room_b"),
            pos = listOf(0, 0)
        )
        val roomB = testRoom(
            id = "room_b",
            connections = mapOf("west" to "room_a", "east" to "room_c"),
            pos = listOf(1, 0)
        )
        val roomC = testRoom(
            id = "room_c",
            connections = mapOf("west" to "room_b", "east" to "room_d"),
            pos = listOf(2, 0)
        )
        val roomD = testRoom(
            id = "room_d",
            connections = mapOf("west" to "room_c"),
            pos = listOf(3, 0)
        )

        setPrivateField(
            viewModel,
            "roomsById",
            mapOf(roomA.id to roomA, roomB.id to roomB, roomC.id to roomC, roomD.id to roomD)
        )
        setPrivateField(
            viewModel,
            "roomsByNodeId",
            mapOf("node_1" to listOf(roomA, roomB, roomC, roomD))
        )
        setPrivateField(
            viewModel,
            "nodeIdByRoomId",
            mapOf(roomA.id to "node_1", roomB.id to "node_1", roomC.id to "node_1", roomD.id to "node_1")
        )

        val discoveredRooms = getPrivateField<MutableSet<String>>(viewModel, "discoveredRooms")
        discoveredRooms.clear()
        discoveredRooms.add(roomA.id)

        val visitedRooms = getPrivateField<MutableSet<String>>(viewModel, "visitedRooms")
        visitedRooms.clear()
        visitedRooms.addAll(listOf(roomA.id, roomB.id, roomC.id, roomD.id))

        val method = ExplorationViewModel::class.java.getDeclaredMethod("buildFullMapState", Room::class.java).apply {
            isAccessible = true
        }
        val fullMap = method.invoke(viewModel, roomA) as FullMapUiState

        assertEquals(4, fullMap.cells.size)
        val ids = fullMap.cells.map { it.roomId }.toSet()
        assertTrue(ids.containsAll(listOf("room_a", "room_b", "room_c", "room_d")))
        val currentCell = fullMap.cells.first { it.roomId == roomA.id }
        val farCell = fullMap.cells.first { it.roomId == roomD.id }

        assertTrue(currentCell.isCurrent)
        assertTrue(currentCell.visited)
        assertEquals(0, currentCell.offsetX)
        assertEquals(3, farCell.offsetX)
        assertFalse(farCell.discovered)
        assertTrue(farCell.visited)
    }

    @Test
    fun unlockedExitClearsBlockedDirection() {
        val viewModel = createViewModel()
        val lockedRoom = testRoom(
            id = "lock_room",
            connections = mapOf("north" to "dest_room"),
            pos = listOf(0, 0),
            blockedDirections = mapOf(
                "north" to BlockedDirection(
                    type = "lock",
                    messageLocked = "Locked",
                    messageUnlock = "Unlocked"
                )
            )
        )
        val destination = testRoom(
            id = "dest_room",
            connections = mapOf("south" to "lock_room"),
            pos = listOf(0, 1)
        )

        setPrivateField(
            viewModel,
            "roomsById",
            mapOf(lockedRoom.id to lockedRoom, destination.id to destination)
        )

        val method = ExplorationViewModel::class.java.getDeclaredMethod(
            "updateUnlockedExits",
            Set::class.java
        ).apply { isAccessible = true }

        method.invoke(viewModel, setOf("lock_room::north"))

        val roomsById = getPrivateField<Map<String, Room>>(viewModel, "roomsById")
        val updated = roomsById[lockedRoom.id]
        assertNotNull(updated)
        assertTrue(updated?.blockedDirections?.containsKey("north") == false)
    }

    @Test
    fun tutorialScriptsTriggeredForQuestStage() {
        val viewModel = createViewModel()
        val questRepository = getPrivateField<QuestRepository>(viewModel, "questRepository")
        val tutorialManager = getPrivateField<TutorialRuntimeManager>(viewModel, "tutorialManager")
        val sessionStore = getPrivateField<GameSessionStore>(viewModel, "sessionStore")
        val quest = testQuest("quest_training", listOf("stage_one", "stage_two"))

        whenever(questRepository.questById("quest_training")).thenReturn(quest)

        val stageTutorialKey = "quest_training:stage_two:movement"
        val method = ExplorationViewModel::class.java.getDeclaredMethod(
            "scheduleQuestStageTutorials",
            String::class.java,
            String::class.java
        ).apply { isAccessible = true }

        sessionStore.startQuest("quest_training")
        method.invoke(viewModel, "quest_training", "stage_two")
        dispatcher.scheduler.advanceUntilIdle()

        val stageTutorialKeys = getPrivateField<MutableSet<String>>(viewModel, "stageTutorialKeys")
        assertTrue(stageTutorialKeys.contains(stageTutorialKey))
    }

    private fun testRoom(
        id: String,
        connections: Map<String, String>,
        pos: List<Int>,
        blockedDirections: Map<String, BlockedDirection>? = emptyMap()
    ): Room = Room(
        id = id,
        env = "env",
        title = id,
        backgroundImage = "bg_$id.png",
        description = id,
        npcs = emptyList(),
        items = emptyList(),
        enemies = emptyList(),
        connections = connections,
        pos = pos,
        state = emptyMap<String, Any>(),
        actions = emptyList<Map<String, Any?>>(),
        blockedDirections = blockedDirections
    )

    private fun testQuest(id: String, stageIds: List<String>): Quest {
        val stages = stageIds.mapIndexed { index, stageId ->
            QuestStage(
                id = stageId,
                title = "Stage ${index + 1}",
                description = "Stage description",
                tasks = listOf(
                    QuestTask(
                        id = "${stageId}_task",
                        text = "Complete $stageId",
                        tutorialId = "movement"
                    )
                )
            )
        }
        return Quest(
            id = id,
            title = "Test Quest",
            summary = "Quest summary",
            description = "Quest description",
            stages = stages
        )
    }

    private fun createViewModel(): ExplorationViewModel {
        val worldAssets = mock<WorldAssetDataSource> {
            on { loadRooms() } doReturn emptyList()
            on { loadHubs() } doReturn emptyList()
            on { loadWorlds() } doReturn emptyList()
            on { loadCharacters() } doReturn emptyList()
            on { loadSkills() } doReturn emptyList()
        }
        val sessionStore = GameSessionStore()
        val dialogueService = mock<DialogueService>()
        val inventoryFlow = MutableStateFlow<List<InventoryEntry>>(emptyList())
        val inventoryService = mock<InventoryService> {
            on { state } doReturn inventoryFlow
        }
        doAnswer { }.whenever(inventoryService).loadItems()
        doAnswer { }.whenever(inventoryService).addOnItemAddedListener(any())
        doAnswer { }.whenever(inventoryService).removeOnItemAddedListener(any())
        val craftingService = mock<CraftingService> {
            on { cookingRecipes } doReturn emptyList()
            on { firstAidRecipes } doReturn emptyList()
            on { tinkeringRecipes } doReturn emptyList()
        }
        val cinematicService = mock<CinematicService>()
        val cinematicCoordinator = CinematicCoordinator(cinematicService)
        val questRepository = mock<QuestRepository>()
        val shopRepository = mock<ShopRepository>()
        val levelingManager = mock<LevelingManager> {
            on { levelBounds(any()) } doReturn (0 to 100)
        }
        val audioRouter = AudioRouter(AudioBindings())
        val promptManager = UIPromptManager()
        val uiEventBus = UiEventBus()
        val questRuntimeManager = QuestRuntimeManager(questRepository, sessionStore, scope, uiEventBus)
        val tutorialManager = TutorialRuntimeManager(sessionStore, promptManager, null, scope)
        val milestoneRepository = mock<MilestoneRepository> {
            on { milestoneById(any()) } doReturn null
        }
        val milestoneManager = MilestoneRuntimeManager(milestoneRepository, sessionStore, promptManager, scope)
        val fishingService = mock<FishingService>()
        val themeRepository = mock<ThemeRepository> {
            on { getTheme(any()) } doReturn null
            on { getStyle(any()) } doReturn null
        }
        val environmentThemeManager = EnvironmentThemeManager(themeRepository)
        val saveRepository = mock<GameSaveRepository>()
        val userSettingsFlow = MutableStateFlow(UserSettings())
        val userSettingsStore = mock<UserSettingsStore> {
            on { settings } doReturn userSettingsFlow
        }
        val voiceoverController = mock<VoiceoverController>()

        val dispatcherProvider = object : DispatcherProvider {
            override val io = dispatcher
            override val default = dispatcher
            override val main = dispatcher
        }
        return ExplorationViewModel(
            worldAssets = worldAssets,
            sessionStore = sessionStore,
            dialogueService = dialogueService,
            inventoryService = inventoryService,
            craftingService = craftingService,
            cinematicCoordinator = cinematicCoordinator,
            questRepository = questRepository,
            questRuntimeManager = questRuntimeManager,
            milestoneManager = milestoneManager,
            audioRouter = audioRouter,
            voiceoverController = voiceoverController,
            shopRepository = shopRepository,
            themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager,
            levelingManager = levelingManager,
            tutorialManager = tutorialManager,
            promptManager = promptManager,
            fishingService = fishingService,
            saveRepository = saveRepository,
            userSettingsStore = userSettingsStore,
            eventDefinitions = emptyList(),
            dispatchers = dispatcherProvider
        )
    }

    private fun setPrivateField(target: Any, name: String, value: Any?) {
        val field = target.javaClass.getDeclaredField(name).apply { isAccessible = true }
        field.set(target, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getPrivateField(target: Any, name: String): T {
        val field = target.javaClass.getDeclaredField(name).apply { isAccessible = true }
        return field.get(target) as T
    }
}
