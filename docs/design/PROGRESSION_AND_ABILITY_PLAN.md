# Progression and ability plan

October 19 update: the owner approved proceeding with the recommended crew structure. See [production adoption](../testing/CREW_PRODUCTION_ADOPTION_2026-10-19.md) for implementation, compensation policy, current verification and remaining manual gates. The plan below records the earlier proposal and historical findings.

October 14, 2026. **Plan with an opt-in Nova prototype implemented.** See the [implementation and verification record](../testing/NOVA_PROTOTYPE_IMPLEMENTATION.md). The remaining crew redesign, production migration and broad balance testing are still pending. Disabling legacy nodes and refunding AP has not been applied to production saves.

## Outcome

A player should be able to earn, understand, purchase, equip, use, save, and carry forward a build whose abilities do exactly what their descriptions promise. Each crew member needs useful choices without mandatory grinding, duplicate purchases, dead prerequisites, or one dominant combination.

Work from current production behavior and earned campaign states. Preserve story causality and the four existing crew identities. Old proposed kits are not replacement specifications. Human balance/readability testing follows executable contracts and automated evidence.

## What the audit establishes

The [source baseline](PROGRESSION_ABILITY_BASELINE_2026-10-14.json) preserves all 100 tree nodes, all 43 skill records owned by the four active crew, AP reward records, and inspected source hashes. Ollie's retired companion tree is excluded. This is a screening inventory, not proof that every cataloged skill works.

| Current finding | Implication |
|---|---|
| 15 tree nodes match a combat skill definition; 30 more are passive-stat consumer candidates | These 45 require behavior verification, including units, targeting, ownership and acquisition. Catalog presence is insufficient. |
| 48 other nodes have no matching combat definition; seven passive promises lack a complete matching consumer | Review all 55 individually. Check exploration consumers before deciding to implement, replace or retire an entry. |
| Nova/Zeke trees cost 66 AP each; Orion/Gh0st cost 46 each: 224 AP total | Tree size and AP income must be designed together. The objective is meaningful builds, not purchasing every node in a first run. |
| Only five enemy records award AP: Administrator 2, Titan 3, Avatar 4, Vale 5, God 8 | These records total 22 AP for one defeat of each. This is catalog arithmetic, not a certified route budget; trace encounter frequency, event rewards and grant eligibility before adopting it. The inspected quest/event/dialogue catalogs contain no `ap` reward entries. |
| Tier requirements use row × 5, reaching 25 AP in a single character's tree | The current five-boss total alone cannot open the highest row in a fresh first run. Shared AP also creates competition between crew members. |
| Twelve skills are granted at levels 6/9/12; eleven also appear in the trees | Acquisition and tree investment overlap. A level grant can also count as AP investment because current investment is inferred from unlocked IDs. |
| Smoke Bomb has an implemented active +15 evasion effect for two turns, but its tree record is also treated as a permanent +10 evasion passive | Active skills and permanent passives need distinct effect identities. A working special case does not validate every consequence of its tree record. |
| Party/status/details use base HP; combat and rest use `PartyHealth.maxHp` | The same build can display different HP ceilings. Stat presentation must share the runtime calculation. |
| NG+ omits unlocked skills and unspent AP; learned schematics also reset | Skills contradict the current title-menu promise. AP and schematic carryover require an explicit contract. |
| The existing Android NG+ contract test reconstructs its own transition | Replace the imitation with production bootstrap/service calls and actual save/reload verification. |

The [completed regression review](../testing/REGRESSION_REVIEW_2026-10-14.md) provides the starting automated baseline: 652 Android-hosted and 147 desktop tests passed. It does not certify the untested progression promises above.

## Proposed progression model

### Keep core competence separate from optional specialization

Starter actions, level-earned core skills, and story-earned Source arts establish each character's usable kit. AP should buy a deliberate specialization or a distinct optional ability. A player should not pay AP for an ability they already own, and a free grant should not masquerade as spent AP.

Keep a single acquisition policy per ability. If an AP purchase can unlock a skill earlier than its later level grant, explicitly support that exception and define its later refund or conversion. The simpler recommended first version keeps level/story grants outside AP purchase paths.

Use explicit purchase history for new investments. Distinguish learned abilities, purchased nodes, spent AP and story prerequisites. Existing `unlockedSkills` conflates these, so it cannot reliably establish how much a player paid.

Do not preserve 100 purchases merely to preserve tree size. Start with two distinct build directions per character; add more only when they produce different decisions. Exact node counts, costs and prerequisites come after the AP budget and capability audit.

### Character directions to develop

These are proposed emphases using current kit behavior, not assertions that new abilities exist.

| Crew | Direction A | Direction B | Design boundary |
|---|---|---|---|
| Nova | Equipment sabotage: expose Guard, set up Brittle and exploit openings | Aggressive improvisation: pressure groups, choose damage against control | Mechanical competence; avoid a second unrelated shadow-magic or theft subsystem just to justify old nodes. |
| Zeke | Reliable protection and survival under pressure | Disruption and exploiting compromised enemies | Protection must show who is protected and how. Procedure-flavored names need concrete effects rather than an undefined morale stat. |
| Orion | Sustain and planned recovery | Field control and careful setup | Healing, regeneration, revival and prevention are different mechanics. Select a small complementary set and prove targeting/resource limits. |
| Gh0st | Precision and finishing exposed targets | Disabling priority threats and creating safe windows | Countering, interception and invisibility require actual state and targeting rules; they cannot be inferred from a name. |

Every direction needs an early useful choice, a mid-campaign payoff and a later refinement. No branch should require another character to make a basic purchase useful. Crew combinations should add opportunities without making one party composition or control loop compulsory.

### A contract for every ability

For each retained or proposed ability, record:

- Owner, acquisition source and prerequisite; whether it is an active, permanent passive, conditional passive or exploration interaction.
- Exact target, magnitude, units, duration, cooldown, action cost and resource cost. Define whether a percentage is additive percentage points or a multiplier.
- Runtime consumer and player-visible feedback; behavior when a target is immune, defeated, unavailable or already affected.
- Stacking, refresh and expiry rules; interaction with equipment, meals, snacks, Break, statuses and story arts.
- Purchase/reload/NG+ behavior and at least one test that demonstrates the advertised consequence through public actions.

Use typed supported effects. Reject unrecognized effect types at validation/purchase boundaries; do not silently accept and spend AP. Keep renderer descriptions and runtime parameters connected. New subsystems such as theft, morale, teleportation or recruitment need a separate demonstrated gameplay purpose before implementation.

## Balanced AP pacing

First capture AP earned, spent and available at each world arrival, major boss, recruitment and departure through public campaign actions. Include minimal-route, ordinary optional-content and completionist variants. Credit no debug grants or repeated unique rewards. Count AP available before the final encounter separately from victory rewards.

Then choose costs and income together. Recommended targets:

- Every recruited character can receive a useful specialization within their first world of participation, without requiring a missed side quest or replaying encounters.
- A normal route can develop one direction for each active crew member, with some cross-branch choices. A completionist route offers extra flexibility rather than being required for viability.
- Early experimentation is inexpensive; later power costs more because of its actual consequence, not its visual row.
- Every prerequisite chain is satisfiable using legitimate AP available at the intended campaign point. Validate both concentrating AP and spreading it across the crew.
- Optional rewards and NG+ extend choices without trivializing a first run or creating an infinite AP farming loop.

Keep the current shared AP pool for the initial design to avoid introducing another currency. Reconsider per-character AP only if earned-route evidence shows shared AP consistently starves late recruits. Decide income placement after measuring routes; do not increase every enemy reward or inflate costs globally.

Do not choose final AP numbers from the 22-point screening total alone. The next design artifact must contain an actual world-by-world budget and a candidate purchase path for each crew member.

## HP, stat information and NG+

Use a shared derived-stat result for combat, party summaries, character details, rest/healing and equipment previews. Include base stats, equipment/mods, permanent passives and active meals once each. Clearly label base values if shown separately. Show maximum and current HP consistently, refresh an open details panel when a loadout changes, and define clamping when a temporary bonus expires without granting free healing. Preview proposed equipment without mutating the session.

Proposed NG+ contract:

| State | Recommendation |
|---|---|
| Crew, levels/XP, gear, inventory and credits | Preserve the existing promised policy, including current credit-floor behavior until deliberately changed. |
| Learned abilities, purchased nodes, unspent AP and permanent schematics | Preserve earned build progress. Apply any approved node migration exactly once. |
| Story quests, room/event state and access milestones | Reset and start the actual opening route. Retain only the Master Protocol marker and explicitly defined NG+ access. |
| HP and temporary effects | Start at derived full HP; reset meals and battle state. Document this separately from permanent progression. |
| Story arts learned in the previous run | Recommend retaining them under the existing "skills carry over" promise. Do not carry quest flags with them. Check that early abilities cannot skip required events or soft-lock the replay. |
| Bestiary, fishing and arcade records | Decide individually before implementation; these are records, rewards and access state with different duplication risks. Do not assume all should reset or carry as one group. |

Carryover must run through actual Android/desktop startup and save APIs. A production bootstrap test is necessary but insufficient for service-level persistence.

## Save compatibility and respec

Prepare an old-to-new node map before changing catalogs. For each old ID choose: retain exact behavior, convert with equivalent progress, or retire with a stated refund. Preserve recognizable ability IDs wherever possible. Never reinterpret an existing investment as a weaker unrelated effect without an explicit decision.

Because old saves lack purchase history, distinguish provable purchases from ambiguous level/story grants. Audit grant sources before calculating refunds; never blindly sum all unlocked node costs. For ambiguous changed trees, a one-time full affected-tree respec with preserved core abilities is preferable to pretending the paid amount is known. Agree the compensation rule before rollout.

Make migration versioned and idempotent. Saving/reloading, a second migration pass, NG+ and battle checkpoints must not duplicate AP or remove retained abilities. Check manual, auto, quick and backup saves. Recommend free respec during development and at the migration; a permanent in-game respec policy is a separate decision.

## Execution order and acceptance gates

| Phase | Concrete output | Gate before the next phase |
|---|---|---|
| 1. Complete evidence | Expand the baseline into per-ability executable contracts for all 100 tree entries and 43 crew skill records; trace grant sources and produce earned AP budgets. Include stat, story-passive and snack interactions. | No entry classified as working solely because a catalog ID exists. Every unsupported promise has a disposition candidate. |
| 2. Review the design | Proposed retained/new trees, acquisition policy, exact effect descriptions, world-by-world AP budgets, reachable build examples and migration map. | Owner chooses character directions, capability scope, pacing targets and compensation policy. This is the principal design decision. |
| 3. Repair information and carryover | Shared derived HP/stat presentation; production NG+ tests and the agreed field-by-field carryover policy. | Same saved loadout agrees across combat/field/recovery/preview; actual NG+ start and save/reload preserve the agreed build. |
| 4. Implement one complete build slice | One character with two coherent directions, real effects, public purchases, UI feedback and migration support; representative partner interactions. Nova is the recommended pilot because she is present from the opening. | Earned AP reaches the intended purchases; a purchased active and passive both produce demonstrable consequences; old saves remain valid. No synthetic unlocks substitute for purchase tests. |
| 5. Complete the other crew | Apply proven infrastructure to Zeke, Orion and Gh0st, adding mechanics only where their contracts require them. | Each direction is reachable and distinct; late-recruit AP and cross-crew status/control interactions pass. |
| 6. Automated balance | Compare earned builds against representative normal/group/elite/boss encounters and consecutive routes across all six worlds. | Published wins, losses, timeouts, durations, item use, HP/resource pressure and control uptime; no required grind or repeatable dominant lock loop. Select acceptance bands before running candidates. |
| 7. Player acceptance | Short guided checks for build comprehension and feedback, then sustained earned-route play and save/recovery on both platforms. | Players understand purchases and can use the builds; touch/mouse/large-text presentation and route balance meet the reviewed targets. |

Implementation should proceed in these bounded slices. Run relevant regressions after each behavior change, then full suites at integration gates. Reuse existing public campaign, combat and save harnesses; record fixture limitations and avoid claiming synthetic loadouts prove affordability.

## Immediate next deliverable

[Candidate 1](PROGRESSION_DESIGN_PACKET_CANDIDATE_1.md) now proposes 32 purchases, exact effect contracts, a 48-AP pre-finale main-route budget, purchase-path examples and migration rules. Its data includes dispositions for all 100 old nodes and acquisition screening for all 43 crew skill records. These are reviewed-source proposals with checked arithmetic, not executed route/balance evidence. Complete the remaining consumer/acquisition tracing and review the design before replacing or disabling production nodes. The small HP/NG+ edits begun during the initial audit were set aside when the owner requested planning; this pass changes documentation only.
