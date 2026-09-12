package com.example.starborn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.actionKey
import com.example.starborn.feature.exploration.ui.buildInlineActionPlan
import com.example.starborn.feature.exploration.ui.hud.RoomDescription
import com.example.starborn.feature.exploration.viewmodel.ActionHintUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExplicitActionLinkInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun cargoPilotDispatchesAuthoredInspection() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val services = com.example.starborn.di.AppServices(context)
        val models = androidx.lifecycle.ViewModelStore()
        lateinit var vm: com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
        compose.runOnIdle {
            services.sessionStore.restore(com.example.starborn.domain.session.GameSessionState(
                roomId = "astra_cargo_bay", playerId = "nova", partyMembers = listOf("nova")))
            vm = androidx.lifecycle.ViewModelProvider(models,
                com.example.starborn.feature.exploration.viewmodel.ExplorationViewModelFactory(services))[
                com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel::class.java]
        }
        try {
            compose.waitUntil(30000) { vm.uiState.value.currentRoom?.id == "astra_cargo_bay" &&
                vm.uiState.value.actions.any { it.name == "repair workbench" } }
            val state = vm.uiState.value
            val room = requireNotNull(state.currentRoom)
            val plan = requireNotNull(buildInlineActionPlan(room.description, state.actions, state.actionHints, room))
            compose.setContent {
                MaterialTheme {
                    RoomDescription(plan, plan.description, false, Color.White,
                        vm::onActionSelected, {}, {}, Color.Cyan)
                }
            }
            compose.onAllNodesWithContentDescription("repair workbench")[0].performClick()
            compose.waitUntil(5000) { vm.uiState.value.statusMessage ==
                "A sturdy titanium bench equipped with laser soldering tools and schematic vices." }
            compose.runOnIdle {
                org.junit.Assert.assertNotNull(vm.uiState.value.narrationPrompt)
                assertEquals("astra_cargo_bay", vm.uiState.value.currentRoom?.id)
            }
        } finally {
            compose.runOnIdle { models.clear(); services.tutorialManager.cancelAllScheduled() }
        }
    }
    @Test fun wrappedExplicitLinkActivates() = verify(false)
    @Test fun lockedExplicitLinkDoesNotActivate() = verify(true)
    private fun verify(locked: Boolean) {
        val action = GenericAction("debug manifest", "generic")
        val plan = requireNotNull(buildInlineActionPlan(
            "Read [action:debug manifest|the very long layout manifest for this test room].",
            listOf(action), mapOf(action.actionKey() to ActionHintUi(locked, null)), null))
        var calls = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                MaterialTheme {
                    Box(Modifier.width(240.dp)) {
                        RoomDescription(plan, plan.description, false, Color.White,
                            { calls++ }, {}, {}, Color.Cyan)
                    }
                }
            }
        }
        compose.waitForIdle()
        val nodes = compose.onAllNodesWithContentDescription("debug manifest").fetchSemanticsNodes()
        org.junit.Assert.assertTrue("Wrapped link exposes line targets", nodes.size > 1)
        compose.onAllNodesWithContentDescription("debug manifest")[0].performClick()
        compose.runOnIdle { assertEquals(if (locked) 0 else 1, calls) }
    }
}
