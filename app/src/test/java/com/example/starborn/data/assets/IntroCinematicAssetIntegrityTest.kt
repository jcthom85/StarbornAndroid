package com.example.starborn.data.assets

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroCinematicAssetIntegrityTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun illustratedIntroHasCompleteReadableSequenceAndResolvablePanels() {
        val type = Types.newParameterizedType(List::class.java, CinematicSceneAsset::class.java)
        val scenes = moshi.adapter<List<CinematicSceneAsset>>(type)
            .fromJson(File("src/main/assets/cinematics.json").readText())
            .orEmpty()
        val intro = scenes.single { it.id == "intro_prologue" }
        val steps = intro.steps.orEmpty().filterNotNull()

        assertEquals("illustrated", intro.presentation)
        assertEquals(true, intro.skippable)
        assertEquals("amb_intro_containment_pressure", intro.ambientCue)
        assertTrue(steps.sumOf { it.durationSeconds ?: 0.0 } in 25.0..35.0)

        val completeCopy = steps.joinToString("\n") { it.text.orEmpty() }
        listOf(
            // The cold open must name its antagonist. The story bible specifies this
            // PA line verbatim; without it the only signal of danger is a pressure
            // readout, and the scene has no threat in it at all.
            "SOURCE BEAST CONTAINMENT BREACH",
            "Broadcast my identity. Draw it away.",
            // The Chorus answering the Chime is the thematic seed of World 1's
            // "First Sound" pillar and pays off across the campaign.
            "many notes agreeing to carry it",
            "Mute this room. Begin stasis."
        ).forEach { required ->
            assertTrue("Intro is missing required beat: $required", completeCopy.contains(required))
        }

        assertTrue("The prologue must not reveal the unknown speaker", steps.none { it.speaker == "Orion" })
        assertTrue("The prologue must not identify Orion in narration", !completeCopy.contains("Orion"))
        // The beacon line is the quiet beat, but it resolves rather than hooks. The
        // cold open ends on a threat and then lands on the title, so the drop into a
        // mining bunk reads as a deliberate cut rather than an abrupt one.
        assertTrue(
            "The prologue must still include the beacon beat",
            completeCopy.contains("The pod seals. The beacon continues in the dark.")
        )
        assertTrue(
            "The cold open must end on a threat, not a resolution",
            completeCopy.contains("Something reaches the glass.")
        )
        val titleCard = steps.last()
        assertEquals("The prologue must land on the title card", "none", titleCard.captionStyle)
        // Pins the asset, not its container format: the wordmark card is the locked intent, and the
        // asset pack ships WebP. Also assert the file is really there -- an extension-only check
        // would happily pass on a path that resolves to nothing.
        val titleCardPath = titleCard.imagePath.orEmpty()
        assertEquals(
            "The title card must use the shipped Starborn wordmark",
            "images/cinematics/intro_title_card_v1",
            titleCardPath.substringBeforeLast('.')
        )
        assertTrue(
            "The title card asset must exist in the asset pack: $titleCardPath",
            File("../world_assets/src/main/assets/$titleCardPath").isFile
        )
        // The cold open runs on ambience and impacts alone; the theme entering on the
        // wordmark is what makes the title land instead of playing as wallpaper. The
        // bunk suppresses music while the light is off, so the room's audio hands the
        // theme out again on its own.
        assertEquals("music_title_theme", titleCard.musicCue)
        val titleFadeOut = titleCard.fadeOutSeconds ?: 0.0
        assertTrue(
            "The title card must dim into the bunk fade rather than cutting",
            titleFadeOut > 0.0 && titleFadeOut < (titleCard.durationSeconds ?: 0.0)
        )
        assertTrue("The redundant Shift System card should remain removed", steps.none { it.speaker == "SHIFT SYSTEM" })
        assertTrue("The prologue should cut directly into the bunk", !completeCopy.contains("Nova got"))
        val fadeIn = scenes.single { it.id == "new_game_fade_in" }.steps.orEmpty().filterNotNull().single()
        assertEquals("sfx_intro_shift_buzzer", fadeIn.audioCue)
        // This step is not played through the cinematic overlay; ExplorationViewModel
        // reads its duration to time the fade up on the bunk. A short value here reads
        // as a snap cut off the title card, so keep it deliberately slow.
        assertTrue(
            "The wake-up fade must stay slow enough to read as waking, not a cut",
            (fadeIn.durationSeconds ?: 0.0) >= 5.0
        )

        val questCopy = File("src/main/assets/quests.json").readText()
        val roomCopy = File("src/main/assets/rooms.json").readText()
        assertTrue("Opening quest must retain immediate quota pressure", questCopy.contains("Quota is due at first cage"))
        assertTrue("Bunk reveal must retain the suppressed quota ticket", roomCopy.contains("CLOSED: WITHIN QUOTA"))
        val unknownDialogue = steps.filter { it.speaker == "???" }
        assertEquals(2, unknownDialogue.size)
        assertTrue(
            "Spoken intro lines must use dialogue presentation",
            unknownDialogue.all { it.type == "dialogue" && it.captionStyle == "dialogue" }
        )
        assertTrue(
            "Unknown speaker lines must use contextual portraits without revealing the name",
            unknownDialogue.all { !it.portrait.isNullOrBlank() && it.portrait!!.contains("orion_") }
        )
        val illustratedSteps = steps.filter { it.imagePath != null }
        illustratedSteps.zipWithNext().forEach { (current, next) ->
            if (current.imagePath == next.imagePath) {
                assertEquals(current.cameraEndScale, next.cameraStartScale)
                assertEquals(current.cameraEndX ?: 0.0, next.cameraStartX ?: 0.0, 0.001)
                assertEquals(current.cameraEndY ?: 0.0, next.cameraStartY ?: 0.0, 0.001)
            }
        }

        steps.mapNotNull { it.imagePath }.distinct().forEach { imagePath ->
            val file = File("../world_assets/src/main/assets/$imagePath")
            // A stub floor, not a quality bar. It was 100 KB when these panels were multi-megabyte
            // PNGs; the pack now ships WebP at roughly a tenth of that, and the title card -- a
            // wordmark on near-black -- compresses hardest of all at ~90 KB. 20 KB still catches a
            // placeholder or a truncated export, which is all this check was ever for.
            assertTrue("Missing cinematic image: $imagePath", file.isFile && file.length() > 20_000L)
        }
        steps.mapNotNull { it.portrait }.distinct().forEach { portraitPath ->
            val file = File("../world_assets/src/main/assets/$portraitPath")
            assertTrue("Missing cinematic portrait: $portraitPath", file.isFile && file.length() > 100_000L)
        }
    }
}
