# World 5 navigation repair — November 5, 2026

Navigation policy follow-up: [named travel review](NAMED_TRAVEL_REVIEW_2026-11-05.md) restores selected reciprocal vertical alternatives in Worlds 3 and 5. Counts and blanket NSEW claims below describe the earlier pass. World 4 gameplay is unchanged by the follow-up.


Scope: Ring room navigation, return travel between upper/deep regions, the Construct/Anchor story boundary, optional content access, save compatibility, and maps. No installation, commit, push or publication is performed by this pass.

## Context and decisions

World 5 had six diagonal direction entries and three vertical entries, including the one-way Gamma-to-Anchor drop. Server Farm exposed six exits, so converting labels in place would have caused conflicting cardinal directions. The Ring’s story requires the Firewall Construct to protect Elara and the Anchor, yet the Server Farm’s east connection already permitted walking into the chamber before that victory.

| Before | Repair |
| --- | --- |
| Solarium SE ↔ Security Hub NW | Remove the redundant passage. Walk north to Executive Dock, east to Grand Concourse, east to Security Hub; reverse west/west/south to return. |
| Security Hub NE ↔ service alcove SW | Extend a south maintenance corridor through the seven existing branch rooms, ending at Ring Threat Cathedral. Return north. The surveillance platform stays north of Security Hub. |
| Service Shaft down ↔ Server Farm up | Use Service Shaft → east → Zero-G Junction → south → SysAdmin Nest → east → Server Farm. Every segment has an immediate opposite-direction return. No rooms are added. The actual three-tube Zero-G Junction now has a south exit to the deep region alongside its western shaft and eastern lab routes. |
| Server Farm NE ↔ deep service alcove SW | Extend the backup aisle south: Server Farm → south → Purged Backup Rack → south → service alcove → south through the remaining maintenance rooms. Return north. |
| Firewall Gamma → down → Anchor, one way | Remove the redundant drop. Return west through Beta and Alpha to Mainframe Nave, south to Server Farm, then east to the Anchor entrance. The static room description and victory feedback explain that route. |
| Anchor entrance already open before its supposed guardian falls | Gate both directions of the Farm/Anchor doorway until the Construct victory clears it permanently. Preserve legacy players already beyond the doorway without granting story rewards. |

All 81 existing rooms remain. The 160 directed local connections are NSEW and reciprocal, with no room above four exits. Branch and mainframe coordinates match the new compass routes. Five stale cross-node transition records are removed; two reciprocal Junction/SysAdmin records are added. The nine optional return shortcuts remain repeatable conveniences with normal walking alternatives.

There is no collapse, sealed return door or recorded one-way story commitment attached to the old Gamma drop. Its destination was already reachable from the Farm, and its return path required a detour through the firewall aisles. The revised route gives predictable forward/back travel while making the actual guardian requirement meaningful.

## Story boundaries and existing progress

- **Arrival supplies:** Customs Lounge remains reachable before the Orbital Fighter. Its shop/rest route is deliberately preserved; the new navigation must not remove the earned preparation strategy already used by the campaign tests.
- **Descent and Thorne:** the shaft timing task, mainframe inspection and Thorne conversation keep their requirements and order. The route now passes the SysAdmin station rather than using an unswipeable up/down link.
- **Firewall:** Alpha, Beta and Gamma still require their ordered controls. Disabling all three is insufficient to enter the Anchor Chamber; the real Construct victory clears the entrance. The existing 1,200-XP completion, MQ24 start and all subsequent story actions remain intact.
- **Elara and Anchor:** their scene, quest/task checks, item, Stasis unlock and rewards remain unchanged. The physical door now reflects the story’s existing “chamber opens” promise.
- **Admin Privileges:** the key can still be found early. Its executive-armory turn-in now happens after the chamber route is cleared; the earned completionist route turns it in at Throne Room after taking the Anchor and before the Soloist scene. This is an intentional consequence of enforcing the existing story boundary, not a removed side quest.
- **Legacy saves:** restore permanent entrance clearance from completed Firewall/later Ring story progress, completion/task/event receipts, or a saved/visited location already in the Anchor, Throne or Tear regions. Existing players beyond the new gate retain their exit and return. No boss win, quest completion, XP, AP, inventory or skill is granted by this repair. Gamma-down alone and merely owning an Anchor item do not prove clearance.
- **Magnetic clamp:** it still controls only the optional eastern Breached Avionics Lab crossing. It does not gate the new south route through the SysAdmin station. Open/enter/save/load/return/reset behavior is tested through the controller.
- **Resupply and departure:** after the guardian, Elara/Anchor and Soloist scenes, ordinary walking back to Customs remains possible. Entering the reality breach into the Source remains the deliberate story/world transition. It is not turned into an ordinary reversible local door.

## Verification approach

Final checks passed: **805 Android host tests**, **32 desktop/controller tests**, the static navigation audit, the 81-room walkthrough-reference check, `git diff --check`, and `:app:assembleDebug`. No failures, errors or skips in final test runs. All 40 JSON catalogs packaged in the APK match source.

The seeded controller traversal walks all 81 rooms and all 160 directed links, checks compass coverage, and returns along opposite directions. It seeds completed Ring progress and the open optional magnetic crossing; it is not campaign completion proof.

Other controller cases check: blocked Anchor entry after Gamma controls but before victory; the normal encounter/victory callback opening the door; reload without the new field; walking out to the upper Ring and back; legacy occupants in Anchor/Throne/Tear leaving without boss credit; reloads inside former diagonal branches; the optional magnetic gate and saved return; and shop/rest preparation before the initial Fighter. The controller victory fixture simulates the combat-result callback only. The separate earned campaign plays the actual combat engine.

The earned complementary campaign routes use production crew progression and World 1–4 resources. World 5 movement is now explicitly restricted to NSEW. Both runs complete all five main and all five optional quests, exercise independent-guardian/covenant outcomes and opposite order for the early optional work, and retain the SysAdmin key for its later armory turn-in. After the real Construct win, the crew walks to Customs for rest, saves/reloads, returns through the new cardinal descent, and enters the cleared chamber. The actual Avatar fight, supply restock and Source departure remain covered. Known answers, virtual time, a fixed combat seed and predetermined build/supply policies limit the fixture’s balance claims.

The [updated walkthrough](../playtest/walkthroughs/WORLD_5_COMPLETIONIST_WALKTHROUGH.md) includes the new route, changed optional turn-in timing and all 81 room/exit references. It remains local; the published reader still covers Worlds 1–3.

Final counts, command logs, navigation data, source-delta checks, earned routes and build hashes are in [verification.json](evidence/world5_navigation_2026-11-05/verification.json). The static audit enforces cardinal travel in Worlds 3–5 and reciprocal geometry/transition metadata throughout the game. Only World 6 retains four off-axis direction entries pending its own contextual pass.

## Human acceptance remaining

Check the new descent’s clarity, the maintenance branches’ map framing, discovery of the guarded Anchor entrance, return-walk pacing after the Construct, and resupply on the player’s own build. No manual phone touch, audio, readability or subjective fun acceptance is claimed. Other worlds’ gameplay catalogs are preserved.
