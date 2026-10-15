package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

class NovaTalentCombatTest {
    private val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
    private val registry = StatusRegistry(assets.loadStatuses())
    private val engine = CombatEngine(statusRegistry = registry)
    private val skills = assets.loadSkills(1).associateBy { it.id }
    private fun processor() = CombatActionProcessor(engine, registry, skills::get,
        forcePhysicalHit = { _, _ -> true }, random = SeededCombatRandom(17),
        explicitStatusSkillIds = setOf("nova_cryo_vent"))

    private fun encounter(talents: Set<String> = emptySet(), hp: Int = 1000, statuses: List<StatusEffect> = emptyList()): CombatState {
        val nova = Combatant("nova", "Nova", CombatSide.PLAYER,
            StatBlock(500, 50, 0, 50, 0, 0, 10, stability = 2000), talents = talents)
        val enemy = Combatant("target", "Target", CombatSide.ENEMY,
            StatBlock(1000, 1, 0, 1, 0, 0, 1, stability = 2000))
        val state = engine.beginEncounter(CombatSetup(listOf(nova), listOf(enemy)))
        return state.copy(combatants = state.combatants + ("target" to state.combatants.getValue("target").copy(hp = hp, statusEffects = statuses)))
    }
    private fun use(state: CombatState, skill: String) = processor().execute(state,
        CombatAction.SkillUse("nova", skill, listOf("target"))) { CombatReward() }
    private fun hit(state: CombatState) = state.log.filterIsInstance<CombatLogEntry.Damage>().last { it.sourceId == "nova" }

    @Test fun groundedWireChangesOnlyArcTetherStability() {
        val base = use(encounter(), "nova_arc_tether")
        val bought = use(encounter(setOf(Nova.GROUNDED)), "nova_arc_tether")
        assertEquals(hit(base).amount, hit(bought).amount)
        val baseLoss = 2000 - base.combatants.getValue("target").stability
        assertEquals((baseLoss * 1.2).roundToInt(), 2000 - bought.combatants.getValue("target").stability)
    }

    @Test fun hpTalentsCapAtTwentyFivePercentWithoutIncreasingStability() {
        val weak = listOf(StatusEffect("weak", 2))
        val base = use(encounter(statuses = weak), Nova.BREAKER_SKILL)
        val bought = use(encounter(setOf(Nova.START, Nova.SEAM), statuses = weak), Nova.BREAKER_SKILL)
        assertEquals((hit(base).amount * 1.25).roundToInt(), hit(bought).amount)
        assertEquals(base.combatants.getValue("target").stability, bought.combatants.getValue("target").stability)
        val thresholdBase = use(encounter(hp = 750), Nova.BREAKER_SKILL)
        val thresholdTalent = use(encounter(setOf(Nova.START), hp = 750), Nova.BREAKER_SKILL)
        assertEquals(hit(thresholdBase).amount, hit(thresholdTalent).amount)
    }

    @Test fun breakerBoltDealsIndependentFiftyPercentStabilityBonus() {
        val base = use(encounter(), Nova.BREAKER_SKILL)
        val bought = use(encounter(setOf(Nova.BREAKER)), Nova.BREAKER_SKILL)
        assertEquals(hit(base).amount, hit(bought).amount)
        assertEquals(((2000 - base.combatants.getValue("target").stability) * 1.5).roundToInt(),
            2000 - bought.combatants.getValue("target").stability)
    }

    @Test fun cryoVentAppliesBrittleWithoutImplicitFreeze() {
        val result = use(encounter(), "nova_cryo_vent")
        assertTrue(result.combatants.getValue("target").statusEffects.any { it.id == "brittle" && it.remainingTurns == 2 })
        assertFalse(result.combatants.getValue("target").statusEffects.any { it.id == "freeze" })
    }

    @Test fun scatterChargeDamagesEveryEnemyAndHasNoStatuses() {
        val initial = encounter(setOf(Nova.SCATTER))
        val extra = initial.combatants.getValue("target").let { it.copy(combatant = it.combatant.copy(id = "second")) }
        val state = initial.copy(combatants = initial.combatants + ("second" to extra))
        val result = processor().execute(state, CombatAction.SkillUse("nova", Nova.SCATTER_SKILL,
            listOf("target", "second"))) { CombatReward() }
        assertEquals(setOf("target", "second"), result.log.filterIsInstance<CombatLogEntry.Damage>().map { it.targetId }.toSet())
        assertTrue(result.combatants.getValue("target").statusEffects.isEmpty())
        assertTrue(result.combatants.getValue("second").statusEffects.isEmpty())
        assertEquals(2, result.combatants.getValue("nova").activeCooldowns[Nova.SCATTER_SKILL])
    }

    @Test fun keepMovingIsOncePerActionAndTwicePerBattle() {
        var state = encounter(setOf(Nova.MOVING), hp = 1)
        // Fresh targets preserve the same actor's per-battle trigger state.
        repeat(3) { index ->
            val actor = state.combatants.getValue("nova").copy(statusEffects = emptyList())
            val target = state.combatants.getValue("target").copy(hp = 1, breakTurns = 0)
            state = state.copy(combatants = state.combatants + mapOf("nova" to actor, "target" to target), outcome = null)
            state = processor().execute(state, CombatAction.BasicAttack("nova", "target")) { CombatReward() }
            assertEquals(minOf(index + 1, 2), state.combatants.getValue("nova").talentTriggers[Nova.MOVING])
            assertEquals(index < 2, state.combatants.getValue("nova").statusEffects.any { it.id == "shield" && it.remainingTurns == 1 })
        }
    }

    @Test fun damageOverTimeAndNoKillDoNotProcKeepMoving() {
        val initial = encounter(setOf(Nova.MOVING, Nova.START, Nova.SEAM), hp = 1)
        val dot = engine.applyDamage(initial, "nova", "target", 1, "acid")
        assertTrue(dot.combatants.getValue("nova").talentTriggers.isEmpty())
        assertFalse(dot.combatants.getValue("nova").statusEffects.any { it.id == "shield" })
        val noKill = use(encounter(setOf(Nova.MOVING)), Nova.BREAKER_SKILL)
        assertTrue(noKill.combatants.getValue("nova").talentTriggers.isEmpty())
    }

    @Test fun groundedTutorialRequiresARealQualifyingHitAndAcknowledgmentPreventsRepeats() {
        val guide = com.example.starborn.domain.tutorial.NovaProgressionTutorial
        val session = com.example.starborn.domain.session.GameSessionState(progressionPrototypeVersion = 1)
        val initial = encounter(setOf(Nova.GROUNDED))
        val hit = use(initial, "nova_arc_tether")
        assertEquals(guide.GROUNDED, guide.combatHint(session, initial, hit)?.key)
        assertNull(guide.combatHint(session.copy(tutorialCompleted = setOf(guide.GROUNDED)), initial, hit))
        assertNull(guide.combatHint(session.copy(progressionPrototypeVersion = 0), initial, hit))
        assertNull(guide.combatHint(session, initial, use(initial, Nova.BREAKER_SKILL)))
        assertNull(guide.combatHint(session, encounter(), use(encounter(), "nova_arc_tether")))
    }

    @Test fun quietStepsTutorialFollowsNovaActionAndFirstApGuideRespectsOwnershipAndSettings() {
        val guide = com.example.starborn.domain.tutorial.NovaProgressionTutorial
        val session = com.example.starborn.domain.session.GameSessionState(progressionPrototypeVersion = 1, playerAp = 2)
        assertNotNull(guide.firstAp(session, true))
        assertNull(guide.firstAp(session, false))
        assertNull(guide.firstAp(session.copy(playerAp = 0), true))
        assertNull(guide.firstAp(session.copy(progressionPrototypeVersion = 0), true))
        assertNull(guide.firstAp(session.copy(tutorialCompleted = setOf(guide.FIRST_AP)), true))
        assertNull(guide.firstAp(session.copy(purchasedSkillNodes = mapOf(Nova.STEPS to 1)), true))
        val initial = encounter(setOf(Nova.STEPS))
        assertNull(guide.combatHint(session, initial, initial))
        val result = use(initial, Nova.BREAKER_SKILL)
        assertEquals(guide.STEPS, guide.combatHint(session, initial, result)?.key)
        assertNull(guide.combatHint(session.copy(tutorialCompleted = setOf(guide.STEPS)), initial, result))
    }
}
