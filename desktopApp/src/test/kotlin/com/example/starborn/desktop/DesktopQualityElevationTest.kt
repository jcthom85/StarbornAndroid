package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.feature.exploration.presentation.AstraArcadeStatusUi
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.navigation.CombatResultPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DesktopQualityElevationTest {
    @get:Rule
    val compose = createComposeRule()

    private fun withServices(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("starborn-desktop-quality-").toFile()
        val services = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            block(services)
        } finally {
            services.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun phase1_heroSignatureVfxVantageCalloutTargetedBadgeAndUnstackedGuard() = withServices { services ->
        services.startNewGame()
        runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
        // Verify hero style resolver covers all 5 playable party members
        assertEquals(DesktopAttackFxStyle.NOVA_LASER, resolveDesktopAttackFxStyle("nova", null, setOf("nova")))
        assertEquals(DesktopAttackFxStyle.ZEKE_PUNCH, resolveDesktopAttackFxStyle("player_zeke#1", null, setOf("zeke")))
        assertEquals(DesktopAttackFxStyle.ORION_JEWEL, resolveDesktopAttackFxStyle("orion", null, setOf("orion")))
        assertEquals(DesktopAttackFxStyle.GHOST_SLASH, resolveDesktopAttackFxStyle("gh0st", null, setOf("gh0st")))
        assertEquals(DesktopAttackFxStyle.OLLIE_TECH, resolveDesktopAttackFxStyle("ollie", null, setOf("ollie")))

        compose.setContent {
            DesktopStarbornTheme(services) {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    DesktopCombatScreen(
                        services = services,
                        enemyIds = listOf("the_beast"),
                        onVictory = {},
                        onDefeat = {},
                        onFlee = {}
                    )
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("combat-action-guard").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("combat-stage-backdrop").assertExists()
        compose.onNodeWithTag("combat-enemy-inspection").assertExists()
        val skillsBounds = compose.onNodeWithTag("combat-action-skills").fetchSemanticsNode().boundsInRoot
        val guardBounds = compose.onNodeWithTag("combat-action-guard").fetchSemanticsNode().boundsInRoot
        assertFalse(
            "Guard [G] must not overlap Abilities [2] vertically (skills=$skillsBounds, guard=$guardBounds)",
            skillsBounds.overlaps(guardBounds)
        )
    }

    @Test
    fun phase1_allFiveHeroSignatureCanvasesAndVantageBubbleRender() = withServices { services ->
        compose.mainClock.autoAdvance = false
        try {
            compose.setContent {
                DesktopStarbornTheme(services) {
                    Box(Modifier.size(400.dp, 400.dp)) {
                        DesktopAttackFxStyle.entries.forEachIndexed { idx, style ->
                            DesktopCombatHit(
                                cue = DesktopCombatCue(
                                    token = (idx + 1).toLong(),
                                    targetId = "enemy_1",
                                    label = "-45",
                                    color = Color.Cyan,
                                    kind = DesktopCueKind.HIT,
                                    amount = 45,
                                    critical = idx % 2 == 0,
                                    heroFxStyle = style
                                ),
                                flashesDisabled = false,
                                modifier = Modifier.size(160.dp)
                            )
                        }
                        DesktopDamageBubble(
                            cue = DesktopCombatCue(
                                token = 99L,
                                targetId = "enemy_1",
                                label = "-32 VANTAGE",
                                color = Color(0xFFFFC857),
                                kind = DesktopCueKind.HIT,
                                amount = 32,
                                isVantage = true
                            )
                        )
                        DesktopTargetedWarningBadge(skillName = "Aethel Surge", flashesDisabled = false, modifier = Modifier.size(120.dp))
                    }
                }
            }
            compose.mainClock.advanceTimeBy(120)
            DesktopAttackFxStyle.entries.forEach { style ->
                compose.onNodeWithTag("combat-hero-fx-${style.name.lowercase()}").assertExists()
            }
            compose.onNodeWithText("VANTAGE!").assertIsDisplayed()
            compose.onNodeWithTag("combat-targeted-badge").assertExists()
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test
    fun phase2_astraConsolesAndStandaloneSkillTreeRenderNodeGraph() = withServices { services ->
        assertTrue(services.startNewGame(novaPrototype = true))
        runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
        runBlocking { withTimeout(10_000) { services.exploration.uiState.first { !it.isLoading } } }
        services.exploration.openSkillTree("nova")
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline && services.exploration.uiState.value.skillTreeOverlay == null) {
            Thread.sleep(20)
        }
        val overlayTree = requireNotNull(services.exploration.uiState.value.skillTreeOverlay).copy(availableAp = 4)
        val firstNodeName = overlayTree.branches.first().nodes.first().name
        val uiStateHolder = androidx.compose.runtime.mutableStateOf(
            ExplorationUiState(
                isAstraRecreationVisible = true,
                astraPrizesAvailable = true,
                astraArcadeTokens = 18,
                astraArcadeEntries = listOf(
                    AstraArcadeStatusUi(
                        id = "harmonic_pulse",
                        title = "Harmonic Pulse",
                        worldHint = "World 6",
                        discovered = true,
                        installed = true,
                        highScore = 42000,
                        claimedTiers = setOf("BRONZE", "SILVER", "GOLD")
                    )
                ),
                isMenuOverlayVisible = false
            )
        )
        compose.setContent {
            DesktopStarbornTheme(services) {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    DesktopRuntimeOverlays(services, services.exploration, uiStateHolder.value)
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("astra-recreation-console").assertExists()
        compose.onNodeWithText("Tokens: 18 · 1/1").assertIsDisplayed()

        compose.runOnIdle {
            uiStateHolder.value = ExplorationUiState(
                skillTreeOverlay = overlayTree,
                isMenuOverlayVisible = false
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("standalone-skill-tree-console").assertExists()
        compose.onNodeWithText("4 AP Available").assertIsDisplayed()
        compose.onAllNodesWithText(firstNodeName).assertCountEquals(2)
    }

    @Test
    fun phase3_sfxFourVoicePolyphonyAndAsyncAmbientCrossfade() {
        val assetProvider = DesktopAssetProvider()
        val clips = CopyOnWriteArrayList<TestAudioClip>()
        val driver = DesktopAudioDriver(
            assetProvider = assetProvider,
            prewarmDefaults = false,
            clipFactory = { TestAudioClip().also { clips.add(it) }.clip }
        )
        try {
            assertTrue(
                "sfx_combat_transition_slam must be in DEFAULT_PREWARM_CUES",
                "sfx_combat_transition_slam" in DesktopAudioDriver.DEFAULT_PREWARM_CUES
            )
            // Fire 4 overlapping instances of the same non-looping battle SFX
            repeat(4) {
                driver.execute(AudioCommand.Play(AudioCueType.BATTLE, "ui_click", loop = false))
            }
            val deadline = System.currentTimeMillis() + 3_000L
            while (System.currentTimeMillis() < deadline && driver.activeVoiceCount(AudioCueType.BATTLE, "ui_click") < 4) {
                Thread.sleep(15)
            }
            assertEquals("All 4 polyphonic SFX voices should remain active concurrently", 4, driver.activeVoiceCount(AudioCueType.BATTLE, "ui_click"))
            assertTrue("First 4 clips must all still be running", clips.take(4).all { it.running && !it.closed })

            // 5th voice evicts only the oldest voice (#0), keeping 4 active voices
            driver.execute(AudioCommand.Play(AudioCueType.BATTLE, "ui_click", loop = false))
            val deadline2 = System.currentTimeMillis() + 3_000L
            while (System.currentTimeMillis() < deadline2 && clips.size < 5) {
                Thread.sleep(15)
            }
            Thread.sleep(40)
            assertEquals(4, driver.activeVoiceCount(AudioCueType.BATTLE, "ui_click"))
            assertTrue("Oldest voice must be evicted when exceeding MAX_SFX_VOICES_PER_CUE", clips[0].closed)
            assertTrue("Newest 4 voices must remain running", clips.drop(1).take(4).all { it.running })
        } finally {
            driver.release()
        }

        // Verify slow AMBIENT decode runs on audioLoader without blocking musicWorker commands
        val ambientGate = CountDownLatch(1)
        val releaseAmbient = CountDownLatch(1)
        val calls = AtomicInteger(0)
        val asyncDriver = DesktopAudioDriver(
            assetProvider = assetProvider,
            prewarmDefaults = false,
            clipFactory = {
                if (calls.incrementAndGet() == 1) {
                    ambientGate.countDown()
                    releaseAmbient.await(5, TimeUnit.SECONDS)
                }
                TestAudioClip().clip
            }
        )
        try {
            asyncDriver.execute(AudioCommand.Play(AudioCueType.AMBIENT, "amb_vent", loop = true))
            assertTrue(ambientGate.await(3, TimeUnit.SECONDS))
            // While amb_vent is stalled decoding on audioLoader, musicWorker can still start music!
            asyncDriver.execute(AudioCommand.Play(AudioCueType.MUSIC, "music_title_theme", loop = true))
            val musicDeadline = System.currentTimeMillis() + 3_000L
            while (System.currentTimeMillis() < musicDeadline && !asyncDriver.isPlaying(AudioCueType.MUSIC, "music_title_theme")) {
                Thread.sleep(15)
            }
            assertTrue("Music playback must not stall while AMBIENT decodes on audioLoader", asyncDriver.isPlaying(AudioCueType.MUSIC, "music_title_theme"))
        } finally {
            releaseAmbient.countDown()
            asyncDriver.release()
        }
    }

    @Test
    fun phase4_5_6_titleStarfieldScreeningCatalogVictoryLootCookingArtAndMacIcon() = withServices { services ->
        // Phase 6: macOS icon.icns exists with valid 'icns' header
        val icnsBytes = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.icns")?.use { it.readBytes() }
        assertNotNull("icon.icns must be packaged on desktop resources classpath", icnsBytes)
        assertEquals("icns", String(icnsBytes!!.copyOfRange(0, 4), Charsets.US_ASCII))

        // Phase 4b: Screening Room catalog discovers illustrated World 1-6 cinematics
        val catalog = buildDesktopScreeningCatalog(services)
        assertTrue("Screening catalog should contain illustrated scenes", catalog.size >= 10)
        assertTrue(catalog.any { it.scene.id == "intro_prologue" })
        assertTrue(catalog.any { it.scene.id == "scene_w6_silence_rises" })

        // Phase 4c: Victory spoils renders legendary item icon & rarity badge
        val legendaryItem = services.itemRepository.allItems().firstOrNull { it.rarity.equals("legendary", true) }
        assertNotNull("Catalog should contain at least one legendary item", legendaryItem)
        val payload = CombatResultPayload(
            outcome = CombatResultPayload.Outcome.VICTORY,
            rewardXp = 250,
            rewardCredits = 100,
            rewardItems = mapOf(legendaryItem!!.id to 1)
        )
        compose.setContent {
            DesktopStarbornTheme(services) {
                Box(Modifier.size(900.dp, 700.dp)) {
                    DesktopVictoryDialog(
                        services = services,
                        payload = payload,
                        itemNameResolver = { legendaryItem.name },
                        portraitById = emptyMap(),
                        highContrastMode = false,
                        onContinue = {}
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("victory-loot-${legendaryItem.id}").assertExists()
        compose.onNodeWithTag("victory-loot-icon-${legendaryItem.id}", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("LEGENDARY", useUnmergedTree = true).assertExists()
    }

    @Test
    fun phase5_cookingStationRendersChefPortraitsAndRecipeIcons() = withServices { services ->
        services.startNewGame()
        val sortedMeals = services.craftingService.cookingRecipes
            .filter { it.category == "meal" }
            .sortedWith(
                compareByDescending<CookingRecipe> { services.craftingService.canCook(it) }
                    .thenByDescending { services.craftingService.isRecipeRelevant(it) }
            )
        val visibleMeals = sortedMeals.filter { services.craftingService.isRecipeRelevant(it) }.ifEmpty { sortedMeals }
        val autoSelected = visibleMeals.first()
        compose.setContent {
            DesktopStarbornTheme(services) {
                Box(Modifier.size(1100.dp, 760.dp)) {
                    DesktopCookingScreen(
                        craftingService = services.craftingService,
                        inventoryService = services.inventoryService,
                        source = "Astra Galley",
                        onBack = {},
                        services = services
                    )
                }
            }
        }
        compose.waitForIdle()
        val firstChef = services.craftingService.availableChefs().first()
        compose.onNodeWithTag("cooking-chef-portrait-$firstChef", useUnmergedTree = true).assertExists()
        assertTrue("Starter meal should be discovered", services.craftingService.isRecipeDiscovered(autoSelected))
        compose.onNodeWithTag("cooking-detail-icon-${autoSelected.id}", useUnmergedTree = true).assertExists()
        val firstIngredient = autoSelected.ingredients.keys.first()
        compose.onNodeWithTag("cooking-ingredient-icon-$firstIngredient", useUnmergedTree = true).assertExists()
    }
}
