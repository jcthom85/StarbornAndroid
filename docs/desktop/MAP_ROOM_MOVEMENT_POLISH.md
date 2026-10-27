# Map and room movement polish

October 7, 2026.

- Removed the title-screen game description paragraph.
- Replaced full-map room cards with compact connected nodes, room labels, selected/current rings and authored service/threat indicators. The map fits discovered content on opening and resizing; Fit, zoom and drag controls remain. North stays up.
- Opening room actions are available during a fade from black. The black-screen flag still blocks interaction before the reveal starts; cinematics and actual modal interactions retain their input blocking.
- Desktop now renders shared roomTransition events using Android's 240 ms FastOutSlowIn movement, incoming directional offset/scale, outgoing slide/fade/blur and original ui_room_move audio cue.
- Transforms apply to the sharp portrait artwork and reflected extensions together. HUD geometry stays stationary, with a portrait layout fallback while the incoming bitmap loads. Movement input is gated during the short room transition.

Desktop compilation and portable packaging are recorded in `tmp/map-room-polish-build.log` and `tmp/map-room-polish-package.log`. No gameplay tests were added or run for this pass. Native map composition, fade-time bunk-light interaction and repeated room traversal still require playtesting.

## October 27, 2026: map readability

The shared Android/desktop minimap keeps its size and five-by-five neighborhood but now emphasizes room positions and routes. Removed the background grid, service icons, stacked exit glyphs, and enemy rings. Exit rooms use one diamond; threats use a small red dot; blocked connections use a gap and crossbar. Gold identifies the current room, cyan identifies exits, and lavender marks blocked routes.

Full maps retain service icons and destination details. Android now shows every service instead of silently selecting one; desktop uses the shared room and threat markers. Full-map exit arrows use cyan (lavender when blocked). The legend describes both views, and accessibility descriptions retain service and exit information. Navigation, gates, and discovery rules are unchanged.

Validation: Android and desktop compilation, map/merchant regression tests, and rendered previews at 78 dp and 164 dp. Preview artifacts are generated under `desktopApp/build/reports/map-readability/`; the desktop map flow preview is under `desktopApp/build/reports/map-alignment/`.

## Rest and cooking controls

Rest spots use a dedicated **Rest** control below the room description on both platforms. Beds, shelters, and cooking stations remain plain prose, including authored action markers. Cooking stays a separate **Cook** control with its own icon. Desktop keeps its existing confirmation before executing the activity, and the Actions menu remains available as a fallback. Darkness and blocked overlays hide or disable the controls as appropriate.

This standard changes presentation only: room data, authored rest events, recovery, quest triggers, camp supplies, and cooking discovery remain intact. Regression coverage exercises all 14 authored rest spots and 14 cooking stations, including camp reward deduplication, the bunk state flag and light requirement, and canceling confirmation without recovery.
