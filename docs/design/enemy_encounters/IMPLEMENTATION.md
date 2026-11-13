# Implementation, art production and acceptance

Status: original rollout plan and pre-implementation capability audit. The shared prototype and bounded World 1 Warden pass are now implemented locally; [current progress](PROGRESS.md) supersedes the original gap descriptions below where noted. The other world passes and final art remain proposed. Existing playtest fixes and generated music/portraits predate this work and are preserved.

## 1. Establish the baseline before changing a world

The snapshot records the current working assets, not merely Git HEAD. Revalidate its source hashes before using it as implementation input. If later work legitimately changes those assets, reconcile the affected rows deliberately instead of silently regenerating away decisions.

Capture earned arrivals, mandatory-tool loadouts, shared AP, gear, meals, recovery stock and currently completed encounters. Keep both a main-story route and a completionist route. A stocked debug save and a deterministic combat seed do not establish campaign balance.

The current broader Android suite has 23 previously reproduced baseline failures, recorded in the preceding playtest audit. Preserve that distinction; this documentation check does not resolve them or establish a green production suite. Confirm their status again when implementation begins.

## 2. Runtime capability and work required

| Capability | Current source evidence | Required work |
| --- | --- | --- |
| Authored enemy parties | Room `enemy_parties` and public encounter descriptors support grouped fights. Bare `enemies` produces separate solos. | Author full groups explicitly; preserve room instances and occurrence-specific guaranteed drops. |
| Enemy tells/recovery | Opening skills, conditional abilities and `recovery_after` exist; Titan/Golem already have recovery data. | Add consistent actionable intent preparation where missing; readable queued target and cancellation feedback across both UIs. |
| Independent reinforcement actors | Skills called Deploy Drone/Drone Support Protocol currently apply statuses or damage; they do not add targetable actors. | Add bounded spawn/despawn commands, stable instance IDs, ATB/target-list integration and persisted reserve counts. |
| Composite layout | Enemy model has `CompositePart`; Android roster has a composite branch and exploration has composite icons. No active enemy definition uses it. | Prove body + four weapons in both battle UIs; audit old Driller-specific layout cases and mixed composite/escort behavior rather than assuming parity. |
| Core victory | CombatController recognizes composite role `core` and currently finishes when any marked core is down. | Configure only the intended body as the victory core; specify aggregate completion for duets and staged finales. Zero rewards or story credit for part-only deaths. |
| Disable-linked attacks | Recovery supports individual skills, not a complete detachable-weapon contract. | Track linked part availability, cancel its queued action on destruction/disable, and make chassis fallback explicit. |
| In-battle phases | Existing low-HP skill conditions can change skill eligibility. World 6 forms are currently separate room fights. | Add explicit one-time transition state, presentation, shared action budgets and linked encounter checkpoints. No second AI turn hidden in a transition. |
| Stable targeting | Duplicate enemy instances receive suffixed combat IDs. Some lists are assembled at encounter creation. | Update target resolution, area attacks, ally healing, initiative and dead-target replacement when enemies enter/leave. Never identify a component only by its sprite or list index. |
| Victory progression | Event listeners often match any listed enemy ID rather than an exact encounter. | Scope redesigned victories to stable encounter/stage identity and arena; retain legacy aliases. Do not broaden a trigger until a common replacement enemy can complete the wrong quest. |
| Reward/KO persistence | Combat victory and room return own party-vital and reward persistence. | Inspect `persistPartyVitals` and level-up behavior before linking phases. A room-return helper that clamps KO HP to one must not silently revive the crew mid-finale. |

Relevant source entry points: `gameRuntime/.../domain/model/Enemy.kt`, `Room.kt`, `domain/event/EventManager.kt`, `domain/combat/EncounterCoordinator.kt`, `feature/combat/viewmodel/CombatController.kt`, `helpers/CombatEnemyAI.kt`, Android `RosterViews.kt`/`EnemyFormationViewport.kt`, and desktop `DesktopBattlefield.kt`. Exact file locations are under the repository's current source tree; historical design documents are not evidence of completed runtime behavior.

This is meaningful shared-runtime work, not an art swap. First prove mechanics with temporary assets. Do not generate finished multipart art against an untested layout or claim that adding `composite` metadata disables weapons automatically.

## 3. Quest, gate and save contracts

`BASELINE.json` preserves every encounter victory/defeat listener and its conditions/actions. `PLAN.json` preserves each affected room's gate definitions and instance rewards. Implementation must check the following explicitly:

| Area | Contract |
| --- | --- |
| W1 opening and training | Loader north gate, workshop kit/suit sequence, tutorial-off play, Guard Break drill identity and both legacy drill locations. Legacy listeners alone do not justify creating another drill. |
| W1 mine/launch | First Borer and Bulwark tasks, Cargo Lift gauntlet, Buoy checkpoint credit, Warden victory recorded before or after its quest stage, Pod Bay hatch. |
| W2 new local species | Update the cave combat gate and canopy-clearing recognition for Moss Borer without allowing any unrelated creature victory to clear it. Preserve old Echo-Borer cleared receipts and reduced early creature instances. |
| W2 Beast | Spawned through Face the Beast, complete only after its actual final state, then retain Gh0st/Anchor story progression. |
| W3 | Sewer landing credit, guaranteed Neon Sign Core, Lens/assassin story, rooftop prelude versus Administrator completion, shield/launch route. Interdictor death cannot complete Administrator victory. |
| W4 | Slag River east gate, Phantom/Springs/Assembly progression, Forge/Anvil acquisition, stolen hardware, Titan north gate and Meltdown stage. Neither a weapon nor an optional Champion can count as Titan. |
| W4 Crucible | Three distinct round receipts and final claim. Retrying/reloading cannot restart a cleared round for rewards. Champion-only finale keeps its existing story completion. |
| W5 | Fighter-screen arrival, actual Security Hub squad, Gamma-only Firewall completion and bidirectional Anchor access, earned Stasis, hunter disable/pursuit state, Avatar defeat after the Soloist story beat. |
| W5 duplicate boss replacement | Boardroom Bailiff has its own ID and receipt. Old Boardroom Grave victories must not grant the new elite again or count as a fresh main Avatar kill. Reconcile any old main-quest victory already credited rather than taking completed progress away. |
| W6 rescues | Rescue elites occur only in their actual rescue arenas; the three new optional fragments cannot grant crew-rescue credit. Arrival loadouts reflect whichever companions have actually rejoined, including different rescue orders. |
| W6 conclusion | Human and Silence receipts, stage-one partial legacy saves, old Silence Pulpit location, final tuning focus, ending travel and one-time reward claims. |

Use stable authored encounter IDs going forward. Migration must recognize existing `encounter_cleared:<sorted enemy ids>` room receipts and map them to new identities:

- Retained/remixed fight: preserve its cleared status rather than repopulating a familiar room because one ally was added.
- Merged fights: if either source was already cleared, do not force the new combined fight on a returning player. Respect the stronger prior completion; do not refill migrated reward entitlements.
- Replaced enemy: aliases recognize the original room-scoped receipt. A species rename must not resurrect cleared enemies.
- Retired fight: keep old completion history, remove its presence, grant no retroactive refund or bonus automatically.
- New boss partner/part: inherits the parent encounter's completion and reward receipts; never appears beside an already defeated core on a legacy save.
- Pending in-combat save: resume the original supported descriptor for that encounter or perform an explicit safe migration to its recorded entry checkpoint. Never silently mix old HP/turn order with a new five-actor lineup.

Migration tests must distinguish partially cleared multi-fight rooms from fully untouched ones. The permissive merge policy is intentional: a legacy player keeps progress rather than being forced to re-earn the redesign.

## 4. Encounter budgets and materials

Before assigning final stats, record the actual runtime-resolved rewards for each baseline encounter, including progression modifiers and room overrides. Sum them on the earned mandatory and completionist routes separately. `xp_reward` fields alone do not establish final shared-crew awards.

- **Mandatory route:** preserve its intended level/AP/equipment arrival envelope. Redistribute budgets among the fewer retained mandatory formations after measuring the route; do not multiply enemy rewards or pay for required progression with optional grinding.
- **Optional route:** fewer repeated solos means less filler XP. Set useful rewards on the retained mastery fights deliberately; do not blindly transfer all removed XP into an easy chest or silently erase access to systems materials.
- **Components/reinforcements:** one parent encounter budget, no independent XP, credits, AP or drop rolls. Retry/respawn cannot farm components.
- **Merged formations:** consume one formation receipt. Guaranteed supplies from their baseline sources are allocated once, not once per member or once per legacy source ID.
- **Chance drops:** keep as a bonus. Never make a story-critical item or the only necessary counter depend on them.

Guaranteed supply anchors retained in this candidate:

| World | Location | Preserve |
| --- | --- | --- |
| 1 | Security Post | Wiring Bundle from the teaching Bulwark instance. |
| 1 | Pod Bay | Warden keycard and current boss reward entitlement. |
| 2 | Resonant Sedge / Spore Thickets | Guaranteed Herbs at their existing fights. |
| 2 | Hound Nest | One guaranteed Beast Meat, not one per hound. |
| 2 | Left Reliquary / T-Junction Wiring / High Gantry | Scrap Metal / Wiring Bundle / Battery. |
| 2 | Canopy Ridge | Source Resin after actual Beast victory. |
| 3 | Private Booth | Neon Sign Core, guaranteed, once. |
| 3 | Service Dumbwaiter / Fuel Bridge / Drone Wreck Roof | Circuit Board / Wiring Bundle / two Ration Packs. |
| 4 | Coolant Spillway / Foundry Defect Choir / Engine Service Ring | Two Pure Iron / Circuit Board / two Ration Packs. |
| 3–5 | Main bosses | Existing Power Cell supplies and progression awards once per completed parent encounter. |

All other existing base and instance guarantees are retained in the baseline snapshot for reconciliation. No removed row in this candidate carries a guaranteed instance material. The validator checks that statement, including merges, instead of relying on this abbreviated table.

Do not modify recipe costs, gear stats, Charles's newly repaired favor, or fishing/cooking mechanics as a side effect. If material availability proves insufficient, make a separate, explicit allocation decision at a named retained encounter or existing salvage interaction.

## 5. Art and sound production

The roster contains 69 distinct combat subjects: 48 existing definitions and 21 proposed new definitions. Eight of the new definitions are linked boss parts, a partner or a reserve enemy; the other 13 fill world-specific regular/elite roles. The roster is a scope estimate, not a requirement to generate 69 new files.

1. Review existing dedicated art at actual phone size. Keep suitable unique sprites; a wrong binding does not automatically require regenerating a good existing image.
2. Approve the silhouette sheet per world. Related machines may share materials and insignia, but not the same body with a new tint. Source echoes need their own composition even when they recall an earlier enemy.
3. Approve a whole-machine sketch for Titan and the final transformed composition. Freeze relative scale, part anchors, common perspective, overlap order and target hit areas before generating pieces.
4. Generate complete isolated subjects in the established bold cel-shaded adventure style. Use the current image-production workflow and project references; reconcile the older guide's output folder and transparency pipeline with current `world_assets` packaging rather than copying stale commands.
5. Derive exploration presentation, targeting portraits and bestiary crops from the same approved enemy source. An old inventory icon is never an exploration combat sprite fallback.
6. Supply damaged/deactivated states for parts, readable elite poses and nonambiguous phase transitions. Normal enemies need not each get a full hand-drawn animation set; controlled motion/overlays can carry simple tells.
7. Validate alpha, bounds, mobile silhouette, relative size, sprite path, duplicate image hashes and visual similarity. Different filenames do not establish different art.
8. New music or sound effects use ElevenLabs with the existing `elevenlabs_api_key.txt` credential kept private. Existing suitable cues may be reused intentionally. No new audio is generated during planning.

The enemy-art rule applies to each **kind**, not each repeated instance. Two Shard-Hounds in one authored pack share their own Shard-Hound art. Warden, Titan, Avatar and Vale's forms must all have distinct bodies.

## 6. Prototype gates and rollout

### A — Shared mechanics proof, no final art

Build one representative squad (Welder + Magma Drone), one declared-windup elite, one finite-reserve deployment and the five-target Titan layout using temporary art. Verify target selection with touch and keyboard, duplicate instances, area damage, disable/cancel, KO allies, UI scaling and rewards. This is the point to revise scope if five readable targets do not fit the phone.

### B — World 1

Keep its 11 opportunities. Apply the Warden behavior/presentation and verify the opening, tutorial-off path and earned escape resources. Do not disturb the existing recovery and crafting teaching sequence.

### C — World 2

Adopt the local species, one richer laboratory formation and Beast behavior changes. Verify early reduced instances, cave gate, every companion arrival order supported by the story, and guaranteed supplies.

### D — World 3

Introduce actual city formation support, deploy the previously unused Riot Guard appropriately, and implement bounded Administrator reserves. Recheck the two rooftop fights as a sequence, including a less-combat-heavy arrival save.

### E — World 4

Reduce repeated solos according to the route. Introduce Quench Mite and Magnet Sifter before their combinations. Keep the repaired cooling/finite-welding behavior. Ship Titan only after the multipart proof passes; keep Champion's arena identity distinct. Validate hardware acquisition and the escape end to end.

### F — World 5

Reduce density, preserve story/rest rooms, introduce each squad unit before its advanced formation, and make the Hunter-Killer a coherent one-hunter pursuit. Replace the optional Avatar copy, then implement the main duet. Verify early fighter-screen tuning, Gamma progression and all existing supply/favor routes.

### G — World 6

Differentiate optional nightmare fragments from rescue elites, test rescue-order loadouts and memory-wave pressure, then implement the linked finale last. Legacy stage checkpoints and postbattle ending traversal are release requirements.

### H — Final art and earned-route acceptance per world

Art may proceed in world batches once that world's roles/layouts are stable. Update exploration tells, bestiary information, loot descriptions and the completionist walkthrough together with the tested game build. Preserve reader checklist IDs. Publish no guide instruction for an unshipped interaction.

## 7. Acceptance evidence

- Run earned main-story and completionist routes with ordinary equipment, not only optimized builds. Measure starting/ending HP, KO, consumables, AP/XP, material ownership and checkpoint affordability.
- Sample multiple seeds and target-order policies. Compare body-first, support-first, interrupt and defensive strategies where the design promises them. One seed or one optimal policy is insufficient.
- Measure mandatory-chain attrition separately from fresh-fight wins. A fair isolated squad can still be exhausting after three other fights.
- Treat duration as feedback, not a universal pass/fail timer. Track ordinary fights, elites and climaxes separately; investigate idle time, repeated recovery stalls and unanswerable burst.
- Confirm that every declared dangerous action offers a real response opportunity at the expected crew speed. Animation-only windups cannot substitute for usable response time.
- Test every quest/gate receipt before/after defeat, retreat and reload, including partially cleared legacy rooms. Final blow, damage decals and collapse must finish before victory UI.
- Phone acceptance includes large fonts, typical resolution, target labels, all five Titan targets, composites with effects, and the full final-stage transition. Desktop parity includes keyboard input and target focus after an actor disappears.
- Keep performance, readability, progression correctness and subjective fun as separate findings. Automated tests do not establish that a blind player understood the tell or enjoyed the encounter.

## Decisions retained for later playtests

Exact HP/damage, reserve timing, phase thresholds, reward redistribution and animation counts remain prototype tuning work. The packet fixes the intended decisions, locations, complete formations and contract requirements now; it does not invent balance certainty. The Astra bunk-corridor discussion remains separate and untouched.
