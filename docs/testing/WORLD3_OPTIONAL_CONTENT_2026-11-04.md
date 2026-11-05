# World 3 optional-content acceptance — November 4, 2026

Scope: all six optional quests through public controller interactions, both Corporate Espionage outcomes, exact payouts, earned equipment effects, city backtracking and save/reload. Broader heist staging, narrative redesign and difficulty tuning are separate work. Changes remain local; no device installation or publication is performed.

## Confirmed findings and fixes

1. The first earned completionist route could not return from Upper City to The Static. The hub controller offers only its current hub’s nodes; the room graph had no return connection. This blocked Neon Fix’s intended core turn-in and Cold Case’s kiosk return. The existing Service Lift panel now descends to Transit Plaza after The Plan; the Transit Plaza elevator controls return to Laundry Service. Both are repeatable, check their physical room, and preserve existing progress. Earlier regional-map travel instructions were incorrect and are replaced in the guide and quest directions.
2. Phase Rounds carried `attack_element: source`, but the combat weapon resolver does not apply accessory elements. Its real +2 Agility and +2 Focus contributions were verified in combat. Removed the unused element claim and described the actual accessory effects without changing effective combat values.
3. Desktop item details multiplied percentage-point equipment values by 100. Neon Band displayed 500% critical chance and the coolant mod displayed a 2000% proc chance. The formatter now uses combat’s percentage-point units, preserving fractional points. Production reward fixtures cover 5% crit, 20% proc, accuracy and fractional values.

## Earned completionist route

The test plays Worlds 1 and 2 under production crew rules, including World 2 optional content, and saves the earned World 3 arrival. Each ledger branch starts from that same save. No inventory/AP grants, room warps outside authored player controls, or synthetic victories are used. Navigation, visible objects, visible suspect/Jax interactions, real Sentinel and rooftop combat, rest, trade and normal saves supply the route. Authored puzzle answers, virtual time and combat seed 17 remain fixture limitations.

Both branches complete all five main and all six side quests. Each optional completion is checked for exact XP/AP and persistent inventory; repeat interactions, opposite ledger outcome attempts and reload cannot pay again. A mid-Cold Case save retains its clue before the trip downstairs. Ledger theft is saved before selecting an outcome.

| Quest | XP | AP under crew rules | Other verified effect |
| --- | ---: | ---: | --- |
| Neon Fix | 200 | 0 | Consumes one earned Neon Sign Core; repaired sign persists |
| Cold Case | 200 | 2 | Intercept appears in Gh0st’s combat menu and applies Guard to him in a real rooftop fight |
| Night Market Run | 200 | 0 | One Neon Band |
| Corporate Espionage | 250 | 0 | Publication: Encrypted Ledger; private leverage: Phase Rounds; mutually exclusive |
| Prototype Testing | 300 | 0 | One Phase Rounds; private leverage plus calibration legitimately yields two copies |
| Murder in the Skyline Salon | 450 | 0 | One coolant injector and one evidence passkey; completed conversation cannot pay again |

## Equipment verification

Using the same earned pre-rooftop save, enter the real encounter and inspect production combat setup, reload normally, equip rewards through public equipment controls, save and inspect again. Compare actual combatants, then reload the pre-engagement equipped save and play the fight normally. No battle payout occurs during inspection.

- Neon Band: +2 Focus and +5 critical-chance percentage points.
- Phase Rounds: +2 Agility and +2 Focus; no weapon-element conversion.
- Coolant injector on Nova’s Mining Pistol: +4 Focus, +3 Accuracy, Freeze basic-attack element, freeze status and 20% application chance. Base weapon element precedence remains unchanged.
- Intercept: exercised through the normal combat menu; Guard applied to Gh0st.

## Verification

Final verification passed:

- **794 Android host tests**, zero failures, errors or skips, including the earned completionist routes and the full production campaign through credits.
- **14 focused desktop/controller tests**, zero failures, errors or skips.
- `:app:assembleDebug` passed; no device installation performed.
- Guide browser checks passed for direct links, saved progress, search, mobile layout and offline reload.
- `git diff --check` passed.
- [Verification, source/build hashes and fixture limits](evidence/world3_optional_content_2026-11-04/verification.json); [earned route and reward trace](evidence/world3_optional_content_2026-11-04/earned-completionist-routes.txt). Final command logs are preserved beside these records.

The targeted earned routes and combat reward comparisons pass. Additional shared-controller tests cover service-lift round trips, earlier-save restoration, physical-room guards and murder evidence inspected before accepting the case.

## Remaining human acceptance

Phone touch/readability, dialogue and notification pacing, clue discovery and the subjective value of detours remain playtest questions. This pass establishes specific public routes and effects, not every possible optional order or broad combat balance. The chrono slider, murder deduction structure and linear heist plan are unchanged.
