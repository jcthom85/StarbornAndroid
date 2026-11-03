package com.example.starborn.feature.combat

import android.graphics.Paint
import android.graphics.Typeface
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.json.JSONArray
import java.io.File
import kotlin.math.ceil

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EnemyNameWidthAuditTest {
    @Test fun measureEveryEnemyUsingAndroidSansSerifSemibold() {
        val enemies = JSONArray(File("src/main/assets/enemies.json").readText())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), 600, false)
        }
        val rows = (0 until enemies.length()).map { i ->
            val enemy = enemies.getJSONObject(i)
            val name = enemy.getString("name")
            val measured = listOf(13,12,11).map { size -> paint.textSize = size.toFloat(); ceil(paint.measureText(name)).toInt() }
            assertTrue(measured.all { it > 0 })
            listOf(enemy.getString("id"),name,enemy.optString("tier")) + measured.map { "$it" }
        }
        val out = File("build/reports/enemy-name-audit/android.csv").apply { parentFile.mkdirs() }
        out.writeText("Id,Name,Tier,13sp width,12sp width,11sp width\n" + rows.joinToString("\n") { it.joinToString(",") } + "\n")
        val approved = mapOf("administrator_boss" to "Administrator", "phantom_prototype" to "Phantom",
            "distorted_sentinel" to "Warped Sentinel")
        approved.forEach { (id, name) ->
            assertEquals(name, rows.single { it.first() == id }[1])
        }
        val proposed = approved.values.map { name ->
            val sizes = listOf(13,12,11).map { size -> paint.textSize = size.toFloat(); ceil(paint.measureText(name)).toInt() }
            assertTrue("$name must fit the narrow 82.7 dp label at 11 sp", sizes.last() <= 82)
            name + "," + sizes.joinToString(",")
        }
        File(out.parentFile, "approved-names.csv").writeText("Name,13sp width,12sp width,11sp width\n" + proposed.joinToString("\n") + "\n")
        assertEquals(48, rows.size)
    }
}
