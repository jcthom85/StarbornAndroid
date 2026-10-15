package com.example.starborn.feature.combat.presentation

fun weaknessLabel(element: String): String = when (element) {
    "physical" -> "Physical"
    "burn" -> "Burn"
    "freeze" -> "Freeze"
    "shock" -> "Shock"
    "acid" -> "Acid"
    "source" -> "Source"
    else -> element
}
