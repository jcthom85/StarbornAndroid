# Quest information, discovery, camp supplies and Temple Gate pass

October 30, 2026. Implements the five items the user delegated after the encounter/salvage pass. Residents/Megan and Charlie remain unchanged for collaborative discussion. Active-quest organization remains explicitly ignored.

## Decisions and implementation

### Quest information

The production quest-detail builder already limits active quests to current and previous stages. It does not expose future stages. The remaining issue was content inside visible summaries, descriptions and tasks.

World 2 opening summaries now describe the current lead without announcing Orion's identity, the Bridge Echo, Gh0st's failsafe outcome, the Source Beast or the final flight destination. Sleeping Giant's initial objectives still name Hall of Echoes and Stasis Chamber so players have a usable route. Later stasis objectives refer to the occupant without naming him before the conversation. Ring Array and Source Gate objectives no longer prescribe their puzzle answers. The Signal still names its established Chime interaction. Lost Patrol starts with tracing the patrol rather than advertising the tool discovered later. Stage/task IDs, completion conditions and rewards remain unchanged.

This is a World 2 content pass plus verification of the shared stage filter, not a claim that every later-world quest has been narratively certified.

### Discovery narration

Reviewed the 96-interaction audit by role. Existing event-authored discoveries retain their narration; ordinary inspections and functional controls do not receive extra reward scenes. The 51 later-world containers without event discovery text now have individually authored, short `discovery_message` copy. These span Worlds 2–6 and describe opening/recovery rather than listing the same items as the reward popups.

`ContainerAction` accepts this optional field. The shared controller starts narration before enqueueing rewards. Narration dismissal releases the item queue. Inventory and one-time room receipts are still saved immediately, so leaving the application cannot lose a granted reward. Reusing an opened container cannot pay again. World 1's two containers omit the new field and retain their existing presentation.

The locker drains, the reliquary opens, packing protects salvage, and survey packs retain sealed inner supplies. Later-world containers use similarly restrained text grounded in their existing names and contents; no new lore, rewards, quest prerequisites or routes were invented.

### Natural object language

World 2 scan/gather/salvage interactions are now named **moss**, **bioluminescent weeds**, **crystalline ferns**, **drifting spores**, **tideglass growth** and **power conduits**. Prose describes those objects instead of telling the player to scan/gather before the relevant quest. Their existing active-quest visibility, completion retirement and event IDs remain intact. The canopy's separate passive fern inspection is labeled **low-frequency hum**, avoiding a competing fern action. Quest objectives still use verbs when explaining what the player should do.

### Camp supplies

Removed automatic first-visit Neural Stabilizer (`painkillers`) gifts from all ten generic camp events in Worlds 2–5. Resting is an unlimited HP service, including at full health; it is not a source of medical inventory. Camp prose no longer implies automatic consumption or Erosion treatment. Previously acquired items remain in saves. Explicit quest rewards and exploration supplies are unchanged; Sentinel Scraps still sells Neural Stabilizers, and Botanist/Tideglass rewards still provide them. This preserves deliberate ways to obtain Erosion medicine.

### Temple-to-Hall entry

Using the Chime opens the gate and completes The Signal while leaving the party at Temple Gate. The existing Chime gate stinger remains. Android adds a 1.2-second stone-slab slide on the actual closed-to-open state change, with no camera shake or flash. The screen-shake accessibility setting also skips this movement. Already-open gates and reloads do not replay it.

The opening text describes stone withdrawing into the cliff and cool air from the east. Players walk east into Sanctuary Grand Hall and then east into Hall of Echoes using the existing movement presentation. The gate stays open on revisits. Sleeping Giant still starts at authentication; no new timing-dependent quest requirement was introduced.

## Verification

**65 targeted tests passed**, zero failures/errors/skips. Includes:

- Real World 2 main and completionist routes, including explicit verification that opening the gate leaves the crew at Temple Gate and ordinary eastward travel enters the foyer.
- Opening journal shows the current stage and useful room directions without naming the unknown occupant or Bridge Echo.
- Six representative real-controller container cases across Worlds 2–6: exact authored narration before item prompts, correct inventory delivery, no duplicate reward on reuse. The complete 51-container data set is checked for authored coverage. An initial attempt to instantiate all 51 full campaign harnesses in one test exhausted the test worker's 512 MB heap; representative runtime samples replaced that fixture-heavy loop.
- Repeated and full-health REST at all ten camps, with zero generated stabilizers; existing quest supplies/rewards still pass route checks.
- Natural moss action visibility before/after quest activation, existing read receipts, sluice gates and second-half regressions.
- Both Hub 2 critical-flow tests, data integrity and World 2 discoverability tests.

Android debug assembly and desktop Kotlin compilation passed. Diff whitespace validation passed. The broader PowerShell-based asset validation remains unavailable without `pwsh`; the Kotlin data-integrity suite did pass. [Evidence and source/APK hashes](evidence/WORLD2_QUEST_DISCOVERY_CAMP_AND_GATE_PASS_2026-10-30.json).

The completionist walkthrough was updated for labels, discovery text, encounter locations and physical gate entry. The playtest website was rebuilt locally from that source; it has not been published. No commit, push or Pixel install was performed in this pass.

## Focused physical-device checks

1. Open the submerged locker/reliquary: discovery first, then each item reward; return/reload without another payout.
2. Start Botanist and check that natural object labels become actionable, then retire after scans. Repeat for tideglass and conduits at their appropriate quest stages.
3. Rest repeatedly, including at full HP: health recovers normally, inventory does not gain stabilizers. Check the camp text and rest cue.
4. Authenticate Temple Gate: stone slide and Chime sound feel coordinated, player stays outside, east enters the foyer, then east reaches the Hall. Reload and return: no replay. Check with screen shake disabled.
5. Read Sleeping Giant and the ridge quest before their reveals: useful directions, no premature identities or confrontation outcomes.

Animation and sound acceptance still require a Pixel run. These tests do not certify every later-world route or the balance of every Erosion-heavy sequence.

## What remains

The [status matrix](../playtest/reports/starborn_world2_part1_remaining_important_considerations.md) now has **32 addressed, Charlie awaiting discussion, and active-quest organization ignored**, plus the separately deferred residents/Megan issue. Those two creative conversations are the remaining work from this list.
