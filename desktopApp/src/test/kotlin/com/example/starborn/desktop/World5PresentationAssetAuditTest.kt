package com.example.starborn.desktop

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.presentation.InlineActionTarget
import com.example.starborn.feature.exploration.presentation.buildInlineActionPlan
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class World5PresentationAssetAuditTest {
    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val roots = listOf(File(root,"app/src/main/assets"), File(root,"world_assets/src/main/assets"))
    private val reader = AssetJsonReader(DesktopAssetProvider(roots), MoshiProvider.instance)
    private fun asset(path: String) = requireNotNull(roots.map { File(it,path) }.firstOrNull { it.isFile }) { "Missing asset: $path" }

    @Test fun allEnemyPrimaryArtResolvesAndCorrectedEnemiesUseTheirOwnSprites() {
        val enemies = reader.readList<Enemy>("enemies.json")
        enemies.forEach { enemy -> asset(enemy.sprite.firstOrNull() ?: enemy.portrait ?: "images/enemies/${enemy.id}_combat.png") }
        for (id in listOf("null_g_drone","aero_drone","riot_guard")) {
            val enemy = enemies.single { it.id == id }
            assertEquals("images/enemies/${id}_combat.png",enemy.sprite.first())
            assertEquals(enemy.sprite.first(),enemy.portrait)
        }
    }

    @Test fun allCrewHaveDistinctTransparentKoArtAndTheDroneHasTransparency() {
        val files = listOf("nova","zeke","orion","gh0st").map { asset("images/characters/emotes/${it}_down.png") } + asset("images/enemies/null_g_drone_combat.png")
        val signatures = mutableSetOf<Int>()
        for (file in files) {
            val image = ImageIO.read(file)
            assertTrue(file.name,image.width >= 512 && image.height >= 512)
            assertTrue(file.name,image.colorModel.hasAlpha())
            assertEquals("Corner must be transparent: ${file.name}",0,image.getRGB(0,0) ushr 24)
            assertTrue("Distinct art: ${file.name}",signatures.add(file.readBytes().contentHashCode()))
        }
    }

    @Test fun everyWorldFiveAndSixPassiveInspectionHasAVisibleTargetInEveryAuthoredDescription() {
        val rooms = reader.readList<Room>("rooms.json").filter { it.id.startsWith("orbital_") || it.id.startsWith("deep_") || it.id.startsWith("source_") }
        var count = 0
        for (room in rooms) {
            val actions = room.actions.filter { it["type"] !in setOf("rest_stop","shop","fishing","tinkering") && it["hide_inline"] != true }
                .map { GenericAction(it["name"] as String,"inspect") }
            val passive = room.actions.filter { it["type"] in setOf("generic","inspect") && it["action_event"] == null && it["hide_inline"] != true }
            for (description in listOf(room.description) + room.descriptionVariants.map { it.description }) {
                val names = buildInlineActionPlan(description,actions,emptyMap(),room)?.segments.orEmpty()
                    .mapNotNull { (it.target as? InlineActionTarget.Room)?.action?.name }
                for (raw in passive) assertTrue("${room.id}: ${raw["name"]}",raw["name"] in names)
            }
            count += passive.size
        }
        assertTrue(count > 150)
        val empty = rooms.filter { it.actions.isEmpty() }.map { it.id }.toSet()
        assertEquals(setOf("source_orion_tide_well","source_echo_elevator"),empty)
        assertTrue(rooms.single { it.id == "source_orion_tide_well" }.enemies.isNotEmpty())
        assertFalse(reader.readList<Room>("rooms.json").single { it.id == "astra_cargo_bay" }.actions.any { it["type"] == "tinkering" })
    }
}
