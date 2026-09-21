# Foundry playtest report

Date: 2026-09-21

Status: **ISOLATED SKILLS COMPLETE / STATUS-TIMING DEFECT REPRODUCED**

## Latest: isolated level-9 skills

Test-only change: `FOUNDRY_SKILL_POLICY` now accepts `overload`, `disruption`, and `crash`. Each adds just that skill to the pre9 offensive policy while retaining the same level-9 unlocks, stats, gear, medkits and two-AP allocation. Seeds 41–70, two loadouts, six parties, support/drone targeting: **2,160 fights completed, zero timeouts**. All three driver runs passed. Each evidence folder contains only its summary-listed 720 fixtures and traces: `test-results/foundry-skill-{overload,disruption,crash}-30-seed/`.

| Offensive choices | Wins / 720 | Difference from pre9 |
| --- | --- | --- |
| Earlier-skill baseline | 530 | — |
| + Overload Fists | 533 | +3 |
| + Disruption Pulse | 506 | -24 |
| + System Crash | 483 | -47 |

Totals include repeated solo policies and are not independent player win rates. These measure fixed-priority usage, not optimal play or universal skill strength. Changing actions changes random-number consumption. Overload is approximately neutral in aggregate; the two other automatic priorities underperform. Effects are not additive (the prior all-skills batch won 479/720).

### Confirmed timing behavior

`FoundryStatusTimingAuditTest` passed using real skill/status assets and CombatActionProcessor. It characterizes current behavior, not the intended future contract:

- System Crash applies `stun` (duration 1), then the same action's finalization expires it. The target has no stun left for its next action. Both application and expiration are logged. Its advertised shutdown does not survive to an enemy turn in this reproduction.
- Disruption Pulse applies `weak` (duration 2, outgoing damage multiplier 0.75). Its casting action consumes one tick; an intervening ally Defend consumes the other. Both affected enemies can lose the debuff before acting. Full runtime example: `41-weak_ap-4-support-disruption-trace.txt` shows Orion applying weak to both enemies, GH0ST acting, both weak statuses expiring, and then the golem using Molten Slam.
- Root mechanism: `CombatActionProcessor.finalizeAction` invokes `CombatEngine.tickEndOfTurn`, which decrements statuses for every combatant after each action. This also warrants auditing damage-over-time, regeneration, buffs and cooling exposure before any global change.

### Recommendation

Prioritize a separately approved status-duration design/fix before enemy-stat tuning. Define whether each effect expires on the affected actor's action, a full round, or another explicit boundary; ensure duration-1 stun survives application and skips exactly one intended action. Add multi-actor timing, reapplication, skipped-action, DOT/regen, recovery exposure and save/resume tests. Do not simply increase every status duration: that risks magnifying other effects and hiding the lifecycle problem.

Then rerun the fixed-policy comparisons, followed by campaign route-income and attrition verification. No production skill, enemy, status or balance data changed in this audit. The new characterization test should be updated to the intended contract when a fix is implemented, not preserved as a requirement to retain the defect.

## Latest: level held separate from offensive skill choice

After building release 1.3.48, added test-only `FOUNDRY_SKILL_POLICY=pre9`. It retains the level-9 fixture's unlocks/stats but excludes level-9-and-later skills from offensive selection. Seeds 41–70, `weak_ap,purchased_ap`, `support,pressure`, XP 11000: **720 terminal fights, 530 victories, 190 defeats, zero timeouts**. Evidence: `test-results/foundry-level9-pre9-30-seed/summary.txt` and the 720 `-pre9-trace.txt` files identified by that summary.

All 720 corresponding action traces are byte-identical to the XP-6430 batch; normalized summaries also match. Thus the level-7/level-9 discrepancy in this experiment is caused by changed offensive choices, not level alone. This does not establish that every level-9 skill is weak: isolate Overload Fists, Disruption Pulse and System Crash individually before attributing the effect to a particular skill or altering production balance. Route-earned gear/income and attrition remain unverified.

Evidence directories copied from the driver may also contain older trace files because build output is cumulative. Use each batch's summary as the authoritative run manifest, not a directory-wide file count.

## Latest: XP reconciliation and level-7 sensitivity

The suspected XP discrepancy is resolved: nested cinematic completion actions contribute 750 XP and Zeke's dialogue contributes 75 XP beyond the earlier 3,700-XP scan. All fifteen World 1–3 main quests individually reconcile to their listed rewards, totaling **4,525 XP**. No missing-XP defect is established. Source evidence: `test-results/foundry-progression-audit/xp-sources.txt`. New runtime checks verify the authored cutter callback defers its 50 XP until completion, avoids re-awarding it on retrigger, and verifies companion recruitment catch-up. Four progression-audit tests passed alongside the existing six exploration-XP tests, two loadout tests and combat driver.

A partial tally of quest grants plus explicitly named story-victory enemies gives 6,430 XP (level 7), excluding other encounters and optional rewards. It is a sensitivity scenario, not a verified campaign arrival save. The driver now derives level and skill unlocks from `FOUNDRY_XP`; 6,430 XP excludes level-9 skills. Gear, two spent AP, full starting HP and three medkits per isolated fight are unchanged.

Seeds 41–70, two builds, six parties, two policies: **720 completed fights, 530 victories, 190 defeats, zero timeouts**. Evidence: `test-results/foundry-level7-30-seed/`. Wins out of 30, ordered **support-first / drone-first**:

| Party | Starter + 2 AP, level 7 | Purchased + 2 AP, level 7 |
| --- | --- | --- |
| Drone | 30 / 30 | 30 / 30 |
| Golem | 30 / 30 | 30 / 30 |
| Welder | 30 / 30 | 30 / 30 |
| Welder + Drone | 3 / 8 | 0 / 2 |
| Golem + Welder | 29 / 30 | 29 / 28 |
| Golem + Drone | 0 / 24 | 0 / 17 |

The comparable level-9 batch won 479/720. This does **not** prove leveling weakens the player: level-9 unlocks change which attacks this fixed-priority driver chooses, and the changed actions also alter random-number consumption. It shows policy sensitivity and makes an automatic enemy nerf premature. Follow-up should compare level 9 with the level-7 offensive choices held fixed, then assess individual newer skills. No production balance changes or new emulator route coverage in this batch.

Next substantive gate remains an actual route-income/spending/attrition ledger. The partial named-story-enemy tally yields only 1,100 credits, which alone cannot finance the 2,328-credit purchased set. Do not call that set affordable until the remaining income and expenses are reconciled. See `FOUNDRY_PROGRESSION_AUDIT.md` for scope and source details. Earlier sections are historical; their unresolved-XP statements are superseded above.

## Latest: budgeted AP follow-up

See `FOUNDRY_PROGRESSION_AUDIT.md` for source provenance, limitations and the complete results table. Added two test-only builds spending the Administrator's two shared AP on legal Nova speed/GH0ST accuracy root nodes. Production purchase rules and exported stats validate the allocation. Seeds 41–70, two target policies, six parties and two builds completed **720 fights: 479 victories, 241 defeats, zero timeouts**. Evidence: `test-results/foundry-ap-30-seed/`. No production balance changes or new emulator route tests.

Purchased Golem + Welder support-first improves from 20/30 to 30/30; purchased Golem + Drone drone-first improves from 6/30 to 13/30. Purchased Welder + Drone still wins 0/30 under either policy. This allocation helps but does not eliminate the drone-pair difficulty signal.

Important progression caveat: main-quest displayed XP totals 4,525, while explicit main-quest-prefixed event `give_xp` actions total 3,700. Neither is a complete received-reward ledger. A legacy relay event offers another AP but references a quest absent from the active catalog. Level 9, shop affordability and three fresh medkits per battle remain assumptions. Establish actual route rewards and attrition before calling any fixture representative or choosing enemy nerfs.

## Latest: equipment diagnosis and 30-seed tactics comparison

Production balance unchanged. FoundryLoadoutsTest and FoundryCombatDriverTest passed. Seeds 41–70, two loadouts, six parties, five policies: **1,800 completed fights, 1,063 victories, 737 defeats, zero timeouts**. Evidence: `test-results/foundry-tactics-30-seed/summary.txt` and adjacent fixtures/traces. These are headless runtime tests, not new emulator sessions.

### Equipment findings

The purchased set is an area-attack specialization, not a universal upgrade. The exported runtime fixtures establish these differences:

| Character | Starter → purchased speed | Other important tradeoff |
| --- | --- | --- |
| Nova | 16 → 11 | Single-target multiplier 1.0 → area 0.7; damage reduction 3 → 7 |
| Zeke | 10 → 6 | Single-target shock 1.1 → area 0.7; strength 6 → 12 |
| Orion | 10 → 14 | Focus 13 → 8; weapon damage range 6–10 → 4–7; area multiplier 0.65 |
| GH0ST | 16 → 12 | Single-target 1.15 → area 0.75; strength 7 → 12 |

These changes plausibly explain weaker focus-fire and action economy despite some stronger defenses/stats. They do not isolate each item's causal contribution; weapon-only/armor-only comparisons remain useful. Do not relabel this set as an expected stronger build or buff/nerf gear based only on its price.

### Driver correction and strategies

The old driver passed one explicit target even for skills requiring automatic targeting. The driver now uses `targetRequirementFor` and lets the production runtime resolve non-single-enemy skills, matching the UI targeting contract. All offensive baselines were rerun; do not combine older and newer batches as one unchanged experiment.

Added `defensive_support` and `defensive_pressure`: same emergency medkit policy (lowest living HP fraction below 40%), then legal Smoke Bomb/Bulwark usage, or Orion's Nano Repair below 75% own HP when regen is absent, then the existing offense policy. Support abilities use automatic production targeting; Nano Repair is not granted arbitrary ally targeting. Logged support skill uses total 2,716. These fixed heuristics are not optimal play; aggressive buff use can sacrifice needed damage. Smoke Bomb's special-case evasion buff differs from its authored shield status metadata, so this policy principally relies on its legal cooldown rather than tracking that evasion buff.

Wins out of 30, in order **first / support / pressure / defensive_support / defensive_pressure**:

| Party | Starter (`weak`) | Purchased |
| --- | --- | --- |
| Drone | 30 / 30 / 30 / 30 / 30 | 30 / 30 / 30 / 24 / 24 |
| Golem | 30 / 30 / 30 / 30 / 30 | 30 / 30 / 30 / 30 / 30 |
| Welder | 30 / 30 / 30 / 30 / 30 | 30 / 30 / 30 / 30 / 30 |
| Welder + Drone | 0 / 0 / 3 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| Golem + Welder | 18 / 24 / 18 / 22 / 19 | 4 / 20 / 4 / 14 / 5 |
| Golem + Drone | 0 / 0 / 10 / 0 / 7 | 0 / 0 / 6 / 0 / 1 |

### Recommendations and next gate

- Prioritize drone-pair difficulty investigation. Defensive skill availability alone does not resolve the losses in these fixtures. Do not infer a specific enemy nerf from them.
- Preserve the tactical distinction of the welder: targeting it first consistently improves Golem + Welder results.
- Avoid blanket support-skill buffs based on this heuristic. First test selective use tied to incoming threats and verify buff/status timing.
- Establish campaign-earned AP, equipment and arrival level, then rerun suspect encounters and route attrition. Neither current fixture spends AP; three medkits reset each fight. No ordinary-player win-rate claim is justified.
- Complete emulator encounter-route, exposure-window, disabled-recovery and save-boundary coverage before Foundry sign-off. World 4 is the pilot; other worlds follow after this method and progression baseline are validated.

The 360-fight section below is preserved historical evidence and is superseded by this batch for the current driver's behavior.

## Latest: progression-aware loadout comparison

Test-only changes; no production balance changes. Both FoundryLoadoutsTest and FoundryCombatDriverTest passed, with 360 terminal fights and zero timeouts across seeds 41–50. Evidence: `test-results/foundry-loadouts-10-seed/summary.txt` and adjacent per-fight fixtures/traces. This batch supersedes the earlier 36-fight control batch for current driver behavior; earlier observations remain historical.

Both fixtures use Nova, Zeke, Orion and GH0ST at level 9 / 11,000 XP, character defaults plus the eight level-up skill unlocks through level 9, and three medkits per fresh fight. `weak` uses starter equipment; `purchased` uses compatible mid-tier weapon/armor shop equipment. Asset validation checks ownership, shop listings, milestone gates, skill IDs and the 2,328-credit equipment cost. Availability is established, but campaign affordability and expected arrival level are not. Neither fixture spends AP or uses accessories, meals or crafting bonuses. They are attainable equipment brackets, not measured average World 4 builds.

Wins below are out of 10 seeds per policy, ordered **first-listed / support-first / drone-first**:

| Enemy party | Starter gear (`weak`) | Purchased gear |
| --- | --- | --- |
| Magma Drone | 10 / 10 / 10 | 10 / 10 / 10 |
| Slag Golem | 10 / 10 / 10 | 10 / 10 / 10 |
| Welder Bot | 10 / 10 / 10 | 10 / 10 / 10 |
| Welder + Drone | 0 / 0 / 2 | 0 / 0 / 0 |
| Golem + Welder | 5 / 8 / 5 | 2 / 4 / 2 |
| Golem + Drone | 0 / 0 / 3 | 0 / 0 / 0 |

Total: 211 victories, 149 defeats. Policy repetitions on solo enemies are identical comparisons, not independent player samples. The driver heals the lowest living HP fraction below 40%, then uses a fixed offensive skill priority or basic attack. It does not use defensive/support skills, revives or optimal multi-target planning. These are headless production-runtime results, not new emulator playthroughs; UI and campaign attrition remain untested by this batch.

Recommendations before tuning:

1. Investigate why purchased gear underperforms starter gear with this policy. Compare elemental matchups, derived stats and action traces; price does not prove universal superiority. Do not assume this is an enemy-stat defect.
2. Add a documented defensive/support policy and a route-earned AP/equipment snapshot before judging normal campaign difficulty. Drone-containing pairs are the strongest current difficulty signal, but these limited policies cannot establish a nerf amount.
3. Preserve the support-first comparison: starter Golem + Welder improves from 5/10 to 8/10 wins when prioritizing the welder. This is useful evidence of party-composition tactics, pending more seeds.
4. Continue exposure-window testing. In Golem + Drone, first/support policies saw only one sampled exposed-player opportunity across ten fights for each loadout, despite ten cooling actions. Check status timing and actual attack resolution before adjusting duration.

Next test expansion: support/defensive policy, equipment-interaction diagnosis, then 30-seed suspect comparisons and authored-route/disabled-recovery/save-boundary checks. No campaign balance sign-off yet.

## Current findings (supersede earlier blocker claims below)

- The early landing test now waits for its expected room title instead of a fixed delay. The real Obsidian Shelf screen was verified on the emulator; `Unknown area` was captured before asynchronous exploration loading finished. Evidence: `test-results/foundry-launcher/1790013563877/screen.png`. Both exploration and mixed combat were rerun successfully after explicitly assigning starter gear.
- Ollie is a separate character with slingshot/armor_ollie gear rules. His equipment is not a Nova naming mismatch. The test fixture now explicitly assigns Nova mining_pistol and nova_flux_liner, which are awarded by `w1_mq01_receive_starter_kit`. No production gear ownership fix was justified.
- New `FoundryCombatDriverTest` completed 36 actual fights through production CombatViewModel commands: six parties, two explicit weak gear fixtures, three priority policies, seed 41. Six victories, thirty defeats, zero timeouts. The successful outcomes were the starter-gear solo golem/welder cases; the bare fixture lost every case. These are controlled weak fixtures without healing/AP stat upgrades, not representative World 4 balance estimates.
- Real logs contain golem cooling, exposure application/expiration, welder repairs, player attacks/skills, HP loss, and terminal outcomes. The solo bare welder used two repairs for 96 logged healing. In starter Golem + Welder, support-first produced two repairs and 44 logged actions; first-listed produced one repair and 30 actions. Both lost, so this is a tactical/pacing observation, not proof of an optimal strategy.
- Cooling sometimes produced no sampled player command opportunity in mixed parties. Investigate global status ticks and ATB timing across additional seeds before changing duration. The exported `exploited` counter means targeting an exposed enemy, not proof of a successful hit; full logs are the evidence for damage.

Evidence: `test-results/foundry-driver/summary.txt` and per-run `*-fixture.txt`/`*-trace.txt`. Reproduction instructions and metric limits are in `FOUNDRY_LUNA_RUNBOOK.md`. Earlier report sections below are historical, including the now-superseded route and ownership blocker claims. The remaining task is to establish ordinary World 4 loadouts, repeat the comparisons, and complete authored-route/save/disable coverage. Production balance remains unchanged.

## Follow-up: isolated launcher verified

The earlier UI blocker has been resolved using Android instrumentation. `FoundryPlaytestLauncherTest` renders the real navigation/exploration/combat screens using isolated AppServices persistence. Its mixed-party launch passed on emulator-5554 and a screenshot was inspected showing Slag Golem + Welder Bot with Nova, Zeke, Orion, and GH0ST. Evidence: `test-results/foundry-launcher/1789999881174/screen.png` and its adjacent fixture/destination files. Earlier launcher-development captures are intermediate evidence and must not be counted as successful combat captures.

The device reports SDK 37 / Android 17; the original API 35 entry below was incorrect. Launcher instructions are in `docs/FOUNDRY_LUNA_RUNBOOK.md`. The default fixture remains uncalibrated; this launch smoke test does not establish combat balance. Direct party launch bypasses authored encounter triggering, so route coverage still requires the exploration mode.

## Follow-up: direct party matrix

Seven direct-party runs completed on the emulator with isolated persistence: solo Magma Drone, solo Slag Golem, solo Welder Bot, Welder + Drone, Golem + Welder, Golem + Drone, and Titan. Every instrumentation run passed and captured a real combat screen. The mixed-party evidence shows distinct Magma Drone art and separate names/health bars, while the Golem and Welder still share the same portrait/silhouette. The Golem + Drone screen also showed multiple party targets carrying burn indicators after the opening exchange. These are presentation and smoke observations, not win-rate or pacing measurements because no legal player turns were driven.

Evidence is under `test-results/foundry-matrix/`; the most useful captures are `1790001239625/screen.png` (Golem + Welder) and `1790001253526/screen.png` (Golem + Drone). All seven destinations were `combat/{enemyIds}`.

The campaign exploration launch was also executed for `campaign_w4_mq16`. It passed the instrumentation assertion for the exploration route, but the captured screen displayed an `Unknown area` header on a black background despite the fixture state being `roomId=foundry_slag_landing` and `hubId=hub_7_slag_pits`. This blocks authored route/encounter testing and should be fixed or explained before using the campaign checkpoint for balance work. Evidence: `test-results/foundry-route/1790001334852/screen.png` and `fixture.txt`.

No production balance or combat data was changed during this run.

## Tested build and environment

- App version from the installed APK: 1.3.47 (131).
- Application ID: com.junewiregames.starborn.prealpha.
- Device: emulator-5554, sdk_gphone16k_x86_64, Medium_Phone AVD, API 37 / Android 17 (verified by getprop).
- Debug APK built with :app:assembleDebug successfully and installed with adb install -r successfully.
- The desktop Computer Use connector did not expose an emulator window. Direct visual/tap-through of Foundry rooms could therefore not be completed. The app was launched through its exported activity and the main-menu screenshot was captured.

Evidence:

- Main-menu screenshot: test-results-foundry-mainmenu.png.
- Instrumentation XML: app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 17-_app-.xml.
- Instrumentation logs: app/build/outputs/androidTest-results/connected/debug/Medium_Phone(AVD) - 17/.

## Automated baseline

Unit command: :app:testDebugUnitTest with EnemySupportDecisionTest, Combat*Test, and DataIntegrityTest, rerun from scratch.

Result: **PASS, 83 tests, 0 failures, 0 errors.** This covers data integrity, combat targeting/damage/status, viewmodel behavior, enemy support decisions, and the opening combat runtime. It does not establish player-facing balance or readability.

Device command: :app:connectedDebugAndroidTest with DebugSystemScenariosInstrumentedTest.

Result: **PASS, 3/3 instrumentation tests, 0 skipped, 0 failed.** These exercise isolated AppServices and the real Android runtime for fishing, crafting/cooking/save-load, and shop constraints. They do not reach a Foundry combat.

## Encounter-source audit

The effective Foundry room inventory contains 35 party variants across 71 room records. The source is mixed: some encounters use enemy_parties, while others use the room enemies fallback. Counting both fields as independent encounters would double-count mirrored data.

Notable effective parties include:

- Solo Magma Drone, Slag Golem, Welder Bot, Flame Trooper, Phantom Prototype, Conveyor Crusher, and Titan Walker.
- Slag Golem + Magma Drone at Waste Intake.
- Welder Bot + Magma Drone at Conveyor Belt and the optional Waste Intake scale-final room.
- Flame Trooper + Phantom Prototype at Power Core.
- Slag Golem + Welder Bot at the optional Power Core scale-final room.

This confirms useful composition variation exists in the Foundry data. It does not establish selection frequency, route exposure, or whether the optional pairs are fair; those require reachable campaign play.

## Fixture caveat

DebugCampaignFixtureBuilder assigns World 4 party members to level 9 and supplies progression items, but it does not explicitly establish equipment slots/stats. A future balance run must verify or define legal early- and late-Foundry loadouts before comparing outcomes. The current run intentionally does not infer balance from level alone.

## Acceptance results

| Area | Result | Evidence / limitation |
| --- | --- | --- |
| Build and install | PASS | Debug build and streamed install succeeded. |
| Existing unit combat checks | PASS | 83/83 selected tests. |
| Android runtime smoke | PASS | 3/3 existing instrumentation tests. |
| App launch/main menu | PASS | Captured screenshot; no startup crash in observed logcat. |
| Foundry room-source audit | PASS (static) | 35 effective party variants inventoried. |
| Golem cooling in real battle | NOT RUN | Combat screen launches, but no legal player turns were driven. |
| Welder repair/readability in real battle | PARTIAL | Real mixed-party screens captured; repair activation not observed. |
| Mixed-party balance matrix | PARTIAL | Seven party screens pass; no outcome/action distributions. |
| Save/resume during battle | NOT RUN | No supported turn-level save procedure exercised. |
| Screenshots of repair/cooling cues | PARTIAL | Combat screenshots captured; signature actions not reached. |
| Authored Foundry route | BLOCKED | `campaign_w4_mq16` renders Unknown area from a valid Foundry fixture. |

## Recommendations (no changes applied)

1. Fix the Foundry campaign landing render or its node/room initialization before route balance testing. The current screen is a reproducible runtime/fixture failure, not a balance result.
2. Correct the test fixture’s equipment ownership: the active player is `nova`, but the snapshot equips `basic_slingshot` and `basic_vest` under `ollie`. Until this is corrected in a test-only fixture, loadout comparisons are invalid.
3. Add legal turn driving or a runtime action trace around the existing launcher. The direct matrix proves composition screens render; it does not measure wins, action count, repair amount, cooling windows, resource drain, or player readability during those actions.
4. Resolve distinct Golem/Welder art or add an unmistakable role cue before art expansion is considered complete. Names and bars separate them mechanically, but the shared silhouette makes target recognition slower.
5. Keep numeric balance unchanged until the route and fixture issues are corrected and the handoff matrix has actual outcome/action evidence.

## Completion boundary

The safe portion of the handoff is complete: build, automated baseline, device smoke, static encounter audit, and evidence report are recorded. The visual and reachable-combat portions are explicitly incomplete rather than marked pass.
