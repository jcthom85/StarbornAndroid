package com.example.starborn.feature.arcade.games.canopyhopper

import kotlin.math.abs
import kotlin.math.roundToInt

enum class CanopyDirection { UP, DOWN, LEFT, RIGHT }

data class CanopyHopperInput(
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
    val hop: Boolean = false
)

enum class CanopyHopperAudioEvent {
    NONE, HOP, NEST_CLAIMED, SPLAT, SPLASH, PICKUP, ROUND_CLEAR, GAME_OVER
}

data class CanopyPlatform(
    val id: Int,
    val lane: Int,
    val x: Float,
    val width: Float,
    val speed: Float,
    val isLilypad: Boolean = false,
    val isEel: Boolean = false,
    val electrified: Boolean = false
)

data class CanopyHazard(
    val id: Int,
    val lane: Int,
    val x: Float,
    val width: Float,
    val speed: Float,
    val type: String = "skimmer"
)

data class CanopyPickup(
    val id: Int,
    val lane: Int,
    val x: Float,
    val type: String = "golden_spore", // "golden_spore" or "firefly"
    val collected: Boolean = false
)

data class CanopyHopperSnapshot(
    val hopperGridX: Int,
    val hopperGridY: Int,
    val hopperPixelX: Float,
    val hopperPixelY: Float,
    val hopProgress: Float,
    val facing: CanopyDirection,
    val lives: Int,
    val score: Int,
    val multiplier: Int,
    val round: Int,
    val nests: List<Boolean>, // 5 nest states
    val platforms: List<CanopyPlatform>,
    val hazards: List<CanopyHazard>,
    val pickups: List<CanopyPickup>,
    val poisonFog: Float, // 0f to 100f
    val gameOver: Boolean,
    val message: String,
    val elapsedSeconds: Float,
    val audioEvent: CanopyHopperAudioEvent = CanopyHopperAudioEvent.NONE
)

class CanopyHopperEngine(private val seed: Long = 0x504F4432L) {
    companion object {
        const val WORLD_WIDTH = 320f
        const val WORLD_HEIGHT = 360f
        const val TILE_WIDTH = 32f
        const val LANE_HEIGHT = 36f
        const val GRID_COLS = 10
        const val GRID_ROWS = 9
        const val NEST_COUNT = 5
        private const val STEP = 1f / 60f
    }

    private var rng = seed
    private var gridX = 4
    private var gridY = 8
    private var prevGridX = 4
    private var prevGridY = 8
    private var pixelX = 4 * TILE_WIDTH + 16f
    private var pixelY = 8 * LANE_HEIGHT + 18f
    private var hopAnim = 1f
    private var facing = CanopyDirection.UP
    private var lives = 3
    private var score = 0
    private var multiplier = 1
    private var round = 1
    private var nests = MutableList(NEST_COUNT) { false }
    private var poisonFog = 0f
    private var gameOver = false
    private var message = "CROSS THE SWAMP TO THE CANOPY"
    private var elapsed = 0f
    private var accumulator = 0f
    private var audioEvent = CanopyHopperAudioEvent.NONE

    private var prevInput = CanopyHopperInput()
    private var platforms = mutableListOf<CanopyPlatform>()
    private var hazards = mutableListOf<CanopyHazard>()
    private var pickups = mutableListOf<CanopyPickup>()

    init {
        reset()
    }

    fun reset() {
        rng = seed
        gridX = 4; gridY = 8; prevGridX = 4; prevGridY = 8
        pixelX = 4 * TILE_WIDTH + 16f; pixelY = 8 * LANE_HEIGHT + 18f
        hopAnim = 1f; facing = CanopyDirection.UP
        lives = 3; score = 0; multiplier = 1; round = 1
        nests = MutableList(NEST_COUNT) { false }
        poisonFog = 0f; gameOver = false
        message = "CROSS THE SWAMP TO THE CANOPY"; elapsed = 0f; accumulator = 0f
        audioEvent = CanopyHopperAudioEvent.NONE
        prevInput = CanopyHopperInput()
        spawnEntities()
    }

    fun advance(deltaSeconds: Float, input: CanopyHopperInput) {
        if (gameOver) return
        audioEvent = CanopyHopperAudioEvent.NONE
        accumulator = (accumulator + deltaSeconds.coerceIn(0f, .1f)).coerceAtMost(.2f)
        while (accumulator >= STEP) {
            update(STEP, input)
            accumulator -= STEP
        }
    }

    private fun update(dt: Float, input: CanopyHopperInput) {
        elapsed += dt
        updateEntities(dt)

        // Poison fog slowly creeping up
        poisonFog = (poisonFog + (2.5f + round * 0.4f) * dt).coerceAtMost(100f)
        if (poisonFog >= 100f) {
            killHopper("POISON FOG ENGULFED THE SWAMP", isSplash = false)
            return
        }

        // Handle Player Input (Edge-Triggered)
        val edgeUp = input.up && !prevInput.up
        val edgeDown = input.down && !prevInput.down
        val edgeLeft = input.left && !prevInput.left
        val edgeRight = input.right && !prevInput.right
        val edgeHop = input.hop && !prevInput.hop
        prevInput = input

        when {
            edgeUp || edgeHop -> moveHopper(0, -1, CanopyDirection.UP)
            edgeDown -> moveHopper(0, 1, CanopyDirection.DOWN)
            edgeLeft -> moveHopper(-1, 0, CanopyDirection.LEFT)
            edgeRight -> moveHopper(1, 0, CanopyDirection.RIGHT)
        }

        // Smooth Hop Animation Interpolation
        if (hopAnim < 1f) {
            hopAnim = (hopAnim + 10f * dt).coerceAtMost(1f)
            val targetX = gridX * TILE_WIDTH + 16f
            val targetY = gridY * LANE_HEIGHT + 18f
            val startX = prevGridX * TILE_WIDTH + 16f
            val startY = prevGridY * LANE_HEIGHT + 18f
            pixelX = startX + (targetX - startX) * hopAnim
            pixelY = startY + (targetY - startY) * hopAnim
        } else {
            pixelX = gridX * TILE_WIDTH + 16f
            pixelY = gridY * LANE_HEIGHT + 18f
        }

        // Check River Riding / Water Collision (Lanes 1, 2, 3)
        if (gridY in 1..3 && hopAnim >= 0.8f) {
            val standingPlatform = platforms.firstOrNull { plat ->
                plat.lane == gridY && pixelX >= plat.x - 8f && pixelX <= plat.x + plat.width + 8f
            }

            if (standingPlatform == null) {
                killHopper("LOST IN THE SWAMP RAPIDS", isSplash = true)
                return
            } else if (standingPlatform.isEel && standingPlatform.electrified) {
                killHopper("ELECTROCUTED BY VOLT EEL", isSplash = true)
                return
            } else {
                // Drift with platform
                pixelX += standingPlatform.speed * dt
                gridX = ((pixelX - 16f) / TILE_WIDTH).roundToInt().coerceIn(0, GRID_COLS - 1)
                if (pixelX < 4f || pixelX > WORLD_WIDTH - 4f) {
                    killHopper("SWEPT OFF-SCREEN BY CURRENT", isSplash = true)
                    return
                }
            }
        }

        // Check Road / Thicket Hazard Collision (Lanes 5, 6, 7)
        if (gridY in 5..7) {
            val hitHazard = hazards.firstOrNull { h ->
                h.lane == gridY && abs(pixelX - (h.x + h.width / 2f)) < (h.width / 2f + 8f)
            }
            if (hitHazard != null) {
                killHopper("CRUSHED IN PREDATOR THICKET", isSplash = false)
                return
            }
        }

        // Check Pickups
        val pickup = pickups.firstOrNull { !it.collected && it.lane == gridY && abs(pixelX - it.x) < 16f }
        if (pickup != null) {
            pickups = pickups.map { if (it.id == pickup.id) it.copy(collected = true) else it }.toMutableList()
            if (pickup.type == "golden_spore") {
                score += 1000 * multiplier
                poisonFog = (poisonFog - 20f).coerceAtLeast(0f)
                message = "GOLDEN SPORE DIGESTED // +1,000 PTS"
                audioEvent = CanopyHopperAudioEvent.PICKUP
            } else {
                score += 500 * multiplier
                poisonFog = (poisonFog - 35f).coerceAtLeast(0f)
                message = "GLOW FIREFLY SNAGGED // FOG CLEARED"
                audioEvent = CanopyHopperAudioEvent.PICKUP
            }
        }

        // Check Canopy Nest Arrival (Lane 0)
        if (gridY == 0) {
            val nestIndex = (gridX / 2).coerceIn(0, NEST_COUNT - 1)
            val nestTargetCol = nestIndex * 2 + 1
            val alignedWithNest = abs(gridX - nestTargetCol) <= 0

            if (alignedWithNest && !nests[nestIndex]) {
                // Claim Nest!
                nests[nestIndex] = true
                val nestBonus = 1000 * multiplier
                val timeBonus = ((100f - poisonFog) * 15f).roundToInt()
                score += nestBonus + timeBonus
                multiplier = (multiplier + 1).coerceAtMost(5)
                poisonFog = 0f
                message = "CANOPY NEST CLAIMED // ${multiplier}X"
                audioEvent = CanopyHopperAudioEvent.NEST_CLAIMED

                if (nests.all { it }) {
                    completeRound()
                } else {
                    respawnAtBottom()
                }
            } else if (alignedWithNest && nests[nestIndex]) {
                // Already occupied nest: bounce back down to lane 1
                gridY = 1
                prevGridY = 1
                message = "NEST ALREADY OCCUPIED"
            } else {
                // Hit the thorny canopy barrier between nests
                killHopper("IMPALED ON CANOPY BRAMBLES", isSplash = false)
            }
        }
    }

    private fun moveHopper(dx: Int, dy: Int, dir: CanopyDirection) {
        val newX = (gridX + dx).coerceIn(0, GRID_COLS - 1)
        val newY = (gridY + dy).coerceIn(0, GRID_ROWS - 1)
        if (newX != gridX || newY != gridY) {
            prevGridX = gridX
            prevGridY = gridY
            gridX = newX
            gridY = newY
            facing = dir
            hopAnim = 0f
            if (dy < 0) {
                score += 10 * multiplier
            }
            audioEvent = CanopyHopperAudioEvent.HOP
        }
    }

    private fun killHopper(reason: String, isSplash: Boolean) {
        lives--
        multiplier = 1
        message = reason
        audioEvent = if (isSplash) CanopyHopperAudioEvent.SPLASH else CanopyHopperAudioEvent.SPLAT
        if (lives <= 0) {
            gameOver = true
            message = "GAME OVER // $reason"
            audioEvent = CanopyHopperAudioEvent.GAME_OVER
        } else {
            respawnAtBottom()
        }
    }

    private fun respawnAtBottom() {
        gridX = 4
        gridY = 8
        prevGridX = 4
        prevGridY = 8
        pixelX = 4 * TILE_WIDTH + 16f
        pixelY = 8 * LANE_HEIGHT + 18f
        hopAnim = 1f
        facing = CanopyDirection.UP
    }

    private fun completeRound() {
        round++
        score += 2500 * multiplier
        nests = MutableList(NEST_COUNT) { false }
        poisonFog = 0f
        message = "SWAMP MASTERED // ADVANCING TO TIER $round"
        audioEvent = CanopyHopperAudioEvent.ROUND_CLEAR
        respawnAtBottom()
        spawnEntities()
    }

    private fun updateEntities(dt: Float) {
        val speedScale = 1f + (round - 1) * 0.15f

        // Update River Platforms
        platforms = platforms.map { plat ->
            var newX = plat.x + plat.speed * speedScale * dt
            if (plat.speed > 0 && newX > WORLD_WIDTH + 20f) {
                newX = -plat.width - 20f
            } else if (plat.speed < 0 && newX < -plat.width - 20f) {
                newX = WORLD_WIDTH + 20f
            }
            val eelShock = if (plat.isEel) ((elapsed * 2.5f + plat.id).toInt() % 4 == 0) else false
            plat.copy(x = newX, electrified = eelShock)
        }.toMutableList()

        // Update Road Hazards
        hazards = hazards.map { h ->
            var newX = h.x + h.speed * speedScale * dt
            if (h.speed > 0 && newX > WORLD_WIDTH + 20f) {
                newX = -h.width - 20f
            } else if (h.speed < 0 && newX < -h.width - 20f) {
                newX = WORLD_WIDTH + 20f
            }
            h.copy(x = newX)
        }.toMutableList()
    }

    private fun spawnEntities() {
        platforms.clear()
        hazards.clear()
        pickups.clear()

        // River Lane 1 (Moving Right - Lilypads)
        platforms.add(CanopyPlatform(1, 1, 10f, 42f, 38f, isLilypad = true))
        platforms.add(CanopyPlatform(2, 1, 120f, 42f, 38f, isLilypad = true))
        platforms.add(CanopyPlatform(3, 1, 230f, 42f, 38f, isLilypad = true))

        // River Lane 2 (Moving Left - Medium Logs)
        platforms.add(CanopyPlatform(4, 2, 20f, 68f, -46f))
        platforms.add(CanopyPlatform(5, 2, 160f, 68f, -46f))
        platforms.add(CanopyPlatform(6, 2, 290f, 68f, -46f))

        // River Lane 3 (Moving Right - Moss Logs & Volt Eels)
        platforms.add(CanopyPlatform(7, 3, 0f, 85f, 32f))
        platforms.add(CanopyPlatform(8, 3, 140f, 50f, 32f, isEel = true))
        platforms.add(CanopyPlatform(9, 3, 240f, 85f, 32f))

        // Road Lane 5 (Moving Left - Hover Skimmers)
        hazards.add(CanopyHazard(10, 5, 40f, 34f, -55f, "skimmer"))
        hazards.add(CanopyHazard(11, 5, 180f, 34f, -55f, "skimmer"))

        // Road Lane 6 (Moving Right - Razor Centipedes)
        hazards.add(CanopyHazard(12, 6, 20f, 44f, 42f, "centipede"))
        hazards.add(CanopyHazard(13, 6, 170f, 44f, 42f, "centipede"))

        // Road Lane 7 (Moving Left - Swamp Prowlers)
        hazards.add(CanopyHazard(14, 7, 60f, 52f, -32f, "prowler"))
        hazards.add(CanopyHazard(15, 7, 220f, 52f, -32f, "prowler"))

        // Pickups on Island Lane 4 and River
        pickups.add(CanopyPickup(101, 4, 80f, "golden_spore"))
        pickups.add(CanopyPickup(102, 4, 240f, "firefly"))
        pickups.add(CanopyPickup(103, 2, 160f, "golden_spore"))
    }

    fun snapshot() = CanopyHopperSnapshot(
        hopperGridX = gridX,
        hopperGridY = gridY,
        hopperPixelX = pixelX,
        hopperPixelY = pixelY,
        hopProgress = hopAnim,
        facing = facing,
        lives = lives,
        score = score,
        multiplier = multiplier,
        round = round,
        nests = nests.toList(),
        platforms = platforms.toList(),
        hazards = hazards.toList(),
        pickups = pickups.toList(),
        poisonFog = poisonFog,
        gameOver = gameOver,
        message = message,
        elapsedSeconds = elapsed,
        audioEvent = audioEvent
    )
}
