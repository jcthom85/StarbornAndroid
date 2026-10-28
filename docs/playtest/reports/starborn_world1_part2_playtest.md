# Starborn — World 1, Part 2 of 3 — Playtest Triage

- **Date:** 2026-10-28 (report organized; playtest date not specified)
- **Source:** User-provided voice-recording transcript, World 1 Part 2
- **Status:** Critical fixes and approved Important Considerations implemented locally on 2026-10-28. Physical-action animation/sound feedback in IC-10 is excluded at the user's request.
- **Categories:** Progress Blockers; Critical Bugs/Issues; Important Considerations
- **Counts:** 0 confirmed blockers; 3 critical bugs/issues; 10 important considerations
- **Scope:** World 1, Part 2; several issues may require global fixes
- **ID convention:** IDs are specific to this file (`W1P2-CB-*`, `W1P2-IC-*`) to avoid collision with Part 1.

## Classification rules

- **Progress Blocker:** A confirmed inability to continue or an unrecoverable softlock.
- **Critical Bug/Issue:** An observed malfunction that requires correction during the World 1 cleanup pass.
- **Important Consideration:** A design, UX, presentation, pacing, text, or consistency issue requiring review. These are not automatic implementation orders.
- Preserve uncertainty: where the player says something *might* already have been changed, or a behavior only *appears* wrong, investigate before modifying.

## 1. Progress Blockers

**None confirmed in this recording.** The transcript does not report a softlock or inability to proceed.

## 2. Critical Bugs / Issues

### W1P2-CB-01 — Executive dispenser reward feedback and inventory count disagree; later coffees appear
- **Location:** Staff Lounge; executive dispenser (transcription may refer to its name imperfectly).
- **Observed:** The first dispenser interaction visibly grants one Black Coffee. Subsequent taps still report receiving coffee, but additional coffees do not initially appear in inventory. Later, after moving between rooms and continuing to play, the player noticed five Black Coffees in inventory. The source and timing of those additional coffees have not been established.
- **Concern:** This may involve delayed item grants, stale inventory display/state synchronization, or another reward path rather than merely a repeated false popup. These are investigation hypotheses, not confirmed causes.
- **Action:** Trace each dispenser interaction through the reward event, authoritative inventory count, inventory UI updates, room transitions, and save/load persistence. Identify the source of all five coffees and check whether the same behavior affects other item rewards. Establish whether the dispenser is intended to grant coffee once or repeatedly before changing grant behavior; ensure notifications and displayed counts match actual grants.
- **Status:** Fixed locally. Reproduced a shared inventory StateFlow publication defect: stack quantities mutated previously published entries, suppressing UI emissions. Grants and session counts were immediate; the UI could remain stale until another inventory change. Immutable entries now publish fresh counts on additions and partial removals. The dispenser is explicitly authored as repeatable; its one-coffee-per-tap behavior is preserved. The original recording cannot establish the exact source of all five coffees, but repeated dispenser use reproduces the reported count discrepancy.

### W1P2-CB-02 — Combat music resumes mid-track on new encounters
- **Location:** Combat encounters throughout this section; likely global music state.
- **Observed:** Starting a new battle resumes combat music from its earlier playback position rather than beginning at the start of the track.
- **Desired:** Start combat music from the beginning for **each** battle.
- **Action:** Inspect combat music lifecycle/playback position and reset/restart the track at encounter start. Check transitions when returning from combat.
- **Status:** Fixed locally. Encounter music explicitly requests a fresh start, overriding saved positions and same-track deduplication. Exploration resumes across encounters and return visits within the session; new games and successful save loads clear music history. Android and desktop playback paths are covered.

### W1P2-CB-03 — Extra inconsistent quest-received popup for Lost Shift
- **Location:** Acquisition of the **Lost Shift** quest (verify exact text in game).
- **Observed:** A generic popup resembling `Quest Received: [Lost Shift]` appears **before** the game's normal new-quest popup. The extra presentation does not match other quest acquisitions.
- **Action:** Remove the redundant generic notification and retain the standard quest-acquisition presentation; check for other duplicate trigger paths.
- **Status:** Fixed locally. Removed the authored generic start message from `w1_sq05_start_shunt`; normal quest-acquisition presentation, tracking, task updates, completion, and rewards remain intact.

## 3. Important Considerations

### A. Combat tutorial presentation and encounter design

#### W1P2-IC-01 — Remove redundant combat tutorial labels
- **Location:** Shielded enemy combat tutorial; first popup, plus a later `Combat` notice.
- **Observed:** First tutorial uses a redundant heading resembling `[Combat] Combat Tutorial`. A later attack-related tutorial again displays `Combat` in a way the player dislikes.
- **Action:** Audit heading/title hierarchy; remove repeated or redundant `Combat` labels without losing meaningful instructions.

#### W1P2-IC-02 — Fix shield tutorial overlay timing and positioning
- **Location:** Shielded enemy encounter.
- **Observed:** Combat tutorials no longer obscure the main battle action as badly as in Part 1, but now overlap the top header containing room/enemy information. A `Direct Hit Blocked` explanation appears before the blocked-hit animation/`Block` combat feedback can be seen, hiding the relevant visual result.
- **Action:** Design a tutorial area that avoids both battle visuals and header information. Queue the block explanation **after** the attack/block animation and feedback complete. Coordinate with Part 1 `CB-01` and the UI-event sequencing concern (`IC-07`).
- **Priority note:** Player explicitly classed these as important considerations, not critical bugs.

#### W1P2-IC-03 — Extend shield encounter to teach guard break
- **Location:** Shielded enemy encounter; Hydraulic Kick.
- **Observed:** The enemy dies immediately after using Hydraulic Kick, rather than allowing the player to witness the guard break and perform a follow-up finishing move.
- **Action:** Tune encounter HP/guard and/or scripted tutorial pacing to support one additional meaningful turn/action after the break. Verify it still works with realistic damage variation and other player choices.

### B. Journal, hub, and dialogue clarity

#### W1P2-IC-04 — Group and collapse completed quests in the journal
- **Location:** Journal; completed quests list.
- **Observed:** World 1 will accumulate a sizable list of completed quests.
- **Action:** Group completed quests by area or another sensible category; allow groups to expand and collapse. Consider the list's scalability over all six worlds.

#### W1P2-IC-05 — Review Foreman Boggs's Sector 4 dialogue
- **Location:** Foreman Boggs; dialogue after the cleared/training route.
- **Approximate line from transcript:** `Route's clear. Training is logged. Report when you're ready for Sector 4.`
- **Concern:** The line may be contextually unclear or premature. The player was moving quickly and is not certain.
- **Action:** Check exact dialogue and quest state; ensure Sector 4, training, and reporting instructions make sense at this point. Do not rewrite without inspecting context.

#### W1P2-IC-06 — Make hub Details popup match the UI design system
- **Location:** Hub screen; Details button.
- **Observed:** Details opens a generic-looking popup unlike the game's standard popups.
- **Action:** Verify current implementation (the player notes it may already have changed), then align styling/behavior with established popup components. Related to Part 1's broader hub-screen UX review (`IC-21`).

### C. Combat UI, character building, and consumables

#### W1P2-IC-07 — Simplify enemy Inspect details and improve target selection
- **Location:** Combat > Inspect enemy.
- **Observed:** `HP` and `Stability` are duplicated at left and in the right-side values/bars, making the panel overly busy. The line `Deplete stability to cause a break and create an opening for follow-up attacks` is unnecessary. `Combat Pause` can go; `Tactical Scan Hostile Unit` is potentially acceptable. The `2 of 2` style enemy-switching mechanism is liked, but targeting could be more intuitive.
- **Action:** Remove duplicated left labels and verbose instructional text. Consider removing `Combat Pause`. Allow the player to tap the enemy to select it for inspection, while keeping the existing previous/next enemy switcher.
- **Related:** Part 1 `IC-17` (Inspect cleanup); consolidate before implementation.

#### W1P2-IC-08 — Rename and restyle Reset Build as a character respec
- **Location:** Abilities/character build UI; `Reset Build` control.
- **Observed:** The control looks plain or awkward, and its name is disliked. The displayed number of AP refunded is useful.
- **Action:** Consider `Respec Character` or another clear respec label (the spoken word “respect” appears to mean *respec*). Improve control layout/visual presentation **while preserving the AP-refund display**.

#### W1P2-IC-09 — Clarify combat-only consumables and prevent invalid use
- **Location:** Inventory / black coffee consumable use.
- **Observed:** Trying to use Black Coffee outside combat produces wording along the lines of `Use Black Coffee in combat`. The item category appears too generic.
- **Action:** Consider labeling these `Combat Consumable` rather than just `Consumable`. Disable invalid use attempts or show a concise, consistent explanation. Verify exact copy and behavior before changing.

### D. Cinematics and interaction feedback

#### W1P2-IC-10 — Remove generic cinematic tap prompts; add tactile action feedback
- **Location:** Cinematic popups and exploration actionable words (example: burner lever at Cavern Junction).
- **Observed:** Cinematic popups still show `Tap to Continue` despite the Part 1 concern about unnecessary tap instructions. Separately, activating meaningful world controls via bold actionable words lacks physical feedback.
- **Action:** Remove unnecessary cinematic tap instructions if interaction remains discoverable; add a small animation and/or sound to physical actions like pulling a lever. Track as two distinct implementation subtasks if engineering requires separate ownership.
- **Related:** Part 1 `IC-06` addressed item popups; ensure cinematic and reward prompts are reviewed across the UI.

## 4. Cross-part overlaps / consolidation notes

1. **Tutorial overlays:** Part 1 `CB-01` and Part 2 `W1P2-IC-02` are the same *family* of combat tutorial placement/timing problems. Part 2 confirms improvement in battle visibility, but reveals header occlusion and animation-order problems.
2. **Inspect screen:** Part 1 `IC-17` and Part 2 `W1P2-IC-07` should be one global combat Inspect redesign, not two isolated fixes.
3. **Combat music:** Part 1 noted suspected music resumption; Part 2 `W1P2-CB-02` explicitly classifies it as a critical bug and specifies restarting each battle.
4. **Popup consistency:** Part 1 `IC-06`, `IC-07`, `IC-21` and Part 2 `W1P2-CB-01`, `W1P2-CB-03`, `W1P2-IC-06`, `W1P2-IC-10` suggest global popup, timing, and design-system checks.
5. **Do not confuse `respec` with `respect`:** This is a likely voice transcription error, not a request to use the label “Respect.”

## 5. Suggested immediate cleanup checklist

- [x] `W1P2-CB-01` Trace dispenser grants, inventory/UI synchronization, and persistence; explain the later count of five and assess whether other rewards are affected.
- [x] `W1P2-CB-02` Restart combat music at the beginning of every new encounter.
- [x] `W1P2-CB-03` Remove duplicate Lost Shift quest-received popup.
- [ ] Regression-check shield tutorial overlay/order (`W1P2-IC-02`), prioritizing whether instruction obscures essential information.
- [ ] Defer remaining considerations to the broader World 1 and cross-game design pass unless testing reveals a progression or severe usability failure.

## 6. Instructions for AI coding agents

- **Do not treat this as an instruction to implement every recommendation.** Confirm issues in source and request approval for design decisions when necessary.
- Preserve the distinction among **observed behavior**, **user preference**, **possible transcription error**, and **unverified hypothesis**.
- Prioritize confirmed blockers (if discovered) and critical bugs; avoid expanding scope into unrelated polish.
- When a finding overlaps Part 1, design one coherent global fix and regression-test both playtest locations.
- Preserve existing intended game mechanics unless the user clearly requests a change. For encounter tuning, check damage variance and different player choices.
- Keep issue IDs in commit messages or changelogs so fixes can be traced back to these notes.

## 7. Source filtering

Unrelated television/background conversations and incidental non-game speech were excluded. The source contained no additional actionable progress blockers beyond those documented above.

## 8. Critical-fix validation (2026-10-28)

- Before the fixes, focused regression tests reproduced suppressed stacked-item count emissions and the redundant Lost Shift start message.
- Shared inventory tests cover five consecutive grants, immutable earlier snapshots, partial removal, and consumption. A real dispenser/event/controller integration test checks each live preview count, synchronous session counts, travel, save, and reload.
- Lost Shift's existing start-to-completion regression verifies tracking, tasks, rewards, and absence of the generic duplicate message.
- Music routing and real encounter tests verify that every regular/boss encounter requests a fresh start exactly once. Desktop playback tests verify resumed exploration, combat restarting, background pause/resume, and cleared session history.
- Android instrumentation uses the bundled tracks and real ExoPlayer to verify positions: exploration resumes after battle, each battle (including back-to-back battles) starts fresh, background pause preserves position, and resetting playback history starts exploration fresh.
- These fixes do not implement the ten Important Considerations. Build/device release steps remain separate from local validation.

## 9. Approved Important Considerations — implementation (2026-10-28)

The user approved this pass, excluding animation and sound feedback for IC-10. Original observations above remain as playtest history.

| ID | Implemented behavior |
| --- | --- |
| W1P2-IC-01 | Replaced redundant combat tutorial headings with lesson-specific titles: Momentum & Weaknesses, Break the Guard, and the relevant outcome. |
| W1P2-IC-02 | Nonmodal Android guidance occupies layout space below the encounter header; desktop guidance lives in the context panel. Block/break explanations wait until after the action intro and 1.1 seconds of feedback. Ending a battle clears pending tutorial explanations and records completion. |
| W1P2-IC-03 | The guided shield drill seeds at least 240 HP, allowing Hydraulic Kick followed by an unshielded attack. Ordinary encounters retain their authored stats. |
| W1P2-IC-04 | Completed quests have collapsible World 1–6 groups, with an Other quests fallback. Removed the legacy completed-list eight-quest cap. Desktop search expands matching groups. |
| W1P2-IC-05 | Inspected Boggs's assignment branches and retained their progression gates and lift instructions. His completed-side-quest fallback now confirms cargo clearance/certification and points to the journal, avoiding a potentially stale invitation to report for Sector 4. |
| W1P2-IC-06 | Hub Details uses the dark field-menu palette, readable text, rounded panel, and contextual border. |
| W1P2-IC-07 | Removed duplicate HP/Stability labels, Combat paused badge, and generic break instructions from Android Inspect. Kept relevant broken-turn information and enemy arrows. The bottom-aligned sheet allows taps to reach exposed battlefield enemies; desktop already has direct enemy selection and a compact context inspector. |
| W1P2-IC-08 | Respec Character control displays the AP refund and requires confirmation, with Cancel preserving the build. Android and desktop use matching dark menu styling. |
| W1P2-IC-09 | Combat-only consumables are labelled accordingly and field-use controls are disabled. Shared classification matches the existing item-use restriction while preserving prepared meals and medicine. |
| W1P2-IC-10 | Removed visible generic continuation instructions from cinematic narration cards on Android/desktop. Existing tap/reveal/advance and keyboard behavior remain. Physical-action animation and sound feedback were explicitly left out. |

Validation includes the full combat-controller suite, cooking/field-item rules, World 1 quest progression, desktop menu/ability-tree/Jed popup checks, and Android UI tests for enlarged-text grouping, respec confirmation, combat-only item use, reserved tutorial placement, Inspect selection, and hub navigation/save/quest behavior.
