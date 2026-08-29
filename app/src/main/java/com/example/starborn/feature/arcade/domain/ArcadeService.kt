package com.example.starborn.feature.arcade.domain

import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.session.ArcadeCabinetProgress
import com.example.starborn.domain.session.GameSessionStore

object ArcadeIds {
    const val DEEP_MINE = "deep_mine_asteroid_drill"
    const val REPAIR_RECIPE = "repair_deep_mine_cabinet"
    const val REPAIRED_CORE = "deep_mine_cabinet_core"
    const val LOGIC_BOARD = "hyperion_logic_board"
}

enum class ArcadeRewardTier(val threshold: Int) {
    BRONZE(5_000), SILVER(15_000), GOLD(30_000)
}

data class ArcadeRunSubmission(
    val score: Int,
    val highScore: Int,
    val newlyClaimed: List<ArcadeRewardTier>
)

class ArcadeService(
    private val sessionStore: GameSessionStore,
    private val inventoryService: InventoryService
) {
    fun progress(cabinetId: String): ArcadeCabinetProgress =
        sessionStore.state.value.arcadeProgress[cabinetId] ?: ArcadeCabinetProgress()

    fun discoverDeepMine(): Boolean {
        val current = progress(ArcadeIds.DEEP_MINE)
        if (current.discovered) return false
        if (!inventoryService.hasItem(ArcadeIds.LOGIC_BOARD)) {
            inventoryService.addItem(ArcadeIds.LOGIC_BOARD)
            sessionStore.setInventory(inventoryService.snapshot())
        }
        sessionStore.learnSchematic(ArcadeIds.REPAIR_RECIPE)
        sessionStore.setMilestone("ms_arcade_deep_mine_discovered")
        sessionStore.updateArcadeProgress(ArcadeIds.DEEP_MINE) { it.copy(discovered = true) }
        return true
    }

    fun completeDeepMineRepair(): Boolean {
        val current = progress(ArcadeIds.DEEP_MINE)
        if (current.repaired) return false
        inventoryService.removeItem(ArcadeIds.REPAIRED_CORE)
        sessionStore.setInventory(inventoryService.snapshot())
        sessionStore.setMilestone("ms_arcade_cabinet_01_repaired")
        sessionStore.updateArcadeProgress(ArcadeIds.DEEP_MINE) {
            it.copy(discovered = true, repaired = true, installed = true)
        }
        return true
    }

    fun submitDeepMineScore(rawScore: Int): ArcadeRunSubmission {
        val score = rawScore.coerceAtLeast(0)
        val current = progress(ArcadeIds.DEEP_MINE)
        val earned = ArcadeRewardTier.entries.filter { score >= it.threshold }
        val newlyClaimed = earned.filter { it.name !in current.claimedTiers }
        newlyClaimed.forEach(::grantDeepMineReward)
        val claimed = current.claimedTiers + newlyClaimed.map { it.name }
        sessionStore.updateArcadeProgress(ArcadeIds.DEEP_MINE) {
            it.copy(
                highScore = maxOf(it.highScore, score),
                claimedTiers = claimed,
                playCount = it.playCount + 1
            )
        }
        sessionStore.setInventory(inventoryService.snapshot())
        return ArcadeRunSubmission(score, maxOf(current.highScore, score), newlyClaimed)
    }

    private fun grantDeepMineReward(tier: ArcadeRewardTier) {
        when (tier) {
            ArcadeRewardTier.BRONZE -> {
                sessionStore.addCredits(500)
                inventoryService.addItem("scrap_metal", 3)
                inventoryService.addItem("wiring_bundle", 2)
            }
            ArcadeRewardTier.SILVER -> {
                inventoryService.addItem("miner_gyro_mod")
                sessionStore.setMilestone("ms_arcade_deep_mine_silver")
            }
            ArcadeRewardTier.GOLD -> {
                inventoryService.addItem("drill_bit_mod")
                sessionStore.setMilestone("ms_arcade_deep_mine_gold")
            }
        }
    }
}
