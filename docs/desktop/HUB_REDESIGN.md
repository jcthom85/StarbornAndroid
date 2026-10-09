# Desktop hub redesign

The hub now uses the original fitted portrait artwork with reflected blur, independent regional and tracked quest cards on the left, and destination details and travel controls on the right. Wide layouts keep cards close to the map rather than at the window edges. Compact layouts reserve room below the complete image for a scrollable destination card and expose regional context through a drawer.

Destination artwork, ground contact shadows, rings, and labels follow the same image bounds. Regular anchors use hub_nodes.json. Android artwork widths and synthetic Astra anchors live in the shared gameRuntime hub presentation metadata; Android imports that metadata without changing its cover transform. Quest destination highlighting also uses the shared original Android matcher.

HubController owns selection, visibility, lock text, authored descriptions, tracked objectives, unlock reveals, and travel. No desktop quest summary or invented regional prose is used. Single click selects, double click enters, and focused node Enter activates travel. Locked entries retain their shared prompt. Escape closes overlays before returning, Tab traverses controls, I opens Menu, and F5 quick saves. Return is offered only for a valid current room. Astra disembark routes that return directly to a map remain on the hub screen when the shared controller clears roomId.

The hub menu exposes tracked quest, quick save with shared status feedback, settings, and save-and-return-to-title. Settings reuse desktop controls without opening unrelated field menu tabs. Accessibility supports high contrast, larger targets, and static node presentation when motion is suppressed. Missing artwork keeps selectable destination labels; missing hub artwork uses the neutral backdrop rather than a different region.

Validation: seven focused desktop checks cover fitted anchors and layout metadata coverage, controller selection and tracked quest fidelity, node visibility, locked and normal travel, Astra routes, reveal acknowledgment, disabled locked travel controls, missing-artwork keyboard activation, and compact Escape handling. Android Kotlin compilation passes after sharing metadata. Offscreen screenshots cover five hubs plus high contrast at 1440x900, 1280x720, 900x650, and 2560x1080 (24 images), using temporary saves. These are controlled fixtures; a full native campaign playthrough was not performed.

Portable Windows delivery remains a folder under desktopApp/build/distributions, with no ZIP.
