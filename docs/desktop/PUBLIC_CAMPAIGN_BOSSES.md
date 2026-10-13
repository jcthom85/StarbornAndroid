# Worlds 1-2: real boss and group encounters

The follow-up [World 3 pass](PUBLIC_CAMPAIGN_WORLD_3.md) extends the unassisted
route through the Spire and corrects the combat driver's area targeting. This
document records the preceding pass's results.

Worlds 1-2 now finish with no hidden action calls, room-position shortcuts, or synthetic victories. The runner rejects all three kinds of assistance until World 3.

## Changes

- The combat driver reads the full pending encounter descriptor produced by exploration. Selecting one member of a group no longer drops the other enemies. This also corrects the earlier mine fight, which must include both Echo Borers.
- The cargo lift fights its actual Bulwark/Buoy group. The checkpoint then fights its Dampener/Buoy group before entering the bay. The old runner invented a separate buoy victory in the bay and missed the checkpoint encounter.
- The Iron Warden is defeated through production ATB, AI, attacks, and earned abilities.
- The Beast encounter starts by selecting the visible `Face the Beast` action after confronting Gh0st. Its real combat outcome drives the quest and party progression.
- The route rests at the visible cargo-lift and ridge-plateau stops, collects checkpoint supplies, and uses available healing consumables in combat. It does not add healing stock or change enemy stats.
- The fixture uses production `GameBootstrap.newGame` equipment. Its former custom seed omitted default equipment for joining companions. Nova still obtains and equips Jed's starter gear through the route.
- Shared equip controls accept owned reward weapons and armor even when absent from the permanent unlock set. Character and gear-type checks still apply. Equipping registers the unlock through the existing session APIs. This fixes desktop reward gear silently failing to equip; Android's item listeners normally unlock it earlier.

## Verification

Seven Android test cases and three desktop test cases pass, including the six-world campaign, save/reload checkpoints, public fishing/procurement/crafting/cooking route, owned-gear eligibility, and desktop combat clarity.

The Android debug APK and Windows portable distribution were rebuilt. Asset parity validation passes. The packaged Windows launcher smoke check passes from the temporary directory with 1,328 assets, 477 rooms, and title-music decoding; this does not establish audible playback.

Worlds 1-2 record eight earned encounter victories and one real retreat. The seeded Beast trial ends with Nova at 85 HP, Zeke at 85, and Orion at 72. Full encounter IDs, item uses, rewards, and remaining HP are recorded in `app/build/reports/campaign-systems/public-worlds12-combat.txt`.

Across the six-world runner, assistance is now:

| Assistance | Original baseline | Previous pass | Current |
| --- | ---: | ---: | ---: |
| Room-position shortcuts | 1 | 0 | 0 |
| Hidden action calls | 49 | 36 | 36 |
| Synthetic victories | 23 | 19 | 15 |

All remaining assistance occurs in Worlds 3-6. The driver still uses seed 17, virtual time, immediate decisions, authored puzzle answers, disabled tutorials, and animation-completion callbacks. This is route coverage for the shared runtime, not physical device playtesting or an all-seed balance verdict. Correcting equipment and encounter groups changes earlier reward/economy observations; prior audit numbers remain historical.
