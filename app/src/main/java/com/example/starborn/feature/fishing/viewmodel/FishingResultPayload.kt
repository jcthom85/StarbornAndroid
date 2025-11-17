package com.example.starborn.feature.fishing.viewmodel

data class FishingResultPayload(
    val itemId: String?,
    val quantity: Int?,
    val message: String?,
    val success: Boolean
)