package com.example.starborn.desktop

import com.example.starborn.desktop.ui.*
import org.junit.Assert.*
import org.junit.Test

class DesktopFormationLayoutTest {
    @Test fun crowdedSupportedWindowsKeepSmallEnemiesReadable() {
        val members = listOf(FormationMember("a", "small"), FormationMember("b", "small")) +
            (1..4).map { FormationMember("large$it", "large") }
        for ((width, height) in listOf(520f to 390f, 776f to 300f)) {
            val placements = planBattleFormation(members, width, height, true)
            assertTrue(placements.all { it.spriteHeight >= 60f })
            assertTrue(placements.all { it.width >= 110f })
        }
        val compact = planBattleFormation(members, 776f, 300f, true)
        assertTrue(compact.maxOf { it.y + it.spriteHeight } - compact.minOf { it.y + it.spriteHeight } < .1f)
    }

    @Test fun everyPartySizeKeepsOneArcAndUniformScale() {
        for (width in listOf(300f, 400f, 800f)) for (count in 1..4) {
            val placements = planBattleFormation((1..count).map { FormationMember("crew$it") }, width, 230f, false)
            assertEquals(count, placements.size)
            assertEquals(1, placements.map { it.spriteHeight }.distinct().size)
            assertTrue(placements.zipWithNext().all { (a,b) -> a.x + a.width <= b.x })
            assertTrue(placements.all { it.x >= -.01f && it.x + it.width <= width + .01f && it.y + it.spriteHeight + it.hudHeight <= 230.01f })
        }
        val single = planBattleFormation(listOf(FormationMember("a")), 800f, 260f, false).single()
        val four = planBattleFormation((1..4).map { FormationMember("$it") }, 800f, 260f, false)
        assertEquals(single.spriteHeight, four.first().spriteHeight, .01f)
    }

    @Test fun bossDominatesMixedFormationAtEveryViewport() {
        for (width in listOf(300f, 400f, 800f)) {
            val placements = planBattleFormation(listOf(FormationMember("boss", "massive", 1.3f), FormationMember("drone", "small"), FormationMember("guard", "medium")), width, 320f, true)
            val boss = placements.first { it.id == "boss" }
            val drone = placements.first { it.id == "drone" }
            assertTrue(boss.spriteHeight > drone.spriteHeight * 2f)
            assertTrue(boss.y + boss.spriteHeight > drone.y + drone.spriteHeight)
            assertEquals(width / 2, boss.x + boss.width / 2, .01f)
            assertTrue(placements.all { it.x >= -.01f && it.x + it.width <= width + .01f && it.y + it.spriteHeight + it.hudHeight <= 320.01f })
        }
    }
}
