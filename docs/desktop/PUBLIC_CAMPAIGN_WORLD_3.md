# World 3 public campaign route

The follow-up [World 4 pass](PUBLIC_CAMPAIGN_WORLD_4.md) extends the unassisted
route through the Foundry and replaces the Titan Walker's synthetic victory.
This document records the preceding pass.

Worlds 1-3 now complete without hidden action calls, room-position shortcuts, or synthetic victories. The runner keeps strict route checks active until it reaches World 4.

## Route changes

- The landing pad has no Sewer Crawler. The route walks to Sewer Scale 01 and defeats the actual crawler to satisfy the landing-clear objective.
- Jax's dialogue at The Static supplies his intel. The route visits the Underrail Platform, Security Kiosk, Elevator Service Gate, and Safehouse Roof before returning to Zeke's workstation and planning table.
- Skypark's guest registry establishes the forged profile; Laundry Service's sensor console and service chute complete the infiltration.
- The archive sequence reads the containment field and command tether-lines in the vault, reads the shutters in the Prism Gallery, and selects the gallery's gated braided-chord action before returning to claim the Lens.
- The rooftop fights both full groups: Aero Drone/Heavy Mech, then Administrator/Aero Drone. Earned outcomes drive the shield scan and Foundry departure.

## Combat and preparation

The driver now lets area abilities resolve their normal group targets, with a regression assertion that their queued targets include all living enemies. Its former single-target override wasted area damage and control.

The policy also uses already-unlocked Nova Link, Orion Nano Repair, and Zeke Bulwark Stance when appropriate, prioritizes injured allies for healing, and removes low-HP enemy adds. It selects skills through the public combat controls and supplies no additional unlocks or combat stats.

Before the rooftop, the route uses the visible pavilion shelter and purchases six medkits from the Upper City Concierge for 408 earned credits. Purchase checks verify both inventory gain and exact credit deduction. It returns to the same rest stop between rooftop groups. The seeded successful rooftop trace uses two medkits in the first group, then four medkits and eleven owned ration packs in the Administrator group. This is a costly successful policy, not an optimal or all-seed balance result.

## Validation and remaining work

Ten Android tests and one desktop integration test pass. Coverage includes all 30 main quests, six-world save/reload checkpoints, an unassisted opening, the existing public fishing/procurement/crafting/cooking slice, provision sources, snack contracts, and reward gear eligibility.

| Six-world assistance | Before this pass | Current |
| --- | ---: | ---: |
| Room-position shortcuts | 0 | 0 |
| Hidden action calls | 36 | 28 |
| Synthetic victories | 15 | 12 |

All remaining assistance occurs in Worlds 4-6. Generated reports include `app/build/reports/campaign-systems/public-worlds123-combat.txt` and `assistance.txt`. Combat traces distinguish actual knockouts from the HP persisted after victory recovery.

This pass changes tests and documentation. It continues to use the existing starter-component fixture, production bootstrap equipment, seed 17, virtual time, authored puzzle answers, disabled tutorials, and animation callbacks. It does not establish physical device usability or broad combat balance. Earlier economy and combat snapshots are historical because the driver now follows a different route and spends supplies.
