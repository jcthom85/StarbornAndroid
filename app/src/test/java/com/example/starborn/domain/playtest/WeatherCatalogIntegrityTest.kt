package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.theme.defaultWeatherForEnvironment
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WeatherCatalogIntegrityTest {

    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val assets = File(root, "app/src/main/assets")
    private val rooms by lazy { readRooms() }

    @Test
    fun `authored weather uses only rendered overlay ids`() {
        val campaignEnvironments = setOf("mine", "logistics", "space", "swamp", "neon", "foundry", "void", "source")
        val supported = setOf(
            "dust", "rain", "storm", "snow", "cave_drip", "starfall",
            "steam", "fog", "gas", "resonance", "sparks"
        )
        val unsupported = rooms.filter {
            it.env in campaignEnvironments && it.weather != null && it.weather !in supported
        }
            .map { "${it.id}=${it.weather}" }

        assertTrue("Unsupported room weather: $unsupported", unsupported.isEmpty())
    }

    @Test
    fun `campaign environments do not infer weather`() {
        listOf("mine", "logistics", "space", "swamp", "neon", "foundry", "void", "source")
            .forEach { environment -> assertNull(environment, defaultWeatherForEnvironment(environment)) }
    }

    @Test
    fun `sealed and sheltered rooms remain free of overlays`() {
        val expectedClear = setOf(
            "pit_mess", "pit_kitchen", "pit_jed_bunk", "medbay_exam1",
            "server_backup", "server_crawl", "launch_pod",
            "sector9_pod_interior", "sector9_temple_vestibule", "sector9_temple_lock_chamber",
            "sector9_foyer_grand_hall", "sector9_conduit_junction", "sector9_stasis_chamber",
            "sector9_vents_gantry", "sector9_power_turbine", "sector9_hangar_bay",
            "spire_the_static", "spire_zekes_apartment", "spire_backstreet_clinic",
            "spire_security_kiosk", "spire_skypark_dome", "spire_prism_gallery",
            "foundry_conditioning_chamber", "orbital_executive_dock",
            "orbital_grand_concourse", "orbital_solarium", "orbital_security_hub",
            "source_zeke_nightmare", "source_new_world"
        )
        val byId = rooms.associateBy { it.id }

        expectedClear.forEach { id ->
            assertTrue("Missing audited room $id", byId.containsKey(id))
            assertNull("Sealed room $id must not have weather", byId.getValue(id).weather)
        }
    }

    @Test
    fun `representative exposed and hazard rooms retain audited effects`() {
        val expected = mapOf(
            "pit_L1_landing" to "dust",
            "pit_showers" to "steam",
            "mine_landing" to "cave_drip",
            "mine_gas" to "gas",
            "server_cooling" to "snow",
            "sector9_crash_site" to "fog",
            "sector9_stream_cave_depths" to "cave_drip",
            "sector9_source_gate" to "resonance",
            "spire_vent_output" to "rain",
            "spire_sewers_landing" to "cave_drip",
            "spire_landing_pad_roof" to "storm",
            "spire_laundry_service" to "steam",
            "foundry_slag_landing" to "dust",
            "foundry_cooling_springs" to "steam",
            "foundry_waste_intake" to "gas",
            "foundry_forge_anvil" to "sparks",
            "orbital_server_farm" to "snow",
            "deep_anchor_chamber" to "resonance",
            "source_campfire" to "resonance",
            "source_echo_mines" to "dust",
            "source_memory_bridge_node_scale_03" to "rain",
            "source_orion_nightmare" to "starfall"
        )
        val byId = rooms.associateBy { it.id }

        expected.forEach { (id, weather) ->
            assertEquals("Unexpected weather for $id", weather, byId[id]?.weather)
        }
    }

    private fun readRooms(): List<Room> {
        val file = File(assets, "rooms.json")
        assertTrue("Asset file rooms.json must exist", file.exists())
        val type = Types.newParameterizedType(List::class.java, Room::class.java)
        return MoshiProvider.instance.adapter<List<Room>>(type).fromJson(file.readText()).orEmpty()
    }
}
