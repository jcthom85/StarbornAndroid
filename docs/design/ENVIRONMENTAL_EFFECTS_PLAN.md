# Starborn environmental effects: design and implementation plan

Implementation and verification record: [environmental-effects/IMPLEMENTATION.md](environmental-effects/IMPLEMENTATION.md). The first delivery is implemented; physical-device acceptance and the listed later scope remain.

Status: proposed implementation plan; documentation only.
Date: 2026-10-08.
Platforms: Android and desktop together, as requested.

## 1. Direction and success criteria

Environmental effects are authored layers that make a place feel alive: weather, atmosphere, machinery, heat, organic activity, and unusual phenomena. Weather remains a category within this system.

The visual range must support barely noticeable activity, clearly present atmosphere, and exceptional dramatic events. Quiet rooms should remain quiet. Effects must belong to the pictured space and its current story state.

Success means:

- Android and desktop load the same original room data, preset definitions, conditions, and simulation behavior.
- A room can combine several effects without hardcoded platform-specific room rules.
- An emitter placed on a vent stays on that vent across portrait, desktop three-panel, compact, and combat layouts.
- Broad atmosphere reaches the desktop's entire screen, including the reflected sides. HUD, menus, authored action words, and notifications remain readable and clickable.
- Existing authored effects remain available during migration, and rooms with no authored effects stay clear.
- Effects react to existing room state and milestones without creating gameplay state or changing quest logic.
- Visual intensity, accessibility, performance, and transitions are predictable and reviewable.

First delivery includes room exploration and room-backed combat on both platforms. Hub and cinematic integration is a later explicit authoring pass; they do not inherit room effects automatically.

This document authorizes no implementation, content migration, new artwork, or game changes by itself.

## 2. Current implementation: verified facts

The current `Room` model has one optional `weather` string. `defaultWeatherForEnvironment` intentionally returns null, preventing environment names from putting outdoor weather into sealed rooms.

The 477 entries in `app/src/main/assets/rooms.json` contain:

| Authored value | Rooms |
|---|---:|
| No weather value | 231 |
| resonance | 91 |
| sparks | 30 |
| dust | 27 |
| fog | 20 |
| rain | 18 |
| steam | 12 |
| snow | 12 |
| cave_drip | 11 |
| gas | 9 |
| starfall | 6 |
| storm | 5 |
| industrial | 5 |

Thus 246 rooms have an authored value. Eleven values have Android renderers; `industrial` does not. Its five rooms are Astra Bridge, Common Room, Living Quarters, Cargo Bay, and Simulation Deck.

Relevant differences:

- Android has a single `WeatherOverlay` dispatcher supporting the eleven implemented values.
- Desktop exploration dispatches steam, fog, gas, and resonance separately from its other weather effects.
- Desktop combat uses only `DesktopWeatherOverlay`, which omits steam, fog, gas, resonance, and industrial.
- Storm intensity differs: Android's dispatcher uses medium; desktop uses high.
- Steam uses existing `images/vfx/steam_jet/steam_frame_%02d.png` assets on both platforms, with five fixed nozzle positions rather than room-authored emitters.
- Desktop exploration dims atmosphere to 15% in dark rooms; Android has a separate darkness overlay. These need coordinated treatment in the new integration.
- Existing room-state and milestone predicates already drive description/background variants. Effects should use the same matching semantics.

Source entry points: shared `Room`/`RoomPresentation`, Android `ui/vfx/WeatherOverlay`, and desktop `DesktopExplorationAtmosphere`/`DesktopVfx`. These are migration inputs, not separate future sources of truth.

## 3. Art direction

### Intensity vocabulary

Authoring uses five named levels, resolved to numeric intensity: Whisper 0.10, Subtle 0.25, Present 0.50, Strong 0.75, Extreme 1.00. Intensity modifies a preset's own envelope; Extreme does not mean unlimited particles or opaque white flashes.

Most rooms should use Whisper or Subtle. Present supports locations such as an exposed rainy market or operating machinery. Strong and Extreme belong to visibly hazardous or extraordinary spaces, with recovery intervals between dramatic events.

Intensity is an absolute authored level, not multiplied by the preset default a second time. It changes emission density and contribution within the preset's bounded ranges. Motion speed, physical size, and burst cadence have separate parameters; increasing density must not automatically make rain faster or vents larger.

### Composition rules

- Start with one ambient layer. Add a second layer only when it contributes a different visual behavior.
- A normal authored room has one or two active bindings. The first schema permits at most four bindings per room, including conditional bindings; burst emitters are part of their binding.
- Prefer irregular, seeded pauses over obvious repeating bursts. Give the eye quiet intervals.
- Steam, arcs, drips, and scanner beams need plausible origins. Never spray them from arbitrary screen edges to suggest activity.
- Preserve the original artwork palette. Use warm dust in industrial interiors, cooler precipitation, and room-specific organic or resonance colors.
- Do not add apparent leaks, fires, damage, or hazards to a room unless its artwork and authored setting support them.
- Environmental effects are cosmetic. They do not imply damage, visibility penalties, or interactable objects unless existing authored mechanics already provide those consequences.

### Library and priorities

| Effect/preset family | Whisper/Subtle use | Present/Strong use | Extreme use | Delivery |
|---|---|---|---|---|
| Dust / motes | Slow particles caught in light | Windblown dust, falling debris | Dense dust gusts | First library; reuse dust |
| Steam / vent mist | Occasional thin vent output | Steam jets, coolant releases | Large intermittent pressure releases | First library; reuse steam frames |
| Fog / haze / smoke | Thin low haze | Rolling mist or localized smoke | Dense moving smoke, bounded visibility | First library; reuse fog/gas primitives |
| Rain / storm | Sparse drizzle | Rain with wind and optional lightning | Heavy exposed storms | First library; reuse precipitation |
| Snow / frost particles | Few drifting flakes | Snowfall and icy vapor | Blizzard-like movement | Preserve snow; variants after pilots |
| Sparks / electrical arcs | Rare small sparks | Intermittent equipment discharge | Short large arcs with long rests | First library; sparks plus new arc primitive |
| Embers / ash | Few rising embers | Furnace drift, ashfall | Ember bursts and ash clouds | First library; mote variants |
| Spores / bioluminescence | Sparse slow luminous particles | Organic clouds, gentle growth pulses | Dense, slow luminous releases | First library; mote/light variants |
| Light pulse / flicker | Gentle equipment illumination | Emergency lighting and power instability | Bounded dramatic power events | First library; new light primitive |
| Scanner sweep / light shafts | Slow localized beam | Visible scanning band | Short dramatic projection sweep | First library; light variants |
| Resonance / gravity drift | Slightly unusual drift | Local pulses, suspended motes | Strong field waves and lifting particles | Preserve resonance; extend shared field/mote behavior |
| Cave drips / starfall | Original authored identity | Configurable density and placement | Used only where composition supports it | Preserve existing effects |
| Condensation / visible breath | Small localized droplets or wisps | Cold-space vapor | Larger cold releases | Later content/asset expansion |
| Heat shimmer / spatial distortion | Barely noticeable local distortion | Local heat or field ripples | Strong localized warping | Later renderer research and approval gate |

The first library contains all eleven working legacy effects, the missing industrial treatment, and the new mote, arc, and light variants above. True image distortion is deliberately a separate milestone: it needs an Android/desktop rendering prototype and performance review before a production technique is selected. Moving the entire scene is not an acceptable substitute.

## 4. Shared architecture

### Ownership and interfaces

Create a platform-independent environmental-effects package in `gameRuntime`. Keep Compose, Android graphics classes, desktop graphics classes, and native shaders out of this package.

The minimum interfaces are:

- `EnvironmentalEffectCatalog`: versioned preset definitions loaded from `environmental_effects.json` through the existing asset provider/data-source pattern.
- `RoomEffectBinding`: room-local identity, preset reference, intensity/placement overrides, and state/milestone conditions.
- `ResolvedEnvironmentalEffect`: validated renderer-ready configuration with a stable identity and visibility/accessibility policy applied.
- `EnvironmentalEffectResolver.resolve(room, roomState, milestones, context, settings)`: returns active bindings in authored order.
- `EnvironmentalEffectSimulation.advance(deltaSeconds, geometry)`: seeded, platform-independent particles, burst scheduling, and light/field envelopes.
- Android and desktop `EnvironmentalEffectsOverlay`: draw resolved simulation frames using the appropriate artwork transform and platform asset loader.

Shared primitives are precipitation, motes, haze, steam jets, sparks/arcs, light envelopes, and fields. Presets combine their parameters, rather than adding a new giant platform switch for every named variation. Legacy adapters remain only until their replacement passes comparison review.

The simulation owns mutable particle buffers and reusable frame output. Platform drawing reads it without allocating a new object graph or triggering full-screen Compose recomposition for every particle each frame. Bitmap/steam assets are loaded once and cached through existing loaders.

### Data flow

Room JSON + catalog + resolved room state + completed milestones
-> shared resolver
-> active bindings with stable IDs
-> per-binding shared simulation
-> platform draw adapter
-> artwork/viewport composition below game UI.

Exploration feeds its current resolved room-state snapshot. Combat captures the room identity and geometry context at encounter start, then uses that room's current authoritative state/milestones for condition reevaluation. Navigation changing a session room elsewhere must not retarget an encounter's effects.

Use one catalog instance per app services container, not per frame or overlay. Missing assets/presets do not prevent a room from loading.

## 5. Room authoring and compatibility

### Proposed room field

Add nullable `environmental_effects` to `Room`. Its absence means use the legacy `weather` adapter. An explicit empty array means no environmental effects. A nonempty array replaces legacy weather completely; never draw both paths.

Example of future authored data, not an edit to the current room:

```json
{
  "environmental_effects": [
    {
      "id": "lit_dust",
      "preset": "interior.dust_motes",
      "intensity": 0.10,
      "space": "scene",
      "requires_state": { "light_on": true }
    },
    {
      "id": "vent_release",
      "preset": "machinery.vent_mist",
      "intensity": 0.25,
      "space": "scene",
      "emitters": [
        { "x": 0.35, "y": 0.70, "angle_degrees": -15 }
      ]
    }
  ]
}
```

The vent coordinates above are illustrative. Actual positions must be chosen on the original artwork using the preview tool; do not paste them into Nova's Bunk.

Binding fields:

| Field | Meaning/default |
|---|---|
| `id` | Required, unique within a room; remains stable across content revisions |
| `preset` | Required catalog ID |
| `intensity` | Optional absolute 0..1 intensity override within the preset envelope; defaults to the preset intensity |
| `space` | `scene` or `viewport`; defaults to preset recommendation |
| `region` | Optional normalized rectangle `{x,y,width,height}` in the selected space; default entire space |
| `emitters` | Optional source list; required for source-bound presets that have no authored source |
| `requires_state`, `forbidden_state` | Boolean predicates against the current room's resolved state, matching existing room-variant semantics |
| `requires_milestones`, `forbidden_milestones` | All required and no forbidden milestones, matching existing variants |
| `in_combat` | Defaults true; authors may disable a binding during combat |
| `dark_visibility` | `dim` default, `emissive`, or `hidden` |

Emitters use normalized source position, angle in degrees, and optional normalized radius. Coordinates always refer to the original image or viewport specified by `space`; they never refer to a desktop side card.

The new catalog is an object with `schema_version: 1` and a `presets` object keyed by stable preset ID. Keep version 1 parsing explicit; unsupported versions fail validation and fall back to the still-available legacy path for rooms without new bindings.

Preset definitions specify primitive, category, default intensity, recommended space, RGBA tint, reference-height-relative particle size/speed, emission rate or burst interval range, lifetime, opacity envelope, and rendering priority. Colors use four 0..1 components, size uses fractions of reference image height, speed uses reference heights per second, rates use particles per second, lifetimes/intervals use seconds, and angles use degrees. Ranges are ordered two-number arrays. Support fixed values and bounded ranges, not executable expressions.

Priority is `ambient` or `accent`; both draw beneath actors/UI in the first delivery. Room array order breaks ties. Source-bound effects may use a region to restrict their plume or discharge.

### Resolution and validation rules

- Evaluate conditions before simulation. AND all requirements; a missing required state key does not satisfy a predicate, even when the requested value is false, consistent with existing room variants.
- Apply preset defaults, then binding overrides, then accessibility/performance limits.
- An authored list containing invalid bindings does not fall back to legacy weather. Skip invalid entries and report their room/binding IDs.
- Content validation fails on duplicate IDs, unknown presets/primitives, nonfinite/out-of-range values, invalid regions, missing mandatory emitters, more than four authored bindings, or unresolved assets.
- Release loading fails soft per binding and logs each problem once; it must not crash exploration. Unknown legacy weather is reported rather than silently appearing valid.
- Adding a new effect preset requires a preview fixture and registered primitive support on both platforms.

### Legacy adapter

Create explicit `legacy.<weather>` presets for the eleven supported values. Register `legacy.industrial` as a Whisper, scene-space mote treatment without fabricated vent/spark sources; reviewed Astra bindings replace it during pilot authoring. Use Android's established appearance as the reference for the eleven working effects. Preserve authored colors/assets, and standardize storm's default to Android's medium intensity on both platforms.

Keep the legacy `weather` field and `defaultWeatherForEnvironment` null behavior during migration. No mass search-and-replace or inferred effects from `env`.

Treat `industrial` as a known migration issue. Give its five Astra rooms explicit, restrained profiles after artwork review: light-catching dust in cargo, gentle console/projector illumination where pictured, and very low activity in living spaces. Do not equate it with damaged machinery or add generic steam/sparks.

Old save games require no migration: effects are resolved from existing room state and authored assets. Simulation phases, particles, and random seeds are not saved gameplay state.

## 6. Placement, rendering, and transitions

### Coordinate spaces

`scene` uses the original room image's normalized coordinates. Android uses its actual cover/crop transform; desktop uses the fitted portrait artwork rectangle. Crop/clipping, density, and window resizing are inputs to the transform, not alternate authoring coordinates.

`viewport` fills the entire drawable game viewport. Desktop rain, fog, and broad atmospheric fields extend across the center and reflected sides. One full-width simulation draws them; do not mirror particles independently into the blur panels.

A source-bound effect stays attached to the central artwork. Its plume may extend beyond the artwork into the full viewport unless an authored region clips it. Full-screen atmosphere therefore remains compatible with correctly placed room sources.

Reference particle size/speed is relative to image height, not desktop window width. Count density uses reference portrait area, with viewport expansion capped at 1.5x the reference budget so ultrawide monitors do not produce several times the particles.

### Layer order and readability

Use background/artwork and existing darkness treatment, environmental ambience/accents, room entities or combat sprites, action/combat effects, then HUD and modal notifications. Navigation blackout remains above everything.

Keep environmental render surfaces noninteractive and decorative in accessibility semantics. They must not intercept room action links, sprite selection, map clicks, or menus. No fog or glow is composited over text cards in the first delivery.

For new resolved profiles, `dim` effects contribute at 15% of their normal alpha in dark rooms; `hidden` contributes zero; `emissive` retains 35% within its bounded glow envelope. Integrate their ordering with the existing darkness layer so they are dimmed once, not twice. They must not reveal hidden entities or affect authored title/description concealment.

Combat uses the same active definitions and source mapping, with an additional 0.65 contribution multiplier and disabled large lighting events while timed prompts/modal tutorials are active. It must retain all supported families, including steam/fog/gas/resonance, rather than selecting a smaller dispatcher.

### Lifecycle

- Use a stable seed derived from room ID, binding ID, and catalog version. Preview can supply a fixed seed/time. Do not seed every platform from its current wall-clock time.
- Entering a room starts its simulation; same-room state changes retain unaffected binding phases. Fade changed bindings in/out over 350ms, with no extra pause in interaction.
- During existing room slides, source-bound effects follow the outgoing/incoming artwork transform. Viewport atmosphere stays spatially stationary and crossfades with the corresponding room layers.
- Hub/combat fades use the existing transition controller and timings; do not add another transition system or move the side reflections.
- Transition overlap holds at most two room scenes and shares the normal particle cap; discard outgoing resources after the transition.
- Resizing remaps existing normalized positions without reseeding or producing a fresh burst.
- Pause simulation when the app is backgrounded/minimized or a blocking menu/result overlay covers gameplay. Resume without catching up missed bursts.
- Clamp simulation delta to 50ms. Pool particles; dispose simulations and pending burst work on departure. Stable presets do not reload bitmaps when conditions change.

## 7. Accessibility and performance

Add the same `Environmental effects` setting on Android and desktop, persisted through their existing settings stores:

- **Full**: the authored appearance within limits; default. This does not force Extreme intensity.
- **Reduced**: half particle budget, 35% ambient movement speed, no large field waves or bright lighting events; localized low-opacity activity remains.
- **Off**: no environmental render/simulation work. Existing room artwork, story state, combat feedback, and game lighting remain functional.

Honor existing settings in addition:

- Disable flashes removes lightning, arcs' brightness pulses, abrupt flicker, and emergency flash events; precipitation/steady glow can remain.
- Disable screenshake removes distortion and rapid camera-like/large field motion. It does not silently remove gentle rain or dust.
- High contrast reduces haze contribution by half and retains sharp text/background separation; do not tint HUD text through an effect layer.
- Settings apply immediately without changing rooms or restarting an encounter.

Initial hard budgets: 240 active particles per room scene on Android and 360 on desktop, shared across bindings; Reduced halves these limits. Cap eight active steam sprites and two concurrent accent bursts. Priority keeps primary ambient identity before decorative accents; use authored order for ties. Transition scenes share, rather than double, the cap.

Use shared time math with platform-specific quality caps; visual identity must match even when counts differ. Cap procedural effect redraws at 60Hz, support slower target devices without accelerated simulation, and avoid per-frame bitmap decoding.

Profile the same scenes with effects Off and Full on a physical Android device, a representative desktop, and the lowest-performance supported test device available. For an effects-enabled scene to pass, p95 frame time may not regress by more than 10% relative to Off, with no unbounded allocation growth or work while backgrounded. If it fails, reduce preset budgets/overdraw before delivery; do not globally worsen art quality without reviewing the failing scene.

True distortion has a separate prototype gate covering Android support, desktop rendering, image alignment, reduced-motion behavior, and performance. Its production implementation is not part of this first-delivery commitment.

## 8. Authoring and preview workflow

Extend the existing debug weather selector into an environmental-effects preview. It must use the production resolver/simulation, not a separate demonstration renderer.

The preview supports:

- Original room artwork, current authored effects, and selected preset overrides.
- On-artwork emitter placement, region bounds, coordinate readout, and a full-viewport/scene-space toggle.
- Named intensity levels plus numeric adjustment, seeded pause/scrub, and burst replay.
- Android portrait and desktop wide/compact previews, darkness, state/milestone fixtures, combat attenuation, and accessibility modes.
- Active particle/burst counts and frame-cost/overdraw diagnostics in debug UI only.
- Copy/export of a JSON binding snippet for review. It does not silently overwrite campaign data.

Maintain a contact sheet and short video for each preset at Whisper, Present, and Extreme. Still screenshots cannot establish that burst timing and movement look good.

## 9. Room rollout

### Pilot scenes

| Room | Planned treatment | Review requirement |
|---|---|---|
| Nova's Bunk (`pit_nova_bunk`) | Whisper dust visible when `light_on` is true | Inspect the lamp/artwork; no new fog or fabricated hazards |
| Jed's Bunk (`pit_jed_bunk`) | Sparse warm motes only if they improve the room | Quiet baseline; NPC stays clear |
| Lift Shaft (`pit_shaft`) | Existing steam, with emitters authored against its pipes/grate | Replace generic nozzle placement with inspected coordinates |
| Night Market (`spire_night_market`) | Existing full-screen rain; optional localized stall steam only after artwork review | Full-width atmosphere, readable neon/action words, no arbitrary sources |
| Astra Bridge / Cargo / Quarters / Simulation Deck / Common Room | Room-specific subtle `industrial` replacements | Respect the different functions and pictured equipment |
| Subterranean Smelter Forge (`foundry_subterranean_smelter`) | Existing sparks plus restrained ember drift | Burst separation and combat readability |
| Toxic Pocket (`mine_gas`) | Existing gas with calibrated haze density | Preserve the room's authored danger and readable silhouettes |
| The Campfire (`source_campfire`) | Existing resonance with sparse unusual drift | Keep campfire identity; no permanent dramatic warping |
| Silent Shore (`source_orion_nightmare`) | Existing starfall, refined pacing | Keep the authored nightmare composition and original text |

A pilot addition is optional if the artwork cannot support it convincingly. Existing effects are mandatory to retain; additional layers are selected through the documented visual review, not invented from room names alone.

### Campaign audit

Audit all 477 rooms. Produce a room matrix listing current value, artwork source, intended intensity, source positions, active conditions, combat behavior, and keep/change/none decision.

First review all 246 currently authored rooms, prioritizing the five unsupported industrial entries, twelve steam rooms, five storm rooms, and combat paths missing families. Then inspect the 231 rooms with no effect for deliberate, selective opportunities. An entry of none is a valid finished decision.

Do not migrate all 91 resonance rooms to an extreme field effect. Their authored use must be calibrated individually. Do not rewrite room prose, quests, interactions, or assets during this effects pass.

## 10. Implementation milestones

1. **Baseline and catalog specification.** Capture Android/desktop motion references for every working legacy family; inventory all room bindings and steam assets. Lock the JSON schema and create compatibility/validation fixtures. Deliver room matrix and discrepancy list.
2. **Shared resolution and compatibility.** Add optional room bindings, catalog loading, resolver, legacy mapping, state/milestone matching, and settings contracts. Keep rendering behind debug preview until both platforms load the same resolved fixture data.
3. **Unified rendering and lifecycle.** Integrate shared simulation/geometry into both exploration and combat. Implement existing families, placement, dark visibility, pause/resume, transition ownership, noninteractive layers, and quality caps. Resolve current combat omissions and storm mismatch.
4. **Preview and first library.** Add authoring controls and new ember/ash/spore, arc, scanner, and light variants. Reuse steam/art assets. Deliver fixed-seed comparisons and motion references at three intensities.
5. **Pilot authoring.** Apply reviewed profiles to the pilot scenes and five Astra rooms on both platforms. Validate light-on and other existing state changes. Review portrait, three-panel, compact, and combat scenes before expanding.
6. **Campaign pass and release.** Complete the 477-room matrix, migrate reviewed rooms in small groups, and profile demanding combinations. Finish regression/accessibility checks; build Android and the regular Windows portable folder. Keep legacy compatibility for this release; no forced save migration or ZIP requirement.
7. **Later expansion.** Prototype true heat/spatial distortion, new cold-space assets, and authored hub/cinematic profiles. Optional synchronized environmental SFX is a separately authored follow-up. Existing room ambience/music stays the source of audio during the first delivery; no surprise sound loops accompany a cosmetic profile.

Each milestone ends with reviewable artifacts and passing checks before the next enables campaign content. Do not remove old renderers until every legacy family has a reviewed replacement on both platforms.

## 11. Verification and acceptance

### Automated checks

- Missing new field maps to legacy; empty array suppresses it; explicit bindings never duplicate it.
- Every catalog reference/asset and all 477 rooms validate, including industrial resolution and max-layer constraints.
- State/milestone matching mirrors current room-variant rules, with missing keys, forbidden predicates, and existing-save fixtures.
- Fixed seed and time yield matching simulation output across consumers; ordinary recomposition/state changes do not restart unaffected effects.
- Source transforms remain correct under Android crop, desktop portrait fit, compact drawers, resized windows, and combat backgrounds.
- All twelve legacy names either resolve correctly or have an explicit reviewed migration profile. Exploration and combat support the same registered families.
- Full/Reduced/Off, disable flashes, disable screenshake, high contrast, and dark visibility apply independently and together.
- Off/background states have no active particle simulation; return/resume does not dump accumulated bursts. Repeated navigation does not leak simulations/assets.
- Particle/sprite/burst caps also hold during room transitions and extreme combinations.
- Effects do not acquire pointer/focus semantics or block authored links, NPCs, sprites, arrows, minimap, or menus.

### Visual and motion review

Review at Android portrait, desktop 1440x900, 1280x720, 900x650, and ultrawide 2560x1080, plus a resized high-density window. Use Android/desktop captures at the same seed/time and compare motion recordings, not just stills.

Acceptance scenes include quiet interiors, rain, dense fog/gas, each steam room geometry, sparks/arcs, resonance, storm with flashes suppressed, a dark room before/after light-on, multiple active bindings, timed combat commands, victory overlays, and repeated room/hub/combat transitions.

Pass only when sources align with artwork, side atmosphere remains full-screen and stationary during travel, central images retain their aspect ratio, HUD/text/entities remain readable, dramatic events have rest intervals, and effect changes do not delay interactions.

### Delivery record

Document active profiles, room authoring decisions, deferred opportunities, supported primitives, asset origins, accessibility behavior, and measured frame costs. Ship Android and desktop in the same content increment. Report any file lock preventing replacement of the regular portable build rather than closing the player's running game.

## 12. Explicit defaults and boundaries

- Both platforms together; shared authoring/behavior, platform draw adapters.
- Authored opt-in; no automatic weather from environment names.
- First delivery: exploration and room-backed combat; hubs/cinematics later.
- Existing assets and original room/quest text are preserved. Additional raster assets require their own design step when genuinely needed.
- Full is the default setting, with restraint set by each preset and room. Most pilot additions are Whisper or Subtle.
- No gameplay damage, random world-weather scheduler, time-of-day simulation, audio rewrite, or save-schema migration in the first delivery.
- Distortion and new audio synchronization are explicit later work, not hidden dependencies of the initial system.
- This document records the approved scope; implementation is described in the linked delivery record. Changes are not automatically committed or pushed.
