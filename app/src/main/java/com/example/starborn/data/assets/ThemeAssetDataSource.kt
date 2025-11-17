package com.example.starborn.data.assets

import com.example.starborn.data.local.Theme

class ThemeAssetDataSource(
    private val assetReader: AssetJsonReader
) {
    fun loadThemes(): Map<String, Theme> = assetReader.readMap("themes.json")
}
