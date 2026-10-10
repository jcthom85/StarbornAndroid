package com.example.starborn.desktop

import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.desktop.ui.DialogueVoiceProfile
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class DesktopDialogueMurmurPitchTest {

    private lateinit var assetProvider: DesktopAssetProvider
    private lateinit var driver: DesktopAudioDriver

    @Before
    fun setUp() {
        assetProvider = DesktopAssetProvider()
        driver = DesktopAudioDriver(assetProvider, prewarmDefaults = true)
    }

    @After
    fun tearDown() {
        driver.release()
    }

    @Test
    fun voiceProfilesMapToDedicatedCharacterMurmurAssets() {
        val nova = DialogueVoiceProfile.forSpeaker("Nova")
        val orion = DialogueVoiceProfile.forSpeaker("Orion")
        val zeke = DialogueVoiceProfile.forSpeaker("Zeke")
        val ghost = DialogueVoiceProfile.forSpeaker("Gh0st")
        val femaleNpc = DialogueVoiceProfile.forSpeaker("Maddie")
        val maleNpc = DialogueVoiceProfile.forSpeaker("Jed")

        assertEquals("voice_murmur_nova", nova.cuePrefix)
        assertEquals("voice_murmur_orion", orion.cuePrefix)
        assertEquals("voice_murmur_zeke", zeke.cuePrefix)
        assertEquals("voice_murmur_gh0st", ghost.cuePrefix)
        assertEquals("voice_murmur_female", femaleNpc.cuePrefix)
        assertEquals("voice_murmur_male", maleNpc.cuePrefix)

        val random = Random(42)
        assertTrue(nova.randomCue(random).startsWith("voice_murmur_nova_"))
        assertTrue(orion.randomCue(random).startsWith("voice_murmur_orion_"))
        assertTrue(zeke.randomCue(random).startsWith("voice_murmur_zeke_"))
        assertTrue(ghost.randomCue(random).startsWith("voice_murmur_gh0st_"))
    }

    @Test
    fun celestePitchModulationCalculatesDynamicMelodicContours() {
        val profile = DialogueVoiceProfile.NOVA
        val random = Random(123)
        val text = "Are the reactor shielding manifolds holding up during this solar flare?"

        // Sample pitches across characters
        val pitches = (0 until text.length step 3).map { index ->
            profile.calculateCelestePitch(index, text, random)
        }

        // Must have non-zero variation (pitch is dynamic, not flat)
        val minPitch = pitches.minOrNull() ?: 1.0f
        val maxPitch = pitches.maxOrNull() ?: 1.0f
        assertTrue("Pitch must fluctuate dynamically (Celeste style)", maxPitch > minPitch)
        assertTrue("Pitch range should be within safe audible bounds", minPitch >= 0.6f && maxPitch <= 1.8f)

        // Question sentences rise in pitch towards the end (inflection)
        val earlyPitch = profile.calculateCelestePitch(5, text, Random(999))
        val latePitch = profile.calculateCelestePitch(text.length - 2, text, Random(999))
        assertTrue("Questions should have rising pitch contour toward the end", latePitch > earlyPitch - 0.05f)
    }

    @Test
    fun audioDriverPlaysDialogueMurmurWithDynamicPitchResampling() {
        val deadline = System.currentTimeMillis() + 3000L
        while (System.currentTimeMillis() < deadline && !driver.isPcmCached("voice_murmur_nova_01")) {
            Thread.sleep(50)
        }
        assertTrue("voice_murmur_nova_01 should be prewarmed in PCM cache", driver.isPcmCached("voice_murmur_nova_01"))

        // Dispatch higher-pitch blip
        driver.execute(
            AudioCommand.Play(
                type = AudioCueType.VOICE,
                cueId = "voice_murmur_nova_01",
                loop = false,
                fadeMs = 0L,
                gain = 0.9f,
                pitch = 1.25f
            )
        )

        // Dispatch lower-pitch blip
        driver.execute(
            AudioCommand.Play(
                type = AudioCueType.VOICE,
                cueId = "voice_murmur_orion_01",
                loop = false,
                fadeMs = 0L,
                gain = 0.9f,
                pitch = 0.85f
            )
        )

        Thread.sleep(100)
    }
}
