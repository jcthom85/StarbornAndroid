# Three-panel exploration

The subsequent [narrative hierarchy and independent HUD pass](INDEPENDENT_EXPLORATION_HUD_PASS.md) replaces the full-height enclosing cards described here.

## Layout

The exploration screen uses the portrait backdrop's actual bounds:

- Left: room title, interactive description, contextual controls, and transient feedback.
- Center: the full sharp portrait image, with available directional controls at its edges.
- Right: discovered-area minimap, tracked objective, party health, optional hub return, and Menu.

Side panels have 20dp outer padding, 16dp clearance from the image, and a maximum width of 360dp. Three columns require at least 240dp of usable space on each side. Wider panels align toward the art; their content scrolls independently.

Narrow windows use collapsible Room and Status overlays. Navigation sits along the bottom so the open narrative cannot cover it. Menu remains accessible in the top row.

## Interactions

The duplicate “In this room” card, Room exits card/toggle, and full-width shortcut bar are removed. Room interactions already represented in clickable prose are omitted from the contextual controls. Remaining actions, NPCs, enemies, and ground loot retain their runtime callbacks and visibility/lock rules. Non-cardinal routes remain available in the narrative panel.

The minimap opens Map; the objective opens Journal. The Menu button opens the existing field menu, with Controls available in its header. Existing keyboard shortcuts remain available. Cinematics, dialogs, and fades remain above the exploration layout and block interactions as before.

Minimap connections show locked routes in orange, discovered rooms are distinguished from visited rooms, and the current room is highlighted. Party status uses the existing portraits and health data; missing data is omitted.

## Verification

Automated coverage includes four aspect ratios, image/panel bounds, narrow overlay switching, minimap and objective links, a full party, long narrative scrolling, unmatched interaction deduplication, locked actions, special exits, darkness, and blocked navigation. Existing opening, actionable prose, dialogue, cinematic, and portrait backdrop regressions are included in the pass.

All 14 selected checks passed with no failures. Keyboard focus is restored on the frame after blocking overlays close and on room changes; the live opening regression verifies F5 quicksaving after travel.

Screenshots: `desktopApp/build/reports/desktop/screenshots/three-panel/`, plus the live opening screenshots in the parent folder. Physical audio output and native monitor DPI still require local playtesting.

The rebuilt packaged launcher passed its smoke check from outside the repository: 1,327 assets, 477 rooms, 4 characters, bundled Java 21.0.12.1, exit code 0.
