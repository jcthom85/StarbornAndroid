# Combat layout and demo controls - September 23, 2026

Shipped in Play internal release 1.3.53 (versionCode 137), commit `29b840d`, on September 23, 2026.

## Changes

- Combat now lays out the encounter header, fitted enemy formation, message slot and party in one vertical layout. The enemy formation uses the space remaining after the party is measured, instead of independently overlapping a bottom-anchored party.
- Smaller party portraits, tighter padding and spacing. One/two members use a centered row; three use a centered two-plus-one formation; four use two rows of two. Wide screens can use one row.
- Short screens and crowded encounters use compact crew cards with vitals beside portraits. Health/ATB bars remain visible. The enemy formation, including its labels, scales to fit available space.
- A small Demo button replaces the full-width toolbar. Guide, Resume and Finish are in its menu; combat pauses while that menu or guide is open. Combat reserves header space for the button; exploration places it at bottom left to clear the minimap.

## Evidence

- Debug and instrumentation builds pass.
- Demo navigation passed after relocating the exploration button (94.691 seconds): combat victory, fresh retry, guide, Astra arcade exit, Titan/Story launch, title return and campaign save preservation.
- Astra workbench check and all three recovery/pause checks passed during this pass.
- Formation check passed all 24 combinations on the final build (49.896 seconds): 1-4 party members, 560/720dp available heights, and 1/2/5 enemies. It checks visible portraits, formation separation, sprite bounds and centered solo/three-member rows.
- Evidence: local `test-results/combat-layout/` and its `burgquest-prep/` subdirectory. White space below height-limited test screenshots is outside the deliberately constrained combat surface.

Physical booth-device playtesting remains useful, especially font scaling and tap comfort. The checks do not claim coverage of every composite boss or display configuration.
