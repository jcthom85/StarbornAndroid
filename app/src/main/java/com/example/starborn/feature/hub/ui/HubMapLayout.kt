package com.example.starborn.feature.hub.ui

/** Ground anchors in the full portrait source image; never relative to a UI inset. */
internal data class HubMapSite(
    val x: Float, val y: Float, val artworkWidth: Float = .25f,
    val labelDx: Float = 0f, val labelDy: Float = 0f
)
internal data class HubMapLayout(
    val sites: Map<String, HubMapSite>, val astraDock: HubMapSite? = null
)
internal object HubMapLayouts {
    private fun site(x: Float, y: Float, width: Float = .25f) = HubMapSite(x, y, width)
    val all = mapOf(
        "hub_1_homestead" to HubMapLayout(mapOf(
            "med_bay" to site(.23f, .35f),
            "admin_gate" to site(.74f, .41f),
            "pit" to site(.35f, .55f),
            "trade_row" to site(.73f, .69f, .29f),
            "workshop" to site(.43f, .79f)
        ), astraDock = site(.25f, .66f, .24f)),
        "hub_2_logistics" to HubMapLayout(mapOf(
            "admin_concourse" to site(.32f, .31f),
            "launch_bay" to site(.73f, .30f),
            "server_room" to site(.38f, .46f),
            "echo_chamber" to site(.65f, .51f),
            "deep_mine" to site(.39f, .65f)
        ), astraDock = site(.76f, .73f, .32f)),
        "hub_3_sector9" to HubMapLayout(mapOf(
            "razor_vine_path" to site(.30f, .36f),
            "temple_gate" to site(.68f, .28f),
            "sector9_landing" to site(.32f, .60f),
            "canopy_ridge" to site(.73f, .48f),
            "tideglass_beach" to site(.43f, .72f)
        ), astraDock = site(.50f, .32f, .21f)),
        "hub_4_facility" to HubMapLayout(mapOf(
            "hall_of_echoes" to site(.43f, .29f, .22f),
            "stasis_chamber" to site(.65f, .38f, .22f),
            "source_gate" to site(.35f, .49f),
            "the_sky" to site(.69f, .65f),
            "hangar_bay" to site(.41f, .79f, 0f)
        ), astraDock = site(.24f, .36f, .23f)),
        "hub_5_lower_city" to HubMapLayout(mapOf(
            "spire_sewers" to site(.51f, .36f),
            "spire_transit_plaza" to site(.72f, .51f, .22f),
            "spire_vent_output" to site(.22f, .47f, .20f),
            "spire_the_static" to site(.64f, .68f),
            "spire_night_market" to site(.35f, .76f)
        ), astraDock = site(.50f, .57f, .24f)),
        "hub_6_upper_city" to HubMapLayout(mapOf(
            "spire_skypark" to site(.39f, .38f),
            "spire_exec_lounge" to site(.72f, .30f),
            "spire_laundry" to site(.26f, .56f, .19f),
            "spire_archive" to site(.66f, .47f, .25f),
            "spire_landing_pad" to site(.33f, .76f, .22f)
        ), astraDock = site(.78f, .68f, .25f)),
        "hub_7_slag_pits" to HubMapLayout(mapOf(
            "foundry_service_airlock" to site(.60f, .28f, .23f),
            "foundry_cooling_springs" to site(.34f, .46f, .22f),
            "foundry_obsidian_shelf" to site(.69f, .56f, .23f),
            "foundry_waste_intake" to site(.33f, .68f, .22f),
            "foundry_slag_river" to site(.72f, .77f, .22f)
        ), astraDock = site(.48f, .80f, .25f)),
        "hub_8_assembly_line" to HubMapLayout(mapOf(
            "foundry_forge" to site(.41f, .29f),
            "foundry_power_core" to site(.73f, .39f),
            "foundry_conditioning" to site(.36f, .44f),
            "foundry_titan_dock" to site(.62f, .54f),
            "foundry_conveyor_belt" to site(.36f, .65f, .23f)
        ), astraDock = site(.66f, .78f, .25f)),
        "hub_9_orbital_ring" to HubMapLayout(mapOf(
            "orbital_solarium" to site(.43f, .29f),
            "orbital_security_hub" to site(.78f, .35f),
            "orbital_grand_concourse" to site(.31f, .44f),
            "orbital_service_shaft" to site(.65f, .52f),
            "orbital_executive_dock" to site(.37f, .71f)
        ), astraDock = site(.74f, .70f, .26f)),
        "hub_10_deep_ring" to HubMapLayout(mapOf(
            "deep_throne_room" to site(.49f, .30f),
            "deep_tear" to site(.78f, .42f),
            "deep_server_farm" to site(.27f, .48f),
            "deep_anchor_chamber" to site(.50f, .61f, .22f)
        ), astraDock = site(.49f, .77f, .25f)),
        "hub_11_event_horizon" to HubMapLayout(mapOf(
            "source_echo_mines_node" to site(.27f, .27f),
            "source_gh0st_nightmare_node" to site(.77f, .28f),
            "source_campfire_node" to site(.51f, .37f),
            "source_zeke_nightmare_node" to site(.76f, .49f),
            "source_orion_nightmare_node" to site(.27f, .49f),
            "source_memory_bridge_node" to site(.68f, .77f)
        ), astraDock = site(.32f, .76f, .25f)),
        "hub_12_singularity" to HubMapLayout(mapOf(
            "source_center_node" to site(.59f, .41f),
            "source_new_world_node" to site(.64f, .28f, .21f),
            "source_memory_stair_node" to site(.28f, .56f),
            "source_spire_thought_node" to site(.57f, .77f)
        ), astraDock = site(.76f, .50f, .25f)),
        "hub_astra" to HubMapLayout(mapOf(
            "astra_bridge_node" to site(.50f, .47f, 0f),
            "astra_disembark" to site(.50f, .71f, 0f)
        ))
    )
}

/** Same centered cover transform as ContentScale.Crop: full bleed, no letterboxing. */
internal data class HubMapTransform(
    val width: Float, val imageHeight: Float, val offsetX: Float, val offsetY: Float
) {
    fun x(fraction: Float) = offsetX + width * fraction
    fun y(fraction: Float) = offsetY + imageHeight * fraction
    companion object {
        fun cover(viewWidth: Float, viewHeight: Float, imageAspect: Float): HubMapTransform {
            val width = maxOf(viewWidth, viewHeight * imageAspect)
            val height = width / imageAspect
            return HubMapTransform(width, height, (viewWidth - width) / 2, (viewHeight - height) / 2)
        }
    }
}
