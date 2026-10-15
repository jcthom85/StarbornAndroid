# Sector 9: implemented story reference

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

## Identity and actual sequence

Sector 9 contrasts the colony with jungle, coastline, ruined facilities, and the practical task of getting a ship airborne. The current route starts with Nova and injured Zeke. Zeke joined during the colony launch; this is not a solo-Nova start.

1. Check Zeke, examine the pod, recover supplies/Chime, spend the emergency medkit, and reach the stream.
2. Clear an accepted canopy encounter, reach Temple Gate, and authenticate with the Chime.
3. Investigate Sanctuary, read the stasis clues, stabilize coolant, resolve pressure/memory/return choices, and speak to Orion. Claim the separate Bridge afterward.
4. Return to Canopy Ridge for Gh0st and the Beast. Beast victory recruits Gh0st. The subsequent Anchor Drill cinematic callback unlocks Link and finishes The Hunter.
5. Resolve the Source Gate, inspect the Astra, recover conduits at Pod Fragment, install/power the Bridge, and confirm departure.
6. The departure scene follows a low service airlane beneath the outer Shield, then makes a hard landing beneath the Spire. It does not launch into orbit and bounce off the outer Shield.

## Where the logic lives

`00_quest_list.md` owns the concise quest/reward map. `00_storybeat_packet.md` maps crucial event/callback ownership. Hub quest, cast, and location files are source references rather than independent scripts. Room/hub placement is not quest chronology: The Hunter belongs to the jungle hub but follows Orion's facility quest.

The Chime authenticates; reserve cells and ship systems supply power. The Bridge is a separate inventory item, later consumed when installed. Gh0st's protective choice remains voluntary. Actual dialogue and cinematics are in the production catalogs; the old repeated weapon-failure and protection-directive scripts are retired.

## Puzzles and side content

The stasis and Source Gate sequences are dialogue-choice problems backed by clue milestones and success actions. Ancient Echoes uses three collected/seated crystals and a socket-choice dialogue. These are distinct from the optional three-slider bioluminescent-prism tuning definition. Do not describe all World 2 puzzles as sliders or restore the old C#m7 specification.

Lost Patrol repairs a Thermal Cutter; Tideglass Day crafts Source Resin; Stolen Tech crafts a Rapid Capacitor. Ancient Echoes grants a Focus Conduit, not Chorus Echo. Botanist grants XP and stabilizers, not a beast-critical passive.

## Editorial direction and retired claims

Retain beauty, physical danger, displacement, recovery, and emerging trust. The source still includes acoustic names and dense Source prose; this reference does not claim that prose has been rewritten. Future editing should describe observable machinery, terrain, wounds, and choices, using occasional Source imagery.

Retired instructions: persistent Erosion survival management, compulsory neural-stabilizer snack tutorial, fixed self-only five-turn snacks, Gh0st before Orion, Link at the temple gate, forced protection orders, and a second Shield-crash ending. They are old documentation claims, not fixes to apply to the current game.

## Sources and maintenance

[rooms](../../../app/src/main/assets/rooms.json), [quests](../../../app/src/main/assets/quests.json), [events](../../../app/src/main/assets/events.json), [dialogue](../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
