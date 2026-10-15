# Documentation cleanup record

Started October 14, 2026. Scope: make documentation accurately orient work on the implemented game, remove conflicting creative instructions, and separate history from active references.

## Owner direction

Most documents predate the implementation. Current code, assets, and live routes are the baseline. Normally update or retire a conflicting document. A mismatch alone is not a bug; a game change requires evidence of a real defect or a deliberately chosen redesign.

Sound and frequency imagery should be a light touch. Preserve useful names, ordinary sounds, music, cultural details, and established causal history. Remove universal musical explanations and repeated abstract calibration language from future authoring. Shipped prose/puzzle changes are subsequent work.

## Completed foundation batch

- Added a single [entry point](README.md), a bounded [source snapshot](CURRENT_STATE.md), and a repository-wide Markdown [inventory](DOCUMENTATION_INVENTORY.md).
- Rewrote the PythonDevKit-loaded `data/assistant_briefing.md` with self-contained grounded guidance, essential story constraints, and the implementation-first rule. Verified its consumers by source inspection.
- Reconciled the story/lore references, retaining causal constraints and removing universal acoustic framing, required finale speeches, old scope growth, and unimplemented prototype equipment claims.
- Replaced the old systems overview with a source-backed runtime map. Retired deferred-movement, equal-full-health battle baseline, fixed twelve-skill kits, persistent Erosion survival meter, self-only snacks, fixed sprite dimensions and mandatory black-bar claims.
- Replaced room/quest quotas with purpose, discoverability, consequence, and validation contracts. Kept current catalog counts explicitly dated.
- Removed invented/obsolete combat kits from the character reference. Reconciled the emotional map's memory-loss, copied-identity, colony-safety and war-ending contradictions.
- Refined prose/arc guidance; separated intended character development from implementation evidence.
- Reconciled all three existing Astra specifications with the current source, including consolidated cargo records and recreation routes. Retired promises of a second bank, relic skill synchronization, snack charges, automatic travel banter and cost-free simulations.
- Redirected old root handoffs to the new entry point and marked selected dated global checkpoints/migration instructions as historical.
- Marked 69 supporting world/system documents as pending source reconciliation. Their original design content is retained; notices explicitly prevent older Canon/Locked wording from overruling implemented behavior. This is status triage, not a completed audit of those 69 files.
- Preserved replaced originals with hashes in [the archive](_archive/documentation-reset-2026-10-14/README.md). No game code or production assets were edited by this cleanup.

## Sources inspected

Core story/prose documents; Astra specifications and current design review; `PythonDevKit/ai_assistant.py`, `ai_narrative.py`, `ai_service.py`, `context_index.py`; Gradle/module layout; production worlds, rooms, quests, characters, skills, progression, status and puzzle catalogs; exploration/combat controllers; `PartyHealth`, `MealRules`, `AstraTravel`, `AstraFacilities`, `AstraCatalog`; relevant public campaign and desktop readiness evidence.

Inspection establishes where behavior is implemented. It does not certify all inputs, timing boundaries, or device UI.

## Completed Sector 9 / Spire batch

Replaced 23 outdated world/hub and expansion-reference documents and added a single Spire world entry point. [Sector 9](story/world_2_the_wilds/00_world_story.md) and [Spire](story/world_3_the_spire/README.md) now summarize actual records. Local files retain stable paths while separating quests, cast/encounter references, room membership, and event ownership.

| Old documentation claim | Inspected implementation |
|---|---|
| Solo Nova at World 2 entry; Gh0st before Orion | Zeke joins at colony launch; Orion joins through stasis conversation before The Hunter and Gh0st's Beast-victory recruitment |
| Link at Temple Gate or automatic first rest | `w2_mq04_anchor_drill` cinematic completion callback grants `nova_link` after the Beast fight |
| Erosion survival meter and uniform self-only snack tutorial | No restoration of those old specs; current system reference distinguishes narrative strain, combat status, meals and snacks |
| C#m7 calibration and Chorus Echo reward in Ancient Echoes | Crystal collection/seating plus Mural Array socket choices; `focus_conduit` and XP |
| Ecological Insight / beast-critical passive for Botanist | XP and two painkillers through Zeke's completion dialogue |
| Second outer-Shield crash ending World 2 | Confirmed departure follows a low service airlane; cinematic lands beneath the Spire and callback starts MQ11 |
| Thirty-two-room Spire buildout and five side quests | Current node membership: 70 rooms; six side quests, including the Skyline Salon investigation |
| New reunion with a separately waiting Zeke; speculative VIP alert meter | Existing travelling crew and actual conditioned dialogue/actions; obsolete scripted mechanics are retired as implementation claims |
| Streetwise/Blackmail price passives described as quest grants | Actual completion events and Corporate Espionage branch items are documented without inventing those passive effects |
| Simplified either/or map diagrams and old room aliases | Catalog node entries, policies, and all 163 room memberships across the four hubs; actual movement/gates remain in source |

The puzzle review also corrects the earlier evaluation's scope: six three-slider definitions in `tuning_puzzles.json` are not the complete puzzle system. World 2 stasis, Source Gate and murals already use dialogue choices; the main Spire Gallery uses clue milestones/actions. Optional tuning puzzles are separate. Future redesign must assess existing interactions rather than assume they are missing.

The [world-document archive](_archive/world-doc-reconciliation-2026-10-14/README.md) preserves original bytes and a source snapshot. It records catalog hashes, checked reference IDs, and scope. No game code, logic, production text, quest gates, rewards, or saves were changed. This batch is source reconciliation, not a fresh route/device playtest.

## Remaining batches

1. Remaining world-specific storybeat packets and hub scripts: Mines, Foundry, Orbital Ring and Source. Sector 9 and Spire references are reconciled in the completed batch above. Compare each claim with current records; preserve implemented behavior by default. Inventory entries marked pending are not audited findings.
2. Combat/encounter/tutorial references and debug guides. Reconcile actual earned abilities, current mechanics, supported effects, fixture scope, and exact-text assumptions.
3. Puzzle masterplaybooks and world puzzle specifications. Separate existing behavior from the replacement designs; prototype one scoped replacement before broader implementation.
4. Audio/art/environment references. Preserve technical workflows and literal music terminology while reconciling old framing/layout claims and overbroad motifs.
5. Dated desktop/UI/release reports and root evidence. Retain their evidence and scope; migrate only reusable current instructions into maintained references.

## Verification

This is a documentation-only change. Validate edited live Markdown links, original snapshot hashes, inventory coverage, and `git diff --check`. No new game behavior tests are warranted for the prose/index changes. Existing game failures remain separately tracked in [Current State](CURRENT_STATE.md).

Completed checks: 18 archived originals match their recorded SHA-256 hashes; 401 maintained local links resolve; all 69 added review notices point to the current index; all 291 inventoried paths exist with no duplicates; scoped `git diff --check` passes. The 18 replaced/redirected originals totalled 18,837 words; their replacements totalled 9,881 words at verification, a 47.5% reduction in that material. Archived originals remain available.

Sector 9 / Spire batch checks: all 23 archived originals match their SHA-256 hashes; all 19 inspected source hashes remain current; 314 typed source references and 504 local links resolve; all 316 inventoried paths exist without duplicates; all 24 rewritten/new references have the correct inventory status; scoped `git diff --check` passes. The replaced originals totalled 14,581 words; their 24 replacement/entry documents totalled approximately 7,746 words, a 46.9% reduction. This is reference validation, not a new gameplay test run.

The cleanup does not claim all world/hub documents are reconciled. The inventory makes that boundary explicit so unfinished reviews cannot masquerade as current specifications.
