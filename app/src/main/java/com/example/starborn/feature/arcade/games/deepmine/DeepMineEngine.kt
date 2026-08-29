package com.example.starborn.feature.arcade.games.deepmine

import kotlin.math.abs
import kotlin.math.roundToInt

data class DeepMineInput(
    val left: Boolean = false,
    val right: Boolean = false,
    val boost: Boolean = false,
    val drill: Boolean = false
)

data class DeepMinePad(val x: Float, val width: Float, val ore: Float, val purity: Int)

data class DeepMineSnapshot(
    val shipX: Float,
    val shipY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val fuel: Float,
    val score: Int,
    val multiplier: Int,
    val oreExtracted: Float,
    val pad: DeepMinePad,
    val landed: Boolean,
    val gameOver: Boolean,
    val message: String,
    val elapsedSeconds: Float,
    val depth: Int,
    val slagX: Float,
    val slagY: Float,
    val slagWarning: Boolean
)

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
    private var fuel = 100f
    private var score = 0
    private var multiplier = 1
    private var oreExtracted = 0f
    private var padIndex = 0
    private var pad = createPad(0)
    private var landed = false
    private var gameOver = false
    private var message = "EASE ONTO THE ORE PAD"
    private var elapsed = 0f
    private var accumulator = 0f
    private var slagX = 72f
    private var slagY = -40f
    private var slagDelay = 4f

    fun reset() {
        rng = seed
        shipX = 160f; shipY = 54f; vx = 0f; vy = 0f; fuel = 100f
        score = 0; multiplier = 1; oreExtracted = 0f; padIndex = 0
        pad = createPad(0); landed = false; gameOver = false
        message = "EASE ONTO THE ORE PAD"; elapsed = 0f; accumulator = 0f
        slagX = 72f; slagY = -40f; slagDelay = 4f
    }

    fun advance(deltaSeconds: Float, input: DeepMineInput) {
        if (gameOver) return
        accumulator = (accumulator + deltaSeconds.coerceIn(0f, .1f)).coerceAtMost(.2f)
        while (accumulator >= STEP) {
            update(STEP, input)
            accumulator -= STEP
        }
    }

    private fun update(dt: Float, input: DeepMineInput) {
        elapsed += dt
        updateSlag(dt)
        if (gameOver) return
        val onPad = landed && shipX >= pad.x && shipX <= pad.x + pad.width
        if (onPad && input.drill && pad.ore > 0f && fuel > 0f) {
            val mined = minOf(pad.ore, 13f * dt)
            pad = pad.copy(ore = pad.ore - mined)
            oreExtracted += mined
            fuel = (fuel - 1.6f * dt).coerceAtLeast(0f)
            score += (mined * pad.purity * multiplier).roundToInt()
            message = "DRILLING // PURITY ${pad.purity}"
            if (pad.ore <= .01f) completePad()
            return
        }

        if (landed && !input.boost && !input.left && !input.right) {
            message = if (pad.ore > 0f) "HOLD DRILL TO EXTRACT" else message
            return
        }
        if (landed && (input.boost || input.left || input.right)) {
            landed = false
            vy = -10f
        }

        vy += 20f * dt
        if (input.left && fuel > 0f) { vx -= 31f * dt; fuel -= 2.5f * dt }
        if (input.right && fuel > 0f) { vx += 31f * dt; fuel -= 2.5f * dt }
        if (input.boost && fuel > 0f) { vy -= 46f * dt; fuel -= 5.5f * dt }
        vx = vx.coerceIn(-52f, 52f)
        vy = vy.coerceIn(-62f, 72f)
        shipX += vx * dt
        shipY += vy * dt
        fuel = fuel.coerceAtLeast(0f)
        if (shipX < SHIP_HALF_WIDTH || shipX > WORLD_WIDTH - SHIP_HALF_WIDTH || shipY < SHIP_HALF_HEIGHT) {
            crash("PROBE LOST IN THE SHAFT")
            return
        }
        if (shipY + SHIP_HALF_HEIGHT >= GROUND_Y) classifyLanding()
        if (fuel <= 0f && !landed) crash("FUEL EXHAUSTED")
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
        val centered = shipX >= pad.x + SHIP_HALF_WIDTH && shipX <= pad.x + pad.width - SHIP_HALF_WIDTH
        if (!centered || abs(vx) > 22f || vy > 34f) {
            crash(if (!centered) "MISSED THE ORE PAD" else "HULL FAILURE // HARD IMPACT")
            return
        }
        landed = true
        shipY = GROUND_Y - SHIP_HALF_HEIGHT
        val clean = abs(vx) <= 10f && vy <= 20f
        vx = 0f; vy = 0f
        if (clean) {
            multiplier = (multiplier + 1).coerceAtMost(5)
            score += 350 * multiplier
            message = "CLEAN LANDING // ${multiplier}X"
        } else {
            multiplier = 1
            score += 100
            message = "LANDED // STREAK RESET"
        }
    }

    private fun completePad() {
        score += (fuel * 18f * multiplier).roundToInt()
        fuel = (fuel + 24f).coerceAtMost(100f)
        padIndex++
        pad = createPad(padIndex)
        shipX = 160f; shipY = 62f; vx = if (padIndex % 2 == 0) 9f else -9f; vy = 0f
        landed = false
        message = "ORE SECURED // DESCENDING ${padIndex + 1}"
        slagX = 24f + nextFloat() * (WORLD_WIDTH - 48f)
        slagY = -18f
        slagDelay = 2.2f
    }

    private fun crash(reason: String) {
        gameOver = true
        landed = false
        message = reason
    }

    private fun createPad(index: Int): DeepMinePad {
        val random = nextFloat()
        val width = (78f - index * 3f).coerceAtLeast(46f)
        val x = 18f + random * (WORLD_WIDTH - width - 36f)
        return DeepMinePad(x, width, 55f + index * 5f, 10 + index * 2)
    }

    private fun nextFloat(): Float {
        rng = rng * 6364136223846793005L + 1442695040888963407L
        return ((rng ushr 40) and 0xFFFFFF).toFloat() / 0xFFFFFF.toFloat()
    }

    fun snapshot() = DeepMineSnapshot(
        shipX, shipY, vx, vy, fuel, score, multiplier, oreExtracted, pad,
        landed, gameOver, message, elapsed, padIndex * 100,
        slagX, slagY, padIndex > 0 && slagDelay <= 0f && slagY < 28f
    )
}
