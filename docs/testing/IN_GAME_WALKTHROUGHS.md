# In-game playtest walkthroughs

In the internal tester release (built with `-PenablePlaytestScenarios=true`),
open **Exploration -> Menu -> Playtest walkthrough**. The entry appears in
both modern and classic field menus. The regular public release hides this
tester entry.

## Using the reader

- Worlds 1 and 2 are available offline. The current world is selected on opening.
- **Contents** selects a chapter or reference section. **Previous / Next** moves
  between sections. Each section retains the guide's original numbered steps.
- **This room** lists matching route steps and the room reference. Choose the
  current visit: revisiting a room for a later quest is deliberately not treated
  as an automatic first-visit jump.
- **Search** finds rooms, quests, items and phrases across the whole selected
  guide. **Find tracked quest** searches the active journal quest's title.
- **Quest** lists the route chapters for the tracked quest, including preparation
  and return visits. Track a different quest in the journal to change this list.
- Checkboxes are manual playtest progress, not automatic game-task completion.
- Reading section, scroll position and checkmarks survive closing/reopening and
  app restarts. They belong to this device and guide, not an individual save slot.
- **Contents -> Reset checklist** starts a new guide run after confirmation.
- **Back to game** or Android Back closes the reader and field menu, returning
  directly to the same exploration room. Exploration input and moving-enemy
  ticking are blocked while the reader is open.

## Notes

**Add note** saves text with build version, time, current room ID/title, tracked
quest ID/title and the reading section. **Notes -> Share** opens the Android
share sheet with the selected world's notes. No note is sent automatically.

Notes stay on this device independently of game saves. Campaign and isolated
playtest sessions use separate reader preferences; the standalone playtest app
also has its own Android application storage. Clearing app data removes these
notes and reader progress. Sharing a report gives you a copy outside the app.

## Maintenance

`syncPlaytestGuides` runs before Android builds and bundles the maintained files:

- `docs/playtest/walkthroughs/WORLD_1_COMPLETIONIST_WALKTHROUGH.md`
- `docs/playtest/walkthroughs/WORLD_2_COMPLETIONIST_WALKTHROUGH.md`

There is no separate hand-maintained mobile copy or PDF dependency. The reader
supports this guide dialect: section headings, paragraphs, numbered steps,
bullets, bold text, inline IDs and link labels. Links are displayed as text.

A content revision gets a new reading/checklist namespace, preventing old
checkmarks from being assigned to newly numbered instructions. Notes remain
available across guide revisions. Reader state does not mutate quest state,
inventory, saves or scenario progression.

When reordering route chapters, update the small quest-to-chapter mapping in
`WalkthroughGuide.kt` as well. It does not duplicate the walkthrough text.

## Device acceptance checks

Not performed as part of implementation; use these in the next playtest:

1. Open both guides, navigate Contents, use This room on a first visit and a
   return visit, and search the tracked quest.
2. Check several steps, scroll, close, reopen and restart the app. Confirm the
   reading position and checkmarks persist for each world.
3. Open near a moving enemy and read for a while. Return to the unchanged room;
   confirm movement resumes without an encounter underneath the reader.
4. Add a note, reopen Notes and share it. Check its room/quest/version context.
5. Repeat in an isolated scenario; verify its reading state is separate from
   the campaign. Load another save and confirm game state was not altered.
6. Rotate and use larger Android font sizes; check reader buttons, text,
   keyboard handling and Back behavior.
7. Reset a checklist and confirm notes/game saves remain intact.
