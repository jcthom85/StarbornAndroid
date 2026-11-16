package com.example.starborn.domain.narrative

import com.example.starborn.datastore.GameSessionProto
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.model.EventAction
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.fingerprint
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RestBanterTest {
    private val crew = listOf("nova", "zeke", "orion", "gh0st")

    @Test fun locationCopyAndEveryEligibleCompanionGetATurnBeforeRepeating() {
        for (room in listOf("sector9_stream_falls", "spire_skypark_scale_02", "foundry_forge_control_alcove",
            "orbital_customs_lounge", "deep_anchor_chamber_scale_04", "source_campfire", "astra_common_room")) {
            val store = GameSessionStore().apply { restore(GameSessionState(playerId = "nova", partyMembers = crew)) }
            val eligible = RestBanter.eligible(store.state.value, room, "Local shelter.")
            val heard = List(eligible.size) { store.takeRestBanter(room, "Local shelter."); store.state.value.lastRestBanterId }
            assertEquals(room, eligible.map { it.id }.toSet(), heard.toSet())
            assertEquals("Local copy gets the first turn", "place:$room", heard.first())
            store.takeRestBanter(room, "Local shelter.")
            assertNotEquals("No back-to-back repeat at cycle boundary", heard.last(), store.state.value.lastRestBanterId)
        }
    }

    @Test fun recruitmentAndStoryGatesDoNotLeakAbsentSpeakersOrFutureRevelations() {
        val solo = GameSessionState(playerId = "nova", partyMembers = listOf("nova"))
        val source = RestBanter.eligible(solo, "source_campfire", "A quiet fire.")
        assertEquals(listOf("place:source_campfire"), source.map { it.id })
        val store = GameSessionStore().apply { restore(solo) }
        store.takeRestBanter("source_campfire", "A quiet fire.")
        store.addPartyMember("orion")
        assertTrue(store.takeRestBanter("source_campfire", "A quiet fire.").contains("Orion"))
        store.setPartyMembers(listOf("nova"))
        assertEquals("A quiet fire.", store.takeRestBanter("source_campfire", "A quiet fire."))

        val full = solo.copy(partyMembers = crew)
        fun ids(room: String, marks: Set<String>) = RestBanter.eligible(full.copy(completedMilestones = marks), room, null).map { it.id }
        assertFalse("w5:names" in ids("deep_anchor_chamber_scale_04", emptySet()))
        assertTrue("w5:names" in ids("deep_anchor_chamber_scale_04", setOf("ms_w5_thorne_found")))
        assertFalse("w5:carpet" in ids("orbital_customs_lounge", setOf("ms_w5_thorne_found")))
        assertFalse("w4:seat" in ids("foundry_forge_control_alcove", emptySet()))
        assertTrue("w4:seat" in ids("foundry_forge_control_alcove", setOf("ms_w4_mq17_complete")))
        assertFalse("w4:rail" in ids("foundry_forge_control_alcove", setOf("ms_w4_phantom_records_found")))
        assertFalse("w3:reflection" in ids("spire_safehouse", setOf("ms_w3_mq14_complete")))
        assertFalse("w2:boot_repair" in ids("sector9_stream_falls", emptySet()))
        assertTrue("w2:boot_repair" in ids("sector9_stream_falls", setOf("ms_w2_mq04_complete")))
    }

    @Test fun astraAndDeepRingDoNotFallBackToMineDialogueAndArcadesRequireTheirParticipants() {
        val base = GameSessionState(playerId = "nova", partyMembers = crew)
        assertTrue(RestBanter.eligible(base, "deep_anchor_chamber_scale_04", null).all { it.id.startsWith("w5:") })
        assertTrue(RestBanter.eligible(base, "astra_common_room", null).all { it.id.startsWith("astra:") })
        val missingGhost = base.copy(partyMembers = listOf("nova", "zeke"), completedMilestones = setOf("ms_all_arcade_cabinets_restored"))
        assertFalse(RestBanter.eligible(missingGhost, "astra_common_room", null).any { it.id == "astra:arcade_all" })
        assertTrue(RestBanter.eligible(missingGhost.copy(partyMembers = crew), "astra_common_room", null).any { it.id == "astra:arcade_all" })
    }

    @Test fun restEventLocationMessageDoesNotBypassTheRotationOrBecomeASecondPopup() {
        val store = GameSessionStore().apply { restore(GameSessionState(playerId = "nova", partyMembers = crew)) }
        val messages = mutableListOf<String>()
        var popups = 0
        val manager = EventManager(emptyList(), store, eventHooks = EventHooks(
            onRestParty = { messages += store.takeRestBanter("sector9_stream_falls", it) },
            onMessage = { popups++ }
        ))
        val actions = listOf(EventAction(type = "rest_party"), EventAction(type = "show_message", message = "Shelter behind the falls."))
        repeat(3) { manager.performActions(actions) }
        assertEquals("Shelter behind the falls.", messages.first())
        assertEquals(3, messages.distinct().size)
        assertEquals(0, popups)
        assertTrue(store.state.value.completedMilestones.isEmpty())
        assertTrue(store.state.value.completedEvents.isEmpty())
    }

    @Test fun saveReloadContinuesTheCycleAndOldSavesStartWithAnEmptyHistory() = runBlocking {
        val directory = Files.createTempDirectory("rest-banter-test").toFile()
        try {
            val persistence = GameSessionPersistence(directory)
            // The old wire format has neither new field; defaults must remain safe.
            directory.resolve("game_session_slot1.pb").writeBytes(GameSessionProto.newBuilder()
                .setPlayerId("nova").addAllPartyMembers(crew).build().toByteArray())
            val old = requireNotNull(persistence.readSlot(1))
            assertTrue(old.heardRestBanter.isEmpty())
            assertEquals("", old.lastRestBanterId)
            val store = GameSessionStore().apply { restore(old) }
            val before = store.state.value.fingerprint()
            store.takeRestBanter("sector9_stream_falls", "Shelter.")
            assertNotEquals(before, store.state.value.fingerprint())
            persistence.writeSlot(2, store.state.value)
            val resumed = GameSessionStore().apply { restore(requireNotNull(persistence.readSlot(2))) }
            assertEquals(store.state.value, resumed.state.value)
            assertEquals(store.takeRestBanter("sector9_stream_falls", "Shelter."), resumed.takeRestBanter("sector9_stream_falls", "Shelter."))
            assertEquals(store.state.value.heardRestBanter, resumed.state.value.heardRestBanter)
        } finally { directory.deleteRecursively() }
    }

    @Test fun worldOneAuthoredRestAndNewlyUnlockedLinesRemainAvailable() {
        val state = GameSessionState(playerId = "nova", partyMembers = crew)
        val store = GameSessionStore().apply { restore(state) }
        repeat(3) { assertEquals("The shift alarm wakes Nova.", store.takeRestBanter("pit_nova_bunk", "The shift alarm wakes Nova.")) }
        val room = "deep_anchor_chamber_scale_04"
        repeat(RestBanter.eligible(store.state.value, room, null).size) { store.takeRestBanter(room, null) }
        store.restore(store.state.value.copy(completedMilestones = setOf("ms_w5_thorne_found")))
        store.takeRestBanter(room, null)
        assertEquals("w5:names", store.state.value.lastRestBanterId)
    }
}
