package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class AuthoredEncounterRulesTest {
    private val registry = StatusRegistry(listOf(StatusDefinition("jammed", blockSkills = true),
        StatusDefinition("stun", skipReason = "stunned")))
    private fun enemy(id: String, pattern: AuthoredEnemyPattern? = null) = Enemy(id,id,hp=200,
        strength=8,vitality=4,agility=4,focus=4,luck=1,speed=5,xpReward=0,creditReward=0,drops=emptyList(),authored=pattern)
    private val core = enemy("core",AuthoredEnemyPattern(linkedActions = (1..4).associate { "shot$it" to "part$it" }))
    private val parts = (1..4).map { enemy("part$it",AuthoredEnemyPattern(linkedTo="core")) }
    private val definitions = (listOf(core)+parts).associateBy { it.id }
    private fun state(defs: Map<String,Enemy> = definitions): CombatState {
        val actors=defs.keys.map { Combatant(it,it,CombatSide.ENEMY,StatBlock(200,8,4,4,4,1,5)) } +
            Combatant("nova","Nova",CombatSide.PLAYER,StatBlock(200,8,4,4,4,1,5))
        return CombatState(actors.map { TurnSlot(it.id,0) },0,actors.associate { it.id to CombatantState(it,200,100) })
    }
    private fun prepared(s:CombatState, part:String="part1")=s.copy(authored=s.authored.copy(preparations=mapOf(
        "core" to EnemyPreparation("core",part,"shot1",listOf("nova"),"Weapon charging"))))

    @Test fun everyDestructionOrderCancelsTheRightWeaponWithoutDestroyingTheBody() {
        fun permutations(v:List<String>):List<List<String>> = if(v.isEmpty()) listOf(emptyList()) else
            v.flatMap { x -> permutations(v-x).map { listOf(x)+it } }
        val orders=permutations(parts.map { it.id });assertEquals(24,orders.size)
        for(order in orders) {
            var s=state()
            for(part in order) {
                s=prepared(s,part)
                s=s.copy(combatants=s.combatants+(part to s.combatants.getValue(part).copy(hp=0)))
                s=AuthoredEncounterRules.reconcile(s,definitions,registry)
                assertTrue(s.authored.preparations.isEmpty())
                assertTrue("core" in s.authored.recovery)
                assertTrue(s.combatants.getValue("core").isAlive)
                assertNull(CombatEngine().resolveOutcome(s) { CombatReward() }.outcome)
            }
        }
    }
    @Test fun disablingOrBreakingAWeaponCancelsItsQueuedAction() {
        for(effect in listOf("jammed","stun","break")) {
            var s=prepared(state());val part=s.combatants.getValue("part1")
            s=s.copy(combatants=s.combatants+("part1" to if(effect=="break") part.copy(breakTurns=1)
                else part.copy(statusEffects=listOf(StatusEffect(effect,2)))))
            val after=AuthoredEncounterRules.reconcile(s,definitions,registry)
            assertTrue(effect,after.authored.preparations.isEmpty())
            assertTrue(after.log.filterIsInstance<CombatLogEntry.TurnSkipped>().single().reason.contains("interrupted"))
        }
    }
    @Test fun losingTheDeclaredTargetCancelsRatherThanRetargetingSomeoneElse() {
        var s=prepared(state());s=s.copy(combatants=s.combatants+("nova" to s.combatants.getValue("nova").copy(hp=0)))
        assertTrue(AuthoredEncounterRules.reconcile(s,definitions,registry).authored.preparations.isEmpty())
    }
    @Test fun phaseIsCommittedOnceAndDropsTheDeclaredDefenses() {
        val boss=enemy("core",AuthoredEnemyPattern(phase=AuthoredEnemyPhase(.4,"Overclock","clock",listOf("shot"),listOf("guard","shield"))))
        val defs=mapOf("core" to boss);var s=state(defs);val actor=s.combatants.getValue("core")
        s=s.copy(combatants=s.combatants+("core" to actor.copy(hp=60,statusEffects=listOf(StatusEffect("guard",2),StatusEffect("shield",2),StatusEffect("jammed",2)))))
        val after=AuthoredEncounterRules.reconcile(s,defs,registry)
        assertEquals("Overclock",after.authored.phases["core"])
        assertEquals(setOf("jammed"),after.combatants.getValue("core").statusEffects.map { it.id }.toSet())
        assertEquals(after,AuthoredEncounterRules.reconcile(after,defs,registry))
    }
    @Test fun reservesCountStartingEscortsAndDoNotRecycleDeadReserveSlots() {
        val boss=enemy("core",AuthoredEnemyPattern(reinforcements=AuthoredReinforcements("deploy",listOf("a","b","c"),2)))
        val defs=mapOf("core" to boss,"escort" to enemy("escort"),"reserve" to enemy("a"))
        var s=state(defs)
        assertNull(AuthoredEncounterRules.nextReserve("core",s,defs))
        s=s.copy(combatants=s.combatants+("reserve" to s.combatants.getValue("reserve").copy(hp=0)),
            authored=AuthoredEncounterState(reservesUsed=mapOf("core" to 2)))
        assertEquals("c",AuthoredEncounterRules.nextReserve("core",s,defs))
        s=s.copy(authored=s.authored.copy(reservesUsed=mapOf("core" to 3)))
        assertNull(AuthoredEncounterRules.nextReserve("core",s,defs))
    }
    @Test fun preparationCountersAndPhaseStateRoundTripWithoutAClockOrCoroutine() {
        val original=prepared(state()).authored.copy(reservesUsed=mapOf("core" to 2),
            reserveOwners=mapOf("a#reserve1" to "core"),phases=mapOf("core" to "Overclock"))
        val adapter=MoshiProvider.instance.adapter(AuthoredEncounterState::class.java)
        assertEquals(original,adapter.fromJson(adapter.toJson(original)))
    }
    @Test fun onlyOptedInPreparationsAreCutByAnOrdinaryDamagingHit() {
        val coil = enemy("core", AuthoredEnemyPattern(interruptOnDamage = setOf("coil")))
        val defs = mapOf("core" to coil)
        val before = state(defs).let { it.copy(authored = AuthoredEncounterState(preparations = mapOf(
            "core" to EnemyPreparation("core", "core", "coil", listOf("nova"), "Coiling")))) }
        val after = before.copy(combatants = before.combatants +
            ("core" to before.combatants.getValue("core").copy(hp = 199)))
        val cut = AuthoredEncounterRules.reconcile(after, defs, registry, before)
        assertTrue(cut.authored.preparations.isEmpty())
        assertTrue("core" in cut.authored.recovery)
        assertEquals(before, AuthoredEncounterRules.reconcile(before, defs, registry, before))
        val ordinary = mapOf("core" to enemy("core"))
        assertFalse(AuthoredEncounterRules.reconcile(after, ordinary, registry, before).authored.preparations.isEmpty())
    }

    @Test fun oldVictoriesMapOnlyWithinTheirOriginalWorldTwoRoom() {
        assertEquals(listOf("moss_borer", "moss_borer"), World2EncounterCompatibility.creditedIds(
            "sector9_stream_cave_depths", listOf("echo_borer", "echo_borer")))
        assertEquals(listOf("mist_glider"), World2EncounterCompatibility.creditedIds("sector9_stream_pools", listOf("siren_skimmer")))
        assertEquals(listOf("sentinel_orb", "spore_spitter"), World2EncounterCompatibility.creditedIds(
            "sector9_hall_acoustics_lab", listOf("spore_spitter")))
        assertEquals(listOf("echo_borer"), World2EncounterCompatibility.creditedIds("mine_alpha", listOf("echo_borer")))
        assertEquals(listOf("spore_spitter"), World2EncounterCompatibility.creditedIds("sector9_stream_wetlands", listOf("spore_spitter")))
    }

}
