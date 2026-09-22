# Finale failure investigation

Resolved in follow-up on 2026-09-22: production Link targeting and healing guards now have regression coverage. Adding an authored harmless opening/recovery action to the final boss clears all 35 tested seeds without timeouts. The full unit suite passes 547/547. See the latest `FOUNDRY_PLAYTEST_REPORT.md` section; the investigation and counterfactual below are historical evidence, not current behavior.

Date: 2026-09-21. Diagnostic work only; no production or menu changes in this follow-up.

## Findings

**Link targets enemies for its immediate heal.** `CombatViewModel.determineSkillTargeting` treats positive base power as damage and resolves the `aoe` tag to `ALL_ENEMIES` before considering ally effects. Link has positive healing power and `aoe`, but no explicit `targeting`. The processor recognizes healing and honors those enemy target IDs. Regen independently reaches the party, masking the incorrect immediate heal. This is a production target-resolution defect, not confusion redirecting the spell.

The existing `authored group support reaches living party members...` test does not catch this: the enemy starts at full HP, and the Link assertion incorrectly expects allied HP to remain unchanged. Deferred regeneration does not mean Link's separately authored immediate heal should be deferred.

## Reproduced outcomes

All four runs and the longer diagnostic were repeated with identical results. Phase one clears in every case.

| Seed | Phase-two outcome | Boss HP at defeat | HP healed to boss by Link | Healing supplies remaining at defeat |
| --- | --- | --- | --- | --- |
| 10 | Defeat at 141.50s | 324/660 | 95 | 3 rations, 4 medkit_i |
| 11 | Defeat at 160.75s | 557/660 | 95 | 2 medkit_i |
| 13 | Defeat at 196.75s | 260/660 | 13 | None |
| 24 | Timeout at 240s; defeat at 256.25s in separate diagnostic | 533/660 | 332 | None |

Seed 24 is **not a deadlock**. At the four-minute limit Nova has 44 HP, the boss has 577 HP, animation pauses are zero, and combat continues to a defeat 16.25 seconds later. Its extended run deals 459 total damage to the boss, but Link returns 332 HP. Raising the acceptance limit would turn this timeout into a defeat, not solve the encounter.

The boss opens with Reality Break, then Great Silence, then Shadow Chorus in all four traces (some players act first in seed 10). Stagger removes actions, silence blocks skills for two owner turns, and confusion reduces accuracy/damage; confusion does not redirect healing. The boss's runtime speed stat is 31 versus the party's 10–21, before additional agility-based ATB contributions. Gh0st gets very few actions and often spends them healing, losing the party's substantial erosion damage. The traces support both a targeting bug and opening control pressure, not merely insufficient supplies.

Inventory evidence comes from the combat inventory, not the restored session snapshot after defeat. The latter can show pre-fight stock. Timeout result reporting was corrected to retain actual extra-supply consumption instead of returning an empty map.

## Test-only targeting counterfactual

`FINALE_LINK_ALLIES=1` explicitly targets living party members for Link in the scripted driver only. No assets or production code were changed. Seeds 6–35 with the existing candidate cooldowns yield **27 victories, 1 defeat, 2 timeouts**, compared with **26 victories, 3 defeats, 1 timeout** under production targeting. Both batches retain the four-minute limit.

- Seed 13 now wins at 93.25s.
- Seed 10 still loses, at 198.50s.
- Seed 11 times out, then loses at 263s in the separate extended diagnostic.
- Seed 24 times out, then loses at 247.75s in the separate extended diagnostic.

This establishes the targeting defect but does not establish that fixing it alone clears balance acceptance. Changed targeting also changes the subsequent action/random sequence; do not attribute every outcome difference solely to extra HP.

## Recommended next implementation

1. Give Link explicit `all_allies` targeting and correct the generic resolver to distinguish healing power from damage. Audit other positive-power support skills; avoid changing offensive AoE targeting.
2. Strengthen the regression with wounded allies **and a wounded enemy**. Require immediate allied healing, no enemy healing or enemy heal log, Regen on living allies only, and no revival. Cover no-explicit-target UI resolution and reject inappropriate cross-side healing targets defensively where appropriate.
3. Rerun original seeds and all 30 additional seeds with default production resolution. Do not ship the test-only targeting override as a fix.
4. If long losing fights remain, test a telegraphed or restricted opening Reality Break and a recovery action between major control attacks. Compare action availability, Gh0st survival and supplies before making further numeric changes. Keep the four-minute gate and report defeats honestly.

## Reproduction and evidence

Set `JAVA_HOME` to the Android Studio JBR, then in PowerShell:

```powershell
$env:FINALE_HOLDOUT_SEEDS='10,11,13,24'
$env:FINALE_TRACE='1'
./gradlew.bat :app:testDebugUnitTest --tests '*CampaignEventIntegrationTest' -q
```

Expected: the optional holdout assertion fails for seed 24. The trace flag also performs a separately labelled 16-minute diagnostic on timeouts; it never substitutes that result for the four-minute gate. For the counterfactual, set `FINALE_LINK_ALLIES='1'` and `FINALE_HOLDOUT_SEEDS=(6..35) -join ','`. Unset these variables before running the default suite.

Raw evidence:

- `test-results/finale-investigation/reproduction.xml`
- `test-results/finale-investigation/explicit-allies-counterfactual.xml`

After the diagnostic helper changes, the default suite still passes **545/545** tests. This does not clear the known targeting defect: the current support regression has the blind spot described above, and the optional holdout gate still fails. The BurgQuest Demo button remains untouched.
