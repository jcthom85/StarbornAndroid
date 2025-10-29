package com.example.starborn.data.assets

import com.example.starborn.domain.model.DialogueLine

class DialogueAssetDataSource(
    private val assetReader: AssetJsonReader
) {
    fun loadDialogue(): List<DialogueLine> = assetReader.readList("dialogue.json")
}
