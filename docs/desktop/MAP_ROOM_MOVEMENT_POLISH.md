# Map and room movement polish

October 7, 2026.

- Removed the title-screen game description paragraph.
- Replaced full-map room cards with compact connected nodes, room labels, selected/current rings and authored service/threat indicators. The map fits discovered content on opening and resizing; Fit, zoom and drag controls remain. North stays up.
- Opening room actions are available during a fade from black. The black-screen flag still blocks interaction before the reveal starts; cinematics and actual modal interactions retain their input blocking.
- Desktop now renders shared roomTransition events using Android's 240 ms FastOutSlowIn movement, incoming directional offset/scale, outgoing slide/fade/blur and original ui_room_move audio cue.
- Transforms apply to the sharp portrait artwork and reflected extensions together. HUD geometry stays stationary, with a portrait layout fallback while the incoming bitmap loads. Movement input is gated during the short room transition.

Desktop compilation and portable packaging are recorded in `tmp/map-room-polish-build.log` and `tmp/map-room-polish-package.log`. No gameplay tests were added or run for this pass. Native map composition, fade-time bunk-light interaction and repeated room traversal still require playtesting.
