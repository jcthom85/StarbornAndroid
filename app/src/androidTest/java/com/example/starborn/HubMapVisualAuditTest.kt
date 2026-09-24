package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.AndroidAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import com.example.starborn.feature.hub.ui.HubScreenContent
import com.example.starborn.feature.hub.viewmodel.*
import com.example.starborn.ui.theme.StarbornTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class HubMapVisualAuditTest {
    @get:Rule val compose = createComposeRule()

    @Test fun everyHubHasReadableStableDestinationsAcrossDisplaySizes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assets = WorldAssetDataSource(AssetJsonReader(AndroidAssetProvider(context), MoshiProvider.instance))
        val hubs = assets.loadHubs()
        val nodes = assets.loadHubNodes()
        var state by mutableStateOf(HubUiState())
        var displayScale by mutableStateOf(1f)
        var fontScale by mutableStateOf(1f)
        val evidence = File(context.getExternalFilesDir(null), "hub-map-audit").apply { mkdirs() }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
        }
        shell("mkdir -p /sdcard/Download/starborn-hub-map-audit")
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density * displayScale, fontScale)) {
                StarbornTheme {
                    HubScreenContent(state, SettingsUiState(),
                        onNodeFocused = { state = state.copy(selectedNodeId = it.id) }, onEnterSelectedNode = {},
                        onLockedPromptDismiss = {}, onMusicVolumeChange = {}, onSfxVolumeChange = {},
                        onVoiceVolumeChange = {}, onToggleTutorials = {}, onToggleVignette = {}, onQuickSave = {})
                }
            }
        }
        listOf("normal", "compact", "large_text", "locked_quest", "before_astra").forEach { mode ->
            compose.runOnIdle { displayScale = if (mode == "compact") 1.15f else 1f; fontScale = if (mode == "large_text") 1.4f else 1f }
            hubs.forEach { hub ->
                val hubNodes = nodes.filter { it.hubId == hub.id }.map { node ->
                    HubNodeUi(node.id, node.title, node.entryRoom, node.position.centerX, node.position.centerY,
                        node.size.first().toFloat(), true, iconPath = node.iconImage,
                        description = "A place worth exploring. Follow its paths to discover what lies ahead.",
                        canEnter = mode != "locked_quest", unlocked = mode != "locked_quest",
                        lockReason = if (mode == "locked_quest") "Continue the main story to open this route." else null)
                } + if (hub.id == "hub_astra") listOf(HubNodeUi("astra_disembark", "Disembark", "", .5f, .94f, 180f, true))
                else if (mode == "before_astra") emptyList() else listOf(HubNodeUi("astra_access", "The Astra", "astra_cargo_bay", .5f, .72f, 240f, true, special = "astra",
                    description = "Return to the crew's ship. Use the bridge console to set course for another world."))
                compose.runOnIdle { state = HubUiState(false, hub, hubNodes, hub.backgroundImage, hubNodes.first().id,
                    trackedQuest = if (mode == "locked_quest") HubQuestUi("audit", "Find the way forward", "Explore ${hubNodes.first().title}", null) else null) }
                compose.waitForIdle()
                hubNodes.forEach { node -> compose.onNodeWithContentDescription("Enter ${node.title}").assertIsDisplayed() }
                val first = compose.onNodeWithContentDescription("Enter ${hubNodes.first().title}").fetchSemanticsNode().boundsInRoot
                compose.runOnIdle { state = state.copy(selectedNodeId = hubNodes.last().id) }
                compose.waitForIdle()
                assertEquals("Selection must not move ${hub.id}", first,
                    compose.onNodeWithContentDescription("Enter ${hubNodes.first().title}").fetchSemanticsNode().boundsInRoot)
                // Capture a real destination's description card, not the return shortcut.
                compose.runOnIdle { state = state.copy(selectedNodeId = hubNodes.first().id) }
                compose.waitForIdle()
                val shot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                File(evidence, "${mode}_${hub.id}.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                shot.recycle()
                shell("cp ${evidence.absolutePath}/${mode}_${hub.id}.png /sdcard/Download/starborn-hub-map-audit/${mode}_${hub.id}.png")
            }
        }
    }
}
