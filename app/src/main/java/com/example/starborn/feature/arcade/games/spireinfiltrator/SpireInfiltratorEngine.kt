package com.example.starborn.feature.arcade.games.spireinfiltrator

import kotlin.math.abs
import kotlin.math.roundToInt

enum class InfiltratorDir(val dx: Int, val dy: Int) {
    NONE(0, 0), UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0)
}

enum class SentinelState { HUNTING, VULNERABLE, DEREFERENCED }

data class InfiltratorInput(
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
    val boost: Boolean = false
)

enum class SpireAudioEvent {
    NONE, EAT_NODE, OVERCLOCK_START, EAT_SENTINEL, DEATH, ROUND_CLEAR, GAME_OVER
}

data class Sentinel(
    val id: Int,
    val name: String, // Red, Cyan, Gold, Violet
    var gridX: Int,
    var gridY: Int,
    var pixelX: Float,
    var pixelY: Float,
    var dir: InfiltratorDir = InfiltratorDir.UP,
    var state: SentinelState = SentinelState.HUNTING,
    var respawnTimer: Float = 0f
)

data class SpireInfiltratorSnapshot(
    val playerGridX: Int,
    val playerGridY: Int,
    val playerPixelX: Float,
    val playerPixelY: Float,
    val playerDir: InfiltratorDir,
    val lives: Int,
    val score: Int,
    val multiplier: Int,
    val round: Int,
    val nodesRemaining: Int,
    val overclockSeconds: Float,
    val sentinels: List<Sentinel>,
    val grid: List<CharArray>,
    val gameOver: Boolean,
    val message: String,
    val elapsedSeconds: Float,
    val audioEvent: SpireAudioEvent = SpireAudioEvent.NONE
)

class SpireInfiltratorEngine(private val seed: Long = 0x53504952L) {
    companion object {
        const val COLS = 15
        const val ROWS = 15
        const val TILE_SIZE = 20f
        const val OFFSET_X = 10f
        const val OFFSET_Y = 30f
        private const val STEP = 1f / 60f

        // 15x15 Maze Template: '#' = Wall, '.' = Data Node, 'O' = Overclock Node, ' ' = Empty
        val BASE_MAZE = listOf(
            "###############",
            "#O.....#.....O#",
            "#.###.###.###.#",
            "#.#.........#.#",
            "#.###.# #.###.#",
            "#.....# #.....#",
            "###.### ###.###",
            "  #.#     #.#  ",
            "###.#######.###",
            "#.....# #.....#",
            "#.###.# #.###.#",
            "#.#.........#.#",
            "#.###.###.###.#",
            "#O.....#.....O#",
            "###############"
        )
    }

    private var rng = seed
    private var grid = BASE_MAZE.map { it.toCharArray() }
    private var playerGridX = 7
    private var playerGridY = 11
    private var playerPixelX = 7 * TILE_SIZE + OFFSET_X + 10f
    private var playerPixelY = 11 * TILE_SIZE + OFFSET_Y + 10f
    private var playerDir = InfiltratorDir.NONE
    private var nextDir = InfiltratorDir.NONE

    private var lives = 3
    private var score = 0
    private var multiplier = 1
    private var round = 1
    private var nodesRemaining = 0
    private var overclockSeconds = 0f
    private var sentinelCombo = 0

    private var gameOver = false
    private var message = "INFILTRATE THE SPIRE MAINFRAME"
    private var elapsed = 0f
    private var accumulator = 0f
    private var currentAudioEvent = SpireAudioEvent.NONE

    private val sentinels = mutableListOf<Sentinel>()

    init {
        reset()
    }

    fun reset() {
        rng = seed
        lives = 3
        score = 0
        multiplier = 1
        round = 1
        gameOver = false
        message = "INFILTRATE THE SPIRE MAINFRAME"
        elapsed = 0f
        accumulator = 0f
        currentAudioEvent = SpireAudioEvent.NONE
        initMazeAndEntities()
    }

    private fun initMazeAndEntities() {
        grid = BASE_MAZE.map { it.toCharArray() }
        nodesRemaining = 0
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                if (grid[r][c] == '.' || grid[r][c] == 'O') {
                    nodesRemaining++
                }
            }
        }
        resetPlayerAndSentinels()
    }

    private fun resetPlayerAndSentinels() {
        playerGridX = 7
        playerGridY = 11
        playerPixelX = playerGridX * TILE_SIZE + OFFSET_X + 10f
        playerPixelY = playerGridY * TILE_SIZE + OFFSET_Y + 10f
        playerDir = InfiltratorDir.NONE
        nextDir = InfiltratorDir.NONE
        overclockSeconds = 0f
        sentinelCombo = 0

        sentinels.clear()
        sentinels.add(Sentinel(1, "Red", 7, 6, 7 * TILE_SIZE + OFFSET_X + 10f, 6 * TILE_SIZE + OFFSET_Y + 10f, InfiltratorDir.UP))
        sentinels.add(Sentinel(2, "Cyan", 6, 7, 6 * TILE_SIZE + OFFSET_X + 10f, 7 * TILE_SIZE + OFFSET_Y + 10f, InfiltratorDir.LEFT))
        sentinels.add(Sentinel(3, "Gold", 8, 7, 8 * TILE_SIZE + OFFSET_X + 10f, 7 * TILE_SIZE + OFFSET_Y + 10f, InfiltratorDir.RIGHT))
        sentinels.add(Sentinel(4, "Violet", 7, 7, 7 * TILE_SIZE + OFFSET_X + 10f, 7 * TILE_SIZE + OFFSET_Y + 10f, InfiltratorDir.UP))
    }

    fun advance(deltaSeconds: Float, input: InfiltratorInput) {
        if (gameOver) return
        currentAudioEvent = SpireAudioEvent.NONE
        accumulator = (accumulator + deltaSeconds.coerceIn(0f, .1f)).coerceAtMost(.2f)
        while (accumulator >= STEP) {
            update(STEP, input)
            accumulator -= STEP
        }
    }

    private fun update(dt: Float, input: InfiltratorInput) {
        elapsed += dt

        if (overclockSeconds > 0f) {
            overclockSeconds = (overclockSeconds - dt).coerceAtLeast(0f)
            if (overclockSeconds <= 0f) {
                sentinels.forEach { if (it.state == SentinelState.VULNERABLE) it.state = SentinelState.HUNTING }
                sentinelCombo = 0
            }
        }

        // Buffer Desired Turn Input
        when {
            input.up -> nextDir = InfiltratorDir.UP
            input.down -> nextDir = InfiltratorDir.DOWN
            input.left -> nextDir = InfiltratorDir.LEFT
            input.right -> nextDir = InfiltratorDir.RIGHT
        }

        // Move Player
        val playerSpeed = (70f + (round - 1) * 4f) * (if (input.boost) 1.25f else 1f)
        movePlayer(playerSpeed * dt)

        // Move Sentinels
        val sentinelBaseSpeed = if (overclockSeconds > 0f) 38f else (52f + (round - 1) * 5f)
        sentinels.forEach { s ->
            if (s.state == SentinelState.DEREFERENCED) {
                s.respawnTimer -= dt
                if (s.respawnTimer <= 0f) {
                    s.state = SentinelState.HUNTING
                    s.gridX = 7
                    s.gridY = 7
                    s.pixelX = 7 * TILE_SIZE + OFFSET_X + 10f
                    s.pixelY = 7 * TILE_SIZE + OFFSET_Y + 10f
                }
            } else {
                moveSentinel(s, sentinelBaseSpeed * dt)
            }
        }

        // Check Collisions
        checkSentinelCollisions()
    }

    private fun movePlayer(distance: Float) {
        val targetTileCenterX = playerGridX * TILE_SIZE + OFFSET_X + 10f
        val targetTileCenterY = playerGridY * TILE_SIZE + OFFSET_Y + 10f

        // Check if nextDir turn is valid at or near junction center
        if (nextDir != InfiltratorDir.NONE && nextDir != playerDir) {
            val canTurn = canMove(playerGridX + nextDir.dx, playerGridY + nextDir.dy)
            val closeToCenter = abs(playerPixelX - targetTileCenterX) < 4f && abs(playerPixelY - targetTileCenterY) < 4f
            if (canTurn && closeToCenter) {
                playerDir = nextDir
                playerPixelX = targetTileCenterX
                playerPixelY = targetTileCenterY
            }
        }

        if (playerDir != InfiltratorDir.NONE) {
            val nextX = playerGridX + playerDir.dx
            val nextY = playerGridY + playerDir.dy

            if (canMove(nextX, nextY)) {
                playerPixelX += playerDir.dx * distance
                playerPixelY += playerDir.dy * distance

                // Update Grid Position
                playerGridX = ((playerPixelX - OFFSET_X - 10f) / TILE_SIZE).roundToInt().coerceIn(0, COLS - 1)
                playerGridY = ((playerPixelY - OFFSET_Y - 10f) / TILE_SIZE).roundToInt().coerceIn(0, ROWS - 1)

                // Eat Nodes
                val cell = grid[playerGridY][playerGridX]
                if (cell == '.') {
                    grid[playerGridY][playerGridX] = ' '
                    nodesRemaining--
                    score += 100 * multiplier
                    currentAudioEvent = SpireAudioEvent.EAT_NODE
                    if (nodesRemaining <= 0) {
                        completeSector()
                    }
                } else if (cell == 'O') {
                    grid[playerGridY][playerGridX] = ' '
                    nodesRemaining--
                    score += 500 * multiplier
                    overclockSeconds = 8.0f
                    sentinelCombo = 0
                    sentinels.forEach { if (it.state != SentinelState.DEREFERENCED) it.state = SentinelState.VULNERABLE }
                    message = "OVERCLOCK OVERDRIVE // SENTINELS VULNERABLE"
                    currentAudioEvent = SpireAudioEvent.OVERCLOCK_START
                    if (nodesRemaining <= 0) {
                        completeSector()
                    }
                }
            } else {
                // Stopped at wall
                playerPixelX = targetTileCenterX
                playerPixelY = targetTileCenterY
            }
        }
    }

    private fun moveSentinel(s: Sentinel, distance: Float) {
        val centerX = s.gridX * TILE_SIZE + OFFSET_X + 10f
        val centerY = s.gridY * TILE_SIZE + OFFSET_Y + 10f

        val closeToCenter = abs(s.pixelX - centerX) < 3f && abs(s.pixelY - centerY) < 3f
        if (closeToCenter) {
            s.pixelX = centerX
            s.pixelY = centerY

            // Pick AI Direction
            val validDirs = listOf(InfiltratorDir.UP, InfiltratorDir.DOWN, InfiltratorDir.LEFT, InfiltratorDir.RIGHT)
                .filter { d -> canMove(s.gridX + d.dx, s.gridY + d.dy) && d != oppositeDir(s.dir) }
                .ifEmpty {
                    listOf(InfiltratorDir.UP, InfiltratorDir.DOWN, InfiltratorDir.LEFT, InfiltratorDir.RIGHT)
                        .filter { d -> canMove(s.gridX + d.dx, s.gridY + d.dy) }
                }

            if (validDirs.isNotEmpty()) {
                s.dir = when (s.name) {
                    "Red" -> {
                        // Direct Chase to player
                        validDirs.minByOrNull { d ->
                            val nx = s.gridX + d.dx
                            val ny = s.gridY + d.dy
                            abs(nx - playerGridX) + abs(ny - playerGridY)
                        } ?: validDirs.random(randomSource())
                    }
                    "Cyan" -> {
                        // Flanker: target 2 tiles ahead of player
                        val targetX = playerGridX + playerDir.dx * 2
                        val targetY = playerGridY + playerDir.dy * 2
                        validDirs.minByOrNull { d ->
                            val nx = s.gridX + d.dx
                            val ny = s.gridY + d.dy
                            abs(nx - targetX) + abs(ny - targetY)
                        } ?: validDirs.random(randomSource())
                    }
                    else -> validDirs.random(randomSource())
                }
            }
        }

        s.pixelX += s.dir.dx * distance
        s.pixelY += s.dir.dy * distance
        s.gridX = ((s.pixelX - OFFSET_X - 10f) / TILE_SIZE).roundToInt().coerceIn(0, COLS - 1)
        s.gridY = ((s.pixelY - OFFSET_Y - 10f) / TILE_SIZE).roundToInt().coerceIn(0, ROWS - 1)
    }

    private fun checkSentinelCollisions() {
        sentinels.forEach { s ->
            if (s.state != SentinelState.DEREFERENCED) {
                val dist = abs(playerPixelX - s.pixelX) + abs(playerPixelY - s.pixelY)
                if (dist < 14f) {
                    if (s.state == SentinelState.VULNERABLE) {
                        // Dereference Sentinel!
                        s.state = SentinelState.DEREFERENCED
                        s.respawnTimer = 6f
                        sentinelCombo++
                        val comboScore = 1000 * sentinelCombo * multiplier
                        score += comboScore
                        message = "ICE ${s.name.uppercase()} DEREFERENCED // +$comboScore PTS"
                        currentAudioEvent = SpireAudioEvent.EAT_SENTINEL
                    } else {
                        // Player caught by Sentinel
                        killPlayer("CAUGHT BY ICE ${s.name.uppercase()}")
                        return
                    }
                }
            }
        }
    }

    private fun killPlayer(reason: String) {
        lives--
        multiplier = 1
        message = reason
        currentAudioEvent = SpireAudioEvent.DEATH
        if (lives <= 0) {
            gameOver = true
            message = "SYSTEM PURGE // $reason"
            currentAudioEvent = SpireAudioEvent.GAME_OVER
        } else {
            resetPlayerAndSentinels()
        }
    }

    private fun completeSector() {
        round++
        score += 2500 * multiplier
        multiplier = (multiplier + 1).coerceAtMost(5)
        message = "SECTOR CLEARED // ACCESSING LAYER $round"
        currentAudioEvent = SpireAudioEvent.ROUND_CLEAR
        initMazeAndEntities()
    }

    private fun canMove(c: Int, r: Int): Boolean {
        if (c !in 0 until COLS || r !in 0 until ROWS) return false
        return grid[r][c] != '#'
    }

    private fun oppositeDir(d: InfiltratorDir): InfiltratorDir = when (d) {
        InfiltratorDir.UP -> InfiltratorDir.DOWN
        InfiltratorDir.DOWN -> InfiltratorDir.UP
        InfiltratorDir.LEFT -> InfiltratorDir.RIGHT
        InfiltratorDir.RIGHT -> InfiltratorDir.LEFT
        InfiltratorDir.NONE -> InfiltratorDir.NONE
    }

    private fun randomSource(): kotlin.random.Random {
        rng = rng * 6364136223846793005L + 1442695040888963407L
        return kotlin.random.Random(rng)
    }

    fun snapshot() = SpireInfiltratorSnapshot(
        playerGridX = playerGridX,
        playerGridY = playerGridY,
        playerPixelX = playerPixelX,
        playerPixelY = playerPixelY,
        playerDir = playerDir,
        lives = lives,
        score = score,
        multiplier = multiplier,
        round = round,
        nodesRemaining = nodesRemaining,
        overclockSeconds = overclockSeconds,
        sentinels = sentinels.map { it.copy() },
        grid = grid.map { it.clone() },
        gameOver = gameOver,
        message = message,
        elapsedSeconds = elapsed,
        audioEvent = currentAudioEvent
    )
}
