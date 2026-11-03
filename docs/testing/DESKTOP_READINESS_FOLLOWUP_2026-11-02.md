# Desktop readiness follow-up — November 2, 2026

## Scope

Follow-up to the desktop evaluation: repair clear input defects, check existing desktop flows, and align straightforward popup inconsistencies. No gameplay balance, campaign content, installer targets, or save-format changes.

## Changes

- Fishing clears its Space-held latch on key release. Repeated presses can cast, hook, start the fight, and alternate reeling/releasing without changing window focus.
- The fishing integration test now sends keyboard events for casting, hooking, starting the fight, and repeated reel/release. It retains focus-loss pause, secured inventory, and duplicate-reward checks. Previously most input transitions were invoked directly on the controller.
- Rest recovery uses the established exploration modal. It retains recovery text, full-HP feedback, acknowledgement, and repeated rest behavior.
- Meal replacement uses the same modal with explicit Cancel and Replace meal actions. Cooking feedback receives the screen's high-contrast and large-target settings instead of falling back to default exploration settings.
- Key-binding settings explicitly explain that remapping applies to exploration; combat, fishing, and menu controls have fixed shortcuts documented in the controls guide.

## Review conclusions

The desktop has shared gameplay rules/content/saves, platform-specific keyboard/mouse handling and substantial Compose coverage. Automated flow coverage is useful but does not certify a real continuous campaign session. Remaining facility/log dialogs use standard Material dialogs; this is a remaining visual-consistency review, not evidence they all require replacement. Exploration-only rebinding remains intentional and explicit; a comprehensive remapping system would be a larger feature.

## Manual acceptance still required

1. On the live Mac build, start or load a disposable playtest slot. Travel through several rooms with NPCs, long prose, enemies, and service controls; look for first-frame movement and lost focus.
2. Complete dialogue with and without choices using mouse and keyboard. Verify typewriter completion, choice selection, exit, and return of exploration controls.
3. Fight with attack, abilities, items and timing prompts. Cancel targets with Escape/right click, inspect the log, and switch away from the window during an active fight.
4. Buy an item, cook, cancel and confirm meal replacement, rest when injured and when full, and return to exploration. Check modal focus and notice ordering.
5. Fish through at least two catches using Space, then repeat using mouse. During a fight, alternate holding/releasing, change window focus, return, and verify each catch is awarded once.
6. Save to a high-numbered manual slot, quicksave, return to title, load, quit, relaunch, and reload. Preserve valued saves; use a separate playtest slot.
7. Check minimum window size, Retina scaling, available UI scales, fullscreen/borderless/windowed transitions, and an external monitor if available. Verify readable text, reachable controls and restored bounds.
8. Listen to actual NPC voices and music transitions over a sustained session, including background mute/unmute. Decoder/fake audio-line tests cannot certify output quality or device behavior.
9. Unlock Temple Gate naturally, verify the reveal precedes narration, move into the hall, return, save/load, and repeat with reduced motion/disabled flashes. Render previews are not full quest-driven acceptance.

Mac installers/signing and Windows installer/high-DPI/audio acceptance remain separate release work. Running from Gradle on macOS does not certify a distributable Mac release.

## Broad-suite findings and test maintenance

The first full desktop run executed 210 tests: 204 passed and six failed. The failures were investigated against the implementation/content rather than treated as evidence of six gameplay defects:

- Journal grouping still expected `World 1` / `World 2`; current authored groups are `The Mines` / `Sector 9`.
- Crew respec still expected the retired `Reset build` control. Updated the test to cancel the real confirmation, assert no AP/purchase changes, then confirm the actual respec and verify its refund and character isolation.
- Nova's first-AP tutorial expected the retired `[System]` prefix. Current shared tutorial formatting adds `Tutorial` to the title.
- Opening acceptance attempted to click intentionally hidden optional cargo netting. The test now checks its absence and continues the real door-panel/travel/save route; the optional inspection milestone has no consumers in current quests/events.
- Camp recovery expected a free painkiller. Every current camp event contains rest and narrative actions, without item grants. Updated the assertion to reject invented supplies and explicitly injure/rest again after event completion to verify reusable recovery.
- Combat victory asserted rewards before its subsequent wait for the two-second victory presentation. Removed that premature assertion and retained the bounded wait, visible reward assertion, and Continue callback check.

The original full-run log/counts are retained in `evidence/desktop_readiness_followup_2026-11-02/`. Follow-up results are recorded below. No campaign changes were made to satisfy these expectations.

## Final verification

- Initial full suite: 210 tests, 204 passed, six investigated stale-expectation/timing failures; original results retained.
- After the test corrections: all 30 tests in the nine relevant suites passed, with zero failures/errors/skips. This rerun includes all six formerly failing tests, the enhanced keyboard fishing test, actual rest-modal repeat/full-HP checks, cooking replacement cancel/confirm, and gate presentation.
- The production changes were already present during the initial full run. Subsequent edits corrected tests and added assertions; the unaffected suites were not repeated unnecessarily. This is not a claim of a second complete 210-test green run.
- `git diff --check` passed. No Android gameplay/shared runtime/content changes in this follow-up; prior Android validation is not presented as new validation here.
- High-contrast meal replacement was rendered and inspected; an example is retained in the evidence directory.
- Existing desktop instances were left running to avoid losing an active session. Restart after saving to use these changes. No commit, push, installation, or packaging changes were performed.
