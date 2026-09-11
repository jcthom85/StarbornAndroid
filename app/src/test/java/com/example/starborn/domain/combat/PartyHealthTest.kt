package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.GameSessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class PartyHealthTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val assets = WorldAssetDataSource(reader)
    private val items = reader.readList<Item>("items.json").associateBy { it.id }

    @Test fun `Nova resting ceiling includes combat vitality and armor health`() {
        val nova = assets.loadCharacters().single { it.id == "nova" }
        assertEquals(120, PartyHealth.maxHp(nova, GameSessionState(), items::get, assets.loadSkillNodes()))
        val armor = items.getValue("nova_helioguard_plate").equipment!!
        val vitality = armor.statMods.orEmpty().filterKeys { it in setOf("vit", "vitality") }.values.sum()
        val state = GameSessionState(equippedArmors = mapOf("nova" to "nova_helioguard_plate"))
        assertEquals(CombatFormulas.maxHp(nova.hp, nova.vitality + vitality) + (armor.hpBonus ?: 0),
            PartyHealth.maxHp(nova, state, items::get, assets.loadSkillNodes()))
    }
}
