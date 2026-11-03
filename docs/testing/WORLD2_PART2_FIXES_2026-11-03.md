# World 2 Part 2 fixes and regression checks

The confirmed progression failures were caused by state surviving a save restore in memory. These changes replace cached room and quest progress when loading an earlier timeline, persist ground loot, and repair Tideglass task credit from recorded events. The Pixel save was copied for testing; device progress was not edited.

## Implemented

- **Save restoration:** rebuild room flags, authored room models, unlocks, discovered rooms, ground loot and enemy movement from the restored session. Clear transient dialogue and prompts. Refresh the map, including the Astra deck plan. Temple Gate can return to its closed state and subsequently open again.
- **Quest restoration:** clear cached stages, tasks and recent log on restoration before consuming new progress. A later timeline cannot suppress task writes in an earlier save or falsely complete a quest.
- **Ground loot:** persist remaining per-room quantities in the shared save protocol. Explicit empty entries distinguish collected loot from an untouched room. Older saves without this data retain authored ground loot; inventory alone cannot prove earlier collection.
- **Tideglass recovery:** an existing beach-visit event restores `visit_beach`. A recorded gather event or gathered milestone restores collection credit without granting supplies again. Resin in inventory does not establish gathering or crafting credit. Migration is idempotent and does not grant XP or inventory.
- **Source Gate:** state-based blocked exits display the normal lock indicator; resolving the barrier removes it and loading an earlier save restores it.
- **Console portraits:** Stasis Console, Mural Array, Vent Scanner and Source Gate Console use the existing terminal portrait. Comms interactions retain their radio artwork.

## Evidence and verification

A copied Pixel slot contained the completed `w2_sq03_visit` event but no `visit_beach` task. Gathering required that missing task. A regression reproduced this by visiting the beach, loading an earlier save of the same active quest and visiting again; it failed before the quest-cache fix.

The copied slot is exercised through the normal desktop save loader using the shared protocol and migrations. The test visits Tide Pools, gathers once, learns the schematic, crafts Source Resin and completes Tideglass. Raw player saves stay outside the repository. To repeat locally, set `STARBORN_PLAYTEST_SAVE` to a copied protocol slot; otherwise this private-save test is skipped.

The crew-balance test previously expected two extra AP after loading the early checkpoint and performing only landing, travel and rest. Its expectation now requires the checkpoint AP to remain unchanged: later quest tasks must not survive that load and award a phantom completion.

Final verification:

- Android host suite: **784 tests, zero failures, zero skipped**.
- Targeted desktop suite: **15 tests, zero failures, zero skipped**, including the copied Pixel save, restore/loot tests, Source Gate, resonator prerequisite popup, shared gate presentation and Ring Array progression.
- World 1 checkpoint manifest refreshed from the successful production completionist run; all **10 earned states** match current content.
- `git diff --check` passed.

The resonator-column regression confirms that tapping before seating the crystals shows the standard popup explaining the west/east/north prerequisites and does not complete the puzzle. A complete mural-route device check is still recommended.

## Focused device checks

1. Load a save before Temple Gate, open it, reload that same earlier save without restarting the app, then open it again. Confirm closed artwork, full reveal and narration order.
2. Collect ground loot, leave through a hub or another node, return, then save/reload. Confirm no duplicate loot. Load a genuinely earlier save and confirm its own loot state returns.
3. Load the affected Tideglass save, revisit Tide Pools, gather, learn the schematic and craft Source Resin. Confirm one set of gathering supplies and one completion reward.
4. At Source Gate, confirm the east arrow shows a lock before resolution, becomes usable afterward, and relocks after loading an earlier save.
5. Check all four console portraits on Android and desktop.

These automated checks do not certify animation appearance, touch timing, or device audio.

## Remaining notes and discussion

- Resonator columns: prerequisite inspection is verified; check the complete three-crystal route on device. The authored action is a functional control requiring all three seated crystals.
- Read styling and quest-action visibility: verified and refined in the [exploration and menu pass](WORLD2_EXPLORATION_AND_MENU_POLISH_2026-11-03.md).
- Quest-action audit completed for this pass; quest starters and useful locked puzzles remain visible.
- Orion identity reveal and evening-tide continuity: implemented in the [four followups](WORLD2_NARRATIVE_AND_ASTRA_FOLLOWUP_2026-11-03.md).
- Party overflow and enemy-name sizing: implemented and audited in the exploration/menu pass. The three narrow-layout naming cases received approved replacements, recorded in that report.
- Gh0st recruitment pacing and Botanist completion beside the player: implemented in the four followups. Caretaker Echo chronology is addressed in the exploration/menu pass.
- Astra boarding before World 3: implemented in the four followups. Tinkering layout and return-route orientation are addressed in the exploration/menu pass.
- Roaming enemies and first-attempt puzzle bonuses remain discussion items; no changes made.

No claim is made that every Part 2 consideration is finished. No new build has been installed for these fixes, and no commit or push has been made in this pass.
