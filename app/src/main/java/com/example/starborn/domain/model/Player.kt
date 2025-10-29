package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class Player(
    override val id: String,
    override val name: String,
    val level: Int,
    val xp: Int,
    val hp: Int,
    val strength: Int,
    val vitality: Int,
    val agility: Int,
    val focus: Int,
    val luck: Int,
    val skills: List<String>,
    @Json(name = "mini_icon_path")
    val miniIconPath: String
) : Entity(id, name, "")
