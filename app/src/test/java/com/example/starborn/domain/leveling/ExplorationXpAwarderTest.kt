package com.example.starborn.domain.leveling

import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.*
import org.junit.Test

class ExplorationXpAwarderTest {
    private val leveling = LevelingManager(LevelingData(mapOf("1" to 0, "2" to 100, "3" to 250)))
    private val progression = ProgressionData(mapOf("nova" to mapOf("2" to "nova_two", "3" to "nova_three")))

    @Test fun `quest XP immediately crosses multiple levels and unlocks skills`() {
        val store = GameSessionStore().apply { restore(GameSessionState(playerId = "nova",
            partyMembers = listOf("nova", "zeke"), partyMemberXp = mapOf("zeke" to 30))) }
        ExplorationXpAwarder(store, leveling, progression).award(250)
        val state = store.state.value
        assertEquals(250, state.playerXp)
        assertEquals(250, state.partyMemberXp["nova"])
        assertEquals(3, state.playerLevel)
        assertEquals(3, state.partyMemberLevels["nova"])
        assertEquals(setOf("nova_two", "nova_three"), state.unlockedSkills)
        assertEquals(280, state.partyMemberXp["zeke"])
        assertEquals(3, state.partyMemberLevels["zeke"])
    }

    @Test fun `subthreshold and nonpositive awards do not grant premature unlocks`() {
        val store = GameSessionStore().apply { restore(GameSessionState(playerId = "nova")) }
        val awarder = ExplorationXpAwarder(store, leveling, progression)
        awarder.award(99)
        assertEquals(1, store.state.value.playerLevel)
        assertTrue(store.state.value.unlockedSkills.isEmpty())
        val before = store.state.value
        awarder.award(0)
        awarder.award(-10)
        assertEquals(before, store.state.value)
        awarder.award(1)
        assertEquals(2, store.state.value.playerLevel)
        assertEquals(setOf("nova_two"), store.state.value.unlockedSkills)
    }

    @Test fun `loaded XP catches up without a battle and known skills are preserved`() {
        val store = GameSessionStore().apply { restore(GameSessionState(playerId = "nova",
            playerXp = 250, unlockedSkills = setOf("story_art", "nova_two"))) }
        ExplorationXpAwarder(store, leveling, progression).award(1)
        assertEquals(3, store.state.value.playerLevel)
        assertEquals(setOf("story_art", "nova_two", "nova_three"), store.state.value.unlockedSkills)
        val resumed = GameSessionStore().apply { restore(store.state.value) }
        ExplorationXpAwarder(resumed, leveling, progression).award(1)
        assertEquals(252, resumed.state.value.playerXp)
        assertEquals(store.state.value.unlockedSkills, resumed.state.value.unlockedSkills)
    }

    @Test fun `awarding XP never lowers an existing level`() {
        val store = GameSessionStore().apply { restore(GameSessionState(playerId = "nova", playerLevel = 3)) }
        ExplorationXpAwarder(store, leveling, progression).award(1)
        assertEquals(3, store.state.value.playerLevel)
    }

    @Test fun `missing companion XP is seeded once and absent members receive nothing`() {
        val store = GameSessionStore().apply { restore(GameSessionState(playerId = "nova", playerXp = 90,
            partyMembers = listOf("nova", "zeke", "zeke"), partyMemberXp = mapOf("orion" to 20))) }
        ExplorationXpAwarder(store, leveling, progression).award(10)
        assertEquals(100, store.state.value.partyMemberXp["nova"])
        assertEquals(100, store.state.value.partyMemberXp["zeke"])
        assertEquals(20, store.state.value.partyMemberXp["orion"])
    }

    @Test fun `recruited companion retains catchup and earns its own skills after resume`() {
        val store = GameSessionStore().apply { restore(GameSessionState(playerId = "nova", playerXp = 250,
            playerLevel = 3, partyMembers = listOf("nova"))) }
        store.addPartyMember("zeke")
        val resumed = GameSessionStore().apply { restore(store.state.value) }
        val mapping = ProgressionData(mapOf("zeke" to mapOf("2" to "zeke_two", "3" to "zeke_three")))
        ExplorationXpAwarder(resumed, leveling, mapping).award(10)
        assertEquals(260, resumed.state.value.partyMemberXp["zeke"])
        assertEquals(3, resumed.state.value.partyMemberLevels["zeke"])
        assertEquals(setOf("zeke_two", "zeke_three"), resumed.state.value.unlockedSkills)
    }
}
