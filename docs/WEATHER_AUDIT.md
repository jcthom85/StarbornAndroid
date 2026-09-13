# Weather and Environmental Overlay Audit

Date: 2026-09-13

## Implementation result

The audited assignments are now implemented. Campaign environment fallbacks
were removed so sealed rooms no longer inherit particles, and explicit effects
were assigned only to reviewed exposed, wet, industrial, hazardous, or Source
spaces. The resulting explicit coverage is:

| World | Rooms with effects | Effect totals |
| --- | ---: | --- |
| 1 | 9 / 88 | cave drip 1, dust 3, gas 1, resonance 1, snow 1, steam 2 |
| 2 | 59 / 91 | cave drip 4, fog 19, resonance 36 |
| 3 | 25 / 65 | cave drip 5, rain 13, steam 2, storm 5 |
| 4 | 58 / 71 | dust 14, gas 7, sparks 29, steam 8 |
| 5 | 35 / 80 | resonance 24, snow 11 |
| 6 | 47 / 65 | dust 9, rain 2, resonance 30, starfall 6 |

`WeatherCatalogIntegrityTest` now rejects unsupported campaign effect IDs,
protects a curated set of sealed interiors, requires representative audited
assignments, and confirms campaign environments do not infer weather. Visual
acceptance on a device remains required for particle density, contrast, motion
sensitivity, and performance.

## Scope and method

This audit compares all 465 authored room records and 214 unique World 1-6 room
background references against room prose, physical exposure, audio ambience and
the runtime effects supported by `WeatherOverlay`: `dust`, `rain`, `storm`,
`snow`, `cave_drip`, `starfall`, `steam`, `fog`, `gas`, `resonance`, and
`sparks`.

An overlay is appropriate only when the effect occurs in the player's immediate
space. Rain visible through a sealed window is not a reason to draw rain over an
interior. Likewise, standing water, lava, stars, or a bright machine in the
painted background do not automatically require a particle overlay.

## Structural findings

| World | Room records | Unique backgrounds | Explicit weather | Effective weather |
| --- | ---: | ---: | ---: | ---: |
| 1 | 88 | 81 | 8 | 66 |
| 2 | 91 | 91 | 91 | 91 |
| 3 | 65 | 12 | 0 | 0 |
| 4 | 71 | 10 | 0 | 0 |
| 5 | 80 | 9 | 0 | 0 |
| 6 | 65 | 11 | 0 | 0 |

World 1's effective count is inflated by environment defaults: every `mine`
room receives dust, every `logistics` room receives resonance, and every `space`
room receives starfall. World 2 explicitly assigns fog or resonance to every
room, including sealed interiors. Worlds 3-6 receive no effects because their
`neon`, `foundry`, `void`, and `source` environments have no defaults.

The preferred correction is explicit per-room weather. Environment defaults are
too broad for authored areas that mix exterior, sheltered, and sealed spaces.

## World 1 - Deep Mine and launch complex

Current status: over-applied. Dust currently appears throughout bunks, kitchens,
medical rooms, offices, cells, and other sealed interiors. Resonance appears
throughout the launch complex, and starfall appears in several sealed server and
Echo rooms solely because of environment defaults.

Recommended definite assignments:

| Effect | Rooms or room groups | Reason |
| --- | --- | --- |
| `dust` | `pit_L1_landing`, `workshop_yard`, `mine_sifter` | Explicitly authored dusty work/exterior spaces. |
| `steam` | `pit_shaft`, `pit_showers` | Visible and described steam in the immediate room. |
| `snow` | `server_cooling` | Ice/snow is an immediate cooling-system hazard. |
| `cave_drip` | `mine_landing`; consider other visibly wet mine chambers individually | Local underground moisture, not a global mine default. |
| `gas` | `mine_gas` | Explicit gas chamber. |
| `resonance` | `echo_heart`; story-active Echo/launch rooms only when the prose says the field is present | Supernatural field, not a permanent logistics ambience. |
| `starfall` | Only genuinely exposed cosmic/Source-facing rooms after device review | Never sealed server rooms. |

Remove implicit dust from living, workshop, medbay, checkpoint, and ordinary mine
interiors unless their individual art visibly contains airborne dust. Remove
implicit resonance from ordinary launch rooms and implicit starfall from
`server_backup` and `server_crawl`.

## World 2 - Sector 9

Current status: over-applied. Every room has an explicit overlay. Fog is credible
across the outdoor crash, landing, stream, beach, and exposed jungle routes, but
not across pod interiors, sealed ruins, conduits, stasis rooms, machinery rooms,
or the hangar. Resonance is credible where the environment is visibly or
narratively reacting to the Source.

Recommended groups:

| Effect | Rooms or room groups | Reason |
| --- | --- | --- |
| `fog` | Exposed crash-site, landing, stream-bank, wetland, beach, jungle, and ridge rooms | Humid exterior atmosphere. |
| `cave_drip` | Cave depths, sunken passages, grotto interiors, and other dripping enclosed natural spaces | Local water instead of full-screen outdoor fog. |
| `resonance` | Resonant canopy/wilds states, Hall of Echoes, Archive tuning spaces, Source Gate and emitters, and sky-barrier sequence | Explicit Source or resonance activity. |
| none | Pod interior; temple/foyer interiors without active resonance; conduit, stasis, vent, power, and hangar interiors | Sealed rooms should not inherit exterior fog. |

The existing World 2 assignments must be split by exposure; retaining fog on all
facility rooms would preserve the same category error found in World 1.

## World 3 - Corporate Spire

Current status: missing effects. Rain is painted into several backgrounds and is
explicit in prose and ambience, but no World 3 room receives a weather overlay.

Recommended groups:

| Effect | Rooms or room groups | Reason |
| --- | --- | --- |
| `cave_drip` | `spire_sewers_landing`, `spire_runoff_lock`, `spire_sewers_passage`, wet pump/filter scale rooms | Enclosed drainage spaces; do not use outdoor rain. |
| `rain` | `spire_vent_output`, `spire_rain_gutter_alley`, `spire_underrail_platform`, exposed Transit Plaza routes, `spire_night_market`, `spire_noodle_row`, `spire_lantern_bridge`, `spire_safehouse_roof`, exposed Exec Lounge areas | Rain is visible in the art or explicitly authored around the player. |
| `storm` | `spire_landing_pad_roof` and its exposed approach rooms | Heavy rain plus violent wind at the finale pad. |
| `steam` | `spire_laundry_service`, steam-rack/pressurized-water subrooms | Immediate industrial steam. |
| none | Static bar/subway interior, Zeke's apartment, clinic, security kiosk, service interiors, Skypark Dome, Archive/Prism interiors | Rain is outside glass, artificial scenery, or absent. |

Room-level assignments are required because several backgrounds are reused for
both exposed and interior subrooms.

## World 4 - Foundry

Current status: missing environmental overlays, though conventional weather is
not the main need. The art calls for industrial particles rather than rain.

| Effect | Rooms or room groups | Reason |
| --- | --- | --- |
| `dust` | Exposed slag landing, obsidian overlooks, slag-river approaches | Ash/mineral particulate in open volcanic spaces. |
| `steam` | Cooling Springs, coolant bypasses, decon chamber | Water and coolant meeting extreme heat. |
| `gas` | Waste Intake and explicitly described chemical-runoff pockets | Toxic industrial atmosphere. |
| `sparks` | Active conveyor/reject machinery, Forge Anvil, damaged power machinery, Titan Dock action spaces | Immediate hot machinery and metal work. |
| `resonance` | Power Core only while resonance is narratively active | Avoid treating ordinary reactor glow as weather. |
| none | Service airlock, conditioning rooms, observation rooms, and inactive control spaces | Sealed or visually clean interiors. |

## World 5 - Orbital Ring

Current status: mostly correct with no overlay, but a few explicit effects would
support the authored hazards. A starfield visible through glazing is not falling
weather inside the station.

| Effect | Rooms or room groups | Reason |
| --- | --- | --- |
| `snow` | Exposed/failing Server Farm coolant rows and ice-coated subrooms | The background visibly contains frost and snow accumulation. |
| `sparks` | Breached service-shaft or security subrooms only when damage is active | Local electrical damage, not a Ring-wide effect. |
| `resonance` | Deep Anchor Chamber, Throne Room, and Tear sequence when the fracture is active | Source fracture is immediate and narratively active. |
| `starfall` | Exterior/open-vacuum transition only if the overlay reads correctly on device | Do not draw it over docks, concourses, solarium glass, or sealed rooms. |
| none | Executive Dock interior, Grand Concourse, Solarium, Security Hub, ordinary Service Shaft, and intact station interiors | Space is outside the pressure envelope. |

## World 6 - Source

Current status: missing deliberate surreal ambience. This world should use
effects as authored memory-state cues, not as literal global weather.

| Effect | Rooms or room groups | Reason |
| --- | --- | --- |
| `resonance` | Campfire platform, Memory Bridge, Memory Stair, Spire Thought, Center, and active finale nodes | Source energy occupies the immediate space. |
| `dust` | Echo Mines/workbench memory rooms where mine dust is explicitly present | Local memory material. |
| `rain` | The explicit Spire-rain bridge/stair nodes only | A localized recalled weather line, not the whole shared background. |
| `starfall` | Orion's open nightmare shore or exposed Source-sky transitions, subject to device review | Cosmic ambience in an open surreal space. |
| `sparks` | Root-spark and damaged-memory nodes only | Specific authored activity. |
| none | Office nightmare, sealed combat corridors, elevator interior, and calm New World/diner scenes | Keeps the ending readable and avoids constant particle noise. |

## Implementation and regression plan

1. Remove `mine`, `logistics`, `space`, and `swamp` as automatic weather sources
   for authored campaign rooms, or restrict the fallback to legacy/debug rooms.
2. Add explicit weather to the definite groups above. Do not infer effects only
   from a shared background filename.
3. Add a catalog test that rejects weather on a curated sealed-interior denylist
   and requires it on a curated exposed/hazard allowlist.
4. Validate that every authored weather ID is supported by `WeatherOverlay` and
   has an audio binding where appropriate.
5. Device-test representative rooms for density, contrast, motion sensitivity,
   and performance before extending ambiguous assignments.

The first implementation batch should cover the unambiguous defects: remove fog
from sealed World 2 interiors, add World 3 exterior rain/storm and sewer drips,
and stop World 1 environment defaults from placing particles in sealed rooms.
Worlds 4-6 should follow as a separate environmental-effects pass because those
choices are more about authored industrial/Source ambience than literal weather.
