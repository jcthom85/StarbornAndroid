# Foundry continuation for Luna

## Current follow-up: extended finale verification

**Latest diagnostics complete:** read `REMAINING_BOSS_DIAGNOSTICS.md`. All previously observed alternate-policy timeouts terminate under a separately labelled extended diagnostic; do not repeat them as unexplained stalls. Avatar early triage worsened prepared-build wins from 4/5 to 1/5 and is not a recommended fix. `FINALE_TACTIC` is now restricted to the finale, not Avatar. Foundry's refreshed 720-fight matrix and three consecutive-fight tests pass their terminal/safety gates. Next work is a design decision on Avatar burst and finale pacing, plus actual route-resource validation—not unapproved balance changes or release.

**Latest isolation completed:** read `CAMPAIGN_VALIDATION_STATUS.md`. `FINALE_TACTIC` also accepts `standard_no_venom` and `direct_venom`; results are 32/35 and 34/35 wins. The task is no longer to repeat those comparisons. Next isolate erosion itself rather than removing an entire skill, investigate remaining timeouts/World 5 defeat, then refresh Foundry mixed-party and route-resource validation. Keep the main-menu BurgQuest Demo button.

**Presentation/tactic follow-up completed:** `FINALE_PRESENTATION_AND_TACTICS.md` records the emulator UI check and two alternate offensive policies. Direct: 18/35 wins, 12 timeouts; control: 31/35 wins, 2 timeouts. Standard remains 35/35. Next investigate reliance on erosion and improve recovery/healing explanations before broad balance sign-off. Do not label successful diagnostic Gradle execution as all fights won. Use `--rerun` when changing environment-selected policies. No release is authorized by these test results.

**2026-09-22 implementation supersedes the investigation below:** Link targeting and cross-side healing guards are fixed. The final boss has a harmless opening/recovery action, Gathering Silence. All 547 unit tests pass; all 35 finale seeds clear both phases with deterministic repeats and no timeouts under production targeting. Evidence: `test-results/finale-targeting-fix/final-campaign.xml`. Do not repeat the obsolete targeting counterfactual (`FINALE_LINK_ALLIES` was removed). Next useful verification is player-facing finale readability and varied tactics/builds, followed by release review; do not infer that fixed-policy success proves universal balance. BurgQuest Demo must remain present.

**Investigation completed:** read `FINALE_FAILURE_INVESTIGATION.md`. Seed 24 eventually loses at 256.25s; it is not a deadlock. Link's immediate heal targets the boss because its AoE healing power is misclassified by the target resolver. The existing full-health-enemy test misses this. A test-only explicit-ally comparison improves wins to 27/30 but leaves two long losing fights. Next work requires a production targeting fix and stronger regression coverage, followed by a fresh balance comparison—not another repetition of this investigation. No production change or menu edit was made during the investigation.

Read the newest section of `FOUNDRY_PLAYTEST_REPORT.md` before older instructions below. The original World 1/World 2/Foundry attrition gates now pass and the original finale seeds clear 5/5. The additional finale batch clears 26/30, with defeats at seeds 10, 11, 13 and a four-minute timeout at seed 24. Evidence is `test-results/status-timing-followup/finale-holdout.xml`. Current cooldown changes are candidates, not fully validated balance.

Next bounded test task (no production changes): reproduce seed 24 via `$env:FINALE_HOLDOUT_SEEDS='24'` and `./gradlew.bat :app:testDebugUnitTest --tests '*CampaignEventIntegrationTest'`. Diagnose the opening sequence, action choices, remaining supplies and why damage stalls; compare the three defeat seeds. The timeout assertion is expected to fail. Do not remove it or raise the time limit to mark acceptance passed. A separately labelled longer diagnostic may establish whether the encounter eventually terminates. Report evidence and recommendations before any further tuning.

Android runtime verification: `adb shell am instrument -w -e class com.example.starborn.CombatStatusTimingDeviceTest com.junewiregames.starborn.prealpha.test/androidx.test.runner.AndroidJUnitRunner`. This checks packaged status behavior, not full gameplay balance. The existing isolated Foundry launcher remains available for visual smoke tests without touching player saves.

Read `FOUNDRY_PLAYTEST_HANDOFF.md` for the acceptance matrix. The previous report is partial. You can now launch real exploration and combat through `FoundryPlaytestLauncherTest`; missing desktop Computer Use access does not prevent instrumentation or screenshot capture. Continue the unfinished tests, implement test-only measurement helpers as needed, and recommend production fixes without applying them.

Latest follow-up: read `FOUNDRY_PROGRESSION_AUDIT.md`. The 720-fight two-AP comparison is complete, preserved at `test-results/foundry-ap-30-seed`. Do not repeat it as unfinished work. Reproduction uses seeds 41–70, `FOUNDRY_LOADOUTS='weak_ap,purchased_ap'`, and `FOUNDRY_POLICIES='support,pressure'` with the same driver command below. Two fixture-validation tests pass. Next work is the audit's route-reward ledger and campaign-arrival/attrition acceptance gate. Do not count displayed quest rewards plus event grants twice, assume legacy relay AP is reachable, or label level-9/shop fixtures campaign-earned. Report suspected production reward defects without fixing them.

## Verified launch path

Latest production work: status durations now count affected-actor actions, with newly applied/refreshed self-effects protected on the casting action. Thirty-seed `all` and `pre9` comparisons completed (627/720 and 586/720 victories; zero timeouts). Evidence: `foundry-owner-turn-all` and `foundry-owner-turn-pre9` under test-results. The broader suite still has World 1/World 2 attrition and finale victory regressions; do not call this release-ready or weaken those assertions. Investigate the retained failures before recommending enemy tuning or publishing. `FoundryStatusTimingAuditTest` now asserts corrected behavior; historical statements that it only characterizes the bug are superseded.

Isolated newer-skill batches are complete: policies `overload`, `disruption`, `crash` each run the pre9 baseline plus one newer offensive skill. At XP 11000, seeds 41–70, `weak_ap,purchased_ap`, `support,pressure`, their wins are 533/720, 506/720, 483/720 respectively (baseline 530/720). Evidence: `test-results/foundry-skill-{overload,disruption,crash}-30-seed/`. Each folder contains only that batch's files. `FoundryStatusTimingAuditTest` reproduces same-action stun expiry and weak expiry on an intervening ally action. Read the newest report section before proposing balance changes: status lifecycle needs an explicit design decision and separate approval for production changes. Do not treat characterization tests as the desired fixed behavior.

Controlled comparison complete: `FOUNDRY_SKILL_POLICY='pre9'` at XP 11000, seeds 41–70, `weak_ap,purchased_ap`, `support,pressure` gives 530/720 victories, with all 720 traces identical to XP 6430. Set the skill-policy variable explicitly (`all` is default). Evidence: `test-results/foundry-level9-pre9-30-seed`. Use summary-listed run IDs only: copied output folders can retain older traces. Next isolate individual newer offensive choices if investigating tactics; do not repeat the completed level confound check. Campaign route/attrition verification remains outstanding.

The level-7 sensitivity batch is also complete: 720 fights, 530 wins, 190 defeats, zero timeouts, preserved at `test-results/foundry-level7-30-seed`. Reproduce with `FOUNDRY_XP='6430'`, seeds 41–70, `weak_ap,purchased_ap`, and `support,pressure`. Set `FOUNDRY_XP='11000'` for the historical level-9 comparisons. The apparent level-7 advantage is confounded by different skill choices; do not interpret it as a level-scaling defect. Before further skill/balance recommendations, hold offensive choices fixed across levels. Main outstanding work remains a traversed route ledger and attrition verification, not another unchanged batch of isolated fights.

Progression update: the apparent quest-XP mismatch is resolved, not an outstanding defect. Nested cinematic callbacks and Zeke dialogue reconcile all 15 quests to 4,525 XP; see the audit and `FoundryProgressionAuditTest`. New recruits inherit leader XP/level when their own entries are absent. `FOUNDRY_XP` now derives fixture levels/unlocks from assets (default 11000; partial-source sensitivity case 6430 = level 7). Always set this variable explicitly when reproducing level comparisons. This is still not a traversed campaign save. Continue encounter-income, spending and route-attrition verification rather than reopening the resolved aggregate-XP question.

Latest completed batch: **1,800 fights, seeds 41–70**, saved at `test-results/foundry-tactics-30-seed`. Read the latest report section before older findings. The driver now honors automatic area/support targeting and defaults to five policies. To reproduce:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:FOUNDRY_SEEDS = (41..70) -join ','
$env:FOUNDRY_LOADOUTS = 'weak,purchased'
$env:FOUNDRY_POLICIES = 'first,support,pressure,defensive_support,defensive_pressure'
.\gradlew.bat :app:testDebugUnitTest --tests '*FoundryLoadoutsTest' --tests '*FoundryCombatDriverTest' --rerun-tasks --console=plain
```

Five policies produce 60 fights per seed; the three original policies produce 36. Defensive variants retain emergency medkits, add legal self-support actions, then follow the named target priority. They are fixed heuristics, not optimal human play. Preserve each batch in a new evidence folder before rerunning: the build summary is replaced. Next priorities are campaign-earned AP/loadout provenance, selective support timing, and remaining mechanics/route checks—not repetition of the completed matrix or production tuning.

This instrumentation test supplies isolated AppServices to the production NavigationHost. Normal app launch still constructs its own services and starts at the main menu. No release scenario menu was enabled. Test output is under the app's external files `foundry-playtest/<timestamp>`, while session/save storage is unique for every test under its cache directory. Instrumentation runs affect the running app process; do not treat the normal app as an interactive play session during testing.

Install directly and run instrumentation instead of Gradle connected tests, whose runner may uninstall packages after completion. Do not clear app data or uninstall. Commands from the repository root in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest -q
$adb = 'C:\Users\jcthomas\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb devices -l
& $adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
& $adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb -s emulator-5554 shell am instrument -w -r -e class com.example.starborn.FoundryPlaytestLauncherTest -e foundryEnemies 'slag_golem,welder_bot' com.junewiregames.starborn.prealpha.test/androidx.test.runner.AndroidJUnitRunner
New-Item -ItemType Directory -Force test-results/foundry-launcher | Out-Null
& $adb -s emulator-5554 pull /sdcard/Android/data/com.junewiregames.starborn.prealpha/files/foundry-playtest/. test-results/foundry-launcher
```

Omit `-e foundryEnemies ...` for exploration at Waste Intake. `-e foundryScenario campaign_w4_mq16` selects the early Foundry checkpoint; mq16 through mq20 are accepted. The enemy list accepts up to four Foundry enemies, including duplicate golems/welders and Titan. Direct party launch is a mechanics fixture; it does not prove a party spawns through its authored room trigger.

`-e foundryHoldSeconds 60` keeps the instrumented scene open for observation; default is zero and maximum is 900 seconds. It does not automatically play the fight. For repeatable policies, extend the instrumentation test or add runtime tests following OpeningCombatRuntimeTest. Use legal UI/CombatViewModel commands; record actual action traces. Compose navigation and animations require `compose.waitForIdle()` before recording a screenshot; a destination assertion alone can pass while the prior screen remains visible. Inspect every evidence screenshot.

## Required next work

1. Test-only `weak` and `purchased` loadouts are now validated against assets, and their 10-seed comparison is complete (see latest report). Neither is a campaign-earned expected build. Next diagnose purchased-gear underperformance, add a documented defensive/support policy, and establish a route-earned AP/equipment snapshot. The emulator launcher's defaults remain a separate smoke fixture. Do not tune production stats.
2. Execute the mechanics cases and core comparison matrix in the handoff, preserving raw results. Add tests for disabled recovery, duplicate counters, usable exposure windows, and supported save boundaries where existing coverage is insufficient.
3. Drive the real exploration path for authored encounter/route tests. Capture real repair, cooling, player opportunities, and outcomes through instrumentation. The screenshot mechanism is already working; do not stop merely because the desktop connector lacks an emulator window.
4. Audit event/moving-party encounter sources and runtime selection semantics. The prior 35-room-variant count only inspected JSON fields, so it is not a completed encounter-source audit or observed frequency distribution.
5. Update FOUNDRY_PLAYTEST_REPORT.md with evidence, measurements, issues, and remaining limitations. Do not claim a balance sign-off from launch smoke tests.

Current concrete visual observation: golem and welder share the same portrait in combat; their names distinguish them, but their silhouettes do not. Evaluate this alongside repair and cooling cues.

The earlier suspected blockers have been investigated. Ollie is a separate character with different gear restrictions (GearRules), not an alias for Nova. Do not transfer his slingshot/vest to Nova. The test launcher now explicitly equips Nova's authored starter kit (mining_pistol/nova_flux_liner) and companion default gear. The Unknown area capture was premature: exploration loads on an IO dispatcher, which Compose idleness alone does not await. The launcher now waits up to 30 seconds for the expected room title. Obsidian Shelf and mixed combat both passed again on the emulator. No production loading or balance change was needed.

## Completed-fight driver (verified)

`FoundryCombatDriverTest` now runs production ATB, AI, skills, damage, statuses, and outcomes with test scheduler time. Presentation/audio services are mocked; legal animation completion callbacks replace UI animation acknowledgments. It records initial derived stats/gear and full combat logs plus a summary at `app/build/reports/foundry-driver`. A snapshot of the verification run is in `test-results/foundry-driver`.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:FOUNDRY_SEEDS = '41,42,43,44,45,46,47,48,49,50'
$env:FOUNDRY_LOADOUTS = 'weak,purchased'
.\gradlew.bat :app:testDebugUnitTest --tests '*FoundryCombatDriverTest' --rerun-tasks --console=plain
```

Default seed is 41. Each seed runs 36 fights: six parties, weak/purchased gear, and first-listed/support-first/drone-first target policies. `FoundryLoadouts` defines equipment and progression; `FoundryLoadoutsTest` checks availability, compatibility, cost and skills. Both loadouts have three medkits, eight level-up skill unlocks through level 9, and no AP stat upgrades. The driver heals below 40% HP and otherwise uses fixed offensive priorities; defensive/support skills are not used. These are attainable equipment brackets, not representative campaign builds. Optional `bare,starter` controls remain available but are not exact replays of the historical driver skill fixture. Current ten-seed evidence is preserved separately at `test-results/foundry-loadouts-10-seed` (211 wins, 149 defeats, zero timeouts). Diagnose gear interactions and broaden policy coverage before tuning.

The first verified batch completed all 36 fights (6 victories, 30 defeats; zero timeouts). Starter gear beat solo golem and welder; bare gear lost all matchups. These counts include identical policies on solo enemies and are not independent player win rates. The test passes on either legitimate terminal outcome and checks finite repairs; it does not assert victory is guaranteed.

Metrics: `commands` counts driver submissions; `actions` counts ActionQueued log entries; `incoming` sums logged Damage to players (does not include unlogged HP effects); `repairAmount` sums logged welder Heal amounts. `opportunities` counts player-command selections while a living enemy is exposed; `exploited` counts selections targeting that enemy, not guaranteed hits or bonus damage. Inspect traces for actual damage/status order. Elapsed time is scheduler time, not human fight duration. Runs stop at 200 logged actions or 20 minutes of scheduler time; a timeout fails the test and preserves evidence.

If Gradle reports access denied to the user-level wrapper lock/cache, request the supported escalated command execution. Do not diagnose that sandbox error as a game issue, bypass it with a different Gradle executable, or install stale APKs after a failed build.

## Handoff prompt

Continue the Foundry playtest using docs/FOUNDRY_LUNA_RUNBOOK.md and docs/FOUNDRY_PLAYTEST_HANDOFF.md. The isolated launcher is built and verified on the emulator. Complete the remaining mechanics, loadout, party-comparison, and route checks; collect screenshots/action evidence; update the report and recommend fixes without changing production balance.
