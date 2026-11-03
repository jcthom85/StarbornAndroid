# Desktop interaction controls aligned with Android

November 2, 2026. Android is the interaction-design reference. The visible desktop Actions link is removed; the optional Q keyboard interaction list remains. No new desktop-only object fallback list was added.

## Implementation

- Android's existing Shops, Rest/Cook and Workbench classification is now a shared `isRoomServiceAction` predicate. Android's behavior remains the same; desktop uses that predicate to provide the missing Shop and Workbench service pills alongside Rest/Cook.
- Service controls retain runtime action membership checks, lock hints and normal object-offer confirmations. NPCs, ground items, enemies, directional travel and overworld return keep their existing direct controls.
- Shared room-description corrections expose previously unnamed or mismatched objects on both platforms. They use natural object names and explicit aliases where a descriptive phrase differs from its action ID. Fishing, arcade discoveries, an AI sub-vault, quest evidence and personal-reference inspections are included.
- Changes affect descriptions in **19 rooms**. An independent structural comparison confirms that room IDs, actions, gates, state definitions, exits and other non-description fields are unchanged. No quests, events, rewards, encounters or progression rules were edited.

Changed rooms:

- `spire_sewers_passage`
- `spire_vent_output`
- `spire_old_subway_car`
- `spire_skypark_dome`
- `spire_exec_lounge_bar`
- `foundry_cooling_springs`
- `foundry_reject_bay`
- `orbital_customs_lounge`
- `orbital_solarium`
- `orbital_zero_g_junction`
- `orbital_breached_avionics_lab`
- `source_echo_workbench`
- `source_new_world_node_scale_final`
- `spire_rigging_deck`
- `foundry_pipe_gallery`
- `spire_hidden_studio`
- `spire_skyline_vip_suite`
- `spire_vip_private_booth`
- `foundry_crucible_arena`

## Audit scope and exceptions

The initial inventory compares every action against every authored description in all **477 rooms**. It is conservative: 78 room/action pairs did not match at least one description, but this does not establish that those actions were available or usable in that description's state.

A second check uses production `narrativeActionVisible`, `resolveRoomDescription`, `buildInlineActionPlan` and `EventManager.canHandleTrigger`. It constructs condition-satisfying probes from action gates, event prerequisites and authored description variants. **763 probes pass with no missing usable functional-object links.** These are bounded presentation/eligibility checks, not actual full-game routes or exhaustive save-state certification. Ordinary completed-quest variants are modeled as completed, and Orion's briefing includes its authored prior awakening. Separate existing tests retain legacy-state coverage.

Services are deliberately excluded from inline-link requirements because they have dedicated pills. Authored `hide_inline` entries, including the optional cargo-netting and rusty-locker inspections in Nova's Bunk, remain intentionally hidden from prose; the netting event only sets inspection receipts and narrates. The older short “monitor” alias in Mess Hall does not displace its specific vintage mystery monitor link. Remaining conservative inventory rows also include retired objects, inactive scans and state variants where the relevant action is hidden or its event is not eligible. These are not counted as new blockers merely because their names are absent from raw prose.

## Verification

- **783 app/JVM tests passed**, zero failures/errors/skips, including the production World 1 completionist route and World 2 earned routes and encounter pairs.
- **32 targeted desktop tests passed**, zero failures/errors/skips. Covers service-pill visibility and actual shop confirmation, absent Actions link, locked inline objects, special exits, scrolling, interaction reachability, room-entry stability, and voice routing/decoding.
- Android lint and debug APK assembly pass. All **1,341 Android assets** match desktop staging and the desktop asset archive byte-for-byte.
- World 1 checkpoint source hashes refreshed through the earned route/import workflow after shared room-description changes.
- Existing World 1 and World 2 room descriptions were preserved. The shared room edits are limited to Worlds 3–6. This is a discoverability repair, not a broader creative or readiness assessment of those worlds.

[Counts](evidence/desktop_interaction_parity_2026-11-02/test-summary.json), [targeted output](evidence/desktop_interaction_parity_2026-11-02/targeted-tests.txt), [host/lint/build/assets output](evidence/desktop_interaction_parity_2026-11-02/host-lint-assets-build.txt), [functional probes](evidence/desktop_interaction_parity_2026-11-02/functional-link-check.txt), [remaining conservative inventory](evidence/desktop_interaction_parity_2026-11-02/conservative-unmatched.tsv).

Physical Android acceptance remains pending; no Pixel installation was requested for this pass. The desktop app needs a fresh launch to use the updated code and packaged room descriptions.
