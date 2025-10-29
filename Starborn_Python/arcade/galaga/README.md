# Neo-Galaga (Starborn Arcade)

This package contains a portrait-first, Galaga-inspired mini game that can be embedded anywhere inside Starborn.  All assets are self-contained under `arcade/galaga` and generated at runtime when needed.

## Highlights
- Faithful 9:21 playfield with auto letterboxing (`PortraitStage`).
- Dynamic starfield, wave escalation, boss capture mechanic, and challenging stages every fourth wave.
- Original synthesized audio cues stored in `assets/audio` (generated on first run).
- Helper `build_galaga_screen()` for effortless `ScreenManager` integration.

## Controls
- **Drag / hold** anywhere on the playfield to move the striker horizontally.
- **Hold** to fire (auto-firing with dual ship upgrade).
- Bosses can capture the player; destroy them to reclaim the twin-ship upgrade.

## Integration
1. Import and build the screen:
   ```python
   from arcade.galaga import build_galaga_screen

   galaga_screen = build_galaga_screen()
   screen_manager.add_widget(galaga_screen)
   ```
2. Switch to the screen when appropriate (e.g., from Sam & Ellie’s arcade cabinet).
3. Call `galaga_screen.reset_game()` to restart or `galaga_screen.toggle_pause()` to pause/resume.

## Testing
Run `python -m compileall arcade/galaga` to verify syntax, or instantiate `GalagaGame` directly inside a sandbox app for targeted testing.
