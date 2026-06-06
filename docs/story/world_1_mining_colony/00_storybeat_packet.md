# World 1 Storybeat Packet

**Status:** Build Prep
**Purpose:** This is the working bridge between the World 1 story bible and implementation data. Use it to author `quests.json`, `events.json`, `dialogue.json`, `cinematics.json`, room actions, encounters, rewards, and art prompts.

World 1 should be built as the first full production slice. Do not expand World 2 content until this packet is playable from Nova's bunk through the crash interlude.

---

## 1. Player Experience Target

World 1 is not "the mine dungeon." It is Nova losing the only life she has ever known.

The player should first understand the colony as home, then understand it as a cage, then watch it become a machine trying to kill Nova. The Source should not feel like a power fantasy yet. It is a wound, a signal, and a problem.

**Entry state**
- Party: Nova solo.
- Items: starter weapon/tool, minimal credits, no Source knowledge.
- Emotional state: tired, trapped, but still operating inside normal life.
- World state: Dominion is the boss, not yet the explicit enemy.

**Exit state**
- Party: Nova + Zeke.
- Items: Tuning Fork, Chime / Ghost Signal Cell, Mine Access Badge.
- Abilities: Nova has Blast Wave. Zeke has Signal Jammer, but it is not tutorialized until World 2.
- Emotional state: Jed is dead, Nova is a fugitive, Zeke is a defector.
- World state: Dominion is hunting them. The Planetary Shield exists. The pod crashes in Sector 9.

---

## 2. Non-Negotiable Story Beats

These beats define World 1. If a build cut is needed, cut flavor rooms before cutting these.

| Order | Beat | Function | Required Data |
| --- | --- | --- | --- |
| 0 | Prologue: The Signal | Establish the Chime, Orion, Source danger, Sector 9 mystery. | Cinematic only. Can ship before New Game or as later unlock if needed. |
| 1 | Nova wakes in the Pit | Establish home, poverty, Dominion language, movement/interactions. | `w1_mq01`, `pit_nova_bunk`, onboarding tutorial. |
| 2 | Jed warns Nova | Establish surrogate father, cutter risk, quota pressure. | Jed dialogue, Workshop route, first reward/item. |
| 3 | Hub 1 opens | Let the player meet the colony and learn systems. | Three side quests, shop/med flavor, room actions. |
| 4 | Zeke volunteer beat | Plant Zeke before he saves Nova. | `EVT_W1_00`, early Hub 1 trigger. |
| 5 | Heavy Lifting training | Teach Guard Break/Stagger before shield enemies. | `w1_sq03`, Hydraulic Kick, shield training encounter. |
| 6 | Paperwork death warrant | Reveal Nova is marked for recycling; Zeke defects morally. | `w1_mq02`, Zeke scene, Mine Access Badge. |
| 7 | Bogs sends Nova to Sector 4 | Tie Nova's hustle to punishment and plot movement. | `w1_mq03`, Bogs dialogue, Deep Elevator gate. |
| 8 | First dungeon descent | Shift from social hub to dangerous investigation. | Deep Mine encounters, Riot Guard tutorial, optional SQ05. |
| 9 | Tuning Fork sync | Source reveal, memory cost, Blast Wave unlock. | `EVT_W1_01`, Echo Chamber cinematic, reward action. |
| 10 | Lockdown | Turn known spaces hostile; introduce Warden and distant PA voice. | `EVT_W1_02`, hostile state, new enemies, Zeke comms. |
| 11 | Zeke guides escape | Turn Zeke from clerk into active ally. | `w1_mq04`, comm dialogue, locked doors/gauntlet. |
| 12 | Jed sacrifice + Chime | Emotional cost and World 2 connective tissue. | `EVT_W1_03`, Cargo Lift scene, Chime item. |
| 13 | Warden boss | Combat exam: shield, drones, Blast Wave payoff. | `w1_mq05`, boss encounter, victory event. |
| 14 | Pod launch and crash | Close World 1, introduce Shield and Orion signal. | `EVT_W1_05`, crash cinematic, world transition. |

---

## 3. Main Quest Flow

### W1_MQ01 - Wake Up Call

**Promise:** This place is miserable, but it is Nova's life.

**Playable sequence**
1. Start in `pit_nova_bunk`.
2. Teach room text, tappable words, movement, and save/rest at bunk.
3. Move through Pit rooms toward `workshop_floor`.
4. Talk to Jed.
5. Equip starter weapon/tool.
6. Use tinkering table once.
7. Jed gives a small kindness item and warns about the rigged cutter.
8. Quest completes and opens Hub 1 exploration.

**Story beats**
- Nova is behind quota.
- Her cutter/drill is illegally modified.
- Jed knows and hates it because it will get her killed.
- Nova hears "safety" as "waiting around to die."
- Dominion language should appear everywhere: Asset, Contribution, Retirement, Compliance.

**Required implementation anchors**
- Rooms: `pit_nova_bunk`, `pit_L2_corridor`, `pit_shaft`, `pit_L1_landing`, `workshop_yard`, `workshop_floor`.
- NPCs: Jed.
- Events: quest start, quest task updates, first autosave, optional first tutorial prompts.
- Reward: starter item, small healing/comfort item, Hub 1 exploration milestone.

### W1_MQ02 - Paperwork

**Promise:** The system did not get angry. It did math.

**Playable sequence**
1. Player approaches `checkpoint_queue` / `checkpoint_booth`.
2. Gate Guard denies access without clearance.
3. Zeke recognizes Nova if `EVT_W1_00` has played.
4. Zeke checks her file.
5. Console reveals: Mandatory Retirement / Asset Liability exceeds projected value.
6. Short paperwork spoofing interaction: recode surge as Grid Instability, not Operator Error.
7. Badge prints.
8. Camera logs Zeke's breach.
9. Player receives Mine Access Badge.
10. Transit to Hub 2 becomes available.

**Story beats**
- The rigged cutter power surge caused Nova's file to be flagged.
- Zeke has processed recycling orders before without thinking about the person.
- This is not a heroic speech. It is one frightened clerk refusing one murder.
- Zeke becomes a loose end the moment he saves Nova.

**Required implementation anchors**
- Rooms: `checkpoint_queue`, `checkpoint_booth`, `checkpoint_door`, `checkpoint_tunnel`.
- NPCs: Guard Hank or equivalent, Zeke.
- Events: `EVT_W1_04` style scene, give `mine_access_badge`, complete quest, unlock `hub_2_logistics`.
- Tutorial: Hacking/minigame interaction.

### W1_MQ03 - The Echo

**Promise:** The mine was built over something older than the company.

**Playable sequence**
1. Enter Hub 2 public area.
2. Bogs intercepts Nova and sends her to Sector 4 as punishment.
3. Use Mine Access Badge at Deep Elevator.
4. Descend into Deep Mine.
5. Fight common enemies: Rock-Borer, Security Drone, Fume Bat.
6. Fight first Riot Guard or shielded enemy; require Guard Break/Stagger.
7. Optional side paths support SQ04/SQ05.
8. Reach Architect transition rooms.
9. Interact with Tuning Fork in Echo Chamber.
10. Play sync cinematic.
11. Give Tuning Fork and Blast Wave.

**Story beats**
- Bogs reframes Nova's survival hustle as company damage.
- The tunnels slowly stop looking human-made.
- Dominion bolted warnings onto ancient geometry it does not understand.
- The Source is introduced as pressure and resonance, not magic.
- Nova briefly loses Jed's name. This seeds the finale cost.

**Required implementation anchors**
- Rooms: `admin_window`, `admin_elevator`, `mine_landing`, `mine_alpha`, `mine_checkpoint`, `mine_threshold`, `mine_antechamber`, `echo_heart`.
- NPCs: Foreman Bogs.
- Enemies: Rock-Borer, Security Drone, Riot Guard, Fume Bat.
- Events: relic sync, skill unlock, quest advance, lockdown trigger.
- Tutorial: combat basics, targeting, cooldowns, shield break, Source Art acquisition.

### W1_MQ04 - Red Alert

**Promise:** The home hub is no longer safe.

**Playable sequence**
1. Lockdown begins immediately after the sync.
2. Warden broadcast declares biological contaminant / asset purge.
3. Distant PA voice overrides Warden with colder corporate logic.
4. Zeke contacts Nova over comms.
5. Player moves through restricted escape route and hostile spaces.
6. Zeke opens doors, warns about guards, and panics less as he acts.
7. Nova reaches Cargo Lift.
8. Jed appears and forces the lift open.
9. Jed gives Nova the Chime / Ghost Signal Cell.
10. Jed stays behind and seals the route.
11. Launch Bay path opens.

**Story beats**
- The player should feel the previous hub has been inverted.
- Warden is loud and brutal.
- The PA voice is calm and worse.
- Zeke's line shifts from compliance language to direct survival talk.
- Jed's death is not abstract. It is a parent buying Nova seconds.

**Required implementation anchors**
- Rooms: escape route from Echo/Deep Mine back toward `launch_lift`.
- NPCs: Zeke as comm voice, Jed, Warden broadcast.
- Enemies: Dominion Enforcer, Security Drone, Riot Guard, Heavy Loader.
- Events: `EVT_W1_02`, `EVT_W1_03`, give `ghost_signal_cell`, open Launch Bay.
- Tutorial: chase/gauntlet pacing, limited detours, point-of-no-return warning before final launch.

### W1_MQ05 - The Launch

**Promise:** Nova can escape the colony, but not the planet.

**Playable sequence**
1. Fight through Launch Bay approach.
2. Enter Pod Bay.
3. Warden arrives.
4. Boss teaches final version of World 1 combat language: shield, drones, telegraphs, Blast Wave windows.
5. Warden defeated.
6. Zeke reaches pod or reveals he has hacked access.
7. Chime is spliced into pod core.
8. Pod launches.
9. Auto-nav locks onto unknown signal.
10. Planetary Shield impact.
11. Crash into Sector 9.

**Story beats**
- Zeke physically leaves the booth/cubicle life behind.
- The Chime works, but it is not normal tech.
- The Shield establishes the larger prison.
- The nav lock creates the World 2 question: whose signal?

**Required implementation anchors**
- Rooms: `launch_access`, `launch_gantry`, `launch_fuel`, `launch_checkpoint`, `launch_bay`, `launch_pod`, `launch_rail`.
- NPCs: Zeke, Warden.
- Enemies: Warden boss, drone adds.
- Events: boss victory, add Zeke to party, pod cinematic, transition to crash interlude.
- Tutorial: boss telegraphs. Do not fully tutorial Signal Jammer until World 2.

---

## 4. Side Quest Roles

Side quests are not filler. Each one gives a meaningful tool and a World 1 theme beat.

| ID | Title | Required Before Hub 2? | Primary Job | Reward | Core Room Anchors |
| --- | --- | --- | --- | --- | --- |
| `w1_sq01` | The Scavenger's Stash | No | Show failed resistance and Scrapper's paranoia. | Pulse Grenade | `trade_scrapper`, `trade_stash`, `trade_locker` or `trade_maint`. |
| `w1_sq02` | System Flush | No | Show Dominion neglect and teach status/Corrosion. | Corrosive Rounds | `medbay_exam1`, `medbay_vents`, `medbay_exhaust`. |
| `w1_sq03` | Heavy Lifting | Yes, soft-mandatory | Teach Guard Break/Stagger and show Bogs' priorities. | Hydraulic Kick | `workshop_dock`, loader, cargo rail, riot shield trainer. |
| `w1_sq04` | Protocol Override | No | Show earlier rebels failed but left useful code. | Admin Access passive | `server_airlock`, `server_hub`, `server_backup`. |
| `w1_sq05` | The Lost Shift | No | Show the employment contract was always a cage. | Recoil Dampener | `mine_shunt`, `mine_junction`, `mine_shoring`. |

**Gate rule:** `w1_sq03` should either be mandatory or treated as mandatory by the fiction. The player should not meet the Warden without knowing that shielded enemies need Guard Break/Stagger.

---

## 5. Beat-To-Data Matrix

Use this table as the first pass when authoring implementation data.

| Beat ID | Quest | Trigger Type | Trigger Anchor | Event/Cinematic Need | Data Actions |
| --- | --- | --- | --- | --- | --- |
| `w1_beat_001_wake` | `w1_mq01` | new game / enter room | `pit_nova_bunk` | optional fade-in | start quest, track quest, tutorial movement. |
| `w1_beat_002_bunk_interact` | `w1_mq01` | player action | bunk hotspot | no | set task done, tutorial actionable words/save. |
| `w1_beat_003_jed_intro` | `w1_mq01` | talk_to | Jed | dialogue | task done, give starter item, unlock tinkering. |
| `w1_beat_004_tinkering_done` | `w1_mq01` | player action | tinkering table | optional tutorial cinematic | task done, complete quest, set Hub 1 open milestone. |
| `w1_beat_005_volunteer` | none / flavor | enter room | bulletin/checkpoint public room | short cinematic/dialogue | set `ms_w1_met_zeke_volunteer`. |
| `w1_beat_006_sq03_training` | `w1_sq03` | encounter victory | shield trainer | no | give Hydraulic Kick, set `ms_w1_guardbreak_trained`. |
| `w1_beat_007_checkpoint_denied` | `w1_mq02` | enter room / player action | `checkpoint_booth` | dialogue | start/advance quest. |
| `w1_beat_008_zeke_override` | `w1_mq02` | player action | paperwork spoof | cinematic/dialogue | give Mine Access Badge, set Zeke breach milestone, complete quest. |
| `w1_beat_009_bogs_sector4` | `w1_mq03` | enter room / talk_to | `admin_window` or `admin_office` | dialogue | start/advance quest, mark Sector 4 objective. |
| `w1_beat_010_deep_elevator` | `w1_mq03` | player action | `admin_elevator` | no | require Mine Access Badge, begin Deep Mine node. |
| `w1_beat_011_first_riot_guard` | `w1_mq03` | encounter start/victory | `mine_checkpoint` | no | tutorial shield break, mark task done. |
| `w1_beat_012_relic_sync` | `w1_mq03` | player action | `echo_heart` | required cinematic | give Tuning Fork, unlock Blast Wave, complete quest. |
| `w1_beat_013_lockdown` | `w1_mq04` | quest complete / enter room | post-sync route | required cinematic | start Red Alert, set lockdown milestone, spawn hostile state. |
| `w1_beat_014_zeke_comms` | `w1_mq04` | enter room | escape route | dialogue barks | open doors, update objectives. |
| `w1_beat_015_jed_sacrifice` | `w1_mq04` | enter room | `launch_lift` | required cinematic | give Chime, complete Red Alert, open Launch Bay. |
| `w1_beat_016_warden_intro` | `w1_mq05` | enter room | `launch_bay` | boss intro | start boss encounter. |
| `w1_beat_017_warden_defeated` | `w1_mq05` | encounter victory | Warden | victory cinematic | mark boss defeated, add Zeke, advance quest. |
| `w1_beat_018_launch_crash` | `w1_mq05` | player action | `launch_pod` | required cinematic | complete World 1, set transition/crash state. |

---

## 6. Dialogue Priorities

Write only the dialogue needed to make the first playable pass coherent. Flavor barks can follow later.

**Priority A - blocking dialogue**
- Jed first workshop conversation.
- Zeke paperwork scene.
- Bogs Sector 4 order.
- Relic sync text/narration.
- Warden lockdown broadcast.
- Zeke comms escape barks.
- Jed sacrifice.
- Warden boss intro.
- Pod launch/crash exchange.

**Priority B - side quest dialogue**
- Scrapper stash intro/turn-in.
- Doc vent problem intro/turn-in.
- Bogs Heavy Lifting intro/forced training.
- Hacked terminal SQ04 text.
- Dead miner datapad SQ05 text.

**Priority C - flavor**
- Sam and Ellie meal barks.
- Miner barks in Pit.
- Trade Row paranoia/rumors.
- Med-Bay patient barks.
- Dominion PA slogans.

---

## 7. Encounter Progression

World 1 combat should teach one readable language: break the guard, punish the opening, manage cooldowns.

| Phase | Enemies | Lesson |
| --- | --- | --- |
| Hub 1 training | Loader dummy / shield trainer | Guard Break opens damage windows. |
| Deep Mine early | Rock-Borer, Fume Bat | Basic attacks, AoE, status feedback. |
| Deep Mine tech | Security Drone, Dominion Enforcer | Target priority and low-risk ranged enemies. |
| Deep Mine shield | Riot Guard + support | Some enemies take little or no damage until broken. |
| Escape gauntlet | Mixed guards/drones/heavy loader | Apply lessons under pressure. |
| Warden | Warden + drone summons | Final exam: break shield, read telegraphs, use Blast Wave. |

Do not build World 1 around Jammed or Marked. Zeke joins too late. Those are World 2 lessons.

---

## 8. Implementation Order

Build in this order to get to a playable story fastest.

1. Normalize World 1 quest data into `stages` / `tasks`.
2. Add five side quests to quest data, even if their events are stubbed.
3. Clean active event/dialogue IDs so World 1 canon beats are easy to find.
4. Wire `w1_mq01` from bunk to Jed to tinkering completion.
5. Wire `w1_mq02` checkpoint and Zeke override.
6. Wire `w1_sq03` Guard Break training before Hub 2.
7. Wire `w1_mq03` Deep Mine path and Tuning Fork sync.
8. Wire lockdown state and Red Alert escape.
9. Wire Jed sacrifice and Chime handoff.
10. Wire Warden boss victory and launch/crash.
11. Fill the other side quests.
12. Fill flavor barks and optional rooms.
13. Generate/assign missing room background art.

---

## 9. Current Data Risks To Resolve Before Heavy Authoring

- Active `quests.json` currently uses legacy `steps`, while the Kotlin model and schema expect `stages` and `tasks`.
- Current active `events.json`, `dialogue.json`, and `cinematics.json` still include old vertical-slice/demo content. Keep useful patterns, but do not let old town/Ollie/VFX IDs define World 1 canon.
- The current room graph exists, but most room background image paths do not have files yet. Treat room art as a second pass after the storybeat path is locked.
- NPC data is thinner than the story docs. Add only the cast needed for World 1 first: Jed, Zeke, Scrapper, Doc, Bogs, Guard, Warden/PA voice, Sam, Ellie, a small miner set.
- World 1 should be complete before broad World 2 implementation, because World 2 depends emotionally and mechanically on Jed's death, the Chime, Zeke's guilt, and the Shield crash.
