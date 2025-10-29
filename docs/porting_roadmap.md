# Starborn Kotlin Port Roadmap

## Phase 0 – Project Setup (Week 0)
- Split packages by layer (`core`, `domain`, `data`, `ui`, `feature.*`) within `app` module; enable Hilt, Moshi, Kotlin serialization.
- Migrate JSON assets/fonts/audio into `app/src/main/assets` & `res` with normalized naming; add validation script.
- Establish shared `GameSessionStore` skeleton and baseline unit-test harness.

## Phase 1 – World Exploration Core (Weeks 1-3)
- Implement `WorldRepository` to load worlds/hubs/nodes/rooms with blocked exit logic.
- Port inventory (`Item`, `Bag`) and room state tracking (actions, state flags, enemy placement).
- Build Compose navigation shell with layered rendering: background, room hotspots, HUD, dialogue stub.
- Implement `DialogueService` and Compose `DialogueOverlay` using existing JSON; support triggers → EventEngine stub.
- Deliver walking flow: load save → hub → node map → room transitions with dialogue popups and inventory pickup.

## Phase 2 – Combat & Progression (Weeks 3-6)
- Port combat domain (turn queue, stats, buffs, resistances) with deterministic unit tests.
- Compose combat UI (timeline, actions, FX placeholders) and integrate `CombatEngine`.
- Implement XP/AP rewards, leveling curves, loot drops, and victory/defeat flows.
- Hook combat launches from room encounters and blocked exits.

## Phase 3 – Systems & Overlays (Weeks 6-9)
- Quest/Milestone/Tutorial managers with Compose popups; connect to Dialogue/Event triggers.
- Crafting, Cooking, Tinkering, Fishing minigames: implement logic services and Compose screens.
- Hub/Shop interfaces, radial menu, inventory management, and system tutorials.
- AudioRouter implementation (music layers, ambient loops, combat stingers).

## Phase 4 – Visual & Audio Fidelity (Weeks 9-11)
- Recreate shaders/VFX (vignette, weather, damage numbers, timeline) using AGSL and custom Canvas composables.
- Theme & environment transitions (palettes, overlays), responsive layout polish, accessibility checks.
- Voice/SFX mixing, crossfades, and runtime audio settings menu.

## Phase 5 – Persistence, QA, Packaging (Weeks 11-12+)
- Save/Load parity with autosave slots, migration path from legacy JSON.
- End-to-end UI tests, load testing for large JSON, performance profiling.
- Google Play signing setup, beta build generation, analytics/debug tooling.

## Backlog & Risks
- Shader parity: verify AGSL coverage for Kivy fragment shaders; may require fallback effect pipeline.
- Asset memory footprint: confirm Compose + large PNG pipeline performance; consider texture atlas generation.
- Minigame physics/audio sync: ensure frame timing under 60fps; might need bespoke render loops.
- Tooling integration: decide whether to port Python content generators or embed them as external steps.

## Immediate Next Sprint Backlog
1. Scaffold `core`, `domain`, `data`, `ui`, `feature.mainmenu`, `feature.exploration`, `feature.combat` packages inside `app`.
2. Implement Moshi models for Worlds/Hubs/Nodes/Rooms/Characters/Enemies/Skills, including adapters for optional fields.
3. Prototype `GameSessionStore` (StateFlow + SavedStateHandle) and load initial world graph on app launch.
4. Replace `StarbornUI()` placeholder with layered Compose structure showing room background + basic HUD.
5. Add unit tests validating JSON parsing against sample assets (`worlds.json`, `rooms.json`, `enemies.json`).
