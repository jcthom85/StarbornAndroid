package com.example.starborn

import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.AndroidAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.combat.*
import org.junit.Assert.*
import org.junit.Test

/** Packaged Android assets + production processor; no player save is read or written. */
class CombatStatusTimingDeviceTest {
    @Test fun linkHealsLivingAlliesAndRejectsEnemyTargets() {
        val assets = WorldAssetDataSource(AssetJsonReader(AndroidAssetProvider(
            InstrumentationRegistry.getInstrumentation().targetContext), MoshiProvider.instance))
        val registry = StatusRegistry(assets.loadStatuses())
        val skills = assets.loadSkills().associateBy { it.id }
        assertEquals("all_allies", skills.getValue("nova_link").targeting)
        val engine = CombatEngine(statusRegistry = registry)
        val processor = CombatActionProcessor(engine, registry, skills::get, random = SeededCombatRandom(41))
        fun actor(id: String, side: CombatSide) = Combatant(id, id, side,
            StatBlock(200, 10, 10, 10, 10, 10, 10))
        val initial = engine.beginEncounter(CombatSetup(
            playerParty = listOf(actor("nova", CombatSide.PLAYER), actor("zeke", CombatSide.PLAYER), actor("gh0st", CombatSide.PLAYER)),
            enemyParty = listOf(actor("enemy", CombatSide.ENEMY))))
        val wounded = initial.copy(combatants = initial.combatants.mapValues { (id, value) ->
            value.copy(hp = if (id == "gh0st") 0 else 40)
        })
        val after = processor.execute(wounded,
            CombatAction.SkillUse("nova", "nova_link", listOf("nova", "zeke", "gh0st", "enemy"))) { CombatReward() }
        assertTrue(after.combatants.getValue("nova").hp > 40)
        assertTrue(after.combatants.getValue("zeke").hp > 40)
        assertEquals(0, after.combatants.getValue("gh0st").hp)
        assertEquals(40, after.combatants.getValue("enemy").hp)
        assertTrue(after.log.filterIsInstance<CombatLogEntry.Heal>().none { it.targetId in setOf("enemy", "gh0st") })
    }

    @Test fun ownerTurnsAndSelfEffectsUsePackagedRuntime() {
        val assets = WorldAssetDataSource(AssetJsonReader(AndroidAssetProvider(
            InstrumentationRegistry.getInstrumentation().targetContext), MoshiProvider.instance))
        val registry = StatusRegistry(assets.loadStatuses())
        val skills = assets.loadSkills().associateBy { it.id }
        val engine = CombatEngine(statusRegistry = registry)
        val processor = CombatActionProcessor(engine, registry, skills::get,
            forcePhysicalHit = { _, _ -> true }, random = SeededCombatRandom(41))
        fun actor(id: String, side: CombatSide) = Combatant(id, id, side,
            StatBlock(10000, 10, 10, 10, 10, 10, 10))
        var state = engine.beginEncounter(CombatSetup(
            playerParty = listOf(actor("orion", CombatSide.PLAYER), actor("gh0st", CombatSide.PLAYER)),
            enemyParty = listOf(actor("enemy", CombatSide.ENEMY))))
        state = processor.execute(state, CombatAction.SkillUse("gh0st", "gh0st_system_crash", listOf("enemy"))) { CombatReward() }
        state = processor.execute(state, CombatAction.Defend("orion")) { CombatReward() }
        assertTrue(state.combatants.getValue("enemy").statusEffects.any { it.id == "stun" })
        state = processor.execute(state, CombatAction.Defend("enemy")) { CombatReward() }
        assertEquals(1, state.log.filterIsInstance<CombatLogEntry.TurnSkipped>().size)
        assertFalse(state.combatants.getValue("enemy").statusEffects.any { it.id == "stun" })
        state = processor.execute(state, CombatAction.SkillUse("enemy", "slag_cooldown", listOf("enemy"))) { CombatReward() }
        state = processor.execute(state, CombatAction.Defend("orion")) { CombatReward() }
        assertEquals(2, state.combatants.getValue("enemy").statusEffects.single { it.id == "radiators_exposed" }.remainingTurns)
        state = state.copy(combatants = state.combatants + ("orion" to state.combatants.getValue("orion").copy(hp = 50)))
        state = processor.execute(state, CombatAction.SkillUse("orion", "orion_nano_repair", listOf("orion"))) { CombatReward() }
        assertEquals(50, state.combatants.getValue("orion").hp)
        state = processor.execute(state, CombatAction.Defend("enemy")) { CombatReward() }
        assertEquals(50, state.combatants.getValue("orion").hp)
        state = processor.execute(state, CombatAction.Defend("orion")) { CombatReward() }
        assertTrue(state.combatants.getValue("orion").hp > 50)
    }
}
