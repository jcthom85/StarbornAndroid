# Android Maestro playtests

The flows, helpers, setup scripts, and pinned toolchain are versioned in Git. A clone includes them. Downloaded tools, emulator images, APKs, saves, and run artifacts stay local and ignored.

## First run on another Windows computer

Install Android Studio with its bundled JBR, Android SDK/platform-tools, and the SDK required by the Gradle project. Use Node.js 22+ for the static validator. Create an Android emulator with 4 GB RAM and several GB of free storage, or connect an unlocked Android test phone with USB debugging enabled. The current APK is approximately 1.3 GB, so installation needs additional temporary space.

From the repository root in PowerShell:

```powershell
npm ci --ignore-scripts
node scripts/validate-maestro.cjs
.\scripts\adb.ps1 devices
.\scripts\setup-maestro.ps1 -InstallPlaytest -Device emulator-5554
.\scripts\maestro.ps1 --device emulator-5554 test playtests/maestro/suite_smoke.yaml
.\scripts\maestro.ps1 --device emulator-5554 test playtests/maestro/opening_equipment_later.yaml
```

Replace the serial with the output from `adb devices`. With exactly one device connected, the runner can select it automatically. Setup downloads Maestro 2.11.0 from its official GitHub release and verifies the pinned SHA-256. `-ArchivePath <zip>` supports an already downloaded archive. Android Studio's JBR is detected automatically; `JAVA_HOME` overrides it. Gradle currently uses JBR 21.

The runner targets `com.junewiregames.starborn.prealpha.playtest`. All flows use `${APP_ID}` supplied by the wrapper. `clearState: true` resets that app's test saves. Do not run raw flows against your regular campaign app. The `playtest` build enables Debug Scenarios and has its own Android app data.

## Suites and coverage

October 23 validation: all 91 flow files and 114 helper references pass structural checks; 389 authored-prose selectors pass the strict source scan. Live acceptance is still pending: the local API 36 emulator suffered System UI/rendering failures and did not complete the menu smoke test. The opening/equipment flows are authored coverage, not certified passing device tests. Use a stable emulator or physical phone to finish acceptance.

`suite_smoke.yaml` checks the title/scenario browser, new-game skip, bunk light/door/movement tutorial, and starter equipment offer followed by manually equipping both slots. `opening_equipment_later.yaml` checks deferral. Opening helpers use the real production start rather than a pre-equipped fixture.

World 1 and World 2 flows retain their larger quest, combat, and puzzle routes. Scenario launch confirmation, opening prose, World 2 crash dialogue, and the two-step Source Gate decision have been reconciled with current source. These longer routes require live verification; a successful structure check does not certify them. Older World 3–6 and system flows remain available as historical coverage and may still need UI updates. Run named flows individually instead of treating the entire directory as a passing regression suite.

`scripts/verify_world1.ps1 -InstallPlaytest` combines asset validators, JVM tests, lint, smoke, and equipment deferral. `-InstallDebug` remains an alias but now installs the isolated playtest build. Missing devices or failed flows return a failure instead of silently counting as success.

## Results and maintenance

Each run writes logs, JUnit results, and Maestro debug/screenshot artifacts beneath `playtests/artifacts/maestro/<timestamp>-<id>/`. These and `.maestro-home/` are ignored. The wrapper preserves Maestro's exit status and requires a nonempty successful JUnit report before reporting success. Only the child process environment changes; your normal home/profile stays intact.

Launch commands specify `permissions: {}`. Starborn declares only the normal vibration permission, so automatic permission grants are unnecessary; Maestro's default permission discovery otherwise pulls the entire APK from the device on each launch. The runner wakes the device before testing. Its default overall timeout is 900 seconds; set `STARBORN_MAESTRO_TIMEOUT_SECONDS` higher for longer historical routes.

GitHub's **Maestro flow validation** workflow parses every YAML, checks helper references/cycles, enforces the isolated app parameter, and runs `scripts/validate_maestro_selectors.ps1 -Strict` when flows or game content change. The prose scan handles regex selectors and derives cooldown samples from the shared formatter. It cannot detect every stale short label or verify interaction ordering. CI does not start an emulator; device runs remain the authority for interaction behavior.
