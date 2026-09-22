package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.domain.combat.*
import com.example.starborn.feature.combat.ui.CombatScreen
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.CombatViewModelFactory
import com.example.starborn.ui.theme.StarbornTheme
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Isolated presentation fixture, not a campaign-earned balance result. */
class FinalePresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun openingCueAndLinkSelection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = ApplicationProvider.getApplicationContext<Context>()
        val isolated = IsolatedProgressionContext(base)
        val output = File(base.getExternalFilesDir(null), "finale-presentation/${System.currentTimeMillis()}").apply { mkdirs() }
        lateinit var services: AppServices
        lateinit var vm: CombatViewModel
        var createdVm = false
        fun capture(name: String) {
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        instrumentation.runOnMainSync { services = AppServices(isolated, true) }
        try {
            SystemClock.sleep(1000)
            instrumentation.runOnMainSync {
                assertTrue(services.debugScenarioError, services.startDebugScenario("campaign_w6_mq30"))
                services.sessionStore.unlockSkill("nova_link")
                vm = CombatViewModelFactory(services, listOf("ascended_god"), tutorialsEnabled = false)
                    .create(CombatViewModel::class.java)
                createdVm = true
                File(output, "fixture.txt").writeText("Isolated MQ30 presentation fixture; not balance-calibrated.\n${services.sessionStore.state.value}")
            }
            compose.setContent {
                StarbornTheme {
                    CombatScreen(rememberNavController(), vm, services.audioCuePlayer,
                        suppressFlashes = true, suppressScreenshake = true, highContrastMode = false,
                        largeTouchTargets = false, showCombatActionText = true)
                }
            }
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("Gathering Silence", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            capture("gathering-silence")
            compose.onNodeWithText("Recovery window — attack or heal").assertExists()
            compose.waitUntil(30_000) {
                instrumentation.runOnMainSync { vm.selectReadyPlayer("nova") }
                vm.awaitingAction.value == "nova"
            }
            instrumentation.runOnMainSync {
                vm.setBackgroundPaused(true)
                val field = CombatViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val flow = field.get(vm) as MutableStateFlow<CombatState?>
                val initial = requireNotNull(flow.value)
                flow.value = initial.copy(combatants = initial.combatants.mapValues { (_, actor) ->
                    actor.copy(hp = actor.combatant.stats.maxHp / 3, statusEffects = emptyList(), activeCooldowns = emptyMap())
                })
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Abilities").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText("Abilities").onFirst().performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Link"))
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Link").fetchSemanticsNodes().isNotEmpty() }
            capture("link-menu")
            compose.onNodeWithText("Heals party + Regen", substring = true).assertExists()
            File(output, "semantics.txt").writeText(compose.onAllNodes(isRoot(), useUnmergedTree = true).printToString())
            lateinit var before: CombatState
            instrumentation.runOnMainSync { before = requireNotNull(vm.combatState) }
            val linkNode = compose.onNodeWithText("Link").fetchSemanticsNode()
            val useNode = compose.onAllNodesWithText("Use").fetchSemanticsNodes()
                .minBy { kotlin.math.abs(it.boundsInRoot.center.y - linkNode.boundsInRoot.center.y) }
            compose.onNode(SemanticsMatcher("Link row Use button") { it.id == useNode.id }).performClick()
            compose.waitUntil(10_000) {
                requireNotNull(vm.combatState).log.drop(before.log.size).filterIsInstance<CombatLogEntry.Heal>().isNotEmpty()
            }
            instrumentation.runOnMainSync {
                val after = requireNotNull(vm.combatState)
                before.combatants.forEach { (id, actor) ->
                    if (actor.combatant.side == CombatSide.PLAYER) assertTrue(after.combatants.getValue(id).hp > actor.hp)
                    else assertEquals(actor.hp, after.combatants.getValue(id).hp)
                }
                File(output, "link-result.txt").writeText("before=$before\nafter=$after")
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Close").fetchSemanticsNodes().isEmpty() }
            compose.waitForIdle()
            SystemClock.sleep(300)
            capture("link-result")
            android.util.Log.i("FinalePresentation", "Evidence: ${output.absolutePath}")
        } finally {
            instrumentation.runOnMainSync {
                if (createdVm) vm.viewModelScope.cancel()
                services.release()
            }
        }
    }
}
