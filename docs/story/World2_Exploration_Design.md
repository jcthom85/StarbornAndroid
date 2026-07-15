# World 2 (Sector 9) Exploration Expansion Specification

This document defines the complete exploration, puzzle, and narrative structure for World 2: The Wilds (Sector 9), expanding it from a 10-room linear skeleton into a **91-room branching network**. This aligns World 2's scale with World 1's vertical slice (88 rooms) and integrates the acoustic/oceanic lore, survival mechanics, and campsite narrative pacing.

---

## 1. Architectural & Narrative Rationale

### A. The Core Aesthetic
*   **Visuals:** Concrete ruins of an ancient facility reclaimed by a thick, glowing, bioluminescent jungle. Giant crystalline ferns, spore-choked thickets, and pools of glowing "mineral-rich water" (Glow-Water).
*   **Audio/Resonance:** Warm pads and restrained acoustic swells mark genuine Source contact. Wind through hollow arches and tuned stone stays physical. "Elara's Song" refers only to actual Elara recordings, never Nova's signal or Gh0st's choice.
*   **Erosion (Mental/Neurological Strain):** The spark of the Source carries physical strain. Nova must practice focus and grounding via the **Anchor Drill** (routing static to Zeke as the ground, Gh0st as the shield, and Orion as the tuner) to avoid melting the wire.

### B. Gameplay Progression Spine
1.  **Survival Phase (Hub 3 - Jungle Ruins):**
    *   Nova wakes at the crash site. Zeke is injured.
    *   Nova must explore the surrounding swamp to salvage pod components, find a medkit, and clear the path.
    *   *Exploration choice:* Venture through the dangerous, spore-choked **Razor-Vine Path** or navigate the **Glow-Water Stream** wetlands to discover a cavern bypass.
    *   *The Boss & The Truce:* Ascend to Canopy Ridge, fight the mutated **Source Beast** (mutated silverback), and establish a ceasefire with the hunter **Gh0st**.
2.  **Dungeon Phase (Hub 4 - Sanctuary Facility):**
    *   Enter the Temple Gate by slotting the Chime.
    *   Access the stasis vault, awaken **Orion** by aligning the ring array, and learn the **Anchor Drill** (unlocks **Link** relic).
    *   Unlock the Hangar Bay by routing energy manifold breakers in the Power Grid to overload the Source Gate.
    *   Repair the Astra's fuel lines, boot her bridge systems, and launch.
3.  **Planetary Breach (The Mesosphere):**
    *   Fly a low-altitude service airlane through canopy storms to the Spire. Keep the outer Planetary Shield visible as a distant ceiling; the Lens is needed first to cross the internal curtain to the Foundry.

---

## 2. Complete Room Connections (91 Rooms)

### HUB 3: JUNGLE RUINS (48 Rooms)

#### Area 1: Crash Site Sector (7 Rooms)
*   **landing_core (Crash Site Core):** Escape pod wreckage, half-submerged in swamp.
    *   *Connections:* `landing_pod` (in), `landing_brush` (north), `landing_glade` (west), `landing_stream` (east), `wilds_trailhead` (south).
*   **landing_pod (Escape Pod Interior):** The cramped, smoking cockpit.
    *   *Connections:* `landing_core` (out).
*   **landing_brush (Scorched Brush):** Fire-scarred flora, scattered batteries.
    *   *Connections:* `landing_core` (south).
*   **landing_glade (Overgrown Glade):** Deep ferns, glowing mushrooms.
    *   *Connections:* `landing_core` (east).
*   **landing_stream (Stream Entrance):** Flowing blue water leads out.
    *   *Connections:* `landing_core` (west), `stream_pools` (east).
*   **landing_perimeter (Security Perimeter):** Smashed Dominion sensor drone.
    *   *Connections:* `landing_glade` (north).
*   **landing_drop (Pre-crash Pod Fragment):** Scavengable debris panel.
    *   *Connections:* `landing_brush` (west).

#### Area 2: Glow-Water Stream (8 Rooms)
*   **stream_pools (Mist Pools):** Ground fog, `echo_borer` patrols.
    *   *Connections:* `landing_stream` (west), `stream_flats` (east).
*   **stream_flats (Crystalline Flats):** Crystalline sand beach.
    *   *Connections:* `stream_pools` (west), `stream_falls` (east), `stream_cave_entrance` (north).
*   **stream_falls (Resonant Falls):** Stone pillars and waterfall.
    *   *Connections:* `stream_flats` (west), `beach_cliffs` (east).
*   **stream_tidepools (Beach Transition):** River widens to delta.
    *   *Connections:* `stream_falls` (west), `beach_pools` (east).
*   **stream_cave_entrance (Cavern Entrance):** Waterfall opening.
    *   *Connections:* `stream_flats` (south), `stream_cave_depths` (in).
*   **stream_cave_depths (Glow-Moss Cavern):** Damp tunnels, bioluminescent moss.
    *   *Connections:* `stream_cave_entrance` (out), `stream_sunken_passage` (east).
*   **stream_sunken_passage (Waterlogged Tunnel):** Submerged cave bypass.
    *   *Connections:* `stream_cave_depths` (west), `beach_grotto` (east).
*   **stream_wetlands (Resonant Sedge):** Grassy marshes humming in wind.
    *   *Connections:* `stream_pools` (north).

#### Area 3: Tideglass Beach (8 Rooms)
*   **beach_cliffs (High Overlook):** Overlooking the sparkling shoreline.
    *   *Connections:* `stream_falls` (west), `beach_cove` (down).
*   **beach_cove (Sandy Cove):** Breather campsite, campfire interactable.
    *   *Connections:* `beach_cliffs` (up), `beach_pools` (east).
*   **beach_pools (Tide Pools):** Glow-water tide pools, allows fishing minigame.
    *   *Connections:* `beach_cove` (west), `beach_cargo` (east).
*   **beach_cargo (Beached Cargo Wreck):** Dominion crate containing scrap metal.
    *   *Connections:* `beach_pools` (west), `beach_dunes` (north).
*   **beach_cave (Sea Cavern):** Hissing blowholes.
    *   *Connections:* `beach_pools` (north).
*   **beach_pillar (Sunken obelisk):** Resonant ancient structure in surf.
    *   *Connections:* `beach_cave` (west).
*   **beach_grotto (Hidden Grotto):** Glimmering cave. Contains **Resin Mod**.
    *   *Connections:* `stream_sunken_passage` (west), `beach_cave` (south).
*   **beach_dunes (Star-Sand Dunes):** Musical dunes ringing when walked on.
    *   *Connections:* `beach_cargo` (south).

#### Area 4: The Wilds Thickets (11 Rooms)
*   **wilds_trailhead (Thorn Gate):** Vines blocking direct route.
    *   *Connections:* `landing_core` (north), `wilds_thickets` (south).
*   **wilds_thickets (Spore Thickets):** Foggy paths. `spore_spitter` presence.
    *   *Connections:* `wilds_trailhead` (north), `wilds_nest` (east), `wilds_hollow` (south).
*   **wilds_nest (Hound Nesting Grounds):** Jagged rocks. `shard_hound` pack.
    *   *Connections:* `wilds_thickets` (west).
*   **wilds_hollow (Poison Hollow):** Thick mist draining Guard.
    *   *Connections:* `wilds_thickets` (north), `wilds_grave` (south).
*   **wilds_grave (Patrol Graveyard):** Fallen scout body. Yields **Thermal Cutter**.
    *   *Connections:* `wilds_hollow` (north), `ridge_climb` (east).
*   **wilds_overlook (Canopy Edge):** Cliff edge looking down.
    *   *Connections:* `wilds_grave` (west).
*   **wilds_canopy_walk (Hanging Canopy Bridge):** Root bridge system.
    *   *Connections:* `wilds_overlook` (north).
*   **wilds_archway (Ancient Gateway):** Architect arch.
    *   *Connections:* `wilds_canopy_walk` (south).
*   **wilds_dense_brush (Beast Run):** Torn trees, heavy steps.
    *   *Connections:* `wilds_archway` (north).
*   **wilds_clearance (Burned Glade):** Open area cleared of vines.
    *   *Connections:* `wilds_dense_brush` (west).
*   **wilds_lookout (Hunter's Blind):** Gh0st's lookout post.
    *   *Connections:* `wilds_clearance` (north).

#### Area 5: Canopy Ascent & Ridge (8 Rooms)
*   **ridge_climb (Vine Climb):** Vertical vine wall climb.
    *   *Connections:* `wilds_grave` (west), `beach_grotto` (south), `ridge_ledges` (up).
*   **ridge_ledges (Windy Ledges):** High stone shelves, wind ambience.
    *   *Connections:* `ridge_climb` (down), `ridge_overhang` (north).
*   **ridge_overhang (Beast Lair Approach):** Lair entrance.
    *   *Connections:* `ridge_ledges` (south), `ridge_arena` (north).
*   **ridge_arena (Canopy Ridge Arena):** `the_beast` Boss Arena.
    *   *Connections:* `ridge_overhang` (south), `ridge_viewpoint` (east).
*   **ridge_viewpoint (Shield Viewpoint):** Observation platform. Breather chat.
    *   *Connections:* `ridge_arena` (west), `ridge_sniper_perch` (north).
*   **ridge_sniper_perch (Sniper Lookout):** Spotting site, shell casings.
    *   *Connections:* `ridge_viewpoint` (south), `ridge_crevice` (east).
*   **ridge_crevice (High Pass):** Rock crevice.
    *   *Connections:* `ridge_sniper_perch` (west), `ridge_plateau` (east).
*   **ridge_plateau (Sanctuary Outlook):** Plateau facing Temple.
    *   *Connections:* `ridge_crevice` (west), `temple_plaza` (east).

#### Area 6: Temple Gate Courtyard (6 Rooms)
*   **temple_plaza (Stone Courtyard):** Symmetric stone paths.
    *   *Connections:* `ridge_plateau` (west), `temple_lock_chamber` (east).
*   **temple_lock_chamber (Resonator Lock):** Gate mechanism with Chime slot.
    *   *Connections:* `temple_plaza` (west), `temple_left_wing` (north), `temple_right_wing` (south), `temple_threshold` (east).
*   **temple_left_wing (Resonator Column):** Left frequency tuner.
    *   *Connections:* `temple_lock_chamber` (south).
*   **temple_right_wing (Frequency Dampener):** Right frequency dampener.
    *   *Connections:* `temple_lock_chamber` (north).
*   **temple_threshold (Sanctuary Archway):** Inner gate arch.
    *   *Connections:* `temple_lock_chamber` (west), `foyer_grand_hall` (east).
*   **temple_vestibule (Outer Guard Post):** Deactivated Dominion turrets.
    *   *Connections:* `temple_plaza` (north).

---

### HUB 4: SECTOR 9 RUINS / THE SANCTUARY (43 Rooms)

#### Area 7: Sanctuary Foyer (5 Rooms)
*   **foyer_grand_hall (Entry Hall):** Main lobby of facility.
    *   *Connections:* `temple_threshold` (west), `foyer_left_gallery` (north), `foyer_right_gallery` (south), `hall_main_gallery` (east).
*   **foyer_left_gallery (Architect Reliquary):** Cracked resonance crystal displays.
    *   *Connections:* `foyer_grand_hall` (south).
*   **foyer_right_gallery (Aethel Archives):** Ancient wave logging crystals.
    *   *Connections:* `foyer_grand_hall` (north).
*   **foyer_security_hub (Dominion Outpost):** Abandoned security desk, hackable terminal.
    *   *Connections:* `foyer_left_gallery` (west).
*   **foyer_vault_door (Hangar Deck Portal):** Locked hangar gateway.
    *   *Connections:* `foyer_grand_hall` (east), `conduit_crawlspace` (secret check).

#### Area 8: Maintenance Conduit (4 Rooms)
*   **conduit_crawlspace (Ventilation Conduit):** Hangar ducting.
    *   *Connections:* `foyer_vault_door` (west), `conduit_junction` (east).
*   **conduit_junction (T-Junction Wiring):** Security bypass junction.
    *   *Connections:* `conduit_crawlspace` (west), `conduit_access_shaft` (up).
*   **conduit_access_shaft (vertical climb):** Access ladder.
    *   *Connections:* `conduit_junction` (down), `vents_gantry` (up).
*   **conduit_breaker_room (Auxiliary Generators):** Breaker switches.
    *   *Connections:* `conduit_junction` (south).

#### Area 9: Hall of Echoes (4 Rooms)
*   **hall_main_gallery (Mural Corridor):** Main hall containing the Light Murals.
    *   *Connections:* `foyer_grand_hall` (west), `hall_echo_alcoves` (north), `archive_vault` (east), `stasis_pod_deck` (south).
*   **hall_echo_alcoves (Visual Records):** Skull-to-skull link data panels.
    *   *Connections:* `hall_main_gallery` (south).
*   **hall_acoustics_lab (Research Station):** Dominion logging setups.
    *   *Connections:* `hall_main_gallery` (north).
*   **hall_reflection_pool (Cymatic Basin):** Pools holding standing fluid geometry.
    *   *Connections:* `hall_echo_alcoves` (east).

#### Area 10: Archive Room Vaults (4 Rooms)
*   **archive_vault (Cylinder Vault):** Database shelves.
    *   *Connections:* `hall_main_gallery` (west), `archive_reading_room` (east).
*   **archive_reading_room (Interface Console):** Interface panel.
    *   *Connections:* `archive_vault` (west), `archive_tuning_bay` (south).
*   **archive_tuning_bay (Matrix Puzzle):** Tuning columns (align matrix to C#m7).
    *   *Connections:* `archive_reading_room` (north), `archive_secret_stash` (east).
*   **archive_secret_stash (Hidden Record Room):** A labeled Bridge calibration record sits behind the pedestal glass, with diagrams for consent checks, separate carrier bands, and physical grounding.
    *   *Connections:* `archive_tuning_bay` (west).

#### Area 11: Stasis Chamber Deck (4 Rooms)
*   **stasis_pod_deck (Orion's Podium):** Podium containing Orion's stasis pod.
    *   *Connections:* `hall_main_gallery` (north), `stasis_ring_array` (east), `gate_chamber` (south).
*   **stasis_ring_array (Alignment Rings):** Console controlling energy rings.
    *   *Connections:* `stasis_pod_deck` (west), `stasis_coolant_lines` (south).
*   **stasis_coolant_lines (Erosion Buffers):** Coolant tubes.
    *   *Connections:* `stasis_ring_array` (north).
*   **stasis_observation (Observation Deck):** Safe viewing balcony.
    *   *Connections:* `stasis_pod_deck` (up).

#### Area 12: Stasis Ventilation Shafts (3 Rooms)
*   **vents_gantry (High Gantry):** Metal walkways hanging above pod.
    *   *Connections:* `conduit_access_shaft` (down), `vents_access_grate` (east).
*   **vents_access_grate (Infiltration Hatch):** Entry hatch.
    *   *Connections:* `vents_gantry` (west), `vents_tech_alcove` (north).
*   **vents_tech_alcove (Transmitter Site):** Contains **Dominion Transmitter**.
    *   *Connections:* `vents_access_grate` (south).

#### Area 13: Power Control Grid (4 Rooms)
*   **power_turbine (Source Dynamo):** Glowing turbine dynamo.
    *   *Connections:* `stasis_coolant_lines` (north), `power_manifold` (east).
*   **power_manifold (Fluid Distribution):** Pipe network routing fuel.
    *   *Connections:* `power_turbine` (west), `power_breaker_deck` (south).
*   **power_breaker_deck (Main Switch):** Bypass switches. deactivates Source Gate.
    *   *Connections:* `power_manifold` (north), `power_valve` (east).
*   **power_valve (Emergency Release):** Valve to dump charge.
    *   *Connections:* `power_breaker_deck` (west).

#### Area 14: Source Gate Chamber (3 Rooms)
*   **gate_chamber (Energy Barrier):** Acoustic barrier wall blocking hangar.
    *   *Connections:* `stasis_pod_deck` (north), `gate_emitter_left` (west), `hangar_pad` (east).
*   **gate_emitter_left (Harmonic Horn):** Barrier generator.
    *   *Connections:* `gate_chamber` (east).
*   **gate_emitter_right (Resonance Cup):** Grounding point.
    *   *Connections:* `gate_chamber` (west).

#### Area 15: Astra Hangar Bay (6 Rooms)
*   **hangar_pad (Astra Pad):** The launch pad with the ship hull.
    *   *Connections:* `gate_chamber` (west), `hangar_scaffolding` (up), `hangar_staging` (south).
*   **hangar_scaffolding (Maintenance Gantry):** Walkways around hull.
    *   *Connections:* `hangar_pad` (down), `hangar_tower` (east).
*   **hangar_staging (Cargo staging):** Elevator platform.
    *   *Connections:* `hangar_pad` (north).
*   **hangar_tower (Control Console):** Flight systems computer.
    *   *Connections:* `hangar_scaffolding` (west), `hangar_fuel_lines` (south).
*   **hangar_fuel_lines (Conduit Hookups):** Scorched lines requiring repairs.
    *   *Connections:* `hangar_tower` (north).
*   **hangar_lock (Hangar Blast Doors):** Ceiling blast shields.
    *   *Connections:* `hangar_pad` (up), `sky_ascent` (up).

#### Area 16: Shield Mesosphere (6 Rooms)
*   **sky_ascent (Climbing Flight):** Rising through mesosphere storms.
    *   *Connections:* `hangar_lock` (down), `sky_mesosphere` (up).
*   **sky_mesosphere (Storm Cells):** Heavy lightning.
    *   *Connections:* `sky_ascent` (down), `sky_shield_threshold` (up).
*   **sky_shield_threshold (Shield Edge):** Directly facing the Shield.
    *   *Connections:* `sky_mesosphere` (down), `sky_sensor_buoy` (east).
*   **sky_sensor_buoy (Dominion Sat):** Buoy monitoring flight data.
    *   *Connections:* `sky_shield_threshold` (west).
*   **sky_barrier_core (Frequency Node):** Local shield controller.
    *   *Connections:* `sky_shield_threshold` (north).
*   **sky_hard_deck (The Boundary):** Shield barrier pushback node.
    *   *Connections:* `sky_shield_threshold` (up).

---

## 3. Integration with Quests

### A. Main Quests
*   **w2_mq01 (A Strange Coast):** 
    *   Wake at `landing_core`.
    *   Search `landing_pod` to complete `examine_pod` and salvage a medkit.
    *   Clear `wilds_trailhead` by defeating the vines (requires Cutter from `wilds_grave` or force-burn via Chime).
*   **w2_mq02 (The Signal):**
    *   Follow track from `wilds_thickets` -> `wilds_hollow` -> `ridge_climb` -> `temple_plaza`.
    *   Align resonator batteries at `temple_left_wing` and `temple_right_wing` to slot Chime into `temple_lock_chamber`.
*   **w2_mq03 (Sleeping Giant):**
    *   Investigate light murals at `hall_main_gallery`.
    *   Bypass locked stasis doors by crawling through `conduit_crawlspace` to open observation deck.
    *   Solve the stasis pod ring alignment code at `stasis_ring_array` to awaken Orion at `stasis_pod_deck`.
*   **w2_mq04 (The Hunter):**
    *   Scout sniper positions from `ridge_ledges` -> `ridge_sniper_perch`.
    *   Confront Gh0st at `ridge_arena` and defeat `the_beast` boss.
*   **w2_mq05 (Liftoff):**
    *   Dampen stasis coolant leaks at `stasis_coolant_lines`.
    *   Route generators at `power_breaker_deck` and `power_manifold` to drop the barrier at `gate_chamber`.
    *   Repair intake conduits at `hangar_fuel_lines` and launch Astra from `hangar_tower`.

### B. Side Quests
*   **w2_sq01 (Botanist):** Zeke's scans. Flora located in:
    *   `landing_pod` (Moss)
    *   `stream_pools` (Ferns)
    *   `beach_pools` (Weeds)
    *   `wilds_thickets` (Spores)
    *   `stasis_coolant_lines` (Roots)
*   **w2_sq02 (Lost Patrol):**
    *   Discover the dead scouts at `wilds_grave` and retrieve the cutter modification details.
*   **w2_sq03 (Tideglass Day):**
    *   Discover `beach_cove`. Gather 3 tideglass skewers at `beach_pools` and roast them at the cove campfire.
*   **w2_sq04 (Ancient Echoes):**
    *   Align sound matrices to C#m7 in the `archive_tuning_bay` to unlock the hidden reliquary at `archive_secret_stash`.
*   **w2_sq05 (Stolen Tech):**
    *   Infiltrate `vents_tech_alcove` via high scaffolding `vents_gantry` to recover the missing transmitter case for Gh0st.
