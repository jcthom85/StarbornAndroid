# Starborn (v1.3.91) — Full Evaluation, Competitive Benchmark & Prioritized Roadmap

**Date:** October 16, 2026
**Baseline:** Commit `13745e73` (`versionName = "1.3.91"`, `versionCode = 110`)
**Related Docs:** [Current State](CURRENT_STATE.md) | [Improvement Backlog](IMPROVEMENT_BACKLOG.md) | [Progression & Ability Plan](design/PROGRESSION_AND_ABILITY_PLAN.md) | [Source Language & Puzzle Refinement Plan](design/SOURCE_LANGUAGE_AND_PUZZLE_REFINEMENT_PLAN.md)

---

## 1. Executive Summary & Overall Verdict

**Overall Score: 8.6 / 10 (Pre-Release Commercial Indie Tier)**

**Starborn** is a feature-complete, multiplatform (Android + Windows Desktop JVM) narrative sci-fi RPG built on a clean Kotlin/Compose architecture (`:gameRuntime`, `:shared`, `:app`, `:desktopApp`). It ships with:
- **7 Worlds / 13 Hubs / 60 Hub Nodes / 477 Rooms** (470 campaign rooms + 7 debug rooms)
- **62 Quests** (30 Main Quests `w1_mq01`–`w6_mq30` + 32 Side Quests across all 6 worlds)
- **~57,600 words of interactive prose** across 6,010 string fields (`dialogue.json`, `cinematics.json`, `rooms.json`, `quests.json`, `events.json`, `items.json`, `skills.json`, `npcs.json`)
- **202 audio files** (201 unique by SHA-256 across music, ambient loops, SFX, and voice murmurs) and **983+ visual assets** with **0 broken asset references**
- **ATB + Ready Queue Combat** featuring Momentum overcharge (1–3 stacks), a 6-element affinity matrix with cooldown-refund Weakness Rewards, Stability/Break turn manipulation, and utility-scoring enemy AI with Round-1 ambush grace
- **Rich Side Systems:** 6 restorable 60 FPS arcade cabinets (`DeepMine`, `CanopyHopper`, `SpireInfiltrator`, `SlagCatcher`, `OrbitalDefense`, `HarmonicPulse`), 5-phase accelerometer/touch fishing across 6 world zones, batch cooking with companion chef bonuses, and equipment mod-slot tinkering
- **Automated Verification:** 840+ unit/UI tests across Android and Desktop, including `LegalRouteCampaignRunnerTest.kt` (plays `w1_mq01` to `w6_mq30` via legal actions and real ATB combat).

### Scorecard by Pillar

| Pillar | Score | Grade | Key Takeaway |
| :--- | :---: | :---: | :--- |
| **Core Architecture & Tooling** | **9.2 / 10** | **A** | Clean KMP/JVM split, Protobuf DataStore persistence, 10 Gradle validation tasks, and full-campaign automated test runner. Held back by monolithic UI/controller files (`ExplorationScreen.kt` at 8,590 lines, `ExplorationController.kt` at 6,423 lines). |
| **Narrative, Tone & Worldbuilding** | **8.9 / 10** | **A-** | Distinct "Blue-Collar Cosmic" voice (*mechanism before metaphor*). World 1 and all 27 story cinematics are A-tier after the Round 1 prose pass; Worlds 3–6 event popups and scale rooms still need their final polish pass. |
| **Exploration & Presentation** | **9.0 / 10** | **A-** | Seamless hybrid of illustrated viewports, inline clickable prose (`RoomPresentation.kt`), 250ms patrolling enemies (`EnemyMovementManager.kt`), dark-room entry-direction gating, and 60 FPS zero-allocation environmental particle shaders. |
| **Combat System Design** | **8.7 / 10** | **B+** | Momentum overcharge + Elemental Weakness cooldown refunds + Stability Break create a snappy, non-repetitive tactical loop. |
| **Side Systems & Minigames** | **9.1 / 10** | **A** | Cohesive, deterministic, and well-rewarded: 6 arcade games, 6-zone fishing, companion cooking, and mod tinkering all tie back into party readiness. |
| **RPG Progression & Economy** | **7.2 / 10** | **C+** | **Primary release blocker:** Production New Game still defaults to `progressionPrototypeVersion = 0` (`GameSessionState.kt:54`), exposing legacy 100-node skill trees where 48 nodes have no runtime consumer. Cutover to the finished 32-node `CrewBuildPrototype` raises this to **8.8/10**. |
| **Audio & Visual Assets** | **8.3 / 10** | **B+** | 100% file resolution, 465 unique room backgrounds, 52 emote portraits, 201 unique audio tracks. Held back by enemy/boss sprite reuse in Worlds 3–6 (20 sprites shared across 48 enemies) and portrait-identical player combat sprites. |

---

## 2. Commercial & Critical Benchmark Comparison

| Commercial Benchmark | Shared DNA | Where the Benchmark Leads | Where Starborn Holds Its Own or Leads |
| :--- | :--- | :--- | :--- |
| **Citizen Sleeper** *(Jump Over the Age)* | Blue-collar sci-fi survival, quota/corporate debt, node/text interface, empathy for working-class and synthetic characters. | **100% prose consistency & minimalist tabletop clock system.** | **Tactical & spatial gameplay:** Starborn adds room-by-room exploration, real-time enemy patrols, 4-party ATB combat, and interactive puzzles/minigames. |
| **Sea of Stars / Chained Echoes** *(Sabotage / Matthias Linda)* | Turn-based/ATB combat with resource build/spend (Momentum), lock/break bars (Stability), camp cooking, and minigames. | **Custom multi-frame combat animation & unique boss sprite silhouettes.** | **Writing maturity & character voice:** Starborn's grounded "Blue-Collar Cosmic" prose (especially Nova, Zeke, and Gh0st) is sharper and less trope-reliant. |
| **Roadwarden / 80 Days** *(Moral Anxiety / inkle)* | Illustrated viewports paired with interactive inline prose and regional node navigation. | **Wide narrative branching & open-ended route order.** | **Audio-visual immersion & RPG combat:** 60 FPS procedural weather/particle shaders, 201 audio tracks/cues, voice murmurs, and full party combat. |
| **Star Traders: Frontiers / Caves of Qud** *(Trese Brothers / Freehold)* | Systems-rich indie sci-fi on Android & PC with deep lore and tactical party/character builds. | **Procedural sandbox replayability.** | **Mobile UX & accessibility:** Native Jetpack Compose UI, high-contrast/large-touch-target modes, 2.0x font scaling, and handcrafted narrative pacing. |
| **Honkai: Star Rail / Octopath Traveler** *(HoYoverse / Square Enix)* | Toughness/Stability Break bars, elemental weakness exploitation, turn-order manipulation. | **AAA/AA 3D/HD-2D presentation budget.** | **Zero gacha/monetization friction + Weakness Cooldown Refund loop** (-1 turn on all cooldowns when striking an elemental weakness). |

---

## 3. Prioritized Roadmap: What to Work on Next

### Priority 1: Immediate Code, Progression & Bug Fixes (Phase 1)
1. **Cut Over Production Skill Trees to `CrewBuildPrototype` (`progressionPrototypeVersion = 1`):**
   - Flip `progressionPrototypeVersion: Int = 1` by default in `gameRuntime/src/main/kotlin/com/example/starborn/domain/session/GameSessionState.kt:54`.
   - Implement idempotent legacy save migration in `GameSessionPersistence.kt` (strip retired legacy skill IDs from `unlockedSkills`, refund spent AP once, and upgrade `progressionPrototypeVersion` to `1`).
   - Add a `DataIntegrityTest.kt` assertion verifying that every active skill tree node maps to a valid combat skill or runtime consumer.
   - Delete retired `app/src/main/assets/skill_trees/ollie.json` and empty `sfx.json` / `sfx_meta.json` stubs.
2. **Fix `campaign_w4_crucible` Debug Scenario & Test Coverage:**
   - In `gameRuntime/src/main/kotlin/com/example/starborn/debug/DebugCampaignScenarios.kt:758-856`, add `"w4_sq_crucible" to "foundry_crucible_arena"` in `startRooms` and stop ignoring the `roomId` parameter in `entry(...)` (`line 829`).
   - Update `DebugScenarioCatalogTest.kt` to assert room resolution via `DebugCampaignFixtureBuilder`.
3. **Fix Post-Defeat HP Stabilization & Deduplicate Combat Helpers:**
   - Update `ExplorationController.kt:943-950` to calculate post-defeat stabilization HP via `PartyHealth.maxHp(...)` instead of raw level-1 base HP (`charactersById[memberId]?.hp ?: 100`).
   - Consolidate duplicated `checkSkillConditions` between `CombatController.kt:1164-1208` and `CombatActionProcessor.kt:1458-1518`, and unify equipment stat calculation with `PartyHealth.kt`.
4. **Configure Android Auto-Backup Rules:**
   - Replace the default `TODO` template in `app/src/main/res/xml/data_extraction_rules.xml:8-19` so `datastore/game_session.preferences_pb` and `save_slot_1..3.json` are backed up or excluded consistently.

### Priority 2: World 1 Excellence & Early/Mid Combat Tempo (Phase 2)
1. **World 1 Deep Audit & Polish (First 30 Minutes Hook, Pacing, Level/Encounter Flow):**
   - Ensure the opening sequence (`intro_prologue` -> `w1_mq01` -> `scene_cutter_surge` -> first combat -> Sector 4 -> `the_iron_warden` -> pod escape) hooks the player immediately, eliminates any UI/tutorial or backtracking friction, and maximizes tactical fun.
2. **Specialization AP Tempo & Encounter Differentiation (`CREW_TEMPO_TUNING_2026-10-15.md`):**
   - Ensure paid specialization nodes in `CrewBuildPrototype` create a clear, felt tempo advantage over unspent core-only kits in mid/late encounters.

### Priority 3: Worlds 3–6 Narrative, Puzzle & Asset Polish (Phase 3)
1. **Worlds 3–6 Prose & Event Popup Pass (`Prose_Audit_2026-07-15.md`, `SOURCE_LANGUAGE_AND_PUZZLE_REFINEMENT_PLAN.md`):**
   - Rewrite trailer-speak `show_message` popups in `events.json` (`H-01`).
   - Deduplicate repeated *"Ring Service Alcove"* scale room descriptions in World 5 (`H-02`) and trim stacked religious/harmonic metaphors in World 4 & World 6 traversal rooms (`H-03`).
2. **Late-Game Puzzle Variety (`tuning_puzzles.json`):**
   - Reserve numeric 3-band waveform tuning (`CounterTunePanel`) for genuine Echo/Source handshakes, and convert Worlds 3–5 industrial/security puzzles to optical (`PrismOpticsPanel`) or crew-role mechanical interactions.
3. **Boss & NPC Visual Variety (`enemies.json`, `npcs.json`):**
   - Assign distinct combat sprites to late-game bosses (`administrator_boss`, `titan_walker_boss`, `compliance_avatar`, `ascended_vale`, `ascended_god`, `crucible_champion`) and replace the 4 `orion_portrait.png` NPC placeholders.

### Priority 4: Architectural Cleanup & Release Hardening (Phase 4)
1. **Inventory State Consolidation:** Remove the duplicate mutable `items` map in `InventoryService` (`Inventory.kt:13-15`) in favor of `GameSessionStore.state.value.inventory`, and gate `createPlaceholderItem` behind explicit test mode.
2. **Monolith Decomposition & Legacy Debug Retirement:** Retire the ~1,580 lines of legacy synthetic debug scenarios in `AppServices.kt:783-2361` and split `ExplorationScreen.kt` (8,590 lines) and `ExplorationController.kt` (6,423 lines) into modular delegates/composables.
3. **Physical Device & Packaged Desktop Sign-Off (`MANUAL_ACCEPTANCE.md`).**
