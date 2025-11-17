package com.example.starborn.domain.milestone

data class MilestoneEvent(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long
)
