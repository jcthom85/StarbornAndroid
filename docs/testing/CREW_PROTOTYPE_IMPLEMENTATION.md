# Full-crew progression prototype

[Tempo tuning follow-up](CREW_TEMPO_TUNING_2026-10-15.md): two prototype encounter spikes now use lower enemy base Speed after paired and fresh-seed validation. Support abilities and AP contracts retain their earlier values.

Follow-up: the [automated full-party comparison](CREW_BALANCE_PASS_2026-10-15.md) now covers sixteen seeds, nine encounters, equal-budget allocations, an unspent control and attrition sequences. It identifies difficulty spikes and policy-sensitive perk value; final balance remains open.

October 15, 2026. Supersedes the Nova-only scope in the [earlier record](NOVA_PROTOTYPE_IMPLEMENTATION.md). This is an opt-in build experiment; production migration and final balance remain pending.

## Playable scope

All 32 nodes from [candidate 1](../design/PROGRESSION_DESIGN_PACKET_CANDIDATE_1.md) now have runtime effects: two four-node paths for each of Nova, Zeke, Orion and Gh0st. AP is shared. Fourth nodes require Foundry completion. Earned core skills remain separate from purchases.

Zeke supplies Cover, stability recovery, emergency cooldown bypass and Weak/opening setup. Orion supplies percentage healing, triage, aftercare and Prism Lance/Weak improvements. Gh0st supplies conditional Headshot damage, critical chance, basic-attack charging, stability pressure, Disarm and one-use Stun/Stagger recovery. Nova retains her Break and finishing paths. Talent HP bonuses share a +25% cap; stability bonuses have an independent +50% cap.

Orion receives free Field Patch: Focus-scaled healing plus a one-turn Shield. Field Treatment instead uses target maximum HP. Aftercare uses cast-time maximum HP, never revives, and yields to Regen. Support targets must be living allies. Recenter preserves its charge when another disabling status still prevents acting. Invalid actions do not spend triggers.

Both platforms expose **Reset build** in each tree. It refunds that owner's recorded purchases, removes their effects and preserves earned skills. Repeating a reset cannot duplicate AP. Reset is unavailable during a pending battle. HP is clamped when removing a Vitality perk lowers the ceiling. Saves and NG+ preserve purchases and AP; battle recovery retains existing pre-battle rollback rather than serializing a live fight.

## Launch and compare

Android: **Debug Scenarios → Crew Builds**. These run in the separate Test Session. Save useful progress in numbered test slots before launching another scenario.

Desktop uses the existing separate prototype save directory:

```powershell
.\gradlew.bat :desktopApp:run '-Pstarborn.crewPrototype=true'
```

| Scenario | Starting state | Shared AP |
|---|---|---:|
| `crew_build_prototype` | Normal opening; recruit crew and earn rewards | 0 |
| `crew_build_early` | World 4 landing, full party level 7; capstones locked | 26 |
| `crew_build_middle` | World 5 dock, full party level 9; capstones unlocked | 36 |
| `crew_build_late` | World 6 campfire, full party level 11 | 44 |

Checkpoint gear, XP, resources, currency and story state come from the seed-17 public baseline route through Worlds 1–5, actual combat and save round trips. Its documented opening fixture supplies one cryo inductor and two scrap for the mandatory recipe. Proposed AP is reconstructed from completed quests/recruitments. The two logged baseline purchases are refunded and their paid flags removed. These derived checkpoints do not establish that the prototype cleared those fights.

The generator compares packaged checkpoints with a newly executed route. Regeneration requires `STARBORN_REGENERATE_CREW_CHECKPOINTS=1`; normal tests never rewrite source assets. The report is `app/build/reports/progression-prototype/crew-checkpoints.txt`.

## Evidence and limits

Combat tests cover all new effects, thresholds, target validation, immunity, cooldowns, charges, Regen priority and separate HP/stability modifiers. Desktop integration purchases all 32 nodes through exploration, checks effective combat stats/abilities, uses the visible reset button, and round-trips saves and NG+. Isolated phone coverage exercises all four visible trees, owner refunds and service save/load. Execution totals follow below.

One prototype allocation/policy reached World 5 and lost against the fast Orbital Fighter pair. Earlier worlds passed that route. This used one build, one seed and limited reactive support: it is a pressure finding, not evidence that every build fails or that enemies should immediately be nerfed. The baseline route clears the pair with substantial consumable use. Compare focused and mixed builds from the middle checkpoint before tuning costs or enemy speed.

Next playtests should record allocation, encounter, actions, consumables, knockouts and whether each perk changed a decision. Compare protection/healing, Break/setup and precision builds from the same starting save. Human interest, story fit, resource pressure across multiple fights and final balance remain unverified. Production save compensation is separate planned work.

October 15 execution: **680 Android-hosted unit tests and 156 desktop tests pass**, with no failures, errors or skips in their XML reports. Debug APK and instrumentation APK assembly pass. Three focused emulator tests pass: all four visible trees and per-owner reset/save/load; first-AP guidance; all three checkpoint fixtures and protection of campaign saves. Desktop and phone captures were inspected. The emulator is left in the middle checkpoint's Skills menu with 36 unspent AP; the preceding owner's test session was quicksaved before installation.

The combined initial command reported an instrumentation-test compile error (suspend calls outside a coroutine); the corrected instrumentation APK subsequently built and ran successfully. A separate broad debug-catalog test failed while constructing existing `campaign_w4_crucible`. This is outside the crew checkpoint coverage and remains unresolved; no full debug-catalog acceptance is claimed. Logs are in `tmp/crew-final-check.log`, `tmp/crew-apk-check.log`, `tmp/crew-phone-final.log` and `tmp/crew-phone-check.log`.
