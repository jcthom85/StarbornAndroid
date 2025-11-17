package com.example.starborn.data.repository

import com.example.starborn.data.assets.MilestoneAssetDataSource
import com.example.starborn.domain.model.MilestoneDefinition
import java.util.Locale

class MilestoneRepository(
    private val dataSource: MilestoneAssetDataSource
) {
    private val milestonesById: MutableMap<String, MilestoneDefinition> = mutableMapOf()
    private val milestonesByTrigger: MutableMap<String, MutableMap<String, MutableList<MilestoneDefinition>>> =
        mutableMapOf()

    fun load() {
        val definitions = dataSource.loadMilestones()
        milestonesById.clear()
        milestonesByTrigger.clear()
        definitions.forEach { definition ->
            if (definition.id.isNotBlank()) {
                milestonesById[definition.id] = definition
                definition.trigger?.let { trigger ->
                    val targetId = trigger.targetId()?.takeIf { it.isNotBlank() } ?: return@let
                    val typeKey = trigger.type.lowercase(Locale.getDefault())
                    val idKey = targetId.lowercase(Locale.getDefault())
                    val bucket = milestonesByTrigger.getOrPut(typeKey) { mutableMapOf() }
                    val definitionsForTrigger = bucket.getOrPut(idKey) { mutableListOf() }
                    definitionsForTrigger += definition
                }
            }
        }
    }

    fun milestoneById(id: String?): MilestoneDefinition? =
        id?.let { milestonesById[it] }

    fun all(): Collection<MilestoneDefinition> = milestonesById.values

    fun definitionsForTrigger(type: String, targetId: String): List<MilestoneDefinition> {
        val typeKey = type.lowercase(Locale.getDefault())
        val idKey = targetId.lowercase(Locale.getDefault())
        return milestonesByTrigger[typeKey]?.get(idKey)?.toList().orEmpty()
    }
}
