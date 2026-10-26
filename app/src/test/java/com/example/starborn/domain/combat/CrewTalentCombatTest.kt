package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.*
import com.example.starborn.domain.leveling.CrewBuildPrototype as Crew
import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import com.example.starborn.domain.session.*
import com.example.starborn.domain.model.Skill
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

class CrewTalentCombatTest {
    private val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
    private val registry = StatusRegistry(assets.loadStatuses())
    private val random = object : CombatRandom {
        override fun nextDouble(from: Double, until: Double) = 7.5.coerceIn(from, until - .0001)
        override fun nextInt(from: Int, until: Int) = from
    }
    private val engine = CombatEngine(statusRegistry = registry, random = kotlin.random.Random(17))
    private val session = GameSessionState(progressionPrototypeVersion = 1, partyMembers = listOf("nova", "zeke", "orion", "gh0st"))
    private val skills = assets.loadSkills(1).map { Crew.tune(it, session) }.associateBy { it.id } +
        ("orion_test_weak" to Skill("orion_test_weak", "Weak test", "orion", "support", 0, 0, "", statusApplications = listOf("weak"), targeting = "single_enemy"))
    private fun processor(hit: Boolean = true, rng: CombatRandom = random) = CombatActionProcessor(engine, registry, skills::get,
        forcePhysicalHit = { _, _ -> hit }, random = rng, explicitStatusSkillIds = skills.values.filter { it.character != "enemy" }.map { it.id }.toSet(), prototypeMode = true)
    private fun encounter(owner: String, talents: Set<String> = emptySet(), enemyHp: Int = 1000, allyHp: Int = 1000,
                          enemyStatuses: List<StatusEffect> = emptyList(), actorStatuses: List<StatusEffect> = emptyList(), resistance: Int = 0): CombatState {
        val actor = Combatant(owner, owner, CombatSide.PLAYER, StatBlock(1000, 50, 0, 50, 0, 0, 10, stability = 100), talents = talents)
        val ally = Combatant("ally", "Ally", CombatSide.PLAYER, StatBlock(1000, 1, 0, 1, 0, 0, 1, stability = 100))
        val enemy = Combatant("target", "Target", CombatSide.ENEMY, StatBlock(1000, 1, 0, 1, 0, 0, 1, stability = 5000, statusResistance = resistance))
        val state = engine.beginEncounter(CombatSetup(listOf(actor, ally), listOf(enemy)))
        return state.copy(combatants = state.combatants + mapOf(
            owner to state.combatants.getValue(owner).copy(statusEffects = actorStatuses),
            "ally" to state.combatants.getValue("ally").copy(hp = allyHp, stability = 90),
            "target" to state.combatants.getValue("target").copy(hp = enemyHp, statusEffects = enemyStatuses)))
    }
    private fun cast(s: CombatState, owner: String, skill: String, target: String = "target", p: CombatActionProcessor = processor()) =
        p.execute(s, CombatAction.SkillUse(owner, skill, listOf(target))) { CombatReward() }
    private fun hit(s: CombatState) = s.log.filterIsInstance<CombatLogEntry.Damage>().last().amount
    private fun status(s: CombatState, target: String, id: String) = s.combatants.getValue(target).statusEffects.firstOrNull { it.id == id }
    private fun allOwned(owner: String): Set<String> {
        val store = GameSessionStore().apply { restore(session.copy(playerAp = 20, completedMilestones = setOf("ms_w4_mq20_complete"))) }
        Crew.trees.single { it.character == owner }.branches.values.flatten().forEach { assertTrue(store.purchasePrototypeNode(it.id)) }
        assertEquals(0, store.state.value.playerAp)
        return Crew.ownedTalents(store.state.value, owner)
    }

    @Test fun coverProtectsOneLivingAllyRestoresCappedStabilityAndUsesOrdinaryCooldown() {
        val owned = allOwned("zeke")
        val result = cast(encounter("zeke", owned), "zeke", Crew.COVER_SKILL, "ally")
        assertEquals(2, status(result, "ally", "shield")?.remainingTurns)
        assertEquals(1, status(result, "ally", "shield")?.stacks)
        assertEquals(100, result.combatants.getValue("ally").stability)
        assertEquals(1000, result.combatants.getValue("ally").hp)
        assertEquals(3, result.combatants.getValue("zeke").activeCooldowns[Crew.COVER_SKILL])
        assertFalse(Crew.LAST_LINE in result.combatants.getValue("zeke").talentTriggers)
        val invalid = encounter("zeke", owned, allyHp = 0)
        assertEquals(invalid, cast(invalid, "zeke", Crew.COVER_SKILL, "ally"))
        assertEquals(invalid, cast(invalid, "zeke", Crew.COVER_SKILL, "target"))
    }

    @Test fun lastLineRequiresCooldownAndStrictEmergencyAtCommitAndOnlyBypassesOnce() {
        val base = encounter("zeke", allOwned("zeke"), allyHp = 299)
        val actor = base.combatants.getValue("zeke").copy(activeCooldowns = mapOf(Crew.COVER_SKILL to 1), momentum = 3)
        val ready = base.copy(combatants = base.combatants + ("zeke" to actor))
        assertTrue(TalentEffects.canEmergencyCover(ready, "zeke", Crew.COVER_SKILL, null))
        val cast = cast(ready, "zeke", Crew.COVER_SKILL, "ally")
        assertEquals(3, cast.combatants.getValue("zeke").activeCooldowns[Crew.COVER_SKILL])
        assertEquals(1, cast.combatants.getValue("zeke").talentTriggers[Crew.LAST_LINE])
        assertEquals(cast, cast(cast, "zeke", Crew.COVER_SKILL, "ally"))
        val atThreshold = ready.copy(combatants = ready.combatants + ("ally" to ready.combatants.getValue("ally").copy(hp = 300)))
        assertEquals(atThreshold, cast(atThreshold, "zeke", Crew.COVER_SKILL, "ally"))
        assertEquals(ready, cast(ready, "zeke", Crew.COVER_SKILL, "target"))
    }

    @Test fun stabilityTalentsDoNotIncreaseHpAndDemolitionAddsToFaultLine() {
        for ((owner, skill, talent) in listOf(Triple("zeke", "zeke_shatter_blow", Crew.FAULT_LINE), Triple("orion", "orion_prism_lance", Crew.PRISM_LENS))) {
            val baseline = cast(encounter(owner), owner, skill)
            val bought = cast(encounter(owner, setOf(talent)), owner, skill)
            assertEquals(hit(baseline), hit(bought))
            assertEquals(((5000 - baseline.combatants.getValue("target").stability) * 1.2).roundToInt(), 5000 - bought.combatants.getValue("target").stability)
        }
        val weak = listOf(StatusEffect("weak", 2))
        val baseline = cast(encounter("zeke", enemyStatuses = weak), "zeke", "zeke_shatter_blow")
        val bought = cast(encounter("zeke", setOf(Crew.FAULT_LINE, Crew.CONTROLLED_DEMOLITION), enemyStatuses = weak), "zeke", "zeke_shatter_blow")
        assertEquals(hit(baseline), hit(bought))
        assertEquals(((5000 - baseline.combatants.getValue("target").stability) * 1.35).roundToInt(), 5000 - bought.combatants.getValue("target").stability)
    }

    @Test fun openingRefreshesWithoutStackingWaitsForAnotherCrewAndNeverAmplifiesDot() {
        val owned = allOwned("zeke")
        var marked = cast(encounter("zeke", owned), "zeke", "zeke_shatter_blow")
        assertEquals(2, status(marked, "target", Crew.OPENING_STATUS)?.remainingTurns)
        assertEquals("zeke", status(marked, "target", Crew.OPENING_STATUS)?.sourceId)
        val ownAttack = processor().execute(marked, CombatAction.BasicAttack("zeke", "target")) { CombatReward() }
        assertNotNull(status(ownAttack, "target", Crew.OPENING_STATUS))
        val dot = engine.applyDamage(marked, "STATUS_erosion", "target", 5, "acid")
        assertEquals(5, hit(dot))
        assertNotNull(status(dot, "target", Crew.OPENING_STATUS))
        val without = marked.copy(combatants = marked.combatants + ("target" to marked.combatants.getValue("target").copy(statusEffects = marked.combatants.getValue("target").statusEffects.filterNot { it.id == Crew.OPENING_STATUS })))
        val baseline = processor().execute(without, CombatAction.BasicAttack("ally", "target")) { CombatReward() }
        val consumed = processor().execute(marked, CombatAction.BasicAttack("ally", "target")) { CombatReward() }
        assertEquals((hit(baseline) * 1.1).roundToInt(), hit(consumed))
        assertNull(status(consumed, "target", Crew.OPENING_STATUS))
        val immune = cast(encounter("zeke", owned, resistance = 100), "zeke", "zeke_shatter_blow")
        assertNull(status(immune, "target", "weak")); assertNull(status(immune, "target", Crew.OPENING_STATUS))
    }

    @Test fun fieldTreatmentRoundsCombinedTriageOnceIgnoresFocusAndDoesNotRevive() {
        val initial = encounter("orion", allOwned("orion"), allyHp = 41)
        val target = initial.combatants.getValue("ally").let { it.copy(combatant = it.combatant.copy(stats = it.combatant.stats.copy(maxHp = 119))) }
        val focused = initial.combatants.getValue("orion").let { it.copy(combatant = it.combatant.copy(stats = it.combatant.stats.copy(focus = 100)), momentum = 3) }
        val prepared = initial.copy(combatants = initial.combatants + mapOf("ally" to target, "orion" to focused))
        val result = cast(prepared, "orion", Crew.TREATMENT_SKILL, "ally")
        assertEquals(70, result.combatants.getValue("ally").hp)
        assertEquals(1, result.log.filterIsInstance<CombatLogEntry.Heal>().size)
        assertEquals(5, status(result, "ally", Crew.AFTERCARE_STATUS)?.potency)
        val threshold = encounter("orion", setOf(Crew.TRIAGE), allyHp = 350)
        assertEquals(550, cast(threshold, "orion", Crew.TREATMENT_SKILL, "ally").combatants.getValue("ally").hp)
        val dead = encounter("orion", allOwned("orion"), allyHp = 0)
        assertEquals(dead, cast(dead, "orion", Crew.TREATMENT_SKILL, "ally"))
        val almostFull = encounter("orion", allOwned("orion"), allyHp = 995)
        assertEquals(1000, cast(almostFull, "orion", Crew.TREATMENT_SKILL, "ally").combatants.getValue("ally").hp)
    }

    @Test fun aftercareHasTwoCastTimeTicksAndRegenOwnsTheSingleRecoverySlot() {
        val cast = cast(encounter("orion", allOwned("orion"), allyHp = 100), "orion", Crew.TREATMENT_SKILL, "ally")
        assertEquals(350, cast.combatants.getValue("ally").hp)
        val changed = cast.copy(combatants = cast.combatants + ("ally" to cast.combatants.getValue("ally").let { it.copy(combatant = it.combatant.copy(stats = it.combatant.stats.copy(maxHp = 2000))) }))
        val one = engine.tickEndOfTurn(changed, "ally")
        assertEquals(400, one.combatants.getValue("ally").hp)
        val two = engine.tickEndOfTurn(one, "ally")
        assertEquals(450, two.combatants.getValue("ally").hp)
        assertNull(status(two, "ally", Crew.AFTERCARE_STATUS))
        val regen = engine.applyStatus(cast, "ally", "regen", 1)
        assertNull(status(regen, "ally", Crew.AFTERCARE_STATUS))
        val ready = regen.copy(combatants = regen.combatants + ("orion" to regen.combatants.getValue("orion").copy(activeCooldowns = emptyMap())))
        val again = cast(ready, "orion", Crew.TREATMENT_SKILL, "ally")
        assertEquals(1, status(again, "ally", "regen")?.remainingTurns)
        assertNull(status(again, "ally", Crew.AFTERCARE_STATUS))
    }

    @Test fun fieldPatchIsCoreRecoveryWithOneTurnShieldAndFocusOnly() {
        val result = cast(encounter("orion", allyHp = 100), "orion", Crew.PATCH_SKILL, "ally")
        assertEquals(118, result.combatants.getValue("ally").hp)
        assertEquals(1, status(result, "ally", "shield")?.remainingTurns)
        assertEquals(3, result.combatants.getValue("orion").activeCooldowns[Crew.PATCH_SKILL])
    }

    @Test fun safeExposureAndSteadyFieldOnlyAffectNewWeakAndRespectImmunity() {
        val talents = allOwned("orion")
        val result = cast(encounter("orion", talents), "orion", "orion_prism_lance")
        assertEquals(2, status(result, "target", "weak")?.remainingTurns)
        val one = cast(encounter("orion", setOf(Crew.SAFE_EXPOSURE)), "orion", "orion_prism_lance")
        assertEquals(1, status(one, "target", "weak")?.remainingTurns)
        val existing = cast(encounter("orion", talents, enemyStatuses = listOf(StatusEffect("weak", 1))), "orion", "orion_prism_lance")
        assertEquals(1, status(existing, "target", "weak")?.remainingTurns)
        assertNull(status(cast(encounter("orion", talents, resistance = 100), "orion", "orion_prism_lance"), "target", "weak"))
        val capped = cast(encounter("orion", talents), "orion", "orion_test_weak")
        assertTrue(requireNotNull(status(capped, "target", "weak")).remainingTurns <= 3)
    }

    @Test fun precisionThresholdsAndCriticalBonusAreSpecificToHeadshot() {
        val at75 = encounter("gh0st", enemyHp = 750)
        val scoped = encounter("gh0st", setOf(Crew.SCOPE), enemyHp = 750)
        assertEquals((hit(cast(at75, "gh0st", "gh0st_headshot")) * 1.1).roundToInt(), hit(cast(scoped, "gh0st", "gh0st_headshot")))
        val base = cast(encounter("gh0st"), "gh0st", "gh0st_headshot")
        val aimed = cast(encounter("gh0st", setOf(Crew.STEADY_AIM)), "gh0st", "gh0st_headshot")
        assertFalse(base.log.filterIsInstance<CombatLogEntry.Damage>().last().critical)
        assertTrue(aimed.log.filterIsInstance<CombatLogEntry.Damage>().last().critical)
        assertEquals(hit(base) * 2, hit(aimed))
        val low = encounter("gh0st", setOf(Crew.EXECUTE), enemyHp = 300)
        val baseLow = encounter("gh0st", enemyHp = 300)
        assertEquals((hit(cast(baseLow, "gh0st", "gh0st_headshot")) * 1.15).roundToInt(), hit(cast(low, "gh0st", "gh0st_headshot")))
        assertEquals(hit(processor().execute(baseLow, CombatAction.BasicAttack("gh0st", "target")) { CombatReward() }), hit(processor().execute(low, CombatAction.BasicAttack("gh0st", "target")) { CombatReward() }))
    }

    @Test fun coolHeadChargesOnlyOnLandedBasicAndSpendsOnValidHeadshotIncludingMiss() {
        val initial = encounter("gh0st", setOf(Crew.COOL_HEAD))
        val charged = processor().execute(initial, CombatAction.BasicAttack("gh0st", "target")) { CombatReward() }
        assertEquals(1, charged.combatants.getValue("gh0st").talentTriggers[Crew.COOL_HEAD])
        val shot = cast(charged, "gh0st", "gh0st_headshot")
        assertEquals(0, shot.combatants.getValue("gh0st").talentTriggers[Crew.COOL_HEAD])
        assertEquals(2, shot.combatants.getValue("gh0st").activeCooldowns["gh0st_headshot"])
        val waiting = charged.copy(combatants = charged.combatants + ("gh0st" to charged.combatants.getValue("gh0st").copy(activeCooldowns = mapOf("gh0st_headshot" to 2))))
        assertEquals(waiting, cast(waiting, "gh0st", "gh0st_headshot"))
        val missRandom = object : CombatRandom {
            override fun nextDouble(from: Double, until: Double) = until - .001
            override fun nextInt(from: Int, until: Int) = from
        }
        val missed = cast(charged, "gh0st", "gh0st_headshot", p = processor(false, missRandom))
        assertEquals(0, hit(missed))
        assertEquals(0, missed.combatants.getValue("gh0st").talentTriggers[Crew.COOL_HEAD])
    }

    @Test fun surgicalStrikeChangesOnlyBasicStabilityAndDisarmAppliesExactlyTwoTurnsWeak() {
        val initial = encounter("gh0st")
        val bought = encounter("gh0st", setOf(Crew.SURGICAL_STRIKE))
        val basic = processor().execute(initial, CombatAction.BasicAttack("gh0st", "target")) { CombatReward() }
        val better = processor().execute(bought, CombatAction.BasicAttack("gh0st", "target")) { CombatReward() }
        assertEquals(hit(basic), hit(better))
        assertEquals(((5000 - basic.combatants.getValue("target").stability) * 1.2).roundToInt(), 5000 - better.combatants.getValue("target").stability)
        val disarm = cast(encounter("gh0st", allOwned("gh0st")), "gh0st", Crew.DISARM_SKILL)
        assertEquals(2, status(disarm, "target", "weak")?.remainingTurns)
        assertEquals(3, disarm.combatants.getValue("gh0st").activeCooldowns[Crew.DISARM_SKILL])
        assertNull(status(cast(encounter("gh0st", resistance = 100), "gh0st", Crew.DISARM_SKILL), "target", "weak"))
    }

    @Test fun recenterPreventsOneStunOrStaggerSkipButDoesNotConsumeAgainstMixedFreeze() {
        val owned = allOwned("gh0st")
        val initial = encounter("gh0st", owned, actorStatuses = listOf(StatusEffect("stun", 2), StatusEffect("stagger", 2)))
        val freed = processor().execute(initial, CombatAction.BasicAttack("gh0st", "target")) { CombatReward() }
        assertFalse(freed.log.any { it is CombatLogEntry.TurnSkipped })
        assertEquals(1, freed.combatants.getValue("gh0st").talentTriggers[Crew.RECENTER])
        assertNull(status(freed, "gh0st", "stun")); assertNull(status(freed, "gh0st", "stagger"))
        val again = engine.applyStatus(freed, "gh0st", "stun", 2)
        val skipped = processor().execute(again, CombatAction.BasicAttack("gh0st", "target")) { CombatReward() }
        assertTrue(skipped.log.any { it is CombatLogEntry.TurnSkipped })
        val mixed = encounter("gh0st", owned, actorStatuses = listOf(StatusEffect("stun", 2), StatusEffect("freeze", 2)))
        val frozen = processor().execute(mixed, CombatAction.BasicAttack("gh0st", "target")) { CombatReward() }
        assertTrue(frozen.log.any { it is CombatLogEntry.TurnSkipped })
        assertFalse(Crew.RECENTER in frozen.combatants.getValue("gh0st").talentTriggers)
    }

    @Test fun questPassivesAndBracedStatusWorkProperlyInCombat() {
        // 1. Corporate Insight (> 50% HP) & Nova Legacy (< 50% HP)
        val highHpBase = cast(encounter("nova"), "nova", "nova_arc_tether")
        val highHpInsight = cast(encounter("nova", setOf(Crew.CORPORATE_INSIGHT)), "nova", "nova_arc_tether")
        assertEquals((hit(highHpBase) * 1.1).roundToInt(), hit(highHpInsight))

        val lowHpState = encounter("nova").let { s ->
            s.copy(combatants = s.combatants + ("nova" to s.combatants.getValue("nova").copy(hp = 400)))
        }
        val lowHpLegacyState = encounter("nova", setOf(Crew.NOVA_LEGACY)).let { s ->
            s.copy(combatants = s.combatants + ("nova" to s.combatants.getValue("nova").copy(hp = 400)))
        }
        assertEquals((hit(cast(lowHpState, "nova", "nova_arc_tether")) * 1.15).roundToInt(),
            hit(cast(lowHpLegacyState, "nova", "nova_arc_tether")))

        // 2. Data Shield blocks hostile silence, jammed, and short for party members
        val shieldedParty = encounter("zeke", setOf(Crew.DATA_SHIELD))
        for (blockedStatus in listOf("silence", "jammed", "short")) {
            val afterStatus = engine.applyStatus(shieldedParty, "ally", blockedStatus, 2, sourceId = "target")
            assertNull(status(afterStatus, "ally", blockedStatus))
        }

        // 3. Braced status is registered, adds +4 flat defense, and blocks hostile stagger/stun
        assertNotNull(registry.definition("braced"))
        val bracedParty = engine.applyStatus(encounter("zeke"), "zeke", "braced", 2, sourceId = "zeke")
        assertNotNull(status(bracedParty, "zeke", "braced"))
        val afterStagger = engine.applyStatus(bracedParty, "zeke", "stagger", 1, sourceId = "target")
        assertNull(status(afterStagger, "zeke", "stagger"))

        // 4. Harden grants Guard, Shield, and Braced
        val hardened = cast(encounter("gh0st"), "gh0st", "gh0st_harden", "gh0st")
        assertNotNull(status(hardened, "gh0st", "guard"))
        assertNotNull(status(hardened, "gh0st", "shield"))
        assertNotNull(status(hardened, "gh0st", "braced"))
    }
}
