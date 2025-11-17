package com.example.starborn.data

import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Item
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

class EnemyDropIntegrityTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun allEnemyDropsResolveToCatalogItems() {
        val items = readList("src/main/assets/items.json", Item::class.java)
        val enemies = readList("src/main/assets/enemies.json", Enemy::class.java)
        val aliasMap = buildAliasMap(items)
        val missing = mutableListOf<String>()
        enemies.forEach { enemy ->
            enemy.drops.forEach { drop ->
                val key = drop.id.lowercase(Locale.getDefault())
                if (aliasMap[key] == null) {
                    missing += "${enemy.id}:${drop.id}"
                }
            }
        }
        val message = buildString {
            append("Missing ${missing.size} drop references")
            if (missing.isNotEmpty()) {
                append(": ")
                append(missing.joinToString())
            }
        }
        assertTrue(message, missing.isEmpty())
    }

    private fun buildAliasMap(items: List<Item>): Map<String, String> {
        val whitespace = Regex("\\s+")
        val aliases = mutableMapOf<String, String>()
        items.forEach { item ->
            val id = item.id.lowercase(Locale.getDefault())
            aliases[id] = item.id
            item.aliases.forEach { alias ->
                aliases[alias.lowercase(Locale.getDefault())] = item.id
            }
            val name = item.name?.trim().orEmpty()
            if (name.isNotEmpty()) {
                val lower = name.lowercase(Locale.getDefault())
                aliases[lower] = item.id
                aliases[whitespace.replace(lower, "_")] = item.id
            }
        }
        return aliases
    }

    private fun <T> readList(path: String, clazz: Class<T>): List<T> {
        val file = File(path)
        require(file.exists()) { "$path not found" }
        val type = Types.newParameterizedType(List::class.java, clazz)
        val adapter = moshi.adapter<List<T>>(type)
        return requireNotNull(adapter.fromJson(file.readText())) {
            "Failed to parse $path"
        }
    }
}
