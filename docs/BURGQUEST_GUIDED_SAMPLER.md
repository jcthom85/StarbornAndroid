# BurgQuest guided sampler — September 23, 2026

## Status and intent

Prepared for **1.3.54 (138)** on top of released 1.3.53 (137). Signed release build and internal-track upload are in progress; publication is not yet confirmed. World testing remains paused at [the overall checkpoint](OVERALL_PLAN_CHECKPOINT.md).

The intended short visit is **meet the crew → Tactical Combat → Astra homecoming → one optional activity → farewell**. Aim for 5–10 minutes including reading and choices; this is not a measured newcomer-duration claim. The emphasis is crew and adventure, not demonstrating every system.

## Implemented experience

- Title: BurgQuest Demo and Settings. New Game, Load Game and Debug Scenarios are temporarily hidden; saves and their underlying functionality remain intact.
- Picker: one primary “Start the Starborn sampler” button, then the four standalone experiences. Existing `burgfest_*` IDs remain unchanged. Titan is explicitly harder; Story is a reading-focused alternative, not a short route to combat.
- Combat: a short introductory briefing, detailed advice in Demo → Demo guide, and no added persistent tutorial bar. Existing enemies, equipment, skills, supplies and production balance are unchanged.
- Combat victory, defeat and retreat all offer Visit the Astra. Retries start fresh. The Astra transition is an explicitly fresh, stocked fixture, not inventory/health carryover.
- Homecoming: six skippable lines with existing portraits/dialogue styling and existing character murmurs. Visible Show text / Next / Explore the Astra controls supplement tapping the dialogue. Presentation-only, once per homecoming visit; no campaign dialogue triggers, rewards or progression. Direct Astra launches skip this scene.
- Astra guide: talk to Orion or Gh0st, then choose Deep Mine Asteroid Drill or the west cargo-bay workbench's stocked Cryo-Inductor craft. Other cabinets remain available. Nothing is required to finish.
- Farewell: invitation to chat at the booth; Return to title and Explore another demo. The current title artwork remains behind guides/results after live sessions are released. No external links, accounts or new analytics.
- Root Back opens the Demo menu in exploration/combat; nested screens keep their normal handling. Finishing unmounts the demo session and releases its ViewModels, loops and audio. Launch failures offer a safe picker/title return.

## Verification

- Debug app and instrumentation APKs built successfully.
- Full unit suite: **552 tests passed, zero failures/errors/skips**.
- First offline emulator pass: **12/12 tests passed in 244.065 seconds**, covering the navigation/fixture/save checks, three recovery checks, six first-tap arcade launches, 24 combat formation combinations (1–4 crew, 560/720dp heights, 1/2/5 enemies), and the new sampler checks.
- After visible dialogue controls/backdrop polish: **5/5 targeted tests passed in 208.949 seconds**. The flow completed two real UI-commanded Tactical victories (33.014s and 31.236s, automated inputs, not newcomer timing), fresh retry, the entire six-line scene, Orion's real ship dialogue, arcade finish, Titan/Story launch, title return and unchanged campaign saves. Workbench crafting also passed.
- Ten successive Astra visitor sessions alternated guided homecoming and standalone launch, checked restored inventory/HP, scene skipping, guide reopening without scene replay, nested arcade Back handling, root demo-menu Back, and finishing. The audio logs show 45 distinct players initialized and all 45 released during the targeted run (51/51 in the earlier broad pass). This supports lifecycle cleanup; it is not a venue listening test or an exhaustive heap-leak audit.
- Defeat/retreat action routing is checked by rendering each result state and exercising its callbacks, not by claiming full losing fights were manually played. Failed-launch recovery is exercised through an injected launch failure. Fixture reset and physical campaign session/autosave/manual/quick-save isolation are separately checked with actual packaged services.
- Evidence: `test-results/burgquest-sampler-instrumentation.log`, `burgquest-sampler-final-instrumentation.log`, `burgquest-sampler-final-runtime.log`, and screenshots/UI trees under `test-results/burgquest-sampler-final/`. These screenshots precede the final one-line switch from legacy fallback art to the current title backdrop.
- Final backdrop build, offline compact-display pass: **2/2 tests passed in 143.014 seconds**, covering the complete visitor flow (including two Tactical victories/retry, all crew-scene controls, Orion dialogue, arcade finish and alternate scenarios) and all combat-result actions at **945×1680px / 420dpi (360×640dp), font scale 1.3**. Screenshots/UI trees: `test-results/burgquest-sampler-compact/`; log: `test-results/burgquest-sampler-compact-instrumentation.log`.
- Visual inspection confirmed the current title art, readable homecoming/farewell controls and a reachable primary sampler action. At the compact display with enlarged text, existing room/combat headings and action labels truncate, and scaled enemy labels are small. No new combat-layout overhaul was made in this pass; verify readability at the actual booth configuration. Passing navigation does not certify perfect accessibility at every display/font combination.
- Emulator size was restored to 1080×2400, font scale to 1.0, and Wi-Fi/mobile data to enabled. No app uninstall or save-data clearing was performed. Automated implementation checks are complete; human/booth sign-off remains below.

## Booth operator check — still required

Run three newcomers on the intended device without coaching unless they are stuck. Record first-action time, whole-visit duration, requests for help, missed exits, a remembered crew member/moment, and whether they want more (and why).

Target: all three reach farewell without rescue; at least two naturally finish in 5–10 minutes; at least two remember a crew member or character moment. This is qualitative feedback, not statistical proof. Fix comprehension/navigation before considering any demo-only encounter adjustments.

Check actual-device volume in venue noise, text/tap size, brightness, charging, offline launch, reset for the next visitor and the installed version. Automated combat timings cannot replace this check.

## After BurgQuest

In `MainMenuScreen.kt`, restore the three grouped title flags: `showNewGameForPublicRelease`, `showDebugScenariosOnTitle`, `showLoadGameOnTitle`. Debug Scenarios remains additionally gated by `BuildConfig.ENABLE_SCENARIO_MENU`. No saves need migration or restoration.

Retain BurgQuest Demo unless separately asked to remove it. Resume the world/finale plan from the overall checkpoint, not from historical prep to-do lists.
