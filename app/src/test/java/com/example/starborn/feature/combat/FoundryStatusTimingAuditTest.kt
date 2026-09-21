package com.example.starborn.feature.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.*
import com.example.starborn.domain.combat.*
import org.junit.Assert.*
import org.junit.Test

/** Characterizes current timing, not the desired contract for a future fix. */
class FoundryStatusTimingAuditTest {
    @Test fun disruptionExpiresOnInterveningAllyActionAndCrashStunExpiresImmediately() {
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
        assertFalse(afterAlly.combatants.getValue("enemy").statusEffects.any { it.id == "weak" })
        val crashed = processor.execute(initial(), CombatAction.SkillUse("gh0st", "gh0st_system_crash", listOf("enemy"))) { CombatReward() }
        assertTrue(crashed.log.filterIsInstance<CombatLogEntry.StatusApplied>().any { it.statusId == "stun" })
        assertTrue(crashed.log.filterIsInstance<CombatLogEntry.StatusExpired>().any { it.statusId == "stun" })
        assertFalse(crashed.combatants.getValue("enemy").statusEffects.any { it.id == "stun" })
    }
}
