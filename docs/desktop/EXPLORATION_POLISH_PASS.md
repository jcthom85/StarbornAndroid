# Exploration polish pass

## Presentation

- Original portrait artwork, reflected feathered edges, and stationary reflection crossfades remain the scene foundation.
- Both wide HUD areas anchor beside the artwork; widths remain bounded to 400 dp left and 300 dp right. Three-panel mode still requires 276 dp of side space.
- Compact room/status drawers start closed, support explicit Close, and close before the menu when Escape is pressed. Tab now moves focus; the Controls guide reflects this.
- Shared original Oxanium, Russo One, and Source Sans typography is extracted for use by menus and exploration. Room accents come from the shared theme.
- Context actions have icons and framed rows. Party rows are compact, with shorter portraits/rows on small windows. Overflow is represented by visible scrollbars.
- Minimap symbols and spacing are larger, outer padding is reduced, and hover descriptions use shared runtime information. Dark maps retain interference without topology or a click action.

## Atmosphere

The user requested full-screen weather after reviewing the plan. Weather therefore spans both the artwork and reflected sides, with HUD above it. Only authored room weather is enabled; the shared environment default remains null. Existing desktop effects handle rain, storm, snow, dust, cave drip, starfall, and sparks. Steam, fog, gas, and resonance were ported from Android; steam uses the original 48 sprite frames. Rain thickness is scaled to the portrait width while coverage stays full screen.

Screen-shake suppression removes directional room travel and description sliding, while preserving a stationary crossfade. Flash suppression removes storm lightning and quest progress flashes. High contrast and larger-target settings are applied to the exploration presentation.

## Inputs and dialogs

Hover and focus outlines are added without additional click/focus targets. Inline prose preserves original action bindings. Description and context scroll states reset on room changes and remain during updates in the same room. Toggle, blocked-path, and shop-greeting dialogs use inspection-card styling and preserve authored messages, enabled choices, and runtime callbacks.

## Debug locations

Scenario room mappings are matched to Android AppServices and current room assets. Hub/world membership is derived from authored nodes and hubs. Weather Lab is an explicit standalone room in Homestead. All catalog locations resolve; invalid requests return failure with a visible title-screen error instead of substituting Nova's Bunk. Debug health starts at authored character health. This is a location/seed correction, not a complete port of every Android debug procedure.

## Validation

Five focused DesktopExplorationPolishTest cases pass: catalog locations/node membership, closed compact drawers and Close controls, Escape/Tab behavior, authored inspection text/enabled choices, and dark-map privacy.

Offscreen screenshots are in desktopApp/build/reports/desktop/screenshots/exploration-polish: Nova's Bunk dark/lit, Jed's Bunk, Night Market, Main Tunnel Alpha, and The Campfire with a tracked quest and four party members. Reviewed standard 1440x900, short 1280x720, compact 900x650, ultrawide 2560x1080, and high-contrast/large-target settings. These are isolated presentation checks, not a complete campaign playthrough. Existing broad snapshot suites were not run.
