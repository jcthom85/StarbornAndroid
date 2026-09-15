# Debug Scenario Rebuild and Full Game Test Master Plan

Status: implementation started; inventory generated and 38 rebuilt scenarios implemented. Full campaign coverage review, gameplay acceptance, and legacy retirement remain open.
Date: 2026-09-15

## 1. Objective

Replace the accumulated debug scenarios with a maintained suite for testing the entire current game: campaign, optional content, combat, progression, economy, tutorials, presentation, persistence, and Android lifecycle behavior.

Every retained scenario must have an explicit test purpose, a trustworthy starting state, player instructions, and observable pass criteria. Remove superseded scenarios and their unused implementation after replacement coverage is verified. Existing scenarios receive no automatic exemption, but sound helpers and useful regression cases can be reused.

Completion means both a usable replacement suite and recorded gameplay evidence. Launching every preset successfully does not establish that the game is completable.

## 2. Current evidence and constraints

- `DebugScenario.kt` defines the menu separately from `AppServices.startDebugScenario` and its many bootstrap functions. Hidden launcher IDs and duplicate destinations exist.
- The current catalog spans six campaign worlds, twelve regional hubs, and the Astra. It already includes useful recipe boundaries, route gates, and meal persistence cases.
- `docs/debug-scenario-qa.md` records 91 successful launcher checks on an emulator on September 14. That is historical setup validation, not a fresh execution result or complete gameplay acceptance.
- Existing tests cover asset integrity, legal routes, quest/dialogue events, combat, provisioning, discovery, persistence, and all six arcade engines. Preserve these protections and update scenario dependencies deliberately.
- Some existing gate presets use sandbox combat progression. They cannot establish realistic balance or affordability.
- `docs/MANUAL_ACCEPTANCE.md` identifies interruption recovery, continuous-route affordability, audio, and physical-device performance as areas needing evidence.
- Save state includes quests, milestones, completed events, pending cinematics/battles, party progression, inventory, node discovery, patrols, Astra return location, arcade progress, and meal effects. A valid room ID alone is insufficient fixture validation.

This plan is based on the catalog, launcher, state model, asset inventory, test inventory, and current QA documents. Phase 1 performs the exhaustive content audit; counts and exact checkpoint IDs remain provisional until then.

## 3. Coverage model

Maintain one coverage ledger, with a row for each required content flow or system behavior. Each row records:

- Stable coverage ID and relevant asset/code IDs.
- Feature/world and risk priority.
- Scenario and variant that exercise it, or continuous-run checkpoint.
- Automated checks and required manual actions.
- Expected result and evidence needed.
- Implementation status: planned, built, validated, retired.
- Execution status: not run, passed, failed, blocked, or explicitly not applicable with reason.
- Build/commit, fixture revision, device, run date, evidence location, and linked defect.

Do not merge implementation and execution status. A built scenario may still be untested. Changes to relevant content or setup invalidate affected results until rerun.

Coverage rules:

1. Every main quest and side quest has a playable start-to-completion route assigned.
2. Every regional hub, authored gameplay node, and playable room is visited through an assigned route; inaccessible or unused content is explicitly classified.
3. Every boss, unique encounter mechanic, puzzle, tutorial, recruitment, and world handoff has explicit acceptance coverage.
4. Every player-facing system has success, rejection/failure, repeat-use, and persistence cases where applicable.
5. Every skill, status, item behavior, recipe, shop, fishing location, and arcade cabinet is mapped. Shared behavior can use parameterized checks; unique UI/behavior needs direct gameplay coverage.
6. Every irreversible progression/reward boundary has an exactly-once check and interruption coverage appropriate to its implementation.
7. Generic rule combinations use representative cases and automated parameterization; do not attempt every possible party/item/status combination. Document exclusions and why the selected cases cover the rule.

## 4. Suite organization

Use four types of test entry, with searchable world/system tags and named variants. Avoid a separate menu entry for every small parameter change.

| Type | Purpose | State policy |
| --- | --- | --- |
| Campaign checkpoint | Play a meaningful segment through its next handoff | Believable progression, equipment, supplies, and prerequisites |
| System workbench | Inspect and exercise a mechanic efficiently | Extra supplies/unlocks allowed and disclosed |
| Regression/boundary | Reproduce a specific failure or edge condition | Minimal explicit differences from a known baseline |
| Continuous campaign | Prove connected progression and resource viability | New game; no warps, inventory grants, or state edits during the run |

Group runnable selections into a small smoke playlist, world playlists, system playlists, recovery playlist, and full acceptance playlist. A single scenario can belong to several playlists.

## 5. Proposed campaign coverage

Exact checkpoints follow current authored quest stages, not inherited shortcut names. Each segment starts before the action being tested and ends after its reward, journal update, and onward route have been verified.

| Area | Required segments and outcomes |
| --- | --- |
| World 1: The Mines | Opening/tutorial chain; starter equipment; Jed's crafting and generator progression; errands and checkpoint access; Deep Mine encounters and Source introduction; lockdown escape; Warden; launch and arrival in World 2 |
| World 2: Sector 9 | Crash-site discovery; canopy traversal and cutter gates; temple/puzzles; stasis and recruitment; Hunter; Source Gate; Astra repair, crew state, and departure |
| World 3: The Spire | Sewers and lower-city discovery; safehouse plan/intel; upper-city access; infiltration and puzzles; Lens acquisition; alarm/escape and World 4 handoff |
| World 4: The Foundry | Arrival; records/Phantom progression; assembly traversal; cooling/forge gates and Anvil; Titan/Rylos encounter; core theft and meltdown departure |
| World 5: The Void | Docking; zero-gravity routes; firewall/core approach; Anchor/Elara sequence; Vale/Critical Mass and Source transition |
| World 6: The Source | Nightmare/crew restoration; Echo Mines and Key; Memory Bridge; Spire ascent; finale; completion state, return to menu, and New Game Plus eligibility |
| Astra | Initial access; room availability by story stage; crew conversations; rest/cooking/tinkering; cabinet installation/use; disembark to the correct prior location; save/load aboard ship |
| Optional content | Every currently authored side quest, optional interaction with gameplay consequences, collectible/reward flow, and optional route; verify journal, payout, and repeat interaction |

Cover both entry and return paths at gates. Include premature interaction and repeat interaction wherever prerequisites or one-time rewards matter. Deduplicate boss and story entries when they test the same segment; expose alternate loadouts as variants only when useful.

## 6. System acceptance matrix

| System | Required tests |
| --- | --- |
| Combat fundamentals | Target selection, legal/illegal actions, timing/readiness, party switching, enemy telegraphs, single/multiple enemies, victory, defeat, and retreat where supported |
| Combat depth | Weapon attack styles, momentum/overcharge, guard/stability break, cooldowns, focus/resources, Source Arts, buffs/debuffs, duration/expiry, resistances, healing and downed-member behavior |
| Encounter integration | Patrol contact, authored encounter triggers, retreat/re-entry, party health carryover, loot/XP payout once, quest progression, and route access after victory |
| Party and leveling | Each recruitment; level/XP/AP changes; skill unlock/spending; gear compatibility; derived-stat updates; member changes and persistence |
| Inventory/loadout | Acquire/use/equip/unequip; quantities and empty states; snack assignment/use; key items; long descriptions; restrictions and invalid actions |
| Shops/economy | Every shop inventory/access point; buy/sell; exact/insufficient credits; quantity bounds; unavailable transactions; reload consistency; mandatory purchases and ongoing supply costs |
| Tinkering | Every recipe mapped; tools/schematic gates; exact ingredients; missing ingredient/tool; repeated crafting; cancellation; correct result and resource use; quest continuation |
| Cooking/meals | Every recipe mapped; chef effects; consumption/replacement; recovery; buffs in actual combat; expiry through actual encounters; save/load and invalid actions |
| Fishing | Every location and catch table mapped; rods/lures and unlocks; success/failure/cancel; inventory changes; reward/tutoring events; interruptions and controls |
| Arcade | Deep Mine, Canopy Hopper, Spire Infiltrator, Slag Catcher, Orbital Defense, Harmonic Pulse; discovery/repair/install; gameplay and exit/retry; scores, reward tiers, repeat-claim prevention, persistence |
| Exploration/maps | All hubs/nodes/rooms; discovery/reveal/unlock/completion distinctions; locked previews; reciprocal and intentional one-way exits; room actions/items; patrols and special movement |
| Puzzles | Every authored tuning/environment puzzle; wrong/incomplete/correct input; prerequisite gates; cancel/reopen; reward once; route update and reload |
| Quests/narrative | Acceptance, stage/task tracking, tracking selection, completion and failure where authored; dialogue choices/conditions; repeated interaction; event ordering; cinematic completion/skip where supported |
| Tutorials | Every current script and trigger; first occurrence; completion; non-repetition; interruption/reload; overlay dismissal and input blocking; authentic prerequisite state |
| Saves/menu/NG+ | New game, Continue, manual slots, quicksave, autosave, load selection; empty/error cases; supported legacy migrations; completion unlock; actual NG+ carryover/reset contract from code |
| Presentation | Text wrapping and enlarged fonts; touch targets; scroll/navigation/back behavior; HUD/menu overlays; character/enemy layout; lighting/weather/FX; music, ambience, voice and SFX transitions/settings |
| Android lifecycle | Background/resume, recreation where supported, process termination/relaunch, audio focus loss, repeated session/scenario changes, startup and prolonged play on physical hardware |

Confirm exact supported behaviors during inventory. Do not invent mechanics or require unsupported actions; classify those cases explicitly.

## 7. Persistence and interruption suite

Exercise real actions at critical boundaries, then load/relaunch without reseeding the scenario:

- Before battle, during battle, after victory before leaving the reward screen, and after payout.
- Before a cinematic, during playback, and around its progression callback/handoff.
- Before/after quest completion, recruitment, world travel, key acquisition, and gate unlocking.
- Before/after buying, selling, crafting, eating, catching fish, claiming arcade rewards, and resting.
- During an unsolved puzzle and after puzzle completion.
- In regional exploration and aboard Astra, including stored return destination.
- At game completion and during New Game Plus creation.

Define expected recovery per case from the implemented persistence contract. Recovery may restart an encounter or scene; do not assume frame-perfect restoration. Verify no lost key item, duplicate payout, consumed-but-unfinished event, inaccessible route, or unusable session. Test ordinary disk reload separately from full process death.

Use isolated test storage for automated fixtures. Design an explicit debug-session storage policy before manual launches so test autosaves cannot silently replace personal campaign progress. Verify that policy before broad playtesting.

## 8. Scenario definition and fixture architecture

Each scenario definition contains:

- Stable ID, title, category, world/system tags, purpose, priority, and estimated play duration.
- Coverage IDs, fixture revision, starting destination, and prerequisite state summary.
- Named variants and disclosed synthetic grants/flags.
- Ordered player actions, expected observations, and final state assertions.
- Restart instructions, save/load instructions, and known limitations.

Implementation direction:

1. Create a dedicated debug scenario package with one registry connecting definitions to setup. Keep `AppServices` as the integration boundary rather than growing its scenario switch.
2. Build reusable, explicitly specified campaign baselines and small variant transformations. Avoid chains of unrelated old presets that silently inherit flags, tutorials, or overpowered equipment.
3. Prefer baselines captured from verified legal playthroughs where practical. Version synthetic baselines and validate their prerequisites; synthetic flags never prove the preceding gameplay happened.
4. Validate the complete relevant state: location membership, party/gear, resources, quest stage/tasks/milestones/events, discovery, pending actions, and service/store consistency.
5. Ensure launch clears transient combat, dialogue, cinematic, tutorial, navigation, and audio state as appropriate. Test A-to-B-to-A relaunches for contamination.
6. If randomness is controllable, expose recorded seeds for reproducible regressions; retain ordinary random play for experiential testing. Do not promise determinism for unsupported engines.
7. Fail with an actionable setup error, not a success result followed by an unusable destination.
8. Show purpose, steps, expected results, and variant state in the debug interface. Provide quick restart and a lightweight result/evidence workflow; avoid building a separate test-management application.
9. Keep debug launching unavailable in normal release UI and verify existing build gating.

## 9. Work phases and exit gates

### Phase 1 — Inventory and coverage design

- Enumerate active assets and runtime systems, all current menu and hidden launcher IDs, bootstrap callers, automated references, and QA documentation.
- Produce the coverage ledger and concrete replacement catalog, including every quest, boss, puzzle, tutorial, and arcade game.
- Mark each old scenario replace/merge/retire; identify helpers still used by production or tests.
- Audit NG+ carryover and persistence contracts and identify unsupported/inactive content.

Exit: every current feature/content requirement is mapped or explicitly classified; replacement IDs and scope are reviewable. Do not choose an arbitrary scenario count in advance.

### Phase 2 — Framework and first vertical slice

- Implement the registry, fixture validation, storage isolation, metadata display, restart behavior, and result format.
- Build the opening-to-first-combat slice plus exact/missing recipe variants and one real save/reload case.
- Verify rendered launch, player actions, state transitions, and clean repeated launches on Android.

Exit: a tester can launch, understand, complete, restart, and record a scenario without reading Kotlin.

### Phase 3 — Campaign checkpoints and optional content

- Work through Worlds 1–6 in order, plus Astra and all side quests.
- Verify each segment through its onward handoff, including real boss victories and rewards.
- Capture/version useful baselines from successful play and add boundary variants where progression risk warrants them.

Exit: every campaign/optional-content ledger row has a runnable route and segment evidence; no unassigned handoff.

### Phase 4 — Complete system workbenches and boundaries

- Implement the system matrix, all six arcade games, remaining tutorials, and negative/repeat cases.
- Parameterize repetitive rule/data checks while keeping unique player flows directly playable.
- Connect targeted regression tests to concrete behavior rather than tests that merely duplicate registry entries.

Exit: every current system has mapped happy-path, applicable boundary, and persistence coverage.

### Phase 5 — Recovery, presentation, and device acceptance

- Execute interruption tests, supported migration fixtures, audio checks, enlarged-text/UI checks, and physical-device play.
- Record baseline startup/frame-time/memory observations on selected hardware; set justified budgets before enforcing performance pass/fail.
- Run prolonged sessions and repeated launches to detect leaks, stale state, and audio overlap.

Exit: critical recovery cases pass; presentation/device findings have evidence and disposition.

### Phase 6 — Continuous campaign and balance validation

- Play from New Game through completion and into NG+ using normal actions and resources, with no debug reseeding.
- Record checkpoint saves, levels/loadouts, credits earned/spent, supplies, retries, playtime, and confusing objectives.
- Use a main-path run to establish mandatory progression; add optional-content/backtracking routes for interactions and affordability differences. Use checkpoint variants for supplementary low-resource or alternate-loadout tests.
- Compare real checkpoint states with synthetic fixtures and correct mismatches.

Exit: the connected campaign completes without debug intervention, required resources remain obtainable, completion/NG+ work, and all required optional content has separate evidence where not visited in that run.

### Phase 7 — Retire legacy scenarios and finalize

- Remove superseded catalog entries, hidden aliases, unused setup functions, and obsolete scenario-specific tests after checking callers.
- Retain behavior regression tests; update their setup to the replacement registry where appropriate.
- Update `docs/debug-scenario-qa.md` and `docs/MANUAL_ACCEPTANCE.md` to the new workflow, preserving dated historical results as historical.
- Recheck registry/launcher parity, compile/tests, rendered launch smoke coverage, and affected gameplay after cleanup.

Exit: only the approved replacement suite remains; no dangling legacy callers or lost coverage. Git history provides the old implementation if needed.

## 10. Execution and defect workflow

For each run record scenario/variant, fixture revision, build/commit, device/Android version, seed if applicable, actions taken, expected/actual result, pass/fail/blocked status, and screenshot/log/save evidence as useful.

Classify issues as game defect, fixture defect, instruction defect, or environment issue. A bad fixture is not evidence of a gameplay failure or pass. Link all affected coverage rows, repair, rerun the original case, then rerun adjacent flows affected by the fix.

Priority:

- P0: crash, save loss/corruption, unavoidable softlock, blocked mandatory progression, broken completion/NG+ handoff.
- P1: incorrect combat/system result, reward duplication/loss, major UI blockage, broken optional content or recovery.
- P2: lesser presentation, feedback, polish, or usability problems.

Do not silently waive failures. Any accepted limitation must name its impact and justification.

## 11. Verification layers and maintenance

- Static/data checks: registry uniqueness/parity, referenced assets, coverage links, prerequisite consistency.
- JVM tests: relevant domain/viewmodel behavior, quest/event ordering, combat, economy, crafting, persistence, and navigation.
- Android instrumentation: actual launcher state, rendered destinations, service synchronization, disk saves, and targeted UI flows.
- Manual emulator play: segment completion, boss fights, controls, feedback, and interruption reproduction.
- Physical device: touch/audio/readability/lifecycle/performance acceptance.
- Continuous campaign: connected progression, pacing, discoverability, and cumulative economy.

Use the repository's Gradle tasks for relevant checks, including `:app:testDebugUnitTest`, `:app:runAssetIntegrity`, `:app:lintDebug`, and `:app:connectedDebugAndroidTest` as applicable. Confirm device/test isolation before running instrumentation. Planning itself does not require executing the game test suite.

After content/system changes, update affected coverage IDs, fixtures, and instructions in the same change. Run the smoke playlist and affected suites. Full acceptance belongs to release readiness and substantial progression changes; publishing is a separate action.

## 12. Definition of done

- Every active content/system requirement has a ledger mapping and recorded outcome.
- Replacement scenarios launch reliably with validated, documented state and clear player instructions.
- All required campaign segments, optional content, system cases, and critical interruption cases have passed on the recorded build, or have explicitly accepted limitations.
- A continuous normal campaign completes through the ending and NG+ checks without debug repair.
- Mandatory progression and save/recovery blockers are resolved; remaining defects are explicitly tracked and dispositioned.
- Physical-device acceptance has evidence; emulator checks are not substituted for audible quality or hardware performance.
- All superseded debug scenarios and unused setup code are removed; retained tests cover behavior and the new registry.
- QA instructions and the coverage ledger are current and usable for the next game-testing pass.

Implementation tracking: see `docs/testing/README.md`, the generated inventory and replacement catalog, and dated verification records. Generated route assignments still require prerequisite/reachability review; they are not completed gameplay coverage.
