# Public campaign routes: Worlds 1-2

This pass strengthens the shared-runtime campaign tests used for Android and desktop. It changes test drivers, not game balance or production content.

## Coverage added

- A separate opening test completes World 1 MQ01 with no fallback actions: public movement, Jed's dialogue, equipment controls, real loader combat, the Field Kit crafting bench, quest completion, and save/reload in a recreated harness.
- Jed's starter kit is granted once by dialogue. The runner no longer triggers the reward event a second time.
- Worlds 1-2 reject hidden action calls and room-position fallback. Later worlds retain explicitly recorded assistance.
- The guard certification route visits the Security Post and wins the authored Shield Trainer encounter. It no longer invokes cargo-loader events from Admin Lobby or invents a workshop victory.
- Four encounters earn victories through the production CombatController, ATB ticker, enemy AI, and public attack/earned-skill commands. Room encounter overrides and enemy presence are checked.
- The counter-tuning puzzle uses public slider and submit controls, using the authored solution rather than directly firing its success event.
- World 2 visits the actual medkit, observation mural, coolant garden, ring console, emitter rooms, pressure gauge, breakers, and salvage site before selecting their visible actions.
- The old canopy fixture used an Echo Borer that was absent from that room. The new route engages the actual Stalker Vine, retreats through the public command, verifies it remains undefeated, and defeats an actual Echo Borer in Stream Cave Depths to satisfy the quest's accepted victory condition.

## Results

| Assistance in six-world runner | Before | After |
| --- | ---: | ---: |
| Room-position shortcuts | 1 | 0 |
| Hidden action calls | 49 | 36 |
| Synthetic victories | 23 | 19 |

Worlds 1-2 retain four synthetic victories: the Launch Lift bulwark, Launch Bay buoy and Iron Warden, and World 2 Beast. Those are the next combat routes to replace. The remaining hidden action calls occur in later worlds.

Six focused tests pass: three campaign route tests and three provision-source tests. They include all 30 main quests, six-world save/reload checkpoints, the separate unassisted opening, and the existing public fishing/procurement/crafting/cooking route.

Generated reports:

- `app/build/reports/campaign-systems/public-opening-route.txt`
- `app/build/reports/campaign-systems/public-worlds12-combat.txt`
- `app/build/reports/campaign-systems/assistance.txt`

## Limits

The driver uses a deterministic seed (17), virtual time, immediate menu choices, authored puzzle answers, disabled combat tutorials, and animation-completion callbacks. It selects earned combat abilities without supplying extra unlocks or combat stats. The starting inventory and zero-credit session remain the existing campaign test fixture.

This establishes controller-route coverage, not physical Android touch, desktop keyboard/mouse usability, all-seed combat balance, or human discovery of puzzle answers. The full campaign still has the assistance counted above. Existing economy observations change with the revised route's rewards and inventory; earlier audit reports are historical snapshots.
