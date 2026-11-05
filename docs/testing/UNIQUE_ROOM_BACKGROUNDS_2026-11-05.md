# Unique room backgrounds — November 5, 2026

Twelve new room backgrounds replace the twelve shared-image assignments identified in the global room audit. Each original room retains its original image. Room descriptions, alternate descriptions, actions, navigation, gates, encounters, items and rewards are unchanged by this art pass.

## Production

The user explicitly authorized OpenAI API generation using the local key file. The key was read into the generation processes' environment and was not printed, copied into prompts or recorded in evidence. Generation used the imagegen skill's bundled CLI, `gpt-image-2`, quality `low`, opaque PNG and `1088x1920` output. Each request used the room's existing same-world background as a **style-only reference**, through the images edits endpoint, while requesting an entirely new room composition.

The [twelve briefs](../art/UNIQUE_ROOM_BACKGROUND_BRIEFS_2026-11-05.md) supplied current room descriptions, interactions, neighbors, variants and permanent architectural landmarks. The final prompts added the documented comic/anime environment style, portrait framing and environment-only constraints. No model downgrade, quality escalation or modification of the shared original artwork was needed. All twelve first outputs were accepted after individual visual review.

The new art uses the existing outlined machinery, substantial architectural silhouettes, portrait depth and controlled amber/cyan lighting. The workshop is organized around CRTs and amplifier benches; the lab around containment cylinders; the salon around skyline glass; the booth around enclosed mahogany seating; the rigging deck around winch drums; the geothermal forge around basalt crucibles; the arena around a sunken cage ring; the pipe gallery around a large bypass manifold; the research station around gauges and filters; the submerged vault around a drained dais; the armory around a breached bulkhead and lockers; and the avionics lab around its broken canopy and planetary horizon.

Actors, victim, readable investigation evidence and portable quest rewards are left to the existing gameplay presentation. The background compositions remain usable across the murder-resolution, champion and maintenance-clearance states. Visual acceptance here covers style, recognizable architecture, absence of baked actors/text and portrait framing; it does not claim physical-phone acceptance.

## Saved assets

All PNG masters and the exact individual prompt files are retained locally in `output/imagegen/unique-room-backgrounds/` (the repository already ignores `/output/`). Runtime assets are new WebPs under `world_assets/src/main/assets/images/rooms/`, exported with Pillow at quality 90 / method 6. No original file is overwritten. Exactly twelve `rooms.json` background references change.

| World | New runtime filename | Room |
| --- | --- | --- |
| 1 | `world_1/mine_sealed_munitions_vault.webp` | Sealed Munitions Stash |
| 2 | `world_2/sector9_coastal_research_station.webp` | Tidewatch Station |
| 2 | `world_2/sector9_sunken_aethel_vault.webp` | Submerged Aethel Vault |
| 3 | `world_3/spire_hidden_studio.webp` | Secret Signal Workshop |
| 3 | `world_3/spire_black_site_lab.webp` | Executive Black-Site Lab |
| 3 | `world_3/spire_skyline_vip_suite.webp` | Skyline VIP Salon |
| 3 | `world_3/spire_vip_private_booth.webp` | Syndicate Booth |
| 3 | `world_3/spire_rigging_deck.webp` | Bridgman Rigging Deck |
| 4 | `world_4/foundry_subterranean_smelter.webp` | Subterranean Smelter Forge |
| 4 | `world_4/foundry_crucible_arena.webp` | The Scrapper's Crucible |
| 4 | `world_4/foundry_pipe_gallery.webp` | High-Pressure Pipe Gallery |
| 5 | `world_5/orbital_breached_avionics_lab.webp` | Breached Avionics Lab |

[Contact sheet](evidence/unique_room_backgrounds_2026-11-05/contact-sheet.jpg). [Generation manifest and exact prompts](evidence/unique_room_backgrounds_2026-11-05/generation-manifest.json). [Source preservation and global duplicate audit](evidence/unique_room_backgrounds_2026-11-05/source-and-duplicate-check.json).

## Verification

Every referenced background is found and decoded. All 477 room records now have distinct background paths, distinct file hashes and distinct decoded RGB pixel hashes. New runtime assets are RGB WebP at 1088x1920. Their original references remain byte-for-byte intact. The source comparison against the start-of-art snapshot permits only the twelve background-image fields.

Validation passed: **43 Android host content/navigation tests**, **8 desktop content tests**, the navigation audit, `git diff --check`, and `:app:assembleDebug`. All twelve images decode correctly from the APK and match their source WebPs; the packaged room catalog matches source. New artwork totals **3,304,090 bytes**.

Final test counts, APK asset parity and build hashes are recorded in [verification.json](evidence/unique_room_backgrounds_2026-11-05/verification.json). This pass does not install, publish or commit the changes.
