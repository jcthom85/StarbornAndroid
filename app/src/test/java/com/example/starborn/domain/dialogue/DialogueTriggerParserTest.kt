package com.example.starborn.domain.dialogue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueTriggerParserTest {

    @Test
    fun parsesMultipleActions() {
        val actions = DialogueTriggerParser.parse(
            "start_quest:intro,recruit:ollie,set_milestone:ms_intro"
        )

        assertEquals(3, actions.size)
        assertEquals("start_quest", actions[0].type)
        assertEquals("intro", actions[0].startQuest)
        assertEquals("add_party_member", actions[1].type)
        assertEquals("ollie", actions[1].itemId)
        assertEquals("set_milestone", actions[2].type)
        assertEquals("ms_intro", actions[2].milestone)
    }

    @Test
    fun parsesItemQuantities() {
        val actions = DialogueTriggerParser.parse("give_item:widget*3,take_item:token|2")
        assertEquals(2, actions.size)
        val give = actions[0]
        assertEquals("give_item", give.type)
        assertEquals("widget", give.itemId)
        assertEquals(3, give.quantity)
        val take = actions[1]
        assertEquals("take_item", take.type)
        assertEquals("token", take.itemId)
        assertEquals(2, take.quantity)
    }

    @Test
    fun ignoresEmptyValues() {
        val actions = DialogueTriggerParser.parse("start_quest:,give_item:")
        assertTrue(actions.isEmpty())
    }
}
