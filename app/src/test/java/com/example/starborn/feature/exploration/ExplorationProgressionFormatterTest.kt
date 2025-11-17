package com.example.starborn.feature.exploration

import com.example.starborn.domain.leveling.LevelingData
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.feature.exploration.viewmodel.buildPartyStatusUi
import com.example.starborn.feature.exploration.viewmodel.buildProgressionSummaryUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExplorationProgressionFormatterTest {

    private val levelingManager = LevelingManager(
        LevelingData(
            levelCurve = mapOf(
                "1" to 0,
                "2" to 100,
                "3" to 240
            )
        )
    )

    private val player = Player(
        id = "nova",
        name = "Nova",
        level = 1,
        xp = 0,
        hp = 120,
        strength = 10,
        vitality = 8,
        agility = 6,
        focus = 30,
        luck = 5,
        skills = emptyList(),
        miniIconPath = "images/portraits/nova.png"
    )

    @Test
    fun partyStatusReflectsStoredVitals() {
        val session = GameSessionState(
            playerId = "nova",
            playerLevel = 2,
            playerXp = 150,
            partyMembers = listOf("nova"),
            partyMemberLevels = mapOf("nova" to 2),
            partyMemberXp = mapOf("nova" to 150),
            partyMemberHp = mapOf("nova" to 72),
            partyMemberRp = mapOf("nova" to 18)
        )
        val status = buildPartyStatusUi(
            sessionState = session,
            charactersById = mapOf("nova" to player),
            levelingManager = levelingManager,
            skillsById = emptyMap()
        )
        assertEquals(1, status.members.size)
        assertEquals("72 / 120 HP", status.members.first().hpLabel)
        assertEquals("18 / 30 Focus", status.members.first().rpLabel)
    }

    @Test
    fun progressionSummaryIncludesHpAndRpLabels() {
        val session = GameSessionState(
            playerId = "nova",
            playerLevel = 2,
            playerXp = 120,
            partyMembers = listOf("nova"),
            partyMemberLevels = mapOf("nova" to 2),
            partyMemberXp = mapOf("nova" to 120),
            partyMemberHp = mapOf("nova" to 80),
            partyMemberRp = mapOf("nova" to 20),
            playerAp = 4,
            playerCredits = 250
        )
        val summary = buildProgressionSummaryUi(
            sessionState = session,
            charactersById = mapOf("nova" to player),
            levelingManager = levelingManager
        )
        assertNotNull(summary.hpLabel)
        assertEquals("80 / 120 HP", summary.hpLabel)
        assertEquals("20 / 30 Focus", summary.rpLabel)
    }
}
