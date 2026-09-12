# Route and narrative review

## Astra checkpoint

Boarding saves the return location and warps to `astra_bridge` in
`ExplorationViewModel.boardAstraFromWorld`. The bridge leads south to the common
room, which leads north to bridge, west to cargo, east to quarters and south to
simulation. Each spoke has a reciprocal return connection. All five rooms are
reachable in at most two authored edges from boarding. This is static route
evidence, not an end-to-end navigation playtest.

Facility terminology and behavior:

- Navigation console opens navigation UI; available destinations are progression-dependent.
- Great Frontier film archive opens the tape deck; distinct from the simulation archive console.
- Cargo repair bench is an inspection, not a crafting service. Its current message
  describes equipment without promising a repair operation; retain this distinction.
- Storage terminal explicitly says storage is offline; relic array says synchronization
  is future functionality. Neither should be counted as a functioning service.
- Quarters bed invokes party rest. Simulation console opens simulation UI, but its
  fallback message still says it awaits runtime. Review this stale wording against
  the full simulation flow before editing it.

The all-arcades common-room variant exceeded the 45-word room-copy limit and was
shortened while retaining every inline reference. Other Astra descriptions remain
under that limit.

## Remaining review

### World 2 main-quest route checkpoint (2026-09-12)

Reviewed MQ01–MQ05 journal tasks against authored room edges and action events.
Clarified MQ03 locations (Hall of Echoes, Stasis Chamber, Ring Array) and MQ05's
return to the Pod Fragment for conduits followed by Bridge installation at Hangar
Bay. The authored events confirm the sequence: ring alignment auto-completes
Orion/Bridge recovery; Source Gate requires four independent milestones; Astra
inspection precedes conduit collection; reboot requires Bridge and signal-cell
items; launch requires reboot completion. No gates or event definitions changed.

World 2 route ambiguity remains around side quests and player traversal through
the many canopy/beach/ruins branches. Continue with side-quest route mapping,
then review World 3 terminology. Static catalog tests do not replace device route
acceptance or save/load migration tests.

World 2 side-quest checkpoint: clarified flora scans (Resonant Canopy, Tide
Pools, Crash Site, Stasis Chamber), patrol beacons (Spore Thickets, Poison
Hollow, Sniper Lookout, then Burned Glade), and vent infiltration (High Gantry,
Infiltration Hatch, Transmitter Site). The old prose used generic "wilds," "ridge
perch," and "vent security" labels that did not match room titles. Action events
and gates remain unchanged. A catalog regression binds nine journal destinations
to their authored rooms. Remaining side-quest crafting/economy acceptance is not
proven by these static checks.

### World 3 terminology checkpoint (2026-09-12)

Reviewed all main- and side-quest task labels against World 3 room titles and
inline actions. Corrected nine ambiguous labels: MQ12 now names Security Kiosk,
Elevator Service Gate and Zeke's Apartment; SQ12 names the memorial in Uniform
Sorting and the Security Kiosk; SQ14 names Exec Lounge's concierge credential
mirror and private safe; SQ15 names Drone Test Alcove and its prototype rack.

These are terminology corrections only. The route graph, action events, quest
conditions and rewards are unchanged. A catalog regression ties the revised
tasks to shipped room titles/action names. The static check does not prove heist
sequencing, combat, or navigation on device. Next: trace World 3 event ordering
and transition gates, then continue Worlds 4–6.

World 3 event checkpoint: the planning-table event previously completed MQ12 and
directly started/tracked/warped into MQ13, while the production quest-completion
callback also fired `w3_mq12_to_w3_mq13`, repeating that same handoff. Consolidated
the handoff in the transition event and moved both Upper City node unlock pairs
there so legacy/completion recovery keeps the full route. The planning action now
owns task completion, XP, quest completion and milestone only. Hub3CriticalFlowTest
records quest-start callbacks and asserts MQ13 starts exactly once.

MQ13 ordering is blend at Skypark guest registry, then blind Laundry Service
sensors, then use its service chute. MQ14 requires Archive entry plus three
readings before the braided chord; Lens pickup starts MQ15. Rooftop launch requires
Administrator defeat and Shield-window scan. These gates already agree with the
journal; no further changes made. Device heist/roof traversal remains unproven.

### World 4 terminology checkpoint (2026-09-12)

Reviewed all World 4 main- and side-quest task labels against authored room
titles and inline actions. Corrected eight objectives: SQ18 now points to the
Waste Intake and its waste intake brake; MQ19 names The Forge and Anvil cradle;
SQ19 identifies Reject Bay and the three Conveyor Belt choices; SQ20 names
Control Alcove's hazard cycle map/overclock profile and The Forge's test chamber
terminal.

The edits are quest prose only. Events, gates, rewards, connections and action
definitions are unchanged. A catalog regression binds each revised objective to
the shipped room title or action label. Static coverage does not prove player
navigation, hazard-wave combat or choice behavior on device. The complete JVM
suite passes with 399 tests; narrative, selector and explicit-reference validators
also pass, and the discoverability audit remains at 22 classified omissions.
Next: World 5 terminology and event ordering, followed by World 6.

### World 5 terminology and event checkpoint (2026-09-12)

Reviewed MQ21–MQ25 and SQ21–SQ25 against the Orbital Ring and Deep Ring room
catalog. Corrected 25 objectives to identify their actual interaction points,
including Executive Dock controls, Solarium/Mirror Walk machinery, Service Shaft
and Mainframe routes, the three Director-log actions, Server Farm backup choices,
Anchor Chamber objects, and the Throne Room/Tear actions. Where the same event is
intentionally offered in two rooms, the journal now names both valid routes.

The five main-quest completion events each contain exactly one completion and one
successor start/track pair. No `quest_stage_complete` event repeats those handoffs,
so no event change analogous to World 3 was required. Catalog and handoff tests
lock this behavior. The quest edits are prose only; events, gates, routes, rewards,
connections and action definitions are unchanged. All 401 JVM tests and bounded
validators pass; the audit remains 22 classified omissions. This does not prove
fighter, zero-gravity, firewall, Avatar or route behavior on device. Next: World 6
terminology and event sequencing.

### World 6 terminology and event checkpoint (2026-09-12)

Reviewed MQ26–MQ30 and SQ26–SQ30 against the Source room/action catalog. Corrected
23 objectives to identify their interaction points: the Campfire song-fire,
Manager Patrol Route, all Memory Bridge anchors, the side-quest memory controls,
Memory Stair revisions, shared tuning focus, Aethel trail and future-Astra salvage
route. The Zeke finale now explicitly names both valid choices.

MQ26 through MQ29 completion events each own exactly one successor start/track
handoff, with no duplicate `quest_stage_complete` transition. MQ30 intentionally
has no successor quest: the shared tuning puzzle dispatches the existing cinematic
callback, which completes MQ30, sets game completion and reaches the epilogue.
Catalog and handoff regressions lock the reviewed contracts. Quest edits are prose
only; event, route, gate, reward, connection and action definitions are unchanged.
All 403 JVM tests and bounded validators pass; the audit remains 22 classified
omissions. This completes static per-world terminology/event review, not real
route discovery, finale combat, cinematic-resume or device acceptance.

### SQ03 compatibility resolution

The legacy loader/cargo objectives are still backed by `workshop_dock` actions:
`loader` dispatches `w1_sq03_start_loader`, and `cargo rail` dispatches
`w1_sq03_move_cargo`. Retained both IDs/events and clarified their journal text to
name Loading Dock in Jed's Workshop. No save migration change is needed for this
wording correction; no objective was removed from persisted task sets.

Current MQ03 review dialogue explicitly selects `guard_break_training` without
completing the two setup tasks. QuestRuntimeManager.setStage selects that stage
directly. The mandatory-route regression now omits both legacy event dispatches,
asserts the selected stage and absent setup completions, and simulates victory
at `admin_security` before returning to Boggs for authorization. The separate
legacy flow test retains its Workshop setup event sequence.

CombatTutorialTracker accepts a single Bulwark at either workshop_dock or
admin_security when SQ03 is active (or its training stage selected), provided
tutorials are enabled and not completed. That tutorial can seed the skill and
shield lesson; the quest victory event itself does not require a Guard Break
record. This remains event-level verification, not proof of a played encounter
or all imported-save combinations. Next: World 2 route and terminology review.

### World 1 side-quest checkpoint

Reviewed all five side-quest task lists against room actions and events. Corrected
nine journal tasks, leaving IDs, triggers, gates and rewards unchanged:

- SQ01: Market Strip east to The Hidden Stash; return west and north to Scrapper's
  Stall. The old scrap-pile destination did not match his authored location.
- SQ02: Triage Hall east to Ventilation Hub; inspect vent, use fans, clear toxic
  blockage, return west then north to Doc's Exam Room. No manual bypass lever
  action exists; event conditions require vent inspection before fans and fans
  before blockage clearance.
- SQ04: Air-Lock Entrance north through Aisle 01 to The Core Hub. Terminal starts
  the quest, console thawing enables a second terminal interaction to finish it.
- SQ05: power on at Cavern Junction, south to Main Tunnel Alpha, east to Side
  Shunt 4; inspect crew datapad. Entering the shunt with the power milestone starts
  the quest; this is not a promise that restoring power while already there starts it.

Ten directional edges in these instructions were checked against rooms.json.
SQ03 remains unresolved: journal tasks still mention a loader/cargo setup while
the current Security Post has a shield trainer inspection and training encounter.
Legacy start-loader/move-cargo events exist, but their current invocation and
task retirement need tracing before rewriting or removing objectives. The victory
event checks Acoustic Bulwark and active SQ03, not an explicit Guard Break action.
Do not interpret training prose as proof that Guard Break use is enforced.
No device playtest was run. Next resolve SQ03 journal/runtime compatibility, then
continue World 2 route review.

### Relic-to-launch event checkpoint

The tuning fork opens `w1_echo_counter_tune`: targets are 87 kHz, 68% cold loop,
180-degree ground phase. Puzzle success dispatches `w1_mq03_touch_relic`; that
event checks active MQ03 and an unfinished touch task, not every earlier mine
task. Its cinematic completion sets `echo_heart.relic_synced`, completes MQ03,
and unlocks the launch node. The Emergency Exit's north gate checks that room
flag. Do not describe earlier journal tasks as independently enforced sync gates.

MQ04 starts from quest-stage completion after MQ03 completion. Entering the
Emergency Exit marks the comms task; Cargo Lift victory advances to the sacrifice
stage. Jed's dialogue is stage-gated and dispatches `jed_sacrifice`, completing
MQ04 and starting MQ05. The journal now explicitly tells players to talk to Jed
after clearing the lift. The event itself only checks active MQ04: UI dialogue
gates and direct event conditions are not equivalent.

Pod Bay entry advances MQ05 to the Warden stage; Warden victory sets the hatch
flag and launch stage. Talk to Zeke in Pod Bay to set his relocation milestone,
then talk again in The Pod Core to splice the Chime. Only then does the navigation
console's event launch the cinematic. Completion warps to Sector 9 and starts
World 2. Journal copy now names this two-conversation handoff and console action;
Zeke's acceptance line distinguishes authorization from the pod's own power.
Updated the affected Maestro prose selector without changing flow steps.

Existing Hub1CriticalFlowTest dispatches this event/dialogue sequence with
cinematic callbacks; the new catalog regression binds journal destinations to
the authored room edge, dialogue milestones and console event. Neither is a
fresh movement, puzzle UI, combat, or cinematic-resume playtest. Side-quest routes
are next; World 1 player acceptance and Worlds 2-6 route review remain open.

### World 1 route checkpoint (2026-09-12)

Read all five main-quest task lists against World 1 room connections and hub-node
entry policies. Room edges alone are not the campaign graph: the Pit, Workshop,
Trade Row and Transit Checkpoint are separate hub-entry destinations.

Confirmed and repaired MQ02 directions: Jed's handoff/reminder and two journal
tasks previously implied a northward path through Trade Row and sent the player
to an "Admin window." Trade Row has no room connection to the checkpoint. The
Transit Checkpoint hub node enters Shift Queue; north is Search Bay, then east
is Zeke's Booth. Zeke is present there before MQ02 completion. Blast Door A's
north exit requires the Mine Access Badge; Zeke's booth is before that door.
The corrected copy names the hub destination and actual room titles. A catalog
regression ties these instructions to the node and directional connections.

Static main-route ledger (not movement/encounter acceptance):

| Segment | Authored route / boundary | Remaining evidence |
| --- | --- | --- |
| MQ01 | Nova's Bunk west to Pod Row, south to Lift Shaft, west to Jed's Bunk; Workshop is a separate hub node | Player recognition of hub return; workshop crafting sequence |
| MQ02 | Hub Transit Checkpoint, Shift Queue north to Search Bay, east to Zeke's Booth; return west then north to Blast Door A | Full player clearance flow and dialogue branch acceptance |
| MQ03 | Transit Tunnel north to Admin Concourse; Elevator Lobby north to mine; Junction east through Conveyor/Sifter/Shoring, then Threshold west to Slope and north toward Echo | Trace event gates and required combat detours, not just shortest edges |
| MQ04 | Heart north to Emergency Exit, north to Maintenance Access, north to Cargo Lift | Lockdown/comms and lift event gates |
| MQ05 | Cargo Lift north through Security Checkpoint B to Pod Bay, Pod Core and Launch Rail | Warden and authorization/navigation event gates |

MQ03 follow-up: Boggs is conditionally present in Concourse Lobby between MQ02
and MQ03 completion. His post-training dialogue requires `ms_w1_guardbreak_trained`;
the final line dispatches `bogs_talked`, whose event sets
`admin_lobby.bogs_talked`. The Elevator Lobby's north gate checks that room flag,
not merely the similarly named milestone. Its unconditional base description
incorrectly announced green authorization. Changed it to neutral checking language,
retaining warning signs, scanner and cables. The journal now names Concourse Lobby
and the west-to-Elevator-Lobby/north-to-mine route. The badge gate remains upstream
at Blast Door A; no gate requirements were added or removed.

The mine's Bulwark objective is an encounter-victory event at `mine_checkpoint`,
north of Cavern Junction, while the onward corridor is east via `mine_conveyor`.
The journal now names this detour and Riot Control Post explicitly. Power-on
sets the restoration task and lighting flags; power-off can darken the route
again. Entering Ancient Threshold marks its task and advances toward relic sync.
These are inspected event definitions, not a played movement/combat sequence;
the new regression checks route/title mapping and neutral elevator prose only.
Next trace relic-sync prerequisites and lockdown/launch event sequencing.
Side-quest directions and
Worlds 2-6 route terminology remain unreviewed. No route or gate definitions
were changed in this checkpoint.

### Full validator inventory (2026-09-12)

The validator now reports all errors before exiting nonzero and counts rendered
NPC/action labels instead of hidden marker targets. The initial complete inventory
was 34 errors, zero warnings: 33 over-length descriptions and one duplicate.
All 33 over-length descriptions are now shortened, retaining their existing
action names and route hints. The ridge duplicate now has distinct prose.
No gameplay definitions or variant conditions changed.

| Room | Initial findings | Status |
| --- | --- | --- |
| spire_night_market | base 47; variant[0] 48 words | Shortened |
| spire_archive_vault | variant[0] 49 | Shortened |
| spire_prism_gallery | variant[0] 49 | Shortened |
| spire_drone_test_alcove | variant[0] 54 | Shortened |
| spire_landing_pad_roof | variant[0] 55 | Shortened |
| foundry_obsidian_overlook | variant[0] 47 | Shortened |
| foundry_cooling_springs | base 56; variants[0,2,3] 49,52,56 | Shortened |
| foundry_waste_intake | base 75; variants[0,1,2,3,4] 71,66,69,75,77 | Shortened |
| foundry_service_airlock | base 46; variants[1,2] 54,52 | Shortened |
| foundry_conveyor_belt | variant[2] 46 | Shortened |
| foundry_forge_anvil | base 56; variants[1,2,3] 51,67,60 | Shortened |
| foundry_power_core | variant[3] 46 | Shortened |
| foundry_titan_dock | variants[1,2] 47,48 | Shortened |
| orbital_executive_dock | base 50; variant[0] 50 | Shortened |
| orbital_server_farm | base 50; variants[0,1] 54,60 | Shortened |
| sector9_canopy_ridge | identical base and variant[1] | Distinct wording; compatibility variant retained |

The remaining 28 findings from the first batch are resolved: 22 World 4 lengths,
five World 5 lengths, and the ridge duplicate. The ridge compatibility variant
retains its priority and conditions: MQ04's milestone does not reconstruct absent
hunter flags, so it must still override the completed-drill variant. The existing
partial-save regression now asserts selection of this distinct variant. The 108
raw and 2,592 migrated-state cases continue to cover objective discoverability.

Strict narrative validation passes for 274 dialogue entries and 459 non-debug
rooms, with zero warnings. A structural comparison against HEAD confirms only
34 descriptions changed in rooms.json, with all previously named actions retained.
Do not treat validator success or the 22 classified
static action pairs as narrative acceptance. Next review World 1 route hints and
quest terminology, then Worlds 2–6. Preserve action references when shortening
copy and distinguish static graph checks from player route discovery.
