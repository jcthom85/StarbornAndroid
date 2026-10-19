# Starborn documentation: start here

Reviewed October 14, 2026. This index replaces old takeover files and dated checkpoints as the project entry point.

## What to trust

**Current code, assets, and live routes are the default truth for the implemented game.** Most documents were written before the current implementation. When they disagree, normally correct or retire the document. An old specification is not evidence of a game defect and is not a reason to revert working logic.

A proposed gameplay change needs a reproducible defect, a violated current player-facing contract, or an explicitly chosen redesign. A new tone preference is direction for future editing, not proof that an older implementation was erroneous.

Use document roles rather than one hierarchy for every question:

| Question | Start with | Boundary |
|---|---|---|
| What is implemented? | [Current State](CURRENT_STATE.md), actual controllers, catalogs, and live routes | Source inspection is not complete device acceptance |
| What is the retained plot? | [Master Story](story/Starborn_Master_Story.md), [Lore](story/Lore_Bible.md) | Narrative reference; reconcile unverified details against production scenes |
| How should people behave and sound? | [Characters](story/Characters.md), [Arcs](story/Character_Arcs.md), [Writer's Handbook](story/Starborn_Writer_Handbook.md) | Intended character/prose direction, not a replacement ability catalog |
| How should content earn its place? | [Content Standards](story/Content_Standards.md) | No mandatory room or side-quest quotas |
| Where are the systems? | [Systems Overview](story/Systems_Overview.md) | Exact behavior comes from current source |
| How do we author world content? | [Story structure guide](story/_Structure_Guide.md), relevant world files and production records | Many local drafts still await reconciliation |
| How do we produce artwork? | [Art Production Guide](story/Starborn_Art_Production_Guide.md), [Enemy Sprite Guide](story/Enemy_Sprite_Generation_Guide.md) | Asset-specific production rules, not general lore direction |
| How do we test/package? | [Testing guide](testing/README.md), [Windows build guide](desktop/WINDOWS_BUILD.md), current Gradle tasks and scripts | Check current command/fixture compatibility before execution |
| What did a past pass establish? | Dated reports in this tree | Evidence applies to its build, fixture, and scope; old next-step lists are historical |

## Current work

October 19: [World 1 review](testing/WORLD1_REVIEW_2026-10-19.md) records opening/escape corrections, early-Warden recovery, all ten quests on a production controller route, 71 focused passing tests, build evidence and remaining human acceptance. World 1 is not yet signed off.

October 19: [pre-World 1 readiness](testing/PRE_WORLD1_READINESS_2026-10-19.md) records the bounded Android progression smoke pass, copied legacy-save check, and remaining human acceptance before the World 1 review.

October 19: the owner approved adopting the crew progression structure. [The production adoption record](testing/CREW_PRODUCTION_ADOPTION_2026-10-19.md) describes the coordinated New Game/save migration, legacy respec compensation, AP pacing checks and remaining human acceptance. Earlier prototype and design documents retain historical results; use the adoption record for current rollout status.

[The progression and ability plan](design/PROGRESSION_AND_ABILITY_PLAN.md) records the audit and redesign proposal for ability contracts, character builds, AP pacing, effective stats and NG+ compatibility. The owner subsequently approved crew adoption; use the production adoption record above for implementation and acceptance status.

[Build candidate 1](design/PROGRESSION_DESIGN_PACKET_CANDIDATE_1.md) records the proposed character paths, effects, AP placements, purchase examples and migration policy. The crew implementation and combat experiments now have separate evidence; this historical proposal does not establish final balance acceptance.

[Documentation cleanup](DOCUMENTATION_CLEANUP.md) records completed edits, findings, verification, and remaining batches. [The inventory](DOCUMENTATION_INVENTORY.md) covers retained documents and explicitly distinguishes reviewed files from triage candidates.

The agreed creative direction is a lighter use of acoustic imagery and more grounded, varied interactions. [The refinement plan](design/SOURCE_LANGUAGE_AND_PUZZLE_REFINEMENT_PLAN.md) separates editorial work from proposed puzzle redesigns. This does not require adding a resonance combat system.

World references reconciled against current records: [Sector 9](story/world_2_the_wilds/00_world_story.md) and [Spire](story/world_3_the_spire/README.md). Other world drafts remain pending; their older specifications do not override implemented behavior.

## Scope and history

Supporting world/hub documents describe design or old scripts unless explicitly reconciled and dated. Use them to locate context, then inspect the corresponding current records. Do not bulk-replace production text from them.

Root handoffs and older global status reports now point here. Historical documents may retain imperative language, obsolete numbers, or superseded recommendations in their bodies; their historical status takes precedence over that wording. They are not the default authoring context.

Originals replaced in this pass are preserved in [the documentation-reset archive](_archive/documentation-reset-2026-10-14/README.md). Archive copies are for deliberate historical lookup only. They must not be loaded as current guidance.
