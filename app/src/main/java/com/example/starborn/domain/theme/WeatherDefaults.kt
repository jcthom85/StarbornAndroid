package com.example.starborn.domain.theme

fun defaultWeatherForEnvironment(environmentId: String?): String? {
    return when (environmentId?.lowercase()) {
        "mine" -> "dust"
        "logistics" -> "resonance"
        "space" -> "starfall"
        "swamp" -> "fog"
        else -> null
    }
}
