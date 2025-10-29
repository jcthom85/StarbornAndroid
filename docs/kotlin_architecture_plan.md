# Kotlin/Android Architecture Blueprint

## Objectives
- Achieve feature parity with the Python/Kivy build while embracing Android best practices (Compose, coroutines, Jetpack libraries).
- Maintain data-driven content (JSON assets) and reusable domain logic across UI surfaces.
- Support layered rendering (FX, shaders, overlays) and responsive layouts for phones/tablets.
- Enable incremental delivery (room navigation → dialogue → combat → hubs → side systems) with robust test coverage.

## Module Layout
1. **:`core` (Kotlin library)** – Shared utilities, math helpers, resource loaders, serialization adapters, logging.
2. **:`domain` (Kotlin library)** – Clean-domain models (Room, Hub, Character, Enemy, Item, Buff, Quest, Milestone, DialogueNode, EventScript), business services (WorldGraphService, CombatEngine, DialogueService, QuestTracker, SaveRepository interfaces).
3. **:`data` (Android/Kotlin library)** – Asset-backed repositories (JSON ingestion via Moshi/Kotlinx), persistence (DataStore/Room for saves), audio asset catalogs, shader descriptors. Implements `domain` interfaces.
4. **:`ui` (Android Compose library)** – Reusable UI toolkit: theming, typography, pixel-art scaling, Compose components mirroring Kivy widgets (BorderedFrame, RadialMenu, TimelineMeter, Minimap, WeatherLayer, DialogueBox, Popup framework).
5. **:`feature-*` (Compose libraries)** – Scoped feature stacks (exploration, combat, hubs, inventory, crafting, fishing, skill tree). Each exposes `@Composable` screens and ViewModels, depends on `domain` & `ui`.
6. **`app` (Android application)** – NavHost, DI wiring (Hilt/Koin), lifecycle management, audio routing, configuration overrides.

> Initial milestone can keep a single Gradle module (`app`) while structuring packages per above; migrate to true multi-module once baseline functionality is stable.

## Dependency Injection & State
- Use **Hilt** for DI: provides `WorldRepository`, `CombatEngine`, `AudioRouter`, etc., with custom qualifiers for asset contexts.
- Compose UI backed by **ViewModels** (AndroidX) + **StateFlow**; feature modules expose immutable UI state models.
- Cross-feature game state (current room, inventory, active quests) managed via a `GameSessionStore` (flow-backed, persisted).

## Data & Persistence
- Parse legacy JSON with **Moshi** + adapters to map to domain models; support polymorphic fields (actions, requirements).
- Save system: prefer `Room` (SQLite) or `Proto DataStore` for structured saves mirroring Python `SaveSystem`. Provide migration layer to import existing JSON saves.
- Asset packaging: store JSON under `assets/`, images under `res/drawable-nodpi`, audio in `res/raw` or `assets/audio`.

## Gameplay Services
- **WorldGraphService**: builds node graph, resolves blocked exits, updates room state, emits navigation events.
- **EventEngine**: interprets scripted triggers (`on_enter`, dialogue triggers, milestone checks), uses coroutine queues to sequence actions.
- **DialogueService**: condition parser, branching resolution, interface to UI dialogue box.
- **CombatEngine**: turn queue, timing windows, buffs/debuffs, elemental math, XP/loot distribution. Provide deterministic simulation for tests.
- **QuestTracker / MilestoneManager / TutorialManager**: track progress, emit UI cues.
- **AudioRouter**: orchestrates music/SFX layers with focus/ducking rules.

## UI & Rendering
- Compose `Navigation` with top-level graph: splash → main menu → game shell (Drawer or layered NavHost).
- Implement pixel-perfect scaling via `Modifier.graphicsLayer` and `Canvas` for backgrounds; centralize sprite loading and scaling factors (equivalent to Kivy `ScalableFloatLayout`).
- Recreate effects:
  - Vignette & shader overlays via **AGSL shaders** or `RenderEffect`.
  - Damage numbers & FX using `AnimatedContent`, `LaunchedEffect`, and custom `Canvas`.
  - Weather/particles with low-level `Canvas` drawing + `rememberInfiniteTransition`.
- Use `LayerHost` pattern for stacking: Background → Environment FX → Room interactables → UI overlays → Dialogue/Popup surface.

## Audio Strategy
- Load short SFX with **SoundPool**, longer ambience/music with **ExoPlayer**.
- Mirror `AudioRouter` priorities: battle, ambience, UI, voice. Provide coroutine-based fade and looping utilities.
- Store routing metadata in JSON (similar to `sfx.json`, `audio_bindings.json`) parsed in `data`.

## Testing
- Unit tests in `domain` for combat math, dialogue conditions, quest progression.
- Snapshot tests for Compose components via `Paparazzi` or `Compose UI Test`.
- Instrumentation end-to-end flows for navigation and save/load.

## Tooling
- Gradle tasks to validate JSON schemas (use Moshi codegen or custom validator).
- Debug overlay toggled via `BuildConfig.DEBUG` replicating Python `DebugPanel`.
- Content pipeline scripts (existing Python tools) can be wrapped or called via Gradle to regenerate assets until Kotlin replacements exist.

This blueprint enables staged delivery: we can stand up `domain` logic and tests first, expose data ingestion, then layer UI features while re-implementing audio/visual polish iteratively.
