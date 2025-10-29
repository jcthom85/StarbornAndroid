package com.example.starborn.domain.model

sealed class Entity(
    open val id: String,
    open val name: String,
    open val description: String
)
