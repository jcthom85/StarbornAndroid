package com.example.starborn.domain.combat

import com.example.starborn.domain.model.StatusDefinition

class StatusRegistry(definitions: List<StatusDefinition> = emptyList()) {

    private val byId: Map<String, StatusDefinition>
    private val aliasToId: Map<String, String>

    init {
        val normalized = mutableMapOf<String, StatusDefinition>()
        val aliases = mutableMapOf<String, String>()
        definitions.forEach { definition ->
            val key = definition.id.lowercase()
            normalized[key] = definition
            definition.aliases.orEmpty().forEach { alias ->
                aliases[alias.lowercase()] = definition.id
            }
        }
        byId = normalized
        aliasToId = aliases
    }

    fun definition(idOrAlias: String?): StatusDefinition? {
        if (idOrAlias.isNullOrBlank()) return null
        val key = idOrAlias.lowercase()
        byId[key]?.let { return it }
        val resolvedId = aliasToId[key] ?: return null
        return byId[resolvedId.lowercase()]
    }

    fun allDefinitions(): Collection<StatusDefinition> = byId.values
}
