# Dynamic Enemy Movement

Status: Deferred future system. Do not treat this as a World 1 vertical-slice requirement.

## Purpose

Dynamic enemy movement could make Starborn's rooms feel more alive by allowing some hostile parties to patrol, wander, or pursue the player across bounded areas. The system should add pressure and atmosphere without breaking authored quest gates, mobile readability, or the player's ability to read dialogue and room text.

For the current World 1 vertical slice, enemy placement should remain deterministic. Use room text, audio, and event flavor to imply nearby patrols instead of implementing live roaming enemies.

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
| `wander` | Enemy moves within a zone without a fixed route, avoiding protected rooms. | Wildlife, malfunctioning machines, ambient hazards. |
| `hunter` | Enemy can follow or pursue the player after being alerted. | Rare pressure encounters, late-game threats, stealth/pursuit sequences. |
| `scripted` | Enemy moves only through events or quest state. | Cutscene beats, quest-specific repositioning. |

## Aggression Levels

| Aggression | Combat trigger | Player expectation |
| --- | --- | --- |
| `passive` | Player must initiate combat. | Safe to inspect, read, and prepare. |
| `aggressive` | Enemy warns/pressures the player, then initiates combat after a grace period. | Clear warning, enough time to leave or engage. |
| `very_aggressive` | Shorter grace period, possible pursuit within the movement zone. | Rare and clearly signaled; should feel dangerous, not cheap. |

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

This is a conceptual shape, not an implementation contract:

```json
{
  "movement_zone": "w1_deep_mine_patrols",
  "behavior": "patrol",
  "aggression": "aggressive",
  "route": ["mine_landing", "mine_alpha", "mine_junction"],
  "safe_rooms": ["mine_elevator"],
  "stationary_when_blocking_exit": true,
  "move_interval_seconds": 25,
  "engage_delay_seconds": 8,
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

## World 1 Decision

Do not implement dynamic enemy movement for the current World 1 vertical-slice goal. World 1 should prioritize stable quest progression, deterministic encounter placement, clear room presence, and reliable save/load. Revisit this system after World 1 is playable start-to-finish and stable.
