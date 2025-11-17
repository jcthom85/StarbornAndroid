package com.example.starborn.domain.audio

import com.squareup.moshi.JsonClass
import java.util.Locale

@JsonClass(generateAdapter = true)
data class AudioBindings(
    val music: Map<String, String> = emptyMap(),
    val ambience: Map<String, String> = emptyMap(),
    val weather: Map<String, String> = emptyMap(),
    val ui: Map<String, String> = emptyMap(),
    val battle: Map<String, String> = emptyMap()
)

enum class AudioCueType {
    MUSIC,
    AMBIENT,
    UI,
    BATTLE,
    VOICE
}

data class VoiceoverPlan(
    val commands: List<AudioCommand>,
    val estimatedDurationMs: Long
)

class AudioRouter(
    private val bindings: AudioBindings,
    private val catalog: AudioCatalog? = null,
    private val defaultFadeMs: Long = 600L
) {

    private var currentMusic: String? = null
    private var currentAmbient: String? = null
    private var currentAmbientLayers: Set<String> = emptySet()
    private var musicGain: Float = 1f
    private var ambientGain: Float = 1f

    fun commandsForRoom(
        hubId: String?,
        roomId: String?,
        weatherId: String? = null,
        tags: Set<String> = emptySet()
    ): List<AudioCommand> {
        val commands = mutableListOf<AudioCommand>()

        var desiredMusic = hubId?.let { bindings.music[it] }.normalize()
        if (desiredMusic == null) {
            desiredMusic = findTracksByTags(tags, AudioCueType.MUSIC).firstOrNull()
        }

        val desiredAmbientLayers = parseCueLayers(
            roomId?.let { bindings.ambience[it] } ?: hubId?.let { bindings.ambience[it] }
        )
        val weatherLayers = parseCueLayers(weatherId?.let { bindings.weather[it] })
        val ambientFromTags = if (desiredAmbientLayers.isEmpty() && weatherLayers.isEmpty()) {
            findTracksByTags(tags, AudioCueType.AMBIENT)
        } else emptyList()
        val combinedAmbientLayers = (desiredAmbientLayers + weatherLayers + ambientFromTags).distinct()
        val desiredAmbientSet = combinedAmbientLayers.toSet()

        if (desiredMusic != currentMusic) {
            currentMusic?.let { prev ->
                commands += AudioCommand.Stop(
                    AudioCueType.MUSIC,
                    prev,
                    fadeMs = trackMetadata(prev)?.fadeOutMs ?: defaultFadeMs
                )
            }
            desiredMusic?.let { next ->
                val meta = trackMetadata(next)
                commands += AudioCommand.Play(
                    AudioCueType.MUSIC,
                    next,
                    loop = meta?.loop ?: true,
                    fadeMs = meta?.fadeInMs ?: defaultFadeMs,
                    gain = meta?.gain ?: 1f
                )
            }
            currentMusic = desiredMusic
            musicGain = 1f
        }

        if (desiredAmbientSet != currentAmbientLayers) {
            val toStop = currentAmbientLayers - desiredAmbientSet
            toStop.forEach { cue ->
                commands += AudioCommand.Stop(
                    AudioCueType.AMBIENT,
                    cue,
                    fadeMs = trackMetadata(cue)?.fadeOutMs ?: defaultFadeMs
                )
            }
            val toStart = combinedAmbientLayers.filterNot { currentAmbientLayers.contains(it) }
            toStart.forEach { cue ->
                val meta = trackMetadata(cue)
                commands += AudioCommand.Play(
                    AudioCueType.AMBIENT,
                    cue,
                    loop = meta?.loop ?: true,
                    fadeMs = meta?.fadeInMs ?: defaultFadeMs,
                    gain = meta?.gain ?: 0.85f
                )
            }
            currentAmbientLayers = desiredAmbientSet
            currentAmbient = combinedAmbientLayers.firstOrNull()
            ambientGain = 1f
        }

        return commands
    }

    fun commandsForUi(event: String): List<AudioCommand> {
        val normalized = bindings.ui[event].normalize() ?: return emptyList()
        val meta = cueMetadata(normalized)
        return listOf(
            AudioCommand.Play(
                AudioCueType.UI,
                normalized,
                loop = false,
                fadeMs = meta?.fadeInMs ?: 0L,
                gain = meta?.gain ?: 1f,
                triggerHaptic = true
            )
        )
    }

    fun commandsForBattle(event: String): List<AudioCommand> {
        val normalized = bindings.battle[event].normalize() ?: return emptyList()
        val commands = mutableListOf<AudioCommand>()
        val meta = cueMetadata(normalized)
        if (musicGain > 0.4f && currentMusic != null) {
            commands += AudioCommand.Duck(AudioCueType.MUSIC, gain = 0.35f, fadeMs = 250L)
            musicGain = 0.35f
        }
        commands += AudioCommand.Play(
            AudioCueType.BATTLE,
            normalized,
            loop = false,
            gain = meta?.gain ?: 1f,
            fadeMs = meta?.fadeInMs ?: 0L,
            triggerHaptic = true
        )
        return commands
    }

    fun voiceoverPlan(cueId: String, duckLayers: Boolean = true): VoiceoverPlan? {
        val normalized = cueId.normalize() ?: return null
        val commands = mutableListOf<AudioCommand>()
        if (duckLayers) {
            commands += duckForCinematic(targetGain = VOICE_DUCK_GAIN, fadeMs = VOICE_DUCK_FADE_MS)
        }
        val meta = cueMetadata(normalized)
        commands += AudioCommand.Play(
            AudioCueType.VOICE,
            normalized,
            loop = false,
            fadeMs = meta?.fadeInMs ?: 0L,
            gain = meta?.gain ?: 1f
        )
        val estimate = meta?.durationMs ?: DEFAULT_VOICE_RESTORE_MS
        return VoiceoverPlan(commands, estimate)
    }

    fun restoreAfterVoiceover(): List<AudioCommand> = restoreAfterCinematic()

    fun restoreLayer(layer: AudioCueType, fadeMs: Long = defaultFadeMs): List<AudioCommand> {
        return when (layer) {
            AudioCueType.MUSIC -> {
                if (musicGain >= 0.99f || currentMusic == null) emptyList()
                else {
                    musicGain = 1f
                    listOf(AudioCommand.Restore(AudioCueType.MUSIC, trackMetadata(currentMusic)?.fadeInMs ?: fadeMs))
                }
            }
            AudioCueType.AMBIENT -> {
                if (ambientGain >= 0.99f || currentAmbientLayers.isEmpty()) emptyList()
                else {
                    ambientGain = 1f
                    val primary = currentAmbient ?: currentAmbientLayers.firstOrNull()
                    listOf(AudioCommand.Restore(AudioCueType.AMBIENT, trackMetadata(primary)?.fadeInMs ?: fadeMs))
                }
            }
            else -> emptyList()
        }
    }

    fun reset(): List<AudioCommand> {
        val commands = mutableListOf<AudioCommand>()
        currentMusic?.let { commands += AudioCommand.Stop(AudioCueType.MUSIC, it, fadeMs = trackMetadata(it)?.fadeOutMs ?: defaultFadeMs) }
        currentAmbientLayers.forEach { cue ->
            commands += AudioCommand.Stop(AudioCueType.AMBIENT, cue, fadeMs = trackMetadata(cue)?.fadeOutMs ?: defaultFadeMs)
        }
        currentMusic = null
        currentAmbient = null
        currentAmbientLayers = emptySet()
        musicGain = 1f
        ambientGain = 1f
        return commands
    }

    fun commandsForLayerOverride(
        layer: AudioCueType,
        cueId: String? = null,
        gain: Float? = null,
        fadeMs: Long? = null,
        loop: Boolean? = null,
        stop: Boolean = false
    ): List<AudioCommand> {
        val commands = mutableListOf<AudioCommand>()
        val resolvedFade = fadeMs ?: defaultFadeMs
        when (layer) {
            AudioCueType.MUSIC -> commands += overrideMusicLayer(cueId, gain, resolvedFade, loop, stop)
            AudioCueType.AMBIENT -> commands += overrideAmbientLayer(cueId, gain, resolvedFade, loop, stop)
            AudioCueType.UI, AudioCueType.BATTLE, AudioCueType.VOICE -> {
                val normalized = cueId.normalize() ?: return emptyList()
                val meta = cueMetadata(normalized)
                commands += AudioCommand.Play(
                    layer,
                    normalized,
                    loop = loop ?: false,
                    fadeMs = meta?.fadeInMs ?: resolvedFade,
                    gain = gain ?: meta?.gain ?: 1f
                )
            }
        }
        return commands
    }

    private fun overrideMusicLayer(
        cueId: String?,
        gain: Float?,
        fadeMs: Long,
        loop: Boolean?,
        stop: Boolean
    ): List<AudioCommand> {
        val commands = mutableListOf<AudioCommand>()
        val normalized = cueId.normalize()
        when {
            stop -> {
                val target = normalized ?: currentMusic
                if (target != null && currentMusic == target) {
                    commands += AudioCommand.Stop(
                        AudioCueType.MUSIC,
                        target,
                        trackMetadata(target)?.fadeOutMs ?: fadeMs
                    )
                    currentMusic = null
                    musicGain = 1f
                }
            }
            normalized != null -> {
                if (normalized != currentMusic) {
                    currentMusic?.let { prev ->
                        commands += AudioCommand.Stop(
                            AudioCueType.MUSIC,
                            prev,
                            trackMetadata(prev)?.fadeOutMs ?: fadeMs
                        )
                    }
                    val meta = trackMetadata(normalized)
                    commands += AudioCommand.Play(
                        AudioCueType.MUSIC,
                        normalized,
                        loop = loop ?: meta?.loop ?: true,
                        fadeMs = meta?.fadeInMs ?: fadeMs,
                        gain = gain ?: meta?.gain ?: 1f
                    )
                    currentMusic = normalized
                    musicGain = 1f
                } else if (gain != null) {
                    adjustLayerGain(AudioCueType.MUSIC, gain, fadeMs)?.let { commands += it }
                }
            }
            gain != null -> {
                adjustLayerGain(AudioCueType.MUSIC, gain, fadeMs)?.let { commands += it }
            }
        }
        return commands
    }

    private fun overrideAmbientLayer(
        cueId: String?,
        gain: Float?,
        fadeMs: Long,
        loop: Boolean?,
        stop: Boolean
    ): List<AudioCommand> {
        val commands = mutableListOf<AudioCommand>()
        val normalized = cueId.normalize()
        when {
            stop -> {
                if (normalized != null) {
                    if (currentAmbientLayers.contains(normalized)) {
                        commands += AudioCommand.Stop(
                            AudioCueType.AMBIENT,
                            normalized,
                            trackMetadata(normalized)?.fadeOutMs ?: fadeMs
                        )
                        currentAmbientLayers = currentAmbientLayers - normalized
                    }
                } else if (currentAmbientLayers.isNotEmpty()) {
                    currentAmbientLayers.forEach { cue ->
                        commands += AudioCommand.Stop(
                            AudioCueType.AMBIENT,
                            cue,
                            trackMetadata(cue)?.fadeOutMs ?: fadeMs
                        )
                    }
                    currentAmbientLayers = emptySet()
                }
                currentAmbient = currentAmbientLayers.firstOrNull()
                ambientGain = 1f
            }
            normalized != null -> {
                if (!currentAmbientLayers.contains(normalized)) {
                    val meta = trackMetadata(normalized)
                    commands += AudioCommand.Play(
                        AudioCueType.AMBIENT,
                        normalized,
                        loop = loop ?: meta?.loop ?: true,
                        fadeMs = meta?.fadeInMs ?: fadeMs,
                        gain = gain ?: meta?.gain ?: 0.85f
                    )
                    currentAmbientLayers = currentAmbientLayers + normalized
                    currentAmbient = normalized
                    ambientGain = 1f
                } else if (gain != null) {
                    adjustLayerGain(AudioCueType.AMBIENT, gain, fadeMs)?.let { commands += it }
                }
            }
            gain != null -> {
                adjustLayerGain(AudioCueType.AMBIENT, gain, fadeMs)?.let { commands += it }
            }
        }
        return commands
    }

    private fun adjustLayerGain(
        layer: AudioCueType,
        gain: Float,
        fadeMs: Long
    ): AudioCommand? {
        val clamped = gain.coerceIn(0f, 1f)
        return if (clamped >= 0.99f) {
            when (layer) {
                AudioCueType.MUSIC -> musicGain = 1f
                AudioCueType.AMBIENT -> ambientGain = 1f
                else -> Unit
            }
            AudioCommand.Restore(layer, fadeMs)
        } else {
            when (layer) {
                AudioCueType.MUSIC -> musicGain = clamped
                AudioCueType.AMBIENT -> ambientGain = clamped
                else -> Unit
            }
            AudioCommand.Duck(layer, clamped, fadeMs)
        }
    }

    fun duckForCinematic(targetGain: Float = 0.25f, fadeMs: Long = 300L): List<AudioCommand> {
        val commands = mutableListOf<AudioCommand>()
        if (currentMusic != null && musicGain > targetGain) {
            musicGain = targetGain
            commands += AudioCommand.Duck(AudioCueType.MUSIC, gain = targetGain, fadeMs = fadeMs)
        }
        if (currentAmbientLayers.isNotEmpty() && ambientGain > targetGain) {
            ambientGain = targetGain
            commands += AudioCommand.Duck(AudioCueType.AMBIENT, gain = targetGain, fadeMs = fadeMs)
        }
        return commands
    }

    fun restoreAfterCinematic(): List<AudioCommand> {
        val commands = mutableListOf<AudioCommand>()
        commands += restoreLayer(AudioCueType.MUSIC)
        commands += restoreLayer(AudioCueType.AMBIENT)
        return commands
    }

    private fun String?.normalize(): String? =
        this?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.lowercase(Locale.getDefault())
            ?.replace('-', '_')

    private fun trackMetadata(id: String?): AudioTrackMetadata? = catalog?.track(id)
    private fun cueMetadata(id: String?): AudioCueMetadata? = catalog?.cue(id)

    private fun parseCueLayers(raw: String?): List<String> =
        raw?.split(',', '+', ';')
            ?.mapNotNull { component -> component.normalize() }
            ?.distinct()
            ?: emptyList()

    private fun findTracksByTags(tags: Set<String>, type: AudioCueType): List<String> {
        if (catalog == null || tags.isEmpty()) return emptyList()
        val normalizedTags = tags.map { it.lowercase(Locale.getDefault()) }.toSet()
        val typeName = when (type) {
            AudioCueType.MUSIC -> "music"
            AudioCueType.AMBIENT -> "ambient"
            else -> return emptyList()
        }
        return catalog.tracks
            .filter { track ->
                track.type.equals(typeName, ignoreCase = true) &&
                    track.tags.any { normalizedTags.contains(it.lowercase(Locale.getDefault())) }
            }
            .mapNotNull { track -> track.id.normalize() }
    }
    companion object {
        private const val DEFAULT_VOICE_RESTORE_MS = 3_200L
        private const val VOICE_DUCK_GAIN = 0.45f
        private const val VOICE_DUCK_FADE_MS = 220L
    }
}
