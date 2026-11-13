package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.model.Skill
import org.junit.Assert.*
import org.junit.Test

class RingMechanicsTest {
    private val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
    private val registry = StatusRegistry(assets.loadStatuses())
    private val engine = CombatEngine(statusRegistry = registry)
    private val stats = StatBlock(1000, 30, 15, 20, 25, 0, 10, stability = 9999)
    private val player = Combatant("nova", "Nova", CombatSide.PLAYER, stats)
    private val guard = Combatant("guard", "Guard", CombatSide.ENEMY, stats)
    private fun base() = engine.beginEncounter(CombatSetup(listOf(player), listOf(guard)))

    @Test fun mirrorGuardReturnsOnlySingleTargetFocusEnergyAndCapsTheRebound() {
        for ((element, scaling, area, reflected) in listOf(
            listOf("source", "focus", false, true), listOf("source", "focus", true, false),
            listOf("physical", "focus", false, false), listOf("source", "atk", false, false))) {
            val skill = Skill("test", "Test", "nova", "active", 900, description = "",
                scaling = scaling as String, combatTags = listOf(element as String) + if (area as Boolean) listOf("aoe") else emptyList(),
                targeting = if (area) "all_enemies" else "enemy")
            val state = engine.applyStatus(base(), "guard", "ring_guard", 3)
            val result = CombatActionProcessor(engine, registry, { skill }, random = SeededCombatRandom(4))
                .execute(state, CombatAction.SkillUse("nova", "test", listOf("guard"))) { CombatReward() }
            val rebound = result.log.filterIsInstance<CombatLogEntry.Damage>().filter { it.sourceId == "guard" && it.targetId == "nova" }
            assertEquals("$element/$scaling/area=$area", reflected, rebound.isNotEmpty())
            assertTrue(rebound.all { it.amount <= 250 })
        }
    }

    @Test fun declaredFrequenciesReplaceEachOtherAndAlwaysLeaveNeutralDamageUseful() {
        var state = engine.applyStatus(base(), "guard", "ring_frequency_shock", 4)
        fun damage(element: String) = engine.calculateDamage(state.combatants.getValue("nova"), state.combatants.getValue("guard"), 100, element)
        assertEquals(150, damage("burn")); assertEquals(50, damage("shock"))
        assertEquals(100, damage("physical")); assertEquals(100, damage("source"))
        state = engine.applyStatus(state, "guard", "ring_frequency_burn", 4)
        assertFalse(state.combatants.getValue("guard").statusEffects.any { it.id == "ring_frequency_shock" })
        assertEquals(50, damage("burn")); assertEquals(150, damage("shock"))
        assertEquals(100, damage("physical")); assertEquals(100, damage("source"))
    }
}
