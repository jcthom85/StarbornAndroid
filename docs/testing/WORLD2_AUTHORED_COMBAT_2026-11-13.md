# World 2 authored combat — November 13, 2026

Implemented the World 2 mechanics pass from the encounter plan. This follows the shared prototype and World 1 Warden pass. Worlds 3–6, final enemy artwork and a fresh full-campaign playthrough remain outstanding.

## Result

World 2 retains 11 combat opportunities. Its nine identities now have distinct behavioral roles:

| Enemy | Implemented behavior |
| --- | --- |
| Mist Glider | Forms a short-lived mist membrane, marks a dive, then exposes itself to recover. Replaces the borrowed Siren Skimmer at Mist Pools only. |
| Moss Borer | Closes its carapace, warns of a mineral exhale, then recovers. Cavern partners begin on different beats. Replaces the borrowed Echo-Borer pair only at Glow-Moss Cavern. |
| Shard-Hound | Marks prey before pouncing, then repositions. Duplicate hounds start on staggered cycle positions. |
| Spore-Spitter | Warns with a swelling bulb before one targeted acid/Weak attack, then recovers. |
| Razor-Vine | Coils before weakening a target. Any damaging hit during preparation cuts the coil and forces recovery. |
| Stalker-Vine | Shelters the plant formation once and has two finite group-healing reserves. Remaining root reserves are visible. Interrupted preparation does not spend a reserve. |
| Ruin-Guardian | Braces once, announces a heavy relic strike, then exposes its joints. |
| Sentinel Orb | Announces its targeted iris discharge, exposes itself to recover and alternates with static pressure. |
| Source Beast | Patient marked lunges become a once-only frenzy below half HP: a harmless announcement, two separately warned single-target strikes, then a longer recovery. Overextension reduces defense. |

The new family definitions deliberately use existing temporary art. They do **not** yet satisfy final visual uniqueness. The retained enemies also await their planned art/state review. No final images, music or sound effects were generated during this pass.

## Rewards, story and save compatibility

Existing enemy stats, resistances, XP, credits and drops are unchanged; new Moss Borer/Mist Glider definitions inherit their replaced opportunity's existing budget. Guaranteed herbs, Beast Meat, Scrap Metal, Wiring Bundle and Battery remain at their named encounters. All earlier reduced Canopy/Hound Nest instances remain.

Acoustics Lab now has a reduced Sentinel Orb / Spore-Spitter pair. It keeps the old single-plant budget of 55 XP, 15 credits and the existing plant drop roll. The extra Orb does not add wiring/battery rolls or a full additional reward. Ordinary Orb encounters retain their original payouts. This uses a room-specific reward view, with explicit instance HP/vitality/stability tuning.

The Beast retains its existing stats and single 500 XP / 250 credit payout with one guaranteed Source Resin. `Face the Beast` stays player-selected. Its phase is not a quest victory, does not recruit Gh0st and does not skip the failed-trigger/Anchor drill sequence. Actual public progression verifies those story beats through the World 3 arrival.

The Signal accepts the new cavern identity while retaining the Stalker-Vine and old Echo-Borer alternatives. Historical cleared-room receipts and victories returning from already-running old battles translate only in the three changed rooms. A previously cleared Acoustics Lab does not gain a new Orb encounter. The Pit keeps its original Echo-Borer/Siren Skimmer definitions.

Loading a different save in the same room now refreshes the enemy rail along with its room model. Previously, an unchanged location could leave the old rail visible until another movement refresh. Both earlier uncleared and later cleared restores are exercised. Normal combat gates and retreat routes remain.

Warnings, phases, cycle positions and visible resource counts are combat state. Reopening an interrupted battle still uses the existing battle-entry restart checkpoint; exact midfight restoration is not newly implemented.

## Verification

**144 focused checks passed:** 40 runtime tests, 94 Android unit tests, seven desktop tests and three Android instrumentation tests. The broader Android suite was not rerun or declared green; the preceding audit recorded baseline failures outside this scope. One older support test was updated to expect Root Reserve's cooldown rather than the retired Nature Heal action; a stale generic-action wording expectation was corrected without changing its quest-eligibility assertions.

- **Earned main route:** starts at the previously recorded late-World-1 checkpoint, fights the remaining launch encounters, plays all five World 2 main quests, reaches World 3 and reloads slot 8. Optional World 2 fights are skipped or retreated where allowed. This is not a new-game run, and the World 1 checkpoint contains previously earned side-quest progress.
- **Earned completionist route:** the same launch checkpoint, all 11 World 2 combat opportunities, all ten main/side quests, World 3 arrival and slot 9 reload. No synthetic combat victories, resource injection or assisted actions. New Cavern credit and every guaranteed encounter supply remain available through the public route.
- **Beast strategies:** twelve real fights: earned main and completionist arrivals at levels 5 and 6; seeds 17, 41 and 73; direct basic-attack/healing and marked-target Guard/basic-attack/healing policies. No optional combat abilities are used by these policies. All won, phase triggered once, warnings appeared and the original payout was exact. These deliberately simple policies establish viable responses, not perfect balance or human enjoyment.
- **Finite support / pack rhythm:** real runtime casts exhaust two visible root reserves; shelter casts once; healing reaches the plant formation. Pack members' first actual actions differ. Pure rules check damage-only interruption and scope of historical identity translation.
- **Save/gate checks:** desktop public interactions exercise both World 2 combat gates, historical cleared saves, fresh saves in the same room, and old battle-result IDs. These compatibility tests inject result payloads to test cleanup; the separate earned routes supply actual combat evidence.
- **Presentation:** Android instrumentation checks an actual Beast preparation at 130% font scale (using the compact crew layout so the boss and its HP remain readable), plus the existing five-target proof at normal and short/large-text sizes and distinct target dispatch. The Beast presentation fixture is a stocked Test Session, not balance evidence. Desktop combat flow, formations and portrait/wide multipart controls pass.
- **Asset contracts:** existing stats/rewards/drop definitions and named guaranteed supplies, room-specific remix payout, scoped identities and authored-skill references/limits are checked. The planning validator accepts recorded exact source reconciliations while retaining the original baseline snapshot.

Logs, route traces, combat traces, XML results, source-contract checks and the Android screenshot are in [the evidence folder](evidence/world2_authored_combat_2026-11-13/). Earned Beast arrival fixtures are under `app/src/test/resources/playtest/`; their provenance is the public-route capture above.

## Delivery and next step

The local World 2 completionist walkthrough reflects the new names, mixed Lab encounter and boss rhythm without renumbering. It was not published. No version increment, commit or push was performed. Only the separate `.playtest` package on the isolated laboratory emulator was updated; the original emulator and campaign saves were not replaced. No physical Pixel was connected.

Worlds 1–2 are now ready for the combined hands-on playtest before World 3 implementation. Check warning comprehension, Guard's usefulness, whether the stronger crew can suppress too much boss behavior, recovery pacing and how the new patterns feel without known answers. Final art follows that review.
