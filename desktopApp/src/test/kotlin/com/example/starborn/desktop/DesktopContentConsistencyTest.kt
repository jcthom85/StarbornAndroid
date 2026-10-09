package com.example.starborn.desktop

import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameBootstrap
import com.example.starborn.feature.exploration.presentation.AstraCatalog
import com.example.starborn.feature.exploration.presentation.ItemDetails
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class DesktopContentConsistencyTest {
    private fun withServices(block: (DesktopAppServices) -> Unit) {
        val directory = Files.createTempDirectory("starborn-content-consistency-").toFile()
        val services = DesktopAppServices(directory)
        try { block(services) } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun sharedAstraCatalogIsCompleteAndReferencesAuthoredEntities() = withServices { services ->
        assertEquals(14, AstraCatalog.simulations.size)
        assertEquals(14, AstraCatalog.simulations.map { it.title }.distinct().size)
        AstraCatalog.simulations.forEach { program ->
            assertTrue(program.description.isNotBlank())
            program.enemyIds.forEach { assertNotNull("Unknown simulation enemy: $it", services.enemyDefinitions[it]) }
        }
        assertEquals(10, AstraCatalog.films.size)
        assertEquals(10, AstraCatalog.films.map { it.first }.distinct().size)
        AstraCatalog.films.forEach { (item, cue, metadata) ->
            assertNotNull("Unknown tape: $item", services.itemRepository.findItem(item))
            assertTrue(cue.startsWith("gf_"))
            assertNotNull("Missing film audio track: $cue", services.audioCatalog.track(cue))
            assertTrue("Missing film audio file: $cue", services.assetProvider.exists("raw/$cue.mp3"))
            assertTrue(metadata.first.startsWith("Film "))
            assertTrue(metadata.second.isNotBlank())
        }
    }

    @Test fun desktopStartsFromSharedAndroidPolicy() = withServices { services ->
        val expected = GameBootstrap.newGame(services.worldDataSource.loadCharacters(), services.itemRepository.allItems().toList())
        assertTrue(services.startNewGame())
        val actual = services.sessionStore.state.value
        assertEquals(expected.partyMembers, actual.partyMembers)
        assertEquals(expected.roomId, actual.roomId)
        assertEquals(expected.equippedWeapons, actual.equippedWeapons)
        assertEquals(expected.equippedArmors, actual.equippedArmors)
        assertFalse(actual.equippedWeapons.containsKey("nova"))
        assertFalse(actual.equippedArmors.containsKey("nova"))
        assertTrue(actual.activeQuests.contains(GameBootstrap.startingQuest))
    }

    @Test fun debugStartingGearUsesOriginalItems() = withServices { services ->
        val seed = GameBootstrap.newGame(services.worldDataSource.loadCharacters(), services.itemRepository.allItems().toList(), true)
        assertEquals(listOf("nova", "zeke", "orion", "gh0st"), seed.partyMembers)
        assertEquals(GameBootstrap.defaultWeapons, seed.equippedWeapons.filterKeys { it in seed.partyMembers })
        assertEquals(GameBootstrap.defaultArmors, seed.equippedArmors.filterKeys { it in seed.partyMembers })
        seed.equippedItems.values.forEach { assertNotNull(services.itemRepository.findItem(it)) }
    }

    @Test fun newGamePlusPreservesProgressionAndResetsStory() = withServices { services ->
        val players = services.worldDataSource.loadCharacters()
        val items = services.itemRepository.allItems().toList()
        val previous = GameBootstrap.newGame(players, items, true).copy(playerLevel = 20, playerXp = 1234,
            playerCredits = 8000, inventory = mapOf("vhs_tape_01" to 1), completedMilestones = setOf("ms_game_complete"))
        val seed = GameBootstrap.newGamePlus(players, items, previous)
        assertEquals(previous.partyMembers, seed.partyMembers)
        assertEquals(previous.inventory, seed.inventory)
        assertEquals(previous.equippedWeapons, seed.equippedWeapons)
        assertEquals(previous.equippedArmors, seed.equippedArmors)
        assertEquals(previous.equippedItems, seed.equippedItems)
        assertEquals(20, seed.playerLevel)
        assertEquals(1234, seed.playerXp)
        assertEquals(8000, seed.playerCredits)
        assertEquals(setOf("ms_master_protocol_active"), seed.completedMilestones)
        assertEquals("pit_nova_bunk", seed.roomId)
        assertTrue(seed.activeQuests.isEmpty())
    }

    @Test fun desktopNewGamePlusStartsOriginalQuestAfterRestoringItsSeed() = withServices { services ->
        assertTrue(services.startNewGame())
        services.sessionStore.restore(services.sessionStore.state.value.copy(completedMilestones = setOf("ms_game_complete"), playerLevel = 20))
        assertTrue(services.startNewGamePlus())
        val actual = services.sessionStore.state.value
        assertEquals(20, actual.playerLevel)
        assertTrue(actual.activeQuests.contains(GameBootstrap.startingQuest))
        assertEquals(GameBootstrap.startingStage, actual.questStageById[GameBootstrap.startingQuest])
        assertTrue(actual.completedMilestones.contains("ms_master_protocol_active"))
    }

    @Test fun itemDetailsDoNotInventAbsentValues() {
        assertTrue(ItemDetails.lines(Item("fixture", "Fixture", type = "material")).isEmpty())
        val armor = Item("fixture", "Fixture", type = "armor", equipment = Equipment("armor", defense = 5))
        assertEquals(listOf("Slot: Armor", "Defense: 5"), ItemDetails.lines(armor))
    }

    @Test fun itemDetailsIncludeCombatPropertiesAndResolveAuthoredNames() {
        val item = Item("fixture", "Fixture", type = "weapon", equipment = Equipment("weapon", attackStyle = "charged_splash",
            attackChargeTurns = 2, attackSplashMultiplier = .5, attackElement = "fire", statusOnHit = "burn", statusChance = .25),
            effect = ItemEffect(restoreHp = 30, learnSchematic = "recipe", buffs = listOf(BuffEffect("defense", 3, 2)), usesPerBattle = 1))
        val lines = ItemDetails.lines(item) { mapOf("burn" to "Burning", "recipe" to "Cryo-Inductor")[it] ?: it }
        assertTrue(lines.contains("Charge: 2 turns"))
        assertTrue(lines.contains("On hit: Burning (25%)"))
        assertTrue(lines.contains("Learn schematic: Cryo-Inductor"))
        assertTrue(lines.contains("Defense: +3 (2 turns)"))
        assertTrue(lines.contains("Restore HP: 30"))
        assertTrue(lines.contains("Uses per battle: 1"))
    }
}
