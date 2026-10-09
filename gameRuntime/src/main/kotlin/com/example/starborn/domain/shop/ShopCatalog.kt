package com.example.starborn.domain.shop

import com.example.starborn.domain.model.ShopDefinition

/**
 * Minimal abstraction for accessing shop definitions. Keeping the contract small
 * lets view models operate on fake catalogs during tests without the Android
 * asset stack.
 */
interface ShopCatalog {
    fun shopById(id: String?): ShopDefinition?
    fun allShops(): Collection<ShopDefinition>
}

