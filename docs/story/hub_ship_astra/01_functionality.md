# Astra functionality

Status: Implementation reference, checked October 14, 2026. Primary sources: `rooms.json`, `AstraTravel.kt`, `AstraFacilities.kt`, `AstraCatalog.kt`, and the live exploration controller. UI and native-device acceptance remain separate.

| Facility | Current behavior | Limits |
|---|---|---|
| Bridge navigation | Shows docking context, unlocked destinations, and route guidance | Eligibility follows shared travel rules; BurgQuest restricts inter-world travel |
| Chime archive | Readable notes gated by recovered information/progress | Not an automatic sequence of voiced Phantom logs |
| Common Room table | Chapter-dependent crew check-ins with recruited companions and persistent read flags | Short authored entries, not a mandatory travel-banter system |
| Galley | Opens cooking/preparation through the existing station route | Current recipes, discovery, ingredient, portion, and meal rules apply |
| Quarters bed | Rest restores effective party maximum HP | No consumable snack-charge system; do not promise a meal reset without implementation |
| Crew downtime | Chapter-dependent personal entries and keepsakes | Not a separate relationship progression system |
| Cargo records | Sections for shared supplies, recovered artifacts, and catch records | Shared inventory manifest, not a capacity-limited second bank; relic records do not grant abilities |
| Astra workbench | Uses the crafting controller and actual inventory/recipes | No separate storage inventory |
| Cargo ramp | Returns to the recorded docking context or supported fallback | Consult `AstraTravel` for saved/debug-state recovery |
| Simulation archive | Lists eligible practice, defeated-boss archives, and challenges | Standard combat rewards, damage, and consumable costs apply; controller enforces availability |
| Recreation console | Opens recovered Great Frontier films, installed arcade cabinets, and prizes | Ownership/installation and demo restrictions apply |

## Contract boundaries

Source abilities retain authored story unlocks. The former touch-a-relic-to-catch-up system was a proposal. The former upgradeable storage bank, guaranteed travel interlude banter, free no-XP practice mode, and recharging snack charges were also proposals or obsolete descriptions.

Use the existing five rooms and current panel routes before proposing new destinations. Keep game text honest about what opening a facility does. Verify availability, dismissal, repeat use, costs/rewards, chapter changes, and save/load through each platform's live UI.

## Sources and evidence

Gameplay sources are under `gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/presentation/` and `feature/exploration/viewmodel/ExplorationController.kt`; travel rules are under `domain/session/AstraTravel.kt`. Android and desktop render these through separate overlays/panels.

Historical implementation notes remain in [the design review](04_design_review.md). For the project-wide source/evidence boundary see [Current State](../../CURRENT_STATE.md).
