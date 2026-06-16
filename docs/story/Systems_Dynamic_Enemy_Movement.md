# Dynamic Enemy Movement

Status: Implemented as a reusable system with a balanced Deep Mine pilot.

## Purpose

Dynamic enemy movement makes Starborn's rooms feel more alive by allowing selected hostile parties to patrol across bounded areas. The system adds pressure and atmosphere without breaking authored quest gates, mobile readability, or the player's ability to read dialogue and room text.

## Core Rules

- Exit blockers, bosses, tutorial fights, and quest-gated encounters are stationary.
- Moving enemies must stay inside explicit movement zones; they should never wander into safe rooms, shops, core dialogue rooms, or rooms that would create progression softlocks.
- Movement state must persist across save/load if the system is active.
- Movement timers must pause while dialogue, menus, shops, inventory, quest popups, cinematics, tutorials, combat, combat rewards, and save/load overlays are open.
- Player agency matters: the player should be warned before an aggressive encounter starts.

## Behavior Types

| Behavior | Meaning | Use cases |
| --- | --- | --- |
| `stationary` | Enemy never changes room unless an event explicitly removes it. | Exit blockers, bosses, tutorial fights, story gates. |
| `patrol` | Enemy follows a small route or semi-random route inside a zone. | Security patrols, drones, mine creatures moving through tunnels. |
| `wander` | Reserved. Enemy moves within a zone without a fixed route. | Future wildlife and ambient hazards. |
| `hunter` | Reserved. Enemy pursues the player after being alerted. | Future rare pressure encounters. |
| `scripted` | Reserved. Enemy moves only through authored events. | Future quest-specific repositioning. |

## Aggression Levels

| Aggression | Combat trigger | Player expectation |
| --- | --- | --- |
| `passive` | Player must initiate combat. | Safe to inspect, read, and prepare. |
| `aggressive` | Enemy warns/pressures the player, then initiates combat after a grace period. | Clear warning, enough time to leave or engage. |
| `very_aggressive` | Five-second warning before combat. | Rare and clearly signaled; should feel dangerous, not cheap. |

Aggression should be per enemy party, not only per enemy type. The same enemy can be passive in a tutorial context and aggressive in a later zone.

## Player Signaling

Use layered feedback so movement is legible on mobile:

- Presence dock animation when a hostile party enters or leaves the current room.
- Short event banner such as "Footsteps echo from the east." or "A patrol moves on."
- Minimap or direction pulse when a hostile party is in an adjacent room.
- Audio cue for nearby movement or aggressive pursuit.
- Visible pressure indicator for aggressive encounters before combat starts.

Do not silently start combat from a hidden timer. If the player is reading or in a UI overlay, the system should wait.

## Data Shape Draft

Movement is authored in `app/src/main/assets/enemy_movement.json`:

```json
{
  "zone_id": "w1_deep_mine_side_loop",
  "behavior": "patrol",
  "aggression": "aggressive",
  "route": ["mine_conveyor", "mine_sifter", "mine_shoring"],
  "move_interval_seconds": 25,
  "engage_delay_seconds": 10,
  "signals": {
    "enter_room": "A patrol rounds the corner.",
    "leave_room": "The patrol moves deeper into the tunnel.",
    "adjacent": "Metal steps echo nearby."
  }
}
```

## Implementation Notes

- Treat enemy parties as the moving unit, not individual enemies.
- Movement should be owned by exploration/session state, not combat state.
- Room presence UI must derive from the current room snapshot after movement, combat victory, retreat, save/load, and quest state changes.
- Clearing a moving party in combat should remove that party from its movement zone and persist the cleared state.
- Retreat should leave the party alive and positioned according to explicit retreat rules.
- Maestro coverage should include: enemy enters current room, enemy leaves current room, aggressive warning pauses during overlays, combat victory removes only the defeated party, save/load preserves movement state, and stationary blockers never move.

## Runtime Rules

- Exploration time advances only while exploration is visible and no blocking overlay is open.
- Room changes reconcile the party's current position immediately; app suspension and offline time do not advance patrols.
- Aggressive parties initiate combat after 10 seconds. Very aggressive parties use 5 seconds.
- Retreat grants 15 seconds of grace before aggression can restart.
- Combat carries the stable movement-party ID, so victory removes only that party and retreat preserves it.
- Exact room, route direction, movement timer, aggression timer, retreat grace, and defeated state persist across save/load.
- Current-room entry/exit notices and adjacent-direction threat pulses tell the player where movement occurred.

## World 1 Pilot

The Pressure Hauler patrols `mine_conveyor -> mine_sifter -> mine_shoring` and reverses at each endpoint while `w1_mq03` is active. It moves every 25 seconds and uses aggressive timing. Deep Mine blockers, tutorial encounters, bosses, and progression gates remain stationary.
