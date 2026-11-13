package com.example.starborn.feature.mainmenu

import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.di.AppServices
import com.example.starborn.domain.session.GameSessionSlotInfo
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainMenuSaveSlotsTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun titleLoadGameListsAndLoadsTheTwentiethNumberedSave() = runTest(dispatcher) {
        val assets = mock<WorldAssetDataSource> {
            on { loadWorlds() } doReturn emptyList()
            on { loadRooms() } doReturn emptyList()
            on { loadHubs() } doReturn emptyList()
            on { loadCharacters() } doReturn emptyList()
        }
        val themes = mock<ThemeRepository>()
        val services = mock<AppServices> {
            on { worldDataSource } doReturn assets
            on { themeRepository } doReturn themes
        }
        val saved = GameSessionState(worldId = "world_2", roomId = "sector9_stream_falls", playerId = "nova")
        whenever(services.slotInfo(20)).thenReturn(GameSessionSlotInfo(saved, 1234L))
        whenever(services.loadSlot(20)).thenReturn(true)
        val vm = MainMenuViewModel(services)
        runCurrent()
        assertEquals((1..20).toList(), vm.slots.value.filter { it.slot > 0 }.map { it.slot })
        assertEquals(22, vm.slots.value.size)
        assertEquals(saved, vm.slots.value.single { it.slot == 20 }.state)
        assertTrue(vm.loadSlot(20))
        verify(services).loadSlot(20)
    }
}
