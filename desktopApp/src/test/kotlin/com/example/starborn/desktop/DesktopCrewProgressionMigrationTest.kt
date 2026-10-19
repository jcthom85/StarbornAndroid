package com.example.starborn.desktop

import com.example.starborn.domain.leveling.CrewBuildPrototype as Crew
import com.example.starborn.domain.session.GameSessionState
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class DesktopCrewProgressionMigrationTest {
    @Test fun everyPlayerLoadPathAdoptsLegacyBuildExactlyOnce() {
        val directory = Files.createTempDirectory("starborn-desktop-crew-migration-").toFile()
        try {
            val saves = DesktopSaveManager(directory)
            val old = GameSessionState(worldId = "world_1", playerId = "nova", partyMembers = listOf("nova"),
                playerAp = 5, unlockedSkills = setOf("nova_light_fingers", "nova_smoke_bomb"),
                completedQuests = setOf("w1_mq01"), learnedSchematics = setOf("kept_recipe"))
            for (slot in -1..3) {
                assertTrue(saves.saveGame(slot, old))
                val loaded = requireNotNull(saves.loadGame(slot))
                assertEquals(Crew.VERSION, loaded.progressionPrototypeVersion)
                assertEquals(11, loaded.playerAp)
                assertEquals(setOf("nova_smoke_bomb"), loaded.unlockedSkills)
                assertEquals(old.learnedSchematics, loaded.learnedSchematics)
                assertTrue(saves.saveGame(slot, loaded))
                assertEquals(loaded, saves.loadGame(slot))
            }
        } finally { directory.deleteRecursively() }
    }
}
