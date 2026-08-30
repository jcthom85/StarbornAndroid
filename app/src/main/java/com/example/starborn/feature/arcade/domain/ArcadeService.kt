package com.example.starborn.feature.arcade.domain

import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.session.ArcadeCabinetProgress
import com.example.starborn.domain.session.GameSessionStore

object ArcadeIds {
    const val DEEP_MINE = "deep_mine_asteroid_drill"
    const val REPAIR_RECIPE = "repair_deep_mine_cabinet"
    const val REPAIRED_CORE = "deep_mine_cabinet_core"
    const val LOGIC_BOARD = "hyperion_logic_board"

    const val CANOPY_HOPPER = "canopy_hopper"
    const val REPAIR_CANOPY_RECIPE = "repair_canopy_hopper_cabinet"
    const val REPAIRED_CANOPY_CORE = "canopy_hopper_cabinet_core"
    const val CANOPY_OPTIC_BOARD = "sector9_optic_board"

    const val SPIRE_INFILTRATOR = "spire_infiltrator"
    const val REPAIR_SPIRE_RECIPE = "repair_spire_infiltrator_cabinet"
    const val REPAIRED_SPIRE_CORE = "spire_infiltrator_cabinet_core"
    const val SPIRE_LOGIC_BOARD = "hyperion_mainframe_chip"

    const val SLAG_CATCHER = "slag_catcher"
    const val REPAIR_SLAG_RECIPE = "repair_slag_catcher_cabinet"
    const val REPAIRED_SLAG_CORE = "slag_catcher_cabinet_core"
    const val SLAG_LOGIC_BOARD = "thermite_logic_gate"

    const val ORBITAL_DEFENSE = "orbital_defense"
    const val REPAIR_ORBITAL_RECIPE = "repair_orbital_defense_cabinet"
    const val REPAIRED_ORBITAL_CORE = "orbital_defense_cabinet_core"
    const val ORBITAL_LOGIC_BOARD = "zenith_telemetry_array"

    const val HARMONIC_PULSE = "harmonic_pulse"
    const val REPAIR_HARMONIC_RECIPE = "repair_harmonic_pulse_cabinet"
    const val REPAIRED_HARMONIC_CORE = "harmonic_pulse_cabinet_core"
    const val HARMONIC_LOGIC_BOARD = "prismatic_tuning_fork"
}

enum class ArcadeRewardTier(val threshold: Int) {
    BRONZE(5_000), SILVER(15_000), GOLD(30_000)
}

enum class CanopyRewardTier(val threshold: Int) {
    BRONZE(4_000), SILVER(12_000), GOLD(25_000)
}

enum class SpireRewardTier(val threshold: Int) {
    BRONZE(6_000), SILVER(18_000), GOLD(35_000)
}

enum class SlagRewardTier(val threshold: Int) {
    BRONZE(5_000), SILVER(15_000), GOLD(30_000)
}

enum class OrbitalRewardTier(val threshold: Int) {
    BRONZE(7_500), SILVER(20_000), GOLD(40_000)
}

enum class HarmonicRewardTier(val threshold: Int) {
    BRONZE(10_000), SILVER(25_000), GOLD(50_000)
}

data class ArcadeRunSubmission(
    val score: Int,
    val highScore: Int,
    val newlyClaimed: List<String>
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
        checkAllCabinetsRestored()
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
        return ArcadeRunSubmission(score, maxOf(current.highScore, score), newlyClaimed.map { it.name })
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

    fun discoverCanopyHopper(): Boolean {
        val current = progress(ArcadeIds.CANOPY_HOPPER)
        if (current.discovered) return false
        if (!inventoryService.hasItem(ArcadeIds.CANOPY_OPTIC_BOARD)) {
            inventoryService.addItem(ArcadeIds.CANOPY_OPTIC_BOARD)
            sessionStore.setInventory(inventoryService.snapshot())
        }
        sessionStore.learnSchematic(ArcadeIds.REPAIR_CANOPY_RECIPE)
        sessionStore.setMilestone("ms_arcade_canopy_hopper_discovered")
        sessionStore.updateArcadeProgress(ArcadeIds.CANOPY_HOPPER) { it.copy(discovered = true) }
        return true
    }

    fun completeCanopyHopperRepair(): Boolean {
        val current = progress(ArcadeIds.CANOPY_HOPPER)
        if (current.repaired) return false
        inventoryService.removeItem(ArcadeIds.REPAIRED_CANOPY_CORE)
        sessionStore.setInventory(inventoryService.snapshot())
        sessionStore.setMilestone("ms_arcade_cabinet_02_repaired")
        sessionStore.updateArcadeProgress(ArcadeIds.CANOPY_HOPPER) {
            it.copy(discovered = true, repaired = true, installed = true)
        }
        checkAllCabinetsRestored()
        return true
    }

    fun submitCanopyHopperScore(rawScore: Int): ArcadeRunSubmission {
        val score = rawScore.coerceAtLeast(0)
        val current = progress(ArcadeIds.CANOPY_HOPPER)
        val earned = CanopyRewardTier.entries.filter { score >= it.threshold }
        val newlyClaimed = earned.filter { it.name !in current.claimedTiers }
        newlyClaimed.forEach(::grantCanopyReward)
        val claimed = current.claimedTiers + newlyClaimed.map { it.name }
        sessionStore.updateArcadeProgress(ArcadeIds.CANOPY_HOPPER) {
            it.copy(
                highScore = maxOf(it.highScore, score),
                claimedTiers = claimed,
                playCount = it.playCount + 1
            )
        }
        sessionStore.setInventory(inventoryService.snapshot())
        return ArcadeRunSubmission(score, maxOf(current.highScore, score), newlyClaimed.map { it.name })
    }

    private fun grantCanopyReward(tier: CanopyRewardTier) {
        when (tier) {
            CanopyRewardTier.BRONZE -> {
                sessionStore.addCredits(500)
                inventoryService.addItem("herb", 3)
                inventoryService.addItem("beast_meat", 2)
            }
            CanopyRewardTier.SILVER -> {
                inventoryService.addItem("tideglass_delight", 2)
                sessionStore.setMilestone("ms_arcade_canopy_hopper_silver")
            }
            CanopyRewardTier.GOLD -> {
                inventoryService.addItem("bioluminescent_lure")
                sessionStore.setMilestone("ms_arcade_canopy_hopper_gold")
            }
        }
    }

    fun discoverSpireInfiltrator(): Boolean {
        val current = progress(ArcadeIds.SPIRE_INFILTRATOR)
        if (current.discovered) return false
        if (!inventoryService.hasItem(ArcadeIds.SPIRE_LOGIC_BOARD)) {
            inventoryService.addItem(ArcadeIds.SPIRE_LOGIC_BOARD)
            sessionStore.setInventory(inventoryService.snapshot())
        }
        sessionStore.learnSchematic(ArcadeIds.REPAIR_SPIRE_RECIPE)
        sessionStore.setMilestone("ms_arcade_spire_infiltrator_discovered")
        sessionStore.updateArcadeProgress(ArcadeIds.SPIRE_INFILTRATOR) { it.copy(discovered = true) }
        return true
    }

    fun completeSpireInfiltratorRepair(): Boolean {
        val current = progress(ArcadeIds.SPIRE_INFILTRATOR)
        if (current.repaired) return false
        inventoryService.removeItem(ArcadeIds.REPAIRED_SPIRE_CORE)
        sessionStore.setInventory(inventoryService.snapshot())
        sessionStore.setMilestone("ms_arcade_cabinet_03_repaired")
        sessionStore.updateArcadeProgress(ArcadeIds.SPIRE_INFILTRATOR) {
            it.copy(discovered = true, repaired = true, installed = true)
        }
        checkAllCabinetsRestored()
        return true
    }

    fun submitSpireInfiltratorScore(rawScore: Int): ArcadeRunSubmission {
        val score = rawScore.coerceAtLeast(0)
        val current = progress(ArcadeIds.SPIRE_INFILTRATOR)
        val earned = SpireRewardTier.entries.filter { score >= it.threshold }
        val newlyClaimed = earned.filter { it.name !in current.claimedTiers }
        newlyClaimed.forEach(::grantSpireReward)
        val claimed = current.claimedTiers + newlyClaimed.map { it.name }
        sessionStore.updateArcadeProgress(ArcadeIds.SPIRE_INFILTRATOR) {
            it.copy(
                highScore = maxOf(it.highScore, score),
                claimedTiers = claimed,
                playCount = it.playCount + 1
            )
        }
        sessionStore.setInventory(inventoryService.snapshot())
        return ArcadeRunSubmission(score, maxOf(current.highScore, score), newlyClaimed.map { it.name })
    }

    private fun grantSpireReward(tier: SpireRewardTier) {
        when (tier) {
            SpireRewardTier.BRONZE -> {
                sessionStore.addCredits(750)
                inventoryService.addItem("circuit_board", 2)
                inventoryService.addItem("nano_filament", 2)
            }
            SpireRewardTier.SILVER -> {
                inventoryService.addItem("phase_rounds")
                sessionStore.setMilestone("ms_arcade_spire_infiltrator_silver")
            }
            SpireRewardTier.GOLD -> {
                inventoryService.addItem("cyber_visor")
                sessionStore.setMilestone("ms_arcade_spire_infiltrator_gold")
            }
        }
    }

    fun discoverSlagCatcher(): Boolean {
        val current = progress(ArcadeIds.SLAG_CATCHER)
        if (current.discovered) return false
        if (!inventoryService.hasItem(ArcadeIds.SLAG_LOGIC_BOARD)) {
            inventoryService.addItem(ArcadeIds.SLAG_LOGIC_BOARD)
            sessionStore.setInventory(inventoryService.snapshot())
        }
        sessionStore.learnSchematic(ArcadeIds.REPAIR_SLAG_RECIPE)
        sessionStore.setMilestone("ms_arcade_slag_catcher_discovered")
        sessionStore.updateArcadeProgress(ArcadeIds.SLAG_CATCHER) { it.copy(discovered = true) }
        return true
    }

    fun completeSlagCatcherRepair(): Boolean {
        val current = progress(ArcadeIds.SLAG_CATCHER)
        if (current.repaired) return false
        inventoryService.removeItem(ArcadeIds.REPAIRED_SLAG_CORE)
        sessionStore.setInventory(inventoryService.snapshot())
        sessionStore.setMilestone("ms_arcade_cabinet_04_repaired")
        sessionStore.updateArcadeProgress(ArcadeIds.SLAG_CATCHER) {
            it.copy(discovered = true, repaired = true, installed = true)
        }
        checkAllCabinetsRestored()
        return true
    }

    fun submitSlagCatcherScore(rawScore: Int): ArcadeRunSubmission {
        val score = rawScore.coerceAtLeast(0)
        val current = progress(ArcadeIds.SLAG_CATCHER)
        val earned = SlagRewardTier.entries.filter { score >= it.threshold }
        val newlyClaimed = earned.filter { it.name !in current.claimedTiers }
        newlyClaimed.forEach(::grantSlagReward)
        val claimed = current.claimedTiers + newlyClaimed.map { it.name }
        sessionStore.updateArcadeProgress(ArcadeIds.SLAG_CATCHER) {
            it.copy(
                highScore = maxOf(it.highScore, score),
                claimedTiers = claimed,
                playCount = it.playCount + 1
            )
        }
        sessionStore.setInventory(inventoryService.snapshot())
        return ArcadeRunSubmission(score, maxOf(current.highScore, score), newlyClaimed.map { it.name })
    }

    private fun grantSlagReward(tier: SlagRewardTier) {
        when (tier) {
            SlagRewardTier.BRONZE -> {
                sessionStore.addCredits(850)
                inventoryService.addItem("pure_iron", 3)
                inventoryService.addItem("scrap_metal", 4)
            }
            SlagRewardTier.SILVER -> {
                inventoryService.addItem("foundry_crucible_lining")
                sessionStore.setMilestone("ms_arcade_slag_catcher_silver")
            }
            SlagRewardTier.GOLD -> {
                inventoryService.addItem("thermal_solder_gun")
                sessionStore.setMilestone("ms_arcade_slag_catcher_gold")
            }
        }
    }

    fun discoverOrbitalDefense(): Boolean {
        val current = progress(ArcadeIds.ORBITAL_DEFENSE)
        if (current.discovered) return false
        if (!inventoryService.hasItem(ArcadeIds.ORBITAL_LOGIC_BOARD)) {
            inventoryService.addItem(ArcadeIds.ORBITAL_LOGIC_BOARD)
            sessionStore.setInventory(inventoryService.snapshot())
        }
        sessionStore.learnSchematic(ArcadeIds.REPAIR_ORBITAL_RECIPE)
        sessionStore.setMilestone("ms_arcade_orbital_defense_discovered")
        sessionStore.updateArcadeProgress(ArcadeIds.ORBITAL_DEFENSE) { it.copy(discovered = true) }
        return true
    }

    fun completeOrbitalDefenseRepair(): Boolean {
        val current = progress(ArcadeIds.ORBITAL_DEFENSE)
        if (current.repaired) return false
        inventoryService.removeItem(ArcadeIds.REPAIRED_ORBITAL_CORE)
        sessionStore.setInventory(inventoryService.snapshot())
        sessionStore.setMilestone("ms_arcade_cabinet_05_repaired")
        sessionStore.updateArcadeProgress(ArcadeIds.ORBITAL_DEFENSE) {
            it.copy(discovered = true, repaired = true, installed = true)
        }
        checkAllCabinetsRestored()
        return true
    }

    fun submitOrbitalDefenseScore(rawScore: Int): ArcadeRunSubmission {
        val score = rawScore.coerceAtLeast(0)
        val current = progress(ArcadeIds.ORBITAL_DEFENSE)
        val earned = OrbitalRewardTier.entries.filter { score >= it.threshold }
        val newlyClaimed = earned.filter { it.name !in current.claimedTiers }
        newlyClaimed.forEach(::grantOrbitalReward)
        val claimed = current.claimedTiers + newlyClaimed.map { it.name }
        sessionStore.updateArcadeProgress(ArcadeIds.ORBITAL_DEFENSE) {
            it.copy(
                highScore = maxOf(it.highScore, score),
                claimedTiers = claimed,
                playCount = it.playCount + 1
            )
        }
        sessionStore.setInventory(inventoryService.snapshot())
        return ArcadeRunSubmission(score, maxOf(current.highScore, score), newlyClaimed.map { it.name })
    }

    private fun grantOrbitalReward(tier: OrbitalRewardTier) {
        when (tier) {
            OrbitalRewardTier.BRONZE -> {
                sessionStore.addCredits(1000)
                inventoryService.addItem("composite_plate", 3)
                inventoryService.addItem("nano_filament", 3)
            }
            OrbitalRewardTier.SILVER -> {
                inventoryService.addItem("plasma_lens_mod")
                sessionStore.setMilestone("ms_arcade_orbital_defense_silver")
            }
            OrbitalRewardTier.GOLD -> {
                inventoryService.addItem("orbital_deflector_matrix")
                sessionStore.setMilestone("ms_arcade_orbital_defense_gold")
            }
        }
    }

    fun discoverHarmonicPulse(): Boolean {
        val current = progress(ArcadeIds.HARMONIC_PULSE)
        if (current.discovered) return false
        if (!inventoryService.hasItem(ArcadeIds.HARMONIC_LOGIC_BOARD)) {
            inventoryService.addItem(ArcadeIds.HARMONIC_LOGIC_BOARD)
            sessionStore.setInventory(inventoryService.snapshot())
        }
        sessionStore.learnSchematic(ArcadeIds.REPAIR_HARMONIC_RECIPE)
        sessionStore.setMilestone("ms_arcade_harmonic_pulse_discovered")
        sessionStore.updateArcadeProgress(ArcadeIds.HARMONIC_PULSE) { it.copy(discovered = true) }
        return true
    }

    fun completeHarmonicPulseRepair(): Boolean {
        val current = progress(ArcadeIds.HARMONIC_PULSE)
        if (current.repaired) return false
        inventoryService.removeItem(ArcadeIds.REPAIRED_HARMONIC_CORE)
        sessionStore.setInventory(inventoryService.snapshot())
        sessionStore.setMilestone("ms_arcade_cabinet_06_repaired")
        sessionStore.updateArcadeProgress(ArcadeIds.HARMONIC_PULSE) {
            it.copy(discovered = true, repaired = true, installed = true)
        }
        checkAllCabinetsRestored()
        return true
    }

    fun submitHarmonicPulseScore(rawScore: Int): ArcadeRunSubmission {
        val score = rawScore.coerceAtLeast(0)
        val current = progress(ArcadeIds.HARMONIC_PULSE)
        val earned = HarmonicRewardTier.entries.filter { score >= it.threshold }
        val newlyClaimed = earned.filter { it.name !in current.claimedTiers }
        newlyClaimed.forEach(::grantHarmonicReward)
        val claimed = current.claimedTiers + newlyClaimed.map { it.name }
        sessionStore.updateArcadeProgress(ArcadeIds.HARMONIC_PULSE) {
            it.copy(
                highScore = maxOf(it.highScore, score),
                claimedTiers = claimed,
                playCount = it.playCount + 1
            )
        }
        sessionStore.setInventory(inventoryService.snapshot())
        return ArcadeRunSubmission(score, maxOf(current.highScore, score), newlyClaimed.map { it.name })
    }

    private fun grantHarmonicReward(tier: HarmonicRewardTier) {
        when (tier) {
            HarmonicRewardTier.BRONZE -> {
                sessionStore.addCredits(1500)
                inventoryService.addItem("astral_thread", 3)
                inventoryService.addItem("nano_filament", 4)
            }
            HarmonicRewardTier.SILVER -> {
                inventoryService.addItem("harmonic_resonator_tome")
                sessionStore.setMilestone("ms_arcade_harmonic_pulse_silver")
            }
            HarmonicRewardTier.GOLD -> {
                inventoryService.addItem("source_cadence_crown")
                sessionStore.setMilestone("ms_arcade_harmonic_pulse_gold")
            }
        }
    }

    private fun checkAllCabinetsRestored() {
        val allCabinets = listOf(
            ArcadeIds.DEEP_MINE,
            ArcadeIds.CANOPY_HOPPER,
            ArcadeIds.SPIRE_INFILTRATOR,
            ArcadeIds.SLAG_CATCHER,
            ArcadeIds.ORBITAL_DEFENSE,
            ArcadeIds.HARMONIC_PULSE
        )
        if (allCabinets.all { progress(it).repaired }) {
            sessionStore.setMilestone("ms_all_arcade_cabinets_restored")
        }
    }
}


