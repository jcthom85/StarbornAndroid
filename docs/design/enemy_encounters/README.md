# Enemy and encounter redesign — proposal and implementation progress

Prepared November 12, 2026, from the working tree, including preceding playtest fixes. The original proposal and source snapshot remain frozen below. The shared mechanics prototype, bounded World 1 Warden pass and World 2 mechanics pass are now implemented locally; see [implementation progress](PROGRESS.md). Other world redesigns and final art remain proposed.

## Agreed direction

- Mostly world-specific enemies, with deliberate story connections across worlds.
- A distinct approved visual identity for every enemy kind, including bosses, targetable components and summoned enemy kinds. Repeated instances of one kind may use its same sprite.
- Nearly all encounters individually composed and placed; no random encounter table is introduced.
- Boss variety: disciplined solo opponents, behavioral phases, finite reinforcements, independently destructible weapons, coordinated opponents and a staged final transformation.
- Elites need a signature decision, readable presentation and a worthwhile purpose or reward.
- Combat should connect to story, exploration, equipment and useful materials. Exit gates are one tool, not the sole incentive.

The detailed roster, formations and mechanics below are the first complete design candidate for that direction. Names and timing thresholds remain editable during prototypes. Art production starts after the relevant behavior and silhouette brief are accepted in a playable test.

## Read the packet

1. [Boss designs](BOSS_DESIGNS.md): the six main climaxes, the Crucible Champion, phase rules, counterplay and targetable parts.
2. [World rosters](ROSTER.md): all 48 existing definitions receive a world and disposition; 21 proposed definitions fill specific jobs, including boss components. Total: 69 distinct combat subjects, not 69 automatically commissioned new images.
3. [Encounter route](ENCOUNTER_ROUTE.md): a decision for every one of the 131 campaign encounter sources, plus the relocated final stage. Every retained fight has a purpose; every removed or merged fight has a destination or pacing reason.
4. [Implementation and acceptance](IMPLEMENTATION.md): runtime gaps, progression and save contracts, reward budgeting, art delivery and playtest sequence.
5. [Source snapshot](BASELINE.json) and [structured proposal](PLAN.json): exact source identities, current enemy definitions, current victory listeners and machine-checkable planning data.
6. [Art manifest](ART_MANIFEST.json): every subject's candidate image, distinct proposed destination and required state treatments. This is a production checklist, not a generation request.

Run `python3 docs/design/enemy_encounters/validate_plan.py` to check source coverage, definitions, world assignments, merge destinations, supply guarantees, gate retention and the source snapshot. Known implemented source changes are recorded with exact hashes and reasons in [source reconciliation](SOURCE_RECONCILIATION.json); the original snapshot is retained. This validates the planning packet, not runtime fun or balance.

## Campaign shape

Counts are distinct authored encounter opportunities, not required battles. The baseline includes static formations, four event-spawned fights and the one campaign patrol. Debug encounters, simulations and legacy listeners that do not establish a current spawn are excluded. A linked boss stage is counted as a stage, not falsely advertised as another ordinary encounter.

| World | Current opportunities | Proposed opportunities/stages | Proposed enemy kinds | Intended rhythm |
| --- | ---: | ---: | ---: | --- |
| 1 — The Pit | 11 | 11 | 8 | Retain the teaching sequence and varied pairs; polish the solo Warden. |
| 2 — Sector 9 | 11 | 11 | 9 | Ecology, pairs, ancient defenses and one large phased creature. |
| 3 — The Spire | 9 | 9 | 9 | Scavengers, specialized city squads, optional elites and a reserve-limited Administrator. |
| 4 — The Foundry | 41 | 27 | 14 | Clear introductions, machinery combinations, genuine quiet stretches, arena contrasts, multipart Titan. |
| 5 — The Ring | 42 | 25 | 14 | Scanning/capture formations, fewer interchangeable officers, one hunter, one unique Avatar climax. |
| 6 — The Source | 17 | 15 | 15 | Distinct rescue encounters, introductory fragments, memory defense and linked final forms. |
| **Total** | **131** | **98** | **69** | Fewer repeated opportunities; richer individual compositions. |

These counts are consequences of room-by-room choices, not quotas. Do not pad a world to meet a roster size or reduce an otherwise good fight merely to hit 98. New combinations require fresh encounter budgets; increasing enemy count must not automatically multiply damage, XP or loot.

## World identity and recurring families

| World | Visual language | Combat identity | Connection allowed |
| --- | --- | --- | --- |
| Pit | Rust, work lights, exposed industrial mechanisms, burrowing life | Read a fault, interrupt a charge, break protection | Dominion equipment provides a recognizable engineering lineage later. |
| Sector 9 | Rooted organisms, mineral growth, weathered Architect machinery | Packs, symbiosis and biological commitment/recovery | Moss Borer and Mist Glider suggest related ecology through anatomy, but are distinct designs and definitions. |
| Spire | Compact city hardware, tailored enforcement, polished concierge devices | Marks, shielding, evasive commitment and controlled squad support | Foundry Phantom remains related to Gh0st through story; city and orbital enforcement share insignia, not bodies. |
| Foundry | Furnace ceramics, heat shielding, cooling sacs, heavy presses | Heat/cooling cycles, finite repairs and machinery coordination | Industrial construction is recognizable, but Golem, Welder, Crusher, Titan and Champion have different silhouettes. |
| Ring | Ivory precision, calipers, capture cables, hard-light command geometry | Designation, alignment, gravity tethers and coordinated orders | Threshold Remnant foreshadows the Source without reusing Source Shadow art or its quest-sensitive ID. |
| Source | Broken memory media, impossible anatomy, missing centers, fractured porcelain | Interrupted commands, reflected relationships, memory pressure | A memory may recall a prior enemy's features while getting its own composition, silhouette and behavior. |

No cross-world shared enemy ID is required by this candidate. Recurrence is expressed through related designs and story, leaving room to approve an explicit returning individual later. This respects the desired exceptions without manufacturing mandatory repeat appearances.

## Encounter rules

1. **One primary question, sometimes a secondary complication.** Each route row names its question. A different room name alone does not make an identical battle new.
2. **Show the setup before its payoff.** Use a short intent label and a visible pose/charge/target marker. Color and sound supplement readable text; neither is the only clue.
3. **Support has limits.** Repairs, reinforcements and disabling effects have explicit charges, caps or recovery. No infinite repair loops, hidden summons or repeated hard-control chains.
4. **Respect the available crew.** Solo Nova must be able to answer the opening and rescue fights with mandatory tools. Never require the character being rescued to counter their own captor.
5. **Optional upgrades improve options.** Scan, elemental gear, interrupts and status builds should help; an optional schematic, talent or random drop cannot secretly become the only viable answer.
6. **Choose pressure across the whole formation.** Offset coordinated windups. A boss plus four components is not permission for five full-speed attack streams.
7. **Keep exploration meaningful.** Quiet memorials, shops, rest stops and story revelations do not need interchangeable guards. No new automatic encounter on room entry is proposed.
8. **Retain recovery and withdrawal.** Existing combat gates and backward exits remain; a boss rematch restores a known starting state rather than endlessly eroding an unwinnable save.
9. **Pay for a meaningful success.** Guaranteed crafting supplies stay guaranteed at named opportunities. Common drop rolls are supplementary; new components and reinforcements do not become farms.

## Production order

Freeze the high-level boss lineup and counterplay, then prototype the runtime gaps with temporary existing sprites. Implement and playtest one world at a time, updating its route and earned-resource evidence before moving on. Create final art in world batches after mobile target readability and mechanics are established. Bosses with components get one assembled visual plan before individual pieces are generated.

World 1 remains the conservative reference; World 4 supplies the first multipart proof. World 5's density reduction and final-boss continuity receive their own acceptance checks. Planning all six worlds now does not authorize claiming that any of those runtime changes are already finished.
