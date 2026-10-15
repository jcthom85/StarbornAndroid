# Astra design review — October 2026

The five-room footprint is sufficient. Astra needs more depth in existing facilities before more rooms. Its compact Common Room connects every destination, which keeps visits quick.

## Current implementation

| Room | Working purpose | Main gap |
| --- | --- | --- |
| Bridge | Navigation and travel; Zeke presence | The Chime is an inspection rather than a browsable archive. |
| Common Room | Cooking, recovered films, restored arcade cabinets, prize shop, Orion/Gh0st presence | The relic array and central table describe future/social functions without corresponding systems. Six arcade cabinets also make this room crowded. |
| Living Quarters | Rest, Oliver, personal keepsakes | More decoration than evolving personal interaction. |
| Cargo Bay | Tinkering, disembarkation, newly added Catch Log terminal | Storage is explicitly offline. |
| Simulation Deck | Training, boss programs, challenge encounters | Catalogs expose late-game bosses and encounters without visible progression gates. |

Earlier overview/functionality documents describe plans, not proof of implemented storage, relic synchronization, changing crew dialogue, or repair gates. Room assets and runtime actions are the basis of this review.

## Implemented in this pass

The Cargo Bay has a repeatable Catch Log terminal linked in the room description and available through room actions. It reads persistent catch records when opened. Android and desktop Journal no longer contain fishing records. Fishing screens retain access for practical reference. No new rooms or collection mechanics were added.

## Recommended priorities

1. **Make the existing promises pay off.** Implement relic synchronization when its progression rules are ready, or replace its future-tense placeholder with honest inspection copy. Decide whether storage solves a real inventory problem before implementing a bank.
2. **Let the crew acknowledge progress.** Give the Common Room a small number of authored conversations after major milestones, and Quarters short personal follow-ups. Signal new conversations once; avoid a mandatory circuit through every room after every quest.
3. **Make the Simulation Deck respect discovery.** Keep basic practice available. Gate boss archives behind defeated bosses and challenges behind suitable progress. Show concise encounter purpose and reward rules before launch.
4. **Give the Bridge useful information.** Docked location, reachable destinations, and any actual travel blocker belong here. A Chime archive could replay already encountered logs. Avoid duplicating the full quest journal or exposing future story information.
5. **Make recovery visible.** Extend existing arcade restoration with selective, authored room-description/background changes after ship repairs. Prefer meaningful changes over new maintenance chores.

## Rooms and activities

Do not add a separate fish room, trophy hall, shop, or additional crafting station now. The Catch Log strengthens Cargo, while existing films and arcade restoration already provide optional return activities.

An Engine Room is the strongest possible future addition **if** it supports an authored repair/upgrade quest and makes the ship's changing condition tangible. An Observatory would need a distinct story or discovery function to justify itself. Neither should be added as an empty room containing another menu.

Astra should offer three reasons to return: recover and prepare, reconnect with the crew, and revisit discoveries. The next investment should be changing crew interaction and fulfilled facility functions, not room count.

## Follow-up implementation

The original gaps above describe the state before this follow-up. Android and desktop now share these additions:

- **Cargo Manifest:** the existing storage terminal lists owned supplies. The inventory has no capacity limit, so this is a read-only manifest of shared supplies, not a second bank.
- **Relic Array:** the existing array catalogs owned recovered artifacts. It does not promise or grant unsupported character synchronization. Source abilities retain their authored story unlocks.
- **Crew Check-in / Crew Downtime:** the central table and bunk corridor offer short, authored conversations that change with campaign progress. Only recruited companions appear. Existing persistent room flags mark check-ins read; a new chapter makes them new again.
- **Chime Archive:** discovered signal notes become available through the Chime. Future story notes stay hidden. These are readable summaries, not replayable audio recordings.
- **Simulation Deck:** basic practice remains available. Boss archives require their campaign victory, task, or completion record; challenge programs require corresponding progression. Both the UI and launch method enforce availability. The UI accurately states the existing behavior: standard combat rewards, damage, and consumable costs apply. This supersedes the earlier design document's unimplemented no-XP plan.
- **Navigation:** the Bridge explains how the next route becomes available, and explicitly names the travel restriction in the BurgQuest demo. Desktop destinations also show their descriptions.
- **Ship recovery:** Bridge/Cargo descriptions acknowledge the recovered Deep-Core engine; Quarters acknowledge later repairs. Existing arcade background progression remains intact. No new room artwork or rooms were added.
- **Weapon onboarding:** modern and legacy equipment tutorials name Nova's **Mining Pistol**, matching the inventory. Its item description clarifies that it is a phase-cutter sidearm; authored cutter story references remain valid.

Next work should be richer authored character scenes and, if warranted by progression design, actual relic synchronization. The ship no longer advertises an offline bank as a forthcoming necessity.

## Room organization follow-up

The five rooms now have two or three authored room interactions each. Movement and crew NPC interactions remain separate from that budget.

| Room | Room interactions |
| --- | --- |
| Bridge | Navigation console; Chime archive |
| Common Room | Crew table; galley cooking |
| Quarters | Rest; crew downtime (including personal keepsakes) |
| Cargo Bay | Workbench; Cargo Records; disembark |
| Simulation Deck | Training console; Recreation |

Cargo Records groups Supplies, Catch Log, and Relics as tabs. Recreation groups restored arcade cabinets, Great Frontier films, and arcade prizes. It preserves cabinet installation checks, introductory installation messages, saved scores, and prize-shop rules, including demo restrictions. The Common Room no longer changes into an arcade room; arcade restoration description variants now belong to the Simulation Deck. Its current training artwork is retained, so dedicated recreation artwork is a possible later art pass rather than part of this change.
