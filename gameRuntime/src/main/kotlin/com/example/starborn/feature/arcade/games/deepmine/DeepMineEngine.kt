package com.example.starborn.feature.arcade.games.deepmine

import kotlin.math.abs
import kotlin.math.roundToInt

data class DeepMineInput(
    val left: Boolean = false,
    val right: Boolean = false,
    val boost: Boolean = false,
    val drill: Boolean = false
)

data class DeepMinePad(
    val id: String = "primary",
    val x: Float,
    val width: Float,
    val maxOre: Float,
    val ore: Float,
    val purity: Int,
    val isFissure: Boolean = false
)

data class DeepMinePickup(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val type: String = "fuel",
    val collected: Boolean = false
)

enum class DeepMineAudioEvent {
    NONE, THRUST, DRILL, PICKUP, CLEAN_LANDING, HARD_LANDING, ALARM, CRASH, STAGE_CLEAR
}

data class DeepMineSnapshot(
    val shipX: Float,
    val shipY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val tiltAngle: Float,
    val fuel: Float,
    val score: Int,
    val multiplier: Int,
    val oreExtracted: Float,
    val pads: List<DeepMinePad>,
    val activePad: DeepMinePad?,
    val pickup: DeepMinePickup?,
    val landed: Boolean,
    val gameOver: Boolean,
    val message: String,
    val elapsedSeconds: Float,
    val depth: Int,
    val slagX: Float,
    val slagY: Float,
    val slagWarning: Boolean,
    val audioEvent: DeepMineAudioEvent = DeepMineAudioEvent.NONE
) {
    // Backward compatibility for existing tests and simple readers
    val pad: DeepMinePad get() = activePad ?: pads.firstOrNull() ?: DeepMinePad("primary", 100f, 60f, 50f, 50f, 10)
}

class DeepMineEngine(private val seed: Long = 0x48595045L) {
    companion object {
        const val WORLD_WIDTH = 320f
        const val WORLD_HEIGHT = 360f
        const val SHIP_HALF_WIDTH = 8f
        const val SHIP_HALF_HEIGHT = 9f
        const val GROUND_Y = 326f
        private const val STEP = 1f / 60f
    }

    private var rng = seed
    private var shipX = 160f
    private var shipY = 54f
    private var vx = 0f
    private var vy = 0f
    private var tiltAngle = 0f
    private var fuel = 100f
    private var score = 0
    private var multiplier = 1
    private var oreExtracted = 0f
    private var padIndex = 0
    private var pads = createPads(0)
    private var activePad: DeepMinePad? = null
    private var pickup: DeepMinePickup? = createPickup(0)
    private var landed = false
    private var gameOver = false
    private var message = "EASE ONTO AN ORE PAD"
    private var elapsed = 0f
    private var accumulator = 0f
    private var slagX = 72f
    private var slagY = -40f
    private var slagDelay = 4f
    private var currentAudioEvent = DeepMineAudioEvent.NONE

    fun reset() {
        rng = seed
        shipX = 160f; shipY = 54f; vx = 0f; vy = 0f; tiltAngle = 0f; fuel = 100f
        score = 0; multiplier = 1; oreExtracted = 0f; padIndex = 0
        pads = createPads(0); activePad = null; pickup = createPickup(0)
        landed = false; gameOver = false
        message = "EASE ONTO AN ORE PAD"; elapsed = 0f; accumulator = 0f
        slagX = 72f; slagY = -40f; slagDelay = 4f
        currentAudioEvent = DeepMineAudioEvent.NONE
    }

    fun advance(deltaSeconds: Float, input: DeepMineInput) {
        if (gameOver) return
        currentAudioEvent = DeepMineAudioEvent.NONE
        accumulator = (accumulator + deltaSeconds.coerceIn(0f, .1f)).coerceAtMost(.2f)
        while (accumulator >= STEP) {
            update(STEP, input)
            accumulator -= STEP
        }
    }

    private fun update(dt: Float, input: DeepMineInput) {
        elapsed += dt
        updateSlag(dt)
        updatePickup(dt)
        if (gameOver) return

        val currentPad = activePad
        if (landed && currentPad != null && input.drill && currentPad.ore > 0f && fuel > 0f) {
            val drillRate = if (currentPad.isFissure) 18f else 13f
            val mined = minOf(currentPad.ore, drillRate * dt)
            val updatedPad = currentPad.copy(ore = currentPad.ore - mined)
            activePad = updatedPad
            pads = pads.map { if (it.id == updatedPad.id) updatedPad else it }
            oreExtracted += mined
            fuel = (fuel - 1.5f * dt).coerceAtLeast(0f)
            score += (mined * updatedPad.purity * multiplier).roundToInt()
            message = if (updatedPad.isFissure) "HIGH PURITY DRILL // PURITY ${updatedPad.purity}" else "DRILLING // PURITY ${updatedPad.purity}"
            currentAudioEvent = DeepMineAudioEvent.DRILL
            if (updatedPad.ore <= .01f) completePad(updatedPad)
            return
        }

        if (landed && !input.boost && !input.left && !input.right) {
            val padOre = currentPad?.ore ?: 0f
            message = if (padOre > 0f) "HOLD DRILL TO EXTRACT" else message
            tiltAngle = 0f
            return
        }

        if (landed && (input.boost || input.left || input.right)) {
            landed = false
            activePad = null
            vy = -10f
        }

        // Gravity & Thrusters
        vy += 20f * dt
        if (input.left && fuel > 0f) {
            vx -= 31f * dt
            fuel -= 2.5f * dt
        }
        if (input.right && fuel > 0f) {
            vx += 31f * dt
            fuel -= 2.5f * dt
        }
        if (input.boost && fuel > 0f) {
            vy -= 46f * dt
            fuel -= 5.5f * dt
            currentAudioEvent = DeepMineAudioEvent.THRUST
        }

        vx = vx.coerceIn(-52f, 52f)
        vy = vy.coerceIn(-62f, 72f)
        shipX += vx * dt
        shipY += vy * dt
        fuel = fuel.coerceAtLeast(0f)

        // Dynamic Hull Tilt / Banking Angle
        val targetTilt = when {
            input.left -> -11f
            input.right -> 11f
            else -> (vx * 0.22f).coerceIn(-12f, 12f)
        }
        tiltAngle += (targetTilt - tiltAngle) * (14f * dt)

        // Wall collisions
        if (shipX < SHIP_HALF_WIDTH || shipX > WORLD_WIDTH - SHIP_HALF_WIDTH || shipY < SHIP_HALF_HEIGHT) {
            crash("PROBE LOST IN THE SHAFT")
            return
        }

        // Ground / Landing check
        if (shipY + SHIP_HALF_HEIGHT >= GROUND_Y) {
            classifyLanding()
        }

        // Fuel exhaustion check
        if (fuel <= 0f && !landed) {
            crash("FUEL EXHAUSTED")
        }
    }

    private fun updatePickup(dt: Float) {
        val current = pickup ?: return
        if (current.collected) return
        val newX = current.x + current.vx * dt
        val newY = current.y + current.vy * dt
        val bouncedVx = if (newX < 24f || newX > WORLD_WIDTH - 24f) -current.vx else current.vx
        pickup = current.copy(x = newX, y = newY, vx = bouncedVx)

        // Pickup collision detection
        if (abs(shipX - newX) < 14f && abs(shipY - newY) < 14f) {
            pickup = current.copy(collected = true)
            fuel = (fuel + 22f).coerceAtMost(100f)
            score += 500 * multiplier
            message = "COOLANT POD SECURED // +22% FUEL"
            currentAudioEvent = DeepMineAudioEvent.PICKUP
        }
    }

    private fun updateSlag(dt: Float) {
        if (padIndex == 0) return
        if (slagDelay > 0f) {
            slagDelay -= dt
            return
        }
        slagY += (42f + padIndex * 4f) * dt
        if (abs(shipX - slagX) < 12f && abs(shipY - slagY) < 14f) {
            crash("FALLING SLAG // HULL BREACH")
            return
        }
        if (slagY > GROUND_Y + 16f) {
            slagX = 24f + nextFloat() * (WORLD_WIDTH - 48f)
            slagY = -18f
            slagDelay = (2.8f - padIndex * .15f).coerceAtLeast(1.25f)
        }
    }

    private fun classifyLanding() {
        val targetPad = pads.firstOrNull { pad ->
            shipX >= pad.x + SHIP_HALF_WIDTH && shipX <= pad.x + pad.width - SHIP_HALF_WIDTH
        }

        if (targetPad == null || abs(vx) > 22f || vy > 34f) {
            crash(if (targetPad == null) "MISSED THE ORE PAD" else "HULL FAILURE // HARD IMPACT")
            return
        }

        landed = true
        activePad = targetPad
        shipY = GROUND_Y - SHIP_HALF_HEIGHT
        tiltAngle = 0f
        val clean = abs(vx) <= 11f && vy <= 21f
        vx = 0f; vy = 0f

        if (clean) {
            multiplier = (multiplier + 1).coerceAtMost(5)
            score += (if (targetPad.isFissure) 600 else 350) * multiplier
            message = if (targetPad.isFissure) "CLEAN FISSURE LOCK // ${multiplier}X" else "CLEAN LANDING // ${multiplier}X"
            currentAudioEvent = DeepMineAudioEvent.CLEAN_LANDING
        } else {
            multiplier = 1
            score += 100
            message = "ROUGH LANDING // STREAK RESET"
            currentAudioEvent = DeepMineAudioEvent.HARD_LANDING
        }
    }

    private fun completePad(completedPad: DeepMinePad) {
        val fuelBonus = (fuel * 18f * multiplier).roundToInt()
        score += fuelBonus + (if (completedPad.isFissure) 1000 * multiplier else 0)
        fuel = (fuel + 26f).coerceAtMost(100f)
        padIndex++
        pads = createPads(padIndex)
        activePad = null
        pickup = createPickup(padIndex)
        shipX = 160f; shipY = 62f; vx = if (padIndex % 2 == 0) 9f else -9f; vy = 0f
        landed = false
        tiltAngle = 0f
        message = "ORE SECURED // DESCENDING TO ${padIndex * 100 + 100}M"
        slagX = 24f + nextFloat() * (WORLD_WIDTH - 48f)
        slagY = -18f
        slagDelay = 2.2f
        currentAudioEvent = DeepMineAudioEvent.STAGE_CLEAR
    }

    private fun crash(reason: String) {
        gameOver = true
        landed = false
        message = reason
        currentAudioEvent = DeepMineAudioEvent.CRASH
    }

    private fun createPads(index: Int): List<DeepMinePad> {
        val primaryWidth = (78f - index * 3f).coerceAtLeast(48f)
        val primaryX = 18f + nextFloat() * (WORLD_WIDTH - primaryWidth - 36f)
        val primary = DeepMinePad(
            id = "primary",
            x = primaryX,
            width = primaryWidth,
            maxOre = 55f + index * 5f,
            ore = 55f + index * 5f,
            purity = 10 + index * 2,
            isFissure = false
        )

        // Starting at depth 1, introduce an optional high-risk/high-reward fissure pad on the opposite side
        if (index > 0) {
            val fissureWidth = (48f - index * 2f).coerceAtLeast(36f)
            val fissureX = if (primaryX > WORLD_WIDTH / 2f) {
                16f + nextFloat() * (primaryX - fissureWidth - 28f).coerceAtLeast(10f)
            } else {
                (primaryX + primaryWidth + 24f) + nextFloat() * (WORLD_WIDTH - primaryX - primaryWidth - fissureWidth - 36f).coerceAtLeast(10f)
            }
            if (fissureX > 10f && fissureX + fissureWidth < WORLD_WIDTH - 10f && abs(fissureX - primaryX) > primaryWidth) {
                val fissure = DeepMinePad(
                    id = "fissure",
                    x = fissureX,
                    width = fissureWidth,
                    maxOre = 35f + index * 4f,
                    ore = 35f + index * 4f,
                    purity = (10 + index * 2) * 2 + 5,
                    isFissure = true
                )
                return listOf(primary, fissure)
            }
        }
        return listOf(primary)
    }

    private fun createPickup(index: Int): DeepMinePickup? {
        if (index == 0) return null
        val startX = 30f + nextFloat() * (WORLD_WIDTH - 60f)
        val startY = 120f + nextFloat() * 80f
        val driftVx = if (nextFloat() > 0.5f) 14f else -14f
        return DeepMinePickup(x = startX, y = startY, vx = driftVx, vy = 2f, type = "fuel", collected = false)
    }

    private fun nextFloat(): Float {
        rng = rng * 6364136223846793005L + 1442695040888963407L
        return ((rng ushr 40) and 0xFFFFFF).toFloat() / 0xFFFFFF.toFloat()
    }

    fun snapshot() = DeepMineSnapshot(
        shipX = shipX,
        shipY = shipY,
        velocityX = vx,
        velocityY = vy,
        tiltAngle = tiltAngle,
        fuel = fuel,
        score = score,
        multiplier = multiplier,
        oreExtracted = oreExtracted,
        pads = pads,
        activePad = activePad,
        pickup = pickup,
        landed = landed,
        gameOver = gameOver,
        message = message,
        elapsedSeconds = elapsed,
        depth = padIndex * 100,
        slagX = slagX,
        slagY = slagY,
        slagWarning = padIndex > 0 && slagDelay <= 0f && slagY < 28f,
        audioEvent = currentAudioEvent
    )
}
