package com.example.starborn.data.repository

import com.example.starborn.data.assets.ThemeAssetDataSource
import com.example.starborn.data.assets.ThemeStyleAssetDataSource
import com.example.starborn.data.local.Theme
import com.example.starborn.data.local.ThemeStyle

class ThemeRepository(
    private val themeAssetDataSource: ThemeAssetDataSource,
    private val themeStyleAssetDataSource: ThemeStyleAssetDataSource
) {
    private var themes: Map<String, Theme> = emptyMap()
    private var styles: Map<String, ThemeStyle> = emptyMap()

    fun load() {
        themes = themeAssetDataSource.loadThemes()
        styles = themeStyleAssetDataSource.loadStyles()
    }

    fun getTheme(id: String?): Theme? {
        return id?.let { themes[it] }
    }

    fun getStyle(id: String?): ThemeStyle? {
        return id?.let { styles[it] }
    }
}
