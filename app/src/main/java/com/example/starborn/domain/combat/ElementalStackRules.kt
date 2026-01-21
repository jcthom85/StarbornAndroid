package com.example.starborn.domain.combat

/**
 * Shared configuration for elemental stacking mechanics.
 */
object ElementalStackRules {
    private val stackableElements = linkedSetOf(
        "burn",
        "freeze",
        "shock",
        "acid"
    )

    const val STACK_THRESHOLD = 3

    val displayOrder: List<String> = stackableElements.toList()

    fun normalize(element: String?): String? = element?.lowercase()?.takeIf { it.isNotBlank() }

    fun isStackable(element: String?): Boolean = normalize(element)?.let(stackableElements::contains) == true
}
