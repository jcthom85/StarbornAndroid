package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.feature.combat.ui.CombatScreen
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.CombatViewModelFactory
import com.example.starborn.feature.mainmenu.BurgQuestDemo
import com.example.starborn.ui.theme.StarbornTheme
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class CombatFormationLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun partySizesStaySeparateFromEnemiesOnShortAndTallScreens() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = ApplicationProvider.getApplicationContext<Context>()
        val evidence = File(base.getExternalFilesDir(null), "combat-layout").apply { mkdirs() }
        lateinit var services: AppServices
        var current by mutableStateOf<CombatViewModel?>(null)
        var height by mutableStateOf(720)
        instrumentation.runOnMainSync { services = AppServices(IsolatedProgressionContext(base), true) }
        compose.setContent {
            StarbornTheme { current?.let { vm -> key(vm) {
                Box(Modifier.height(height.dp)) {
                    CombatScreen(rememberNavController(), vm, services.audioCuePlayer,
                        suppressFlashes = true, suppressScreenshake = true,
                        highContrastMode = false, largeTouchTargets = false,
                        showCombatActionText = true, overlayPaused = true, demoMode = true)
                }
            } } }
        }
        try {
            SystemClock.sleep(1000)
            val encounters = listOf(
                listOf("titan_walker_boss"),
                listOf("siren_skimmer", "spore_spitter"),
                List(5) { if (it % 2 == 0) "siren_skimmer" else "spore_spitter" }
            )
            for (encounter in encounters) for (screenHeight in listOf(560, 720)) for (count in 1..4) {
                instrumentation.runOnMainSync {
                    current?.viewModelScope?.cancel()
                    assertTrue(services.startDebugScenario("burgfest_boss"))
                    services.sessionStore.setPartyMembers(BurgQuestDemo.party.take(count))
                    height = screenHeight
                    current = CombatViewModelFactory(services, encounter, tutorialsEnabled = false)
                        .create(CombatViewModel::class.java)
                }
                compose.waitForIdle()
                SystemClock.sleep(500)
                val enemyArea = compose.onNodeWithTag("combat-enemies").fetchSemanticsNode().boundsInRoot
                val partyArea = compose.onNodeWithTag("combat-party").fetchSemanticsNode().boundsInRoot
                android.util.Log.i("CombatLayout", "$screenHeight/$count/${encounter.size}: enemies=$enemyArea party=$partyArea")
                val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                File(evidence, "${screenHeight}h-${count}crew-${encounter.size}enemies.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                assertTrue("Enemy area has usable height: $screenHeight/$count", enemyArea.height > 100f)
                assertTrue("Separated formations: $screenHeight/$count", enemyArea.bottom < partyArea.top)
                val portraits = current!!.playerParty.map { member ->
                    compose.onNodeWithContentDescription(member.name).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                }
                assertEquals(count, portraits.size)
                if (count == 1) {
                    assertEquals("Lone row is centered", partyArea.center.x, portraits.last().center.x, 3f)
                }
                if (count == 3) {
                    assertEquals("Third crew member centered below pair",
                        (portraits[0].center.x + portraits[1].center.x) / 2, portraits[2].center.x, 3f)
                }
                current!!.enemies.distinctBy { it.name }.forEach { enemy ->
                    val matching = compose.onAllNodesWithContentDescription(enemy.name)
                    matching.assertCountEquals(current!!.enemies.count { it.name == enemy.name })
                    matching.fetchSemanticsNodes().forEachIndexed { index, node ->
                        matching[index].assertIsDisplayed()
                        assertTrue("Sprites do not overlap: $screenHeight/$count/${encounter.size}",
                            node.boundsInRoot.bottom < portraits.minOf { it.top })
                    }
                }
                portraits.forEach { assertTrue("Readable portrait size", it.width >= 100f && it.height >= 100f) }
            }
        } finally {
            instrumentation.runOnMainSync { current?.viewModelScope?.cancel(); current = null; services.release() }
        }
    }
}
