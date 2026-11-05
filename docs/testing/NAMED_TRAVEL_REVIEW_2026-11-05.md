# Named travel review — November 5, 2026

Follow-up: [World 2 correction and World 6 navigation](WORLD6_NAVIGATION_2026-11-05.md) resolves the World 2 console finding and the remaining four World 6 diagonals discussed below. This report retains its original verification results.


This follow-up corrects an overly strict interpretation of the navigation preference. NSEW swipes are the default. Diagonals are prohibited. A small number of clearly named, physically plausible tapped routes can supplement the walking network. One-way travel requires an actual story commitment or mechanism; an unexplained missing return is insufficient.

The World 3 and 5 passes removed useful vertical routes along with the diagonals. Four named routes are restored or rebuilt as reciprocal alternatives. None replaces an existing NSEW connection.

| World | Route | Interaction and return | Context and gate |
| --- | --- | --- | --- |
| 3 | Underrail Platform ↔ Transit Plaza | Tap the platform-stairs Paths link at either end. | A direct platform/plaza connection fits the setting. Explicit stairs and both landings replace the unexplained one-way descent. Available before and after the heist. |
| 3 | Service Lift ↔ Archive Vault | Tap the inspection-lift Paths link at either end. | A separate inspection cage makes the old descent understandable and reversible. Room prose and the inspection-hatch response distinguish it from the main lift panel to Lower City. The existing cleared city-lift rule remains intact. |
| 5 | Service Shaft ↔ Server Farm | Tap the named cable descent or climb. | The shaft already describes maintenance cables. Anchoring them supports a plausible climb back. The Zero-G Junction / SysAdmin walking route remains available; its optional magnetic lab gate is unchanged. |
| 5 | Firewall Gamma ↔ Anchor Chamber | Tap the access-lift Paths link at either end. | Replaces the old one-way drop with a two-way access lift. Both directions require the same permanent Construct clearance as the Farm/Anchor doorway. Disabling Gamma alone does not open it. Existing legitimate legacy access also opens the lift. |

Glasswalk → Archive stays removed. Its original prose identifies a glass walkway, rail and surveillance, but no lift, chute or irreversible story event. An invented drop adds little beside the short east-to-Donor / south-to-Archive walking approach. The description now points toward that approach. This decision is about an unexplained mechanism and return, rather than a blanket rejection of shortcuts.

World 4 had no comparable removed vertical route to restore. Its repaired routes and assembly gate are unchanged. Removed diagonal links remain removed in Worlds 3–5. World 6 still has four existing diagonal direction entries awaiting its own contextual pass; this review does not claim game-wide diagonal elimination.

## Preserved behavior

Compared with the start-of-review snapshot, every existing cardinal connection, room ID, coordinate, gate, encounter, item and quest definition is unchanged. Nine room records receive travel cues; eight receive a named vertical connection. Eight corresponding cross-node transition records are added. The only event change is the Construct victory message explaining the lift. Quest conditions, state changes and rewards are unchanged.

World 3 has 70 rooms, 142 directed cardinal connections and four directed named vertical connections. World 4 retains 74 rooms and 146 directed cardinal connections. World 5 has 81 rooms, 160 directed cardinal connections and four directed named vertical connections. Every restored connection has an immediate matching return. Some rooms now have more than four total connections, but never more than four compass directions; the extra controls are named Paths links.

## Verification

The structural audit rejects new diagonals and unreviewed alternatives in Worlds 1–5, verifies reciprocal connections, cross-node metadata, geometry and the Anchor lift clearance requirements. The existing unreachable launch artwork exception is unchanged.

Controller tests select the actual TravelAction exposed to the player and check that its named link appears in the shared inline Paths presentation, rather than as an unsupported compass direction. Every restored pair is used down/up with a save/reload at the lower landing, across earlier and later quest phases. Movement itself grants no XP or inventory. Additional checks cover blocked Gamma travel before victory, including after node shutdown and reload; the normal victory callback opening the lift; and older chamber occupants retaining a return without boss credit.

The room-coverage tests still walk every cardinal connection in Worlds 3–5: named shortcuts cannot conceal a broken walking network. Earned World 3 completionist runs cover both ledger outcomes, with the complementary route using the stairs and inspection lift. Earned Worlds 4–6 runs cover both branch sets through credits, with the alternate World 5 route using the cable descent/climb and the post-victory Anchor lift. Those runs use real combat, earned resources, known puzzle answers, fixed combat seed and predetermined builds. They establish progression and return access, not subjective fun or balance across arbitrary builds.

The World 3 and World 5 walkthroughs now distinguish NSEW Exits from named Tapped Paths. Their complete 70- and 81-room references match the revised data; World 4's 74 references remain consistent. The local Worlds 1–3 reader is rebuilt and browser checked, including its named-route references, mobile layout and offline reload.

Final results: **805/805 Android host tests passed**; **34/34 focused desktop navigation/progression tests passed** within the expanded desktop run. The full desktop run reported **261 tests: 259 passed, one failed, one skipped**. The failure is the unchanged World 2 hangar finding below. The skip is `World2Part2PersistenceTest.copiedPlaytestSaveCanGatherWithoutInventingCraftCredit`, whose external copied-save fixture is absent and triggers its existing JUnit assumption. The navigation audit, reader browser checks, walkthrough-reference checks, `git diff --check`, and a separate `:app:assembleDebug` all passed. All 40 JSON catalogs in the APK match source.

Final counts and build hashes are recorded in [verification.json](evidence/named_travel_review_2026-11-05/verification.json). The [source comparison](evidence/named_travel_review_2026-11-05/source-delta.json) records the limited production-data changes. An expanded desktop audit also identified an existing World 2 hangar description/action mismatch: the Bridge-installed variant lacks a link for the still-visible “dark console” action. That room is identical to the start-of-review snapshot and is recorded as a separate finding.

## Remaining acceptance

Phone playtesting should confirm the stairs/lift/cable labels are easy to discover, the two lift mechanisms at Service Lift are understandable, and the Anchor lift feels natural after the Construct battle. No phone touch, presentation or subjective-fun acceptance is claimed. The APK and updated guides remain local; nothing was installed or published by this review.
