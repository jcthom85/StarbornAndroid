package com.example.starborn.domain.combat

/** Map cleared legacy squads to the replacement in the same room. */
object FoundryEncounterCompatibility {
    fun creditedIds(room: String, ids: List<String>): List<String> {
        val sorted = ids.sorted()
        return when (room) {
            // The old mixed patrol was separate from the remaining solo gatekeeper.
            "foundry_waste_intake" -> if (sorted == listOf("magma_drone", "slag_golem")) emptyList() else ids
            "foundry_service_airlock" -> if (sorted == listOf("slag_golem")) listOf("quench_mite") else ids
            "foundry_conditioning_chamber" -> if (sorted == listOf("flame_trooper")) listOf("flame_trooper", "magnet_sifter") else ids
            "foundry_power_core" -> if (sorted == listOf("flame_trooper") || sorted == listOf("phantom_prototype")) listOf("flame_trooper", "phantom_prototype") else ids
            "foundry_slag_river_scale_03" -> if (sorted == listOf("slag_golem")) listOf("slag_golem", "quench_mite") else ids
            "foundry_waste_intake_scale_03" -> if (sorted == listOf("slag_golem")) listOf("slag_golem", "magnet_sifter") else ids
            "foundry_service_airlock_scale_02" -> if (sorted == listOf("magma_drone")) listOf("quench_mite", "quench_mite") else ids
            "foundry_conveyor_belt_scale_02" -> if (sorted == listOf("welder_bot")) listOf("welder_bot", "quench_mite") else ids
            "foundry_conveyor_belt_scale_03" -> if (sorted == listOf("slag_golem")) listOf("conveyor_crusher", "magma_drone") else ids
            "foundry_conditioning_scale_02" -> if (sorted == listOf("magma_drone")) listOf("magma_drone", "quench_mite") else ids
            "foundry_titan_dock_scale_02" -> if (sorted == listOf("welder_bot")) listOf("flame_trooper", "welder_bot") else ids
            "foundry_slag_river_scale_final" -> if (sorted == listOf("magma_drone")) listOf("magma_drone", "magma_drone") else ids
            "foundry_waste_intake_scale_final" -> if (sorted == listOf("welder_bot") || sorted == listOf("magma_drone")) listOf("welder_bot", "magnet_sifter") else ids
            "foundry_conditioning_scale_final" -> if (sorted == listOf("phantom_prototype")) listOf("phantom_prototype", "welder_bot") else ids
            "foundry_power_core_scale_final" -> if (sorted == listOf("slag_golem") || sorted == listOf("welder_bot")) listOf("slag_golem", "welder_bot") else ids
            else -> ids
        }
    }
}
