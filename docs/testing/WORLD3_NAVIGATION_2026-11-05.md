# World 3 navigation repair — November 5, 2026

Navigation policy follow-up: [named travel review](NAMED_TRAVEL_REVIEW_2026-11-05.md) restores selected reciprocal vertical alternatives in Worlds 3 and 5. Counts and blanket NSEW claims below describe the earlier pass. World 4 gameplay is unchanged by the follow-up.


Scope: World 3 only. Repair diagonal and unintended one-way local travel while preserving the safehouse/heist story, Upper City infiltration, optional content, security gate, return lift and departure. Changes remain local; the phone installation has not been updated by this pass.

## Result

All 70 existing World 3 rooms remain present. Their 142 directed local connections now use only north/south/east/west, with an immediate opposite-direction return for every connection. No room has more than four connections, and no World 3 `special_exits` hide a local path from the compass. Both city floors are individually connected; the story-authorized transfer between them remains separate.

| Before | Decision and resulting route |
| --- | --- |
| The Static NE / Back Bar Ledger SW, beside four existing cardinal exits | Put the existing back-bar passage on the east approach to the subway car: Static → east → Back Bar Ledger → east → Old Subway Car → east → Rigging Deck → east → Secret Signal Workshop. The ledger room’s north roof stairs remain reversible; the safehouse route north from Static is unchanged. |
| Night Market NE / Lantern Roof SW | Extend the existing lantern-bridge steps: Night Market → north → Lantern Bridge → north → Lantern Roof → north → Spice Alley → north → Rain Cache. Every step reverses south. |
| Transit Plaza NE / Ad Bay SW, beside four existing cardinal exits | Extend the under-stairs kiosk corridor: Transit Plaza → south → Security Kiosk → south → Ad Bay → south → Ticket Shadow → south → Underrail Map Room. Every step reverses north. |
| Archive NE / Catalog Atrium SW | Use the Archive’s free south exit: Archive Vault → south → Catalog Atrium → south → Prism Service Bay → south → Alarm Spine. Every step reverses north. |
| Service Lift SE / Exec Lounge NW | Remove redundant direct passage. Walk Service Lift → west → Uniform Sorting → south → Exec Lounge; return north/east. |
| Exec Lounge NE / Archive Vault SW | Remove redundant direct passage. Walk north through Uniform Sorting and Laundry Service, then east through Prism Gallery to Archive Vault. |
| Underrail Platform → down → Transit Plaza, one way | Remove redundant descent. Walk east to Static, then south to Transit Plaza; the return is north/west. |
| Service Lift → down → Archive Vault, one way | Remove redundant chute. Walk west to Uniform Sorting, north to Laundry, then east through Prism Gallery into the Archive. |
| Glasswalk → down → Archive Vault, one way | Remove redundant descent. Walk east to Donor Gallery, then south to Archive Vault; return north/west. |

The three descents had ordinary alternate walking routes, no associated quest gates, and no collapsing/drop/escape sequence requiring one-way movement. The data described them as shortcuts, not narrative commitments. Removing them avoids inventing return lifts and does not remove access to their destinations.

Room positions were adjusted within the four repacked branch areas so minimaps agree with the compass. Seven obsolete World 3 cross-node transition records were removed. Room descriptions, applicable description variants, the completionist walkthrough and the local generated guide reader describe the new paths. No guide publication was performed.

## Story rules preserved

- **Lower City → Upper City:** committing the prepared heist remains a quest-driven move to Laundry Service. The physical floor graphs do not provide an early shortcut around preparation. Existing room IDs, quest requirements, stage transitions and rewards are unchanged.
- **City-level return:** after The Plan, Service Lift’s panel returns to Transit Plaza; Transit Plaza’s elevator controls return to Laundry. These remain deliberate, guarded, repeatable lift actions. Existing public-controller tests cover repeated trips and save/reload.
- **Black-site lab:** Donor Gallery’s north laser gate still requires its optical console. Opening the gate, entering the lab, saving/loading, returning south and resetting the console are exercised through the shared controller. Resetting blocks entry again.
- **Lens, lockdown and rooftop:** the Lens routing puzzle, alarm, Administrator fight, Scan requirement and launch event retain their requirements. Earned completionist routes cover both ledger outcomes, actual rooftop combat, optional-quest turn-ins and persistent rewards.
- **Departure:** the launch into the Foundry remains an intentional story transition, rather than an ordinary room door requiring a direct opposite swipe.
- **Optional return conveniences:** all ten tapped branch-return shortcuts remain available. They are not required to escape a branch: every room also has a reversible walking path. Their inspection feedback now says this explicitly.

No room IDs were added or removed; no quest/event/ownership changes are needed for save migration. Tests save and reload inside each former diagonal branch with old-style direction receipts and then return using the current cardinal path. This checks retained room identity and new-route hydration, not a claim that every historical save has been tested.

## Verification and evidence

Final checks passed: **801 Android host tests**, **22 focused desktop/controller tests**, the static navigation audit, guide browser checks, `git diff --check`, and `:app:assembleDebug`. No test failures, errors or skips. All 40 JSON catalogs packaged in the APK match source.

Final totals, source/build hashes and command logs are in [verification.json](evidence/world3_navigation_2026-11-05/verification.json).

Checks include:

- Whole-game navigation integrity, compass geometry, opposite returns, inbound-direction collisions, room overlap, corridors drawn through unrelated room markers, and cross-node transition metadata.
- World 3 invariants: all 70 rooms, exclusively cardinal directions, at most four exits per room, immediate returns, visible compass paths, and two complete physical floors without an unintended heist bypass.
- Shared-controller traversal of all 70 rooms and all 142 directed connections, with opposite-direction return travel. The traversal fixture seeds a post-Lens timeline and opens the lab gate; it is separate from campaign proof.
- Reloaded returns from all four repacked branches; optical gate entry/return/reset; retained city-lift round trips.
- Earned World 3 completionist runs with public movement explicitly restricted to NSEW, plus the full host suite’s campaign and late-world coverage. Known answers, virtual time, predetermined supply policies and combat seeds remain fixture limitations.
- Updated guide browser checks and debug APK assembly.

The command-line navigation audit also now rejects non-cardinal World 3 links. It distinguishes the unreachable retained World 1 launch-rail artwork from playable one-way travel, consistent with the existing navigation regression test; no World 1 gameplay data changed. The remaining World 5 descent is still reported in raw inventory and reviewed separately.

## Remaining phone acceptance

Check swipe discovery, minimap framing and route pacing on a phone, particularly the market roof, kiosk corridor, back-bar/subway approach and Archive south wing. No device installation or manual touch playthrough is part of this pass. Worlds 4–6 still retain their previously audited diagonal routes pending their own contextual passes.
