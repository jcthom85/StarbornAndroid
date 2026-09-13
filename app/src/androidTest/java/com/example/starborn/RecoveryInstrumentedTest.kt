package com.example.starborn

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.combat.ui.CombatLifecyclePause
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class RecoveryInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun combatPauseFollowsActivityBackgroundAndResume() {
        var paused = true
        compose.setContent { CombatLifecyclePause { paused = it } }
        compose.runOnIdle { assertFalse(paused) }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertTrue(paused)
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle { assertFalse(paused) }
    }

    @Test fun androidDiskSaveRestoresUnpaidBattleCheckpoint() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Isolated cache files: never open or replace the player's save slots.
        val directory = File(context.cacheDir, "recovery-test-${UUID.randomUUID()}").apply { mkdirs() }
        val persistence = GameSessionPersistence(directory)
        val store = GameSessionStore().apply { restore(GameSessionState(
            playerCredits = 100, inventory = mapOf("medkit" to 3),
            pendingEventCinematics = setOf("scene_anchor_drill"))) }
        store.armBattle("battle")
        store.checkpointBattle()
        store.addCredits(50)
        store.setInventory(mapOf("medkit" to 1))
        persistence.writeAutosave(store.state.value)
        val restored = requireNotNull(persistence.readAutosave())
        assertEquals(100, restored.playerCredits)
        assertEquals(3, restored.inventory["medkit"])
        assertEquals("battle", restored.pendingBattleJson)
        assertEquals(setOf("scene_anchor_drill"), restored.pendingEventCinematics)
        store.finishBattle("battle")
        persistence.writeAutosave(store.state.value)
        assertEquals(150, persistence.readAutosave()?.playerCredits)
        assertEquals("", persistence.readAutosave()?.pendingBattleJson)
    }
}
