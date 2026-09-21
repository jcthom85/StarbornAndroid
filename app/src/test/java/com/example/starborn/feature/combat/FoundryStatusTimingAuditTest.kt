package com.example.starborn.feature.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.*
import com.example.starborn.domain.combat.*
import org.junit.Assert.*
import org.junit.Test

/** Effects count affected-actor turns, including skipped turns. */
class FoundryStatusTimingAuditTest {
    @Test fun selfEffectsRefreshWithoutImmediateTickAndPersistAcrossOtherActors() {
        val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
        val registry = StatusRegistry(assets.loadStatuses())
        val engine = CombatEngine(statusRegistry = registry)
        val skills = assets.loadSkills().associateBy { it.id }
        val processor = CombatActionProcessor(engine, registry, skills::get, random = SeededCombatRandom(41))
        fun actor(id: String, side: CombatSide) = Combatant(id, id, side, StatBlock(120, 10, 10, 10, 10, 10, 10))
        var state = engine.beginEncounter(CombatSetup(
            playerParty = listOf(actor("orion", CombatSide.PLAYER)),
            enemyParty = listOf(actor("golem", CombatSide.ENEMY))))
        state = state.copy(combatants = state.combatants + ("orion" to state.combatants.getValue("orion").copy(hp = 50)))
        state = processor.execute(state, CombatAction.SkillUse("orion", "orion_nano_repair", listOf("orion"))) { CombatReward() }
        assertEquals(50, state.combatants.getValue("orion").hp)
        val duration = state.combatants.getValue("orion").statusEffects.single { it.id == "regen" }.remainingTurns
        state = processor.execute(state, CombatAction.SkillUse("golem", "slag_cooldown", listOf("golem"))) { CombatReward() }
        assertEquals(50, state.combatants.getValue("orion").hp)
        assertEquals(2, state.combatants.getValue("golem").statusEffects.single { it.id == "radiators_exposed" }.remainingTurns)
        state = processor.execute(state, CombatAction.Defend("orion")) { CombatReward() }
        assertTrue(state.combatants.getValue("orion").hp > 50)
        assertEquals(duration - 1, state.combatants.getValue("orion").statusEffects.single { it.id == "regen" }.remainingTurns)
        assertEquals(2, state.combatants.getValue("golem").statusEffects.single { it.id == "radiators_exposed" }.remainingTurns)
        val guardDuration = state.combatants.getValue("orion").buffs.single().remainingTurns
        state = processor.execute(state, CombatAction.Defend("golem")) { CombatReward() }
        assertEquals(guardDuration, state.combatants.getValue("orion").buffs.single().remainingTurns)
        state = processor.execute(state, CombatAction.Defend("orion")) { CombatReward() }
        assertEquals(guardDuration, state.combatants.getValue("orion").buffs.single().remainingTurns)
        // DOT ticks only for the affected actor, even when a different actor heads the timeline.
        state = engine.applyStatus(state, "orion", "meltdown", duration = 0)
        val before = state.combatants.getValue("orion").hp
        val otherTurn = engine.tickEndOfTurn(state, "golem")
        assertEquals(before, otherTurn.combatants.getValue("orion").hp)
        val ownerTurn = engine.tickEndOfTurn(otherTurn, "orion")
        assertTrue(ownerTurn.log.drop(otherTurn.log.size).filterIsInstance<CombatLogEntry.Damage>().any { it.sourceId == "status_meltdown" })
    }

    @Test fun disruptionSurvivesAllyActionsAndCrashSkipsExactlyOneEnemyTurn() {
        val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
        val registry = StatusRegistry(assets.loadStatuses())
        val engine = CombatEngine(statusRegistry = registry)
        val skills = assets.loadSkills().associateBy { it.id }
        val processor = CombatActionProcessor(engine, registry, skills::get,
            forcePhysicalHit = { _, _ -> true }, random = SeededCombatRandom(41))
        fun actor(id: String, side: CombatSide) = Combatant(id, id, side,
            StatBlock(10000, 10, 10, 10, 10, 10, 10))
        fun initial() = engine.beginEncounter(CombatSetup(
            playerParty = listOf(actor("orion", CombatSide.PLAYER), actor("gh0st", CombatSide.PLAYER)),
            enemyParty = listOf(actor("enemy", CombatSide.ENEMY))))
        val disrupted = processor.execute(initial(), CombatAction.SkillUse("orion", "orion_disruption_pulse", listOf("enemy"))) { CombatReward() }
        assertTrue(disrupted.combatants.getValue("enemy").statusEffects.any { it.id == "weak" })
        val afterAlly = processor.execute(disrupted, CombatAction.Defend("gh0st")) { CombatReward() }
        assertEquals(2, afterAlly.combatants.getValue("enemy").statusEffects.single { it.id == "weak" }.remainingTurns)
        val afterEnemy = processor.execute(afterAlly, CombatAction.Defend("enemy")) { CombatReward() }
        assertEquals(1, afterEnemy.combatants.getValue("enemy").statusEffects.single { it.id == "weak" }.remainingTurns)
        val afterSecondEnemy = processor.execute(afterEnemy, CombatAction.Defend("enemy")) { CombatReward() }
        assertFalse(afterSecondEnemy.combatants.getValue("enemy").statusEffects.any { it.id == "weak" })
        val crashed = processor.execute(initial(), CombatAction.SkillUse("gh0st", "gh0st_system_crash", listOf("enemy"))) { CombatReward() }
        assertTrue(crashed.log.filterIsInstance<CombatLogEntry.StatusApplied>().any { it.statusId == "stun" })
        assertTrue(crashed.combatants.getValue("enemy").statusEffects.any { it.id == "stun" })
        val skipped = processor.execute(crashed, CombatAction.Defend("enemy")) { CombatReward() }
        assertEquals(1, skipped.log.filterIsInstance<CombatLogEntry.TurnSkipped>().size)
        assertFalse(skipped.combatants.getValue("enemy").statusEffects.any { it.id == "stun" })
        val recovered = processor.execute(skipped, CombatAction.Defend("enemy")) { CombatReward() }
        assertEquals(1, recovered.log.filterIsInstance<CombatLogEntry.TurnSkipped>().size)
    }
}
