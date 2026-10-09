package com.example.starborn.data.assets

import com.example.starborn.data.local.ThemeStyle

class ThemeStyleAssetDataSource(
    private val assetReader: AssetJsonReader
) {
    fun loadStyles(): Map<String, ThemeStyle> = assetReader.readMap("theme_styles.json")
}
