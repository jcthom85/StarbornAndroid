package com.example.starborn.domain.tutorial

import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.migrateOpeningNarrativeState
import org.junit.Assert.*
import org.junit.Test

class OpeningEquipmentGuideTest {
    @Test fun manualKitPolicyDoesNotChangeOtherRewardsOrRecruitedCharacters() {
        val receiving = GameSessionState(completedMilestones = setOf(OpeningEquipmentGuide.MANUAL_KIT))
        assertFalse(OpeningEquipmentGuide.shouldAutoEquip(receiving, "nova", "mining_pistol"))
        assertFalse(OpeningEquipmentGuide.shouldAutoEquip(receiving, "nova", "nova_flux_liner"))
        assertTrue(OpeningEquipmentGuide.shouldAutoEquip(receiving, "zeke", "zeke_shock_fists"))
        assertTrue(OpeningEquipmentGuide.shouldAutoEquip(receiving, "nova", "other_gun"))
        assertTrue(OpeningEquipmentGuide.shouldAutoEquip(GameSessionState(), "nova", "mining_pistol"))
        val later = receiving.copy(completedMilestones = receiving.completedMilestones + "ms_w1_mq01_jed_talked")
        assertTrue(OpeningEquipmentGuide.shouldAutoEquip(later, "nova", "mining_pistol"))
    }

    @Test fun legacyEquippedSaveKeepsItsLoadoutAndDoesNotNeedTheNewGuide() {
        val original = GameSessionState(
            completedMilestones = setOf("ms_w1_mq01_jed_talked"),
            unlockedWeapons = setOf("mining_pistol"), unlockedArmors = setOf("nova_flux_liner"),
            equippedWeapons = mapOf("nova" to "mining_pistol", "zeke" to "zeke_shock_fists"),
            equippedArmors = mapOf("nova" to "nova_flux_liner"))
        val restored = original.migrateOpeningNarrativeState()
        assertEquals(original.equippedWeapons, restored.equippedWeapons)
        assertEquals(original.equippedArmors, restored.equippedArmors)
        assertFalse(OpeningEquipmentGuide.needsGuidance(restored))
        assertFalse(OpeningEquipmentGuide.MANUAL_KIT in restored.completedMilestones)
    }
}
