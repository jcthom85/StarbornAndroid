# Starborn — World 1, Part 1 of 3 — Playtest Triage

- **Date:** 2026-10-27
- **Source:** Voice-recording transcript, World 1 Part 1
- **Status:** Initial triage, not independently reproduced
- **Categories:** Progress Blockers; Critical Bugs/Issues; Important Considerations
- **Counts:** 0 confirmed blockers; 5 critical issues (one conditional on verification); 23 important considerations
- **Scope:** World 1, Part 1; several findings may affect the entire game.

## Classification rules

- **Progress Blocker:** Confirmed issue preventing forward progress or causing an unrecoverable softlock. No confirmed blockers in this session.
- **Critical Bug/Issue:** Reproducible or directly observed usability/tutorial defect that seriously impairs the experience and should be fixed in World 1 cleanup. Verification requirements are stated explicitly.
- **Important Consideration:** Design, UI/UX, tutorial-flow, pacing, balance, polish, or regression concern that needs assessment but should not automatically stop the playtest.

## 1. Progress Blockers

**None confirmed.** Quests appeared to trigger and complete, and the player continued into the Med Bay section.

### Potential blocker regression tests (not confirmed bugs)

- **PB-CHECK-01 — Quest-critical items:** Verify that essential items cannot be permanently sold or lost unless there is a reliable reacquisition path. Related: IC-23.
- **PB-CHECK-02 — Puzzle-room departure:** Verify that exiting Jed's Shutter mid-puzzle cannot softlock progress or corrupt quest/puzzle state. Related: IC-13.

## 2. Critical Bugs / Issues

### CB-01 — Combat tutorial messages obscure combat
- **Location:** Scrapyard; Faulted Loader tutorial encounter.
- **Observed:** Messages such as the prompt indicating Nova is ready to act cover combat animations.
- **Action:** Reposition tutorials to avoid important visuals. Evaluate combat command menu placement at the same time.
- **Status:** Observed; fix needed.

### CB-02 — Stability Broken tutorial appears on enemy death
- **Location:** Scrapyard; Faulted Loader tutorial encounter.
- **Observed:** Stability Broken appears when the enemy dies, undermining the explanation of a stability break.
- **Action:** Trigger at a demonstrable stability break while the enemy remains alive, or demonstrate it in another suitable encounter.
- **Status:** Observed; fix needed.

### CB-03 — Jed dialogue overlaps tinkering menu
- **Location:** Jed's workshop; after tinkering tutorial.
- **Observed:** Jed resumes dialogue before the tinkering UI is exited; dialogue boxes overlap the menu.
- **Preferred action:** Complete tutorial, close tinkering UI automatically, pause ~1–2 seconds, resume Jed dialogue in exploration UI.
- **Status:** Observed; fix needed.

### CB-04 — Skills tutorial overlaps dialogue and skips navigation instruction
- **Location:** Jed's workshop; Skills tutorial.
- **Observed:** Dialogue overlaps tutorial; tutorial opens Party menu but fails to instruct player to select Skills.
- **Action:** Guide player explicitly to Skills; pause/resume dialogue around tutorial completion.
- **Status:** Observed; fix needed.

### CB-05 — Verify Skills vs Abilities terminology
- **Location:** Combat UI and tutorials.
- **Concern:** Commands may be called both "Skills" and "Abilities."
- **Action:** Verify inconsistency, decide canonical term, and normalize UI/tutorial text globally.
- **Status:** Needs verification; critical only if inconsistent use is confirmed.

## 3. Important Considerations

### A. Opening sequence and onboarding

#### IC-01 — Intro cinematic pacing
Review cinematic timing in the introduction. No specific timing correction yet defined.

#### IC-02 — Simplify Novus Bunk interactions
Review actionable words in the opening room. Prefer an obvious "Bunk Light" interaction rather than confusing sleeping-shelf wording; assess limiting primary options to Bunk Light, Sleeping Shelf, and Door Control Panel instead of also offering Cargo Netting and Rusty Locker. Confirm actual labels and interactions in code before changing them.

#### IC-03 — Consistent tutorial labels
Audit naming/formatting such as Tutorial: Navigation, Tutorial: Exploration, Tutorial: Combat. Current presentation is acceptable provisionally.

#### IC-04 — Rest sound effect
Resting/sleeping has no sound. Add suitable audible confirmation.

#### IC-05 — Embedded tutorial card presentation
Review whether equipment and tinkering instruction cards within system interfaces are sufficiently prominent/clear compared with pop-up tutorials.

### B. Notifications and tutorial sequencing

#### IC-06 — Simplify item acquisition notifications
Consider removing "Tap to Continue"/"Tap for Next" prompts and multi-item counters like "1 of 5"; avoid unnecessary UI clutter.

#### IC-07 — Global UI-event sequencing
Dialogue, item-acquired messages, and tutorials sometimes overlap. Implement or enforce an event queue that suspends dialogue for a notification/tutorial and resumes it after dismissal, avoiding stacked overlays. CB-03 and CB-04 are specific manifestations.

#### IC-08 — Supplies and Key Items tutorial timing
The tutorial feels oddly placed. Review trigger and consider teaching inventory use when immediately relevant, potentially via MedKit use.

#### IC-09 — Stronger new-quest presentation
Differentiate new quest acquisition from completion: slower/deliberate presentation with subtle background darkening.

#### IC-10 — Currency acquisition feedback
Winning 75 credits at the dice table does not show a reward popup. Standardize a visible credit-gain notification consistent with item gains.

### C. Exploration, puzzles, and progression

#### IC-11 — Redesign Jed's Shutter puzzle
The Deflux Liner / Mining Cutter / Live Cutter interaction sequence feels weak. Reevaluate the puzzle design rather than polishing it unchanged.

#### IC-12 — Vary puzzle mechanics
Ventilation Hub repeats Jed's Shutter's three-actionable-words-in-order pattern. Increase variety in early puzzles.

#### IC-13 — Consider puzzle-room movement constraints
Decide whether player should be temporarily kept in Jed's Shutter while solving the puzzle; reconsider after redesign. Also run PB-CHECK-02.

#### IC-14 — Explain Jed's Shutter naming
Evaluate whether "Jed's Shutter" has narrative/contextual meaning clear to players. This is a question, not a confirmed defect.

#### IC-15 — Great Frontier VHS distribution
Two early Great Frontier VHS tapes may be acquired within ~20 minutes. Audit placement of all 10 collectible tapes and pace rewards.

#### IC-16 — AP rewards and economy
Wake Up Call awards 2 AP. Verify if AP is per-character vs shared, whether two is too generous, and whether 1 AP would better teach spending. Audit across World 1 and global progression.

### D. Combat interface and mechanics

#### IC-17 — Clean up Inspect UI
Capitalize command labels consistently (e.g., Defend). Consider naming "Stability Gauge" simply "Stability" and "Integrity" "HP"; remove redundant break-mechanic explanation. Change "Discovered Weaknesses/Resistances" to "Weaknesses/Resistances" and display unknown entries with question marks instead of a descriptive sentence. Keep the otherwise useful Inspect functionality.

#### IC-18 — Remove snack detail toggle
Consider removing "Snack Details / Hide Snack Details" from the bottom of combat menu if not needed.

#### IC-19 — Rework Active Effects panel
Tapping status-effect icons over combatants accidentally opens an awkward/poorly laid-out window. Consider relocating details to Inspect or another deliberate access pattern.

#### IC-20 — Combat feedback text overflow
Popups such as "Weakness Exploited" have text clipping/overflow; simplify copy and fix layout.

### E. Maps, menus, shops, and equipment

#### IC-21 — Map/hub UX pass
- Minimap utility symbols crowded: consider replacing room dots with prioritized icons; assess main map too.
- Hub menu should resemble standard exploration menu; verify Quick Save functions from hub.
- Revisit menu button placement/label (currently not obviously labeled "Menu").
- Revisit Track Quest presentation.
- Revisit selected-node details, room description, and Enter button.

#### IC-22 — Weapon mod tutorial trigger
"Weapon Mod Slots Are Now Unlocked" tutorial appears at first Transit Checkpoint entry, potentially after the player could already equip a mod. Consider triggering on receiving a mod at the dock, conditioning it on the optional quest, or revisiting whether that quest should be mandatory.

#### IC-23 — Shop affordance and safe key-item sales
Consider a Shop pill/indicator for the Contraband Shop rather than only bold actionable text. Ensure essential quest items cannot be irretrievably sold (also PB-CHECK-01).

## 4. Other notes / not tracked as formal issues

- **Positive:** Quests appeared to trigger and complete correctly through this part.
- **Dialogue polish:** Scrapper's confidentiality line felt cheesy; user suggested a more natural line along the lines of "Let's just keep this between you and me." Original line is transcription-uncertain. Put into later prose/dialogue pass.
- **Music transition:** User suspected combat music resumes rather than restarts; behavior and intended design need verification in audio pass.
- **Retracted observation:** Thermal Cooker / Ration Soup concern was explicitly withdrawn; no action.

## 5. Immediate World 1 cleanup checklist

- [x] CB-01 Combat tutorial overlay placement (repositioned guidance pill to TopCenter to clear character animations, lunges, and command buttons)
- [x] CB-02 Stability Broken trigger timing (increased Faulted Loader HP to 150 so Arc Tether shatters stability and displays break tutorial while enemy remains alive and staggered)
- [x] CB-03 Tinkering-menu dialogue overlap (auto-close tinkering menu upon functional cryo inductor craft, pause 1.2s, and resume Jed dialogue in exploration UI)
- [x] CB-04 Skills tutorial instruction / dialogue overlap (defer AP build tutorial until active dialogue completes; direct player to Abilities in party menu)
- [x] CB-05 Verify and, if necessary, standardize Skills/Abilities wording (standardized on canonical "Abilities" ecosystem across combat, party menu buttons, AP tutorial copy, and scripts)
- [x] PB-CHECK-01 Verify critical item selling/reacquisition (added system-level blocker in ShopController preventing quest/key item sales; ensured all 18 key items have unsellable=true)
- [x] PB-CHECK-02 Verify leaving puzzle mid-progress does not softlock (verified persistent quest task milestones; added regression test in Hub1CriticalFlowTest covering multi-room mid-puzzle departures)

## 6. Implementation guidance for AI coding agents

- This is a **playtesting triage report**, not authorization to implement all suggestions automatically.
- Fix confirmed critical issues first; reproduce or inspect conditional issues before making changes.
- Treat "consider," "evaluate," and "review" items as design proposals requiring a decision, not predetermined fixes.
- Preserve existing working quest progression; add focused regression tests around event sequencing and quest state.
- Identify whether each finding is local to World 1 or should be resolved via a shared system before implementing.
- Reconcile names from speech transcription with actual code/content identifiers.
- Keep stable issue IDs (CB-xx, IC-xx, PB-CHECK-xx) when incorporating Parts 2 and 3.

## 7. Approved Decisions on Important Considerations (IC-01 to IC-23)

### Category A: Opening Sequence and Onboarding
- **IC-01 (Intro Cinematic Pacing):** Deferred to post-playtest audio/visual polish pass.
- **IC-02 (Novus Bunk Interactions):** Streamline to 3 actionable objects: *Bunk Light*, *Sleeping Shelf*, and *Door Control Panel*. Fold *Cargo Netting* and *Rusty Locker* lore into the room description.
- **IC-03 (Tutorial Labels):** Standardize category prefix tags on tutorial cards: `[System]`, `[Combat]`, `[Navigation]`, `[Field]`.
- **IC-04 (Rest Sound Effect):** Deferred to post-playtest comprehensive audio pass.
- **IC-05 (Embedded Tutorial Cards):** Retain embedded cards inside interfaces; add a subtle cyan pulse / "New" indicator on their first appearance.

### Category B: Notifications and Tutorial Sequencing
- **IC-06 (Item Acquisition Cards):** Preserve individual popup cards for each item type to give discoveries weight; remove redundant `"1 of 5"` and `"Tap to Continue"` / `"Tap for Next"` label clutter.
- **IC-07 (Global UI Event Queue):** Enforce strict priority queue: `Active Modals (Tinkering/Shop) > Tutorials > Dialogue > Item/Reward Toasts`. Lower-priority events pause and wait until higher-priority views are dismissed.
- **IC-08 (Inventory Tutorial Timing):** Defer Supplies/Key Items tutorial until the first relevant tactical moment (e.g. taking damage / obtaining first MedKit).
- **IC-09 (New Quest Presentation):** Upgrade new quest acquisition into a distinct amber banner with subtle background dimming and clear title/objective prominence.
- **IC-10 (Currency Acquisition Feedback):** Standardize credit rewards (such as the 75-credit dice table win) with visible toast notifications matching item gains (`+75 Credits`).

### Category C: Exploration, Puzzles, and Progression
- **IC-11 & IC-12 (Cutter Puzzle Redesign & Puzzle Variety):** Redesign cutter calibration into a lightweight tactile diagnostic (ground/frequency choice with cause-and-effect feedback); use physical diverter routing mechanics in Ventilation Hub rather than flat sequential text noun clicks.
- **IC-13 (Puzzle-Room Movement):** Keep room exits open; do not artificially trap the player during the puzzle (regression verified in PB-CHECK-02).
- **IC-14 (Jed's Shutter Naming):** **Retain room title as "Jed's Shutter"** as intended in narrative design.
- **IC-15 (VHS Tape Pacing):** Keep Film 01 in the colony pit; move Film 02 out of Jed's office deeper into World 1 (e.g. Deep Mine or Transit Checkpoint) to pace the 10-tape set across the campaign.
- **IC-16 (AP Starting Economy):** Award 1 AP for *Wake Up Call* to create an immediate, meaningful choice between Nova's *Saboteur* (*Grounded Wire*) and *Improviser* (*Quiet Steps*) branches; award the next AP after the first major boss/checkpoint.

### Category D: Combat Interface and Mechanics
- **IC-17 (Inspect UI Cleanup):** Rename `Integrity` → `HP` and `Stability Gauge` → `Stability`. Simplify header to `Weaknesses & Resistances` and use question mark affinity slots (`[ ? ]`) for unrevealed weaknesses. Ensure consistent title-cased labels.
- **IC-18 (Snack Details Toggle):** Remove the inline `"Snack Details / Hide Snack Details"` toggle button from the combat palette; display cooldown and effect on the button itself or on press.
- **IC-19 (Active Effects Badges):** Disable direct-tap accidental popups from floating status badges over combatants; consolidate full status descriptions and turn durations into the Inspect view.
- **IC-20 (Feedback Text Overflow):** Streamline the second line of text (subtitle/effects explanation) to fit comfortably on a single line and give the notification pill dynamic vertical padding so multi-line text never clips.

### Category E: Maps, Menus, Shops, and Equipment
- **IC-21 (Map & Hub UX Pass):** Prioritize minimap icons for active quest objectives and doors over room dot clutter; clearly label the menu button with `"Menu"` text; ensure the hub screen provides clear Quick Save parity; add a node preview card with a high-contrast `"Enter"` button.
- **IC-22 (Weapon Mod Tutorial Trigger):** Trigger the `"Weapon Mod Slots Are Now Unlocked"` tutorial immediately when the player acquires their first weapon mod (dock quest or checkpoint) rather than gating strictly on checkpoint entry.
- **IC-23 (Shop Affordance):** Add prominent `[ 🛒 Shop ]` pill badges to merchants and trade interactions to distinguish them from standard conversation text. (Quest item sell prevention already enforced in PB-CHECK-01).
