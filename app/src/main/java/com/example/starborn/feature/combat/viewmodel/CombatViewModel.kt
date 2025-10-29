package com.example.starborn.feature.combat.viewmodel

import androidx.lifecycle.ViewModel
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill

class CombatViewModel(
    private val worldAssets: WorldAssetDataSource,
    private val enemyId: String
) : ViewModel() {

    val player: Player? = worldAssets.loadCharacters().firstOrNull()
    val enemy: Enemy? = worldAssets.loadEnemies().find { it.id == enemyId }
    val skills: List<Skill>? = worldAssets.loadSkills().filter { it.character == player?.id }

    val combatState = if (player != null && enemy != null) {
        CombatState(player, enemy)
    } else {
        null
    }

    fun nextTurn() {
        combatState?.let {
            val current = it.turnQueue.removeAt(0)
            it.turnQueue.add(current)

            if (it.turnQueue.first() is Enemy) {
                enemyAttack()
            }
        }
    }

    fun playerAttack() {
        combatState?.let {
            val damage = it.player.strength
            it.enemyHealth -= damage
            if (it.enemyHealth <= 0) {
                // Enemy defeated
            }
            nextTurn()
        }
    }

    fun useSkill(skill: Skill) {
        combatState?.let {
            val damage = (it.player.strength * (skill.basePower / 100.0)).toInt()
            it.enemyHealth -= damage
            if (it.enemyHealth <= 0) {
                // Enemy defeated
            }
            nextTurn()
        }
    }

    fun enemyAttack() {
        combatState?.let {
            val damage = it.enemy.strength
            it.playerHealth -= damage
            if (it.playerHealth <= 0) {
                // Player defeated
            }
            nextTurn()
        }
    }

    fun checkCombatEnd(): Boolean {
        combatState?.let {
            if (it.playerHealth <= 0 || it.enemyHealth <= 0) {
                return true
            }
        }
        return false
    }
}
