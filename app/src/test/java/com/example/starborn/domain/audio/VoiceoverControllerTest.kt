package com.example.starborn.domain.audio

import com.example.starborn.domain.audio.AudioCommand.Play
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceoverControllerTest {

    @Test
    fun `enqueue plays cues sequentially and restores layers`() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        val catalog = AudioCatalog(
            cues = listOf(
                AudioCueMetadata(id = "intro_vo", category = "voice", durationMs = 40),
                AudioCueMetadata(id = "followup_vo", category = "voice", durationMs = 30)
            )
        )
        val router = AudioRouter(AudioBindings(), catalog)
        val dispatched = mutableListOf<List<AudioCommand>>()
        val controller = VoiceoverController(
            audioRouter = router,
            dispatchCommands = { dispatched += it },
            scope = scope,
            dispatcher = dispatcher
        )

        controller.enqueue("intro_vo")
        controller.enqueue("followup_vo")

        assertEquals(1, dispatched.size)
        val firstPlay = dispatched.first().single { it is Play } as Play
        assertEquals("intro_vo", firstPlay.cueId)

        scope.advanceTimeBy(45)
        scope.runCurrent()

        assertEquals(2, dispatched.size)
        val secondPlay = dispatched[1].single { it is Play } as Play
        assertEquals("followup_vo", secondPlay.cueId)

        scope.advanceTimeBy(35)
        scope.runCurrent()

        assertEquals(2, dispatched.size)
    }
}
