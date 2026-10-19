# Starborn Project-Wide Development Audit

**Audit date:** October 19, 2026
**Repository baseline:** `aa95649c21f5c942ce6af2bc1205e4555dff5be3`
**Method:** Read-only source inspection, catalog analysis, asset metadata checks, and non-mutating validators.

No source, assets, configuration, or documentation were modified during the audit. No dependencies were installed. No builds, unit tests, instrumentation tests, or gameplay sessions were run. The working-tree status remained unchanged during the audit. This Markdown document was subsequently created at the owner's request.

---

## A. Executive Summary

**Starborn has a substantial implemented campaign and gameplay foundation, but it is not yet ready to be treated as feature-complete or beta-ready.** The principal remaining risk is integration correctness: authored conditions, progression promises, presentation behavior, and tests do not always agree with the production runtime.

The repository contains:

| Measured content | Current count |
|---|---:|
| Campaign worlds | 6 |
| Additional ship world | Astra |
| Campaign rooms | 465 |
| Astra rooms | 5 |
| Unassigned debug rooms | 7 |
| Total room records | 477 |
| Hubs / hub nodes | 13 / 60 |
| Main / side quests | 30 / 32 |
| Event / dialogue records | 480 / 443 |
| Cinematic records, including demonstrations | 51 |
| Enemy definitions / skill definitions | 48 / 125 |
| Item records | 261, including one duplicate ID |
| Production image files | 985 |
| Shipped MP3 files | 202 |

These are content counts, not completion percentages or playtime estimates.

### What is substantially implemented

- All six worlds have room networks, quests, enemies, interactions, artwork, and completion-event producers.
- Static navigation checks passed across **954 room connections and 118 transition records**.
- Every node's rooms are reachable from its entry through the room graph **when progression gates are ignored**.
- ATB combat, enemy decision-making, elemental affinities, Break, cooldowns, equipment, inventory, crafting, meals, fishing, arcade games, and Astra travel have substantive implementations.
- Saves include protobuf persistence, backups, battle checkpoints, pending cinematics, and migration code.
- All **687 explicit image-path occurrences checked in asset JSON**, representing 602 distinct paths, resolved.
- All audio catalog entries resolved to shipped files; all 202 MP3s were readable by `ffprobe`.
- A substantial automated test infrastructure exists, including a campaign runner with real combat and save/reload checkpoints.

### What prevents a completion claim

1. **Dialogue conditions are not consistently implemented.** Both the Android and shared evaluators ignore several condition types used by production dialogue. Unknown conditions return `true`. The scan found **66 dialogue records containing 105 unsupported condition tokens**.
2. **Normal New Game still exposes legacy skill trees with unsupported promises.** The four playable characters have 100 legacy nodes. The alternative 32-node crew system exists, but normal sessions default to progression version `0`. Prototype balance improvements are also conditional on opting into that system.
3. **Current verification gates are not green.** Strict content, progression-reference, prose, and Maestro-selector checks returned failure. Some failures represent obsolete validator assumptions; others identify real maintenance work.
4. **Full-campaign automation is valuable but narrower than its name suggests.** Its opening fixture supplies a Cryo-Inductor and two Scrap Metal. It also uses mocked platform services. It does not independently establish the complete Android New Game experience.
5. **Presentation remains uneven across worlds.** Later bosses reuse earlier sprites; several unrelated NPCs use Orion's portrait; World 5 contains extensive repeated room descriptions.
6. **Android lifecycle and recovery need deliberate acceptance testing.** The normal navigation host creates `AppServices` without a matching release call. Save collectors, audio resources, interruption boundaries, packaged assets, memory, and device behavior remain material risks.

### February assessment

**February 19 remains a plausible development-complete target if shared correctness is addressed immediately and the world-review weeks are used for acceptance and bounded fixes.** It becomes much less credible if progression migration, recovery testing, or major puzzle redesign waits until January.

World 1 is the strongest available benchmark, but it still needs acceptance work. Its current implementation should establish the review process; its unresolved behaviors should not become the standard copied into later worlds.

---

## B. System Status Matrix

Path abbreviations used below:

- **Assets:** `app/src/main/assets/`
- **Runtime:** `gameRuntime/src/main/kotlin/com/example/starborn/`
- **Android:** `app/src/main/java/com/example/starborn/`

Confidence describes the evidence supporting the assessment, not confidence that the system works correctly.

| System | Observed state and evidence | Outstanding work | Priority | Confidence | Manual verification |
|---|---|---|---|---|---|
| Architecture | Shared JVM gameplay runtime, separate Android/desktop adapters, shared presentation module. `settings.gradle.kts`, `gameRuntime/`, `shared/` | Reduce duplicated contracts; avoid broad restructuring during world polish | P1–P2 | High | Platform parity |
| Content loading | JSON catalogs loaded through `AssetJsonReader`; parse failures can become empty lists/maps | Validate required catalogs before entering gameplay; surface actionable failures | P1 | High | Packaged missing/corrupt asset behavior |
| World navigation | All room edges and transition metadata passed topology checks; four declared one-way shortcuts | Verify gate satisfiability, hub shortcuts, return rules, and story urgency | P1 | High for topology | Yes |
| Quests/events | 62 quests have completion producers; event manager supports conditions, rewards, callbacks, repeatability | Test negative prerequisites, order permutations, interrupted completion | P0–P1 | High for structure; medium for behavior | Yes |
| Dialogue | Runtime condition support disagrees with production data | Implement shared condition semantics and eliminate fail-open behavior | **P0** | High | Yes |
| Cinematics | Illustrated/text sequences and completion callbacks are implemented | Interrupt/resume testing; review timing, duplication, and narrative continuity | P1 | Medium | Yes |
| Combat/ATB | 50 ms ticker, ready queue, pause checks, targeting, statuses, Break, rewards | Validate production builds, response windows, loss/retreat paths, long-session behavior | P1 | Medium–high | Yes |
| Enemy AI/bosses | Role/behavior scoring, cooldown checks, usage limits, distinct boss ability lists | Confirm readable tactics and fair encounter sequences | P1 | Medium | Yes |
| Patrol encounters | Movement manager and persistence exist; catalog has one campaign zone plus debug zone | Decide whether later-world patrol expansion is actually desired | P2 | High | Yes |
| Leveling/AP | XP thresholds and level-based unlocks implemented; level does not generally multiply base stats | Finalize growth expectations, AP pacing, and player-facing explanations | P1 | High | Balance |
| Skill trees | 100 legacy playable-character nodes; alternative 32-node system is opt-in | Choose production system, fulfill promises, migrate saves, rebalance | **P0** | High | Yes |
| Inventory/equipment | Ownership, compatibility, mod-slot rules, synchronous inventory bridge | Resolve duplicate item ID; reduce silent placeholder fallback | P1 | High | Yes |
| Shops/economy | Buying/selling checks, equipped-copy protection, price/gate logic, overflow checks | Earned-route affordability and optional-system economy | P1 | Medium | Yes |
| Tinkering/cooking/meals | 27 tinkering and 18 cooking recipes; batch and discovery checks; three-encounter meals | Verify acquisition, consumption, interruption, and regional access | P1 | Medium–high | Yes |
| Fishing | Multi-phase controller, gear, catch handling, touch/motion controls and pause support | Verify each zone, cancellation, rewards, accessibility | P1 | Medium | Yes |
| Arcade | Six engines and tier/repeat reward logic | Discovery-to-install routes; interruption and payout boundaries | P1–P2 | Medium | Yes |
| Astra | Five rooms, travel, facilities, crew entries, recreation and simulations | Post-world return continuity and completion-state dialogue | P1 | Medium–high | Yes |
| Saves/NG+ | Protobuf state, backups, legacy import, battle checkpoints, NG+ carryover | Production progression migration; process death; backup policy; write failure handling | **P0 acceptance gate** | Medium–high | Essential |
| Android lifecycle | Combat/fishing have explicit pause handling; audio observes background state | Normal service ownership/disposal; activity recreation; arcade interruption | P1 | High for inspected code | Essential |
| Room art | Every campaign room's base background has a 1088×1920 header | Visual consistency, cropping, scene-state accuracy, duplicate exports | P1–P2 | High for metadata | Essential |
| Characters/enemies | Existing artwork is integrated; substantial sprite reuse confirmed | Distinct important bosses/NPCs; choose final combat-character presentation | P1–P2 | High | Essential |
| Animation/VFX | Combat lunges, FX canvas, transitions, environmental simulation and steam frames exist | Full action readability, comfort settings, performance and coverage | P1–P2 | Medium | Essential |
| Audio | 55 music, 26 ambient, 121 cue catalog entries; all files exist | Streaming fade implementation, interruption mix, loops, listening approval | P1 | High for routing/metadata | Essential |
| UI/accessibility | High contrast, large targets and comfort settings have real consumers | Small-screen/large-text/TalkBack acceptance; consistent interactions | P1 | Medium | Essential |
| Regression infrastructure | Broad JVM/device/desktop tests and 85 Maestro YAML flows | Repair stale checks; strengthen fresh-start and negative-path coverage | **P0–P1** | High | Device execution required |
| Release packaging | Android API 26 minimum, SDK 36 target; install-time asset pack; debug/playtest/release variants | Test actual release bundle and installed assets; track/identity review | **P0 acceptance gate** | High for configuration | Essential |

### Architectural assessment

The shared runtime is a useful production foundation. This is not an argument for a rewrite.

The main maintenance risk is concentration and duplication:

- `ExplorationScreen.kt`: **8,589 lines**
- `ExplorationController.kt`: **6,422 lines**
- `CombatController.kt`: **3,736 lines**
- Android `AppServices.kt`: **2,611 lines**

More consequential than file length is duplicated logic: dialogue conditions exist in both Android services and the shared runtime; combat condition checks exist in both controller and processor; legacy/prototype progression paths coexist.

The dialogue defect demonstrates the practical cost: content authors can use a supported-looking token that a runtime silently ignores.

---

## C. World-by-World Status

### Comparative inventory

“Encounter rooms” below means rooms with a nonempty base `enemies` field. It excludes event-spawned and moving encounters and is not a complete encounter count.

| World | Rooms | Nodes | Main / side quests | Authored actions | Base encounter rooms | Rooms with description variants | Distinct base background paths |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 — Mines | 82 | 10 | 5 / 5 | 190 | 10 | 33 | 81 |
| 2 — Sector 9 | 93 | 10 | 5 / 5 | 164 | 13 | 41 | 91 |
| 3 — Spire | 70 | 10 | 5 / 6 | 194 | 8 | 20 | 65 |
| 4 — Foundry | 74 | 10 | 5 / 6 | 198 | 33 | 19 | 71 |
| 5 — Orbital Ring/Void | 81 | 9 | 5 / 5 | 198 | 40 | 18 | 80 |
| 6 — Source | 65 | 10 | 5 / 5 | 150 | 17 | 11 | 65 |

Fewer variants or shared backgrounds are not defects by themselves. They identify where equivalent story reactivity and presentation deserve inspection.

### World 1 — The Mines

**Observed state:** A developed opening with a complete main-quest chain, optional training, equipment preparation, power/lighting interactions, patrol content, relic calibration, escape combat, recruitment, and the Sector 9 transition.

The opening's current implementation contains concrete mechanical relationships:

- Cryo-Inductor repair, Flux Liner grounding, governor confirmation and the cutter test.
- A cinematic callback completes `w1_mq01`.
- Relic contact leads into Red Alert.
- The launch requires Chime splicing and the correct quest stage.
- Completion of the launch cinematic moves the party to `sector9_crash_site`.

**Confirmed issues and review targets:**

- `launch_pod` has an ungated north connection to `launch_rail`. That room describes an active launch, yet permits walking south back into the pod. The actual launch event independently warps to Sector 9.
- The kitchen's `thermal cooker` lacks the `w1_cook_ration_soup` action hook and refers to “Cook Rourke,” while the present NPC is `cook_vance`. Treat this as an orphaned content contract; decide whether to expose the recipe or retire it.
- The dialogue-condition defect affects opening and launch dialogue.
- The strict World 1 validator reports seven variant/action warnings, but several are stale checks: the corresponding actions are explicitly hidden after completion.
- The existing roadmap's premature-sacrifice claim is outdated. Current `w1_mq04_gauntlet_victory` advances to `cargo_lift_sacrifice`; Jed's sacrifice dialogue requires that stage.
- Current cutter-surge completion is callback-driven. Do not implement an old recommendation to remove an aftermath event without checking the current event IDs.

**Quality concerns requiring play:**

- Whether the first repair sequence feels purposeful rather than instructional.
- Whether the solo-Nova encounters teach readable tactical decisions.
- Whether safe resting and hub travel undermine Red Alert's urgency.
- Whether `medbay_exhaust`, `echo_well`, and `echo_memory` provide worthwhile atmosphere. Their lack of material rewards is not automatically a defect.
- Whether the transition between Jed's farewell, lift movement, Warden combat and Zeke's departure reads coherently.

**Verdict:** Suitable as the benchmark candidate, not yet signed off.

### World 2 — Sector 9

**Observed state:** The largest world, with substantial stateful content: crash recovery, temple access, stasis systems, Orion's awakening, Gh0st/Beast confrontation, grounded Link training, ship restoration, and departure confirmation.

The Beast-to-Link relationship is explicit: `w2_mq04_anchor_drill` requires the Beast task, unlocks `nova_link` in the cinematic callback, and then starts Liftoff.

**Important defects and risks:**

- World 2 is heavily exposed to ignored dialogue task conditions. Examples include Orion's awakening eligibility, Zeke's crash follow-up hints, stasis success/failure dialogue, Source Gate success/failure dialogue, and Botanist turn-in.
- `zeke_w2_sq01_turnin_1` requires five scan tasks in its authored condition, but those task checks are absent from the live evaluator. Its trigger awards XP/items and completes the quest. This is a concrete premature-turn-in risk, not merely stale prose.
- The discoverability audit reports **25 candidates**, requiring state-aware triage.
- Several Maestro flows quote retired World 2 dialogue.
- The explicit-link validator rejects `tide fishing`, although current rendering supports service actions inline. Correct the validator before changing valid content.

**Benchmark work:** Apply World 1's prerequisite-negative testing to stasis, crystal collection, patrol recovery, security-grid work and departure. World 2 already has extensive variants; the problem is ensuring the right one becomes available at the right time.

**Verdict:** Substantial content and integration exist; dialogue correctness is the immediate prerequisite to trustworthy acceptance.

### World 3 — The Spire

**Observed state:** A structured heist world with safehouse establishment, reconnaissance, credentials, disguises, blueprints, planning, Lens acquisition, lockdown and departure. Six side quests include the Skyline Salon investigation.

`w3_mq12_assemble_planning` explicitly requires six preparation tasks. This is a meaningful integrated quest structure.

**Navigation:**

Three nonreciprocal exits are intentionally declared as one-way shortcuts:

- `spire_underrail_platform` down to `spire_transit_plaza`
- `spire_service_lift` down to `spire_archive_vault`
- `spire_glasswalk` down to `spire_archive_vault`

The topology check accepts these and finds return paths. Their readability and availability still need gameplay verification.

**Important issues:**

- **28 discoverability candidates**, the highest world count in that screening.
- `director_vance_default_talk` uses three `milestone_set` guards that the dialogue evaluator ignores. This weakens the intended investigation gating.
- `quest_not_completed` is also unsupported, affecting the prototype-testing offer.
- `administrator_boss` shares `the_warden_combat.png` with several later bosses.
- World 3's departure event still describes slipping “the Shield,” whereas the current master story distinguishes the internal curtain from the later outer-Shield breach. Reconcile the player-facing transition wording; this is a narrative continuity concern, not proof of a broken transition.

**Benchmark work:** Clear signage for special exits, tested investigation prerequisites, distinct heist feedback and consistent transition terminology.

**Verdict:** Mechanically structured, with interaction discoverability and narrative continuity needing focused review.

### World 4 — The Foundry

**Observed state:** A more combat-dense world with workers, sabotage, conditioning systems, Gh0st/Rylos material, Anvil acquisition, engine/array theft, Titan combat and the Crucible side activity.

The departure event requires engine and array acquisition and moves the campaign to `orbital_executive_dock`.

**Important issues:**

- 33 base encounter rooms versus 10 in World 1. This does not prove poor pacing, but warrants an attrition review.
- `titan_walker_boss` uses the shared Warden sprite; `crucible_champion` uses the heavy-loader sprite.
- `vale_w4_mq20_coup` uses an unsupported `milestone_set` dialogue condition.
- Crucible claim dialogue uses unsupported `quest_not_completed`.
- **10 discoverability candidates**.
- The current departure payoff is substantially conveyed through a summary message. Review whether Gh0st's decision and the workers' consequences receive sufficient playable or cinematic space.

**Benchmark work:** Earned-loadout combat testing, preparation availability, optional arena rewards, and clear separation between industrial puzzles and Source interactions.

**Verdict:** Content-rich; combat density and character payoff are the major human-review priorities.

### World 5 — Orbital Ring / The Void

**Observed state:** A complete structural chain through docking, security, Thorne, firewall progression, Elara, Anchor acquisition, Compliance Avatar and the Tear.

**Important issues:**

- **40 of 81 rooms** have base enemy lists, the highest count in the campaign.
- **32 room records share six repeated description texts** across different regions. These include service alcoves, overlook niches, control pockets, caches and echo walks.
- This repetition can weaken orientation and make exploration feel padded even though the rooms and artwork exist.
- `compliance_avatar` shares the Warden sprite.
- Solar Maintenance uses unsupported `quest_not_completed` dialogue gating.
- The `deep_firewall_gamma` down exit to `deep_anchor_chamber` is an intentional one-way shortcut; test it before and after the relevant gates.
- Recent Orbital Fighter tempo adjustments in `CrewEncounterTempo` apply only to the prototype progression version. They do not establish normal-game balance.

**Benchmark work:** Differentiate repeated spaces by function, clue, consequence or character interaction; test combat attrition before adding more encounters; verify all optional work and return routes around the Tear.

**Verdict:** Structurally present, with the clearest evidence of repetitive traversal content.

### World 6 — The Source

**Observed state:** Crew rescue/reassembly, memory locations, final approach, Vale and god-form fights, final tuning, epilogue and credits are represented in production events.

`w6_mq30_tune_world` requires the god-form victory task. Its nested callbacks set `ms_game_complete`, warp to `source_new_world`, play credits, and set `ms_credits_seen`.

**Important issues:**

- Final-confrontation and companion-release dialogue use unsupported task conditions.
- `ascended_vale` uses the common Warden sprite; `ascended_god` uses the Iron Warden sprite.
- Source encounter tempo adjustments are prototype-only.
- The finale spans multiple state changes and nested cinematic callbacks. It requires explicit interruption testing before, during and after each callback.
- The final tuning interaction is implemented. Replacing it with a new mechanic remains a design choice, not unfinished implementation that can be assumed completed.
- Fewer description variants make it particularly important to check rescue-state and post-finale consistency.

**Benchmark work:** Each rescued companion should have distinct agency; victory must be mechanically earned; the finale must survive interruption; the ending must communicate Nova's cost and the regional rather than universal resolution.

**Verdict:** An implemented ending exists. Emotional effectiveness, late-game balance and recovery remain unverified.

---

## D. Master Development Backlog

**Classification**

- **Defect:** Contradiction established from current code/data.
- **Risk:** Concrete exposure identified; failure not reproduced.
- **Design:** A production-quality recommendation requiring judgment.

Effort estimates are focused person-hours, including targeted verification. They exclude queue time and extensive art/music revision cycles. Related items overlap and should not be summed mechanically.

### P0 — Required before closed beta

| ID | Classification | Problem and evidence | Acceptance criterion | Dependencies | Hours / estimate confidence |
|---|---|---|---|---|---|
| P0-1 | Defect | Dialogue ignores `quest_task_done`, `quest_task_not_done`, `milestone_set`, and `quest_not_completed`; unknown tokens pass. [Android evaluator](../app/src/main/java/com/example/starborn/di/AppServices.kt), [shared evaluator](../gameRuntime/src/main/kotlin/com/example/starborn/domain/dialogue/DialogueTriggerLogic.kt), `dialogue.json` | One shared evaluator supports every authored condition; unknown types fail validation; affected turn-ins/hints are unavailable before prerequisites and correct afterward | None | **12–24 / medium-high** |
| P0-2 | Defect + production decision | Normal New Game selects legacy progression; unsupported effects remain purchasable. [Session default](../gameRuntime/src/main/kotlin/com/example/starborn/domain/session/GameSessionState.kt), [catalog selection](../gameRuntime/src/main/kotlin/com/example/starborn/data/assets/WorldAssetDataSource.kt), `skill_trees/*.json` | Choose and ship one supported production contract. Every visible purchase has an executable effect. Migration refunds/translates old purchases exactly once; NG+ and saved AP remain coherent | Design decision; effect inventory | **32–64 / medium** |
| P0-3 | Risk / acceptance gate | Current source-only evidence does not establish recoverable full-game Android progression. [Persistence](../gameRuntime/src/main/kotlin/com/example/starborn/domain/session/GameSessionPersistence.kt), `events.json`, `RecoveryInstrumentedTest.kt` | Fresh release-like Android run reaches credits; saved checkpoints reload; killing the process around rewards, recruits, transitions and finale neither duplicates rewards nor blocks progress | P0-1, P0-2; stable candidate | **40–72 / low-medium**, excluding discovered repairs |
| P0-4 | Verified test gap | “Fresh game” campaign harness preloads mandatory materials and mocks platform services. [Harness](../app/src/test/java/com/example/starborn/domain/playtest/LegalRouteCampaignRunnerTest.kt) | Separate true production-bootstrap coverage from prepared fixtures; no mandatory acquisition is masked; retain zero-assisted combat assertions and world-boundary reloads | P0-1; progression choice | **12–24 / medium** |
| P0-5 | Verified gate failure | Strict validators currently fail; CI invokes the validation aggregate. `app/build.gradle.kts`, `.github/workflows/android-ci.yml`, `scripts/validate_*` | All release-blocking checks pass for documented reasons. Align validators with current runtime; do not weaken checks merely to obtain green output | Runtime contracts settled | **16–28 / medium** |

**Progression evidence:** `nova_light_fingers` advertises `steal_rate`; `nova_shadow_blade` advertises a damage multiplier; `nova_lockpicking` advertises an unlock utility. The inspected runtime applies selected stat buffs and explicit skills/talents, but these advertised legacy effect contracts have no corresponding consumer found. I did **not** independently establish the older report's exact “48 unsupported nodes” count.

### P1 — Important for a solid February beta

| ID | Classification | Problem and evidence | Acceptance criterion | Dependencies | Hours / estimate confidence |
|---|---|---|---|---|---|
| P1-1 | Defect | `phase_rounds` has two incompatible definitions; later record overwrites earlier in `ItemRepository.load()`. [Items](../app/src/main/assets/items.json), [repository](../gameRuntime/src/main/kotlin/com/example/starborn/data/repository/ItemRepository.kt) | One intentional ID contract; reward/shop/UI agree on stats, value and description; duplicate IDs rejected | Item design decision | **3–6 / high** |
| P1-2 | Defect | Post-defeat recovery uses raw character HP rather than effective maximum HP. [Recovery](../gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt), `PartyHealth.kt` | Recovery uses the chosen effective-health rule consistently with equipment, passives and meals; no unintended clamp to base HP | Progression contract | **3–6 / high** |
| P1-3 | Defect + risk | Normal `NavigationHost` creates `AppServices`, but no matching release call was found; its scopes/audio have explicit cleanup available. [Host](../app/src/main/java/com/example/starborn/navigation/AppNavigation.kt), [release](../app/src/main/java/com/example/starborn/di/AppServices.kt) | Explicit lifecycle owner; repeated activity recreation leaves one active service/session owner and no orphan audio or save collectors | Lifecycle design | **8–16 / medium** |
| P1-4 | Defect | Streaming music/ambient start methods ignore `Play.fadeMs`; music switches cancel the preceding fade and replace the media immediately. [Audio player](../app/src/main/java/com/example/starborn/domain/audio/AudioCuePlayer.kt), `AudioRouter.kt` | Authored fade behavior is honored or deliberately redefined; transitions, rapid reentry, duck/restore and mute work consistently | Audio transition decision | **8–16 / medium-high** |
| P1-5 | Defect | Walkable `launch_pod` → `launch_rail` contradicts launch state. `rooms.json`, `w1_mq05_use_nav_console` | Rail presentation is accessible only in a valid launch state; no prelaunch “in-flight” excursion or return | World 1 sequence decision | **2–4 / high** |
| P1-6 | Risk | Discoverability screening finds 78 candidates; runtime visibility and validator rules differ | Triage each candidate; every required action has a usable target in each relevant state; service-link validator matches runtime | P0-1, P0-5 | **12–24 / medium** |
| P1-7 | Risk | Saves have backups/checkpoints, but transaction boundaries and write failures remain unverified; Android backup XML remains template configuration | Test failed writes, corrupt saves, process death and restore; explicitly define production/test-save backup scope using actual `.pb` files | Stable save schema | **16–32 / medium-low** |
| P1-8 | Risk | Large media footprint and multiple decode paths. [Bitmap cache](../app/src/main/java/com/example/starborn/ui/background/BackgroundPainters.kt), `HubScreen.kt`, environmental frame cache | Release installation resolves every asset; representative low-memory phone completes long traversal without crash or progressive memory growth; startup/frame-time budgets recorded | Packaged candidate | **20–36 / medium-low** |
| P1-9 | Risk | Normal and prototype progression use different late encounter tuning; W4/W5 contain dense encounter placement | Test earned builds, limited supplies, defeat/retreat and optional spending; document acceptable difficulty/attrition and paid-build value | P0-2 | **24–48 / medium-low** |
| P1-10 | Design, supported by asset reuse | Major bosses and unrelated NPCs lack distinct visual identity. `enemies.json`, `npcs.json` | Approve distinct silhouettes for major antagonists/bosses; replace Orion portraits on unrelated creatures; verify in combat/dialogue layouts | Art direction | **32–72 / low** |
| P1-11 | Design | World 5 repeats six descriptions across 32 rooms; later transition payoffs often use summary messages | Each repeated location earns its place through distinct navigation, interaction, consequence or deliberate pacing; chapter turns receive director approval | World review | **16–32 / medium** |
| P1-12 | Risk | Accessibility settings are implemented but complete usability is unverified | Complete opening, combat, inventory, puzzles, saves and departure on small screen/large text; test TalkBack labels, touch targets and comfort settings | Stable UI | **16–28 / medium-low** |
| P1-13 | Risk | Music/SFX files exist, but cue coverage and listening quality are unaccepted | Listening checklist covers exploration, combat, bosses, menus, cinematics, failure, looping and interruptions for all worlds | P1-4; stable content | **16–28 / medium-low** |

For P1-2, the discrepancy is concrete: Nova's authored base HP is 60 and Vitality is 6; the shared formula gives 120 HP before other bonuses. Recovery instead calculates its floor and cap from 60.

### P2 — Polish ideally completed before May

| ID | Classification | Work and evidence | Acceptance criterion | Dependencies | Hours / confidence |
|---|---|---|---|---|---|
| P2-1 | Design | Broaden puzzle interaction vocabulary. Six tuning-puzzle records exist; only the World 2 relic explicitly selects `prism_optics` | Changes improve distinctness without breaking authored prerequisites, failure recovery or saves | Stable campaign; approved prototypes | **24–56 / low** |
| P2-2 | Design | Main-character combat images are identical to portraits; combat motion largely comes from rendering/VFX | Approve a deliberate presentation standard; prioritize readable attacks, impacts and major abilities before expanding animation volume | Art direction/performance budget | **40–100 / low** |
| P2-3 | Risk | Mixed legacy/demo records, obsolete tree entry and overlapping implementations increase maintenance noise | Clearly separate production and retained test content; remove only proven-unused records; prevent new silent placeholders | Stable references and migration | **12–24 / medium** |
| P2-4 | Design | Tone, timing, music mix and scene-state cohesion need a unified pass | All worlds meet the same review rubric; remaining exceptions have owner approval | World acceptance | **24–48 / low-medium** |

### P3 — Optional or post-release

| ID | Classification | Work | Acceptance criterion | Dependencies | Hours / confidence |
|---|---|---|---|---|---|
| P3-1 | Design | Extend moving patrols beyond the single campaign pilot zone | Demonstrated pacing benefit; no added quest/recovery instability | Accepted movement model | **20–48 / low** |
| P3-2 | Maintenance | Broader decomposition of exploration/UI monoliths | Preserve behavior while reducing ownership boundaries; targeted regression remains green | Stable release | **40–80 / low** |

Do not make wholesale refactoring, universal patrol expansion, or a complete animation replacement prerequisites for February.

---

## E. Reusable World Review Template

Use one review packet per world, tied to a commit and build. Distinguish **content complete**, **functionally accepted**, and **presentation approved**.

### 1. Scope and entry contract

- [ ] Record world, hubs, nodes, rooms, quests, bosses, optional activities and departures.
- [ ] Define expected entry party, inventory, learned abilities and story state.
- [ ] Enter through the preceding world's actual transition.
- [ ] Repeat entry from a saved checkpoint.
- [ ] Verify Astra return access separately from first arrival.
- [ ] Identify irreversible decisions and genuinely missable content.

**Completion criterion:** Entry never depends on debug grants or manually inserted milestones.

### 2. Main progression

- [ ] Complete every main quest through public interactions.
- [ ] Attempt each consequential action before prerequisites.
- [ ] Verify blocked feedback names a useful next step.
- [ ] Complete prerequisites in alternate reasonable orders.
- [ ] Repeat interactions after completion.
- [ ] Leave/reenter rooms during partially completed tasks.
- [ ] Verify journal task state, tracked objective and map visibility.
- [ ] Confirm each chapter transition happens once and reaches the correct destination.

**Completion criterion:** No premature completion, unrecoverable state, unexplained gate or duplicate payout.

### 3. Navigation

- [ ] Traverse every room and authored exit.
- [ ] Check cardinal direction against map geometry.
- [ ] Identify one-way exits before commitment.
- [ ] Test diagonals, lifts, vertical exits and authored travel links.
- [ ] Check node discovery versus hub-entry permission.
- [ ] Verify hub return from entry rooms and refusal elsewhere.
- [ ] Check dark-room movement and generator changes.
- [ ] Revisit after major story changes.

**Completion criterion:** Every intended room is reachable in an appropriate state; every intentional restriction is understandable.

### 4. Dialogue and character development

- [ ] Test first meeting, active objective, completed objective and post-world dialogue.
- [ ] Verify all condition types against runtime support.
- [ ] Test every option with a gameplay consequence.
- [ ] Check NPC presence before/after recruitment, departure, death and rescue.
- [ ] Confirm each party member contributes beyond exposition.
- [ ] Review names, terminology, character knowledge and causal continuity.
- [ ] Read scenes aloud at actual display speed.
- [ ] Inspect portraits/emotes and unknown-speaker treatment.

**Completion criterion:** Dialogue reflects the actual game state and preserves the intended character/plot outcome.

### 5. Side quests and exploration value

- [ ] Complete all side quests through earned acquisition.
- [ ] Test acceptance and turn-in before prerequisites.
- [ ] Test alternate quest order and revisits.
- [ ] Check shared ingredients and key-item consumption.
- [ ] Confirm rewards and optional choices persist.
- [ ] Review every dead end for atmosphere, information, interaction or reward.
- [ ] Identify repeated rooms that do not earn their traversal time.

**Completion criterion:** Optional content is discoverable, coherent and independently completable under its advertised rules.

### 6. Combat and economy

- [ ] Enter with an earned baseline loadout.
- [ ] Test at least one alternative supported build.
- [ ] Check weak/resistant/immune feedback.
- [ ] Test status expiry, cooldowns, Break, overcharge and enemy telegraphs.
- [ ] Test defeat, retreat and return to exploration.
- [ ] Verify boss phase/ability behavior.
- [ ] Track combat time, damage taken, healing used and repeated low-value actions.
- [ ] Test affordability after plausible optional purchases.
- [ ] Confirm resting, shops, cooking and equipment preparation are reachable.

**Completion criterion:** Required fights are understandable and recoverable; success does not depend on an undocumented exploit or an unusually specific build.

### 7. Puzzles and activities

- [ ] Solve from clues available in-game.
- [ ] Attempt incorrect inputs and cancellation.
- [ ] Leave/reenter and save/reload mid-puzzle.
- [ ] Confirm completion persists and rewards cannot be duplicated.
- [ ] Test fishing/crafting/arcade acquisition as well as isolated operation.
- [ ] Check touch-only operation where motion is optional.

**Completion criterion:** Failure teaches or permits retry; no mandatory interaction requires external instructions.

### 8. Art, animation and UI

- [ ] Review every used background and important state variant on-device.
- [ ] Confirm image framing leaves interaction text and characters readable.
- [ ] Check NPC/boss identity, transparency, scale and ground alignment.
- [ ] Inspect transitions and attack/impact timing.
- [ ] Verify required actions remain tappable across description variants.
- [ ] Test smallest supported layout, large text and large-target mode.
- [ ] Check system Back, modal dismissal and overlapping prompts.
- [ ] Verify high contrast and flash/shake settings.

**Completion criterion:** No missing/misleading asset, obscured required control, unreadable text or unavoidable comfort-setting violation.

### 9. Music and sound

- [ ] Listen to each regional track and its loop boundary.
- [ ] Check exploration-to-combat-to-victory return.
- [ ] Check boss, death, cinematic and departure cues.
- [ ] Verify key actions have appropriate feedback.
- [ ] Test rapid transitions, pause/resume, incoming interruptions and mute.
- [ ] Review loudness on speakers and headphones.
- [ ] Confirm character themes support the intended scene.

**Completion criterion:** No missing essential cue, persistent ducking, abrupt unintended cut, intrusive loop seam or inconsistent volume.

### 10. Recovery and final sign-off

- [ ] Save/reload at entry, mid-world, pre-boss and post-world.
- [ ] Kill/relaunch around purchases, crafting, rewards and cinematics.
- [ ] Test completed and partially completed side quests after reload.
- [ ] Run current static checks and relevant automated regressions.
- [ ] Complete a continuous playthrough without debug assistance.
- [ ] Record build, device, settings, route, defects and accepted exceptions.

**World completion gate:** No open P0; no unresolved world-critical P1; required and optional routes accepted; presentation reviewed; recovery evidence retained.

---

## F. World 1 Action Plan: October 20–23

**Recommendation: reserve four days, with an early-exit gate after Thursday.** Static inspection does not justify declaring that World 1 will certainly finish sooner.

Assumption: roughly six focused hours per day for the primary developer/director. Separate QA or art support can run concurrently.

| Day | Work | Exit evidence |
|---|---|---|
| **Tuesday, October 20** | Fix shared dialogue conditions first. Cover opening repair dialogue, Warden intro and Zeke's post-Warden/Chime states. Reconcile World 1 validator warnings with runtime hide rules. Begin a genuine New Game walkthrough through the cutter surge. | Before/after prerequisite cases pass; opening reaches `w1_mq02` with earned items; current content checks have a truthful result |
| **Wednesday, October 21** | Review clearance, Heavy Lifting, generator/lighting, mine navigation and relic calibration. Test cart diversion/reset/breach, optional routes and patrol encounter. Verify current HP recovery. Resolve the kitchen hook/name decision if retained. | Main and optional progression state recorded; no required action disappears; defeat/rest/save behavior consistent |
| **Thursday, October 22** | Review the complete Red Alert-to-Sector 9 sequence. Fix launch-rail access. Verify gauntlet → Jed farewell → Warden → Zeke → Chime → launch ordering. Check backtracking/rest continuity and audio transitions. | Correct scene order; one launch/one payout; successful reload before and after departure |
| **Friday, October 23** | Run uninterrupted World 1 acceptance plus focused side-quest/recovery checks. Review small-screen/large-text behavior and key audiovisual moments. Repair only material findings. Produce the benchmark checklist and accepted exceptions. | World 1 sign-off packet, or a short explicit list of remaining blockers |

### Scope boundaries for this week

- Decide the production progression direction now, but do not promise the entire migration and balance program fits inside World 1's four days.
- Do not retune `pressure_hauler` solely from the older roadmap's stat comparison. Use actual solo-Nova fight evidence.
- Do not reintroduce an obsolete fix for sacrifice-on-entry; current stage gating already differs.
- Do not add loot to every atmospheric room by default.
- Do not spend the week redesigning all menus or replacing all character art.

**Start World 2 early** if Thursday's continuous run, optional-route evidence and recovery checks are clean. In that case, use Friday for World 2 crash/stasis dialogue regression—the shared condition fix makes that particularly valuable.

If Tuesday uncovers a broader opening acquisition problem or the progression decision requires immediate migration, four days may not be enough. Record that specifically rather than accepting World 1 on the strength of existing fixtures.

---

## G. Schedule and Critical Path

There are **89 Monday–Friday dates from October 20 through February 19 inclusive**, before holidays, leave and the lighter Thanksgiving week.

At six focused hours per day, that is **534 hours per full-time person before those deductions**. It is not an estimate of available team capacity.

### Proposed calendar

| Dates | Primary objective |
|---|---|
| Oct 20–23 | World 1 acceptance and shared-condition repair |
| Oct 26–30 | World 2 |
| Nov 2–6 | World 3 |
| Nov 9–13 | World 4 |
| Nov 16–20 | World 5 |
| Nov 23–27 | Lighter week: triage, asset review, documentation reconciliation, bounded regressions |
| Nov 30–Dec 4 | World 6 and ending |
| Dec 7–Jan 29 | Production progression, balance, asset/audio integration, platform hardening and continuous campaign testing |
| Feb 1–12 | Release-candidate regression and fixes |
| Feb 15–19 | Final acceptance, packaging and development-complete decision |
| Feb 22 | Closed beta |

This preserves **55 weekdays from December 7 through February 19**, before holidays.

### Critical path

1. **Dialogue/event contracts**
2. **Production progression decision and migration**
3. **Earned-build world and economy acceptance**
4. **Recovery and platform hardening**
5. **Release-bundle verification**
6. **Final continuous campaign regression**

Art and audio can proceed alongside this path once scene and encounter requirements are stable.

### Schedule risks

**Shared defects can invalidate world sign-off.** Fixing dialogue conditions after six world reviews would require substantial retesting. Address them before using those reviews as acceptance evidence.

**Prototype success can be mistaken for production success.** `CrewEncounterTempo.apply()` explicitly returns unchanged enemies for nonprototype sessions. Tests and reports must name the progression version.

**Content editing can invalidate automation.** The current 45 stale selector findings already demonstrate this. Stabilize action identifiers and semantic selectors while preserving a smaller set of presentation assertions.

**Asset volume can hide release risk.** The inventoried images total approximately **981.5 MB**, and MP3s **265.6 MB**, in source files. These are not APK/download measurements. The install-time asset pack and decoded-memory footprint need real artifact/device evidence.

**A polished JRPG requires human review time.** A valid route cannot establish emotional payoff, navigation clarity, encounter fatigue or music quality.

### Parallel work opportunities

- **Engineering:** condition contracts, progression migration, save/lifecycle correctness.
- **World review:** route, dialogue, pacing and optional content.
- **Art:** major boss/NPC differentiation and scene-state corrections.
- **Audio:** cue sheet, listening review, mixing and transition acceptance.
- **QA:** regression maintenance and device matrix.

For a team with only one primary engineer, prioritize narrow fixes and test reliability over architectural cleanup.

### Required for beta versus February–May

**Before beta:**

- Trustworthy progression and dialogue gates.
- Executable player-facing skill promises.
- Recoverable saves and transitions.
- A finishable continuous campaign on Android.
- Clear required interactions.
- Stable installed assets and acceptable device performance.
- Major bosses and story actors visually identifiable.
- No known release-blocking validation failures.

**Can continue between beta and May:**

- Additional animation richness.
- Optional puzzle redesigns.
- Broader patrol placement.
- Secondary NPC uniqueness.
- Fine music mixing and optional cue variety.
- Noncritical prose refinement.
- Structural refactoring with limited player benefit.

The deadline need not move automatically. The necessary change is to make acceptance evidence and shared integration work first-class scheduled work.

---

## H. Top 10 Next Actions

1. **Implement one shared dialogue-condition evaluator.** Cover the four missing token types, reject unknown tokens, and test the 66 affected records.
2. **Choose the production progression contract.** Audit all 100 current nodes, then implement the approved cutover or fulfill legacy effects with idempotent save migration.
3. **Repair the current validation baseline.** Reconcile runtime/validator action visibility, supported dialogue triggers, prose rules and the 45 stale Maestro selectors.
4. **Remove mandatory material grants from a true fresh-start regression.** Keep prepared campaign fixtures separately labeled and retain the existing real-combat runner.
5. **Fix World 1's prelaunch rail access and complete the escape acceptance route.** Verify gauntlet, Jed, Warden, Zeke, Chime, launch and reload ordering.
6. **Resolve `phase_rounds` and add catalog uniqueness checks.** Verify arcade reward and shop behavior against the chosen item definition.
7. **Use effective maximum HP in defeat recovery.** Test base, equipped, passive and meal-modified cases.
8. **Give normal Android `AppServices` explicit lifecycle ownership.** Verify recreation, one active save collector, and audio cleanup.
9. **Honor audio transition contracts and perform the first listening pass.** Start with opening, Warden, launch/crash, battle return and finale.
10. **Create the release acceptance run matrix.** Include earned-build world routes, optional quests, process death, low-memory traversal, large text and the actual install-time asset pack.

---

## I. Audit Coverage and Limitations

### 1. Repository coverage

The initial inspection read the repository entry documentation, current implementation snapshot, systems overview, master story, content guidance, testing guide and current World 1/project evaluation documents. No `AGENTS.md` was found in the searched repository or checked ancestor locations.

| Area discovered | Reliably measured files | Inspection depth |
|---|---:|---|
| `app/src/main/java` | 120 Kotlin | Broad inventory; focused semantic inspection of services, navigation, audio, image loading and selected UI |
| `gameRuntime/src/main` | 180 Kotlin + 1 proto | Broad inventory; substantial selected-system inspection, not every line |
| `shared/src` | 15 Kotlin | Partial: build configuration and environmental/presentation consumers |
| `desktopApp/src/main` | 88 files, 86 Kotlin | Primarily inventory and selected integration references |
| `app/src/test` | 146 Kotlin | Full inventory; representative test-body inspection |
| `app/src/androidTest` | 31 Kotlin | Full inventory; selected recovery/device-test inspection |
| `desktopApp/src/test` | 67 Kotlin | Inventory and selected campaign/platform test references |
| `gameRuntime/src/test` | 4 Kotlin | Inventory |
| `scripts` | 77 files | Validator safety inspection; selected validators executed |
| `playtests/maestro` | 85 YAML, 2 Markdown, 1 JS | Inventory, selector scan, representative flow inspection |
| `docs` | 344 files | Entry/current documents and selected design/testing reports; not all documents |
| `app/src/main/assets` | 51 files, including 49 JSON | All JSON parsed for inventory/reference work; semantic depth varies |
| `world_assets/src/main/assets` | 983 images | All inventoried and image headers read |
| `app/src/main/res/raw` | 202 MP3 | All inventoried, hashed and probed |
| `PythonDevKit` | 51 files | Inventory only |
| `Starborn_Python` | 2,624 files | Inventory only; legacy Python implementation not audited |
| `starborn-observatory` | 24 files excluding generated/dependency directories | Inventory only |

Other discovered areas included `data`, root `skill_trees`, `distribution`, `reports`, `output`, `tools`, booth assets, build outputs and multiple local caches.

**Not comprehensively audited:** desktop UI/packaging, iOS targets, legacy Python tools/game, observatory tooling, generated reports, distribution binaries, all archival design documents, complete dependency compatibility, and every UI screen.

### 2. Game-system coverage

| System | Coverage classification |
|---|---|
| Catalog structure and selected typed references | Comprehensive mechanical checks over the selected catalogs |
| Room topology | Comprehensive static check of all records/connections |
| Stateful navigation and hub gates | Partial semantic inspection |
| Main-quest transitions and ending | All six world transitions inspected; full state-space not verified |
| Side quests | Completion producers checked for all; representative prerequisite/turn-in tracing |
| Dialogue conditions | Comprehensive scan of top-level authored conditions against inspected evaluators |
| Combat | Partial but substantial: ATB, AI interfaces, formulas, skills, rewards, recovery |
| Skill trees | All tree records inventoried; selected unsupported contracts traced |
| Inventory/equipment | Substantial selected-path inspection |
| Shops/crafting/meals | Selected transaction and eligibility paths inspected |
| Fishing/arcade | Partial controller, lifecycle, reward and test inspection |
| Saves/NG+ | Substantial selected-path inspection; no runtime recovery verification |
| Audio | Complete file/catalog inventory; partial routing/player semantic inspection |
| UI/accessibility | Partial implementation inspection; no device usability acceptance |
| Art/animation | Complete image inventory/header checks; three visual samples; rendering sampled |
| Release | Build/manifest/asset-pack/CI inspection; no artifact verification |

No system is labeled fully gameplay-verified.

### 3. World-content coverage

Every world's room records were parsed and included in topology, ownership, action/reference and background-metadata checks. This is **not** equivalent to reading and judging every description or exploring every state.

| World | Sources inspected | All rooms mechanically checked? | Quest/progression tracing | Navigation verification | Remaining gaps |
|---|---|---|---|---|---|
| 1 | Rooms, nodes, transitions, quests, events, dialogue, enemies, movement, cinematics, guides/tests | Yes | Deepest sampling; opening and escape traced | Full topology; selected gates | Complete live run, all side-state permutations, audiovisual acceptance |
| 2 | Same shared catalogs, puzzle records and selected flows | Yes | Recruitment, Link, stasis/turn-in conditions, departure | Full topology; selected gates | All puzzle branches, optional routes and recovery |
| 3 | Shared catalogs, heist/investigation events and navigation | Yes | Planning prerequisites, investigation guards, departure | Full topology including shortcuts | Full social/investigation state-space, pacing |
| 4 | Shared catalogs, combat/provisioning records and chapter events | Yes | Major acquisition/departure and selected optional gates | Full topology | Earned attrition, Crucible lifecycle, character payoff |
| 5 | Shared catalogs, repeated prose, firewall/Anchor/Tear events | Yes | Main transitions and selected side guards | Full topology including shortcut | Optional timing, combat density, room-by-room editorial review |
| 6 | Shared catalogs, rescue/finale events, tuning and combat definitions | Yes | Reassembly and ending callbacks traced | Full topology | Rescue-order permutations, final combat and interruption testing |

### 4. Asset coverage

**Images**

- Inventoried **985 production image files**: 983 in the asset pack plus two in app assets.
- Read headers/dimensions for all; no header-opening failures.
- Checked all explicit `images/...png/webp/jpg` strings found in asset JSON: no missing files.
- Confirmed all campaign base-room backgrounds are 1088×1920.
- Inventoried 712 files under room-image directories, including alternate/unused versions.
- Inspected character portrait/combat hashes; all four main-character pairs are identical.
- Examined reuse assignments across all enemy and NPC records.
- Visually inspected `orbital_executive_dock.webp`, `the_warden_combat.png`, and `nova_combat.png`.

Those samples show a detailed portrait environment, a full-body armored enemy, and a bust portrait used for Nova's combat image. They do not establish whole-game style consistency.

**Animation**

- Inspected selected combat/VFX/environmental code and frame-loading paths.
- Did not watch animation sequences or inspect every frame.
- No claim of complete animation coverage or consistent frame pacing.

**Audio**

- Inventoried and hashed all 202 MP3s.
- Found 201 distinct hashes; `music_victory_standard.mp3` and `music_victory_theme.mp3` are identical.
- Probed all files successfully; durations range approximately **0.157–361.8 seconds**.
- Catalog contains 55 music, 26 ambient and 121 cue entries.
- No missing catalog files.
- No filenames explicitly labeled temporary/placeholder were found.
- No tracks specify custom loop start/end points; current playback uses whole-track repeat.
- **No listening-quality assessment was performed.** Absence of a temporary filename does not establish final music approval.

### 5. Non-mutating verification results

| Check | Result | Interpretation |
|---|---|---|
| `navigation_audit.py --check` | Pass | Geometry, return paths, declared shortcuts and transition metadata consistent |
| Independent node-entry traversal | No unreachable owned rooms with gates ignored | Structural connectivity only |
| Selected catalog duplicate-ID scan | Duplicate `phase_rounds` | Confirmed incompatible item definitions |
| Quest completion-producer scan | All 62 quests have a producer | Does not prove reachable/satisfiable completion |
| Image references and headers | Pass for checked scope | Existence/resolution, not artistic approval |
| Audio file/catalog checks | Pass | Metadata/readability, not playback quality |
| Room presence validator | Pass: 477 rooms, 47 NPCs, zero duplicate-availability warnings | Authored presence-rule screening |
| Audio references validator | Pass: 114 referenced cues, zero catalog warnings | Checked reference integrity |
| World 1 balance validator | Pass: 7 enemy types, 82 rooms | Guardrails only; not balance acceptance |
| Enemy movement validator | Pass: 2 zones, 7 party definitions | Includes debug content |
| Dialogue emotes validator | Pass: 53 used references | Referenced emotes exist |
| Strict World 1 content validator | Exit 1 | Seven variant/action warnings; several contradicted by runtime hide rules |
| Strict progression references | Exit 1 | `unlock_skill` and `sting` token warnings; runtime support differs between parser/event layers |
| Strict prose validator | Exit 1: 126 errors, 54 warnings | Includes length limits and retired required wording; not 126 established narrative defects |
| Strict Maestro selectors | Exit 1: 45 candidates | Flow maintenance required; regex/selector cases need triage |
| Explicit action references | 45 references, 7 reported errors | Validator rejects service targets supported by current renderer |
| Discoverability screening | 477 rooms, 1,061 inline-action candidates, 78 missing-name candidates | Candidate list, not 78 proven inaccessible actions |

The explicit-action mismatch is directly visible: the validator rejects fishing, rest and tinkering targets, while `RoomPresentation.isInlineDescriptionAction()` currently excludes only NPC and enemy actions, and Android passes the current action list into that renderer.

### 6. Testing limitations and recommended regression structure

Existing tests should be retained, but their evidence should be separated:

1. **Static contracts:** IDs, references, condition/action support, navigation, assets and skill promises.
2. **Pure gameplay rules:** combat, inventory, equipment, prices, crafting, XP and status timing.
3. **Public-controller routes:** earned acquisitions, side-quest permutations, full campaign, save/reload.
4. **Android interaction tests:** actual controls, navigation, modals, lifecycle and accessibility.
5. **Physical-device/release tests:** installation, asset delivery, memory, interruptions and sustained play.
6. **Human acceptance:** narrative, pacing, tactical interest, art and sound.

For reliable full-game regression:

- Maintain a true New Game route separately from prepared checkpoints.
- Record progression version, seed, route, equipment, purchases and assistance.
- Test prerequisites negatively, not only successful paths.
- Save at every world boundary and before irreversible events.
- Include optional-order permutations and repeat-interaction checks.
- Inject process termination around reward/cinematic boundaries.
- Prefer stable semantic selectors for actions; reserve prose assertions for intentional copy contracts.
- Preserve build-specific evidence rather than treating old passing totals as current status.

No build or runtime test was executed during this audit because those operations were outside the authorized read-only scope.

### 7. Remaining unknowns

| Unknown | Evidence needed |
|---|---|
| Does the current Android build finish from an untouched New Game? | Continuous release-like run with no seeded inventory or debug assistance |
| How much do ignored dialogue guards affect real routes? | Negative/positive prerequisite tests and replay of affected conversations |
| Which progression design will ship? | Explicit decision, complete effect contract and migration acceptance |
| Are paid builds rewarding and late encounters fair? | Earned-loadout, multi-seed combat plus human play |
| Can saves survive every consequential interruption? | Process-death and failed-write matrix |
| Does normal activity recreation leak service ownership? | Lifecycle instrumentation and resource/collector observation |
| Are all important sound triggers audible and appropriately mixed? | Device listening and interruption pass |
| Are all rooms visually coherent and worth their traversal time? | Room-by-room human review |
| Is the installed asset pack reliable on target phones? | Actual bundle/install testing |
| Is February workload supportable by the team? | Named owners and realistic weekly engineering, QA, art and audio capacity |

**Coverage conclusion:** This audit establishes broad structural coverage and several substantiated cross-system defects. It does not establish complete semantic coverage of every source file, quest branch, room state, visual asset or device behavior. The highest-value next work is now specific enough to begin without repeating the repository-wide investigation.
