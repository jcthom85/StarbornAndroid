# Desktop field menu

Implemented October 7, 2026, using Android's modern field menu as the reference.

## Layout and navigation

- The menu is an overlay inside the game window, with the exploration artwork dimmed behind it.
- Android destination names and order: Party, Items, Tinker, Journal, Map; Settings is separate.
- Left navigation becomes compact in narrower windows. Content uses the available width without redundant section shells.
- Header shows the current page, authored room title, credits (or skill AP), quick save and close.
- Generic Menu opening uses the shared controller's first-open Party page, remembered destination and tutorial routing. I explicitly opens Items; minimap and quest shortcuts open their destinations.
- Alt+1 through Alt+6 changes pages; Ctrl+Tab cycles them, Shift reverses; F5 quick saves. Search fields retain ordinary number input.
- Escape leaves skill/attribute details before closing the menu. The exploration keyboard handler yields to the menu.

## Pages

- Party: authored portraits, shared HP/level/XP, attributes, skills and equipment. Equipment uses slot selection, compatible candidates and selected-item comparison; all equip/unlock checks remain in the shared runtime.
- Skills: branch selector and authored node details inside the menu, including AP costs, unmet requirements and shared purchase eligibility.
- Items: searchable/sortable list and scrollable details; equipment management moved to Party. Party portrait selection follows Android by opening that character's Party page. Key items are inspection-only. Existing item target and meal replacement flows remain.
- Tinker: Workbench, Schematics and Scrap. The workbench presents compatible materials beside assembly sockets and results. Tutorial directions use Android's modern menu wording. Crafting, discoveries, requirements and scrapping still use CraftingController.
- Journal: original authored summary/description/flavor, stage text and objectives, with tracking and history.
- Map: discovered rooms only, north up, drag/scroll panning, scrollbars, zoom/reset, and selected-room details. Narrow windows place details below the map.
- Settings: audio, display, tutorials, flashes, screen shake, high contrast, larger controls, keyboard help, manual saves and save-and-return to title. Loading another game continues through the title screen.

Page search and selection state uses Compose saveable state while changing destinations. No game data, quest copy or gameplay rules were invented for this redesign.

## Build and review

Desktop compilation succeeded. Portable packaging includes the Android asset parity check. No automated tests were added or run for this pass.

Native visual/playtesting still needs to cover window resizing, longer equipment and item descriptions, populated party/skill trees, tutorial repair, save feedback and map dragging.
