package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.feature.combat.ui.CombatScreen
import com.example.starborn.feature.combat.viewmodel.*
import com.example.starborn.ui.theme.StarbornTheme
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Separate cache-backed Test Session; never loads or moves the player's campaign. */
class AuthoredEncounterPresentationTest {
    @get:Rule val compose=createComposeRule()
    @Test fun silenceFormationKeepsTargetsReadableWhenAnAnchorFalls() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        lateinit var vm: CombatViewModel
        compose.runOnUiThread {
            services = AppServices(IsolatedProgressionContext(base), true)
            assertTrue(services.startDebugScenario("w3_lockdown_escape"))
            services.sessionStore.restore(services.sessionStore.state.value.copy(
                worldId = "world_6", hubId = "hub_12_singularity", roomId = "source_center"))
            vm = CombatViewModelFactory(services, listOf("ascended_god"), false).create(CombatViewModel::class.java)
            vm.setBackgroundPaused(true)
        }
        compose.setContent { StarbornTheme { Box(Modifier.height(720.dp)) {
            CombatScreen(rememberNavController(), vm, services.audioCuePlayer,
                suppressFlashes = true, suppressScreenshake = true, highContrastMode = false,
                largeTouchTargets = false, showCombatActionText = true, overlayPaused = true)
        } } }
        try {
            val ids = listOf("ascended_god", "silence_voice_anchor", "silence_memory_anchor")
            for (id in ids) {
                compose.onNodeWithTag("combat-linked-target-$id").assertIsDisplayed().performTouchInput { click() }
                compose.runOnIdle { assertEquals(id, vm.inspectedEnemyId.value); vm.dismissEnemyInspection() }
            }
            val field = com.example.starborn.feature.combat.viewmodel.CombatController::class.java
                .getDeclaredField("_state").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val state = field.get(vm.runtime) as kotlinx.coroutines.flow.MutableStateFlow<com.example.starborn.domain.combat.CombatState?>
            val folder = File(base.getExternalFilesDir(null), "world6-authored").apply { mkdirs() }
            fun shot(name: String) {
                val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
                File(folder, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
            compose.waitForIdle()
            shot("silence-formation.png")
            val before = compose.onNodeWithTag("combat-enemies").fetchSemanticsNode().boundsInRoot
            compose.runOnIdle {
                val current = requireNotNull(state.value)
                val anchor = current.combatants.getValue("silence_voice_anchor")
                state.value = current.copy(combatants = current.combatants + ("silence_voice_anchor" to anchor.copy(hp = 0)))
            }
            compose.onNodeWithTag("combat-linked-target-silence_voice_anchor").assertIsNotEnabled()
            compose.onNodeWithTag("combat-linked-target-silence_memory_anchor").assertIsEnabled()
            assertEquals(before, compose.onNodeWithTag("combat-enemies").fetchSemanticsNode().boundsInRoot)
            shot("silence-anchor-broken.png")
        } finally { compose.runOnUiThread { vm.runtime.close(); services.release() } }
    }

    @Test fun ringDuetArtAndBothTargetsStayReadableAcrossLinkLoss() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        lateinit var vm: CombatViewModel
        compose.runOnUiThread {
            services = AppServices(IsolatedProgressionContext(base), true)
            assertTrue(services.startDebugScenario("w3_lockdown_escape"))
            services.sessionStore.restore(services.sessionStore.state.value.copy(worldId = "world_5",
                hubId = "hub_10_deep_ring", roomId = "deep_tear"))
            vm = CombatViewModelFactory(services, listOf("compliance_avatar"), false).create(CombatViewModel::class.java)
            vm.setBackgroundPaused(true)
        }
        compose.setContent { StarbornTheme { Box(Modifier.height(720.dp)) {
            CombatScreen(rememberNavController(), vm, services.audioCuePlayer,
                suppressFlashes = true, suppressScreenshake = true, highContrastMode = false,
                largeTouchTargets = false, showCombatActionText = true, overlayPaused = true)
        } } }
        try {
            val tags = listOf("compliance_avatar", "directive_conductor")
            for (id in tags) {
                compose.onNodeWithTag("combat-enemy-$id", useUnmergedTree = true).assertIsDisplayed()
                    .performTouchInput { click() }
                compose.runOnIdle { assertEquals(id, vm.inspectedEnemyId.value); vm.dismissEnemyInspection() }
            }
            val field = com.example.starborn.feature.combat.viewmodel.CombatController::class.java.getDeclaredField("_state").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val state = field.get(vm.runtime) as kotlinx.coroutines.flow.MutableStateFlow<com.example.starborn.domain.combat.CombatState?>
            val folder = File(base.getExternalFilesDir(null), "world5-authored").apply { mkdirs() }
            fun shot(name: String) {
                val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
                File(folder, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
            }
            compose.waitForIdle(); shot("avatar-linked.png")
            val before = compose.onNodeWithTag("combat-enemy-compliance_avatar", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            compose.runOnIdle {
                val current = requireNotNull(state.value)
                val killed = current.copy(combatants = current.combatants + ("directive_conductor" to current.combatants.getValue("directive_conductor").copy(hp = 0)))
                val definitions = vm.enemies.associateBy { it.id }
                state.value = com.example.starborn.domain.combat.AuthoredEncounterRules.reconcile(killed, definitions,
                    com.example.starborn.domain.combat.StatusRegistry(services.worldDataSource.loadStatuses()))
            }
            compose.waitForIdle()
            assertEquals(before, compose.onNodeWithTag("combat-enemy-compliance_avatar", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot)
            shot("avatar-unlinked.png")
        } finally { compose.runOnUiThread { vm.runtime.close(); services.release() } }
    }
    @Test fun administratorReinforcementsReuseFixedVacanciesWithoutMovingSurvivors() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        lateinit var vm: CombatViewModel
        compose.runOnUiThread {
            services = AppServices(IsolatedProgressionContext(base), true)
            assertTrue(services.debugScenarioError, services.startDebugScenario("w3_lockdown_escape"))
            services.sessionStore.restore(services.sessionStore.state.value.copy(roomId = "spire_landing_pad_roof"))
            vm = CombatViewModelFactory(services, listOf("administrator_boss", "aero_drone"), false)
                .create(CombatViewModel::class.java)
            vm.setBackgroundPaused(true)
        }
        compose.setContent { StarbornTheme { Box(Modifier.height(720.dp)) {
            CombatScreen(rememberNavController(), vm, services.audioCuePlayer,
                suppressFlashes = true, suppressScreenshake = true, highContrastMode = false,
                largeTouchTargets = false, showCombatActionText = true, overlayPaused = true)
        } } }
        val controller = com.example.starborn.feature.combat.viewmodel.CombatController::class.java
        val field = controller.getDeclaredField("_state").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val state = field.get(vm.runtime) as kotlinx.coroutines.flow.MutableStateFlow<com.example.starborn.domain.combat.CombatState?>
        val deploy = controller.getDeclaredMethod("deployReserve", com.example.starborn.domain.combat.CombatState::class.java,
            String::class.java).apply { isAccessible = true }
        fun bounds(id: String) = compose.onNodeWithTag("combat-enemy-$id", useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        fun spawn(): String {
            compose.runOnIdle { state.value = deploy.invoke(vm.runtime, state.value, "administrator_boss") as com.example.starborn.domain.combat.CombatState }
            compose.waitForIdle()
            return state.value!!.authored.reserveOwners.keys.last()
        }
        fun remove(id: String) {
            compose.runOnIdle {
                val current = state.value!!
                state.value = current.copy(combatants = current.combatants + (id to current.combatants.getValue(id).copy(hp = 0)))
            }
            compose.waitForIdle()
        }
        try {
            val boss = bounds("administrator_boss")
            val escort = bounds("aero_drone")
            val first = spawn()
            val firstPosition = bounds(first)
            assertEquals(boss, bounds("administrator_boss"))
            assertEquals(escort, bounds("aero_drone"))
            remove("aero_drone")
            assertEquals(boss, bounds("administrator_boss"))
            assertEquals(firstPosition, bounds(first))
            val second = spawn()
            assertEquals(escort, bounds(second))
            assertEquals(boss, bounds("administrator_boss"))
            assertEquals(firstPosition, bounds(first))
            remove(first)
            val third = spawn()
            assertEquals(firstPosition, bounds(third))
            assertEquals(escort, bounds(second))
            assertEquals(boss, bounds("administrator_boss"))
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(base.getExternalFilesDir(null), "administrator-fixed-formation.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        } finally { compose.runOnUiThread { vm.runtime.close(); services.release() } }
    }
    @Test fun administratorBattleScaleReview() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        lateinit var vm: CombatViewModel
        compose.runOnUiThread {
            services = AppServices(IsolatedProgressionContext(base), true)
            assertTrue(services.debugScenarioError, services.startDebugScenario("w3_lockdown_escape"))
            vm = CombatViewModelFactory(services, listOf("administrator_boss", "aero_drone"), false)
                .create(CombatViewModel::class.java)
            vm.setBackgroundPaused(true)
        }
        compose.setContent { StarbornTheme { Box(Modifier.height(720.dp)) {
            CombatScreen(rememberNavController(), vm, services.audioCuePlayer,
                suppressFlashes = true, suppressScreenshake = true, highContrastMode = false,
                largeTouchTargets = false, showCombatActionText = true, overlayPaused = true)
        } } }
        try {
            compose.onNodeWithTag("combat-enemy-administrator_boss", useUnmergedTree = true).assertIsDisplayed()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(base.getExternalFilesDir(null), "administrator-combat-scale.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        } finally { compose.runOnUiThread { vm.runtime.close(); services.release() } }
    }
    @Test fun fiveTargetButtonsDispatchDistinctTargetIds() {
        val ids=listOf("body","missile","cutter","anchor","projector")
        val actors=ids.map { com.example.starborn.domain.combat.Combatant(it,it,
            com.example.starborn.domain.combat.CombatSide.ENEMY,
            com.example.starborn.domain.combat.StatBlock(100,5,5,5,5,5,5)) }
        val state=com.example.starborn.domain.combat.CombatState(emptyList(),0,
            actors.associate { it.id to com.example.starborn.domain.combat.CombatantState(it,100,50) })
        var selected by mutableStateOf("")
        compose.setContent { StarbornTheme {
            com.example.starborn.ui.combat.LinkedEnemyTargets(state,ids,setOf(selected),{ selected=it })
        } }
        ids.forEach { id ->
            compose.onNodeWithTag("combat-linked-target-$id").performClick()
            compose.runOnIdle { assertEquals(id,selected) }
        }
    }
    @Test fun foundryTitanTargetsAndWarningStayReadableOnShortAndLargeTextScreens() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val base=ApplicationProvider.getApplicationContext<Context>()
        lateinit var services:AppServices
        lateinit var vm:CombatViewModel
        var showing by mutableStateOf(false)
        var height by mutableIntStateOf(720)
        var fontScale by mutableFloatStateOf(1f)
        instrumentation.runOnMainSync {
            services=AppServices(IsolatedProgressionContext(base),true)
            assertTrue(services.debugScenarioError,services.startDebugScenario("boss_titan_walker"))
            services.sessionStore.restore(services.sessionStore.state.value.copy(
                partyMembers = listOf("nova", "zeke", "orion", "gh0st")
            ))
            vm=CombatViewModelFactory(services,listOf("titan_walker_boss"),false)
                .create(CombatViewModel::class.java)
        }
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,fontScale)) {
                StarbornTheme { if(showing) Box(Modifier.height(height.dp)) {
                    CombatScreen(rememberNavController(),vm,services.audioCuePlayer,
                        suppressFlashes=true,suppressScreenshake=true,highContrastMode=false,
                        largeTouchTargets=false,showCombatActionText=true,overlayPaused=true)
                } }
            }
        }
        try {
            compose.waitUntil(20000) { vm.combatState?.authored?.preparations?.isNotEmpty()==true }
            compose.runOnIdle { showing=true };compose.waitForIdle()
            compose.runOnIdle { vm.runtime.setBackgroundPaused(true) }
            val ids=listOf("titan_walker_boss","titan_missile_battery","titan_cutting_arm","titan_anchor_cannon","titan_shield_projector")
            val field = com.example.starborn.feature.combat.viewmodel.CombatController::class.java
                .getDeclaredField("_state").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val state = field.get(vm.runtime) as kotlinx.coroutines.flow.MutableStateFlow<com.example.starborn.domain.combat.CombatState?>
            val folder=File(base.getExternalFilesDir(null),"authored-encounters").apply { mkdirs() }
            for((screenHeight,scale) in listOf(720 to 1f,560 to 1.3f)) {
                compose.runOnIdle { height=screenHeight;fontScale=scale };compose.waitForIdle()
                val warning = compose.onNodeWithTag("combat-preparation-titan_walker_boss")
                if (screenHeight >= 640) warning.assertIsDisplayed() else warning.assertExists()
                if (screenHeight < 640) compose.onNodeWithTag("combat-linked-target-titan_missile_battery").performScrollTo()
                compose.onNodeWithTag("combat-linked-target-titan_missile_battery")
                    .assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                        "Preparing an action"))
                val party=compose.onNodeWithTag("combat-party").fetchSemanticsNode().boundsInRoot
                val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
                File(folder,"titan-${screenHeight}h-font$scale.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                bitmap.recycle()
                File(folder,"bounds-${screenHeight}h-font$scale.txt").writeText(ids.joinToString("\n") { id ->
                    "$id: ${compose.onNodeWithTag("combat-linked-target-$id").fetchSemanticsNode().boundsInRoot}" } + "\nParty: $party")
                ids.forEach { id ->
                    val button=compose.onNodeWithTag("combat-linked-target-$id")
                    button.assertIsDisplayed()
                    if (screenHeight >= 640) assertTrue("Target stays above the crew: $id",button.fetchSemanticsNode().boundsInRoot.bottom < party.top)
                    button.performTouchInput { click() }
                    compose.runOnIdle {
                        assertEquals("Target card must inspect $id above the sprite", id, vm.inspectedEnemyId.value)
                        vm.dismissEnemyInspection()
                    }
                }
                if (screenHeight < 640) {
                    compose.onNodeWithTag("combat-party").performScrollTo().assertIsDisplayed()
                    compose.onNodeWithTag("combat-preparation-titan_walker_boss").performScrollTo().assertIsDisplayed()
                    compose.onNodeWithTag("combat-linked-target-titan_shield_projector").performScrollTo().assertIsDisplayed()
                }
            }
            val intact = requireNotNull(state.value)
            for (disabled in listOf(ids.drop(1).take(1), ids.drop(1))) {
                compose.runOnIdle {
                    height = 720
                    fontScale = 1f
                    state.value = intact.copy(
                        combatants = intact.combatants.mapValues { (id, actor) ->
                            if (id in disabled) actor.copy(hp = 0) else actor
                        },
                        authored = intact.authored.copy(preparations = emptyMap())
                    )
                }
                compose.waitForIdle()
                disabled.forEach { compose.onNodeWithTag("combat-linked-target-$it").assertIsNotEnabled() }
                compose.onNodeWithTag("combat-linked-target-titan_walker_boss").assertIsEnabled()
                val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
                File(folder, "titan-disabled-${disabled.size}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
        } finally { instrumentation.runOnMainSync { vm.viewModelScope.cancel();services.release() } }
    }
    @Test fun worldTwoBeastWarningStaysReadableWithLargePhoneText() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val base=ApplicationProvider.getApplicationContext<Context>()
        lateinit var services:AppServices
        lateinit var vm:CombatViewModel
        var showing by mutableStateOf(false)
        var textScale by mutableFloatStateOf(1.3f)
        instrumentation.runOnMainSync {
            services=AppServices(IsolatedProgressionContext(base),true)
            assertTrue(services.debugScenarioError,services.startDebugScenario("campaign_w2_mq04"))
            vm=CombatViewModelFactory(services,listOf("the_beast"),false).create(CombatViewModel::class.java)
        }
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,textScale)) {
                StarbornTheme { if(showing) Box(Modifier.height(720.dp)) {
                    CombatScreen(rememberNavController(),vm,services.audioCuePlayer,
                        suppressFlashes=true,suppressScreenshake=true,highContrastMode=false,
                        largeTouchTargets=false,showCombatActionText=true,overlayPaused=true)
                } }
            }
        }
        try {
            compose.waitUntil(20000) { vm.combatState?.authored?.preparations?.isNotEmpty()==true }
            compose.runOnIdle { vm.runtime.setBackgroundPaused(true);showing=true }
            compose.waitForIdle()
            compose.onNodeWithTag("combat-authored-warnings").assertIsDisplayed()
            compose.onNodeWithTag("combat-preparation-the_beast").assertIsDisplayed()
            compose.onNodeWithTag("combat-enemies").assertIsDisplayed()
            compose.onNodeWithTag("combat-party").assertIsDisplayed()
            val folder=File(base.getExternalFilesDir(null),"world2-authored").apply { mkdirs() }
            val field=com.example.starborn.feature.combat.viewmodel.CombatController::class.java
                .getDeclaredField("_state").apply { isAccessible=true }
            @Suppress("UNCHECKED_CAST")
            val flow=field.get(vm.runtime) as kotlinx.coroutines.flow.MutableStateFlow<com.example.starborn.domain.combat.CombatState?>
            val warning=vm.combatState!!
            val marked=warning.authored.preparations.values.single().targetIds.single()
            for(scale in listOf(1f,1.3f)) {
                compose.runOnIdle { textScale=scale;flow.value=warning };compose.waitForIdle()
                val tags=listOf("combat-enemies","combat-enemy-the_beast","combat-party")
                val geometry=tags.associateWith { compose.onNodeWithTag(it, useUnmergedTree=true).fetchSemanticsNode().boundsInRoot }
                compose.onNodeWithTag("combat-threat-target-$marked", useUnmergedTree=true).assertExists()
                vm.playerParty.filter { it.id!=marked }.forEach { compose.onNodeWithTag("combat-threat-target-${it.id}", useUnmergedTree=true).assertDoesNotExist() }
                compose.runOnIdle { flow.value=warning.copy(authored=warning.authored.copy(preparations=emptyMap(),phases=emptyMap())) }
                compose.waitForIdle()
                compose.onNodeWithTag("combat-preparation-the_beast").assertDoesNotExist()
                compose.onNodeWithTag("combat-threat-target-$marked", useUnmergedTree=true).assertDoesNotExist()
                for(tag in tags) assertEquals("Warning removal must not move or resize $tag at font $scale",
                    geometry.getValue(tag),compose.onNodeWithTag(tag, useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
                compose.runOnIdle { flow.value=warning.copy(authored=warning.authored.copy(preparations=emptyMap(),phases=mapOf("the_beast" to "Frenzy — two strikes, then overextension"))) }
                compose.waitForIdle()
                compose.onNodeWithTag("combat-authored-status").assertIsDisplayed()
                for(tag in tags) assertEquals("Phase status must not move or resize $tag at font $scale",
                    geometry.getValue(tag),compose.onNodeWithTag(tag, useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
                File(folder,"threat-geometry-font$scale.txt").writeText(geometry.entries.joinToString("\n"))
            }
            compose.runOnIdle { flow.value=warning };compose.waitForIdle()
            val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
            File(folder,"beast-phone-font1.3.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        } finally { instrumentation.runOnMainSync { vm.viewModelScope.cancel();services.release() } }
    }

    @Test fun twoIncomingAttacksFitTheExistingMessageSlotTogether() {
        val stats=com.example.starborn.domain.combat.StatBlock(100,5,5,5,5,5,5)
        val actors=listOf("orb","spitter","nova","zeke").map { id ->
            com.example.starborn.domain.combat.Combatant(id,id,
                if(id in listOf("nova","zeke")) com.example.starborn.domain.combat.CombatSide.PLAYER else com.example.starborn.domain.combat.CombatSide.ENEMY,stats)
        }
        val state=com.example.starborn.domain.combat.CombatState(emptyList(),0,actors.associate {
            it.id to com.example.starborn.domain.combat.CombatantState(it,100,50)
        },authored=com.example.starborn.domain.combat.AuthoredEncounterState(preparations=mapOf(
            "orb" to com.example.starborn.domain.combat.EnemyPreparation("orb","orb","beam",listOf("nova"),"Iris opening"),
            "spitter" to com.example.starborn.domain.combat.EnemyPreparation("spitter","spitter","spore",listOf("zeke"),"Spore bulb swelling"))))
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.3f)) {
                StarbornTheme { androidx.compose.foundation.layout.Box(
                    Modifier.height(56.dp)) {
                    com.example.starborn.ui.combat.AuthoredEncounterPanel(state,emptyList(),{},skillName={ if(it=="beam") "Iris Discharge" else "Spore Release" })
                } }
            }
        }
        compose.onNodeWithTag("combat-preparation-orb").assertIsDisplayed()
        compose.onNodeWithTag("combat-preparation-spitter").assertIsDisplayed()
        val a=compose.onNodeWithTag("combat-preparation-orb").fetchSemanticsNode().boundsInRoot
        val b=compose.onNodeWithTag("combat-preparation-spitter").fetchSemanticsNode().boundsInRoot
        assertTrue(a.bottom<=b.top)
    }

}
