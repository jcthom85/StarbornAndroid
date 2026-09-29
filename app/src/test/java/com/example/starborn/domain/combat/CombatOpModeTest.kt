package com.example.starborn.domain.combat

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CombatOpModeTest {

    private val engine = CombatEngine()

    private val player = Combatant(
        id = "player",
        name = "Player",
        side = CombatSide.PLAYER,
        stats = StatBlock(
            maxHp = 100,
            strength = 20,
            vitality = 10,
            agility = 10,
            focus = 10,
            luck = 5,
            speed = 12,
            stability = 30
        )
    )

    private val ally = Combatant(
        id = "ally",
        name = "Ally",
        side = CombatSide.ALLY,
        stats = StatBlock(
            maxHp = 80,
            strength = 15,
            vitality = 8,
            agility = 12,
            focus = 10,
            luck = 5,
            speed = 10,
            stability = 25
        )
    )

    private val bossEnemy = Combatant(
        id = "boss",
        name = "Boss",
        side = CombatSide.ENEMY,
        stats = StatBlock(
            maxHp = 5000,
            strength = 50,
            vitality = 30,
            agility = 10,
            focus = 10,
            luck = 5,
            speed = 8,
            stability = 100
        )
    )

    @Before
    fun setUp() {
        CombatPlaytestBridge.reset()
    }

    @After
    fun tearDown() {
        CombatPlaytestBridge.reset()
    }

    private fun createBattleState(): CombatState {
        val setup = CombatSetup(
            playerParty = listOf(player, ally),
            enemyParty = listOf(bossEnemy)
        )
        return engine.beginEncounter(setup)
    }

    @Test
    fun `toggleOpMode flips state and fires flow`() {
        assertFalse(CombatPlaytestBridge.isOpMode)
        assertFalse(CombatPlaytestBridge.isOpModeFlow.value)

        val newState = CombatPlaytestBridge.toggleOpMode()
        assertTrue(newState)
        assertTrue(CombatPlaytestBridge.isOpMode)
        assertTrue(CombatPlaytestBridge.isOpModeFlow.value)

        val toggledOff = CombatPlaytestBridge.toggleOpMode()
        assertFalse(toggledOff)
        assertFalse(CombatPlaytestBridge.isOpMode)
    }

    @Test
    fun `when OP Mode is active player deals 9999 damage and breaks stability`() {
        CombatPlaytestBridge.isOpMode = true
        val initial = createBattleState()

        val afterDamage = engine.applyDamage(
            state = initial,
            attackerId = player.id,
            targetId = bossEnemy.id,
            amount = 15,
            element = "physical"
        )

        val bossState = afterDamage.combatants[bossEnemy.id]!!
        assertEquals(0, bossState.hp)
        assertTrue("Boss break turns should be triggered", bossState.breakTurns > 0)

        val lastLog = afterDamage.log.last() as CombatLogEntry.Damage
        assertEquals(9999, lastLog.amount)
        assertTrue(lastLog.critical)
    }

    @Test
    fun `when OP Mode is active player is invulnerable to enemy damage and stability damage`() {
        CombatPlaytestBridge.isOpMode = true
        val initial = createBattleState()

        val afterAttack = engine.applyDamage(
            state = initial,
            attackerId = bossEnemy.id,
            targetId = player.id,
            amount = 80,
            element = "physical"
        )

        val playerState = afterAttack.combatants[player.id]!!
        assertEquals(100, playerState.hp)
        assertEquals(30, playerState.stability)
        assertEquals(0, playerState.breakTurns)

        val lastLog = afterAttack.log.last() as CombatLogEntry.Damage
        assertEquals(0, lastLog.amount)
    }

    @Test
    fun `when OP Mode is active ally is also invulnerable to enemy damage`() {
        CombatPlaytestBridge.isOpMode = true
        val initial = createBattleState()

        val afterAttack = engine.applyDamage(
            state = initial,
            attackerId = bossEnemy.id,
            targetId = ally.id,
            amount = 999,
            element = "fire"
        )

        val allyState = afterAttack.combatants[ally.id]!!
        assertEquals(80, allyState.hp)
        assertEquals(25, allyState.stability)
    }

    @Test
    fun `when OP Mode is disabled normal damage math applies`() {
        CombatPlaytestBridge.isOpMode = false
        val initial = createBattleState()

        val afterPlayerAttack = engine.applyDamage(
            state = initial,
            attackerId = player.id,
            targetId = bossEnemy.id,
            amount = 25,
            element = "physical"
        )
        val bossState = afterPlayerAttack.combatants[bossEnemy.id]!!
        assertEquals(4975, bossState.hp)

        val afterEnemyAttack = engine.applyDamage(
            state = afterPlayerAttack,
            attackerId = bossEnemy.id,
            targetId = player.id,
            amount = 30,
            element = "physical"
        )
        val playerState = afterEnemyAttack.combatants[player.id]!!
        assertEquals(70, playerState.hp)
    }

    @Test
    fun `insta win instantly defeats all enemies and triggers victory outcome`() {
        val initial = createBattleState()
        val updatedCombatants = initial.combatants.mapValues { (_, combatantState) ->
            if (combatantState.combatant.side == CombatSide.ENEMY) {
                combatantState.copy(hp = 0, stability = 0)
            } else {
                combatantState
            }
        }
        val defeatedState = initial.copy(combatants = updatedCombatants)
        val victoryState = engine.resolveOutcome(defeatedState) {
            CombatReward(xp = 100, credits = 50)
        }

        assertNotNull(victoryState.outcome)
        assertTrue(victoryState.outcome is CombatOutcome.Victory)
        val victory = victoryState.outcome as CombatOutcome.Victory
        assertEquals(100, victory.rewards.xp)
        assertEquals(50, victory.rewards.credits)
    }
}
