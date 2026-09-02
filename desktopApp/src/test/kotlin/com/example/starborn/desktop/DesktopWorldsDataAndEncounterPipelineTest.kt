package com.example.starborn.desktop

import com.example.starborn.domain.combat.*
import com.example.starborn.domain.model.Room
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DesktopWorldsDataAndEncounterPipelineTest {

    @Test
    fun testAllSixWorldsIntegrityAndBossCombatants() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_world_pipe_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)

            // 1. Verify all rooms across all worlds can load
            val allRooms: List<Room> = services.worldDataSource.loadRooms()
            assertTrue("Should have a substantial room graph across worlds", allRooms.size >= 30)

            // 2. Test Boss Encounter Definitions across Worlds
            val bossEnemyIds = listOf(
                "acoustic_bulwark",     // World 1 Boss
                "the_iron_warden",      // Heavy Enforcer
                "ruin_guardian",        // Ancient Guardian
                "faulted_loader"        // Heavy Mech
            )

            val enemyCatalog = services.worldDataSource.loadEnemies().associateBy { it.id }
            bossEnemyIds.forEach { bossId ->
                val enemy = enemyCatalog[bossId]
                assertNotNull("Enemy $bossId must exist in assets/enemies.json", enemy)
                assertTrue("Boss $bossId must have HP > 0", (enemy?.hp ?: 0) > 0)
            }

            // 3. Test Full Combat Round Setup with Party vs World 1 Boss
            val bulwark = enemyCatalog["acoustic_bulwark"]!!
            val partyCombatants = listOf(
                Combatant(
                    id = "nova",
                    name = "Nova",
                    side = CombatSide.PLAYER,
                    stats = StatBlock(maxHp = 120, strength = 14, vitality = 12, agility = 11, focus = 10, luck = 10, speed = 12, stability = 100)
                ),
                Combatant(
                    id = "zeke",
                    name = "Zeke",
                    side = CombatSide.PLAYER,
                    stats = StatBlock(maxHp = 140, strength = 16, vitality = 15, agility = 9, focus = 8, luck = 9, speed = 10, stability = 120)
                )
            )

            val enemyCombatant = Combatant(
                id = bulwark.id,
                name = bulwark.name,
                side = CombatSide.ENEMY,
                stats = StatBlock(
                    maxHp = bulwark.hp,
                    strength = bulwark.strength,
                    vitality = bulwark.vitality,
                    agility = bulwark.agility,
                    focus = bulwark.focus,
                    luck = bulwark.luck,
                    speed = bulwark.speed,
                    stability = bulwark.stability ?: 80
                )
            )

            val setup = CombatSetup(
                playerParty = partyCombatants,
                enemyParty = listOf(enemyCombatant)
            )

            val combatState = services.combatEngine.beginEncounter(setup)
            assertNotNull("Combat encounter should be initialized", combatState)
            assertEquals(3, combatState.combatants.size)

            // 4. Verify All 26 Crafting Recipes Parse
            val recipes = services.craftingService.tinkeringRecipes
            assertEquals("Should have exactly 26 tinkering schematics", 26, recipes.size)

            val allItems = services.itemRepository.allItems().associateBy { it.id }
            recipes.forEach { recipe ->
                assertTrue("Recipe must have a valid non-empty result", recipe.result.isNotBlank())
                assertTrue("Recipe must have ingredients", recipe.ingredients.isNotEmpty())
            }

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
