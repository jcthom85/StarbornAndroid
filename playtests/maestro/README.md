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
.\scripts\verify_world1.ps1 -InstallDebug
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\smoke_launch.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\start_new_game.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\early_tutorial_dismiss.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\early_exploration_navigation.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\room_keyword_inspection.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\early_jed_dialogue.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\mainquest_wake_up_call.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\room_entities_tray.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\room_item_pickup.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\presence_stress.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\presence_combat_return.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\shop_scrapper_contraband.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\sidequest_system_flush.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\sidequest_scavenger_stash.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\heavy_lifting_training.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\checkpoint_badge_gate.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\mainquest_the_echo.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\dynamic_enemy_movement.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\mainquest_red_alert.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\mainquest_the_launch.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\debug_hub1.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\debug_hub2.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\first_combat_entry.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\enemy_party_combat.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\combat_target_prompt.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\combat_command_menu.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\combat_menu_dismiss.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\combat_enemy_status_rail.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\combat_flashbang_fx.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\save_load_roundtrip.yaml
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\debug_full_inventory_menu.yaml
```

For focused World 1 repair runs, Maestro 2.6.0 can intermittently fail `launchApp` immediately after clearing app data. Use this ADB preflight when a clean app state is required:

```powershell
.\scripts\adb.ps1 -s 46121JEKB11849 shell pm clear com.junewiregames.starborn.prealpha
.\scripts\adb.ps1 -s 46121JEKB11849 shell am start -n com.junewiregames.starborn.prealpha/com.example.starborn.MainActivity
Start-Sleep -Seconds 2
.\scripts\maestro.ps1 --device 46121JEKB11849 test .\playtests\maestro\mainquest_wake_up_call.yaml
```

`verify_world1.ps1` sends best-effort wake and keyguard-dismiss commands before running Maestro. Keep the device unlocked while Maestro is running. A failed first assertion with a lock-screen screenshot usually means the app never launched into the foreground; run `.\scripts\adb.ps1 -s <device-id> shell input keyevent KEYCODE_WAKEUP` and `.\scripts\adb.ps1 -s <device-id> shell wm dismiss-keyguard`, then rerun the flow.

## Current Flows

- `smoke_launch.yaml`: launches Starborn and verifies the title menu text/buttons are visible.
- `start_new_game.yaml`: clears app state, starts a new game, waits for the main menu to disappear, and captures a screenshot.
- `early_tutorial_dismiss.yaml`: starts a fresh game, verifies the first tutorial prompt requires a tap, dismisses it with `GOT IT`, then verifies `Nova's Bunk` is visible.
- `early_exploration_navigation.yaml`: dismisses the first tutorial, swipes west from `Nova's Bunk` to `Pod Row`, verifies the quest detail popup requires `Continue`, opens the field menu, closes it, and captures a screenshot.
- `room_keyword_inspection.yaml`: starts a fresh game, moves to `Pod Row`, taps the highlighted `Sleeping pods` room keyword through its semantic selector (`Inspect Sleeping pods`), verifies its authored inspection text appears in a tap-to-dismiss overlay, and captures a screenshot.
- `early_jed_dialogue.yaml`: starts a fresh game, follows the early path to `Jed's Bunk`, dismisses tutorial/quest overlays, taps the `Jed` NPC chip, and verifies dialogue opens.
- `mainquest_wake_up_call.yaml`: starts a fresh game and verifies the full `Wake Up Call` route through bunk inspection, Jed dialogue, hub return, Jed's Workshop entry, tinkering tutorials, Functional Cryo-Inductor crafting, and quest completion.
- `room_entities_tray.yaml`: starts from the debug enemy-party checkpoint, verifies the compact room presence tray exposes the hostile party selector, and captures a screenshot before combat.
- `room_item_pickup.yaml`: starts from the debug room-items checkpoint, verifies a compact item pickup chip renders in the presence tray, picks up `Medkit I`, and verifies the item tray clears.
- `shop_scrapper_contraband.yaml`: starts from the Trade Row Scrapper debug checkpoint, opens the `Contraband` service action, verifies Scrapper's buy/sell shop screen, visible stock, empty sell state, and return to exploration.
- `sidequest_system_flush.yaml`: starts from the Med-Bay debug checkpoint, completes Doc's `System Flush` side quest through real dialogue, room travel, vent interaction, return dialogue, and quest completion.
- `sidequest_scavenger_stash.yaml`: starts from the Trade Row Scrapper debug checkpoint, completes `The Scavenger's Stash` through Scrapper dialogue, stash discovery, rebel-cache opening, return dialogue, and quest completion.
- `heavy_lifting_training.yaml`: starts from the Loading Dock debug checkpoint, verifies Boggs starts `Heavy Lifting`, inline loader/cargo interactions enable the trainer, Nova learns and uses `Hydraulic Kick`, the Acoustic Bulwark training fight resolves through spoils and level-up, reward item popups are acknowledged, and the quest completion popup dismisses back to exploration.
- `checkpoint_badge_gate.yaml`: starts from the Transit Checkpoint debug checkpoint, verifies Hank's denial, Zeke's override/badge grant, immediate `The Echo` handoff, the Mine Access Badge opening Blast Door A, and Boggs redirecting an untrained Nova into Heavy Lifting.
- `mainquest_the_echo.yaml`: starts from the Deep Mine debug checkpoint, verifies Boggs' MQ03 assignment, Deep Elevator progression, persisted cleared-encounter suppression along the Deep Mine route, Echo Chamber navigation, the Tuning Fork relic cinematic, Tuning Fork item grant, Blast Wave Source Art tutorial, and the handoff from `The Echo` into `Red Alert`.
- `dynamic_enemy_movement.yaml`: starts in the dark Deep Mine patrol pilot, verifies darkness hides room/enemy identity, then waits for the automatic Pressure Hauler combat transition.
- `dark_room_visibility.yaml`: starts in the same dark patrol room and verifies the dark-room header hides the room title, hostile label, and engage affordance.
- `title_menu_version.yaml`: verifies the title menu exposes the Play Console version name and code.
- `mainquest_red_alert.yaml`: starts from the Red Alert debug checkpoint, verifies Zeke's comms route out of the Echo Chamber, persisted cleared-encounter suppression through Maintenance Access and Cargo Lift, Jed's sacrifice dialogue and Chime handoff, and the handoff from `Red Alert` into `The Launch`.
- `mainquest_the_launch.yaml`: starts from the Launch debug checkpoint, verifies the post-Warden Pod Bay state, Zeke's Chime-splice dialogue, pod-core navigation-console interaction, the Planetary Impact cinematic, `The Launch` completion, the `A Strange Coast` World 2 handoff, and the Crash Site return to exploration.
- `debug_hub1.yaml`: opens Homestead Quarter directly and verifies all five illustrated node destinations are visible.
- `debug_hub2.yaml`: opens Logistics Sector directly and verifies all five illustrated node destinations are visible without overlap hiding a selector.
- `first_combat_entry.yaml`: starts from a debug MQ03 Deep Mine checkpoint, swipes into `Main Tunnel Alpha`, taps the Echo-Borer hostile, and verifies the combat command UI appears.
- `enemy_party_combat.yaml`: starts from the debug enemy-party checkpoint, engages a multi-enemy Dominion party, and verifies the combat screen presents the encounter correctly.
- `presence_stress.yaml`: starts from a debug-only unreachable room with several NPCs, item pickups, and distinct hostile-party chips, then captures the crowded exploration presence tray for layout review.
- `presence_combat_return.yaml`: starts from the same crowded debug room, clears one hostile party, then verifies the defeated party disappears while remaining NPCs, items, and enemy parties stay visible after returning from combat.
- `combat_target_prompt.yaml`: verifies the redesigned top target prompt/cancel path does not cover target selection.
- `combat_command_menu.yaml`: verifies the combat command menu opens with the expected actions and stays anchored without shifting the party layout.
- `combat_abilities_menu.yaml`: verifies the redesigned abilities list, ability detail popup, return path, and integration with the item menu.
- `combat_menu_dismiss.yaml`: verifies tapping the active character again dismisses the combat command menu without firing an accidental action.
- `combat_enemy_status_rail.yaml`: verifies enemy status/effect presentation uses the compact rail instead of stretching enemy health cards.
- `combat_flashbang_fx.yaml`: waits for the Flashbang enemy action and verifies blind status applies to the party without square portrait artifacts.
- `save_load_roundtrip.yaml`: starts a fresh game, opens the field menu settings tab, saves to Slot 1, closes the menu, reopens Load, loads Slot 1, and verifies `Nova's Bunk` is restored.
- `debug_full_inventory_menu.yaml`: starts a debug full-inventory game, verifies the tutorial dismissal path still works, opens the field menu, and confirms debug inventory/party state is visible.

Draft flows live under `playtests/maestro/drafts/` until their navigation path is stable.

## Authoring Rules

Prefer visible text and content descriptions over coordinates. For Compose-only controls that Maestro cannot reliably select, add stable `contentDescription` or visible labels in the app before writing tap-heavy flows. Keep combat assertions tied to real UI state text such as status names, cooldown labels, turn banners, or accessible command labels.

