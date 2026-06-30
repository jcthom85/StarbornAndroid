package com.example.starborn.feature.exploration.ui

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueVoiceProfileTest {

    @Test
    fun corePartySpeakersUseCharacterMurmurProfiles() {
        assertEquals(DialogueVoiceProfile.NOVA, DialogueVoiceProfile.forSpeaker("Nova"))
        assertEquals(DialogueVoiceProfile.NOVA, DialogueVoiceProfile.forSpeaker("Player"))
        assertEquals(DialogueVoiceProfile.ORION, DialogueVoiceProfile.forSpeaker("Orion"))
        assertEquals(DialogueVoiceProfile.ZEKE, DialogueVoiceProfile.forSpeaker("Zeke"))
        assertEquals(DialogueVoiceProfile.GH0ST, DialogueVoiceProfile.forSpeaker("Gh0st"))
        assertEquals(DialogueVoiceProfile.GH0ST, DialogueVoiceProfile.forSpeaker("Ghost"))
    }

    @Test
    fun characterMurmursKeepStableCuePrefixes() {
        assertTrue(DialogueVoiceProfile.NOVA.randomCue(Random(1)).startsWith("voice_murmur_nova_"))
        assertTrue(DialogueVoiceProfile.ORION.randomCue(Random(1)).startsWith("voice_murmur_orion_"))
        assertTrue(DialogueVoiceProfile.ZEKE.randomCue(Random(1)).startsWith("voice_murmur_zeke_"))
        assertTrue(DialogueVoiceProfile.GH0ST.randomCue(Random(1)).startsWith("voice_murmur_gh0st_"))
    }

    @Test
    fun nonCoreSpeakersStillUseFallbackProfiles() {
        assertEquals(DialogueVoiceProfile.FEMALE, DialogueVoiceProfile.forSpeaker("Maddie"))
        assertEquals(DialogueVoiceProfile.MALE, DialogueVoiceProfile.forSpeaker("Foreman Boggs"))
        assertEquals(DialogueVoiceProfile.NONE, DialogueVoiceProfile.forSpeaker("System"))
    }
}
