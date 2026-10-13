# Desktop readiness pass - October 12, 2026

## Changes

- Fixed the silent title screen: the desktop was asking for an undefined
  `main_menu` room binding. It now requests the authored `music_title_theme`
  explicitly, stops exploration ambience, and restores the music layer.
- Exploration audio has one owner per controller. Recreating a controller after
  New Game or Load also recreates its audio collector; screens no longer
  forward the same commands twice.
- Audio output lines are selected using their decoded format. Stop invalidates
  music that is still loading, and duplicate requests do not restart a pending
  loop. Tests decode real assets through a controllable output line; these
  checks do not substitute for listening on real hardware.
- Exploration keys can be rebound in Settings > Controls, with conflict checks,
  persistence, and reset to defaults. Guides, HUD glyphs and tutorial hints use
  the current bindings. Combat and field-menu keys remain fixed.
- Q opens a numbered room-interaction chooser (Tab/Enter also work). Locked
  actions stay disabled. F9 loads the quicksave after confirmation and gives a
  clear message when no quicksave exists.
- Search fields receive numeric input before unmodified menu page shortcuts.
- HUD shortcuts and settings sections wrap. Compact navigation has readable
  labels, equipment lists use a smaller icon and the detail-pane Equip button,
  and short windows reserve space for quest status by limiting minimap height.
- Windowed bounds survive fullscreen/borderless changes. UI scale fits the
  available window space.
- Player settings hide diagnostic pitch testing unless debug mode is enabled.
  Mute when unfocused remains available. Exploration and hub settings support
  Save and quit to desktop. Quicksave HUD feedback reports the actual result.
- Solid loading colors bypass unnecessary native blur. Legacy snapshots use
  explicit viewport sizes and close their services; offscreen test classes run
  in separate JVMs to isolate native graphics state.
- Packaged smoke checks now decode the title MP3 in addition to opening every
  packaged asset.

## Verification

- Final desktop suite: **120 tests in 44 classes; 0 failures, 0 errors,
  0 skipped**. See `desktopApp/build/reports/tests/test/index.html`.
- Asset parity: **1,328 original assets** matched the staged and packaged bytes.
- `:desktopApp:packagePortableWindows` completed successfully using the local
  full JDK 21. The launcher is
  `desktopApp/build/distributions/Starborn-Windows-portable/Starborn/Starborn.exe`.
- The portable launcher ran `--smoke-check` from the Windows temporary directory,
  outside the checkout: **PASS**, loading 1,328 assets, 477 rooms and 4 characters,
  and decoding the title MP3 successfully with the packaged Java 21.0.12.1 runtime.
  Report: `desktopApp/build/reports/packaged-title-smoke.txt`.
- Reviewed fresh compact inventory/settings and short ultrawide exploration
  renders. Gear names, settings section labels, shortcut badges and the tracked
  quest remain readable.
- Audio tests use real file decoding and a controllable output line for fades,
  mute and cancellation. Audible playback is not established by those tests.

## Remaining release work

- Listen to title music, exploration ambience, combat transitions, and dialogue
  on an enabled Windows audio device. This environment cannot establish audible
  playback.
- Complete a packaged six-world playthrough, including defeat/recovery,
  optional stations, saved returns, and long-session memory/performance.
- Exercise native display switching and resizing on multiple monitors and
  high-DPI displays, plus installer upgrade/uninstall behavior.
- Controller support and combat-key rebinding need their own implementation.
- Long music still uses decoded Clips; streaming remains a future performance
  improvement if real-device profiling shows excessive memory or startup delay.
