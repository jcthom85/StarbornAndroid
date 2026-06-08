package com.example.starborn.feature.crafting

data class IngredientUi(
    val label: String,
    val required: Int,
    val available: Int
)

enum class MinigameDifficulty {
    EASY,
    NORMAL,
    HARD
}

data class TimingMinigameUi(
    val recipeId: String,
    val recipeName: String,
    val progress: Float,
    val successStart: Float,
    val successEnd: Float,
    val perfectStart: Float,
    val perfectEnd: Float,
    val isRunning: Boolean,
    val difficulty: MinigameDifficulty,
    val timeRemainingSeconds: Float,
    val durationSeconds: Float
)
