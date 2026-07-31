# World 1 Session Handoff — 2026-07-31

**Goal driving all of this:** make World 1 a *fantastic vertical slice*, so the patterns proven there become the formula the remaining worlds are built and rebuilt against. Extract rules, don't just patch instances.

---

## Where things stand

World 1 started this session with a professional-grade story attached to a game loop that asked the player almost nothing: 81 rooms, 12 encounters, every one a solo duel, one element answering 9 of 12 fights, and 74 of 81 rooms that never changed. The Maestro gate was at 11 of 31 flows passing.

Now: combat poses decisions, the world reacts to what the player did, darkness is a real mechanic, the cold open has an antagonist, and the gate is at 30 of 31.

### Shipped this session

| Area | Change |
| --- | --- |
| **Combat** | 12 solo duels → 11 encounters, 8 of them formations via room `enemy_parties`. Shock went from answering 9/12 fights to 5/11, Freeze 4. XP held flat — player still reaches the Warden at level 8. |
| **Stateful rooms** | `description_variants` 7/81 → 15/81, clustered on two causal beats: the MQ01 cutter surge registering across the Pit and Trade Row, and `echo_heart` re-describing itself after the Fork handshake. |
| **Dark rooms** | `mine_gas`, `echo_well`, `echo_memory`. Zero engine work — the mechanic is data-driven. |
| **Cold open** | Rebuilt. Named the Source Beast, restored the Chorus beat, ends on a threat then the STARBORN title card. 25.5s → 34.5s. |
| **Quests** | Removed the vestigial opening task from both side quests (they opened by telling you to do the thing you'd just done). |
| **Test gate** | 11/31 → 30/31 flows. Nine distinct drift classes repaired, all documented. |
| **Tooling** | `validate_maestro_selectors.ps1` (in the gate), `play_upload_check.py`, `generate_cinematic_image.py`, `build_intro_title_card.py`. |

### Two real content bugs found and fixed

- Turning on the bunk light — **the first action in the game** — permanently hid the `netting` action holding Jed's carving.
- The Red Alert variant on `launch_lift` hid **both** actions for the entire Cargo Lift sacrifice, so World 1's emotional climax played in a room with nothing to touch.

Both were caused by the same trap: **a `description_variants` or `description_dark` entry that omits an action's name makes that action untappable while active**, and `-StrictInlineActions` cannot catch it because it checks whether the name appears in *any* description.

### Commits

`6c0948a` combat + stateful rooms + gate repair · `c003b0d` Play upload tooling · `953432e` dark rooms · `3ce5f0d` cold open rebuild. All pushed to `main`.

---

## THE NEXT THING: play it

**Nobody has played World 1.** Every check is a validator, a unit test, or a scripted flow. None of those answer whether a 2-Skimmer fight is fun, whether the Chorus beat lands, or whether the Toxic Pocket is tense or just annoying.

Design calls this session were made from the story bible and the data. Some are probably wrong in ways only playing reveals. **Do not prioritise the backlog below until a human has played it** — that feedback should reorder everything.

`1.1.33` (versionCode 35) is on the internal track with all of the above.

---

## Backlog, in recommended order

1. **~18 remaining filler dead-end rooms.** 21 were pure filler (one connection, one flavour action, nothing to find); 3 are now dark rooms. Each has bespoke art, so deleting burns production value. Dark rooms proved the conversion pattern at zero engine cost. Note `mine_gas` was *already* flagged dark and simply never finished — there may be more like it.
2. **The 1.2 GB install.** `base` 136 MB + `world_assets` 1078 MB (install-time). Play accepts it, but it exhausts a 10 GB emulator and testers on real phones will feel it. Free wins first: `_archive/**` (83 files), `emotes - Copy/**`, `_old/**` all ship inside the asset pack. Then WebP. Then per-world on-demand packs.
3. **MQ02's death warrant.** Cheapest high-emotion work left. The shipped quest says "bury the liability flag"; the bible says `MANDATORY RETIREMENT / ASSET LIABILITY > PROJECTED VALUE / RECYCLE`, a camera swivelling, a silent `LOGGED`. ~1 hour, no new systems.
4. **Nova is solo for all of World 1.** Zeke joining early **breaks canon** — his entire World 1 arc is *"physically leaves his safe booth"* at the Launch Bay. The canon-safe alternative is a **Comms Relay**: Zeke assisting from behind glass during MQ04, which the bible already describes but never built. Only worth doing if the playtest says the back half feels thin.
5. **Dark rooms as a designed feature.** Jason wants them for puzzles/atmosphere/danger. See `Encounter_Design_Standard.md` §8.

### Smaller

- `save_load_roundtrip` — the 1 failing gate flow. Unstable across multiple different steps; needs restructuring, not another point fix.
- The 10 `Hub*CriticalFlowTest` harnesses wire `onQuestTaskUpdated` straight to the session store, while production routes through `questRuntimeManager.markTaskComplete()`. They structurally cannot catch banner or stage-progression regressions.
- 4 remaining `validate_maestro_selectors` findings block flipping it to `-Strict`. Two are the dark-room flows (leave until dark rooms are built out), one is `hub_rest_w1`, two are World 2 facility lines.

---

## Things that will bite you

**Maestro text matching is full-match, not substring.** `"Heavy Lifting"` matches the debug scenario card but not the quest card `Heavy Lifting: Required Training`. Same literal, two elements, one matches — reads exactly like an ordering flake. Wrap composed strings in `.*`.

**Transient UI cannot be asserted.** The `Quest Updated` banner auto-dismisses in 5s and a single Maestro hierarchy dump can outlast that. I spent a long time convinced it was an engine bug; instrumentation showed the full emit → receive → queue → render chain working. Anything with a short auto-dismiss (that banner, the 2.0s intro threat beat, the location plate) is covered by unit tests, not device assertions.

**`clearState: false` makes flows order-dependent.** Leftover text in the Debug Scenarios search box turns `inputText: "Heavy"` into a query matching nothing. Every debug-menu flow now issues `eraseText` first.

**Emulator degradation is real and looks like a regression.** Repeated 1.2 GB installs filled the AVD (7.2 GB used, install failures, cold starts slow enough to miss transient beats). Wiping reclaimed 6.4 GB. If prologue-dependent flows start failing deterministically while debug-scenario flows pass, wipe before debugging.

**Raise a timeout only when the thing genuinely takes longer.** The cold open grew 9s, so `start_new_game`'s bunk wait legitimately went 45s → 120s. That is different from padding to hide a degraded machine — which is how the suite rotted in the first place.

**`IntroCinematicAssetIntegrityTest` is a canon lock** on the cold open. It previously required the old PA line and old ending; it was re-pointed (not weakened) at the new intent with Jason's approval. Treat it as intentional.

---

## Key documents

- `docs/story/Encounter_Design_Standard.md` — encounter composition, element spread, XP budgeting, quest-opening rule, dark rooms (§8).
- `docs/story/World1_Vertical_Slice_Status.md` — Maestro drift table, the stateful-room authoring trap, resolved-bug record.
- `docs/story/world_1_mining_colony/00_world_story.md` — World 1 bible. Enemy roster reconciled with the shipped acoustic roster.
- `docs/story/NARRATIVE_CANON_MIGRATION_HANDOFF.md` — canon authority order and locks.

## Verification

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:runAssetIntegrity :app:testDebugUnitTest      # 10 validators, 225 tests
.\scripts\maestro.ps1 --device <id> test .\playtests\maestro\<flow>.yaml
```

Device installs need `STARBORN_ADB_TIMEOUT_SECONDS=900` (the wrapper defaults to 45s, shorter than a 1.2 GB install) and an `adb uninstall` first, or you get `INSTALL_FAILED_INSUFFICIENT_STORAGE`.

Release: `python scripts/play_upload_check.py` validates only; `--track internal --commit` actually ships.
