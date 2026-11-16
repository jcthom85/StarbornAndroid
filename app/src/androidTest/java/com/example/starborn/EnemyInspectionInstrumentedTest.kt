package com.example.starborn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.combat.*
import com.example.starborn.domain.model.*
import com.example.starborn.feature.combat.ui.CombatImpactBanner
import com.example.starborn.feature.combat.ui.components.*
import com.example.starborn.feature.combat.viewmodel.CombatBannerAccent
import com.example.starborn.feature.combat.viewmodel.CombatBannerIcon
import com.example.starborn.feature.combat.viewmodel.CombatBannerMessage
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

    @Test fun twoRowSkillBannerIsNotClippedInsideFixedMessageSlot() {
        compose.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    CombatImpactBanner(
                        message = CombatBannerMessage(
                            id = "banner_1",
                            primary = "Gathering Silence",
                            secondary = "Recovery window — attack or heal",
                            accent = CombatBannerAccent.ENEMY,
                            icon = CombatBannerIcon.SKILL,
                            actorLabel = "Sovereign Vale",
                            isPlayerActor = false
                        ),
                        hasInstruction = false,
                        persistent = true,
                        highContrastMode = false,
                        theme = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("SOVEREIGN VALE").assertIsDisplayed()
        compose.onNodeWithText("Gathering Silence").assertIsDisplayed()
        compose.onNodeWithText("Recovery window — attack or heal").assertIsDisplayed()
        val secondaryBounds = compose.onNodeWithText("Recovery window — attack or heal").fetchSemanticsNode().boundsInRoot
        assertTrue("Secondary line height must not be crushed: $secondaryBounds", secondaryBounds.height >= 14f)
    }

}
