# World 1 testing and playtesting

World 1 has four named Maestro suites. The continuous campaign starts at normal New Game, uses earned resources and actual combat, and ends at the World 2 crash site. Focused quest cases load earned completionist checkpoints. Recipe cases deliberately use synthetic exact/missing-material fixtures. These are different kinds of evidence.

**Emulator acceptance passed:** each named suite passed twice consecutively on the final build. Windows execution, human phone acceptance and broader host/narrative checks remain open. An authored flow or a passing static check is not a device pass. See [current results and coverage](WORLD1_MAESTRO_REFRESH_2026-10-23.md). Do not run the full historical flow directory as a regression suite.

## Setup on macOS

Install Android Studio with its JBR and SDK/platform-tools, Node.js 22+, Python 3.10+, and PowerShell 7 (`pwsh`, used by existing asset/prose validators). Create a stable Android emulator with at least 4 GB RAM or connect an unlocked USB-debugging phone. Android Studio's SDK/JBR locations are detected automatically; `ANDROID_HOME` and `JAVA_HOME` override them. Gradle provisions its pinned JBR 21. Leave several GB free for the approximately 1.3 GB APK and installation.

```sh
npm ci --ignore-scripts
./scripts/setup-maestro.sh --install --device emulator-5554
./scripts/maestro.sh --suite smoke --device emulator-5554
```

## Setup on Windows

Install the same prerequisites, including Python on PATH and PowerShell 7. Android Studio's bundled JBR and the usual Android SDK directory are detected automatically. The wrapper has been exercised with PowerShell 7 on macOS; execution on a Windows machine remains unverified. Maestro's [CLI installation guide](https://docs.maestro.dev/maestro-cli/how-to-install-maestro-cli) documents Windows installation.

```powershell
npm ci --ignore-scripts
.\scripts\setup-maestro.ps1 -InstallPlaytest -Device emulator-5554
.\scripts\maestro.ps1 --suite smoke --device emulator-5554
```

Both platforms call `scripts/maestro_runner.py`, share `toolchain.json` and `suites.json`, and pin Maestro 2.11.0 with SHA-256 verification. Setup supports `--archive path.zip` on Mac and `-ArchivePath path.zip` on Windows. Use `adb devices` from the SDK's platform-tools directory to find a serial. Exactly one ready device may be selected automatically.

## What to run

| Suite | Purpose | Start and limits |
| --- | --- | --- |
| `smoke` | Title/scenario browser, opening interactions, starter equipment acceptance and deferral | Resets isolated app data between independent cases |
| `campaign` | All five main quests and required certification through World 2 | One continuous fresh campaign; no injected inventory or debug victory |
| `focused` | All five side quests, cart vault, recipe boundaries and menus | Independent earned checkpoints; earlier optional quests may already be completed |
| `recovery` | Changed opening save, movement away, manual load, force-stop/relaunch and load | Opening only; later lifecycle and mid-battle acceptance also need the checklist |

```sh
./scripts/maestro.sh --suite campaign --device emulator-5554
./scripts/maestro.sh --suite focused --device emulator-5554
./scripts/maestro.sh --suite recovery --device emulator-5554
./scripts/verify_world1.sh --install --device emulator-5554
```

Windows uses the corresponding `.ps1` wrappers; suite arguments are identical. `verify_world1.ps1 -InstallDebug` remains an alias for `-InstallPlaytest`. Verification performs structure/freshness checks, runner safeguards, strict selectors, World 1 asset validators, host tests, lint, then all four device suites. Mac skips use `--skip-gradle`, `--skip-tests`, `--skip-lint`, `--skip-maestro`; Windows retains the existing `-Skip*` switches. A skipped stage is an incomplete acceptance run.

A single flow can be isolated with `./scripts/maestro.sh --device emulator-5554 -- test playtests/maestro/world1/stash.yaml` (omit the separating `--` in PowerShell). Chapter helpers 02–06 require the preceding chapter's state; they are not independent tests. Temporary resume flows used for diagnosis are retained only in ignored local artifacts, outside the maintained flow directory. Their results do not count as fresh campaign passes.

Tests target `com.junewiregames.starborn.prealpha.playtest`. Their resets erase that app's saves. The runner rejects APP_ID overrides to the normal campaign package. Debug scenarios use separate test-session saves; settings such as tutorials/audio/accessibility must still be recorded. Launching a scenario resets its session; loading a saved slot resumes it.

## Evidence and acceptance

Every device run writes `run.json`, stdout/stderr, JUnit, and Maestro screenshots/hierarchies under `playtests/artifacts/maestro/<run>/`. Failed runs also collect recent logcat. New run metadata also hashes the flow/configuration tree and the runner source. Metadata records commit/dirty state, package version, serial, Android/API, display, font scale, animation settings and the local APK hash when available. That local hash does not establish the installed binary's hash. Record in-game settings separately in the [run template](run-template.md).

A nonzero exit, timeout, absent/empty report, failure, error, or any skipped required testcase fails acceptance. Default overall timeouts are 1200 seconds for smoke/recovery, 5400 for focused, 7200 for campaign, and 900 for an individual raw flow; override `STARBORN_MAESTRO_TIMEOUT_SECONDS` if measured route duration requires more. An environmental ANR remains a failed/incomplete run, with evidence retained.

Require two consecutive clean runs of every named suite on the same installed build and emulator. Then complete the [World 1 phone checklist](../playtest/WORLD1_ACCEPTANCE_CHECKLIST.md). Phone pacing, sound, clarity, accessibility and encounter variation cannot be signed off by Maestro or the fixed-seed host route. Record blockers and untested cases explicitly.

## Checkpoint maintenance

World 1 checkpoints ship in debug/playtest assets and in release builds only when the existing `enablePlaytestScenarios` flag is explicitly enabled. They are captured from the production completionist controller test using real combat, earned resources, seed 17, supplied puzzle answers and accelerated presentation. Their manifest hashes the source JSON catalogs and each save. CI refuses stale or hand-edited captures.

Regenerate after gameplay/content changes:

```sh
./gradlew :app:testDebugUnitTest --tests '*LegalRouteCampaignRunnerTest.productionWorld1CompletionistRouteFinishesAllTenQuestsAndTheCartDetour' --rerun-tasks
python3 scripts/world1_checkpoints.py --import-earned
python3 scripts/world1_checkpoints.py
node scripts/validate-maestro.cjs
```

On Windows use `gradlew.bat` and `python`. Review the checkpoint diff before committing. Import refuses captures made against different current JSON sources. Earlier optional rewards in these completionist snapshots are declared starting state, not proof of main-route balance. Accelerated controller presentation can leave early AP guidance incomplete; the helper advances it and closes Party without spending AP. Test tutorial teaching itself from normal New Game.

For isolated Android instrumentation of the browser/session boundaries, build with `-PandroidTestBuildType=playtest` and select the focused test classes. The default test build type remains debug.

When UI changes, update stable IDs/helpers and verify the actual device hierarchy. Keep display-copy assertions where the wording is itself under test. Never replace a missing interaction with debug victory, silently skip a failing branch, or mark a flow passing because its strings still exist somewhere in source.
