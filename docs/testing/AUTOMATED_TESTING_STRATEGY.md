# Starborn Automated Testing Strategy & Gap Roadmap

> **Reference Document:** Created on 2026-09-16 to track prioritized automated testing gaps and the implementation roadmap across balance, progression, narrative pacing, audio, and content verification.

---

## 1. Top Immediate Priorities (The Critical List)

These are the direct risks and blind spots in the current test suite where automated testing is urgently needed:

### 1. Multi-Encounter Attrition Testing (Balance Blind Spot #1) [COMPLETED ✅]
* **Status:** Complete. Fully automated test suite (`MultiEncounterAttritionGauntletTest.kt`) tests chained 3-encounter dungeon legs without HP/inventory reset:
  * *World 1 Mining Depths:* `echo_borer` $\to$ `siren_skimmer` $\to$ `dominion_dampener` (seeds 1..5).
  * *World 2 Undercity:* `shard_hound` $\to$ `spore_spitter` $\to$ `ruin_guardian` (seeds 1..5).
  * *World 4 Foundry Gauntlet:* `faulted_loader` $\to$ `slag_golem` $\to$ authored camp rest (`w4_camp_forge_alcove`) $\to$ `titan_walker_boss` (seeds 2..5).
  * Enforces carry-over of depleted HP, persistent medkit expenditure, and economic sustainability without grinding.


### 2. Interactive Quest Branch Coverage [COMPLETED ✅]
* **Status:** Complete. Fully automated test suite (`InteractiveQuestBranchTest.kt`) tests both branches for all four interactive side quests (`w3_sq14`, `w4_sq19`, `w5_sq24`, `w6_sq27`), verifying mutual exclusivity, milestone persistence, reward payouts (items/skills), and room state validity.
* **Quests Covered:**
  * `Corporate Espionage`: Publish ledger (`ms_w3_blackmail_unlocked`, `encrypted_ledger`) vs. weaponize (`ms_w3_ledger_weaponized`, `phase_rounds`).
  * `Quality Control`: Release units (`ms_w4_quality_control_complete`, `gh0st_harden`) vs. line defenders (`ms_w4_units_defend_workers`, `rapid_capacitor`).
  * `Ghost in the Shell`: Data Shield (`ms_w5_data_shield_unlocked`, `data_shield`) vs. Guardian Covenant (`ms_w5_guardian_covenant`, `zeke_guardian_covenant`).
  * `The HR Record`: Leave review room (`ms_w6_unshackled_unlocked`, `zeke_unshackled`) vs. broadcast revocation (`ms_w6_workers_unshackled`, `zeke_unshackled`).

### 3. Side Quest Ordering & Independence Permutations [COMPLETED ✅]
* **Status:** Complete. Fully automated test suite (`SideQuestPermutationIndependenceTest.kt`):
  * **Decoupling Audit:** Programmatically audits all 30 side quests (5 per world across Worlds 1–6) verifying 0 cross-quest milestone, prerequisite, or task leakage.
  * **Permutation Coverage:** Executes side quests in forward order, reverse order, and multiple deterministic shuffled permutations across World 3 (The Spire), World 4 (Foundry), World 5 (Archive), World 6 (Aethel Core), and World 1 (Homestead & Mines).
  * Enforces that side quests can be started and completed in arbitrary order without sequence locks, milestone corruption, or state pollution.


### 4. Worlds 3–6 Playthrough Narrative Audits [COMPLETED ✅]
* **Status:** Complete. Fully automated test suites (`World1PlaythroughAuditTest.kt` through `World6PlaythroughAuditTest.kt`) cover all 30 main quests (MQ01–MQ30) across Worlds 1–6 with 0 blockers.
* **Reports Generated:** `docs/playtest/reports/WORLD_1_PLAYTHROUGH_AUDIT.md` through `WORLD_6_PLAYTHROUGH_AUDIT.md` and `docs/playtest/reports/FULL_CAMPAIGN_AUDIT_SUMMARY.md`.

### 5. Cinematic Skip & Interruption Invariant Testing [COMPLETED ✅]
* **Status:** Complete. `CinematicSkipInvariantTest.kt` audits all scenes in `cinematics.json`:
  * Verifies structural integrity of all authored scenes (non-empty steps, non-blank text/timing).
  * Validates referential integrity: 100% of cinematic references in `events.json` resolve to authored scenes.
  * Proves immediate skip executes completion callbacks without dropping milestones, room states, or item grants.
  * Proves exact session state parity: immediate skip produces identical state to full playback.
  * **Fix Applied:** Identified and removed duplicate `ollie_intro_scene` in `cinematics.json`.

### 6. Automated Emotional, Conflict, and Humor Pacing Validation [COMPLETED ✅]
* **Status:** Complete. Fully automated test suite (`NarrativePacingAndTonalLinterTest.kt`):
  * **Breather Beats Precedence:** Programmatically verifies that all 4 mandatory breather locations (`sector9_canopy_ridge`, `spire_skypark_dome`, `foundry_cooling_springs`, `orbital_solarium`) exist, connect cleanly, and are sequenced before their respective act climaxes (`w2_mq05`, `w3_mq15`, `w4_mq20`, `w5_mq25`).
  * **Sacred Moment Protection:** Programmatically audits dialogue nodes across Jed's sacrifice (`jed_w1_mq04_sacrifice_`), Gh0st's origin confrontation (`rylos_w4_mq20`), Thorne's execution (`w5_mq25`), and Nova's final sacrifice (`w6_mq30`), asserting 0 comedic keywords and 0 inappropriate emotes (`happy`, `cool`, `confident`).
  * **Tonal Whiplash Lint:** Traverses the entire dialogue graph to prevent sudden transitions from intense grief/crying into comedy/levity without buffer beats.
  * **Conflict Density:** Asserts every world maintains $\ge 8$ enemy encounter rooms to prevent exploration droughts.
  * **Quest Depth:** Verifies all 30 main quests contain $\ge 2$ structured narrative tasks, preventing empty placeholder quests.


### 7. Device Audio Transition & Ducking Verification
* **The Problem:** Unit tests verify `AudioRouter` cue selection, but do not verify audio hardware lifecycle on real Android runtimes.
* **What to Build:**
  * Instrumented / Maestro test verifying:
    * Clean crossfades between exploration layers (`music_primary`) and combat layers (`music_combat`) without clicks, pops, or dead silence.
    * `VoiceoverController` ducking: verify music volume drops during active voiceover lines and smoothly restores upon completion.
    * App background/foreground / audio focus loss (e.g., incoming call interruption) restores mute and volume states without leaks.

---

## 2. Additional Overlooked Vectors to Test ("Anything Else?")

Beyond the top 7, here are crucial system interactions that frequently break JRPG/narrative Android games:

### A. The "Back Button & Gestural Trapping" Suite
* **Risk:** In Jetpack Compose navigation, pressing system Back during active combat, mid-dialogue prompt, during a shop transaction, or inside the tinker mini-game can pop the backstack to an invalid screen, orphan game loop coroutines, or duplicate items.
* **Test Needed:** Maestro or Compose UI test firing Back at every interactive modal and state machine step.

### B. Process Death & Low-Memory Restoration (Activity Recreation)
* **Risk:** Android frequently kills background activities during device rotation, multitasking, or low RAM. Starborn relies on `GameSessionStore` and DataStore.
* **Test Needed:** Instrumentation test that triggers simulated Android process death (`am kill`) mid-dialogue, mid-combat turn, and mid-tinkering, restarts the app, and asserts exact session state restoration.

### C. Full Campaign Economy Deficit / Starvation Simulation [COMPLETED ✅]
* **Status:** Complete. Fully automated test suite (`CampaignEconomyStarvationTest.kt`):
  * **Zero-Deficit Solvency Invariant:** Simulates a clean, no-grind playthrough across Worlds 1–6 (strictly mandatory combat encounters and quest rewards minus baseline medkits/restoratives). Proves player credits remain strictly $\ge 0$ throughout every world and conclude endgame with a positive surplus.
  * **Shop Arbitrage Guard:** Verifies every shop definition in `shops.json` enforces `sell_markup > buy_markdown` (e.g. 1.2 vs 0.35), mathematically preventing infinite credit loops from buy/sell cycles.
  * **Stock Catalog Integrity:** Validates that every item stocked across all vendor inventories resolves to a valid, positive-priced item in `items.json`.

### D. Inventory & Consumable Boundary Edge Cases [COMPLETED ✅]
* **Status:** Complete. Fully automated test suite (`InventoryAndConsumableBoundaryTest.kt`):
  * **Meal Buff Stacking Guard:** Proves applying multiple chef meals in succession replaces the active meal buff cleanly and refreshes the 3-encounter timer without compounding stats infinitely.
  * **Encounter Lifecycle & Expiry:** Asserts `decrementMealBuffEncounter()` accurately decrements encounters from 3 $\to$ 2 $\to$ 1 $\to$ 0 and clears the active buff at 0.
  * **Atomic Transactions:** Enforces all-or-nothing consumption in `consumeItems()`—if any item requirement is deficient, zero items are deducted and state remains pristine.
  * **Quantity Boundary Guards:** Verifies zero/negative entries are ignored during restore or add, and excess removal clamps to zero while purging entry.
  * **High-Capacity Inventory:** Asserts inventory holds $100+$ distinct item stacks and never drops incoming quest items.


### E. New Game Plus (NG+) Contract Enforcement [COMPLETED ✅]
* **Status:** Complete. Fully automated test suite (`NewGamePlusContractEnforcementTest.kt`):
  * **Defect Identified & Fixed:** Removed premature `ms_w2_mq05_complete` from `AppServices.startNewGamePlus()` seedState (lines 894–898) which was previously blocking all four World 2 side quests (`w2_sq01`, `w2_sq03`, `w2_sq04`, `w2_sq05`) from starting in NG+.
  * **Pristine Reset:** Proves NG+ cleanly carries over player level, credits, unlocked weapons/armors, and inventory, while setting `ms_master_protocol_active` and zeroing narrative milestones.
  * **World 1 Loop Parity:** Walks through `w1_mq01` through `w1_mq05` verifying zero sequence breaks, auto-completions, or quest stalls.


---

## 3. Recommended Implementation Phases

```
Phase 1: Game-Breaking Fixes & Core Balance
  ├── Multi-encounter combat attrition simulation
  ├── Quest branch coverage (4 interactive quests × 2 paths)
  └── Cinematic skip callback invariants

Phase 2: Narrative & Pacing Guardrails
  ├── Worlds 3–6 narrative playthrough audit tests
  ├── Side quest ordering permutation tests
  └── Pacing rule analyzer (sacred moments, breather beats, humor linter)

Phase 3: Android Platform & Immersion Verification
  ├── Audio transition & ducking device tests
  ├── Compose Back-button gesture stress testing
  └── Activity death / process recreation resilience
```
