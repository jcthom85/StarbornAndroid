package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import com.example.starborn.domain.combat.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class AuthoredEncounterDesktopTest {
    @get:Rule val compose=createComposeRule()
    @Test @OptIn(ExperimentalTestApi::class)
    fun compositeTargetsStayAccessibleAtPortraitAndWideSizes() {
        val dir=Files.createTempDirectory("authored-encounter-desktop").toFile()
        val s=DesktopAppServices(dir,RecordingAudioDriver())
        val ids=listOf("prototype_titan","prototype_missile","prototype_cutter","prototype_anchor","prototype_projector")
        var wide by mutableStateOf(false)
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(compose.runOnIdle { s.startDebugScenario("tut_npc_dialogue") })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            compose.runOnIdle { s.sessionStore.restore(s.sessionStore.state.value.copy(
                worldId="world_4",hubId="hub_8_assembly_line",roomId="foundry_titan_dock",
                partyMembers=listOf("nova","zeke","orion","gh0st"),
                activeQuests=emptySet(),completedQuests=emptySet(),completedEvents=emptySet(),
                completedMilestones=emptySet(),questTasksCompleted=emptyMap())) }
            compose.setContent { DesktopStarbornTheme(s) {
                CompositionLocalProvider(LocalDesktopForeground provides false) {
                    Box(Modifier.size(if(wide) 1024.dp else 390.dp,768.dp)) {
                        DesktopCombatScreen(s,ids,{},{},{})
                    }
                }
            } }
            val output=File("build/reports/authored-encounters").apply { mkdirs() }
            for(isWide in listOf(false,true)) {
                compose.runOnIdle { wide=isWide };compose.waitForIdle()
                ImageIO.write(compose.onRoot().captureToImage().asAwtImage(),"png",File(output,if(isWide) "titan-wide.png" else "titan-portrait.png"))
                File(output,"bounds-$isWide.txt").writeText(ids.joinToString("\n") { id ->
                    "$id: ${compose.onNodeWithTag("combat-linked-target-$id").fetchSemanticsNode().boundsInRoot}" })
                ids.forEach { compose.onNodeWithTag("combat-linked-target-$it").assertIsDisplayed() }
                val labels=ids.map { compose.onNodeWithTag("combat-linked-target-$it").fetchSemanticsNode().boundsInRoot }
                assertTrue("Target bounds: $labels", labels.all { it.width>=60 && it.height>=32 })
                Thread.sleep(300)
                ImageIO.write(compose.onRoot().captureToImage().asAwtImage(),"png",File(output,if(isWide) "titan-wide.png" else "titan-portrait.png"))
            }
        } finally { compose.runOnIdle { s.close() };dir.deleteRecursively() }
    }
    @Test fun warningAndTargetReticleKeepDesktopFormationBoundsFixed() {
        val dir=Files.createTempDirectory("threat-layout").toFile()
        val services=DesktopAppServices(dir,RecordingAudioDriver())
        val stats=StatBlock(200,8,4,4,4,1,5)
        val actors=listOf(Combatant("the_beast","The Source Beast",CombatSide.ENEMY,stats),
            Combatant("nova","Nova",CombatSide.PLAYER,stats),Combatant("zeke","Zeke",CombatSide.PLAYER,stats))
        val base=CombatState(emptyList(),0,actors.associate { it.id to CombatantState(it,200,100) })
        var state by mutableStateOf(base)
        var wide by mutableStateOf(false)
        val preparation=EnemyPreparation("the_beast","the_beast","beast_hunt_claw",listOf("nova"),"Prey marked — hunting lunge")
        var focused=""
        try {
            compose.setContent { DesktopStarbornTheme(services) {
                androidx.compose.foundation.layout.Column(Modifier.size(if(wide) 1024.dp else 390.dp,768.dp)) {
                    DesktopBattleFormation(state.combatants.values.filter { it.combatant.side==CombatSide.ENEMY },services,
                        Modifier.fillMaxWidth().weight(.6f),true,sprite={ "images/enemies/the_beast_combat.png" },
                        selectedId=null,validTargets=emptySet(),targeting=false,meters=emptyMap(),intents=emptyMap(),
                        feedback=emptyMap(),cues=emptyList(),lungeStyle=com.example.starborn.feature.combat.viewmodel.AttackLungeStyle.MELEE,
                        lungeActor=null,lungeToken=0,missActor=null,missToken=0,onSelect={})
                    Box(Modifier.fillMaxWidth().height(24.dp),contentAlignment=Alignment.Center) {
                        if(com.example.starborn.ui.combat.hasAuthoredPreparation(state))
                            com.example.starborn.ui.combat.AuthoredEncounterPanel(state,emptyList(),{focused=it},compact=true,skillName={"Hunting lunge"})
                        else com.example.starborn.ui.combat.AuthoredEncounterStatus(state,emptyList())
                    }
                    DesktopBattleFormation(state.combatants.values.filter { it.combatant.side==CombatSide.PLAYER },services,
                        Modifier.fillMaxWidth().weight(.4f),false,sprite={ "images/characters/emotes/${it}_cool.png" },
                        selectedId=null,validTargets=emptySet(),targeting=false,meters=emptyMap(),intents=emptyMap(),
                        feedback=emptyMap(),cues=emptyList(),lungeStyle=com.example.starborn.feature.combat.viewmodel.AttackLungeStyle.MELEE,
                        lungeActor=null,lungeToken=0,missActor=null,missToken=0,authoredState=state,onSelect={})
                }
            } }
            for(isWide in listOf(false,true)) {
                compose.runOnIdle { wide=isWide;state=base };compose.waitForIdle()
                val tags=listOf("combat-sprite-the_beast","combat-figure-nova","combat-figure-zeke")
                val bounds=tags.associateWith { compose.onNodeWithTag(it, useUnmergedTree=true).fetchSemanticsNode().boundsInRoot }
                for(authored in listOf(base.authored.copy(preparations=mapOf("the_beast" to preparation)),
                    base.authored.copy(phases=mapOf("the_beast" to "Frenzy — two strikes, then overextension")),base.authored)) {
                    compose.runOnIdle { state=base.copy(authored=authored) };compose.waitForIdle()
                    tags.forEach { assertEquals("$it must stay fixed, wide=$isWide",bounds.getValue(it),compose.onNodeWithTag(it, useUnmergedTree=true).fetchSemanticsNode().boundsInRoot) }
                    if(authored.preparations.isNotEmpty()) {
                        compose.onNodeWithTag("combat-threat-target-nova", useUnmergedTree=true).assertExists()
                        compose.onNodeWithTag("combat-threat-target-zeke", useUnmergedTree=true).assertDoesNotExist()
                        compose.onNodeWithTag("combat-preparation-the_beast").performClick()
                        assertEquals("the_beast",focused)
                    } else compose.onNodeWithTag("combat-threat-target-nova", useUnmergedTree=true).assertDoesNotExist()
                }
            }
        } finally { compose.runOnIdle { services.close() };dir.deleteRecursively() }
    }

}
