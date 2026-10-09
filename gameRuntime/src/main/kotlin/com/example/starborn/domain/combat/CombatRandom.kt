package com.example.starborn.domain.combat

import kotlin.random.Random

interface CombatRandom {
    fun nextDouble(from: Double, until: Double): Double
    fun nextInt(from: Int, until: Int): Int
}

object DefaultCombatRandom : CombatRandom {
    override fun nextDouble(from: Double, until: Double): Double = Random.nextDouble(from, until)
    override fun nextInt(from: Int, until: Int): Int = Random.nextInt(from, until)
}

class SeededCombatRandom(seed: Int) : CombatRandom {
    private val delegate = Random(seed)

    override fun nextDouble(from: Double, until: Double): Double = delegate.nextDouble(from, until)
    override fun nextInt(from: Int, until: Int): Int = delegate.nextInt(from, until)
}
