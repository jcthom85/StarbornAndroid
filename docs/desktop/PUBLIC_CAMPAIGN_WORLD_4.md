# World 4 public campaign route

Worlds 1-4 now finish without hidden action calls, room-position shortcuts, or synthetic victories in the test driver. Strict checks remain active until World 5.

## Route coverage

- Slag crossing, cooling-vent controls, service airlock, Project Phantom terminal, and cooling springs are visited before their visible actions are selected.
- Conveyor navigation uses the belt timing console. The conditioning records and fabrication matrix are selected in their authored rooms.
- The Anvil sequence visits the pulse board, grease marks, override paddle, and breath pistons before opening its cradle.
- The party rests at the visible forge control-alcove cot, confronts Rylos at the Titan Dock, and defeats the actual Titan Walker through production combat.
- Escape is tested before theft: it must leave the party at the dock, keep MQ20 active, and leave World 5 locked. The route then collects the engine and arrays from the power core, verifies possession, and launches from the dock.
- The World 4 save/reload checkpoint preserves the resulting session before the campaign continues through the finale.

## Results

Seven focused tests pass: four campaign/gear cases and three provision-source cases. These include all 30 main quests, six-world save/reload checkpoints, the unassisted opening, and public fishing/procurement/crafting/cooking.

| Six-world assistance | Before this pass | Current |
| --- | ---: | ---: |
| Room-position shortcuts | 0 | 0 |
| Hidden action calls | 28 | 14 |
| Synthetic victories | 12 | 11 |

All remaining assistance occurs in Worlds 5-6. The seeded Titan trial records 43 damage events, 600 XP, 3 AP, 400 credits, no item use, and no knocked-out party members. It follows rest and uses available combat abilities; it is not an all-seed balance result.

Reports are regenerated under `app/build/reports/campaign-systems/`, including `public-worlds1234-combat.txt`, `encounter-titan_walker_boss-before.txt`, and `assistance.txt`.

## Content observation and limits

The authored `conditioning records` action fires `w4_mq18_defeat_prototypes`, which directly completes the prototype-defeat objective without an encounter. This pass verifies that visible action and its progression, not a prototype-wave battle. The mismatch between the objective and interaction should be reviewed as a separate content correction.

This pass changes tests and documentation. It retains the existing starter-component fixture, production bootstrap equipment, seed 17, virtual time, authored answers, disabled tutorials, and animation callbacks. Physical controls, human traversal time, and broad difficulty remain outside this route test. Earlier combat and economy observations remain historical snapshots.
