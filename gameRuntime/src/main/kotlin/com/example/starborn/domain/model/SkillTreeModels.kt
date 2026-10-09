package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SkillTreeDefinition(
    val character: String,
    val branches: Map<String, List<SkillTreeNode>> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class SkillTreeNode(
    val id: String,
    val name: String,
    @Json(name = "cost_ap")
    val costAp: Int = 0,
    val pos: List<Int>? = null,
    val requires: List<String>? = null,
    val effect: SkillNodeEffect? = null
)

@JsonClass(generateAdapter = true)
data class SkillNodeEffect(
    val type: String? = null,
    val mult: Double? = null,
    val value: Int? = null,
    @Json(name = "buff_type")
    val buffType: String? = null,
    val subtype: String? = null,
    val element: String? = null,
    val target: String? = null
)
