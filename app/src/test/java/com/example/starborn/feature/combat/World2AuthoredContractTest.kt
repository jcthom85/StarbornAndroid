package com.example.starborn.feature.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.forEncounterRoom
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class World2AuthoredContractTest {
    private val assets=WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(),MoshiProvider.instance))
    private val enemies=assets.loadEnemies().associateBy { it.id }
    private data class Snapshot(val enemies:List<Enemy>)

    @Test fun originalStatsRewardsAndGuaranteedSuppliesSurviveTheBehaviorPass() {
        val root=if(File("../docs/design").exists()) File("..") else File(".")
        val original=MoshiProvider.instance.adapter(Snapshot::class.java).fromJson(
            File(root,"docs/design/enemy_encounters/BASELINE.json").readText())!!.enemies.associateBy { it.id }
        val identities=mapOf("moss_borer" to "echo_borer","mist_glider" to "siren_skimmer")+
            listOf("shard_hound","spore_spitter","ruin_guardian","razor_vine","stalker_vine","sentinel_orb","the_beast").associateWith { it }
        for((id,oldId) in identities) {
            val before=original.getValue(oldId);val now=enemies.getValue(id)
            assertEquals(before.copy(id=now.id,name=now.name,abilities=now.abilities,description=now.description,
                flavor=now.flavor,authored=now.authored),now)
        }
        // Pit identities remain unchanged, including their behavior and rewards.
        for(id in listOf("echo_borer","siren_skimmer")) assertEquals(original.getValue(id),enemies.getValue(id))
        val supplies=mapOf("sector9_stream_wetlands" to "herb","sector9_wilds_thickets" to "herb",
            "sector9_wilds_nest" to "beast_meat","sector9_foyer_left_gallery" to "scrap_metal",
            "sector9_conduit_junction" to "wiring_bundle","sector9_vents_gantry" to "battery")
        val rooms=assets.loadRooms().associateBy { it.id }
        supplies.forEach { (id,item) -> assertTrue("$item stays guaranteed at $id",rooms.getValue(id).enemyInstances.orEmpty()
            .flatMap { it.extraDrops }.any { it.id==item && it.chance==1.0 }) }
    }

    @Test fun acousticsRemixKeepsTheOriginalSingleEncounterPayoutAndDrops() {
        val sentinel=enemies.getValue("sentinel_orb").forEncounterRoom("sector9_hall_acoustics_lab")
        val plant=enemies.getValue("spore_spitter").forEncounterRoom("sector9_hall_acoustics_lab")
        val oldBudget=enemies.getValue("spore_spitter")
        assertEquals(oldBudget.xpReward,sentinel.xpReward+plant.xpReward)
        assertEquals(oldBudget.creditReward,sentinel.creditReward+plant.creditReward)
        assertEquals(oldBudget.drops,sentinel.drops+plant.drops)
        assertEquals(enemies.getValue("sentinel_orb"),enemies.getValue("sentinel_orb").forEncounterRoom("sector9_vents_gantry"))
    }

    @Test fun newWorldTwoIdentitiesAreScopedAndAllAuthoredSkillsHaveValidLimitsAndTargets() {
        for(room in assets.loadRooms()) {
            val ids=room.enemies+room.enemyParties.orEmpty().flatten()
            if(room.id.startsWith("sector9_")) assertTrue(ids.none { it in setOf("echo_borer","siren_skimmer") })
            else assertTrue(ids.none { it in setOf("moss_borer","mist_glider") })
        }
        val skills=assets.loadSkills().associateBy { it.id }
        for(enemy in enemies.values.filter { it.authored!=null }) {
            val pattern=enemy.authored!!
            (pattern.cycle+pattern.phase?.cycle.orEmpty()+listOfNotNull(pattern.phase?.openingSkill)).forEach { id ->
                assertTrue("${enemy.id} action $id exists",id in skills)
            }
            pattern.resourceLabels.keys.forEach { id -> assertTrue((skills.getValue(id).usesPerBattle ?: 0)>0) }
        }
    }
}
