package com.example.starborn.domain.audio

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AudioCatalog(
    val tracks: List<AudioTrackMetadata> = emptyList(),
    val cues: List<AudioCueMetadata> = emptyList()
) {
    private val tracksById: Map<String, AudioTrackMetadata> = tracks.associateBy { it.id }
    private val cuesById: Map<String, AudioCueMetadata> = cues.associateBy { it.id }

    fun track(id: String?): AudioTrackMetadata? = id?.let { tracksById[it] }
    fun cue(id: String?): AudioCueMetadata? = id?.let { cuesById[it] }
}

@JsonClass(generateAdapter = true)
data class AudioTrackMetadata(
    val id: String,
    val type: String = "music",
    val loop: Boolean = true,
    @Json(name = "fade_in_ms") val fadeInMs: Long? = null,
    @Json(name = "fade_out_ms") val fadeOutMs: Long? = null,
    @Json(name = "loop_start_ms") val loopStartMs: Long? = null,
    @Json(name = "loop_end_ms") val loopEndMs: Long? = null,
    val gain: Float? = null,
    val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudioCueMetadata(
    val id: String,
    val category: String = "ui",
    val priority: Int = 0,
    val gain: Float? = null,
    @Json(name = "fade_in_ms") val fadeInMs: Long? = null,
    @Json(name = "fade_out_ms") val fadeOutMs: Long? = null,
    @Json(name = "duration_ms") val durationMs: Long? = null
)
