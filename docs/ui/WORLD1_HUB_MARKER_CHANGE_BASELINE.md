# World 1 Hub Marker Change Baseline

Recorded July 15, 2026 before changing the World 1 hub presentation.

## Repository baseline

- Branch: `main`
- Commit: `82ab6c43632493abfef46f31b80956c66dd864dc`
- Commit subject: `Fix exploration transitions and enemy sizing`
- Pre-change worktree: only `docs/story/NARRATIVE_CANON_MIGRATION_HANDOFF.md` was untracked. It is unrelated and must remain untouched.

This commit is the exact code rollback point. Reverting any later hub-specific commit is preferable to resetting the worktree because unrelated local work may exist.

## Existing World 1 presentation

World 1 uses `images/hubs/homestead_v5.png`, a finished isometric scene with its major buildings and routes already painted into the background. Five separate node illustrations are drawn over it:

| Node | Position | Overlay asset | Previous UI composition |
| --- | --- | --- | --- |
| The Pit | 0.14, 0.43 | `pit_hub1_v3.png` | 132×132 dp; image 116 dp; anchor 0.48, 0.68 |
| Jed's Workshop | 0.50, 0.68 | `workshop_hub1_v3.png` | 166×180 dp; image 166 dp; anchor 0.50, 0.72 |
| Med-Bay | 0.48, 0.34 | `medbay_hub1_v3.png` | 132×134 dp; image 118 dp; anchor 0.50, 0.58 |
| Trade Row | 0.81, 0.61 | `trade_row_hub1_v3.png` | 132×128 dp; image 112 dp; anchor 0.50, 0.68 |
| Transit Checkpoint | 0.84, 0.25 | `checkpoint_hub1_v3.png` | 150×126 dp; image 110 dp; anchor 0.56, 0.62 |

The node coordinates and progression data live in `app/src/main/assets/hub_nodes.json`. The per-node rendering overrides live in `HubScreen.kt`.

## Visual audit finding and intended change

The overlays repeat architecture already present in the background. Their perspective, footprint, lighting, and visual centers do not consistently match the painted landmarks, so they read as floating dioramas rather than locations on one map. The Pit is the clearest mismatch; Workshop is the closest match.

The first attempted direction was to retain the finished background and replace the five miniatures with restrained hotspot rings. Device inspection showed that this fixed spatial clutter but removed too much destination identity and made the screen feel generic. That intermediate treatment was rejected.

The accepted direction retains `homestead_v5.png` and the representative `v3` isometric destination miniatures, but renders them at a compact and much more consistent mobile scale. The background already provides five connected clearings suited to this composition; replacing it would add risk without solving a remaining structural problem. Rings and glows are reserved for selection and objective state behind the destination art. Other worlds keep their current illustrated node treatment.

Node IDs, source assets, unlock rules, selection, double-tap entry, quest highlighting, accessibility descriptions, and progression behavior remain unchanged. Coordinates are device-tuned below.

## Final device-tuned composition

Device inspection exposed that the generic layer clamped the entire marker container at the screen edges, silently shifting edge destinations away from their configured landmarks. World 1 markers now honor their authored X coordinate while later hubs retain the existing safety clamp.

The final landmark coordinates are:

| Node | Final position | Ground contact |
| --- | --- | --- |
| The Pit | 0.10, 0.59 | Left rail entrance and lift structure |
| Jed's Workshop | 0.50, 0.72 | Central circular work pad |
| Med-Bay | 0.46, 0.20 | Upper central foundation |
| Trade Row | 0.82, 0.54 | Right market platform |
| Transit Checkpoint | 0.88, 0.27 | Elevated upper-right guarded approach |

The final miniature widths range from 86 to 106 dp according to the available footprint and silhouette. Labels remain separate UI, and state rings do not replace the representative art.

The Pit source miniature faces down-left, opposite the rail and route direction at its map position. Its image layer is mirrored horizontally at render time so the entrance faces down-right. The source PNG, label, badges, hit target, and progression data remain unchanged. Med-Bay uses a 96 dp image width to fill its upper foundation.

## Background correction

Device inspection showed that `homestead_v5.png` baked a second mine portal beneath The Pit miniature. Positioning and mirroring could not resolve the doubled silhouette. `homestead_v6.png` replaces that portal with an open rail-connected rocky foundation while retaining the hub's isometric cavern composition, paths, lighting, and remaining destination clearings. `hubs.json` now points World 1 Homestead at the versioned `v6` background; `v5` remains untouched as the rollback asset.

The edit was generated with the built-in OpenAI image-generation workflow using `homestead_v5.png` as the edit target, then normalized to the source asset's 840×1871 dimensions before integration. No API key file was accessed.

The complete `docs/story/Starborn_Art_Production_Guide.md`, `docs/story/Visual_Prompting_Guide.md`, and `data/assistant_briefing.md` were consulted before reusing the approved node assets and creating the corrected versioned background.
