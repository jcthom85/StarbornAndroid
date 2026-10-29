# World 2 focused phone playtest

Companion to the [October 29 readiness report](../testing/WORLD2_READINESS_2026-10-29.md). Start every row **NOT RUN**. Host tests do not tick phone acceptance boxes. Preserve a failing save and record PASS / FAIL / NOT RUN / BLOCKED, expected/actual result, room/action, build and screenshot/video.

## Before starting

- [ ] Install the updated APK. The earlier Pixel installation predates the World 2 fixes and bunk-light fix. Record APK SHA-256, base commit plus dirty state, version, Pixel/Android, date and tester.
- [ ] Record earned arrival versus debug scenario, slot, tutorials, font size/accessibility, audio, roster, levels, gear/mods, AP and consumables. Use an earned World 1 exit save for the primary route; scenarios are separate functional evidence.
- [ ] Save personal progress first; use separate slots. Scenarios reset a test session. Reset-based Maestro belongs only in the isolated `.playtest` package.
- [ ] Keep pre-awakening, pre-Beast, pre-drill and pre-departure saves. Finish local content before departure; stop this assessment at the landing/handoff boundary.

## Suggested order

| Check | Steps and expected result | Result / evidence |
|---|---|---|
| P01 — Arrival | Finish crash Zeke dialogue. Party already contains Zeke; no new recruitment/rewards on repeated talk or reload. Inspect console, then use Pod Interior emergency medkit; exactly one Medkit consumed. Enter Stream Bank: MQ01 completes once, MQ02 starts/tracks, Zeke is tappable. **Stabilize before using/selling that Medkit elsewhere.** | NOT RUN |
| P02 — Friendly NPC exits | Charlie: leave before/between/after trust stages; gift once. At Resonant Falls, talk through David and Penny, then use their leave options. Megan at Coastal Resilience Station must also let you leave before/after favor interactions. Navigation returns immediately; no gift on leaving. | NOT RUN |
| P03 — Kaelen | Meet at Sandy Cove, collect first supplies once, leave/revisit and repeat hazard reminder. No extra ration/antidote. Accept favor; try one herb + one meat: no reward or material loss, clear shortage. Bring two herbs + one meat: one sight/150 XP/200 credits, actual quantities removed; revisit, no repeat payout. | NOT RUN |
| P04 — Wildlife and map | Explore canopy/coast and return across nodes/hubs. Check minimap position/routes and full-map services; locked directions should be legible. Fight flora/hounds/spitters/skimmers with earned gear; inspect statuses and use counterplay. Record actual hits, damage, recoveries and missing information. | NOT RUN |
| P05 — Temple barrier | Before using stone lock, east must remain blocked with useful feedback. Clear an accepted vine/borer encounter, locate gate, use Chime: open, MQ02 completes once, MQ03 starts. Return through the gate and reload an already-open save: no relock/reward repeat. Alternative Borer victory is accepted; note if “clear canopy” wording confuses. | NOT RUN |
| P06 — Stasis | Read Hall murals, pod, Observation overview, Coolant garden. Before all four, ring console explains prerequisites. Try wrong answers at each decision, leave/reopen and save/load. Correct sequence wakes Orion; return west and finish the full conversation. Only then recruit him; separately open Bridge cradle for MQ03 completion/250 XP. No repeated recruitment, AP or Bridge. Judge pacing. | NOT RUN |
| P07 — Botanist, critical payout | Accept from Zeke at Stasis after awakening/recovery. Scan moss at Crash Site, ferns **and** spores at Resonant Canopy, weeds at Tide Pools, roots at Stasis. After scan five, journal still requires **return to Zeke**. Record XP and painkillers; turn in gives exactly 100 XP + two painkillers and one 2-AP claim. Repeat/reload: no further payout. Zeke remains available at Stasis. | NOT RUN |
| P08 — Orion optional offers | Before finishing Hunter, accept Tideglass Day, then Ancient Echoes. If deferred, resolve Source Gate and inspect Astra, then return to Orion: both offers should still be reachable before departure. An active Ancient Echoes hint can precede the other offer until that quest finishes; no local quest should require departing. | NOT RUN |
| P09 — Lost Patrol | Alpha (canopy remains or Spore Thickets), beta (Poison Hollow), gamma (Sniper Lookout), then cache in Burned Glade. Use the long southern loop before owning cutter. Learn blueprint and craft from supplied damaged cutter/wire/scrap. Completion gives 100 XP; actual cutter opens optional vine shortcuts, persists through reload and cannot be sold. | NOT RUN |
| P10 — Tideglass/chorus | Re-enter actual Tide Pools after accepting, gather ingredients, learn/craft Source Resin: 150 XP + two painkillers. Existing Beast resin does not count as the craft. Revisit west/east/north crystals after Ancient Echoes acceptance, solve relief/matrix sockets: 150 XP + Focus Conduit once. Try wrong/leave/reload; ordinary Orion talk must not solve it. | NOT RUN |
| P11 — Prism and vault | Solve Hidden Grotto Prism visually before looking up a target. Check sliders/beam/fringe/brightness, wrong submit and cancel/reopen; one relic payout. Drain sluice, enter vault, open reliquary once; save/load and return. Check no stranding or duplicate loot. | NOT RUN |
| P12 — Beast | Before fight, save and record Nova/Zeke/Orion builds. Confront hunter; observe complete dialogue. Fight, then test retreat/defeat/retry on a separate save. Victory recruits Gh0st once and makes him tappable on ridge. Compare with Warden; record turns, recovery items, knockouts, statuses and Corrosive Rounds. Repeat from comparable saves if possible. | NOT RUN |
| P13 — Anchor Drill / Link | Before full scene completion, Hunter remains active and Link is locked. Normal finish, skip and save/resume should each complete once: 300 XP, Link, next quest. Test party heal/Regen in subsequent combat and verify skill units/targets. Note explanation/tutorial timing and disable-tutorial behavior. | NOT RUN |
| P14 — Stolen Tech | Talk to newly visible Gh0st on ridge after drill; accept. Gantry grid decision → hatch bypass → Transmitter Site case → learn blueprint → craft capacitor. Check 150 XP once, materials and journal. Premade toolbox capacitor does not finish this quest. **Known issue: capacitor currently grants Speed/Agility, despite cooldown wording.** | NOT RUN |
| P15 — Source barrier / salvage | East blocks until horn, cup, gauge, breakers and lock decisions are completed. Try wrong answers and leaving; gate stays shut until success. Solve, inspect Astra, use salvage route to Pod Fragment, recover conduits and return. Cross-hub/map state updates; completed gate remains open after load. | NOT RUN |
| P16 — Services / inventory | Rest and Cook are separate controls, no duplicate actionable bed/cooker prose. Check actual HP restoration and already-full behavior; cooking uses ingredients. David/Sentinel shops can open/close and cannot sell unique progression items. Check all nine containers on revisits/load; no duplicate loot. | NOT RUN |
| P17 — Install and point of no return | With Chime/Bridge/conduits, install Bridge once; inventory loses Bridge and systems activate. Cancel launch, perform another local action, return. Confirm only when ready. Projection rooms are a preview, not another playable flight or shield crossing. | NOT RUN |
| P18 — Departure boundary | Normal/skip/background/resume from separate saves: one 350 XP/4-AP completion, full party retained, new quest active/tracked at landing, no duplicate launch sound at final impact. Stop before playing the next world for this assessment. | NOT RUN |
| P19 — UI/audio throughout | Large text, crowded NPC/item rails, navigation visible but disabled during dialogue, quest-banner swipes, ability Details, Guard Break duration and reward/tutorial/quest overlap. Listen at awakening, Beast, drill and departure; record overlapping/restarting cues. A host route cannot judge these. | NOT RUN |

## Separate reproduction saves

- [ ] Spend/sell the last ordinary Medkit before treating Zeke. Can the player understand the missing requirement and obtain a replacement from David? Preserve the save if recovery is confusing, unaffordable or blocked.
- [ ] Load an in-progress five-scan Botanist save from the older build and attempt the new report task. Separately record already-completed but unpaid old saves; no automatic restitution was implemented.
- [ ] Quit/save during Gh0st confrontation. Its event credits confrontation before the final dialogue line; check whether an important beat becomes unavailable after return.
- [ ] If possible, revisit old opened Temple/Source gate saves. Reconciliation must reopen the physical route without replaying XP or items.

## Optional fresh-game spot check requested during assessment

- [ ] Start New Game and tap **bunk light** while the opening fade is still running. Fade ends immediately, “The bunk light coughs on…” appears and the west-door lock is visible. Repeat near the fade's end/during the former pause. No late extra popup or restart; isolate fault and open the door normally.

Do not use instant victory/OP mode for balance verdicts. Do not mark unplayed optional encounters, audio, fonts or lifecycle cases PASS because chapter completion worked.
