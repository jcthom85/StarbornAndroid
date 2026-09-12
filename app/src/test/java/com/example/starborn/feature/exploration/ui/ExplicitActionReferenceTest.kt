package com.example.starborn.feature.exploration.ui

import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.actionKey
import com.example.starborn.feature.exploration.ui.hud.InlineActionTarget
import com.example.starborn.feature.exploration.viewmodel.ActionHintUi
import org.junit.Assert.*
import org.junit.Test

class ExplicitActionReferenceTest {
    @Test fun `shipped debug pilot resolves custom label to authored action`() {
        val reader = com.example.starborn.data.assets.AssetJsonReader(
            com.example.starborn.core.platform.DesktopAssetProvider(), com.example.starborn.core.MoshiProvider.instance)
        val room = reader.readList<com.example.starborn.domain.model.Room>("rooms.json")
            .single { it.id == "debug_enemy_party_sizes" }
        val target = GenericAction("debug manifest", "generic")
        val plan = requireNotNull(buildInlineActionPlan(room.description, listOf(target), emptyMap(), room))
        assertFalse(plan.description.contains("[action:"))
        val segment = plan.segments.single()
        assertEquals("layout manifest", plan.description.substring(segment.start, segment.end))
        assertEquals(InlineActionTarget.Room(target), segment.target)
    }
    private val action = GenericAction("crew datapad", "generic")

    @Test fun `explicit labels choose exact targets and preserve lock hints`() {
        val plan = requireNotNull(buildInlineActionPlan("Read [action:crew datapad|the battered tablet].",
            listOf(action), mapOf(action.actionKey() to ActionHintUi(locked = true, message = null)), null))
        assertEquals("Read the battered tablet.", plan.description)
        val segment = plan.segments.single()
        assertEquals("the battered tablet", plan.description.substring(segment.start, segment.end))
        assertEquals(InlineActionTarget.Room(action), segment.target)
        assertTrue(segment.locked)
    }

    @Test fun `hidden unknown and ambiguous references remain plain text without accidental fallback`() {
        for (actions in listOf(emptyList(), listOf(action, action.copy(actionEvent = "other")))) {
            val plan = requireNotNull(buildInlineActionPlan("[action:crew datapad|crew datapad]", actions, emptyMap(), null))
            assertEquals("crew datapad", plan.description)
            assertTrue(plan.segments.isEmpty())
        }
        val plan = requireNotNull(buildInlineActionPlan("[action:missing|crew datapad]", listOf(action), emptyMap(), null))
        assertTrue(plan.segments.isEmpty())
    }

    @Test fun `legacy matching NPC markers and repeated explicit labels coexist`() {
        val legacy = requireNotNull(buildInlineActionPlan("Read the crew datapad.", listOf(action), emptyMap(), null))
        assertEquals(1, legacy.segments.size)
        val plan = requireNotNull(buildInlineActionPlan(
            "[npc:Jed] offers [action:crew datapad|notes] and [action:crew datapad|more notes].",
            listOf(action), emptyMap(), null))
        assertEquals("Jed offers notes and more notes.", plan.description)
        assertEquals(3, plan.segments.size)
        assertEquals(3, plan.segments.map { it.id }.distinct().size)
        assertTrue(plan.segments.first().target is InlineActionTarget.Npc)
    }
}
