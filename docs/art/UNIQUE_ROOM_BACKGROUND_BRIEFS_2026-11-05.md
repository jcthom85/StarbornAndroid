# Unique room background briefs — November 5, 2026

Production follow-up: all twelve briefs are now generated and wired; see the [completed art pass](../testing/UNIQUE_ROOM_BACKGROUNDS_2026-11-05.md). The planning text below records the pre-generation brief.


Status: briefs prepared for review. No images generated, room references changed, or existing artwork replaced by this step.

## Scope and visual direction

Twelve additional rooms currently borrow twelve original rooms' images. Keep the original artwork assigned to its original room; create one new composition for each additional room. All 477 room records and their 702 description entries were audited before this step: 12 shared background groups, no repeated room descriptions after whitespace/case normalization. This pass does not rewrite the descriptions.

Authority: current rooms, their action data, neighbor descriptions and description variants; `docs/story/Starborn_Art_Production_Guide.md`, `docs/story/Visual_Prompting_Guide.md`, and `data/assistant_briefing.md`. The later Art Production Guide's room template supplies the production settings; the scene paragraphs below remain pure environmental descriptions in the Visual Prompting Guide format. Built-in image generation is the intended execution path under the session imagegen skill; no API/CLI call or spending has occurred.

The existing artwork inspected for this brief includes World 1's Cavern Junction, World 2's Waterlogged Tunnel, World 3's Exec Lounge, World 4's Service Airlock and World 5's Zero-G Junction. Their strongest shared traits are portrait depth, outlined substantial machinery, clear foreground/midground separation and controlled amber/cyan lighting. Preserve that continuity while following the current canon's colorful anime/comic adventure direction. Do not introduce a separate photorealistic or muddy-horror style.

## Production settings shared by all briefs

- New opaque portrait room background, target export **1088 × 1920**. Generate and retain a PNG master; ship a WebP in `world_assets/src/main/assets/` using quality 90 / method 6. These are proposed destinations, not files already produced.
- First-person view from an actual entry. Use each brief's architectural focal point and clear depth planes. Keep essential landmarks toward the central field because Android uses `ContentScale.Crop` with small travel translations; leave calmer foreground and upper regions for UI.
- Environment stage only: no NPCs, player characters, animals, creatures, corpse, blood, readable writing, logos or baked-in interface. Jason, Meeko, Megan, Cheno, Cameron, Brutus and the murder patrons remain represented by gameplay prose/portraits rather than permanently painted actors.
- Permanent installed architecture may be visible. Loose evidence, quest items, personal keepsakes and loot stay out of the background. A fixed container location may be suggested by its housing, but not a permanently open/closed lid that contradicts later state.
- Use one background per target across its existing variants. Avoid active sirens, cheering spectators, a defeated opponent, an opened reward or a visibly sealed return route. The new backgrounds must survive those state changes.
- No recolors, alternative crops or edits to the shared originals. The twelve compositions must differ at thumbnail size through architecture, silhouette and viewpoint.
- Keep all current descriptions, actions, exits, gates, rewards and original image files intact during eventual wiring. This brief proposes artwork, not gameplay additions.

## Production order and replacement map

| Order | World | New room image | Original room keeps existing image |
| --- | --- | --- | --- |
| 1 | 3 | Secret Signal Workshop (`spire_hidden_studio`) | The Static |
| 2 | 3 | Executive Black-Site Lab (`spire_black_site_lab`) | Donor Gallery |
| 3 | 3 | Skyline VIP Salon (`spire_skyline_vip_suite`) | Exec Lounge |
| 4 | 3 | Syndicate Booth (`spire_vip_private_booth`) | Archive Vault |
| 5 | 3 | Bridgman Rigging Deck (`spire_rigging_deck`) | Landing Pad Roof |
| 6 | 4 | Subterranean Smelter Forge (`foundry_subterranean_smelter`) | Stepping Stones |
| 7 | 4 | The Scrapper's Crucible (`foundry_crucible_arena`) | Intake Overlook |
| 8 | 4 | High-Pressure Pipe Gallery (`foundry_pipe_gallery`) | Service Airlock |
| 9 | 2 | Tidewatch Station (`sector9_coastal_research_station`) | Mist Pools |
| 10 | 2 | Submerged Aethel Vault (`sector9_sunken_aethel_vault`) | Waterlogged Tunnel |
| 11 | 1 | Sealed Munitions Stash (`mine_sealed_munitions_vault`) | Cavern Junction |
| 12 | 5 | Breached Avionics Lab (`orbital_breached_avionics_lab`) | Zero-G Junction |

## Individual briefs

### 1. Secret Signal Workshop — World 3

Room: `spire_hidden_studio`. Current borrowed image: `images/rooms/world_3/spire_the_static.webp`.

**Identity:** An intimate electronics-and-music workshop, recognizable by a CRT wall and acoustic baffles rather than a public bar.

**Composition:** From the western doorway, a narrow aisle between built-in electronics benches leads toward a bank of chunky CRT housings. Tube-amplifier cabinets and a fixed mixing desk form a second silhouette; the route back is behind the viewer.

**Light and palette:** Warm amber amplifier light and green phosphor against charcoal acoustic panels, with a restrained cyan glow from the fixed simulation display. Match the Spire's outlined steelwork, but make this room warmer and more personal than its public spaces.

**Permanent landmarks:** Use permanent monitor casings, blank circuit-board fixtures, acoustic baffles, wired amplifier cabinets and mixing-console surfaces. Abstract screen patterns only.

**State and content constraints:** Keep Jason and Meeko out of the painted background. Omit loose game cartridges, food wrappers, readable project titles and whiteboard jokes; these belong to prose and portraits. Do not turn the room into a bar, server farm or nightclub.

**Route context:** west → Bridgman Rigging Deck (`spire_rigging_deck`).

**Scene prompt:**

> A first-person view from a reinforced doorway into a compact workshop tucked behind thick charcoal acoustic baffles. Built-in electronics benches form a narrow aisle toward a wall of rounded CRT housings, their green phosphor patterns reflected in scratched steel. Amber valves glow through the grilles of substantial amplifier cabinets beside a bolted mixing desk, while a small geometric starfield turns on a recessed display. Bundled cables and patched equipment brackets frame the room, leaving a quiet strip of floor through its center.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_3/spire_hidden_studio.webp`. PNG master: `output/imagegen/unique-room-backgrounds/spire_hidden_studio.png`.

### 2. Executive Black-Site Lab — World 3

Room: `spire_black_site_lab`. Current borrowed image: `images/rooms/world_3/spire_donor_gallery.webp`.

**Identity:** A cold, secret research chamber dominated by cylindrical containment architecture, not the donor gallery's portraits.

**Composition:** View northward from the southern gallery entrance. Rows of upright cylinders converge toward integrated server banks and an overhead projector assembly; a recessed security enclosure sits beneath it.

**Light and palette:** Cyan and cold white containment light, dark graphite server cabinets and small amber service indicators. Keep the cylinders readable and the scene bright enough for mobile presentation.

**Permanent landmarks:** Fixed stasis-cylinder shells with opaque or frosted interiors, server banks, ceiling conduits and an abstract blue projection. Treat the safe location as a recessed housing whose contents and door state are not resolved.

**State and content constraints:** No suspended bodies, subjects in tanks, neural-collar victims, readable classified plans or loose loot. Do not bake a permanently closed or open safe into the image.

**Route context:** south → Donor Gallery (`spire_donor_gallery`).

**Scene prompt:**

> A first-person view into a restricted laboratory where tall frosted cylinders stand in orderly rows beneath cold cyan service lights. Their heavy graphite bases connect through floor conduits to substantial server banks along the far wall. An overhead projector casts simple blue mechanical shapes above an inset security enclosure, its door face hidden by the angle of the surrounding housing. Scratched metal, reinforced glass and small amber maintenance lights break up the laboratory's severe symmetry, with a clear central approach between the cylinders.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_3/spire_black_site_lab.webp`. PNG master: `output/imagegen/unique-room-backgrounds/spire_black_site_lab.png`.

### 3. Skyline VIP Salon — World 3

Room: `spire_skyline_vip_suite`. Current borrowed image: `images/rooms/world_3/spire_exec_lounge_bar.webp`.

**Identity:** A broad public-facing salon with skyline glass and a permanent dispenser/security station; distinct from both the bar and the private booth.

**Composition:** From the western gallery entrance, curved glass reveals rain-covered towers behind plush built-in seating and a central open salon. A discreet northern opening leads toward the private booth.

**Light and palette:** Amber and champagne interior lighting, burgundy upholstery and cool cyan skyline reflections. Fixed chandelier geometry supplies the recognizable overhead landmark.

**Permanent landmarks:** Velvet stanchion bases, integrated dispenser counter, recessed security console, rain-streaked soundproof glass and abstract holographic chandelier forms.

**State and content constraints:** No patrons, guards, slumped suspect, police silhouettes, readable logs or accusation clues. Do not freeze siren activity or deployable crime-scene tape into the permanent background; the barrier area stays spatially legible through its fixed anchors.

**Route context:** west → Donor Gallery (`spire_donor_gallery`); north → Syndicate Booth (`spire_vip_private_booth`).

**Scene prompt:**

> A first-person view into a spacious skyline salon enclosed by tall rain-streaked glass. Curved burgundy seating and velvet stanchions divide the polished floor beneath geometric chandelier light, while gold-edged towers rise through cyan reflections outside. A built-in drinks dispenser and recessed security console share a counter near a discreet private doorway at the side of the room. Fixed barrier anchors mark the transition into that private area, and broad reflections leave the central floor visually calm.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_3/spire_skyline_vip_suite.webp`. PNG master: `output/imagegen/unique-room-backgrounds/spire_skyline_vip_suite.png`.

### 4. Syndicate Booth — World 3

Room: `spire_vip_private_booth`. Current borrowed image: `images/rooms/world_3/spire_archive_vault.webp`.

**Identity:** A small, enclosed mahogany booth overlooking neon clouds, not an archive hall or a second open salon.

**Composition:** View from the southern salon doorway toward a curved banquette and its substantial fixed table. The window wraps tightly around the booth; a recessed service shelf offers an architectural cue to the evidence area.

**Light and palette:** Warm mahogany and muted wine upholstery against cold rain-blue glass, with a narrow amber table light. Smaller, closer and more private than the salon.

**Permanent landmarks:** Curved mahogany enclosure, fixed table, banquette, soundproof window and installed comms/service fittings. Keep tabletop detail restrained.

**State and content constraints:** Omit Malick's body, blood, crystal glass, datapad, portable comms rig and any readable executive crest. These are narrative evidence, not permanent architecture. The image must not reveal who committed the murder.

**Route context:** south → Skyline VIP Salon (`spire_skyline_vip_suite`).

**Scene prompt:**

> A first-person view into a private curved mahogany booth, with a heavy table enclosed by deep wine-colored upholstery and a low amber reading light. Tall soundproof glass wraps around the far side, revealing rain and illuminated clouds between distant spires. A narrow service recess and built-in cable fittings occupy the inner wall, while warm wood grain and worn brass edges make the space feel expensive, close and insulated from the city. The broad tabletop remains quiet beneath the window reflections.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_3/spire_vip_private_booth.webp`. PNG master: `output/imagegen/unique-room-backgrounds/spire_vip_private_booth.png`.

### 5. Bridgman Rigging Deck — World 3

Room: `spire_rigging_deck`. Current borrowed image: `images/rooms/world_3/spire_landing_pad_roof.webp`.

**Identity:** An exposed structural work platform beneath the entertainment level, dominated by winch drums and crossing load cables rather than a landing pad.

**Composition:** From the western subway-car door, look along the working deck toward the studio access at the eastern end. Heavy trusses and load cables frame a central grated walkway above an unmistakable structural gap.

**Light and palette:** Warm contractor work lamps on yellow machinery, cool rain-blue city depth, dark graphite trusses and restrained cyan spill from the entertainment structure above.

**Permanent landmarks:** Bolted hydraulic winch drums, anchored cables, steel trusses, mounted speaker stacks, a fixed schematic panel and a work locker.

**State and content constraints:** Omit Cheno, family portraits, cider mug, legible contractor branding and readable load schematics. No aircraft, launch pad, giant open central hole on the walking route, or extra passage suggesting an unwired exit.

**Route context:** west → Old Subway Car (`spire_old_subway_car`); east → Secret Signal Workshop (`spire_hidden_studio`).

**Scene prompt:**

> A first-person view along a reinforced rigging deck suspended beneath a city entertainment structure. High-tension cables cross between deep steel trusses above a broad grated walkway, and heavy hydraulic winch drums sit bolted into yellow-edged machinery beds. Warm work lamps catch rain on the railings and speaker cabinets while cyan-lit towers descend into the gap beyond. A fixed schematic panel and scarred work locker occupy a sheltered bay, with a reinforced studio door visible at the far end.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_3/spire_rigging_deck.webp`. PNG master: `output/imagegen/unique-room-backgrounds/spire_rigging_deck.png`.

### 6. Subterranean Smelter Forge — World 4

Room: `foundry_subterranean_smelter`. Current borrowed image: `images/rooms/world_4/foundry_slag_stepping_stones.webp`.

**Identity:** An ancient enclosed geothermal forge, with monumental crucibles and a basalt anvil rather than an outdoor lava crossing.

**Composition:** View from the southern access toward a central monolithic anvil beneath immense crucible silhouettes. A dry, continuous approach separates the viewer from recessed geothermal channels.

**Light and palette:** Crimson geothermal depth, orange channel light, black obsidian and small cyan traces in cooling grooves. Keep forms clear rather than burying the chamber in smoke.

**Permanent landmarks:** Colossal obsidian crucibles, geothermal vent mouths, a basalt anvil and carved cooling channels. Leave the container-bearing recess partially occluded so its opening state is not painted in.

**State and content constraints:** No loose iron, mod rewards, active container lid state, figures, or modern Dominion logos. Distinguish the chamber from the ordinary Forge and the neighboring Stepping Stones.

**Route context:** south → Stepping Stones (`foundry_slag_stepping_stones`).

**Scene prompt:**

> A first-person view into an enclosed forge hollowed beneath black volcanic rock. Colossal obsidian crucibles rise around a monolithic basalt anvil, its broad surface traced by shallow cooling channels catching faint cyan light. Geothermal vents glow crimson behind massive stone ribs, and narrow orange channels run below the dry approach rather than across it. A deep recess in the anvil foundation disappears into shadow, while heavy silhouettes and reflected heat give the chamber the weight of machinery built long before its current owners.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_4/foundry_subterranean_smelter.webp`. PNG master: `output/imagegen/unique-room-backgrounds/foundry_subterranean_smelter.png`.

### 7. The Scrapper's Crucible — World 4

Room: `foundry_crucible_arena`. Current borrowed image: `images/rooms/world_4/foundry_intake_overlook.webp`.

**Identity:** A sunken machine-combat amphitheater with a clear ring silhouette, not an overlook of industrial intake machinery.

**Composition:** View from the southern access down into the ring. Heavy cage supports outline the combat floor, with recessed slag sluices outside it and a raised telemetry/podium structure above.

**Light and palette:** Orange slag glow beneath tungsten-gray cages, industrial yellow rail edges, cool cyan telemetry and warm overhead lamps. Energetic and readable rather than a muddy fight pit.

**Permanent landmarks:** Blast-cage frames, scorched titanium floor, raised victor platform, fixed telemetry housings and a large pneumatic announcer horn.

**State and content constraints:** No Brutus, cheering loaders, spectators, combatants, trophies, Forge-Plate prize or readable round counter. Use fixed cage framing without committing to gate-open versus gate-closed battle states.

**Route context:** south → Intake Overlook (`foundry_intake_overlook`).

**Scene prompt:**

> A first-person view from an iron access landing into a sunken circular combat amphitheater. Thick tungsten cage supports ring a broad scorched titanium floor, while recessed slag sluices cast orange light beneath the surrounding platforms. A raised victor platform and bulky telemetry housings overlook the ring beneath a large pneumatic announcer horn. Yellow-edged rails and cyan instrument glows separate the tiers clearly, and the open center of the arena remains spacious against the heavy industrial enclosure.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_4/foundry_crucible_arena.webp`. PNG master: `output/imagegen/unique-room-backgrounds/foundry_crucible_arena.png`.

### 8. High-Pressure Pipe Gallery — World 4

Room: `foundry_pipe_gallery`. Current borrowed image: `images/rooms/world_4/foundry_service_airlock.webp`.

**Identity:** A long steam-service workspace whose twin-loop manifold and personal workstation distinguish it from a generic airlock.

**Composition:** View east from the western Decon approach. Overhead conduits lead toward the Assembly maintenance shutter; the permanent twin-loop bypass assembly occupies the midground beside a bolted workbench.

**Light and palette:** Cool steel and pale steam balanced by amber work lamps, yellow safety bands and small cyan pressure indicators. Do not obscure the walkable gallery in vapor.

**Permanent landmarks:** Large flanged pipes, twin-loop bypass manifold, built-in tool storage, a pipe-fitting bench, checkered pennants without numbers, and the far shutter frame.

**State and content constraints:** Omit Cameron, loose tools/gloves/baseball/bat, readable number 24, family photograph details and dog-collar text. Frame the shutter with its center beyond the sightline, so the same image supports locked and permanently open states. Do not make the east return corridor look destroyed or inaccessible.

**Route context:** west → Decon Chamber (`foundry_decon_chamber`); east → Reject Bay (`foundry_reject_bay`).

**Scene prompt:**

> A first-person view along a pressurized maintenance gallery beneath massive flanged steam conduits. A twin-loop bypass manifold fills a service bay beside a bolted pipe-fitting bench and recessed tool storage, with checkered pennants hanging beneath amber work lights. Pale steam escapes in narrow wisps around the upper pipework without covering the dry central walkway. Steel ribs and yellow hazard bands lead toward a deep maintenance-shutter frame at the far end, where the central opening lies beyond the angle of the adjoining wall.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_4/foundry_pipe_gallery.webp`. PNG master: `output/imagegen/unique-room-backgrounds/foundry_pipe_gallery.png`.

### 9. Tidewatch Station — World 2

Room: `sector9_coastal_research_station`. Current borrowed image: `images/rooms/world_2/stream_pools.webp`.

**Identity:** A patched coastal utility station with a desk and filtration system, rather than another open pool landscape.

**Composition:** View from the western tidal approach into a sheltered monitoring workspace. A broad window overlooks the wetlands; side openings suggest the coastal route without multiplying exits.

**Light and palette:** Teal wetland daylight and fog outside, warm amber desk light within, weathered cream panels and copper plumbing. A practical refuge with signs of careful maintenance.

**Permanent landmarks:** Fixed telemetry gauges, patched filtration pipes, a scrubbed built-in desk and a recessed warming plate. Use abstract gauge shapes and blank screen patterns.

**State and content constraints:** No Megan, readable forensic evidence, coffee notes, legible gauges or portable slate. Avoid an abandoned horror clinic: the station is actively maintained even when empty in the background art.

**Route context:** west → Beach Transition (`sector9_stream_tidepools`); south → Beach Cliffs (`sector9_beach_cliffs`); north → Sandy Cove (`sector9_beach_cove`).

**Scene prompt:**

> A first-person view from the tidal approach into a sheltered coastal monitoring station. Patched copper pipes run from sturdy filter housings along weathered cream walls, while a bank of analog gauges and simple illuminated displays faces a broad window over misty tidal wetlands. A well-scrubbed built-in desk stands under a warm amber lamp beside a recessed heating plate. Teal daylight reflects from damp railings and carefully repaired panel seams, giving the compact utility workspace a quiet, maintained warmth.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_2/sector9_coastal_research_station.webp`. PNG master: `output/imagegen/unique-room-backgrounds/sector9_coastal_research_station.png`.

### 10. Submerged Aethel Vault — World 2

Room: `sector9_sunken_aethel_vault`. Current borrowed image: `images/rooms/world_2/stream_sunken_passage.webp`.

**Identity:** A revealed domed Aethel chamber with a dry dais and visible drainage, distinct from the flooded access tunnel.

**Composition:** View southward from the northern tunnel entrance toward a raised central dais, framed by basalt pillars and a coral-shaped dome. Drainage channels pull remaining brine away from the dry approach.

**Light and palette:** Deep cyan and restrained amber along coral-encrusted conductors, dark basalt pillars, warm brass architectural edges and residual teal brine.

**Permanent landmarks:** Basalt pillars, coral-shaped mineral architecture, embedded conductors, floor drainage slits and raised dais. The reliquary recess is shaded without a resolved lid or contents.

**State and content constraints:** No creatures, loose loot, fully flooded central dais, readable Aethel inscriptions, or copied tunnel composition. Keep the chamber as the successfully drained reward space; coral reads as the established architectural material rather than added fauna.

**Route context:** north → Waterlogged Tunnel (`sector9_stream_sunken_passage`).

**Scene prompt:**

> A first-person view from a low stone entrance into a domed chamber supported by dark basalt pillars. Coral-shaped mineral ribs curl across the ceiling around ancient metal conductors, with deep cyan and amber light caught beneath translucent deposits. Floor slits draw the remaining brine into narrow channels around a raised dry dais, its inset recess sheltered beneath a low brass-edged canopy. The shallow water reflects the pillars while the central stone approach stays exposed and clearly walkable.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_2/sector9_sunken_aethel_vault.webp`. PNG master: `output/imagegen/unique-room-backgrounds/sector9_sunken_aethel_vault.png`.

### 11. Sealed Munitions Stash — World 1

Room: `mine_sealed_munitions_vault`. Current borrowed image: `images/rooms/world_1/mine_junction.webp`.

**Identity:** A small breached armory behind the solved ore-cart obstruction, distinct from the multi-tunnel junction.

**Composition:** View westward through the broken eastern bulkhead into a compact locker chamber. Shattered door plates and the permanently wedged ore cart frame the opening; installed lockers supply the dominant interior feature.

**Light and palette:** Dusty amber industrial lights, muted steel blue in deep storage recesses, rusted iron and gray stone. Match the mine's chunky supports and clear cyan/amber separation.

**Permanent landmarks:** Damaged blast-bulkhead frame, fixed locker banks, scattered rockfall and the wedged ore-cart chassis. Show cache architecture without an explicit lid state.

**State and content constraints:** No weapons on racks, loose scrap or reward items, soldiers, readable Dominion marks, alternate tunnels, or intact bulkhead blocking the entrance. The cart is persistent aftermath of the route-opening puzzle, not a fresh movable puzzle object.

**Route context:** east → Cavern Junction (`mine_junction`).

**Scene prompt:**

> A first-person view through a shattered mine bulkhead into a compact rock-cut armory. Twisted blast-door plates and the crushed chassis of an ore cart frame the entrance, with gray slag dust banked beneath bent iron supports. Stacked steel locker housings occupy the far wall under amber service lamps, their recessed faces partly hidden by a fallen support. Muted blue light separates the storage bays from the warm dust, and a single clear approach leads into the chamber.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_1/mine_sealed_munitions_vault.webp`. PNG master: `output/imagegen/unique-room-backgrounds/mine_sealed_munitions_vault.png`.

### 12. Breached Avionics Lab — World 5

Room: `orbital_breached_avionics_lab`. Current borrowed image: `images/rooms/world_5/orbital_zero_g_junction.webp`.

**Identity:** A breached research chamber recognizable by a broken canopy, planet horizon and quantum-computer housings, rather than the junction's gyroscope.

**Composition:** View east from the western magnetic-clamp approach. A fractured canopy fills the far upper field above anchored server benches; a secured service-vault housing sits under fixed equipment.

**Light and palette:** Cold stellar blue and shield cyan against pale alloy, with restrained amber equipment lamps. Maintain the orbital ring's outlined mechanical language without repeating the junction's warm round hub.

**Permanent landmarks:** Shattered canopy frame, curved planetary horizon, distant shield corona, anchored mainframe banks and fixed service-vault enclosure. A few frozen coolant beads may occupy the depth planes as established atmospheric detail.

**State and content constraints:** No people, creatures, loose rewards, legible computer UI, gyro centerpiece, breathable drifting fog or wide-open new doorway into space. Do not paint the optional magnetic crossing as already repaired inside this room; the crossing belongs to its approach.

**Route context:** west → Zero-G Junction (`orbital_zero_g_junction`).

**Scene prompt:**

> A first-person view into a damaged orbital avionics laboratory where a fractured observation canopy opens onto the curved horizon of a planet beneath a distant cyan shield corona. Cold stellar light cuts across anchored quantum-mainframe housings and pale alloy benches, their small amber service lamps still lit. A few frozen coolant beads hang between reinforced canopy ribs and severed overhead conduits. A secured equipment enclosure sits partly concealed beneath the fixed server framework, while the clear central approach contrasts with the dark depth beyond the broken glass.

**Proposed image:** `world_assets/src/main/assets/images/rooms/world_5/orbital_breached_avionics_lab.webp`. PNG master: `output/imagegen/unique-room-backgrounds/orbital_breached_avionics_lab.png`.

## Review and acceptance before shipping

Compare each generated image with its original shared image and at least one approved same-world room. Check architectural identity first, then palette and line treatment. Reject merely decorative changes that still read as the original location.

For the salon/booth, inspect the base and solved murder descriptions together. For the Crucible, inspect pre- and post-champion prose. For the Pipe Gallery, check both locked and cleared east-route states. The fixed artwork must support both without painting changing actors or gates into place. Inspect the breach and drained-vault images against their unlocked access descriptions.

After visual review, export the twelve WebPs and update exactly twelve `background_image` fields. Decode-check exports, check each destination and PNG-master retention, and confirm every original room still points at its unchanged image. Re-run the global shared-path and image-file hash audit; expected outcome is 477 distinct room background paths, zero sharing groups and zero missing images. Check in-game crop/readability, transitions and actual loaded assets before claiming phone acceptance. Build and packaging verification follows any wiring; no gameplay tests are needed for these prose-only briefs.

The accompanying JSON contains all twelve briefs, current source descriptions/variants, neighbors, original owners, proposed destinations and source-record hashes so a future production pass can detect intervening content changes.
