package com.example.starborn.data

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.Enemy
import com.squareup.moshi.Types
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class EnemyAssetLoadingTest {

    private val moshi = MoshiProvider.instance

    @Test
    fun enemiesJson_parsesWithDefaults() {
        val file = sequenceOf(
            File("app/src/main/assets/enemies.json"),
            File("src/main/assets/enemies.json")
        ).firstOrNull { it.exists() } ?: error("Unable to locate enemies.json in assets")
        val json = file.readText()
        val listType = Types.newParameterizedType(List::class.java, Enemy::class.java)
        val adapter = moshi.adapter<List<Enemy>>(listType)

        val enemies = try {
            adapter.fromJson(json)
        } catch (ex: Exception) {
            throw AssertionError("Failed to parse enemies.json: ${ex.message}", ex)
        }

        assertNotNull("enemies.json should parse into a list", enemies)
        assertFalse("enemies list should not be empty", enemies.orEmpty().isEmpty())
    }
}
