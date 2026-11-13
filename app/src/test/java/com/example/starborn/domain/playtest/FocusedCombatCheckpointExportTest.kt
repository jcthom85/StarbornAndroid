package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.SessionSaveRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Produces an additive slot bundle from recorded earned arrivals, never a player's save directory. */
class FocusedCombatCheckpointExportTest {
    @Test fun earnedReviewArrivalsRoundTripInFourNewSlotsWithTheirFightsStillAvailable() = runBlocking {
        val assets=WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(),MoshiProvider.instance))
        val rooms=assets.loadRooms().associateBy { it.id }
        val output=File("build/reports/focused-combat-checkpoints/slots").apply { mkdirs() }
        val persistence=GameSessionPersistence(output)
        val manifest=mutableListOf<String>()
        val cases=listOf(
            Triple(11,"warden","launch_bay"),Triple(12,"beast","sector9_canopy_ridge"),
            Triple(13,"hound-pack","sector9_wilds_nest"),Triple(14,"plant-pair","sector9_canopy"))
        assertEquals((1..20).toList(),SessionSaveRepository.MANUAL_SLOTS.toList())
        for((slot,name,room) in cases) {
            val arrival=javaClass.getResourceAsStream("/playtest/focused-combat/$name.json")!!.bufferedReader().use {
                MoshiProvider.instance.adapter(GameSessionState::class.java).fromJson(it.readText())!!
            }
            assertEquals(room,arrival.roomId)
            assertTrue(arrival.pendingBattleJson.isBlank())
            assertNull(arrival.battleCheckpoint)
            assertTrue(arrival.purchasedSkillNodes.isEmpty())
            assertEquals(if(name=="warden") listOf("nova") else if(name=="beast") listOf("nova","zeke","orion") else listOf("nova","zeke"),arrival.partyMembers)
            val cleared=arrival.roomStates[room].orEmpty().filter { (key,value) -> key.startsWith("encounter_cleared:") && value==true }
            assertTrue("Encounter must be uncleared: $name",cleared.isEmpty())
            if(name=="warden") assertFalse("ms_w1_warden_defeated" in arrival.completedMilestones)
            else if(name=="beast") {
                assertTrue("ms_w2_stalker_confronted" in arrival.completedMilestones)
                assertFalse("defeat_the_beast" in arrival.questTasksCompleted["w2_mq04"].orEmpty())
            } else assertEquals(2,rooms.getValue(room).enemies.size)
            persistence.writeSlot(slot,arrival)
            assertEquals("Slot $slot must retain the exact earned loadout and progress",arrival,persistence.readSlot(slot))
            manifest+="Slot $slot: $name; $room; level ${arrival.playerLevel}; party=${arrival.partyMembers}; file=game_session_slot$slot.pb"
        }
        // This export bundle contains no legacy numbered, auto, quick, or live-session saves.
        assertTrue(output.listFiles().orEmpty().filter { it.name.endsWith(".pb") }.all {
            it.name in cases.map { (slot,_,_) -> "game_session_slot$slot.pb" }
        })
        File(output.parentFile,"manifest.txt").writeText(manifest.joinToString("\n")+"\n")
    }
}
