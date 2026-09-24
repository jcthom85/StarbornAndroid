# Hub map visual pass — September 24, 2026

Implemented locally; not versioned, committed, or released by this pass.

## Layout rules

The map background and destination anchors now share one aspect-preserving transform. Coordinates refer to the original image, with an authored vertical crop per hub. Destination card selection cannot shift the map. Header, quest, return control, and bottom card reserve space outside the map.

Use proportionally sized artwork on empty ground/pads, but small labeled markers when the background already depicts the destination. Labels retain selection, quest, locked, and completed coloring; landmark markers also show state icons. Single tap selects; double tap or the destination card enters. Accessibility label actions select the destination.

The Astra remains gated by the existing game state. Show its ship on plausible docks in Logistics, Upper City, and Orbital Ring. Other maps use a separate ship-and-name return control when access is available, rather than parking a spacecraft inside buildings or on unsuitable terrain. Existing travel/boarding/disembark rules are unchanged.

## Individual hub decisions

| Hub | Treatment |
| --- | --- |
| Homestead | Individually sized buildings grounded on their respective open pads. |
| Logistics | Painted administrative, mine, and launch landmarks; fitted server/echo artwork; Astra on the lower open apron. |
| Sector 9 | Fitted crash, vine, and temple artwork; terrain markers for beach and canopy. Taller crop retains the temple. |
| Facility | Markers on painted rooms and entrances, fitted hangar artwork. |
| Lower City | Markers on existing industrial structures and entrances. |
| Upper City | Markers on painted destinations; Astra on the right landing platform; displaced landing label has a connector. |
| Slag Pits | Markers on terrain/industrial landmarks instead of duplicate structures. |
| Assembly Line | Markers on the painted machinery and facility areas. |
| Orbital Ring | Clear the previously painted shuttle, place interactive Astra on that berth, and mark painted destinations. |
| Deep Ring | Markers on the server, anchor, throne, and tear landmarks. |
| Event Horizon | Individually placed markers on the distinct illustrated islands/areas. |
| Singularity | Markers on the path, spire, center approach, and distant exit; keep the main light unobscured. |
| Astra | Interior and disembark anchors on the ship map; no redundant return control. |

## Verification

- 10 focused unit tests passed: HubMapLayoutTest (2), HubAstraTravelTest (3), AstraTravelTest (5).
- Two emulator instrumentation tests passed: HubMapVisualAuditTest and AstraTransitNavigationTest.
- Visual audit renders all 13 hubs in five configurations: normal, compact, large text, locked destinations with quest header, and before Astra availability (65 captures).
- Audit checks destination visibility and stable marker bounds while changing selection. Transit test exercises actual travel, hub arrival, and reboarding.
- Reviewed all normal hub compositions during iteration and representative stress layouts; final dock corrections inspected in Upper City and Orbital Ring.
- Final build log: `test-results/hub-map-final-tests.log`.
- Final captures: `test-results/hub-map-final/starborn-hub-map-audit/` (older parent-directory captures are intermediate iterations).
- Audit fixtures deliberately expose destinations and use sample description text; they are layout tests, not proof of every campaign progression state. Compact and large-text configurations are simulated on the phone emulator, not a physical-device matrix.
- `git diff --check` passed.

## Generated art provenance

Used the imagegen skill's API/CLI mode, as requested, with `gpt-image-2`, high quality, and the local API key (never recorded here). Original assets remain intact.

Final assets:

- `world_assets/src/main/assets/images/nodes/astra_ship_map.webp`: 1024-square ship edit derived from `images/nodes/world_2/hangar_bay.webp`; magenta removed using the bundled chroma-key utility with soft matte/despill.
- `world_assets/src/main/assets/images/hubs/hub_9_orbital_ring_clear.webp`: 1088 × 1920 sibling background edit, selected in `hubs.json`.

### Ship prompt

```text
Use case: precise-object-edit
Asset type: isometric mobile RPG world-map ship sprite
Image 1 is the edit target and identity reference: the Astra shuttle in its hangar.
Extract and redraw ONLY the repaired ship, preserving its recognizable elongated pointed cockpit, worn ivory-gray hull, dark cockpit windows, angular twin rear engine shoulders and panel geometry. Remove the entire hangar, gantry, platform, stairs and all surrounding machinery. Repair the hull damage without making it glossy or pristine. Landed stance, subtle landing feet, nose pointing lower-left, three-quarter elevated isometric perspective matching the reference. Entire ship visible, centered with a tight but uncropped silhouette. Hand-painted science-fiction RPG illustration with crisp dark outlines and muted warm-gray shading, readable at small size. Flat pure magenta #FF00FF background for chroma-key extraction; no magenta on the ship, no ground plane, no cast shadow, no glow, no text, no border, no extra objects.
```

### Orbital dock prompt

```text
Use case: precise-object-edit
Asset type: existing portrait mobile RPG hub background
Image 1 is the edit target. Remove ONLY the small white-and-gold shuttle parked on the left-hand projecting docking platform at approximately x=20%, y=40%. Reconstruct the empty ivory docking platform under the ship, matching the gold edging, painted shading and isometric perspective. Preserve the platform itself, garden at upper center, security door, tall service shaft, ring architecture, planet, stars, every walkway and the exact overall composition and framing. Do not add a replacement ship. Do not add text or new objects. This empty berth will receive a separate interactive Astra ship sprite in the game.
```
