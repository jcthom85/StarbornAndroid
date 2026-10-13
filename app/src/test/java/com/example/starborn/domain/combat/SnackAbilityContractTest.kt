package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.ItemAssetDataSource
import com.example.starborn.data.repository.ItemRepository
import com.example.starborn.domain.inventory.InventoryService
import org.junit.Assert.*
import org.junit.Test

class SnackAbilityContractTest {
    private val catalog = ItemRepository(ItemAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))).apply { load() }
    private val registry = StatusRegistry(emptyList())
    private val player = Combatant("player", "Player", CombatSide.PLAYER, StatBlock(10000, 20, 10, 8, 10, 5, 12),
        weapon = CombatWeapon("blade", "Blade", "sword", WeaponAttack.SingleTarget()))
    private val enemy = Combatant("enemy", "Enemy", CombatSide.ENEMY, StatBlock(10000, 1, 8, 2, 2, 2, 10))

    @Test fun snackBuffsSurviveActivationAndEnemyTurnsAndExpireAfterTheAdvertisedPersonalTurns() {
        for ((id, duration) in listOf("comet_gummies" to 4, "pulse_citrus" to 3, "mineral_trail_mix" to 2)) {
            val inventory = InventoryService(catalog).apply { loadItems(); addItem(id) }
            val engine = CombatEngine(statusRegistry = registry, random = kotlin.random.Random(1))
            val processor = CombatActionProcessor(engine, registry, { null }, inventory::useItem, catalog::findItem, random = SeededCombatRandom(1))
            var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
            state = processor.execute(state, CombatAction.SnackUse("player", id)) { CombatReward() }
            assertTrue(state.combatants.getValue("player").buffs.all { it.remainingTurns == duration })
            assertEquals(5, state.combatants.getValue("player").snackCooldown)
            assertEquals(state, processor.execute(state, CombatAction.SnackUse("player", id)) { CombatReward() })
            repeat(duration) { turn ->
                state = processor.execute(state, CombatAction.BasicAttack("enemy", "player")) { CombatReward() }
                assertEquals(duration - turn, state.combatants.getValue("player").buffs.first().remainingTurns)
                state = processor.execute(state, CombatAction.BasicAttack("player", "enemy")) { CombatReward() }
            }
            assertTrue(state.combatants.getValue("player").buffs.isEmpty())
            repeat(5 - duration) { state = processor.execute(state, CombatAction.BasicAttack("player", "enemy")) { CombatReward() } }
            assertEquals(0, state.combatants.getValue("player").snackCooldown)
            state = processor.execute(state, CombatAction.SnackUse("player", id)) { CombatReward() }
            assertTrue(state.combatants.getValue("player").buffs.isNotEmpty())
            assertEquals(1, inventory.snapshot()[id])
        }
    }

    @Test fun cometCritBuffIncreasesNormalWeaponCritsAcrossMatchedSeeds() {
        val effects = requireNotNull(catalog.findItem("comet_gummies")?.effect?.buffs)
        fun crits(buffed: Boolean): Int = (0..199).count { seed ->
            val engine = CombatEngine(statusRegistry = registry, random = kotlin.random.Random(seed))
            val processor = CombatActionProcessor(engine, registry, { null }, random = SeededCombatRandom(seed))
            var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
            if (buffed) state = engine.applyBuffs(state, "player", effects)
            val resolved = processor.execute(state, CombatAction.BasicAttack("player", "enemy")) { CombatReward() }
            resolved.log.filterIsInstance<CombatLogEntry.Damage>().any { it.sourceId == "player" && it.critical }
        }
        assertTrue("Comet's crit stat must affect eligible weapon attacks", crits(true) >= crits(false) + 30)
    }
}
