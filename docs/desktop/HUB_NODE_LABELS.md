# Hub node nameplate pass

Every destination keeps its original visible name. Nameplates use Source Sans at 12sp (11sp compact), 7dp horizontal and 4dp vertical padding, and content widths capped at 128dp (104dp compact). Lock/check icons replace map status-word rows. Long names wrap to two lines and have full-title hover tooltips when truncated. Selection, hover, and keyboard focus strengthen the border and text treatment. Full name and availability remain accessible.

Visible plates and interaction bounds are separate. Transparent targets are at least 44dp, or 56dp with large targets; extending a target does not enlarge the painted plate. Artwork clicks, double-click travel, and focused Enter retain shared controller callbacks.

DesktopHubLabelLayout is a pure pixel-space placement helper. Inputs include measured visible plate sizes, original image/art bounds, viewport bounds, and occupied side HUD bounds. It prefers below/above placements, then nearby sides and adjacent blur callouts. It avoids artwork, existing plates, and HUD areas where possible, favors clear leader routes, and uses a deterministic overlap fallback when space is exhausted. Selection styles do not invalidate placement; resizing, asset loading, content changes, and changed occupied HUD geometry do. Artwork anchors remain authored and unchanged.

Map rendering now uses a viewport-sized overlay, so labels in adjacent blur remain inside the pointer surface. Side regional and destination cards report their actual occupied bounds. Leader lines are subdued and drawn underneath artwork and labels.

Validation: twelve focused checks passed (eight hub UI/runtime checks and four pure label-placement checks), including separate hit/paint sizes, large targets, missing artwork keyboard activation, locked travel, Astra travel, deterministic fallback, collisions, reserved HUD regions, and viewport edges. Reviewed offscreen screenshots of Homestead, dense Event Horizon, Logistics, Lower City, Astra, and high contrast in standard/compact/ultrawide arrangements. Screenshot fixtures use temporary saves, not a native campaign playthrough. The latest final review captures nine representative configurations after the full size matrix review.

Portable Windows delivery remains a folder, with no ZIP. Authored data and shared progression are unchanged.

Delivery: the existing portable folder had a user-mapped icudtl.dat lock, so the completed native app was copied into desktopApp/build/distributions/Starborn-Windows-hub-labels/Starborn instead. All 438 native app file sizes were verified against the build output. No running game process was closed.
