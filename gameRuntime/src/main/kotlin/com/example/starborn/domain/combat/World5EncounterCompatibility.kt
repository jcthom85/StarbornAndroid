package com.example.starborn.domain.combat

object World5EncounterCompatibility {
    fun creditedIds(room: String, ids: List<String>): List<String> = when (room) {
        "orbital_grand_concourse" -> if (ids.sorted() == listOf("compliance_officer")) listOf("customs_screener") else ids
        "orbital_mirror_walk" -> if (ids.sorted() == listOf("null_g_drone")) listOf("prism_surveyor") else ids
        "orbital_surveillance_pit" -> if (ids.sorted() == listOf("compliance_officer") || ids.sorted() == listOf("null_g_drone")) listOf("customs_screener", "void_turret") else ids
        "orbital_server_farm" -> if (ids.sorted() == listOf("void_turret", "void_turret") || ids.sorted() == listOf("firewall_construct")) listOf("void_turret", "void_turret") else ids
        "deep_firewall_alpha" -> if (ids.sorted() == listOf("void_turret")) listOf("void_turret", "customs_screener") else ids
        "deep_firewall_beta" -> if (ids.sorted() == listOf("void_turret")) listOf("void_turret", "null_g_drone") else ids
        "deep_firewall_gamma" -> if (ids.sorted() == listOf("firewall_construct")) listOf("firewall_construct", "prism_surveyor") else ids
        "orbital_executive_dock_scale_04" -> if (ids.sorted() == listOf("null_g_drone")) listOf("tether_rigger", "null_g_drone") else ids
        "orbital_grand_concourse_scale_02" -> if (ids.sorted() == listOf("compliance_officer")) listOf("customs_screener", "null_g_drone") else ids
        "orbital_solarium_scale_04" -> if (ids.sorted() == listOf("null_g_drone")) listOf("prism_surveyor", "null_g_drone") else ids
        "orbital_security_hub_scale_04" -> if (ids.sorted() == listOf("null_g_drone")) listOf("compliance_officer", "prism_surveyor") else ids
        "orbital_service_shaft_scale_02" -> if (ids.sorted() == listOf("compliance_officer")) listOf("tether_rigger") else ids
        "orbital_service_shaft_scale_06" -> if (ids.sorted() == listOf("compliance_officer")) listOf("customs_screener", "tether_rigger") else ids
        "deep_server_farm_scale_02" -> if (ids.sorted() == listOf("firewall_construct")) listOf("prism_surveyor", "void_turret") else ids
        "deep_throne_room_scale_06" -> if (ids.sorted() == listOf("elite_guard")) listOf("elite_guard", "prism_surveyor") else ids
        "deep_tear_scale_02" -> if (ids.sorted() == listOf("source_shadow")) listOf("threshold_remnant") else ids
        "orbital_grand_concourse_scale_final" -> if (ids.sorted() == listOf("compliance_officer")) listOf("customs_screener", "prism_surveyor") else ids
        "orbital_security_hub_scale_final" -> if (ids.sorted() == listOf("void_turret")) listOf("compliance_officer", "void_turret", "null_g_drone") else ids
        else -> ids
    }
}
