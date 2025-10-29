# Starborn Port – Remaining Conversion Scope

## High-Priority Gameplay Systems
- **Exploration interactions**: current Compose shell shows NPC/action lists but lacks dialogue popups, item pickup logic, inventory UI, and interactive map hotspots.
- **Combat engine**: Kotlin ViewModel drives a stub loop; needs full turn logic (buffs, timing, resistances, elemental math), FX overlays, victory/defeat flows, rewards, and integration with leveling/AP credit systems.
- **Dialogue & events**: Python `dialogue_manager.py`, `event_manager.py`, `cinematics.py`, `cutscene_runner.py`, and story JSON (dialogue/events/narrative flows) are not yet ported. Triggers, branching, quest hooks, and cinematic playback must be recreated.
- **Quests/Milestones/Tutorials**: Managers from `quest_manager.py`, `milestone_manager.py`, `tutorial_manager.py` with their UI surfaces (quest popup, theme bands, tutorial overlays) remain unimplemented.
- **Inventory/Crafting/Cooking/Tinkering/Fishing**: Feature screens and logic from `game.py`, `crafting_manager.py`, `ui/tinkering_screen.py`, `ui/cooking_screen.py`, `ui/fishing_screen.py`, etc., need Compose counterparts plus data services.
- **Skill trees & leveling**: `skill_tree_manager.py`, `data/leveling_manager.py`, and the skill tree UI/minigame are missing. Need to port progression graphs, resonance, and talent unlock screens.
- **Audio & theme management**: `sound_manager.py`, `audio_router.py`, `theme_manager.py`, `environment.py` drive ambience, music, SFX priorities, palette swaps, shaders; Kotlin only loads static assets today.
- **Save/Load**: `save_system.py` provides autosave/manual slots with JSON migrations. Android needs a persistence layer (Room/DataStore) plus save-slot UI.

## Supporting Infrastructure
- **Shader/VFX layers**: Python uses custom shaders (`vfx.py`, `ui/weather_layer.py`, `ui/combat_fx.py`). Android needs AGSL/RenderEffect implementations and Compose-compatible layering.
- **Minimap, radial menu, overlays**: Widgets like `ui/minimap_widget.py`, `ui/radial_menu.py`, `ui/menu_overlay.py`, `ui/theme_bands.py`, `ui/bordered_frame.py` must be redesigned in Compose.
- **Tooling & editors**: Python repo includes editors/generators (item editor, milestone editor, starfield generator). Decide whether to port, reimplement in Kotlin, or leave as external tools.
- **Localization & typography**: Font loader/manager (`font_loader.py`, `font_manager.py`) ensure pixel-perfect text. Android needs theme-aware typography, fallback fonts, and pixel rounding.

## Data Validation & Testing
- JSON schema validation, unit tests for combat/quests/dialogue, and instrumentation tests for navigation/save flows need to be added (Python relies on manual QA plus limited pytest scripts).
- Continuous integration build/test flow for Android is not yet configured.

## Asset & Performance Considerations
- Verify memory footprint and scaling for large PNG backgrounds and pixel-art assets.
- Audio streaming/mixing strategy (SoundPool/ExoPlayer) to mirror Python’s layered router.
- Content migration: ensure existing saves can be imported or provide conversion tooling.

## Delivery Checklist
- Implement missing systems above.
- Replace placeholder UIs with production Compose components.
- Build QA plan: automated tests + manual checklists for main loops (new game, combat, quests, crafting, hub navigation).
- Optimize and polish for mobile (performance, orientation, screen sizes).
- Package for release (app icons, signing, Play Store metadata) once gameplay parity is achieved.
