# World 4 navigation repair — November 5, 2026

Navigation policy follow-up: [named travel review](NAMED_TRAVEL_REVIEW_2026-11-05.md) restores selected reciprocal vertical alternatives in Worlds 3 and 5. Counts and blanket NSEW claims below describe the earlier pass. World 4 gameplay is unchanged by the follow-up.


Scope: the Foundry’s diagonal routes, unintended loss of backtracking after the Springs scene, story gates, saved progress, and room-map consistency. Game changes remain local; no Pixel installation or GitHub publication is part of this pass.

## Findings and decisions

World 4 had ten diagonal connection directions: three branch entrances/returns and two redundant cross-area passages. Unlike World 3, its static graph had no one-way descents. Its larger return-travel defect was the split between the Slag Pits and Assembly: completing Ghost in the Machine warped the crew to Conveyor Belt, but no walking connection joined the two regions. The current hub map cannot directly select a node in the other hub, and relying on ship travel is an inappropriate substitute for a factory maintenance route.

The Phantom records and Cooling Springs conversation intentionally precede Assembly. The repair keeps that sequence and its original transfer, then opens a permanent, reversible maintenance corridor. It does not grant early access to the production floor or turn Gh0st’s personal override key into a factory door key.

| Existing route | Repair |
| --- | --- |
| Waste Intake NE ↔ Crusher Throat SW | Waste Intake → south → Crusher Throat → south through Records Shed, Magnet Ramp, Intake Catwalk and Sorter Spine. All returns are north. The Crucible approach stays north of Waste Intake. |
| Conveyor Belt NE ↔ Timing Tower SW | Extend Service Walk north into Timing Tower, then continue north through Reject Chute, Roller Underpass, Control Perch and Belt Nerve. All returns are south. The tower’s main-quest controls remain accessible. |
| Power Core NE ↔ Turbine Balcony SW | Power Core → south → Turbine Balcony → south through Engine Cradle, Array Lockers, Coolant Choir and Core Shadow. All returns are north. Engine Service Ring stays north of the core. |
| Cooling Springs SE ↔ Waste Intake NW | Remove the redundant passage. Walk north through Obsidian Shelf, then east through Slag River to Waste Intake; reverse west/west/south to return. |
| Forge SE ↔ Power Core NW | Remove the redundant passage. Walk north to Conveyor Belt, then east through Conditioning Chamber to Power Core; reverse west/west/south to return. |
| No walkable Slag Pits/Assembly connection | Join Pipe Gallery east ↔ Reject Bay west. Both directions require the permanent clearance established by the Phantom/Springs sequence. |

All 74 existing rooms remain, with 146 directed NSEW connections and an immediate opposite-direction return for each. No room has more than four exits. Branch coordinates agree with the new compass routes; four obsolete cross-node transition records are removed and the two new maintenance-corridor records are added. The ten tapped return conveniences remain optional, with their walking alternatives intact.

## Story and gate handling

- **Project Phantom and Gh0st’s decision:** the records and Springs dialogue remain in their original order. The Springs event retains its key, skill, XP, completion, next-quest and transfer actions, and additionally clears the maintenance corridor before entering Assembly.
- **No premature entry:** both ends of the new corridor have an explicit gate. A fresh MQ17 state, the Phantom-records milestone alone, or merely owning Gh0st’s override key does not satisfy migration proof of the story clearance.
- **Old saves:** migration restores corridor clearance from completed MQ17, its completion/Springs milestone, its completed regroup task, or later Foundry main-quest progress. It does not move the player, replay dialogue, complete a quest, grant a key/skill, or pay rewards. Tests cover positive evidence, insufficient evidence and idempotence. An older post-Springs save already back in the Slag Pits can now walk into Assembly again.
- **Optional smelter:** the cryogenic valve still controls the north crossing from Stepping Stones into the Subterranean Smelter. The real controller verifies blocked entry, opening, entry, save/load, return and reset.
- **Crucible:** the existing three-round ladder and real retreat/retry remain unchanged. The earned complementary route now leaves all early optional quests and the Crucible until after entering Assembly, walks back to finish them, and returns to the main route.
- **Meltdown and departure:** Rylos, Titan victory, the Anvil, engine and array requirements remain intact. The story supplies urgency, but has no existing timer or corridor-collapse event justifying a permanent early-area lockout. Ordinary backtracking stays open until the deliberate departure to the Orbital Ring.

## Runtime and earned-route verification

The controller traversal fixture walks all 74 rooms and all 146 directed connections, including opposite-direction returns and compass indicators. It seeds a post-Springs state with the optional coolant crossing open; it is separate from earned campaign proof.

The live Springs fixture first attempts the sealed maintenance passage, follows the actual lower-region walking route to Cooling Springs, finishes the authored conversation, and receives the original one-time reward and Assembly transfer. It then walks back through Reject Bay to Pipe Gallery, saves a representation of an older completed-story state without the new corridor flag, reloads it through the normal save manager, and walks east into Assembly again. XP, inventory and story progress are preserved.

Separate fixtures reload inside each former diagonal branch and exercise the coolant-controlled smelter crossing. Broader World 3 and Worlds 4–6 controller regressions are included. Their desktop fixtures now perform save loading, dialogue advancement and service shutdown on the UI thread; the first combined run exposed a fixture-thread race, not a World 3 gameplay regression.

The earned campaign test starts through Worlds 1–3 with production crew progression, then completes both complementary late-world routes. World 4 movement is now explicitly restricted to NSEW. Both routes test the new cross-hub walk and save/load. One does early optional quests before Springs; the other returns for all three and the Crucible afterward. Quality Control’s two outcomes, exact optional rewards, real arena/Titan combat, the Anvil machinery and launch prerequisites remain covered. Known answers, virtual time, a fixed combat seed and predetermined build/supply policies remain test limitations.

## Documentation and verification record

Final checks passed: **803 Android host tests**, **26 desktop/controller tests**, the static navigation audit, the 74-room walkthrough reference check, `git diff --check`, and `:app:assembleDebug`. No failures, errors or skips in final test runs. All 40 JSON catalogs packaged in the APK match source.

The [World 4 walkthrough](../playtest/walkthroughs/WORLD_4_COMPLETIONIST_WALKTHROUGH.md) describes the new corridors and all 74 room references. It no longer tells the player early optional content must be finished before Springs. The local reader currently covers Worlds 1–3, so its publication is unchanged.

Final totals, command logs, source/build hashes, source-delta checks, navigation inventory and earned routes are in [verification.json](evidence/world4_navigation_2026-11-05/verification.json).

The static audit now rejects off-axis World 4 links as well as World 3 links, while continuing to check reciprocity, coordinate agreement, inbound collisions, room overlap and transition metadata across the game. Worlds 5–6 retain thirteen directed off-axis links pending their own contextual passes.

## Remaining human acceptance

Play the repaired build on a phone to assess route discovery, map framing, whether the new maintenance connection is clear, and whether the additional walking during the Foundry escape feels appropriate. No touch/audio/pacing acceptance, installation, commit or publication is claimed here.
