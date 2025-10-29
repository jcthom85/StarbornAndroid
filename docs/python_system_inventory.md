# Starborn Python System Inventory

## Core Application Loop
- `Starborn_Python/game.py`: Kivy entry point wiring >20 subsystems (quests, dialogue, combat, crafting, tutorials), manages screen manager, overlays, environment theming, and device configuration.
- `Starborn_Python/main.py`: Android launcher configuring Kivy input/device options before booting `StarbornApp`.
- `Starborn_Python/world_manager.py`: Loads worlds/hubs/nodes/rooms JSON, instantiates `Room` objects, connects exits, tracks current location, and feeds theme selection.

## Domain Models & Systems
- `Starborn_Python/entities.py`: Defines combatants (`Character`, `Enemy`), buff/status logic, derived stat math, growth tables, and elemental enums.
- `Starborn_Python/game_objects.py`: Provides `Item`, `Bag`, and `Room` scaffolding used across inventory, encounters, and hub navigation.
- `Starborn_Python/constants.py`: Core tuning constants (accuracy, crit, growth multipliers, etc.) leveraged by combat, leveling, and UI.
- `Starborn_Python/quest_manager.py`, `Starborn_Python/milestone_manager.py`, `Starborn_Python/tutorial_manager.py`: Track quest progression, milestone flags, and tutorial triggers referenced by dialogue/events.
- `Starborn_Python/save_system.py`: Persists autosaves, manual slots, and migrates JSON structures.

## Gameplay Systems
- `Starborn_Python/combat.py`: Full battle engine with turn timelines, buffs/debuffs, elemental interactions, attack FX, and victory handling.
- `Starborn_Python/skill_tree_manager.py`, `Starborn_Python/data/leveling_manager.py`: Skill progression, XP curves, ability unlocks, resonance systems.
- `Starborn_Python/crafting_manager.py`, `Starborn_Python/ui/cooking_screen.py`, `Starborn_Python/ui/tinkering_screen.py`: Crafting/cooking/tinkering feature UIs and logic.
- `Starborn_Python/data/fishing_manager.py`, `Starborn_Python/ui/fishing_screen.py`, `Starborn_Python/ui/fishing_minigame.py`: Fishing minigame logic, timing windows, and UI.
- `Starborn_Python/audio_manager.py`, `Starborn_Python/audio_router.py`, `Starborn_Python/sound_manager.py`: Audio graph with routing, event-driven playback, ambient layers, and SFX metadata.

## Narrative & Events
- `Starborn_Python/dialogue_manager.py`, `Starborn_Python/dialogue_box.py`: Dialogue retrieval with condition parsing, Kivy dialogue box presentation, and trigger dispatch to events.
- `Starborn_Python/event_manager.py`, `Starborn_Python/cutscene_runner.py`, `Starborn_Python/cinematics.py`: Event scripting, cutscene sequencing, and cinematic playback.
- `Starborn_Python/narrative_flows.json`, `Starborn_Python/dialogue/*.json`: Narrative data used by dialogue/event systems.

## UI & Presentation
- `Starborn_Python/ui/hub_screen.py`, `Starborn_Python/ui/menu_popup.py`, `Starborn_Python/ui/radial_menu.py`, `Starborn_Python/ui/menu_overlay.py`: Hub/exploration UI, context menus, and overlays.
- `Starborn_Python/ui/combat_background.py`, `Starborn_Python/ui/combat_fx.py`, `Starborn_Python/ui/weather_layer.py`, `Starborn_Python/ui/shadow_label.py`: Visual FX layers, shaders, weather particles, and custom text rendering.
- `Starborn_Python/ui/victory_screen.py`, `Starborn_Python/ui/quest_popup.py`, `Starborn_Python/ui/narrative_popup.py`, `Starborn_Python/ui/theme_bands.py`: Post-combat feedback, quest notifications, narrative overlays, and thematic decorations.
- `Starborn_Python/font_loader.py`, `Starborn_Python/font_manager.py`, `Starborn_Python/fonts/*`: Custom font registration pipeline and font metadata.
- `Starborn_Python/theme_manager.py`, `Starborn_Python/environment.py`: Theme selection, palette swaps, environment effects, and shader routing.

## Assets & Data Stores
- Structured JSON stores in `Starborn_Python/*.json` (rooms, hubs, items, enemies, events, skills, dialogue, cinematics, milestones, quests).
- 2D art assets under `Starborn_Python/images/**` and `Starborn_Python/mirrored_images`, shaders in `Starborn_Python/shaders`, audio in `Starborn_Python/music` and `Starborn_Python/sfx`.
- Tooling scripts (`Starborn_Python/tools/*`, `Starborn_Python/development/*`) support content generation (starfield frames, image mirroring, editors).

## Testing & Utilities
- `Starborn_Python/test_starfield.py`, `Starborn_Python/test_weather.py`: Example pytest testbeds for graphical systems.
- `Starborn_Python/debug_cli.py`, `Starborn_Python/ui/debug_panel.py`: Developer tooling for inspecting runtime state.

This inventory informs the Kotlin port backlog: each category requires an Android equivalent (domain models, event systems, UI components, FX/shaders, audio routing, and save pipelines) to reach feature parity.
