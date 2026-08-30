package com.example.starborn.feature.arcade.games.harmonicpulse

import kotlin.math.abs
import kotlin.math.sin

enum class HitRating(val points: Int, val harmonyDelta: Float, val label: String) {
    PERFECT(300, 3.5f, "PERFECT"),
    GREAT(150, 1.5f, "GREAT"),
    OK(50, 0f, "OK"),
    MISS(0, -8f, "MISS")
}

enum class PulseAudioEvent {
    NONE, HIT_NOTE, HIT_PERFECT, OVERDRIVE_TRIGGER, MISS_NOTE, TRACK_CLEAR, GAME_OVER
}

data class RhythmNote(
    val id: Int,
    val lane: Int, // 0 to 3
    var targetTime: Float, // seconds into song
    var y: Float = -0.05f, // 0f (top) to 1f (strike line)
    var hit: Boolean = false,
    var missed: Boolean = false
)

data class HarmonicPulseInput(
    val tapLane0: Boolean = false,
    val tapLane1: Boolean = false,
    val tapLane2: Boolean = false,
    val tapLane3: Boolean = false
)

data class HarmonicPulseSnapshot(
    val score: Int,
    val multiplier: Int,
    val combo: Int,
    val maxCombo: Int,
    val harmony: Float, // 0f to 100f
    val overdriveSeconds: Float,
    val round: Int,
    val notes: List<RhythmNote>,
    val lastRating: HitRating?,
    val ratingTimer: Float,
    val gameOver: Boolean,
    val message: String,
    val elapsedSeconds: Float,
    val audioEvent: PulseAudioEvent = PulseAudioEvent.NONE
)

class HarmonicPulseEngine(private val seed: Long = 0x534F5552L) {
    companion object {
        const val STEP = 1f / 60f
        const val STRIKE_LINE_Y = 0.86f
        const val NOTE_FALL_DURATION = 1.4f // seconds from top to strike line
        const val LANES = 4
        const val TRACK_DURATION = 42f // seconds per song phase
    }

    private var rng = seed
    private var score = 0
    private var multiplier = 1
    private var combo = 0
    private var maxCombo = 0
    private var harmony = 75f
    private var overdriveSeconds = 0f
    private var round = 1
    private var songTime = 0f
    private var nextNoteSpawnTime = 0.8f
    private var lastRating: HitRating? = null
    private var ratingTimer = 0f
    private var nextNoteId = 1

    private var prevInput = HarmonicPulseInput()
    private var gameOver = false
    private var message = "THE SOURCE // HARMONIC RESONANCE START"
    private var elapsed = 0f
    private var accumulator = 0f
    private var currentAudioEvent = PulseAudioEvent.NONE

    private val notes = mutableListOf<RhythmNote>()

    init {
        reset()
    }

    fun reset() {
        rng = seed
        score = 0
        multiplier = 1
        combo = 0
        maxCombo = 0
        harmony = 75f
        overdriveSeconds = 0f
        round = 1
        songTime = 0f
        nextNoteSpawnTime = 0.8f
        lastRating = null
        ratingTimer = 0f
        nextNoteId = 1
        prevInput = HarmonicPulseInput()
        gameOver = false
        message = "THE SOURCE // HARMONIC RESONANCE START"
        elapsed = 0f
        accumulator = 0f
        currentAudioEvent = PulseAudioEvent.NONE
        notes.clear()
        generateInitialTrack()
    }

    private fun generateInitialTrack() {
        notes.clear()
        var t = 1.0f
        val interval = (0.55f - (round - 1) * 0.04f).coerceAtLeast(0.24f)
        while (t < TRACK_DURATION) {
            val lane = (randomFloat() * 4).toInt().coerceIn(0, 3)
            notes.add(RhythmNote(nextNoteId++, lane, t))

            // Occasional simultaneous chord note
            if (randomFloat() < 0.22f && round >= 2) {
                val secondLane = (lane + 1 + (randomFloat() * 2).toInt()) % 4
                notes.add(RhythmNote(nextNoteId++, secondLane, t))
            }

            t += interval + (if (randomFloat() < 0.3f) interval / 2f else 0f)
        }
    }

    fun advance(deltaSeconds: Float, input: HarmonicPulseInput) {
        if (gameOver) return
        currentAudioEvent = PulseAudioEvent.NONE
        accumulator = (accumulator + deltaSeconds.coerceIn(0f, .1f)).coerceAtMost(.2f)
        while (accumulator >= STEP) {
            update(STEP, input)
            accumulator -= STEP
        }
    }

    private fun update(dt: Float, input: HarmonicPulseInput) {
        elapsed += dt
        songTime += dt

        if (ratingTimer > 0f) {
            ratingTimer = (ratingTimer - dt).coerceAtLeast(0f)
        }

        // 1. Overdrive Countdown
        if (overdriveSeconds > 0f) {
            overdriveSeconds = (overdriveSeconds - dt).coerceAtLeast(0f)
        }

        // 2. Process Edge-Triggered Lane Taps
        val tappedLanes = mutableListOf<Int>()
        if (input.tapLane0 && !prevInput.tapLane0) tappedLanes.add(0)
        if (input.tapLane1 && !prevInput.tapLane1) tappedLanes.add(1)
        if (input.tapLane2 && !prevInput.tapLane2) tappedLanes.add(2)
        if (input.tapLane3 && !prevInput.tapLane3) tappedLanes.add(3)
        prevInput = input

        tappedLanes.forEach { lane ->
            processLaneTap(lane)
        }

        // 3. Update Note Positions & Check Misses
        val speedMultiplier = 1f + (round - 1) * 0.08f
        val noteFallSpeed = STRIKE_LINE_Y / NOTE_FALL_DURATION * speedMultiplier

        notes.forEach { note ->
            if (!note.hit && !note.missed) {
                val timeUntilTarget = note.targetTime - songTime
                note.y = STRIKE_LINE_Y - (timeUntilTarget * noteFallSpeed)

                // Check Miss (Passed strike line without tap)
                if (note.y > STRIKE_LINE_Y + 0.12f) {
                    note.missed = true
                    processHit(HitRating.MISS)
                }
            }
        }

        // 4. Check Track Phase Clear
        if (songTime >= TRACK_DURATION && notes.all { it.hit || it.missed }) {
            completeTrack()
        }
    }

    private fun processLaneTap(lane: Int) {
        // Find closest active note in this lane
        val candidate = notes.filter { !it.hit && !it.missed && it.lane == lane }
            .minByOrNull { abs(it.y - STRIKE_LINE_Y) }

        if (candidate != null) {
            val dist = abs(candidate.y - STRIKE_LINE_Y)
            val rating = when {
                dist < 0.045f -> HitRating.PERFECT
                dist < 0.090f -> HitRating.GREAT
                dist < 0.140f -> HitRating.OK
                else -> null
            }

            if (rating != null) {
                candidate.hit = true
                processHit(rating)
            }
        }
    }

    private fun processHit(rating: HitRating) {
        lastRating = rating
        ratingTimer = 0.5f

        if (rating == HitRating.MISS) {
            combo = 0
            multiplier = 1
            harmony = (harmony + rating.harmonyDelta).coerceIn(0f, 100f)
            message = "HARMONY DISRUPTED // MISS"
            currentAudioEvent = PulseAudioEvent.MISS_NOTE

            if (harmony <= 0f) {
                gameOver = true
                message = "RESONANCE DESTABILIZATION // HARMONY COLLAPSE"
                currentAudioEvent = PulseAudioEvent.GAME_OVER
            }
            return
        }

        // Successful Hit
        combo++
        if (combo > maxCombo) maxCombo = combo
        multiplier = (1 + combo / 10).coerceAtMost(5)
        if (overdriveSeconds > 0f) multiplier = (multiplier * 2).coerceAtMost(10)

        val pts = rating.points * multiplier
        score += pts
        harmony = (harmony + rating.harmonyDelta).coerceIn(0f, 100f)

        if (harmony >= 100f && overdriveSeconds <= 0f) {
            overdriveSeconds = 6.0f
            message = "HARMONIC OVERDRIVE ACTIVE // 2× SCORE SURGE!"
            currentAudioEvent = PulseAudioEvent.OVERDRIVE_TRIGGER
        } else {
            message = "${rating.label}! // +$pts PTS"
            currentAudioEvent = if (rating == HitRating.PERFECT) PulseAudioEvent.HIT_PERFECT else PulseAudioEvent.HIT_NOTE
        }
    }

    private fun completeTrack() {
        round++
        val clearBonus = 5000 * multiplier
        score += clearBonus
        harmony = (harmony + 25f).coerceIn(0f, 100f)
        songTime = 0f
        message = "RESONANCE LAYER ${round - 1} CLEARED // BONUS +$clearBonus PTS"
        currentAudioEvent = PulseAudioEvent.TRACK_CLEAR
        generateInitialTrack()
    }

    private fun randomFloat(): Float {
        rng = rng * 6364136223846793005L + 1442695040888963407L
        return (rng ushr 32).toFloat() / 4294967296.0f
    }

    fun snapshot() = HarmonicPulseSnapshot(
        score = score,
        multiplier = multiplier,
        combo = combo,
        maxCombo = maxCombo,
        harmony = harmony,
        overdriveSeconds = overdriveSeconds,
        round = round,
        notes = notes.filter { it.y in -0.1f..1.1f && !it.hit }.map { it.copy() },
        lastRating = lastRating,
        ratingTimer = ratingTimer,
        gameOver = gameOver,
        message = message,
        elapsedSeconds = elapsed,
        audioEvent = currentAudioEvent
    )
}
