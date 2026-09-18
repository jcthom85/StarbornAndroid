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
    fun parsesCreditsAsRewardAction() {
        val actions = DialogueTriggerParser.parse("give_credits:75,give_xp:40")

        assertEquals(2, actions.size)
        assertEquals("give_reward", actions[0].type)
        assertEquals(75, actions[0].credits)
        assertEquals("give_xp", actions[1].type)
        assertEquals(40, actions[1].xp)
    }

    @Test
    fun ignoresEmptyValues() {
        val actions = DialogueTriggerParser.parse("start_quest:,give_item:")
        assertTrue(actions.isEmpty())
    }

    @Test
    fun parsesMusicAndPersistentMusicActions() {
        val actions = DialogueTriggerParser.parse("music:gf_06_refuge,music_persist:gf_01_unpayable_debt")
        assertEquals(2, actions.size)

        val transient = actions[0]
        assertEquals("audio_layer", transient.type)
        assertEquals("music", transient.audioLayer)
        assertEquals("gf_06_refuge", transient.audioCueId)
        assertEquals(true, transient.audioLoop)
        assertEquals("transient", transient.context)

        val persist = actions[1]
        assertEquals("audio_layer", persist.type)
        assertEquals("music", persist.audioLayer)
        assertEquals("gf_01_unpayable_debt", persist.audioCueId)
        assertEquals(true, persist.audioLoop)
        assertEquals("persist", persist.context)
    }

    @Test
    fun parsesSilenceAndRestoreActions() {
        val actions = DialogueTriggerParser.parse("music_stop:400,music_silence_persist,music_restore,sting:sfx_warden_entry")
        assertEquals(4, actions.size)

        val stop = actions[0]
        assertEquals("audio_layer", stop.type)
        assertEquals(true, stop.audioStop)
        assertEquals(400L, stop.audioFadeMs)
        assertEquals("transient", stop.context)

        val silencePersist = actions[1]
        assertEquals("audio_layer", silencePersist.type)
        assertEquals(true, silencePersist.audioStop)
        assertEquals("persist", silencePersist.context)

        val restore = actions[2]
        assertEquals("audio_layer", restore.type)
        assertEquals("restore", restore.context)

        val sting = actions[3]
        assertEquals("audio_layer", sting.type)
        assertEquals("battle", sting.audioLayer)
        assertEquals("sfx_warden_entry", sting.audioCueId)
        assertEquals(false, sting.audioLoop)
    }
}
