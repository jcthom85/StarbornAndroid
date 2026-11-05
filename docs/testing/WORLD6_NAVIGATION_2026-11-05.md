# World 2 console correction and World 6 navigation — November 5, 2026

This pass fixes the reported World 2 hangar action mismatch, then applies the contextual navigation review to World 6. NSEW remains the default, diagonals are prohibited, and useful named alternatives remain where they make sense. No installation, commit, push or publication is performed.

## World 2 correction

The “dark console” inspection was hidden only by its inspection milestone. A legacy save could have installed the Bridge or even departed without that earlier milestone. The powered/departed hangar description then omitted the console while the obsolete action remained visible. Its event could not meaningfully advance a completed quest.

The action now also retires when the Bridge is installed or MQ05 is complete. The initial inspection remains available on a fresh arrival. No task, inspection receipt, inventory, XP or quest completion is invented. The existing legacy-launch regression now expects the console to retire; the whole-game functional-action link audit verifies the original failure is resolved.

## World 6 route decisions

There were four diagonal direction entries, forming two reciprocal Campfire/nightmare links. The Campfire already used its east slot for a memory branch, so relabeling every link in place would conflict with another exit.

| Route | Repair and return |
| --- | --- |
| Campfire NW ↔ Infinite Cubicle SE | Campfire west ↔ Infinite Cubicle east. |
| Infinite Cubicle east ↔ Performance Review Loop west | Review Loop moves west of Infinite Cubicle, returning east. Break Room Exit remains south of the loop, returning north. Their local positions follow those directions. |
| Campfire NE ↔ Silent Shore SW | Campfire east → existing Blue Ash Circle east → Silent Shore. Return west/west. Blue Ash Circle retains its north memory branch. |
| Campfire north ↔ Endless War south | Preserved, including the north branch to Gh0st's nightmare encounter and its return. |

All 65 rooms remain, with 128 directed local connections. All are NSEW, every link has an immediate opposite-direction return, and no room has more than four compass exits. Room IDs, node ownership, content, rewards and the nightmare fights are preserved. Six cross-node transition records are replaced by six matching the new approaches.

Campfire, Blue Ash Circle, Infinite Cubicle, Review Loop and Silent Shore descriptions explain the new directions, including post-reassembly variants. Source Morning Street no longer advertises a nonexistent tapped alley shortcut; it describes the southern walking return. The New World's west route is framed as revisiting the remembered Source, consistent with the existing postgame return connection.

## Story crossings and retreat

Four previously open east exits allowed walking past unfinished story steps. These now use permanent physical clearance flags set by the existing successful events:

| Forward crossing | Opens when | Return |
| --- | --- | --- |
| Memory Bridge east → Shared Memory Span | All four anchors are set and the player uses the unfinished bridge. | Span west remains usable. |
| Singularity Threshold east → Memory Stair | The campfire promise is shared and the player uses the threshold. The existing first-crossing transfer remains. | Stair west remains usable. |
| Memory Stair east → World-Fracture Landing | Nova refuses all three revisions and uses the memory stair. | Landing west remains usable. |
| Memory Defense Arena east → Center | The existing quest records victory over the memory defenders. | Center west remains usable. |

Setting the last anchor alone does not build the bridge. Sharing the promise alone does not perform the threshold crossing. Refusing the last revision alone does not climb the stair. A mere visit to the arena does not count as defeating its guardians. The locks explain the next required action. They do not replace the objectives or award anything themselves.

The Center's pre-existing east ending gate remains. Both Ascended battles and final tuning are still required; the pending cinematic save/reload and single ending payout remain covered. Westward rest access to Shared Memory Span stays available before and between final battles, and the Campfire remains reachable along the mine route. Exploration of the mine echo before reassembly is left available; that visit cannot construct the bridge or complete later quests.

## Older saves

The migration restores physical passages from existing quest tasks, completed events, completed quest milestones, later active story quests, or saved/visited locations beyond the relevant crossing. Later receipts also establish the earlier passages needed for backtracking. Four anchors without the build action provide no bridge clearance. Merely carrying the Key or rescuing the crew provides no crossing clearance.

A saved New World location also preserves the existing physical return from the remembered Center into the epilogue. This does not set game completion or credits, grant rewards, or authorize the gated postgame shuttle without actual completion. Migration is idempotent and does not move the player or create quest credit.

## Named alternatives retained

All nine authored memory return actions are preserved: trail, door, clean signal, harmonic wave, echo from First Fear, light beam, slide, return circuit and echo from Voice Orbit. They remain repeatable tapped conveniences with complete swipe routes for walking back into their branches. Public controller checks cover their clickable shared description links, use after old completion receipts, repeated trips, walking back, reload and unchanged inventory/XP.

No Throne previously used the same “circuit” name for a flavor inspection and its actual return action. The flavor interaction could claim the clickable text. The travel control now has the distinct name **return circuit** and its own explicit sentence; the flavor circuit remains. The controller test checks the actual return action identity in the description link, rather than merely finding a matching word.

Story transfers remain at reassembly, the mine elevator, the singularity crossing and the ending cinematics. The deliberate Astra shuttle control remains at Source Morning Street after game completion. No diagonal is retained as a story exception.

## Verification and limits

Controller tests walk all 65 rooms and 128 directed edges using compass travel and opposite-direction returns. They also cover old saves in the former diagonal routes, premature crossing attempts, the actual anchor/build/refusal/climb actions, guardian victory callbacks, legacy return access, rest access and named shortcut repetition. The callback fixtures verify controller transitions; the separate earned complementary Worlds 4–6 campaigns play actual combat, both nightmare orders, both HR outcomes, every World 6 main and optional quest, final tuning, pending-ending reload and credits.

The earned campaigns use production crew progression and earned supplies. They supply known puzzle answers and use a fixed combat seed and predetermined builds. Room coverage seeds cleared progress. Neither fixture establishes subjective discovery, fun, arbitrary-build balance or phone touch acceptance.

The [World 6 walkthrough](../playtest/walkthroughs/WORLD_6_COMPLETIONIST_WALKTHROUGH.md) now includes the repaired approaches, four story locks, all named travel exceptions and all 65 room/exit references. It remains local; the published reader still covers Worlds 1–3.

Final validation passed: **808 Android host tests**, **41 focused desktop/controller tests**, the global navigation audit, all 65 walkthrough references, `git diff --check`, and `:app:assembleDebug`. All 40 JSON catalogs packaged in the APK match source. The earlier expanded desktop run exercised 266 tests: its only failure was the newly detected No Throne return-link collision, subsequently fixed and covered by the passing final run. One existing copied-save test was skipped because its external fixture is absent.

The shortcut inventory fixture marks the existing Echo Mines Astral Thread pickup as already collected, so walking through the mines does not conflate a legitimate first-visit reward with a shortcut payout. The production pickup event is unchanged.

Final counts, command logs, source comparisons and APK catalog hashes are recorded in [verification.json](evidence/world6_navigation_2026-11-05/verification.json).

## Phone acceptance remaining

Check that the Campfire's west/east routes are easy to understand; that passing through Blue Ash Circle feels natural on the way to Orion; that each story lock points clearly to its required control; and that rest trips between final battles remain comfortable. Confirm ending replay, the remembered westward return and the shuttle on the player's own save/build. No additional broad redesign is implied by these acceptance checks.
