# Starborn Windows private builds

## Build

Use a full JDK 21 containing `jpackage`. Android Studio's bundled JBR works for
compilation but does not contain the Windows packaging tools.

If Gradle requires the Android Studio toolchain for compilation, keep it as
`JAVA_HOME` and select the packaging JDK separately:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$packagingJdk='C:\path\to\full-jdk-21'
.\gradlew.bat :desktopApp:packagePortableWindows "-Pstarborn.packagingJavaHome=$packagingJdk"
```

```powershell
$env:JAVA_HOME='C:\path\to\jdk-21'
.\gradlew.bat :desktopApp:packagePortableWindows :desktopApp:packageMsi :desktopApp:packageExe
```

The portable app folder is `desktopApp/build/distributions/Starborn-Windows-portable/Starborn`.
Launch `Starborn.exe` inside that folder; keep its companion files together. Installers are in
`desktopApp/build/compose/binaries/main`. Extract the entire portable archive
before launching `Starborn/Starborn.exe`. The launcher includes its Java runtime.
The game assets are packaged with the application; the repository is not needed.

## Saves

Desktop saves and settings live in `%USERPROFILE%\.starborn`.
Manual slots, quick save and autosave use the shared protocol format.
Existing JSON slots are imported on first access. Original JSON files and a
`.pre-migration.bak` copy are retained. Once a protocol save exists, it takes
precedence. Explicit deletion removes the playable protocol and legacy slot.

## Controls

- Exploration defaults: WASD / arrows to move, E for the first available room action,
  Q for a numbered interaction chooser, Esc for the field menu, I for inventory,
  M for the field map, H for help, F5 for quick save, and F9 to load the quicksave
  after confirmation. Change exploration keys in Settings > Controls; the HUD
  and guide follow the configured keys. Arrow keys, Esc, Tab and Enter remain
  available for navigation.
- Combat: a ready party member is selected automatically. 1 selects Attack,
  2 Abilities, 3 Items, and 4 the equipped snack. Arrows select a target and
  Enter confirms. Tab cycles ready party members, Space responds to timing
  prompts, R retreats, L opens the battle log, and Esc / right click cancels.
- Dialogue: Enter reveals the text, then advances a line without choices;
  number keys select responses after the text is revealed.
- Illustrated intro: progresses automatically; hold the on-screen Skip control
  to skip it. Click the bold underlined words in room descriptions to interact.
- Field menu: 0 Stats, 1 Inventory, 2 Tinker, 3 Journal, 4 Map, 5 Settings.
  Alt+1-6 selects pages in sidebar order; Ctrl+Tab cycles pages (Shift reverses).
  Tab moves focus. Esc / right click goes back or closes the menu. F5 quick
  saves and F9 loads the quicksave after confirmation. Digits remain usable
  while entering search text.
- Fishing: Space casts, hooks and starts reeling; hold mouse or Space to reel.
- Arcade: Enter starts or resumes; Esc pauses; arrows / WASD move; Space is the
  primary action and E the secondary action. Harmonic Pulse uses D / F / J / K.
- Map: drag to pan and use the mouse wheel or +/- buttons to zoom.
- F11 toggles fullscreen; Alt+Enter toggles borderless / windowed mode.
  Windowed bounds are preserved across display-mode changes. UI scaling fits
  the available window space.
- Settings in exploration and the hub offer Save and quit to desktop. Audio
  diagnostics and pitch testing are available only with --debug.

## Private release checks

Before wider distribution, play through all six worlds on the packaged build,
including transitions, recoverable defeat, optional stations and saved returns.
Check audio on a machine with an enabled Windows audio device, window resizing,
high DPI, and both installer upgrade and uninstall behavior.
These installers are private unsigned builds.
