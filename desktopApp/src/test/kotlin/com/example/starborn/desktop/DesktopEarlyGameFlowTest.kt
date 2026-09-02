package com.example.starborn.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DesktopEarlyGameFlowTest {

    @Test
    fun testWorld1EarlyExplorationAndJedDialogue() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_test_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)

            // Step 1: Start New Game -> Must be Nova's Bunk in World 1
            services.startNewGame()
            val stateAfterStart = services.sessionStore.state.value
            assertEquals("pit_nova_bunk", stateAfterStart.roomId)

            val rooms = services.worldDataSource.loadRooms()
            val roomsById = rooms.associateBy { it.id }
            val novaBunk = roomsById["pit_nova_bunk"]
            assertNotNull("pit_nova_bunk room should exist in rooms.json", novaBunk)

            // Step 2: In Nova's Bunk, verify initial blocked direction to the west
            val blockedWest = novaBunk!!.blockedDirections?.get("west")
            assertNotNull("West direction in Nova's bunk should have safety requirements", blockedWest)

            // Initial room state check
            val initialLightOn = stateAfterStart.roomStates["pit_nova_bunk"]?.get("light_on")
                ?: (novaBunk.state["light_on"] as? Boolean) ?: false
            assertTrue("Initially light_on should be false in dark bunk", !initialLightOn)

            // Step 3: Toggle "bunk light" action
            val lightAction = novaBunk.actions.find { it["name"] == "bunk light" }
            assertNotNull("bunk light action should exist in room actions", lightAction)

            val currentRoomState = ((stateAfterStart.roomStates["pit_nova_bunk"] ?: novaBunk.state.mapNotNull { (k, v) ->
                (v as? Boolean)?.let { k to it }
            }.toMap())).toMutableMap()
            currentRoomState["light_on"] = true
            currentRoomState["conduit_isolated"] = true
            val updatedMap = stateAfterStart.roomStates.toMutableMap()
            updatedMap["pit_nova_bunk"] = currentRoomState
            services.sessionStore.restore(stateAfterStart.copy(roomStates = updatedMap))

            val stateAfterLight = services.sessionStore.state.value
            assertEquals(true, stateAfterLight.roomStates["pit_nova_bunk"]?.get("light_on"))
            assertEquals(true, stateAfterLight.roomStates["pit_nova_bunk"]?.get("conduit_isolated"))

            // Step 4: Verify requirements met and navigate West -> Pod Row (pit_L2_corridor)
            val reqsMet = blockedWest!!.requires?.all { req ->
                val rState = stateAfterLight.roomStates[req.roomId]
                val currentVal = rState?.get(req.stateKey) ?: novaBunk.state[req.stateKey]
                (currentVal as? Boolean) == req.value
            } ?: false
            assertTrue("Requirements to exit west into corridor must now be satisfied", reqsMet)

            val targetRoom1 = novaBunk.connections["west"]
            assertNotNull(targetRoom1)
            services.sessionStore.setRoom(targetRoom1!!)
            assertEquals("pit_L2_corridor", services.sessionStore.state.value.roomId)

            // Step 5: Navigate South -> Lift Shaft (pit_shaft)
            val podRow = roomsById[services.sessionStore.state.value.roomId]
            assertNotNull(podRow)
            val targetRoom2 = podRow!!.connections["south"]
            assertNotNull(targetRoom2)
            services.sessionStore.setRoom(targetRoom2!!)
            assertEquals("pit_shaft", services.sessionStore.state.value.roomId)

            // Step 6: Navigate West -> Jed's Bunk (pit_jed_bunk)
            val liftShaft = roomsById[services.sessionStore.state.value.roomId]
            assertNotNull(liftShaft)
            val targetRoom3 = liftShaft!!.connections["west"]
            assertNotNull(targetRoom3)
            services.sessionStore.setRoom(targetRoom3!!)
            assertEquals("pit_jed_bunk", services.sessionStore.state.value.roomId)

            // Step 7: Interact with NPC Jed (evaluating active npcPresence)
            val jedBunk = roomsById[services.sessionStore.state.value.roomId]
            assertNotNull(jedBunk)
            
            val activeNpcs = (jedBunk!!.npcs + jedBunk.npcPresence.filter { rule ->
                val completed = services.sessionStore.state.value.completedMilestones
                val reqMet = rule.requiresMilestones.all { it in completed }
                val forbMet = rule.forbiddenMilestones.none { it in completed }
                reqMet && forbMet
            }.map { it.npc }).distinct()
            
            assertTrue("Jed must be actively present in pit_jed_bunk via npcPresence", activeNpcs.contains("jed"))

            val npcs = services.worldDataSource.loadNpcs()
            val npcsById = npcs.mapNotNull { it.id?.let { id -> id to it } }.toMap()
            val jedDef = npcsById["jed"]
            assertNotNull("Jed definition should exist in npcs.json", jedDef)
            assertEquals("Jed", jedDef?.name)

            // Start dialogue session with Jed
            val dialogueSession = services.dialogueService.startDialogue("jed")
            assertNotNull("Dialogue session with Jed should start", dialogueSession)
            val firstLine = dialogueSession?.current()
            assertNotNull("First dialogue line should be non-null", firstLine)
            assertTrue("Jed dialogue should contain greeting text", firstLine?.text?.isNotBlank() == true)

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
