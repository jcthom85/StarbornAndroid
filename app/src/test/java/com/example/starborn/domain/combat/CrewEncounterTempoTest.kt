package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

class CrewEncounterTempoTest {
    private val enemies = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)).loadEnemies().associateBy { it.id }
    private val tuning = CrewEncounterTempo()

    @Test fun prototypeEncounterChangesOnlyBaseSpeedAndKeepsDistinctEnemyRoles() {
        val cases = listOf(Triple("orbital_fighter", "orbital_executive_dock", 34),
            Triple("source_shadow", "source_memory_fragments", 31),
            Triple("distorted_sentinel", "source_memory_fragments", 31),
            Triple("glitch_hound", "source_memory_fragments", 42))
        cases.forEach { (id, room, expected) ->
            val original = enemies.getValue(id)
            val changed = tuning.apply(original, room, 1)
            assertEquals(expected, CombatFormulas.speed(changed.speed, changed.agility).roundToInt())
            assertEquals(original, changed.copy(speed = original.speed))
        }
    }

    @Test fun ordinaryCampaignsOtherRoomsAndUnrelatedEnemiesKeepAuthoredStats() {
        enemies.values.forEach { enemy ->
            assertSame(enemy, tuning.apply(enemy, "orbital_executive_dock", 0))
            assertSame(enemy, tuning.apply(enemy, "source_memory_fragments", 0))
            assertSame(enemy, tuning.apply(enemy, "another_room", 1))
        }
        assertSame(enemies.getValue("compliance_officer"), tuning.apply(enemies.getValue("compliance_officer"), "orbital_executive_dock", 1))
        assertSame(enemies.getValue("source_shadow"), tuning.apply(enemies.getValue("source_shadow"), "deep_tear_scale_02", 1))
    }
}
