package com.example.starborn.desktop

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class DesktopRewardGearTest {
    @Test fun ownedRewardGearCanBeEquippedWithoutAndroidUnlockListeners() {
        val directory = Files.createTempDirectory("starborn-reward-gear-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            assertTrue(services.startNewGame())
            val exploration = services.exploration
            exploration.equipWeapon("nova", "mining_pistol")
            assertNull(services.sessionStore.state.value.equippedWeapons["nova"])
            // Isolated reward-acquisition fixture, not a claim of desktop UI campaign traversal.
            services.inventoryService.addItem("mining_pistol")
            services.inventoryService.addItem("nova_flux_liner")
            exploration.equipWeapon("nova", "mining_pistol")
            exploration.equipArmor("nova", "nova_flux_liner")
            assertEquals("mining_pistol", services.sessionStore.state.value.equippedWeapons["nova"])
            assertEquals("nova_flux_liner", services.sessionStore.state.value.equippedArmors["nova"])
            assertTrue("mining_pistol" in services.sessionStore.state.value.unlockedWeapons)
            assertTrue("nova_flux_liner" in services.sessionStore.state.value.unlockedArmors)
        } finally { services.close(); directory.deleteRecursively() }
    }
}
