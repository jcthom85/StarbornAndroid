package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.ItemAssetDataSource
import com.example.starborn.data.repository.ItemRepository
import com.example.starborn.domain.model.BuffEffect
import org.junit.Assert.*
import org.junit.Test

class CombatClarityTest {
    @Test fun combatLogDistinguishesBuffAmountsFromStatusStacks() {
        assertEquals("Accuracy +8% (4 turns for this character)", CombatClarity.statusAppliedLabel(
            CombatLogEntry.StatusApplied(1, "nova", "buff_accuracy", 8, 4)))
        assertEquals("Weapon crit chance +30% (1 turn for this character)", CombatClarity.statusAppliedLabel(
            CombatLogEntry.StatusApplied(1, "nova", "buff_crit_rate", 30, 1)))
        assertEquals("Defense -6", CombatClarity.statusAppliedLabel(
            CombatLogEntry.StatusApplied(1, "nova", "buff_defense", -6)))
        assertEquals("Poison (3 stacks)", CombatClarity.statusAppliedLabel(
            CombatLogEntry.StatusApplied(1, "nova", "poison", 3), "Poison"))
        assertEquals("Weapon crit chance", CombatClarity.statusName("buff_crit_rate"))
    }

    private val items = ItemRepository(ItemAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))).apply { load() }

    @Test fun equippedAbilitiesExplainActualEffectsCostScopeAndRemainingCooldown() {
        val comet = CombatClarity.snackPreview(items.findItem("comet_gummies"), 3)
        assertTrue(comet.contains("Weapon crit chance +30%"))
        assertTrue(comet.contains("4 turns for this character"))
        assertTrue(comet.contains("Ready in 3 turns for this character"))
        assertTrue(comet.contains("Spends one action"))
        assertTrue(comet.contains("not consumed"))
        assertTrue(comet.contains("skills use their own crit rules"))
        val healing = CombatClarity.snackPreview(items.findItem("stellar_scone"), 0)
        assertTrue(healing.contains("Restores 35 HP"))
        assertTrue(healing.contains("Target: self"))
        assertFalse(healing.contains("Weapon crit"))
        val pulse = CombatClarity.snackPreview(items.findItem("pulse_citrus"), 0)
        assertTrue(pulse.contains("Speed +16"))
        assertTrue(pulse.contains("3 turns for this character"))
        assertTrue(CombatClarity.snackPreview(null, 0).contains("Equip a snack"))
    }

    @Test fun signedBuffAmountsPercentUnitsAndOwnerTurnsStayDistinct() {
        val buff = ActiveBuff(BuffEffect("accuracy", -12, 4), 2)
        assertTrue(CombatClarity.buffLabel(buff).contains("Accuracy -12%"))
        assertTrue(CombatClarity.buffLabel(buff).contains("2 turns for this character left"))
        assertTrue(CombatClarity.compactBuffLabel(buff).contains("(2t)"))
        val chip = com.example.starborn.feature.combat.ui.components.statusChipsFor(emptyList(), listOf(buff)).single()
        assertEquals(2, chip.turns)
        assertTrue(chip.percent)
        assertEquals(CombatClarity.buffLabel(buff), chip.description)
    }
}
