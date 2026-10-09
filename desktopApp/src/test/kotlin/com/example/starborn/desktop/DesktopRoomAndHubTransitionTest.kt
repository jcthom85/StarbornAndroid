package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import com.example.starborn.feature.exploration.viewmodel.RoomTransitionUi
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class DesktopRoomAndHubTransitionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun nodeDeploymentTransitionRunsPhasesAndExecutesAction() {
        compose.mainClock.autoAdvance = false
        lateinit var travel: DesktopHubTravel
        lateinit var scope: CoroutineScope
        var actionExecuted = false

        compose.setContent {
            scope = rememberCoroutineScope()
            travel = remember { DesktopHubTravel(scope) }
            DesktopStarbornTheme {
                DesktopHubTransitionOverlay(travel = travel)
            }
        }

        compose.runOnIdle {
            assertTrue(
                travel.requestNodeDeployment(
                    scope = scope,
                    nodeTitle = "Slag Works",
                    regionTitle = "World 1",
                    entryRoomTitle = "Lower Foundry",
                    description = "Heavy industrial processing zone.",
                    action = { actionExecuted = true }
                )
            )
            assertTrue(travel.busy)
            assertTrue(travel.transitionType is DesktopHubTransitionType.DeployToNode)
        }

        // Advance into Phase 1 (fading to black)
        compose.mainClock.advanceTimeBy(150)
        compose.runOnIdle {
            assertFalse("Action should only fire once screen is fully obscured", actionExecuted)
        }

        // Advance to Phase 2 (black achieved, action fired, card revealing)
        compose.mainClock.advanceTimeBy(150)
        compose.runOnIdle {
            assertTrue("Action should have executed behind black overlay", actionExecuted)
        }

        // Card text is on screen
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithText("SLAG WORKS").assertExists()
        compose.onNodeWithText("// WORLD 1 //").assertExists()
        compose.onNodeWithText("INITIAL INFILTRATION: LOWER FOUNDRY").assertExists()

        // Advance past hold & dissolve (Phase 3)
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle {
            assertFalse(travel.busy)
            assertEquals(DesktopHubTransitionType.None, travel.transitionType)
        }
    }

    @Test
    fun hubReturnTransitionDisplaysSubtleDisengageAndCompletes() {
        compose.mainClock.autoAdvance = false
        lateinit var travel: DesktopHubTravel
        lateinit var scope: CoroutineScope
        var returned = false

        compose.setContent {
            scope = rememberCoroutineScope()
            travel = remember { DesktopHubTravel(scope) }
            DesktopStarbornTheme {
                DesktopHubTransitionOverlay(travel = travel)
            }
        }

        compose.runOnIdle {
            assertTrue(
                travel.requestHubReturn(
                    scope = scope,
                    action = { returned = true }
                )
            )
            assertTrue(travel.transitionType is DesktopHubTransitionType.ReturnToHub)
        }

        compose.mainClock.advanceTimeBy(220)
        compose.runOnIdle {
            assertTrue(returned)
        }

        // Advance until overlay settles
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle {
            assertFalse(travel.busy)
            assertEquals(DesktopHubTransitionType.None, travel.transitionType)
        }
    }

    @Test
    fun roomTransitionBackdropHandlesParallaxWithoutErrors() {
        val directory = Files.createTempDirectory("starborn-room-transition-").toFile()
        val services = DesktopAppServices(directory)
        try {
            assertTrue(services.startNewGame())
            val room = services.worldDataSource.loadRooms().first { it.id == "pit_nova_bunk" }
            val transitionUi = ExplorationUiState(
                currentRoom = room,
                roomTransition = RoomTransitionUi(
                    id = 1L,
                    direction = "east",
                    fromRoomId = "pit_jed_bunk",
                    toRoomId = "pit_nova_bunk",
                    fromBackgroundImage = "images/rooms/world_1/pit_L1_landing_v5.webp"
                )
            )

            var progress by mutableFloatStateOf(0f)
            compose.setContent {
                DesktopStarbornTheme {
                    Box(Modifier.size(900.dp, 600.dp)) {
                        DesktopRoomTransitionBackdrop(
                            services = services,
                            ui = transitionUi,
                            progress = progress,
                            background = "images/rooms/world_1/pit_L1_landing_v5.webp"
                        ) { layout ->
                            DesktopExplorationPanels(
                                services = services,
                                ui = transitionUi,
                                layout = layout,
                                description = room.description,
                                plan = null,
                                isDark = false,
                                blocked = false,
                                transitionProgress = progress
                            ) {}
                        }
                    }
                }
            }

            compose.waitForIdle()
            progress = 0.5f
            compose.waitForIdle()
            progress = 1.0f
            compose.waitForIdle()
            compose.onNodeWithTag("exploration-narrative").assertExists()
        } finally {
            services.close()
            directory.deleteRecursively()
        }
    }
}
