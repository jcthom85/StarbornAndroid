package com.example.starborn.feature.arcade.games.orbitaldefense

import kotlin.math.abs
import kotlin.math.sin

enum class EnemyType(val points: Int, val divePoints: Int, val maxHp: Int) {
    FLAGSHIP(300, 600, 2),
    CRUISER(150, 300, 1),
    SWARM(80, 160, 1),
    MYSTERY(1500, 1500, 3)
}

enum class EnemyFlightState { GRID, DIVING, RETURNING, MYSTERY_FLYBY }

data class EnemyShip(
    val id: Int,
    val type: EnemyType,
    val gridCol: Int,
    val gridRow: Int,
    var x: Float, // 0f to 1f normalized
    var y: Float, // 0f to 1f normalized
    var hp: Int,
    var flightState: EnemyFlightState = EnemyFlightState.GRID,
    var diveProgress: Float = 0f,
    var diveStartX: Float = 0f,
    var diveStartY: Float = 0f,
    var alive: Boolean = true
)

data class Projectile(
    val id: Int,
    var x: Float,
    var y: Float,
    val vy: Float,
    val isPlayer: Boolean,
    var active: Boolean = true
)

data class OrbitalDefenseInput(
    val shipTargetX: Float? = null,
    val moveLeft: Boolean = false,
    val moveRight: Boolean = false,
    val fire: Boolean = false,
    val triggerEmp: Boolean = false
)

enum class OrbitalAudioEvent {
    NONE, PLAYER_FIRE, ENEMY_EXPLODE, MYSTERY_SPAWN, EMP_BLAST, PLAYER_HIT, ROUND_CLEAR, GAME_OVER
}

data class OrbitalDefenseSnapshot(
    val playerX: Float, // 0f to 1f
    val lives: Int,
    val score: Int,
    val multiplier: Int,
    val empBombs: Int,
    val round: Int,
    val enemies: List<EnemyShip>,
    val projectiles: List<Projectile>,
    val empActiveSeconds: Float,
    val gameOver: Boolean,
    val message: String,
    val elapsedSeconds: Float,
    val audioEvent: OrbitalAudioEvent = OrbitalAudioEvent.NONE
)

class OrbitalDefenseEngine(private val seed: Long = 0x5A454E49L) {
    companion object {
        const val STEP = 1f / 60f
        const val PLAYER_Y = 0.90f
        const val PLAYER_WIDTH = 0.08f
        const val GRID_COLS = 6
        const val GRID_ROWS = 3
    }

    private var rng = seed
    private var playerX = 0.5f
    private var lives = 3
    private var score = 0
    private var multiplier = 1
    private var streak = 0
    private var empBombs = 1
    private var round = 1
    private var fireCooldown = 0f
    private var empActiveSeconds = 0f
    private var diveCooldown = 2.5f
    private var mysteryCooldown = 12f
    private var nextEntityId = 1

    private var gameOver = false
    private var message = "ORBITAL DEFENSE // SECTOR 1 ACTIVE"
    private var elapsed = 0f
    private var accumulator = 0f
    private var currentAudioEvent = OrbitalAudioEvent.NONE

    private val enemies = mutableListOf<EnemyShip>()
    private val projectiles = mutableListOf<Projectile>()

    init {
        reset()
    }

    fun reset() {
        rng = seed
        playerX = 0.5f
        lives = 3
        score = 0
        multiplier = 1
        streak = 0
        empBombs = 1
        round = 1
        fireCooldown = 0f
        empActiveSeconds = 0f
        diveCooldown = 2.5f
        mysteryCooldown = 12f
        nextEntityId = 1
        gameOver = false
        message = "ORBITAL DEFENSE // SECTOR 1 ACTIVE"
        elapsed = 0f
        accumulator = 0f
        currentAudioEvent = OrbitalAudioEvent.NONE
        enemies.clear()
        projectiles.clear()
        spawnWave()
    }

    private fun spawnWave() {
        enemies.clear()
        for (r in 0 until GRID_ROWS) {
            val type = when (r) {
                0 -> EnemyType.FLAGSHIP
                1 -> EnemyType.CRUISER
                else -> EnemyType.SWARM
            }
            for (c in 0 until GRID_COLS) {
                val gridX = 0.18f + c * 0.13f
                val gridY = 0.12f + r * 0.09f
                enemies.add(
                    EnemyShip(
                        id = nextEntityId++,
                        type = type,
                        gridCol = c,
                        gridRow = r,
                        x = gridX,
                        y = gridY,
                        hp = type.maxHp,
                        flightState = EnemyFlightState.GRID
                    )
                )
            }
        }
    }

    fun advance(deltaSeconds: Float, input: OrbitalDefenseInput) {
        if (gameOver) return
        currentAudioEvent = OrbitalAudioEvent.NONE
        accumulator = (accumulator + deltaSeconds.coerceIn(0f, .1f)).coerceAtMost(.2f)
        while (accumulator >= STEP) {
            update(STEP, input)
            accumulator -= STEP
        }
    }

    private fun update(dt: Float, input: OrbitalDefenseInput) {
        elapsed += dt

        // 1. Player Movement
        val moveSpeed = 1.2f
        if (input.shipTargetX != null) {
            val diff = input.shipTargetX - playerX
            playerX += (diff * 14f * dt).coerceIn(-moveSpeed * dt, moveSpeed * dt)
        } else {
            if (input.moveLeft) playerX -= moveSpeed * dt
            if (input.moveRight) playerX += moveSpeed * dt
        }
        playerX = playerX.coerceIn(0.06f, 0.94f)

        // 2. EMP Trigger
        if (input.triggerEmp && empBombs > 0 && empActiveSeconds <= 0f) {
            empBombs--
            empActiveSeconds = 3.0f
            // Clear enemy bullets
            projectiles.removeAll { !it.isPlayer }
            message = "EMP DETONATION // BULLETS CLEARED"
            currentAudioEvent = OrbitalAudioEvent.EMP_BLAST
        }

        if (empActiveSeconds > 0f) {
            empActiveSeconds = (empActiveSeconds - dt).coerceAtLeast(0f)
        }

        // 3. Player Firing
        fireCooldown -= dt
        if (input.fire && fireCooldown <= 0f) {
            fireCooldown = 0.18f
            // Twin plasma cannons
            projectiles.add(Projectile(nextEntityId++, playerX - 0.025f, PLAYER_Y - 0.03f, -1.2f, isPlayer = true))
            projectiles.add(Projectile(nextEntityId++, playerX + 0.025f, PLAYER_Y - 0.03f, -1.2f, isPlayer = true))
            currentAudioEvent = OrbitalAudioEvent.PLAYER_FIRE
        }

        // 4. Update Grid Wave Sway
        val waveSway = sin(elapsed * 2.2f) * 0.06f
        enemies.filter { it.alive && it.flightState == EnemyFlightState.GRID }.forEach { e ->
            val baseX = 0.18f + e.gridCol * 0.13f
            val baseY = 0.12f + e.gridRow * 0.09f
            e.x = baseX + waveSway
            e.y = baseY
        }

        // 5. Dive Bombing AI
        diveCooldown -= dt
        if (diveCooldown <= 0f && empActiveSeconds <= 0f) {
            val aliveInGrid = enemies.filter { it.alive && it.flightState == EnemyFlightState.GRID }
            if (aliveInGrid.isNotEmpty()) {
                val diver = aliveInGrid.random(randomSource())
                diver.flightState = EnemyFlightState.DIVING
                diver.diveProgress = 0f
                diver.diveStartX = diver.x
                diver.diveStartY = diver.y
            }
            diveCooldown = (2.2f - (round - 1) * 0.15f).coerceAtLeast(0.8f)
        }

        // Update Diving / Mystery Enemies
        val speedMultiplier = 1f + (round - 1) * 0.1f
        enemies.filter { it.alive && it.flightState != EnemyFlightState.GRID }.forEach { e ->
            when (e.flightState) {
                EnemyFlightState.DIVING -> {
                    e.diveProgress += dt * 0.85f * speedMultiplier
                    e.y = e.diveStartY + (1.1f - e.diveStartY) * e.diveProgress
                    e.x = e.diveStartX + sin(e.diveProgress * 6.28f) * 0.18f

                    // Fire while diving
                    if (randomFloat() < 0.04f * speedMultiplier && empActiveSeconds <= 0f) {
                        projectiles.add(Projectile(nextEntityId++, e.x, e.y + 0.02f, 0.65f * speedMultiplier, isPlayer = false))
                    }

                    if (e.diveProgress >= 1f) {
                        // Loop back to top
                        e.flightState = EnemyFlightState.RETURNING
                        e.diveProgress = 0f
                    }
                }
                EnemyFlightState.RETURNING -> {
                    e.y = 0.05f + (0.12f + e.gridRow * 0.09f - 0.05f) * e.diveProgress
                    e.diveProgress += dt * 1.2f
                    if (e.diveProgress >= 1f) {
                        e.flightState = EnemyFlightState.GRID
                    }
                }
                EnemyFlightState.MYSTERY_FLYBY -> {
                    e.x += dt * 0.35f
                    if (e.x > 1.15f) {
                        e.alive = false
                    }
                }
                else -> Unit
            }
        }

        // 6. Mystery Mothership Spawning
        mysteryCooldown -= dt
        if (mysteryCooldown <= 0f) {
            mysteryCooldown = 16f + randomFloat() * 8f
            enemies.add(
                EnemyShip(
                    id = nextEntityId++,
                    type = EnemyType.MYSTERY,
                    gridCol = -1,
                    gridRow = -1,
                    x = -0.1f,
                    y = 0.05f,
                    hp = EnemyType.MYSTERY.maxHp,
                    flightState = EnemyFlightState.MYSTERY_FLYBY
                )
            )
            currentAudioEvent = OrbitalAudioEvent.MYSTERY_SPAWN
        }

        // 7. Enemy Shooting (Grid)
        if (empActiveSeconds <= 0f && randomFloat() < (0.04f + round * 0.01f)) {
            val shooters = enemies.filter { it.alive && it.flightState == EnemyFlightState.GRID }
            if (shooters.isNotEmpty()) {
                val shooter = shooters.random(randomSource())
                projectiles.add(Projectile(nextEntityId++, shooter.x, shooter.y + 0.02f, 0.6f * speedMultiplier, isPlayer = false))
            }
        }

        // 8. Update Projectiles & Collisions
        val pIterator = projectiles.iterator()
        while (pIterator.hasNext()) {
            val p = pIterator.next()
            p.y += p.vy * dt

            if (p.isPlayer) {
                // Check collision with enemies
                val target = enemies.firstOrNull { e ->
                    e.alive && abs(p.x - e.x) < 0.05f && abs(p.y - e.y) < 0.04f
                }
                if (target != null) {
                    p.active = false
                    pIterator.remove()
                    target.hp--
                    if (target.hp <= 0) {
                        target.alive = false
                        val basePts = if (target.flightState == EnemyFlightState.DIVING) target.type.divePoints else target.type.points
                        val earnedPts = basePts * multiplier
                        score += earnedPts
                        streak++
                        if (streak % 10 == 0) {
                            multiplier = (multiplier + 1).coerceAtMost(5)
                        }
                        message = "${target.type.name} DESTROYED // +$earnedPts PTS"
                        currentAudioEvent = OrbitalAudioEvent.ENEMY_EXPLODE
                    }
                    continue
                }
            } else {
                // Check collision with player
                if (abs(p.x - playerX) < PLAYER_WIDTH / 2f && abs(p.y - PLAYER_Y) < 0.035f) {
                    p.active = false
                    pIterator.remove()
                    hitPlayer("HULL STRUCK BY ORBITAL PLASMA")
                    continue
                }
            }

            if (p.y < -0.05f || p.y > 1.05f) {
                p.active = false
                pIterator.remove()
            }
        }

        // 9. Enemy / Player Direct Collision Check
        enemies.filter { it.alive && it.flightState == EnemyFlightState.DIVING }.forEach { e ->
            if (abs(e.x - playerX) < 0.06f && abs(e.y - PLAYER_Y) < 0.04f) {
                e.alive = false
                hitPlayer("RAMMED BY DIVING ${e.type.name}")
            }
        }

        // 10. Wave Clear Check
        if (enemies.none { it.alive && it.type != EnemyType.MYSTERY }) {
            completeWave()
        }
    }

    private fun hitPlayer(reason: String) {
        lives--
        multiplier = 1
        streak = 0
        message = reason
        currentAudioEvent = OrbitalAudioEvent.PLAYER_HIT
        if (lives <= 0) {
            gameOver = true
            message = "ORBITAL DEFENSE BREACHED // $reason"
            currentAudioEvent = OrbitalAudioEvent.GAME_OVER
        }
    }

    private fun completeWave() {
        round++
        val waveBonus = 3000 * multiplier
        score += waveBonus
        if (lives < 3) lives++
        if (empBombs < 2) empBombs++
        multiplier = (multiplier + 1).coerceAtMost(5)
        message = "ORBITAL SECTOR ${round - 1} CLEARED // BONUS +$waveBonus PTS"
        currentAudioEvent = OrbitalAudioEvent.ROUND_CLEAR
        spawnWave()
    }

    private fun randomFloat(): Float {
        rng = rng * 6364136223846793005L + 1442695040888963407L
        return (rng ushr 32).toFloat() / 4294967296.0f
    }

    private fun randomSource(): kotlin.random.Random {
        rng = rng * 6364136223846793005L + 1442695040888963407L
        return kotlin.random.Random(rng)
    }

    fun snapshot() = OrbitalDefenseSnapshot(
        playerX = playerX,
        lives = lives,
        score = score,
        multiplier = multiplier,
        empBombs = empBombs,
        round = round,
        enemies = enemies.filter { it.alive }.map { it.copy() },
        projectiles = projectiles.filter { it.active }.map { it.copy() },
        empActiveSeconds = empActiveSeconds,
        gameOver = gameOver,
        message = message,
        elapsedSeconds = elapsed,
        audioEvent = currentAudioEvent
    )
}
