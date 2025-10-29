package com.example.starborn.data.assets

import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.domain.model.FirstAidRecipe
import com.example.starborn.domain.model.TinkeringRecipe

class CraftingAssetDataSource(
    private val assetReader: AssetJsonReader
) : CraftingRecipeSource {
    override fun loadTinkeringRecipes(): List<TinkeringRecipe> = assetReader.readList("recipes_tinkering.json")
    override fun loadCookingRecipes(): List<CookingRecipe> = assetReader.readList("recipes_cooking.json")
    override fun loadFirstAidRecipes(): List<FirstAidRecipe> = assetReader.readList("recipes_firstaid.json")
}

interface CraftingRecipeSource {
    fun loadTinkeringRecipes(): List<TinkeringRecipe>
    fun loadCookingRecipes(): List<CookingRecipe>
    fun loadFirstAidRecipes(): List<FirstAidRecipe>
}
