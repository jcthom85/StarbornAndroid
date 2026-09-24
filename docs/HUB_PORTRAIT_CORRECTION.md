# Portrait hub correction — September 24, 2026

Corrects the rejected hub presentation shipped in 1.3.66. Local changes; no new release requested during this correction.

## Presentation

- Full-screen portrait background, centered aspect-preserving cover. Removed the cropped illustration inset and dark duplicate framing.
- The background and node anchors use the same cover transform. Changing the selected destination cannot move either.
- All 59 destination miniatures from hub_nodes.json are rendered again. The Astra interior/disembark actions have no authored destination icon and retain their action markers.
- Twelve new terrain backgrounds have connected routes and open ground for separate miniatures. Each uses 1088x1920 portrait composition; the existing Astra cutaway is retained.
- Node positions are individually placed against each generated terrain image, with smaller proportional artwork to preserve the paths and label spacing.
- Astra physical docks remain in Logistics, Upper City, and Orbital Ring. Available return access elsewhere stays outside the world terrain, beside the menu. Existing access and travel logic is preserved.
- When a quest is tracked, the header shows the quest instead of the hub subtitle to keep the artwork visible.

## Canonical art workflow

Read Starborn_Art_Production_Guide.md, Visual_Prompting_Guide.md, and data/assistant_briefing.md. Inspected existing hub backgrounds and approved node miniatures before generation.

Used the imagegen skill API/CLI workflow requested by the user: gpt-image-2, quality=low, opaque 1088x1920 PNG background generation. Converted to WebP with quality=90, method=6, exact=True. Original assets are preserved as siblings.

Background outputs: world_assets/src/main/assets/images/hubs/<hub_id>_portrait_v2.webp, for hub_1_homestead through hub_12_singularity. Exact job prompts are in [prompts.jsonl](art/hub-portrait-correction/prompts.jsonl). PNG source files are in output/imagegen/hub-portrait-correction/.

Ship output: world_assets/src/main/assets/images/nodes/astra_ship_map_v2.webp. Edited the prior Astra sprite at quality=low, 1024x1024 to simplify fine texture into bold cel-shaded forms while preserving identity. Green chroma-key PNG converted with scripts/remove_chroma_key.py, then the same WebP settings. [Exact prompt](art/hub-portrait-correction/astra-prompt.txt). Transparent corners and the composited silhouette were inspected.

## Verification scope

scripts/audit_hub_portrait_art.py checks all generated background dimensions/opacity and existing destination-icon files/transparency. scripts/hub_capture_contact_sheet.py assembles emulator evidence for visual review.

HubMapLayoutTest now checks full-screen coverage and explicitly rejects zero artwork widths for world destinations. HubMapVisualAuditTest now asserts that every authored node image actually appears; label visibility alone is insufficient.

Focused tests: HubMapLayoutTest, HubAstraTravelTest, AstraTravelTest, HubMapVisualAuditTest, AstraTransitNavigationTest. Emulator fixtures cover normal, compact, large text, locked/quest, and pre-Astra access. These fixtures expose all destinations and use sample descriptions; they do not constitute full campaign progression testing or a physical-device matrix.

Final run log: test-results/hub-portrait-correction-verified.log. Large text uses a capped, scrollable description beside the Enter button, avoiding a tall stacked card over the lowest destinations.

Results: all 10 focused unit tests and both emulator tests passed. After the final label-spacing adjustment, the 65-screen visual audit passed again (test-results/hub-portrait-final-spacing.log). Latest captures: test-results/hub-portrait-evidence/. Reviewed the normal compositions across all hubs and large-text/quest cases; corrected Upper City label collisions and ship placement, Facility spacing, and bottom-label clearance. The full-screen background and visible destination art are verified separately from campaign progression. git diff --check passed.
