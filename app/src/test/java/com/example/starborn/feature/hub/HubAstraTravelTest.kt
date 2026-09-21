package com.example.starborn.feature.hub

import androidx.lifecycle.viewModelScope
import com.example.starborn.core.DispatcherProvider
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.session.AstraTravel
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.hub.viewmodel.HubViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class HubAstraTravelTest {
    private val dispatcher = StandardTestDispatcher()
    private val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
    private val store = GameSessionStore()
    private lateinit var viewModel: HubViewModel

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() {
        if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    private fun start(state: GameSessionState) {
        store.restore(state)
        viewModel = HubViewModel(assets, mock(), store, object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val default = dispatcher
        })
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test fun `regional boarding enters cargo ramp and returns to the exact location`() {
        start(GameSessionState(
            worldId = "world_3", hubId = "hub_6_upper_city", roomId = "spire_laundry_service",
            completedMilestones = setOf("ms_w2_mq05_complete")
        ))
        viewModel.enterNode("astra_access") {}
        viewModel.enterNode("astra_access") {}
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(AstraTravel.ENTRY_ROOM_ID, store.state.value.roomId)
        assertEquals(AstraTravel.HUB_ID, store.state.value.hubId)
        assertEquals("spire_laundry_service", store.state.value.astraReturnRoomId)
        assertEquals(setOf(AstraTravel.NODE_ID, "astra_disembark"), viewModel.uiState.value.nodes.map { it.id }.toSet())
        viewModel.enterNode("astra_disembark") {}
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("spire_laundry_service", store.state.value.roomId)
        assertEquals("hub_6_upper_city", store.state.value.hubId)
        assertEquals("world_3", store.state.value.worldId)
        assertNull(store.state.value.astraReturnRoomId)
    }

    @Test fun `legacy ship hub always has a usable disembark option`() {
        start(GameSessionState(
            worldId = AstraTravel.WORLD_ID, hubId = AstraTravel.HUB_ID, roomId = "astra_quarters",
            visitedNodes = setOf("astra_quarters_node"), completedMilestones = setOf("ms_w2_mq05_complete")
        ))
        assertTrue(viewModel.uiState.value.nodes.single { it.id == "astra_disembark" }.canEnter)
        viewModel.enterNode("astra_disembark") {}
        assertEquals("spire_sewers_landing", store.state.value.roomId)
        assertEquals("world_3", store.state.value.worldId)
    }
}
