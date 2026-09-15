package com.example.starborn

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.starborn.di.AppServices
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.feature.exploration.ui.buildInlineActionPlan
import com.example.starborn.feature.exploration.ui.hud.RoomDescription
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModelFactory
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LaunchConsoleInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun earlyRenderedConsoleTapThenSpliceThenLaunch() {
        val services = AppServices(IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>()))
        val models = ViewModelStore()
        lateinit var vm: ExplorationViewModel
        compose.runOnIdle {
            services.sessionStore.restore(GameSessionState(
                worldId = "world_1", hubId = "hub_2_logistics", roomId = "launch_pod",
                playerId = "nova", partyMembers = listOf("nova", "zeke"),
                activeQuests = setOf("w1_mq05"), questStageById = mapOf("w1_mq05" to "launch_pod"),
                completedMilestones = setOf("ms_w1_warden_defeated", "ms_w1_zeke_directed_to_pod"),
                inventory = mapOf("ghost_signal_cell" to 1)
            ))
            services.syncInventoryFromSession()
            vm = ViewModelProvider(models, ExplorationViewModelFactory(services))[ExplorationViewModel::class.java]
        }
        try {
            compose.waitUntil(30000) { vm.uiState.value.actions.any { it.name == "navigation console" } }
            val state = vm.uiState.value
            val room = requireNotNull(state.currentRoom)
            val plan = requireNotNull(buildInlineActionPlan(room.description, state.actions, state.actionHints, room))
            compose.setContent {
                MaterialTheme { RoomDescription(plan, plan.description, false, Color.White,
                    vm::onActionSelected, {}, {}, Color.Cyan) }
            }
            compose.onAllNodesWithContentDescription("navigation console")[0].performClick()
            compose.runOnIdle {
                assertFalse("w1_mq05_use_nav_console" in services.sessionStore.state.value.completedEvents)
                // Exercise the same parser/runtime path as the authored dialogue's final trigger.
                val trigger = ExplorationViewModel::class.java.getDeclaredMethod("handleDialogueTrigger", String::class.java)
                    .apply { isAccessible = true }
                trigger.invoke(vm, "player_action:w1_mq05_splice_chime")
                assertTrue("ms_w1_chime_spliced" in services.sessionStore.state.value.completedMilestones)
                assertEquals(0, services.inventoryService.snapshot()["ghost_signal_cell"] ?: 0)
            }
            compose.onAllNodesWithContentDescription("navigation console")[0].performClick()
            compose.waitUntil(5000) { "scene_launch_crash" in services.sessionStore.state.value.pendingEventCinematics }
        } finally {
            compose.runOnIdle { models.clear(); services.tutorialManager.cancelAllScheduled() }
        }
    }
}
