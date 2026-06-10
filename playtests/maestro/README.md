# Starborn Maestro Playtests

This directory contains device-level smoke/playtest flows for Starborn using the repo-local Maestro CLI install.

## Install Location

Maestro CLI `cli-2.6.0` is installed under:

```text
tools/maestro/cli-2.6.0/maestro
```

Use the repo wrappers from Windows PowerShell. They resolve Android SDK and keep Maestro's home/log/temp state inside the repo:

```powershell
.\scripts\maestro.ps1 --version
.\scripts\adb.ps1 devices
```

## Running Flows

Build/install the debug app first if it is not already on the target device:

```powershell
$env:GRADLE_USER_HOME = "$PWD\.gradle-codex"
.\gradlew.bat :app:installDebug
```

Then run a flow. If more than one Android device is connected, pass the device id from `.\scripts\adb.ps1 devices`:

```powershell
.\scripts\adb.ps1 devices
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\smoke_launch.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\start_new_game.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\early_tutorial_dismiss.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\early_exploration_navigation.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\early_jed_dialogue.yaml
```

## Current Flows

- `smoke_launch.yaml`: launches Starborn and verifies the title menu text/buttons are visible.
- `start_new_game.yaml`: clears app state, starts a new game, waits for the main menu to disappear, and captures a screenshot.
- `early_tutorial_dismiss.yaml`: starts a fresh game, verifies the first tutorial prompt requires a tap, dismisses it with `GOT IT`, then verifies `Nova's Bunk` is visible.
- `early_exploration_navigation.yaml`: dismisses the first tutorial, swipes west from `Nova's Bunk` to `Pod Row`, verifies the quest detail popup requires `Continue`, opens the field menu, closes it, and captures a screenshot.
- `early_jed_dialogue.yaml`: starts a fresh game, follows the early path to `Jed's Bunk`, dismisses tutorial/quest overlays, taps the `Jed` NPC chip, and verifies dialogue opens.

Draft flows live under `playtests/maestro/drafts/` until their navigation path is stable.

## Authoring Rules

Prefer visible text and content descriptions over coordinates. For Compose-only controls that Maestro cannot reliably select, add stable `contentDescription` or visible labels in the app before writing tap-heavy flows. Keep combat assertions tied to real UI state text such as status names, cooldown labels, turn banners, or accessible command labels.

