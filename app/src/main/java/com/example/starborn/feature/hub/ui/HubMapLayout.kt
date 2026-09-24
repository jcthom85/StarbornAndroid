package com.example.starborn.feature.hub.ui

/** Coordinates refer to the source illustration, never to the UI's padded screen. */
internal data class HubMapSite(
    val x: Float,
    val y: Float,
    val artworkWidth: Float = 0f,
    val labelDx: Float = 0f,
    val labelDy: Float = 0f
)

internal data class HubMapLayout(
    val cropTop: Float,
    val cropBottom: Float,
    val sites: Map<String, HubMapSite>,
    val astraDock: HubMapSite? = null
)

internal object HubMapLayouts {
    private fun site(x: Float, y: Float, width: Float = 0f, dx: Float = 0f, dy: Float = 0f) =
        HubMapSite(x, y, width, dx, dy)

    val all = mapOf(
        "hub_1_homestead" to HubMapLayout(.20f, .85f, mapOf(
            "pit" to site(.14f, .54f, .24f),
            "workshop" to site(.49f, .66f, .25f),
            "med_bay" to site(.46f, .32f, .24f),
            "trade_row" to site(.77f, .53f, .29f),
            "admin_gate" to site(.89f, .38f, .20f)
        )),
        "hub_2_logistics" to HubMapLayout(.10f, .92f, mapOf(
            "admin_concourse" to site(.38f, .29f),
            "server_room" to site(.24f, .51f, .22f),
            "deep_mine" to site(.24f, .72f),
            "echo_chamber" to site(.80f, .53f, .24f),
            "launch_bay" to site(.76f, .20f)
        ), astraDock = site(.64f, .85f, .26f)),
        "hub_3_sector9" to HubMapLayout(.04f, .94f, mapOf(
            "sector9_landing" to site(.24f, .66f, .22f),
            "razor_vine_path" to site(.39f, .35f, .19f),
            "tideglass_beach" to site(.22f, .85f),
            "canopy_ridge" to site(.72f, .52f),
            "temple_gate" to site(.73f, .20f, .23f)
        )),
        "hub_4_facility" to HubMapLayout(.08f, .96f, mapOf(
            "hall_of_echoes" to site(.24f, .18f),
            "stasis_chamber" to site(.75f, .30f),
            "source_gate" to site(.50f, .57f),
            "hangar_bay" to site(.45f, .80f, .24f),
            "the_sky" to site(.82f, .89f)
        )),
        "hub_5_lower_city" to HubMapLayout(.23f, .93f, mapOf(
            "spire_vent_output" to site(.20f, .46f),
            "spire_the_static" to site(.22f, .67f),
            "spire_night_market" to site(.68f, .78f),
            "spire_sewers" to site(.69f, .39f),
            "spire_transit_plaza" to site(.87f, .54f)
        )),
        "hub_6_upper_city" to HubMapLayout(.15f, .94f, mapOf(
            "spire_laundry" to site(.20f, .76f),
            "spire_skypark" to site(.51f, .29f),
            "spire_exec_lounge" to site(.24f, .42f),
            "spire_archive" to site(.61f, .53f),
            "spire_landing_pad" to site(.73f, .51f, dx = .06f, dy = .10f)
        ), astraDock = site(.87f, .46f, .17f)),
        "hub_7_slag_pits" to HubMapLayout(.14f, .94f, mapOf(
            "foundry_obsidian_shelf" to site(.22f, .44f),
            "foundry_slag_river" to site(.29f, .66f),
            "foundry_cooling_springs" to site(.31f, .30f),
            "foundry_waste_intake" to site(.69f, .84f),
            "foundry_service_airlock" to site(.82f, .32f)
        )),
        "hub_8_assembly_line" to HubMapLayout(.09f, .94f, mapOf(
            "foundry_conveyor_belt" to site(.22f, .77f),
            "foundry_conditioning" to site(.23f, .61f),
            "foundry_forge" to site(.32f, .27f),
            "foundry_power_core" to site(.57f, .46f),
            "foundry_titan_dock" to site(.84f, .49f)
        )),
        "hub_9_orbital_ring" to HubMapLayout(.08f, .82f, mapOf(
            "orbital_executive_dock" to site(.38f, .43f, dx = .06f, dy = .055f),
            "orbital_grand_concourse" to site(.48f, .60f),
            "orbital_solarium" to site(.42f, .25f),
            "orbital_security_hub" to site(.64f, .40f),
            "orbital_service_shaft" to site(.86f, .68f)
        ), astraDock = site(.23f, .455f, .19f)),
        "hub_10_deep_ring" to HubMapLayout(.12f, .83f, mapOf(
            "deep_server_farm" to site(.18f, .62f),
            "deep_anchor_chamber" to site(.43f, .56f),
            "deep_throne_room" to site(.70f, .44f),
            "deep_tear" to site(.89f, .27f)
        )),
        "hub_11_event_horizon" to HubMapLayout(.07f, .96f, mapOf(
            "source_campfire_node" to site(.49f, .48f),
            "source_zeke_nightmare_node" to site(.18f, .71f),
            "source_gh0st_nightmare_node" to site(.73f, .22f),
            "source_orion_nightmare_node" to site(.81f, .73f),
            "source_echo_mines_node" to site(.24f, .22f),
            "source_memory_bridge_node" to site(.49f, .84f)
        )),
        "hub_12_singularity" to HubMapLayout(.18f, .91f, mapOf(
            "source_memory_stair_node" to site(.22f, .72f),
            "source_spire_thought_node" to site(.41f, .45f),
            "source_center_node" to site(.73f, .43f, dy = .055f),
            "source_new_world_node" to site(.87f, .28f)
        )),
        "hub_astra" to HubMapLayout(.13f, 1f, mapOf(
            "astra_bridge_node" to site(.50f, .45f),
            "astra_disembark" to site(.50f, .94f)
        ))
    )
}

/** Fit an authored crop without changing its aspect ratio or moving individual nodes. */
internal data class HubMapTransform(val width: Float, val imageHeight: Float, val top: Float) {
    fun x(fraction: Float) = width * fraction
    fun y(fraction: Float) = imageHeight * (fraction - top)
    companion object {
        fun fit(viewWidth: Float, viewHeight: Float, imageAspect: Float, top: Float, bottom: Float): HubMapTransform {
            val width = minOf(viewWidth, viewHeight * imageAspect / (bottom - top))
            return HubMapTransform(width, width / imageAspect, top)
        }
    }
}
