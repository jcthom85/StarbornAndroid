package com.example.starborn.feature.combat.viewmodel

import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Entity
import com.example.starborn.domain.model.Player

class CombatState(
    val player: Player,
    val enemy: Enemy
) {
    var turnQueue: MutableList<Entity> = mutableListOf()
    var playerHealth: Int = player.hp
    var enemyHealth: Int = enemy.hp

    init {
        turnQueue.add(player)
        turnQueue.add(enemy)
    }
}
