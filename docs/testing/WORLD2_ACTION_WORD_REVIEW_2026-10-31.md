# World 2 action-word review — read only

October 31, 2026. User requested review/report only. No action names, descriptions, quest gates, events or room content were changed for this audit. Charlie/gate presentation fixes are tracked separately.

## Verdict

**The scan/gather/salvage naming pass was completed, but the broader inline-action standard is not fully consistent across World 2.** Review covered all 93 Sector 9 rooms and 164 authored room actions, including base prose, variants, explicit inline display aliases and the production action/visibility code. This is a code/data review, not a full phone replay of every quest state.

| Category | Count |
|---|---:|
| Generic | 121 |
| Inspect | 19 |
| Containers | 9 |
| Rest/Cook service definitions | 9 |
| Shops | 2 |
| Toggle, fishing, tuning puzzle, arcade discovery | 1 each |

Of the 140 generic/inspect definitions, 85 have no event and are passive inspections; 55 dispatch events. Functional behavior cannot be inferred from bold styling alone. Seven actions explicitly require an active quest for visibility.

## What is already consistent

- The six renamed labels are **moss, bioluminescent weeds, crystalline ferns, drifting spores, tideglass growth and power conduits**. Their original event IDs and quest/progression behavior remain.
- Root lattice already used an object name and also has explicit active-quest visibility. The other six follow the same visibility pattern, with additional progression gating for conduits.
- Rest and cooking use service controls. The two shop controls deliberately appear separately rather than needing bold shop names embedded in prose.
- Every non-service action name is referenced in at least one base/variant description or explicit action marker. This detects no obvious globally orphaned keyword; it does **not** prove every currently eligible action appears in every selected prose variant.
- **Look at Shield** is an internal action name. The actual rendered alias is **Planetary Shield**, so renaming that raw ID is not needed to fix the visible words.

## Remaining inconsistencies to discuss

1. **Resonant Canopy: “scavenge the remains.”** This still reads as an instruction in the paragraph. An object label such as “fallen scout” or “scout’s remains” would follow the newer noun-based pattern. Keep its Lost Patrol start/advance behavior.
2. **Canopy Ridge: “confront the stalker” and “face the Beast.”** These are live, stage-gated story/combat commands embedded in prose. They are not ordinary flavor inspections. Decide whether to keep deliberate command wording with an activity confirmation or present them as separate explicit actions. Do not mechanically convert them into innocuous-looking inspection nouns.
3. **Anchor Drill.** This is an activity name, not a decorative object. Its stage gating and completion retirement are already deliberate. It merits the same presentation decision as other major activities.
4. **Navigation exceptions.** “return to the Astra” and “salvage return route” are functional shortcuts. The latter is a noun phrase despite starting with “salvage.” These should be considered with navigation consistency, not accidentally treated as leftover harvesting verbs.

## More consequential feedback concern

**24 actions still carry the fallback “Objective completed.”** The generic action handler posts this status before dispatching the event. The event independently evaluates active quests, tasks and other prerequisites; it may refuse the trigger. Thus the fallback is not evidence that an objective actually completed and can be misleading when a visible action is not currently eligible.

Examples worth focused checks: patrol transceiver before its required beacon steps; security grid/guard-system bypass/transmitter case outside the optional vent quest’s eligible stage; story consoles before their required task. Some rooms/actions are protected by other progression gates, so the data alone does not establish how often a player encounters each case.

A related pattern: many event actions lack explicit active-quest visibility. That is **not automatically a bug**—main quests start in sequence, some controls have milestone/state gates, and the wing crystals deliberately provide inspection context before their quest. Each needs either purposeful context feedback or a clear eligibility rule. Blanket hiding would risk removing useful clues or quest starters.

## Recommended next pass — not implemented

Keep noun-based labels for passive inspections and resource objects. Treat major story/combat activities and navigation as deliberate exceptions with clear confirmations. Review the 24 placeholder fallbacks against event eligibility so every visible tap gives truthful feedback. Preserve passive read-state tinting and one-time pickup disappearance; functional controls should not be styled as exhausted flavor text merely because they were used once.

[Full action inventory and source hashes](evidence/WORLD2_ACTION_WORD_REVIEW_2026-10-31.json).

### Placeholder inventory

| Room | Action |
|---|---|
| Crash Site | navigation console |
| Crash Site | moss |
| Escape Pod Interior | emergency medkit |
| Tide Pools | bioluminescent weeds |
| Hidden Grotto | cache |
| Resonant Canopy | crystalline ferns |
| Spore Thickets | distress signature alpha |
| Poison Hollow | distress signature beta |
| Burned Glade | patrol transceiver |
| Canopy Ridge | Anchor Drill |
| Sniper Lookout | distress signature gamma |
| Temple Gate | stone lock |
| Hall of Echoes | murals |
| Stasis Chamber | stasis pod |
| Stasis Chamber | root lattice |
| Coolant Lines | coolant garden |
| High Gantry | security grid |
| Infiltration Hatch | guard-system bypass |
| Transmitter Site | case |
| Generator Controls | generator breakers |
| Purge Valve | root pressure gauge |
| Harmonic Horn | emitter memory |
| Resonance Cup | emitter service log |
| Hangar Bay | dark console |
