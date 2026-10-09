# Desktop field menu visual pass

Used Android ModernFieldMenu and the shared FieldMenuDesign palette as references.

- Menu shell: cyan/gold palette, layered gradients, etched grid and orbit lines, slow ambient signal lights, stronger heading and selected navigation treatment.
- Page transitions: 180 ms fade/short movement, disabled for high contrast, reduced flashes, or disabled screen shake. Signal animation is disabled for high contrast/reduced flashes.
- Party: larger original portrait, XP bar and character header; selector appears when there are multiple members, avoiding duplication for Nova alone.
- Attributes and skills: consistent outlined cards, skill state icons and authored names for matching prerequisite node IDs.
- Items: category glyphs replace identical diamonds. Existing item details/actions and category toggle retained.
- Tinker: assembly socket outlines and consistent panel treatment.
- Journal: outlined dark cards throughout quest detail/history/milestones/fishing.
- Map: subtle grid and outlined map/detail frames. Geometry and north-up orientation retained.
- Settings: bounded width, grouped audio panel, cyan sliders and section hierarchy.

Validation: Kotlin compilation and offscreen rendering succeeded. Visually reviewed six main pages, Attributes, Skills, and a 900x650 Party layout. Screenshots: desktopApp/build/reports/desktop/screenshots/menu-style. No gameplay test suite was run. Native interaction and audio playthrough remain outstanding.


## October 8: structural menu pass

- Loaded the original Android font bytes: Oxanium and Russo One headings, Source Sans 3 body and controls, with desktop-sized typography.
- Reduced shell header height; added Alt+Left for Back and restored keyboard focus after nested pages close. Existing Alt+1?6, Ctrl+Tab, Escape and F5 shortcuts remain.
- Party uses a portrait-led side panel on wide windows and a compact horizontal summary on smaller windows.
- Equipment slots, candidate lists and inspectors use Android item category/weapon artwork. Comparisons use aligned current/candidate values; Equip/Unequip remains visible outside the scrolling details.
- Inventory uses an adaptive artwork grid and persistent inspector. Arrow keys select entries and reveal offscreen items. Key items never offer field use; nonusable supplies no longer display a disabled action. Item target selection includes the original party portraits and current HP.
- Skills uses original row/column positions and prerequisites as a connected, scrollable tree, plus a selected-node inspector. Purchase state and tier/AP restrictions come from the shared controller. Arrow-key navigation keeps selected nodes visible.
- Journal adds quest-state labels, numbered stage markers and completion colors while keeping authored text intact.
- Map adds mouse-wheel zoom, center-preserving zoom controls, a current-room recenter action, and portrait previews for revealed rooms that are not dark. Original discovery rules and north-up geometry remain.
- Tinker shows item artwork in parts, assembly sockets and learned recipe previews. Scrap now has a selection list and a dedicated salvage inspector. Settings uses grouped columns on wide windows and stacks on smaller windows; save archive styling matches the menu.
- Feedback appears in a consistent cyan message band. The menu continues to use the original gold/cyan palette and subtle background signals, with reduced-flash and high-contrast handling.

Validation: Kotlin compilation and offscreen screenshots of all six top-level pages, Attributes, Skills, plus Party, Items and Skills at 900x650. Screenshots are in `desktopApp/build/reports/desktop/screenshots/menu-redesign`. Portable packaging verifies all original Android assets. Native popup interaction, extended game-state coverage and a full campaign playthrough are not established by these presentation checks.

The four-character Party layout was also rendered at 1440x900 and 900x650. Compact selectors, a shorter character header, conditional equipment search, and a fixed equipment action preserve space at the smaller size. Item, equipment, quest, map-room, skill, and scrap inspectors use visible cyan scrollbars.
