package com.example.starborn.domain.session

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.model.Item
import org.junit.Assert.*
import org.junit.Test

class RetiredCompanionMigrationTest {
    @Test fun oldPartyAndCheckpointRetireOllieWithoutLosingItemsOrStory() {
        val old = GameSessionState(playerId = "nova", partyMembers = listOf("nova", "Ollie", "zeke"),
            partyMemberLevels = mapOf("nova" to 9, "Ollie" to 8),
            partyMemberXp = mapOf("Ollie" to 25), partyMemberHp = mapOf("Ollie" to 40),
            inventory = mapOf("basic_slingshot" to 1), equippedWeapons = mapOf("ollie" to "basic_slingshot"),
            equippedArmors = mapOf("ollie" to "basic_vest"), equippedItems = mapOf("ollie:accessory" to "charm"),
            completedMilestones = setOf("ms_ollie_met", "ms_ollie_recruited"))
        val store = GameSessionStore()
        store.restore(old.copy(battleCheckpoint = old))
        val state = store.state.value
        assertEquals(listOf("nova", "zeke"), state.partyMembers)
        assertEquals(mapOf("nova" to 9), state.partyMemberLevels)
        assertTrue(state.partyMemberHp.isEmpty())
        assertTrue(state.partyMemberXp.isEmpty())
        assertTrue(state.equippedWeapons.isEmpty())
        assertTrue(state.equippedArmors.isEmpty())
        assertTrue(state.equippedItems.isEmpty())
        assertEquals(old.inventory, state.inventory)
        assertEquals(old.completedMilestones, state.completedMilestones)
        assertTrue("basic_slingshot" in state.unlockedWeapons)
        assertTrue("basic_vest" in state.unlockedArmors)
        assertEquals(listOf("nova", "zeke"), state.battleCheckpoint?.partyMembers)
        store.restore(state)
        assertEquals(state, store.state.value)
    }

    @Test fun oldOllieLeaderFallsBackToNovaAndCannotBeRecruitedAgain() {
        val store = GameSessionStore()
        store.restore(GameSessionState(playerId = "ollie", partyMembers = listOf("ollie"), playerLevel = 8))
        assertEquals("nova", store.state.value.playerId)
        assertEquals(listOf("nova"), store.state.value.partyMembers)
        assertEquals(1, store.state.value.playerLevel)
        store.setPartyMembers(listOf("nova", "ollie", "zeke"))
        store.setPlayer("ollie")
        store.setEquippedWeapon("ollie", "basic_slingshot")
        store.setEquippedArmor("ollie", "basic_vest")
        assertEquals(listOf("nova", "zeke"), store.state.value.partyMembers)
        assertEquals("nova", store.state.value.playerId)
        assertTrue(store.state.value.equippedWeapons.isEmpty())
        assertTrue(store.state.value.equippedArmors.isEmpty())
    }

    @Test fun authoredPlayableRosterAndEquipmentExcludeOllie() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        assertEquals(setOf("nova", "zeke", "orion", "gh0st"),
            WorldAssetDataSource(reader).loadCharacters().map { it.id }.toSet())
        val vest = reader.readList<Item>("items.json").first { it.id == "basic_vest" }
        assertFalse(GearRules.matchesSlot(vest.equipment, "armor", "ollie", vest.type))
        assertNull(GearRules.allowedWeaponTypeFor("ollie"))
    }
}
