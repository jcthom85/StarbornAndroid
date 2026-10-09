package com.example.starborn.feature.arcade.games.slagcatcher

import kotlin.math.abs
import kotlin.math.sin

enum class SlagItemType(val points: Int, val heatDelta: Float, val isHazard: Boolean) {
    SLAG_DROPLET(100, 4f, false),
    TITANIUM_INGOT(300, 8f, false),
    PRISMATIC_CORE(1000, -25f, false),
    VOLATILE_BOMB(0, 0f, true)
}

enum class SlagAudioEvent {
    NONE, CATCH_DROPLET, CATCH_INGOT, CATCH_CORE, HIT_BOMB, VENT_STEAM, BUCKET_LOST, ROUND_CLEAR, GAME_OVER
}

data class FallingSlag(
    val id: Int,
    val type: SlagItemType,
    var x: Float, // 0f to 1f normalized
    var y: Float, // 0f (top) to 1f (bottom)
    var vy: Float, // speed
    var radius: Float = 0.035f,
    var caught: Boolean = false
)

data class SlagCatcherInput(
    val paddleTargetX: Float? = null, // Direct touch target
    val moveLeft: Boolean = false,
    val moveRight: Boolean = false,
    val ventSteam: Boolean = false
)

data class SlagCatcherSnapshot(
    val paddleX: Float, // 0f to 1f
    val paddleWidth: Float,
    val buckets: Int, // 1 to 3
    val score: Int,
    val multiplier: Int,
    val streak: Int,
    val heat: Float, // 0f to 100f
    val round: Int,
    val itemsRemainingInShift: Int,
    val fallingItems: List<FallingSlag>,
    val steamVentingSeconds: Float,
    val gameOver: Boolean,
    val message: String,
    val elapsedSeconds: Float,
    val audioEvent: SlagAudioEvent = SlagAudioEvent.NONE
)

class SlagCatcherEngine(private val seed: Long = 0x534C4147L) {
    companion object {
        const val STEP = 1f / 60f
        const val BASE_PADDLE_WIDTH = 0.22f
        const val MAX_HEAT = 100f
        const val SHIFT_ITEMS = 35
    }

    private var rng = seed
    private var paddleX = 0.5f
    private var buckets = 3
    private var score = 0
    private var multiplier = 1
    private var streak = 0
    private var heat = 0f
    private var round = 1
    private var itemsLeftInShift = SHIFT_ITEMS
    private var spawnTimer = 0.6f
    private var steamVentingSeconds = 0f
    private var nextItemId = 1

    private var gameOver = false
    private var message = "FOUNDRY BLAST CRUCIBLE // SHIFT START"
    private var elapsed = 0f
    private var accumulator = 0f
    private var currentAudioEvent = SlagAudioEvent.NONE

    private val fallingItems = mutableListOf<FallingSlag>()

    init {
        reset()
    }

    fun reset() {
        rng = seed
        paddleX = 0.5f
        buckets = 3
        score = 0
        multiplier = 1
        streak = 0
        heat = 0f
        round = 1
        itemsLeftInShift = SHIFT_ITEMS
        spawnTimer = 0.6f
        steamVentingSeconds = 0f
        nextItemId = 1
        gameOver = false
        message = "FOUNDRY BLAST CRUCIBLE // SHIFT START"
        elapsed = 0f
        accumulator = 0f
        currentAudioEvent = SlagAudioEvent.NONE
        fallingItems.clear()
    }

    fun advance(deltaSeconds: Float, input: SlagCatcherInput) {
        if (gameOver) return
        currentAudioEvent = SlagAudioEvent.NONE
        accumulator = (accumulator + deltaSeconds.coerceIn(0f, .1f)).coerceAtMost(.2f)
        while (accumulator >= STEP) {
            update(STEP, input)
            accumulator -= STEP
        }
    }

    private fun update(dt: Float, input: SlagCatcherInput) {
        elapsed += dt

        // 1. Handle Paddle Movement
        val moveSpeed = 1.3f // normalized screens per second
        if (input.paddleTargetX != null) {
            val diff = input.paddleTargetX - paddleX
            paddleX += (diff * 14f * dt).coerceIn(-moveSpeed * dt, moveSpeed * dt)
        } else {
            if (input.moveLeft) paddleX -= moveSpeed * dt
            if (input.moveRight) paddleX += moveSpeed * dt
        }
        val currentPaddleWidth = BASE_PADDLE_WIDTH * (0.7f + buckets * 0.1f)
        paddleX = paddleX.coerceIn(currentPaddleWidth / 2f, 1f - currentPaddleWidth / 2f)

        // 2. Handle Steam Venting
        if (input.ventSteam && heat > 20f && steamVentingSeconds <= 0f) {
            steamVentingSeconds = 0.6f
            val ventedHeat = heat
            heat = 0f
            val ventBonus = (ventedHeat * 10f).toInt() * multiplier
            score += ventBonus
            message = "HEAT VENTED // +$ventBonus PTS"
            currentAudioEvent = SlagAudioEvent.VENT_STEAM
        }

        if (steamVentingSeconds > 0f) {
            steamVentingSeconds = (steamVentingSeconds - dt).coerceAtLeast(0f)
        }

        // Natural heat dissipation
        heat = (heat - dt * 1.5f).coerceAtLeast(0f)

        // 3. Spawning Falling Items
        spawnTimer -= dt
        if (spawnTimer <= 0f && itemsLeftInShift > 0) {
            spawnFallingItem()
            val baseInterval = (0.75f - (round - 1) * 0.06f).coerceAtLeast(0.28f)
            spawnTimer = baseInterval + randomFloat() * 0.35f
        }

        // 4. Update Falling Items
        val speedMultiplier = 1f + (round - 1) * 0.12f
        val catchLineY = 0.88f
        val iterator = fallingItems.iterator()

        while (iterator.hasNext()) {
            val item = iterator.next()
            item.y += item.vy * speedMultiplier * dt

            // Check Catch Line
            if (!item.caught && item.y >= catchLineY - 0.03f && item.y <= catchLineY + 0.05f) {
                val halfW = currentPaddleWidth / 2f
                if (item.x >= paddleX - halfW && item.x <= paddleX + halfW) {
                    item.caught = true
                    processCatch(item)
                }
            }

            // Check Miss (Fell off bottom)
            if (item.y > 1.05f) {
                if (!item.caught && !item.type.isHazard) {
                    processMiss(item)
                }
                iterator.remove()
            }
        }

        // 5. Shift Clear Check
        if (itemsLeftInShift <= 0 && fallingItems.isEmpty()) {
            completeShift()
        }
    }

    private fun spawnFallingItem() {
        itemsLeftInShift--
        val roll = randomFloat()
        val type = when {
            roll < 0.18f -> SlagItemType.VOLATILE_BOMB
            roll < 0.28f -> SlagItemType.PRISMATIC_CORE
            roll < 0.58f -> SlagItemType.TITANIUM_INGOT
            else -> SlagItemType.SLAG_DROPLET
        }

        val baseSpeed = when (type) {
            SlagItemType.SLAG_DROPLET -> 0.42f
            SlagItemType.TITANIUM_INGOT -> 0.55f
            SlagItemType.PRISMATIC_CORE -> 0.48f
            SlagItemType.VOLATILE_BOMB -> 0.38f
        }

        val spawnX = 0.08f + randomFloat() * 0.84f
        fallingItems.add(
            FallingSlag(
                id = nextItemId++,
                type = type,
                x = spawnX,
                y = -0.05f,
                vy = baseSpeed + randomFloat() * 0.08f
            )
        )
    }

    private fun processCatch(item: FallingSlag) {
        if (item.type.isHazard) {
            // Caught a bomb! Destroy 1 crucible bucket
            buckets--
            multiplier = 1
            streak = 0
            message = "VOLATILE SLAG DETONATION! BUCKET LOST"
            currentAudioEvent = SlagAudioEvent.HIT_BOMB
            if (buckets <= 0) {
                gameOver = true
                message = "CRUCIBLE BREACH // ALL BUCKETS DESTROYED"
                currentAudioEvent = SlagAudioEvent.GAME_OVER
            }
            return
        }

        // Safe Catch
        streak++
        if (streak % 8 == 0) {
            multiplier = (multiplier + 1).coerceAtMost(5)
        }

        val pts = item.type.points * multiplier
        score += pts
        heat = (heat + item.type.heatDelta).coerceIn(0f, MAX_HEAT)

        when (item.type) {
            SlagItemType.SLAG_DROPLET -> {
                currentAudioEvent = SlagAudioEvent.CATCH_DROPLET
            }
            SlagItemType.TITANIUM_INGOT -> {
                currentAudioEvent = SlagAudioEvent.CATCH_INGOT
                message = "TITANIUM INGOT RECOVERED // +$pts PTS"
            }
            SlagItemType.PRISMATIC_CORE -> {
                currentAudioEvent = SlagAudioEvent.CATCH_CORE
                message = "PRISMATIC ALLOY CORE! CRUCIBLE COOLED // +$pts PTS"
            }
            else -> Unit
        }

        // Overheat check
        if (heat >= MAX_HEAT) {
            buckets--
            heat = 40f
            multiplier = 1
            streak = 0
            message = "CRUCIBLE BOIL OVER! BUCKET MELTED"
            currentAudioEvent = SlagAudioEvent.BUCKET_LOST
            if (buckets <= 0) {
                gameOver = true
                message = "CRUCIBLE MELTDOWN // ALL BUCKETS LOST"
                currentAudioEvent = SlagAudioEvent.GAME_OVER
            }
        }
    }

    private fun processMiss(item: FallingSlag) {
        buckets--
        multiplier = 1
        streak = 0
        message = "SLAG SPILLED INTO DRAIN! BUCKET DAMAGED"
        currentAudioEvent = SlagAudioEvent.BUCKET_LOST
        if (buckets <= 0) {
            gameOver = true
            message = "CRUCIBLE ABANDONED // SHIFT FAILED"
            currentAudioEvent = SlagAudioEvent.GAME_OVER
        }
    }

    private fun completeShift() {
        round++
        val shiftBonus = 2000 * multiplier
        score += shiftBonus
        if (buckets < 3) buckets++
        itemsLeftInShift = SHIFT_ITEMS + round * 5
        multiplier = (multiplier + 1).coerceAtMost(5)
        message = "SHIFT ${round - 1} COMPLETED // BONUS +$shiftBonus PTS"
        currentAudioEvent = SlagAudioEvent.ROUND_CLEAR
    }

    private fun randomFloat(): Float {
        rng = rng * 6364136223846793005L + 1442695040888963407L
        return (rng ushr 32).toFloat() / 4294967296.0f
    }

    fun snapshot() = SlagCatcherSnapshot(
        paddleX = paddleX,
        paddleWidth = BASE_PADDLE_WIDTH * (0.7f + buckets * 0.1f),
        buckets = buckets,
        score = score,
        multiplier = multiplier,
        streak = streak,
        heat = heat,
        round = round,
        itemsRemainingInShift = itemsLeftInShift,
        fallingItems = fallingItems.map { it.copy() },
        steamVentingSeconds = steamVentingSeconds,
        gameOver = gameOver,
        message = message,
        elapsedSeconds = elapsed,
        audioEvent = currentAudioEvent
    )
}
