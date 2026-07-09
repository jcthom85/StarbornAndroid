# Codex task — verify Starborn telemetry/UX changes (2026-07-02)

One-off verification handoff. Safe to delete when done. (Note: the older `CODEX_HANDOFF.md`
in this repo is an unrelated historical doc — ignore it.)

## Context

A batch of changes was just made to the Starborn Android app (pure Kotlin/Compose, no game engine).
What changed:

- **Playtest telemetry**: new `domain/telemetry/TelemetryLogger.kt` writes JSONL to `filesDir/telemetry/`.
  Instrumented in `AppServices` (room_enter), `CombatViewModel.applyOutcomeResults` (combat_start/end),
  `QuestRuntimeManager.appendLog` (quest events), `MainActivity` (foreground/background), save/load.
- **Crash capture**: new `StarbornApplication.kt` (registered in manifest) installs an
  `UncaughtExceptionHandler` via `domain/telemetry/CrashRecorder.kt`, writing to `filesDir/crash/`.
- **Bug report**: `domain/telemetry/BugReportBuilder.kt` zips telemetry+crash+saves+device info;
  "Report Issue" button in `SettingsTabContent.kt`; FileProvider added to manifest + `res/xml/file_paths.xml`.
- **Playtime**: new proto field `total_playtime_ms = 50`, `GameSessionState.totalPlaytimeMs`,
  accumulated in `AppServices`, shown in save-slot subtitle via `MainMenuViewModel.saveDetails`.
- **Objective HUD**: new `feature/exploration/ui/hud/ObjectiveHud.kt`, wired into `ExplorationScreen`.
- **UX**: Continue button (`MainMenuScreen`), overwrite/delete confirm dialogs (`SaveLoadDialog`),
  "Autosaved ✓" toast (`AppServices`).
- **Accessibility**: contentDescription/stateDescription on combat command tiles, roster, status chips,
  save-slot cards, settings sliders.
- Fixed a mojibake bug in `ExplorationViewModel.kt` (`"âœ“"/"â—‹"` → `"✓"/"○"`).
- New unit test: `app/src/test/java/com/example/starborn/domain/telemetry/TelemetryLoggerTest.kt`.

## Known issue — READ BEFORE RUNNING

A prior `.\gradlew.bat lintDebug testDebugUnitTest runAssetIntegrity` failed with many
`java.lang.NoSuchMethodError: ...GameSessionState.<init>(...)`. **This is NOT a code bug.**
It is a stale Kotlin incremental-build artifact: the test classpath still held the old
`GameSessionState` constructor from before `totalPlaytimeMs` was added. `app/src/main` compiles
cleanly on its own (`:app:compileDebugKotlin` passes). The fix is a clean rebuild.
**Start with `clean`.** Do not "fix" this by editing source or tests.

## Environment

- Repo root: `C:\Users\jctho\StudioProjects\StarbornAndroid`
- Shell: Windows PowerShell. Use `.\gradlew.bat`.
- `adb` is NOT on PATH. Full path: `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`
- Connected device: Pixel 8a, serial `46121JEKB11849`
- applicationId: `com.junewiregames.starborn.prealpha`
- Launch activity: `com.example.starborn.MainActivity`
- No Maestro installed — drive the device with `adb` (`exec-out screencap` + `input tap`) if needed.

## Step 1 — Clean build + full verification suite

```powershell
.\gradlew.bat clean
.\gradlew.bat lintDebug testDebugUnitTest runAssetIntegrity --console=plain
```

Expected: BUILD SUCCESSFUL. Baseline before these changes was 157 tests / 0 failures, plus the new
`TelemetryLoggerTest` (3 tests). If anything other than a stale-build `NoSuchMethodError` fails,
capture the failing test name and the message from
`app/build/test-results/testDebugUnitTest/TEST-*.xml` and report it — do not edit tests to make them pass.

## Step 2 — Install on the Pixel 8a

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
.\gradlew.bat installDebug
& $adb -s 46121JEKB11849 shell am start -n com.junewiregames.starborn.prealpha/com.example.starborn.MainActivity
```

## Step 3 — On-device smoke checks

Play a few minutes (New Game → move between rooms → one combat → open a quest). Verify:

1. **Objective HUD**: quest chip under the room header during exploration; updates as objectives
   complete; hidden during dialogue/menus.
2. **Continue button**: back at title (or relaunch) — "Continue" appears above "New Game" and loads
   the most recent non-empty slot.
3. **Playtime in slots**: open Load — each occupied slot subtitle ends with e.g. "12m" or "1h 3m".
4. **Overwrite/delete confirm**: Save onto an occupied slot → "Overwrite …?" dialog; Delete → confirm dialog.
5. **Autosave toast**: after ~90s of state change, an "Autosaved ✓" toast appears.

## Step 4 — Verify telemetry files

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s 46121JEKB11849 shell run-as com.junewiregames.starborn.prealpha ls -l files/telemetry
& $adb -s 46121JEKB11849 shell "run-as com.junewiregames.starborn.prealpha cat files/telemetry/*.jsonl" | Select-Object -Last 40
```

Expect JSONL lines with `"e"` values: `session_start`, `room_enter`, `combat_start`, `combat_end`,
`quest`, `save`/`load`, `app_foreground`/`app_background`.

## Step 5 — Bug report share (manual)

In-app: Settings → Support → "Report Issue". A share sheet should open with `starborn_bugreport_*.zip`.
Share to Drive/Files and confirm it contains `device_info.txt`, `telemetry/`, and `saves/`.

## Step 6 — Crash capture (optional)

No built-in force-crash trigger. Either add a temporary `throw RuntimeException("codex-test")` behind a
debug tap, rebuild, tap, relaunch, and confirm a file under `files/crash/` + the "crash recorded"
main-menu snackbar (then revert); or skip on-device and confirm the handler is wired in
`StarbornApplication.onCreate` by code review.

## Report back

Reply with: clean-build test pass/fail (+ any real failure text), which Step-3 checks passed, and a
paste of the last ~20 telemetry JSONL lines from Step 4.
