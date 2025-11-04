# Starborn Port – Remaining Conversion Scope

## High-Priority Gameplay Systems
- **Exploration overlays & interactions**: Direction ring, minimap highlights, service tray, quick radial menu, milestone bands, hotspot art/animations, weather/vignette shaders, and quest journal overlay are in place; remaining polish: zoomed-map legend, long-form room lore tooltips, and accessibility sizing for touch targets.
- **Combat presentation**: FX, ally support cues, defeat/retreat cinematics, and weather/theme overlays landed; polish burst timing, enemy telegraphs, and post-battle scripting to reach Kivy fidelity.
- **Dialogue & scripted events**: Branching choices display quest/tutorial/milestone badges, portraits resolve via metadata, and launch events now mirror Python (intro cinematic, Ollie arrival, Nova light switch). Remaining: late-game encounter choreography, ally recruitment arcs, and combat epilogue cinematics.
- **Quests/Milestones/Tutorials**: Quest journal overlay, milestone gallery timeline, and tutorial queue are active; remaining work: late-game tutorials, per-node quest breadcrumbs, and hub-summary journal cards.
- **Inventory-adjacent systems**: Crafting/tinkering is live; cooking, first-aid, and fishing minigames now expose tuned timing flows with FX/audio cues. Shop loops include rotating stock and smalltalk scripting; still pending: equipment comparison overlays and bulk-buy shortcuts.
- **Skill trees & leveling**: `skill_tree_manager.py`, `data/leveling_manager.py`, and the associated UI/minigames are missing; need level-up ribbon, skill unlock panels, and resonance progression screens driven by `LevelingManager`.
- **Audio & theme management**: `sound_manager.py`, `audio_router.py`, `theme_manager.py`, `environment.py` drive ambience, music, SFX priorities, palette swaps, shaders; Kotlin blends hub music + ambient/weather layers, milestone stingers, and exposes in-game volume sliders, but still lacks theme swaps, full voiceover routing, and combat finale mixing.
- **Save/Load**: `save_system.py` provides autosave/manual slots with JSON migrations. Kotlin build now supports autosave cadence, legacy JSON import, and slot UI; remaining work covers quest/milestone migration fidelity and cloud/backup strategy.

## Supporting Infrastructure
- **Shader/VFX layers**: Python uses custom shaders (`vfx.py`, `ui/weather_layer.py`, `ui/combat_fx.py`). Android needs AGSL/RenderEffect implementations and Compose-compatible layering.
- **Minimap, radial menu, overlays**: Minimap and service tray exist in Compose; remaining: theme bands, vendor-specific overlays, and zoomed-map legend tooling.
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
