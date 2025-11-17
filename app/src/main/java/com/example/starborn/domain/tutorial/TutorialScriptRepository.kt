package com.example.starborn.domain.tutorial

import com.example.starborn.data.assets.AssetJsonReader
import com.squareup.moshi.Types

class TutorialScriptRepository(
    private val reader: AssetJsonReader,
    private val assetPath: String = "tutorial_scripts.json"
) {

    private val scripts: Map<String, TutorialScript> by lazy {
        val type = Types.newParameterizedType(List::class.java, TutorialScript::class.java)
        val parsed = reader.read<List<TutorialScript>>(assetPath, type).orEmpty()
        parsed.associateBy { it.id }
    }

    fun script(id: String): TutorialScript? = scripts[id]

    fun allScripts(): Collection<TutorialScript> = scripts.values
}
