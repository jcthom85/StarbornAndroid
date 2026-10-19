# Starborn World 1 review: opening pass

Historical opening-pass record. The subsequent [World 1 review](WORLD1_REVIEW_2026-10-19.md) extends this evidence through all ten quests and early-Warden recovery; use it for current status.

October 19, 2026. **Opening corrections verified; World 1 review remains in progress.** This pass covers the bunk-to-cutter sequence, with a controller regression through the main route to World 2. It is not human gameplay or audiovisual sign-off.

## Confirmed corrections

| Issue | Correction and evidence |
|---|---|
| Jed's starter kit gave one Flux Liner, then patching created another | Removed the extra reward from `events.json/w1_mq01_patch_flux_liner`. A regression first reproduced the count increasing from 1 to 2. Jed's starter-kit grant remains. |
| Starter gear appeared after the loader in the checklist, despite being given in Jed's bunk | Reordered the existing task after talking to Jed and corrected `quests.json/w1_mq01/report_to_jed`. Stable IDs remain unchanged. |
| Fighting before reading the diagnostic left an unchecked objective | The task now permits reading the strip **or** disabling the loader. Victory resolves it without fabricating `ms_w1_mq01_loader_inspected`; reading afterward remains possible. This was a checklist inconsistency, not a proven hard blocker: the cutter event explicitly completes the quest. |
| The cutter's refusal message claimed preparation was already complete | Updated `rooms.json/workshop_floor/live cutter test` to name the needed grounding and bypass steps. Tests also verify the cutter cannot complete before both preparations. |
| The bunk named possessions without connecting their authored interaction | Connected cargo netting to existing `w1_mq01_check_netting` metal-bird narration and added a locker inspection. Both are hidden in darkness and named in both lit variants. No item rewards or mandatory chores were added. |

The combat-budget fixture now gets its free armor from the actual starter-kit reward. No combat stats, enemy tuning, AP award amounts, plot events or save schema changed.

## AP accounting correction

The real World 1 route earns **8 mandatory AP**: 2 after Wake Up Call, 2 from Guard Break certification, 2 after The Echo and 2 after The Launch. Despite its `w1_sq03` ID, certification gates Boggs' mine authorization and is not optional.

Including it, the core campaign budget floor is 8/16/26/36/44/50 across Worlds 1–6 before the finale. The five remaining AP-paying side quests add 10; their optionality in later worlds was not re-audited here. This corrects the earlier classification of 6/14/24/34/42/48 plus 12 "optional" AP without changing any reward. `CrewProgressionPacingTest`, the readiness record and the [adoption record](CREW_PRODUCTION_ADOPTION_2026-10-19.md) now reflect it.

## Earlier claims revalidated

- **Wake-up fade:** `IntroCinematicAssetIntegrityTest` explicitly preserves the slow 8-second fade with no one-shot buzzer. Both renderers animate a reveal; the controller clears forced black before its extra 2.2-second completion delay. The older report's 10.2 seconds of black claim is inaccurate. Subjective timing and actual audible room/music behavior still need playback review. No duration or audio changed.
- **Unknown-speaker portraits:** current tests explicitly retain contextual Orion portraits while keeping the speaker unnamed. Removing them would reverse a current presentation contract; that is a design choice for human review.
- **Cutter-aftermath popup:** the named old aftermath event is absent. The active completion chain starts Jed's handoff dialogue and contains no redundant `show_message`. Generator/relic presentation is outside this opening inspection.
- **Workshop training stage:** current `w1_sq03` already labels its workshop setup tasks optional and Security Post certification mandatory. The main route passed without the workshop tasks. Do not delete that stage solely to satisfy the old report; inspect the optional route separately.

The October 16 quality score was not adopted. Generated headless transcripts are route evidence, not exact accounts of a blind player's screen experience.

## Verification

**45 focused Android-hosted JVM tests passed** in two runs: 43 opening/combat/story tests, then 2 pacing tests. **One desktop UI test passed.** The full 699-test suite was not rerun for this content pass.

| Suite | Passed |
|---|---:|
| `Hub1CriticalFlowTest` | 15 |
| `NarrativeRuntimeRulesTest` | 4 |
| `OpeningCombatRuntimeTest` | 17 |
| `OpeningNarrativeMigrationTest` | 3 |
| `IntroCinematicAssetIntegrityTest` | 1 |
| `LegalRouteCampaignRunnerTest`, selected opening/handoff cases | 3 |
| `CrewProgressionPacingTest` | 2 |
| `DesktopOpeningAcceptanceTest` | 1 |

Both opening routes start with no seeded inventory/AP, win the actual loader fight, craft the Cryo-Inductor, complete every Wake Up Call task, earn 2 AP, retain one equipped Flux Liner and preserve their save. One reads the diagnostic; the other fights first. The full World 1 controller route clears all five main quests plus certification, wins six authored encounters at seed 17, reaches `sector9_crash_site` with `w2_mq01` active, and preserves its full saved state and 8 AP on reload. No assisted actions or synthetic victories were used.

These routes use authored puzzle answers, drain cinematics and follow a fixed combat policy. They do not establish balance across seeds, optional-content completion or enjoyable pacing.

The rendered desktop check clicks the bunk light, connected netting and door, then verifies movement and existing save controls. Its first run passed in 235 seconds while advancing presentation. The test now controls its startup clock, drains presentations through supported skip/completion APIs and uses a recording audio driver; the final test took 14.152 seconds. This improves test isolation, not the production renderer. Initial reward-fixture compilation and capitalization-sensitive assertion errors were corrected before the final passing runs.

Other checks:

- Changed JSON parses; `git diff --check` passes.
- Explicit action references checked across **all 82 World 1 room definitions: zero errors**. This is a target/syntax check, not comprehensive room review. The global validator still reports seven pre-existing Sector 9/Astra references.
- Metadata checked for the bunk, Jed's bunk, yard and workshop backgrounds: all 1088 x 1920 RGB WebP. Only the bunk artwork was visually sampled. No artwork changed; no listening-quality judgment was made.
- A current Android debug APK was built at `app/build/outputs/apk/debug/app-debug.apk`. SHA-256: `3ba65449f8d312ef47fe147b301729c815d8cc7243b82e20098834db7ebff0d1`. It was not installed on the physical phone or published. The earlier Android smoke report predates these content edits and does not certify the new room interactions on Android.

See [retained verification and controller reports](evidence/world1-opening-2026-10-19/verification.md). Local logs: `tmp/world1-opening-tests.log`, `tmp/world1-pacing-correction.log`, `tmp/world1-desktop-opening.log`, `tmp/world1-opening-android-build.log`. Filtered Gradle runs replace earlier XML; the table describes the distinct recorded runs.

## Remaining World 1 passes

1. **Checkpoint and mine entry:** actual Hank/Zeke directions, both introductions, Boggs' certification routes, elevator/hub convention and save/reload at handoffs. Revalidate old locations and quotations before editing.
2. **Mine, relic and escape:** cart/power puzzle, clues, lockdown/backtracking, Jed/lift sequencing, rest dialogue, launch-rail access and World 2 transition. Controller success does not establish continuity or navigation clarity.
3. **Optional content:** remaining side quests, workshop practice, loot, cooking/fishing access, dead-end purpose and patrol balance. These have not received completion sign-off.
4. **Human Android acceptance:** fresh game, tutorials enabled, no route answers or debug grants. Measure first meaningful input, first fight, AP and purchase; review cutter-to-AP prompt timing, readability, touch, audio and interruptions. Judge the existing fade during actual playback.

World 1 is not signed off. The separate World 6 balance failure remains open; removing a duplicate suit has not recertified later-world finances or supplies.
