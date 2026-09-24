# Astra hub placement progress

Work in two-hub world batches. Preserve the full-screen portrait backgrounds, illustrated destinations, and existing ship availability/travel rules. Do not publish until requested.

## World 1

- Homestead: replace the small upper-right shortcut with an in-scene ship on the lower-left service route, source anchor (0.25, 0.66), width 0.24. Smaller than the Logistics placement to fit the colony route without clipping the portrait edge.
- Logistics: ship on the broad lower-right apron, source anchor (0.76, 0.73), width 0.32.
- Existing Astra artwork reused; no new API generation or background changes.
- Same map-node selection and boarding controls as other physical Astra placements. Visibility continues to depend on the existing astra_access node.
- Focused emulator audit: hubIds=hub_1_homestead,hub_2_logistics, five configurations per hub (normal, compact, large text, locked/quest, pre-Astra).
- Log: test-results/astra-world1-placement.log. Screenshots: test-results/astra-world1-homestead.png and test-results/astra-world1-logistics.png.

## World 2

- Jungle Ruins (`hub_3_sector9`): replace the upper-right shortcut button with an in-scene ship on the upper-mid road clearing / mountain pass junction, source anchor (0.50, 0.32), width 0.21. Sized to match the perspective of the uphill path between Razor-Vine Path and Temple Gate, with landing gear resting on the stone pavers.
- Sector 9 Ruins (`hub_4_facility`): ship parked on the raised ancient stone terrace in the upper-left, source anchor (0.24, 0.36), width 0.23. Landing legs firmly planted on the stone platform above Source Gate and diagonally opposite Stasis Chamber, establishing a balanced zigzag cadence down the facility ruins.
- Subterranean Hangar Bay node (`hangar_bay`) on `hub_4_facility`: rendered as a clean, glowing tactical pin (`artworkWidth = 0f`) at (0.41, 0.79), maintaining full room interaction/discovery without displaying the historical in-cradle ship illustration that duplicated the active parked Astra.
- Existing Astra artwork (`images/nodes/astra_ship_map_v2.webp`) reused; no background or node art changes.
- Map-node selection, boarding controls, and travel rules preserved; ship appears only when existing `astra_access` state makes the Astra available.
- Focused emulator audit: `.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.starborn.HubMapVisualAuditTest' '-Pandroid.testInstrumentationRunnerArguments.hubIds=hub_3_sector9,hub_4_facility'` across all five configurations per hub (normal, compact, large text, locked/quest, pre-Astra; 10 screens total).
- Log: test-results/astra-world2-placement.log. Screenshots: test-results/astra-world2-sector9.png and test-results/astra-world2-facility.png (large-text captures: test-results/astra-world2-sector9-large.png and test-results/astra-world2-facility-large.png; verified pin capture: test-results/normal_hub_4_facility_pin.png; full configuration captures in test-results/astra-world2/).

## World 3

- Lower City (`hub_5_lower_city`): in-scene Astra docked in the central street crossroads, source anchor (0.50, 0.57), width 0.24. Fills the central asphalt void between The Sewers, Transit Plaza, The Static, and Vent Output. Eliminates the legacy top-right 28dp fallback button. `spire_vent_output` tuned to anchor (0.22, 0.47) and width 0.20 to mount cleanly against the left pipeline wall and balance architectural scale against the Night Market. `spire_transit_plaza` tightened to width 0.22 and `spire_night_market` nudged to (0.35, 0.76).
- Upper City (`hub_6_upper_city`): in-scene Astra docked on the dedicated sunken arrival terrace in the lower-right, source anchor (0.78, 0.68), width 0.25, landing firmly within the gilded parapet platform. The corporate `Landing Pad` node (`spire_landing_pad`) repositioned to the lower-left sky terrace at (0.33, 0.76), width 0.22, opening up the grand central promenade and cleanly distinguishing the player's arrival starship from the high-security executive shuttle pad. `spire_laundry` scaled to width 0.19 and positioned at (0.26, 0.56) to hug the hedge perimeter; `spire_archive` scaled to width 0.25 at (0.66, 0.47) to command the mid terrace.
- Motion & Grounding system integrated: contact shadows, subtle idle antigrav float (±3dp desynced sine wave) for The Astra, bouncy spring selection pop, ground sonar pulse, and atmospheric non-selected dimming.
- Verified via connected visual audit test across all 5 configurations per hub (normal, compact, large text, locked/quest, pre-Astra) on emulator-5554. All tests passed.

## World 4

- Slag Pits (`hub_7_slag_pits`): in-scene Astra docked at the center of the massive lower volcanic shelf overlooking the magma basin, source anchor (0.48, 0.80), width 0.25. Replaced the upper-right 28dp fallback button with physical placement on the dark volcanic stone. Node alignment harmonized with terrain features: `foundry_cooling_springs` tuned to (0.34, 0.46), width 0.22, beside the cyan coolant stream; `foundry_service_airlock` at (0.60, 0.28), width 0.23, at the upper junction; `foundry_obsidian_shelf` at (0.69, 0.56), width 0.23, hovering over the magma canyon; `foundry_waste_intake` at (0.33, 0.68), width 0.22; and `foundry_slag_river` at (0.72, 0.77), width 0.22, over the magma channel.
- Assembly Line (`hub_8_assembly_line`): in-scene Astra docked on the expansive lower-right industrial staging floor, source anchor (0.66, 0.78), width 0.25. Grounded directly onto the steel deck plating opposite Conveyor Belt at (0.36, 0.65), creating a clean zigzag cadence through the factory interior: The Forge (0.41, 0.29), Power Core (0.73, 0.39), Conditioning (0.36, 0.44), and Titan Dock (0.62, 0.54).
- Motion & Grounding system active: contact shadows, subtle idle antigrav float for The Astra, bouncy selection spring, ground sonar ring, and non-selected atmospheric dimming.
- Verified via connected visual audit test across all 5 configurations per hub on emulator-5554. All tests passed.

## World 5

- Dominion Orbital Ring (`hub_9_orbital_ring`): in-scene Astra aligned precisely to the center of the dedicated lower-right tarmac apron, source anchor (0.74, 0.70), width 0.26. Sits squarely inside the painted yellow perimeter markings without clipping curbs or guardrails. Harmonizes with Executive Dock (0.37, 0.71) across the boulevard, Service Shaft (0.65, 0.52), Grand Concourse (0.31, 0.44), Solarium (0.43, 0.29), and Security Hub (0.78, 0.35).
- Deep Ring (`hub_10_deep_ring`): replaced legacy top-right 28dp fallback button with in-scene Astra docked on the lower stone arrival bridge between the two bottom ice chasms, source anchor (0.49, 0.77), width 0.25. `deep_anchor_chamber` crowned at (0.50, 0.61), width 0.22, at the southern apex of the diamond ice chasm. Forms an authoritative vertical hierarchy: Throne Room (0.49, 0.30) -> Server Farm (0.27, 0.48) / The Tear (0.78, 0.42) -> Anchor Chamber (0.50, 0.61) -> The Astra (0.49, 0.77).
- Verified via connected visual audit test across all 5 configurations per hub on emulator-5554. All tests passed.

## World 6

- Event Horizon (`hub_11_event_horizon`): in-scene Astra docked on the lower-left sandstone terrace across from Memory Bridge, source anchor (0.32, 0.76), width 0.25. Replaced the legacy top-right 28dp fallback button with physical placement grounded firmly on the sandstone shelf. Forms a balanced bilateral entrance opposite Memory Bridge at (0.68, 0.77), leading cleanly upward past Silent Shore (0.24, 0.47) and Infinite Cubicle (0.80, 0.48) to The Campfire (0.50, 0.33), Echo of the Mines (0.21, 0.23), and Endless War (0.81, 0.25).
- The Singularity (`hub_12_singularity`): in-scene Astra docked on the eastern celestial landing terrace overlooking the purple void chasm, source anchor (0.76, 0.50), width 0.25. Grounded on the stone spur opposite Memory Stair at (0.23, 0.52), establishing a harmonious celestial gateway opposite Spire of Thought (0.59, 0.73) on the lower road and leading into The Center (0.61, 0.37) and New World (0.67, 0.24).
- Motion & Grounding system active: contact shadows, subtle idle antigrav float for The Astra, bouncy selection spring, ground sonar ring, and non-selected atmospheric dimming.
- Verified via connected visual audit test across all 5 configurations per hub on emulator-5554. All tests passed.

## Status

All 6 worlds (12 hubs) have completed physical in-scene Astra grounding and visual audits. All legacy 28dp fallback buttons successfully replaced. Motion, shadow, scale, and multi-display mode layout verified. Do not publish until requested.

