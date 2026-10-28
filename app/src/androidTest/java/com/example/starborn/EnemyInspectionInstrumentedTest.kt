package com.example.starborn

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.combat.*
import com.example.starborn.domain.model.*
import com.example.starborn.feature.combat.ui.components.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EnemyInspectionInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val enemy = Enemy("drone","Drone",hp=100,strength=1,vitality=1,agility=1,focus=1,luck=1,speed=1,
        resistances=Resistances(shock=-100),xpReward=0,creditReward=0,drops=emptyList())
    private val state = CombatantState(Combatant("drone","Drone",CombatSide.ENEMY,
        StatBlock(100,1,1,1,1,1,1)),80,40)
    @Test fun overviewDoesNotRevealAuthoredWeaknessAndBodyIsNotDismissal() {
        var inspected=0;var closed=0
        compose.setContent {MaterialTheme {EnemyOverview(enemy,state,emptySet(),true,true,{inspected++},{closed++})}}
        compose.onNodeWithText("Weaknesses: unknown").assertIsDisplayed()
        compose.onAllNodesWithText("Shock",substring=true).assertCountEquals(0)
        compose.onNodeWithText("Drone").performTouchInput {click()}
        compose.runOnIdle {assertEquals(0,closed)}
        compose.onNodeWithText("Inspect").performClick()
        compose.runOnIdle {assertEquals(1,inspected)}
        compose.onNodeWithText("Close overview").performClick()
        compose.runOnIdle {assertEquals(1,closed)}
    }
    @Test fun sheetShowsOnlyLearnedWeaknessesAndExplicitClose() {
        var closed=0
        compose.setContent {MaterialTheme {EnemyInspectionSheet(enemy,state,setOf("burn"),{closed++})}}
        compose.onNodeWithText("Burn").assertIsDisplayed()
        compose.onNodeWithText("HP", substring = false).assertDoesNotExist()
        compose.onNodeWithText("STABILITY", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Combat paused").assertDoesNotExist()
        compose.onNodeWithText("Deplete Stability", substring = true).assertDoesNotExist()
        compose.onAllNodesWithText("Shock",substring=true).assertCountEquals(0)
        compose.onNodeWithText("Drone").performTouchInput {click()}
        compose.runOnIdle {assertEquals(0,closed)}
        compose.onNodeWithText("Close").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(1,closed)}
    }
    @Test fun inspectionLeavesBattlefieldTargetsTappableAndKeepsArrowSelection() {
        var targetTaps = 0
        var next = 0
        compose.setContent { MaterialTheme {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                androidx.compose.material3.Button(onClick = { targetTaps++ }) { androidx.compose.material3.Text("Battlefield enemy") }
                EnemyInspectionSheet(enemy, state, emptySet(), {}, enemyCount = 2,
                    onPreviousEnemy = {}, onNextEnemy = { next++ })
            }
        } }
        compose.onNodeWithText("Battlefield enemy").performClick()
        compose.runOnIdle { assertEquals(1, targetTaps) }
        compose.onNodeWithContentDescription("Next enemy").performClick()
        compose.runOnIdle { assertEquals(1, next) }
    }

}
