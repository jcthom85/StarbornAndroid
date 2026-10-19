package com.example.starborn

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.LegacyCrewProgression
import com.example.starborn.domain.session.migrateOpeningNarrativeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Explicit copied-save acceptance; the supplied historical file and normal saves stay untouched. */
class CrewLegacySnapshotInstrumentedTest {
    @Test fun copiedHistoricalSessionPreservesProgressAndCreditsOnlyOnce() = runBlocking {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val source = File(base.getExternalFilesDir(null), "crew-legacy-original.pb")
        assertTrue("Supply a copied historical save before running this acceptance check", source.isFile)
        val context = IsolatedProgressionContext(base)
        val directory = File(context.filesDir, "datastore").apply { mkdirs() }
        source.copyTo(File(directory, "game_session_slot3.pb"))
        val disk = GameSessionPersistence(directory)
        val old = requireNotNull(disk.readSlot(3))
        assertEquals("This check requires a real version-0 snapshot", 0, old.progressionPrototypeVersion)
        val opening = old.migrateOpeningNarrativeState()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var services: AppServices
        instrumentation.runOnMainSync { services = AppServices(context) }
        try {
            // Wait for startup's empty-store restore before explicitly loading the copied slot.
            delay(1500)
            withTimeout(15000) {
                withContextMain { assertTrue(services.loadSlot(3)) }
            }
            val migrated = services.sessionStore.state.value
            assertEquals(Nova.VERSION, migrated.progressionPrototypeVersion)
            assertEquals(opening.inventory, migrated.inventory)
            assertEquals(opening.playerCredits, migrated.playerCredits)
            assertEquals(opening.completedQuests, migrated.completedQuests)
            assertEquals(opening.learnedSchematics, migrated.learnedSchematics)
            // GameSessionStore already removes retired Ollie records independently of crew migration.
            assertEquals(opening.equippedWeapons - "ollie", migrated.equippedWeapons)
            assertEquals(opening.equippedArmors - "ollie", migrated.equippedArmors)
            val learnedCombat = opening.unlockedSkills.intersect(LegacyCrewProgression.retainedCombatSkills)
            assertTrue(migrated.unlockedSkills.containsAll(learnedCombat))
            val credit = opening.unlockedSkills.sumOf { LegacyCrewProgression.nodeCosts[it] ?: 0 }
            assertTrue(migrated.playerAp >= opening.playerAp + credit)
            withContextMain {
                services.saveSlot(3)
                assertTrue(services.loadSlot(3))
            }
            assertEquals(migrated.playerAp, services.sessionStore.state.value.playerAp)
            assertEquals(migrated.progressionAwardClaims, services.sessionStore.state.value.progressionAwardClaims)
            assertEquals(Nova.VERSION, requireNotNull(disk.readSlot(3)).progressionPrototypeVersion)
            File(base.getExternalFilesDir(null), "crew-legacy-migration.txt").writeText(
                "Historical copy: version=${old.progressionPrototypeVersion}; room=${old.roomId}; " +
                    "quests=${old.completedQuests.size}; AP=${old.playerAp}; legacyCredit=$credit\n" +
                    "Migrated: version=${migrated.progressionPrototypeVersion}; AP=${migrated.playerAp}; " +
                    "claims=${migrated.progressionAwardClaims}; repeated load preserved AP\n")
        } finally { instrumentation.runOnMainSync { services.release() } }
    }

    private suspend fun withContextMain(block: suspend () -> Unit) =
        kotlinx.coroutines.withContext(Dispatchers.Main) { block() }
}
