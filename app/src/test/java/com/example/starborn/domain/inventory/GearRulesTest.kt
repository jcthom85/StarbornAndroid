package com.example.starborn.domain.inventory

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.Item
import org.junit.Assert.*
import org.junit.Test

class GearRulesTest {
    private val items = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        .readList<Item>("items.json").associateBy { it.id }

    @Test fun `generic earned armor fits all party members but not other slots`() {
        val armor = items.getValue("heat_liner")
        for (owner in listOf("nova", "zeke", "orion", "gh0st", "ollie")) {
            assertTrue(GearRules.matchesSlot(armor.equipment, "armor", owner, armor.type))
            assertFalse(GearRules.matchesSlot(armor.equipment, "weapon", owner, armor.type))
            assertFalse(GearRules.matchesSlot(armor.equipment, "accessory", owner, armor.type))
        }
    }

    @Test fun `character armor remains restricted to its owner`() {
        val armor = items.getValue("nova_flux_liner")
        assertTrue(GearRules.matchesSlot(armor.equipment, "armor", "nova", armor.type))
        assertFalse(GearRules.matchesSlot(armor.equipment, "armor", "orion", armor.type))
        val boots = items.getValue("grav_boots")
        assertFalse(GearRules.matchesSlot(boots.equipment, "armor", "orion", boots.type))
    }
}
