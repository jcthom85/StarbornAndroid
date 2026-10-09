# Title and exploration status cleanup

October 7, 2026.

- Removed the remaining DEEP-SPACE TACTICAL RPG tagline and its containing card.
- Enlarged the original Starborn logo with its 3:2 asset ratio, a larger share of the title layout, and a clear gap before the action buttons. The original artwork and animation remain.
- Rendered and inspected wide and narrower title layouts. The focused title layout test passes; screenshots are `desktopApp/build/reports/desktop/screenshots/title-large-logo.png` and `title-large-logo-narrow.png`.
- Traced “Turn on the bunk light to see the door controls.” to the door-control action's condition_unmet_message in original Android rooms.json. It is authored data, not a desktop invention. Similar authored condition messages exist throughout the original room data.
- Removed the desktop-only persistent statusMessage area beneath room descriptions across all rooms. Android's live exploration HUD does not display that area. Original authored inspection/blocked-action prompts remain in their intended interactions; menuFeedback remains transient in the menu.

Test log: `tmp/title-layout-verification.log`. Portable packaging log: `tmp/title-status-cleanup-package.log`.
