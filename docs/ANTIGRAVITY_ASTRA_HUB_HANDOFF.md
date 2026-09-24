# Antigravity handoff: Astra placement on hub maps

Paste the **Task for Antigravity** section into Antigravity CLI, then let it work in the existing StarbornAndroid workspace. This handoff is intentionally scoped to the next world only; preserve the established two-hub-at-a-time workflow.

## Task for Antigravity

Continue the in-scene Astra placement pass across Starborn’s world hub maps. The goal is to make the Astra feel parked naturally in a different, suitably sized location on each hub map, instead of appearing as a tiny awkward shortcut near the upper-right. Keep the hub maps mobile-portrait, retain their current backgrounds and node art, and preserve all Astra availability, map selection, and travel rules.

World 1 (Homestead and Logistics) is already implemented and visually tested in the current working tree. **Do not redo or revert it.** Work on World 2 only in this pass: Jungle Ruins (`hub_3_sector9`) and Sector 9 Ruins (`hub_4_facility`). Inspect each actual portrait background and choose an individual plausible parking site and scale for each hub. Reuse existing Astra artwork unless inspection shows a genuine need for new art. If creating art is necessary, first read the local art guides listed below and follow the existing image-generation workflow; the user authorized the API key in `openai_api_key.txt`, but never print, copy into logs, or expose the key.

Before editing, inspect the current working-tree diff and the progress/handoff docs. Preserve unrelated user changes and untracked assets. Use the existing in-scene docking-position mechanism if suitable; don’t add another Astra shortcut or alter travel/gameplay behavior just to place the ship. Check that each position and size works across portrait layouts, doesn’t crowd destination nodes, labels, header, or bottom panel, and remains selectable. Keep the ship visible only when existing game state makes the Astra available.

Validate both World 2 hubs in the emulator using the existing hub visual-audit test harness in its five configurations (normal, compact, large text, locked/quest, and before Astra availability). Inspect the screenshots—not just test assertions—for natural placement, fit, readability, and overlap. Record the command, result, and screenshot paths in `test-results` and update `docs/ASTRA_HUB_PLACEMENT_PROGRESS.md` with the chosen placements and results. Run relevant focused tests and `git diff --check`; do not change production balance or unrelated features.

Then stop and report World 2 results and screenshot links for review. Do not start World 3 until asked. Do not bump the version, commit, push, upload, publish, or make other release changes unless the user explicitly requests them.

## Project context and implementation references

- Android project root: `C:\Users\jcthomas\StudioProjects\StarbornAndroid`.
- Current implementation: `app/src/main/java/com/example/starborn/feature/hub/ui/HubMapLayout.kt`, `HubMapScene.kt`, and `HubScreen.kt`.
- Hub/node data: `app/src/main/assets/hubs.json` and `app/src/main/assets/hub_nodes.json`.
- Portrait hub art: `app/src/main/assets/images/hubs/` (inspect the actual World 2 `*_portrait_v2.webp` assets).
- Astra map art is reused from `images/nodes/astra_ship_map_v2.webp`.
- World 1 progress and exact locations: `docs/ASTRA_HUB_PLACEMENT_PROGRESS.md`.
- Broader background/node artwork correction context: `docs/HUB_PORTRAIT_CORRECTION.md` and `docs/HUB_MAP_VISUAL_PASS.md`.
- Relevant tests include `HubMapLayoutTest.kt`, `HubMapVisualAuditTest.kt`, and the existing Astra transit-navigation tests. The visual audit supports selecting hubs with `-Pandroid.testInstrumentationRunnerArguments.hubIds=...`; inspect the harness for the exact current Gradle invocation and output naming.

## World 1 already completed

- **Homestead** (`hub_1_homestead`): ship at normalized source anchor `(0.25, 0.66)`, width `0.24`; lower-left service route, smaller to fit the available space.
- **Logistics** (`hub_2_logistics`): anchor `(0.76, 0.73)`, width `0.32`; broad lower-right apron.
- Reused existing Astra art; no background or gameplay/travel changes.
- Focused emulator audit covered normal, compact, large-text, locked/quest, and pre-Astra configurations for both hubs (10 screens total). Log: `test-results/astra-world1-placement.log`. Main captures: `test-results/astra-world1-homestead.png`, `test-results/astra-world1-logistics.png`; a large-text Homestead capture is also available.
- The full working tree may contain unrelated user modifications/untracked art. Inspect and preserve them; don’t clean or reset the tree.

## Art and layout guardrails

Read these before any image generation or hub-art edits:

- `docs/story/Starborn_Art_Production_Guide.md`
- `docs/story/Visual_Prompting_Guide.md`
- `data/assistant_briefing.md`

Important established constraints: this is a **mobile portrait** game. Hub backgrounds are opaque portrait images (1088×1920) and destination miniatures are separate transparent assets (1024×1024). Keep the existing painterly/cel-shaded visual language. Do not replace full backgrounds with landscape images, bake node buildings into backgrounds, or introduce circular “stage” pads. Preserve original assets and use sibling variants for new iterations; convert generated final images to WebP with the project’s established settings and update their data references in the same change. Prefer code/layout placement changes and existing assets when those solve the problem.

## Remaining sequence (do not execute beyond World 2 in this pass)

| World | Hub IDs | Hub names |
|---|---|---|
| 2 | `hub_3_sector9`, `hub_4_facility` | Jungle Ruins, Sector 9 Ruins |
| 3 | `hub_5_lower_city`, `hub_6_upper_city` | Lower City, Upper City |
| 4 | `hub_7_slag_pits`, `hub_8_assembly_line` | Slag Pits, Assembly Line |
| 5 | `hub_9_orbital_ring`, `hub_10_deep_ring` | Dominion Orbital Ring, Deep Ring |
| 6 | `hub_11_event_horizon`, `hub_12_singularity` | Event Horizon, The Singularity |

Complete and report each two-hub world as its own reviewable batch. Pause after World 2 for user review before proceeding.
