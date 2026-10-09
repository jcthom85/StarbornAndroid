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

- Exploration: WASD / arrows to move, E for the first room action, Esc / I / Tab
  for the field menu, M to request the hub map, H for help.
- Combat: click a ready party member, select a command, then its target.
  1 selects Attack, 2 Abilities, 3 Items; arrows select a target and Enter confirms.
  Tab cycles ready party members; Space responds to timing prompts; Esc cancels targeting.
- Dialogue: Enter reveals the text, then advances a line without choices;
  number keys select responses after the text is revealed.
- Illustrated intro: progresses automatically; hold the on-screen Skip control
  to skip it. Click the bold underlined words in room descriptions to interact.
- Field menu: 1–6 select tabs; Esc closes it.
- Fishing: Space casts, hooks and starts reeling; hold mouse or Space to reel.
- Arcade: Enter starts or resumes; Esc pauses; arrows / WASD move; Space is the
  primary action and E the secondary action. Harmonic Pulse uses D / F / J / K.
- F11 toggles fullscreen.

## Private release checks

Before wider distribution, play through all six worlds on the packaged build,
including transitions, recoverable defeat, optional stations and saved returns.
Check audio on a machine with an enabled Windows audio device, window resizing,
high DPI, and both installer upgrade and uninstall behavior.
These installers are private unsigned builds.
