# Starborn World 1 review and corrections

October 19, 2026. **Automated review passes; human World 1 acceptance remains open.** This record extends the [opening pass](WORLD1_OPENING_REVIEW_2026-10-19.md). It does not certify the whole campaign or replace a phone playthrough.

## Result

The production crew route reaches World 2 using earned resources. A second route completes all five main and five side quests, including the cart-vault detour. A third defeats the Warden before returning to Jed and still completes the world. These controller tests supply authored puzzle answers, use seed 17 and a fixed combat policy, and skip presentation waits. They do not seed inventory/AP or use assistance. Save/reload parity is checked at the handoff.

The final focused run passed **71 Android-hosted JVM tests**, with zero failures, errors or skips. `:app:assembleDebug` succeeded in the same invocation. A separate rendered desktop opening test passed; its accelerated presentation setup does not measure cinematic pacing. Earlier Android smoke tests predate these World 1 changes.

Evidence: [suite totals, room/background inventory and APK hash](evidence/world1-review-2026-10-19/verification.json), [build/test log](evidence/world1-review-2026-10-19/final-verification.log), [main route](evidence/world1-review-2026-10-19/world1-handoff.txt), [completionist route](evidence/world1-review-2026-10-19/completionist-handoff.txt).

## Confirmed defects corrected

| Problem | Correction | Implementation and regression evidence |
|---|---|---|
| Defeating the Warden while the preceding quest was active removed the enemy without satisfying the later launch quest. Returning to Jed then left progression stuck. | Record physical boss defeat independently of quest timing. On re-entering the launch bay with the launch quest active, recover its defeated-boss tasks and stage. No additional combat reward is granted. | `app/src/main/assets/events.json`: `w1_record_warden_victory`, `w1_mq05_recover_early_warden_victory`; `LegalRouteCampaignRunnerTest.productionWorld1CanRecoverIfTheWardenIsDefeatedBeforeReturningToJed`. [Failure reproduced before correction](evidence/world1-review-2026-10-19/early-warden-before.log). |
| Existing saves with actual Warden-defeat evidence could retain the same progression failure. | Restore physical defeat state and, only when the launch quest is already active, its recovery stage/tasks. Do not fabricate Chime authorization, quest completion or rewards. | `gameRuntime/.../session/OpeningNarrativeMigration.kt`; `OpeningNarrativeMigrationTest` tests evidence requirements, unrelated-enemy rejection, no premature quest activation, idempotence and reward preservation. `World1RecentChangesTest` covers a disk round trip. |
| The pod exposed a normal north exit onto the launch rail before the launch interaction. | Remove that exit. Keep the rail definition for compatibility; relocate saves already there to the pod without launching them. | `rooms.json/launch_pod`; migration regression; production route asserts north travel remains in the pod before authorization. |
| Mira's presence used `requires_state`, which the NPC presence model does not interpret. | Gate her appearance using the existing loader-clear milestone. | `rooms.json/workshop_yard/npc_presence`; opening controller asserts absent before loader defeat and present afterward. |
| Patching the Flux Liner created a second copy after Jed had already supplied one. | Remove the duplicate grant; preserve the starter kit. | `events.json/w1_mq01_patch_flux_liner`; `Hub1CriticalFlowTest`. |
| Fighting the loader before reading its diagnostic left an unresolved checklist item. | Allow diagnostic reading or victory to satisfy the task without falsely recording that the strip was read. | `quests.json/w1_mq01`, loader victory event; both opening orders have production route regressions. |
| Opening interactions and preparation guidance were inconsistent. | Connect the authored cargo-netting interaction, add locker inspection, order gear tasks appropriately and clarify cutter requirements. | `rooms.json`, `quests.json`; `NarrativeRuntimeRulesTest`, `Hub1CriticalFlowTest`, desktop opening test. |
| Cooker and lift-rest text referred to the wrong cook or assumed Jed's presence. | Use Cook Vance, direct cooking through the Field Kit and make the rest text independent of Jed. | `rooms.json/pit_kitchen`, `cargo_lift`. The orphan legacy soup event was not connected because it would bypass the current recipe ingredients. |

No encounter stats, AP reward amounts or new loot were introduced. Required certification pays 2 AP, making the observed mandatory World 1 total **8 AP**, not 6. The pacing test and progression records now include it. Main-route and completionist-route AP both finish at 8.

## Coverage

| Area | Examination and limits |
|---|---|
| Room catalog | All 82 World 1 room definitions across 10 nodes were included in structural/reference inspection. This does not mean every room was visited through a controller. |
| Navigation | No missing connection targets. Starting from every node entry and ignoring locks reaches all rooms except retained legacy `launch_rail`. This structural check does not establish unlock order; the three full routes provide scoped progression evidence. |
| Quests | All ten quests completed through the production completionist controller route. Certification, server terminal, medical repair, Scrapper cache, Lost Shift and cart detour are included. Alternate choices and every interruption point are not exhaustive. |
| Room action references | Scoped explicit-action review reported zero World 1 errors. The global validator still reports seven unrelated Sector 9/Astra references; those were not changed here. |
| Artwork | All 82 primary backgrounds exist and are 1088 x 1920 RGB WebP files under `world_assets/src/main/assets/images/rooms/world_1`. Five backgrounds were visually inspected: bunk, checkpoint booth, mine junction, Echo Heart and launch bay. No artwork was modified. Other artwork, sprite/animation states and phone cropping were not comprehensively evaluated. |
| Audio | Nine distinct direct World 1 room/hub cues resolve to available assets. The global audio-reference check reported 114 referenced cues and no catalog warnings. Existence/reference checks do not establish playback, transition timing, loudness or musical quality; no listening sign-off occurred. |
| Combat | Actual combat runs in the route tests, with earned resources and no assistance. Fixed seed/policy success does not prove broad balance. Roaming/patrol encounters and all optional fights are not covered by these routes. |
| Persistence | Handoff parity, early-Warden recovery, legacy rail relocation and scoped progression migration are covered. These checks do not establish Android process-death/lifecycle correctness across every world state. |
| Presentation | Desktop opening interaction test passes. No new complete Android UI or physical-device playthrough was performed for this patch. Cinematics are accelerated in automated routes. |

## Remaining work before calling World 1 finished

1. **Phone acceptance, required.** Play from fresh New Game through the World 2 landing without developer assistance. Review both loader orders, gear teaching, Guard Break certification, Fork acquisition, lockdown instructions, Jed handoff and pod authorization. Check touch targets, scrolling, large text, combat readability, pause/resume and save/relaunch at these boundaries. Record device/build and any failures. Reserve 4–8 focused hours including notes; confidence low until real playtime is measured.
2. **Audio/pacing acceptance, required.** Listen through opening reveal, workshop, server room, mine, boss and escape. Confirm looping, speech/text readability, transition timing, music continuity and effect balance. Automated cue existence does not satisfy this. Allow 2–4 hours alongside the playthrough.
3. **Encounter variation, required for balance confidence.** Exercise patrol/roaming fights and optional detours using earned checkpoint states, multiple seeds and more than one sensible build. Prioritize healing attrition and Pressure Hauler. Do not change stats based solely on static enemy comparisons. Allow 3–6 hours for initial evidence and triage.
4. **Echo Heart visual state, presentation review.** `echo_heart.webp` visibly retains the cyan Fork/cradle after the text says the relic is removed and the cradle is quiet. Review the post-acquisition screen; if this conflicts with intended staging, author a dim/empty state and connect it through the existing background-state support. Estimated 2–6 hours depending on art production; no asset was generated in this pass.
5. **Protocol Override reward promise, content decision.** `quests.json/w1_sq04` promises admin access that speeds future security work. The inspected completion grants a circuit board and XP, and `override_applied` is a local terminal state; no corresponding downstream access benefit was found. Either align the description with the recovered-board outcome (roughly 0.5 hour) or explicitly design and test the intended benefit (estimate pending scope). Current route completion is unaffected.
6. **Lockdown backtracking, human pacing decision.** Existing routes permit backtracking. Decide during play whether urgency and dialogue remain credible. Do not add a point of no return without checking optional content and save compatibility.

The earlier cinematic fade, portrait style and optional workshop tasks were not changed on the strength of old audit claims. Current content/tests explicitly support those choices; human review may still recommend changes. Fishing gear and the server/cooking flows were checked against their actual current gates rather than older proposed content.

## Build and campaign boundary

Reviewable artifact: `app/build/outputs/apk/debug/app-debug.apk`.

SHA-256: `96131c3f122ee5a886b32f3f330ae7e231c99b533a70914976b8de7941644b01`.

It has not been installed on the user's phone by this pass. No commit or deployment was performed. The focused checks are not a rerun of the complete application suite. The previously documented continuous production-campaign failure at World 6's memory-fragments encounter remains open and is not resolved by World 1 success.

World 1 is ready for the human acceptance pass above. It is not yet signed off as finished. Use the same evidence standard for subsequent worlds: actual quest/handoff routes, alternate-order recovery, resource provenance, save parity, asset/reference checks and explicit human presentation acceptance.
