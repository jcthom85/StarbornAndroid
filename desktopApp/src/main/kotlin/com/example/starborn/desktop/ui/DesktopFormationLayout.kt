package com.example.starborn.desktop.ui

import kotlin.math.min

internal data class FormationMember(val id: String, val size: String = "medium", val width: Float = 1f)
internal data class FormationPlacement(val id: String, val x: Float, val y: Float, val width: Float, val spriteHeight: Float, val hudHeight: Float)

/** Coordinates are in dp. HUD space stays fixed; the entire formation shares one sprite scale. */
internal fun planBattleFormation(members: List<FormationMember>, width: Float, height: Float, enemySide: Boolean): List<FormationPlacement> {
    if (members.isEmpty() || width <= 0f || height <= 0f) return emptyList()
    fun size(member: FormationMember): Float = if (!enemySide) 128f else when (member.size) {
        "small" -> 104f; "large" -> 208f; "massive" -> 288f; else -> 152f
    }
    fun centered(row: List<FormationMember>): List<FormationMember> {
        if (row.size < 3) return row
        val dominant = row.maxBy { size(it) }
        val others = row.filter { it.id != dominant.id }.toMutableList()
        others.add(others.size / 2, dominant)
        return others
    }
    val rows = if (!enemySide) listOf(members) else {
        val dominant = members.maxBy { size(it) }
        if (dominant.size == "massive" && members.size == 2) {
            val companion = members.first { it.id != dominant.id }
            listOf(listOf(companion, dominant, companion.copy(id = "__formation_balance")))
        } else if (dominant.size == "massive" && members.size == 3) listOf(centered(members))
        else if (dominant.size == "massive" && members.size == 4)
            listOf(centered(members.filter { it.id != dominant.id }), listOf(dominant))
        else if (members.size <= 3 || width >= 720f && members.size <= 6) listOf(centered(members))
        else {
            val front = members.sortedByDescending { size(it) }.take((members.size + 1) / 2)
            listOf(centered(members.filter { member -> front.none { it.id == member.id } }), centered(front))
        }
    }
    val hud = if (enemySide) 56f else 68f
    val gap = 12f
    val bossCompanions = enemySide && members.size in 2..3 && members.any { it.size == "massive" }
    val arc = if (bossCompanions) 24f else if (!enemySide && members.size > 1) 18f else 0f
    val scaleByHeight = ((height - hud * rows.size - gap * (rows.size - 1) - arc) /
        rows.sumOf { row -> row.maxOf { size(it) }.toDouble() }.toFloat()).coerceAtLeast(.01f)
    // Reserve a readable minimum label width, then solve one scale for all rows.
    val minimumLabelWidth = min(if (enemySide) 112f else 72f, ((width - gap * (rows.maxOf { it.size } - 1)) / rows.maxOf { it.size }).coerceAtLeast(16f))
    fun slotWidth(member: FormationMember, row: List<FormationMember>, scale: Float): Float {
        fun natural(it: FormationMember) = maxOf(minimumLabelWidth, size(it) * it.width.coerceIn(.6f, 1.8f) * scale)
        return if (bossCompanions && member.size != "massive") row.filter { it.size != "massive" }.maxOf { natural(it) }
            else natural(member)
    }
    fun rowWidth(row: List<FormationMember>, scale: Float) = row.sumOf {
        slotWidth(it, row, scale).toDouble()
    }.toFloat() + gap * (row.size - 1)
    var scale = min(1f, scaleByHeight)
    var low = 0f
    var high = scale
    repeat(24) {
        val candidate = (low + high) / 2f
        if (rows.any { rowWidth(it, candidate) > width }) high = candidate else low = candidate
    }
    scale = low
    val totalHeight = rows.sumOf { row -> (row.maxOf { size(it) } * scale + hud).toDouble() }.toFloat() + gap * (rows.size - 1) + arc
    var y = if (enemySide) (height - totalHeight).coerceAtLeast(0f) else 0f
    return rows.flatMap { row ->
        val maxSprite = row.maxOf { size(it) } * scale
        var x = (width - rowWidth(row, scale)) / 2f
        val placements = row.mapIndexed { index, member ->
            val sprite = size(member) * scale
            val slotWidth = slotWidth(member, row, scale)
            val depth = if (bossCompanions) { if (member.size == "massive") arc else 0f } else if (arc > 0f && row.size > 2 && index in 1 until row.lastIndex) arc else if (arc > 0f && row.size == 2 && index == 1) arc * .5f else 0f
            FormationPlacement(member.id, x, y + maxSprite - sprite + depth, slotWidth, sprite, hud).also { x += slotWidth + gap }
        }
        y += maxSprite + hud + gap
        placements.filter { it.id != "__formation_balance" }
    }
}
