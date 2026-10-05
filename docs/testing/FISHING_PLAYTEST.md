# Android fishing pass

## Intended experience

Fishing is an optional source of ingredients and salvage, with a persistent field journal. Read the fish: reel during Calm and Recovery, watch Pull, and release during Surge Coming and Surge. Giving slack through a surge preserves progress. Fish stamina decreases over time. Stronger rods provide modest speed and forgiveness; all native species can be caught with the Field Rod, and the strongest rod still requires line control.

## Campaign progression

- The W2 pod provides the Field Rod and Plain Chime Lure.
- David's Camp & Provisions sells replacement Plain Chime Lures, Resin Rods, and Glimmer Lures.
- Sentinel Scraps in W3 sells Carbon Thread Rods and Ghost-Signal Lures. Their bases make the existing advanced recipes reachable.
- Crafting the Magnetic Salvage Lure uses scrap metal and wiring and keeps the Plain Chime Lure as a reusable tool.
- The first clean native species earns a Glimmer Lure once.
- Three different native species caught cleanly earn a Ghost-Signal Lure once. Catching the same species in multiple zones counts once for this challenge.
- A native fish in each of all six waters awards Master Angler and the exclusive Six-Water Lure once. Junk and salvage do not count.
- Records update at each catch and survive cooking, selling, and save/reload. Existing inventory alone does not fabricate old catch records.

## Manual checks

1. Start at the W2 crash through the campaign, examine the pod, then reach Sector 9 fishing. Compare this with the stocked Fishing debug scenario; the debug scenario cannot validate acquisition.
2. On the first bite, wait ten seconds before tapping Strike: the practice bite must not expire. After hooking, spend ten seconds reading the safe briefing. Start reeling explicitly. Fail, cancel, and retry; the briefing should not punish reading, and canceled briefings should remain available.
3. Land five fish using the Field Rod. Check whether the warning is clear early enough, giving slack feels protective, and the fight stays enjoyable on repeated casts. Note species and time rather than judging one lucky cast.
4. Compare Field, Resin, and the strongest rod against common fish, eels, and Void Rays in the debug fixtures. Stronger equipment should help while surges remain important. Holding continuously against a Void Ray should snap even the strongest rod.
5. Land a clean native fish. Check the lure reward and journal immediately, then use Fish Again. Catch another fish and fail a later cast; earlier inventory, records, and rewards must remain.
6. Catch three different species cleanly. Verify the second reward once. Catch each again and check there are no repeated rewards.
7. Catch salvage at each zone: it should use a short steady retrieval, display salvage wording, and not satisfy Master Angler. Then catch a native fish at all six zones and verify the exclusive Six-Water Lure once. On an old Master Angler save, catch any native fish to receive the new lure without repeating the achievement. Record all seven native species for the Field Medallion; record a clean catch in every water for Clean-Water Angler.
8. Sell or cook a recorded fish, save, and reload. Journal entries and unlocked reward milestones must remain; the consumed fish should not return.
9. Craft the salvage lure from the real tinkering bench. Verify the plain lure remains and both can be selected afterward.
10. Disable Haptics and fish through every phase. Check silence of the vibrator while sound effects still follow audio settings. Check optional motion hooking separately from touch.
11. Background the app during waiting, hooking, and reeling. Resume after ten seconds. The hook/fight should retain its time/state; a held reel should resume released.
12. On a short display and with large touch targets, reach all setup and result controls by scrolling. During waiting, hooking, and reeling, verify controls remain pinned at the bottom and are usable after casting from a scrolled setup. Check the field menu Journal also shows fishing records. Select gear, leave, reopen, and save/reload: owned selections should persist. Verify screen-reader reel activation toggles reeling/slack. Compare backgrounds and descriptions across all six waters.

## Focused automated command

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest --tests 'com.example.starborn.domain.fishing.FishingRefinementTest' --tests 'com.example.starborn.domain.fishing.FishingServiceTest' --tests 'com.example.starborn.domain.crafting.ProvisionIntegrationTest' --tests 'com.example.starborn.data.DataIntegrityTest' --tests 'com.example.starborn.domain.playtest.EconomyCatalogTest' --tests 'com.example.starborn.domain.session.GameSessionPersistenceTest' --console=plain
```

Automated balance checks validate the arithmetic and state flow. Device play remains necessary for response time, motion sensing, sound/haptics, accessibility, and enjoyment.

## Automated results — October 1, 2026

The command above compiled Android production and test code and passed 89 tests with zero failures, errors, or skips:

- FishingRefinementTest: 15 tests, including all 30 authored rod/behavior combinations, continuous-hold failure, per-catch journal/reward records, gear source paths, background assets, pause behavior, safe first-hook briefing, repeated real casts, and retained starter lure after crafting.
- FishingServiceTest: 9 tests.
- ProvisionIntegrationTest: 7 tests.
- DataIntegrityTest: 30 tests.
- EconomyCatalogTest: 6 tests.
- GameSessionPersistenceTest: 22 tests, including journal and reward milestone autosave round trip.

Gradle emitted existing deprecation warnings. This run did not include device playtesting.

## Automated verification (October 1, 2026)

FishingServiceTest (9) and FishingRefinementTest (15) passed in the focused system run. The full Android suite also passed all 622 tests. Device playtesting of timing, haptics, readability, and game feel remains outstanding.

## October 5 refinement pass

Gentle fish, steady pullers, darting fish, drifting eels, paired-wave tetras, and ether rays now have separate rhythms with advance surge warnings. Fish energy responds to safe reeling and surge protection. Rare lure bonuses now directly boost rare-or-better catch weights (half that bonus for uncommon catches); attraction and local bonuses still specialize the lure. Compare repeated catches before deciding final tuning.

The new code was compiled without running tests. Previous test totals below or in earlier reports do not verify this pass. The existing Master Angler reward assertion was updated for the exclusive lure.
