# World 2 refinement pass

Implemented September 30, 2026. This describes the current behavior; older
completion plans and walkthrough estimates are not balance acceptance evidence.

## October 1 writing and puzzle follow-up

- Orion's awakening now has six dialogue beats, with Nova and Zeke responding
  to the lost years before Orion joins. Recruitment and Bridge access still
  occur only at the final dialogue trigger.
- Revised crash dialogue, companion reactions and optional quest motivations
  around Zeke's procedural habits, Orion's personal ties to Sanctuary, and
  Gh0st's uncertainty without orders. Removed Nova's claim that the Warden sent
  them. Ancient Echoes no longer uses identical hint/success/failure text.
- Stasis retains its three safety decisions. Source Gate now has two routing
  decisions; Orion's resolution follows the final correct switch without a
  third repeated question about merging voices. Wrong answers and leaving
  retain the existing retry flow. Mural choices no longer include the role
  explanation inside the answer labels; textual clues remain available.
- World 1 certification labels workshop setup as optional practice while
  retaining the task IDs and compatibility with the workshop drill.
- Updated the existing World2RefinementTest sequence lengths. Tests were not
  run for this follow-up. Edited JSON parses, dialogue IDs are unique, all
  dialogue next references resolve, and the scoped diff has no whitespace errors.
- Playtest acceptance remains character voice, six-beat awakening pacing,
  Source Gate satisfaction, mural clue clarity and certification journal display.

## Progression and recovery

- Face the Beast can restart after defeat or retreat, including old saves that
  consumed the former one-time encounter event. Only a victory on Canopy Ridge
  records the defeated milestone. Recruitment and drill rewards remain guarded.
- The Anchor Drill completes its quest and unlocks Link only when its cinematic
  completes. Pending scenes resume through the existing save recovery mechanism.
- Load migration removes premature Beast/drill completion milestones from an
  active unfinished Hunter quest while retaining actual earned task/room evidence.
- Stasis now has three separate milestones: ring alignment, conversation with
  Orion, and opening the Bridge cradle. The 250 XP payout occurs at Bridge recovery.
  Completed old saves retain their Bridge/installed-Bridge compatibility behavior.
- Departure asks the player to finish Sector 9 exploration before committing.
  Cancelling leaves the quest active; confirming starts a recoverable cinematic.
  World 3 transition and 350 XP occur on cinematic completion, once.

## Interactions

- Stasis: inspect the murals, pod, overview and coolant. At the Ring Array divert
  pressure around memory, preserve distinct identities, then reconnect return
  with relief open. Wrong choices explain the problem and retry that decision;
  stepping away permits reopening. Return west to talk to Orion, then open the
  sealed Bridge cradle.
- Source Gate: prepare horn, cup, pressure gauge and breakers. At the lock open
  relief into the grounded receiver, isolate lock supply while retaining relief,
  then let Orion resolve the remaining chord. Wrong choices never open the gate.
- Prism: shared Android/desktop beam view. Aim into the circular receiver, close
  the colored fringe using crystal spacing, and adjust glow until steady green.
  Visual diagnostics and submission use the same authored tolerances. No numeric
  targets are displayed. Cancel/reopen resets the local controls without rewards.
- Ancient Echoes: after recovering all three crystals, use the mural array to
  seat west in the foundation, east in the paired arcs, and north in the star.
  Ordinary conversation with Orion no longer completes the puzzle automatically.
- Stolen Tech: read two scan pulses followed by the maintenance gap. Latch the
  mask to the gap before bypassing the hatch. There is no fictitious countdown.
- Tideglass Day now describes crafting Source Resin and discovers the beach at
  Tide Pools, rather than Mist Pools. The canopy-clear message fits either enemy.
- Inspection of Astra opens an explicit salvage return route to Pod Fragment;
  collecting conduits enables the return action to Hangar Bay.
- The hangar annex has safety, cargo, fuel and navigation inspections. Existing
  Sky rooms/art are retained as grounded flight-plan projections, not a second
  playable launch or a flight through the outer Shield. Departure follows a low
  service airlane to the Spire and encounters local Dominion interference.

## Verification

New `World2RefinementTest` exercises real authored events and DialogueService,
with headless hooks, actual protobuf disk save/load, deferred cinematic callbacks,
wrong decisions, cancellation, guarded rewards, old-state migration and Prism
diagnostics. It does not simulate boss balance or render UI.

Related campaign fixtures now traverse the new dialogue decisions and separate
Orion/Bridge steps. The headless audit uses the current Stalker-Vine canopy enemy
and correct starting hub. These fixtures still synthesize victories and may seed
positions; their success does not establish device navigation or puzzle fun.

Campaign XP expectations were corrected to exclude the retired 480 XP relay leak:
4,625 event XP through World 3 (4,525 main quest XP plus 100 training XP), level 6
without battle XP; the scripted pre-Titan route receives 9,970 XP, level 8.

The selected regression batch contains 78 methods across:
World2RefinementTest, DataIntegrityTest, WorldTwoDiscoverabilityStateTest,
OpeningNarrativeMigrationTest, World1RecentChangesTest, CounterTuneSignalTest,
CampaignEventIntegrationTest, LegalRouteCampaignRunnerTest and
World2PlaythroughAuditTest. Android unit compilation and desktop Kotlin
compilation passed. All 78 methods passed with zero failures, errors or skips. Evidence: `build/world2-refinement/focused.log` and
`app/build/test-results/testDebugUnitTest`.

## Device playtest still required

1. Try each wrong puzzle answer, leave midway, save/reload, and reopen it.
2. Lose to or retreat from the Beast, then restart from Face the Beast.
3. Check that waking Orion feels substantial and the new next-objective guidance
   points back to the Stasis Chamber and Bridge cradle.
4. Check Prism beam readability, slider touch accuracy and diagnostics on a phone.
5. Verify the salvage shortcut and cross-hub transitions refresh room/map state.
6. Cancel departure, finish optional quests, then confirm. Reload during the drill
   and departure scenes. Check party, rewards and destination after resuming.
7. Run ordinary combat with campaign-earned gear and inventory to evaluate the
   Beast, elite encounters, recovery supplies and overall chapter length.

The facility Maestro flow's changed interaction boundaries were updated, but it
has not been executed on a device in this pass. Its older combat timing and UI
selectors still require device validation; a passing unit suite is not a Maestro
result.
