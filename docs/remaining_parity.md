# Starborn Port – Remaining Conversion Scope

## High-Priority Gameplay Systems
- **Exploration overlays & interactions**: Direction ring, minimap highlights, hotspot tray, tutorial ribbons, blocked-exit cinematics, and quest journal detail pane are in place; remaining polish: hotspot art/icon passes and navigation breadcrumbs for zoomed-map view.
- **Combat presentation**: FX, ally support cues, and defeat/retreat cinematics landed; polish burst timing, enemy telegraphs, and post-battle scripting to reach Kivy fidelity.
- **Dialogue & scripted events**: Branching choices display quest/tutorial/milestone badges, portraits resolve via metadata, and voice cues fire; cinematic sequencing and portrait asset alignment still require work alongside scripted choreography (`cinematics.py`, `cutscene_runner.py`).
- **Quests/Milestones/Tutorials**: Quest log detail pane, milestone history ribbon, and tutorial queue are active; scripted auto-prompts now leverage reusable tutorial scripts, and the milestone gallery is available from the services menu. Remaining work: polish journal integration and late-game tutorials.
- **Inventory-adjacent systems**: Crafting/tinkering is live; cooking, first-aid, and fishing minigames now expose tuned timing flows with FX/audio cues. Shop loops still need rotating stock logic plus authored vendor dialogue branches.
- **Skill trees & leveling**: `skill_tree_manager.py`, `data/leveling_manager.py`, and the associated UI/minigames are missing; need level-up ribbon, skill unlock panels, and resonance progression screens driven by `LevelingManager`.
- **Audio & theme management**: `sound_manager.py`, `audio_router.py`, `theme_manager.py`, `environment.py` drive ambience, music, SFX priorities, palette swaps, shaders; Kotlin now plays spot cues but still lacks layered ambience/music mixing and theme swaps.
- **Save/Load**: `save_system.py` provides autosave/manual slots with JSON migrations. Kotlin build now supports autosave cadence, legacy JSON import, and slot UI; remaining work covers quest/milestone migration fidelity and cloud/backup strategy.

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
