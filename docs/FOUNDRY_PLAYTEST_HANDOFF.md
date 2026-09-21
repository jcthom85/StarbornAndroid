# Foundry playtest and balance-check handoff

## Objective and boundaries

Execute this protocol after the user switches models. Determine whether the Foundry pilot adds meaningful tactical variety without unfair difficulty, tedious healing, or unreadable mechanics. Test enemy party composition as well as individual enemies. This is a Foundry pilot, not certification of the entire campaign.

Read `docs/FOUNDRY_ENEMY_VARIETY_PILOT.md` first. Existing implementation has automated coverage but has NOT been validated through emulator play. Previously reported baseline: 83 selected tests passed; rerun rather than treating that as current evidence.

Allowed work: add isolated test fixtures, automated tests, measurement helpers, screenshots, logs, and a report. Do not change production combat, balance data, art, or campaign progression during this evaluation. Propose fixes with evidence for a subsequent approved pass. Do not read `openai_api_key.txt`; no art or paid API calls are needed. Preserve all existing worktree changes, saves, and unrelated assets. Do not commit, publish, uninstall the app, clear its data, or wipe the emulator.

Work in the stages below; record progress in the report after each stage. Do not ask the user to operate the emulator unless access actually requires their intervention. Follow applicable computer-use skill instructions before controlling the emulator UI. Do not substitute simulated results for observed play.

## 1. Establish a reproducible baseline

- Record current commit, scoped working-tree diff, app build, device/API level, test commands, and fixture versions. The worktree already contains substantial user changes; HEAD alone is not the tested version.
- Inspect production encounter selection before counting encounters. Audit Foundry rooms, scripted/event battles, moving parties, and any other live encounter sources. Identify actual enemy parties, occurrence conditions, selection weights, repeatability, and route ordering. Do not double-count an `enemies` list that mirrors `enemy_parties`. Distinguish authored counts from measured encounter frequency.
- Produce a short encounter inventory: source/room, enemy IDs and counts, mandatory/optional, repeatability, and intended tactical distinction. Flag unsupported source types instead of guessing.
- Verify how levels, equipment, skill unlocks, and derived combat stats are constructed. `DebugCampaignFixtureBuilder` currently assigns World 4 level 9 to Nova, Zeke, Orion, and GH0ST, but does not explicitly set equipment. Its comment is not proof of campaign realism; copied session state can also retain unrelated state.
- Define explicit clean loadouts with item IDs, equipment slots, stats, skill IDs, inventory counts, and provenance from obtainable campaign rewards/shops. Use an expected early-Foundry loadout, an expected later-Foundry loadout, and a weaker plausible loadout (e.g. fewer optional upgrades). If progression cannot support a reliable expectation, label assumptions and limit conclusions. Do not quietly use overpowered debug inventories.
- Start individual comparisons at identical HP/resources. For route tests preserve HP, consumable depletion, and legal recovery between battles; do not reset after every fight.

## 2. Recheck mechanics before balance measurements

Reuse real production AI, action processing, targeting, statuses, ATB, and combat-end handling. Useful starting points: `OpeningCombatRuntimeTest.kt`, `EnemySupportDecisionTest.kt`, `CombatViewModelTest.kt`, `DebugSystemScenariosInstrumentedTest.kt`, and `IsolatedProgressionContext.kt`. Mock presentation or storage only where appropriate, not the mechanics under test.

Required cases:

1. Golem selects Molten Slam during an ordinary fight, then cooling on its next eligible action. Cooling targets itself and changes actual damage taken. Verify the real duration/tick rules and whether a player receives an actionable attack opportunity before exposure expires.
2. Two golems maintain independent recovery histories. Test Jammed and other applicable disabling statuses between slam and recovery, including recovery after disable expires. Report whether an intervening basic/defend action loses the pending recovery; do not assume that is intended.
3. Welder repairs living allies, including itself where valid, but never opponents or defeated allies. Exercise damaged squad, full-health squad, one surviving ally, cooldown, and exhausted supplies. Verify a maximum of two actual repairs per welder per battle and offensive behavior between repairs. Two-welder stress fixture tests independent usage counters, not a proposed authored encounter.
4. Verify authored skill targeting through the production viewmodel path, not only a helper supplied by the test.
5. Titan still vents following both Missile Barrage and Titan Stomp; test normal and disabled recovery paths.
6. Background/resume during cooling and repair cooldown. Inspect whether mid-battle save/reload is supported; if supported, verify history, HP, cooldowns, and repair supplies cannot reset or duplicate rewards. If not supported, report the actual supported save boundary and test that instead.
7. Victory, defeat, and leaving/re-entering an encounter do not carry stale counters between battles or award duplicate completion/rewards.

Separate deterministic failures from balance concerns. A broken mechanic invalidates measurements relying on it; continue independent tests and report the dependency.

## 3. Repeatable combat comparisons

Build the smallest test-only harness needed; avoid a new general-purpose simulation framework. Drive legal player commands through real combat. Use deterministic seeds where the runtime supports them. If randomness is not controllable, explicitly mark runs unseeded and preserve action traces; do not claim paired determinism.

Core matrix:

| Party | Question |
| --- | --- |
| Solo Magma Drone | Is fast pressure distinct and survivable? |
| Solo Slag Golem | Is cooling frequent, useful, and exploitable? |
| Solo Welder | Does finite repair avoid a drawn-out solo fight? |
| Welder + Magma Drone | Does target priority change damage/resource cost? |
| Golem + Welder | Does support create a decision rather than merely extra HP? |
| Golem + Magma Drone | Is mixed pressure distinct from the healer pair? |

For expected and weaker loadouts, screen each core party with 10 runs per policy. Compare a fixed straightforward policy (focus first selected enemy, normal available attacks, heal below a declared threshold) with a tactical policy (prioritize support or pressure deliberately, exploit visible cooling and known weaknesses). Document exact rules, resource limits, and tie breaks; neither policy may use hidden future rolls or illegal actions. For mixed parties, explicitly compare support-first versus damage-source-first at the same starting state. Expand suspicious comparisons to 30 runs per policy, not every case indiscriminately.

Use test-only previous-party fixtures for composition comparisons: Conveyor Belt two Welders versus Welder + Drone; Sorter Spine solo Welder versus Welder + Drone; Core Shadow solo Golem versus Golem + Welder. Keep the current mechanics constant to isolate party-size effects. Label these as composition comparisons, not full historical balance comparisons. Do not revert production assets to create a baseline.

Record per run: fixture/loadout/party/source, seed or unseeded ID, policy, win/loss/timeout, player and enemy action counts, damage dealt/received, knockouts, remaining HP, consumables, actual repair uses and effective healing, signature move counts, cooling opportunities and successful exploitation, XP/credits/loot. Report medians and ranges, with raw run data. Scripted policy win rate is not human player win rate.

Bound each run at 200 executed combat actions and add a no-progress timeout appropriate to the test scheduler. Treat exceeding the bound as an investigation case, not proof of an infinite loop. Exclude debugger pauses and tool latency from combat-time measurements; prefer actions/ATB time over wall-clock speed.

## 4. Emulator playthrough and readability

Use an isolated test context/save area. `IsolatedProgressionContext` and `AppServices(context, true)` are existing instrumentation patterns. Scenario-menu BuildConfig is currently false; do not enable it globally just to test. Add a test-only entry point if needed, ensuring the displayed combat uses the real UI/viewmodel/runtime and cannot overwrite normal saves.

Candidate campaign starts: `campaign_w4_mq16` at `foundry_slag_landing`, `campaign_w4_mq17` at `foundry_waste_intake`, and subsequent World 4 quest checkpoints. Verify their prerequisites/loadouts before relying on them.

Play and capture evidence for:

- Introductory solo drone and golem fights with the expected early loadout.
- Both Conveyor Belt parties, including its separate Phantom encounter; verify actual triggering and sequencing, not merely direct enemy spawning.
- Sorter Spine (`foundry_waste_intake_scale_final`) with Welder + Drone.
- Core Shadow (`foundry_power_core_scale_final`) with Golem + Welder.
- Waste Intake golem/drone composition and one Titan recovery regression.
- One continuous reachable Foundry route of at least three fights with expected gear, then weaker gear. Record route, recovery opportunities, replenishment costs, and mandatory versus optional encounters. Do not invent connections between disconnected checkpoints.
- One background/resume case and applicable save-boundary case.

For the healer pairs, replay different target priorities from identical isolated starting snapshots. Do not resolve battles through victory injection, direct HP edits, or skipping enemy turns.

Capture screenshots/log timestamps of repair, cooling start, a usable player attack window, and battle outcome. Judge using what the screen communicates: can the player distinguish enemies, notice repair/exposure, select the intended target, and understand why a tactic helps? Shared portraits remain a known limitation. Internal logs establish mechanics, not visual readability. Report absent cues even if tests pass.

## 5. Pass criteria and investigation flags

Hard requirements: legal targeting; finite repair supplies; correct per-instance state; no crashes/softlocks; no unintended reward duplication; real recoveries and status effects; no save corruption. Any violation is a failed case with reproduction evidence.

Balance/readability investigation flags (provisional, not established design laws):

- No actionable cooling window during observed normal ATB sequences.
- Signature mechanics rarely occur before enemies die, or the UI never makes them discernible.
- Identical best tactics across all pairings, with no observable resource or risk tradeoff.
- Repair repeatedly restores most progress and inflates median action count by more than 25% against an appropriate controlled comparison without a tactical payoff.
- An added optional pair creates a large difficulty/reward discontinuity relative to adjacent encounters, or route attrition demands resources unavailable on that route.
- Expected loadout repeatedly loses even with deliberate legal counterplay, or weaker gear requires a single obscure solution.

Investigate flags with action traces and expanded runs. Do not auto-nerf from arbitrary thresholds or claim statistical proof of fun. Summarize separately: mechanical correctness, tactical distinction, pacing/attrition, readability, and confidence in fixtures.

## Local commands and environment notes

PowerShell from the repository root:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest --tests '*EnemySupportDecisionTest' --tests '*Combat*Test' --tests '*DataIntegrityTest' --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
& 'C:\Users\jcthomas\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices -l
```

Use actual new test class names when running added suites. Quote dotted Gradle properties in PowerShell, for example:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.starborn.DebugSystemScenariosInstrumentedTest' --console=plain
```

That example runs existing system scenarios, not the new Foundry acceptance suite. App ID is `com.junewiregames.starborn.prealpha`. Previously observed device was `emulator-5554`; rediscover instead of assuming. A prior APK was roughly 1.28 GB and emulator installation stalled. Check storage, package state, selected device, and installation logs before retrying. Use bounded waits with progress updates. Do not repeatedly reinstall, kill unrelated processes, or erase saves to force progress. If safe diagnostics cannot restore access, finish independent headless checks and explicitly mark emulator coverage blocked.

## Deliverables and stopping point

Create `docs/FOUNDRY_PLAYTEST_REPORT.md` and store raw evidence under a clearly named local test-results directory. Link evidence in the report; do not commit large recordings. Include:

1. Tested build and exact reproducible commands/loadouts/policies.
2. Encounter-source inventory and route coverage, including omissions.
3. Per-case PASS / FAIL / BLOCKED / NOT RUN, with evidence paths.
4. Aggregate measurements plus raw runs, without blending fixture types.
5. Ranked issues with reproduction steps, expected versus observed behavior, confidence, and smallest recommended fix.
6. Recommendation: ready for art/expansion, needs targeted fixes, or insufficient evidence. Separate verified facts from subjective observations.

Complete when the required cases have evidence-backed outcomes (or explicit external blockers), the report is written, and any added harness tests are rerun. Do not silently expand into Worlds 5/6 or implement production fixes. If the harness requires a substantial production redesign, report the specific obstacle and request direction. The next review selects changes, followed by replaying the same fixtures to measure their effect.
