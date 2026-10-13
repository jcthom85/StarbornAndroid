# Fishing, tinkering and cooking refinement

The subsequent [food and fishing economy audit](FOOD_FISHING_ECONOMY_AUDIT.md)
adds production enemy-AI and ATB-speed modeling plus recipe/income measurements.

Implemented the approved system-by-system plan on Android and desktop.

## Tinkering

Mods use the four real weapon/armor modification slots. Owned copies can be
equipped independently across characters, with one of each mod type per
character. Moving an assigned copy requires an explicit source and confirmation;
cancellation leaves equipment unchanged. Selling and scrapping protect assigned
copies while allowing spares.

Comparisons identify the destination, replaced item, signed stat changes and
effective weapon element using the same precedence as combat. Locked slots and
missing parent equipment explain their requirements. Save reconciliation favors
explicit character assignments, respects owned quantities and repairs invalid
legacy bindings without creating copies.

## Cooking

Mineral Trail Mix grants +40 maximum HP instead of +120. Its recipe, discovery,
defense ability and cooldown remain unchanged. Restored HP is clamped to the
current legal maximum without healing characters or changing meal duration.

The food audit records 1,000 seeded combat trials in
`desktopApp/build/reports/food-balance.csv`. In the boss fixture, passive Trail Mix
won 13/50 trials at +120 HP and 7/50 at +40, versus 2/50 without provisions.
This supports retaining the +40 trial, not a final balance verdict: the fixed-turn
basic-attack policy excludes live ATB speed benefits, human decisions and
pre-fight meal healing. Automatically using the snack also spends attack turns.

## Fishing

Gentle and steady behavior remain unchanged. Other behaviors vary calm, pull and
recovery durations by seeded encounter, retaining warning windows and a minimum
840 ms duration. Every third clean native catch grants one additional fish.
Ordinary catches, failures and salvage neither advance nor reset progress.
Progress persists as protobuf field 55, defaults to zero for old saves, and is
shown in results. Securing the same result again cannot grant another reward.

## Validation and limits

The full desktop suite passed 127 tests and the shared runtime suite passed 28.
After the legacy-import correction, all 160 selected Android tests and six
affected desktop flow tests passed. The refreshed portable launcher passed from
the Windows temporary directory, including title-music decoding; all 1,328
packaged assets matched their source bytes.
Focused coverage includes transfer cancellation/confirmation, spare-copy
protection, save migration, HP restoration, meal replacement, batch cooking and
1,500 seeded rod/behavior combinations. Rendered desktop previews cover compact
crafting, the transfer dialog and fishing results.

Automated timing policies and screenshots do not establish physical touch,
keyboard feel or audible music playback. Those remain the next playtest checks;
the packaged smoke test verifies title music decoding and resource loading.
