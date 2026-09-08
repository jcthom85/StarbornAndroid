package com.example.starborn.data

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionIntegrityTest {
    private val assets = File("src/main/assets")

    @Test
    fun levelRewardsAreReachableDistinctAndOwnedByTheirCharacter() {
        val curve = JSONObject(File(assets, "leveling_data.json").readText()).getJSONObject("level_curve")
        val levels = curve.keys().asSequence().map(String::toInt).sorted().toList()
        assertTrue("XP levels must be contiguous", levels == (1..levels.last()).toList())
        assertTrue("XP thresholds must increase", levels.zipWithNext().all { (a, b) ->
            curve.getInt(a.toString()) < curve.getInt(b.toString())
        })
        val characters = JSONArray(File(assets, "characters.json").readText())
        val skills = JSONArray(File(assets, "skills.json").readText())
        val owners = (0 until skills.length()).associate {
            skills.getJSONObject(it).getString("id") to skills.getJSONObject(it).getString("character")
        }
        val rewards = JSONObject(File(assets, "progression.json").readText()).getJSONObject("level_up_skills")
        for (i in 0 until characters.length()) {
            val character = characters.getJSONObject(i)
            val id = character.getString("id")
            val mapping = rewards.optJSONObject(id) ?: continue
            val starting = character.getJSONArray("skills")
            val acquired = (0 until starting.length()).map { starting.getString(it) }.toMutableSet()
            mapping.keys().asSequence().toList().sortedBy(String::toInt).forEach { level ->
                val skill = mapping.getString(level)
                assertTrue("$id reward $skill at level $level is unreachable", level.toInt() in levels)
                assertTrue("$id reward $skill is already available", acquired.add(skill))
                assertTrue("$id reward $skill must exist and belong to them", owners[skill] == id)
            }
        }
    }
}
