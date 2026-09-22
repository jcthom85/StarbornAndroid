# Finale presentation and tactic checks

2026-09-22. Verification only; no production balance or main-menu changes in this pass.

## Tactics

All policies use the same earned checkpoint preparation, supplies, support/healing priorities, and four-minute cap. Only offensive skill priorities differ. Both finale phases must clear to count as a win. Each seed was repeated deterministically.

| Offensive policy | Seeds | Wins | Defeats | Timeouts |
| --- | ---: | ---: | ---: | ---: |
| Standard (including Venom Edge/erosion) | 35 | 35 | 0 | 0 |
| Direct damage: Arc Tether, Shatter Blow, Headshot, Prism Lance | 35 | 18 | 5 | 12 |
| Control: System Crash, Cryo Vent, Overload Fists, then damage skills | 35 | 31 | 2 | 2 |

These are sensitivity measurements, not player win-rate estimates. The optional `FINALE_TACTIC` mode records all outcomes rather than enforcing the standard tactic's victory gate; its successful Gradle exit means measurement completed, not all fights won. Original standard gates remain unchanged. The integration report echoes rows, so count unique tactic/seed rows, not raw matching lines.

Evidence: `test-results/finale-presentation/{standard,direct,control}.xml`. Reproduce a variant with `$env:FINALE_TACTIC='direct'` or `'control'` and `./gradlew.bat :app:testDebugUnitTest --rerun --tests '*CampaignEventIntegrationTest' -q`. `--rerun` is necessary because Gradle does not track these environment settings as task inputs. Clear the variable before standard acceptance. Full standard suite: **547/547 passed**, including the extra finale seeds.

**Recommendation: hold broad balance sign-off.** Direct-damage play remains much less reliable and often exceeds the cap. Next isolate the contribution of Venom Edge/erosion versus control and wasted skill cooldowns during defensive windows. Do not conclude that every strategy should win or nerf the boss solely from these scripted policies. Verify that players can discover useful status tactics and that there is a reasonable non-erosion alternative before release.

## Emulator presentation

`FinalePresentationTest` uses real CombatScreen, the production view model and the Abilities menu with disposable MQ30 persistence. It is explicitly a presentation fixture, not an earned balance fixture. After observing the opening action, it pauses background ATB and sets all combatants to one-third HP to isolate Link's interaction. Tapping Link's **Use** button through the UI heals all four living players and leaves the wounded boss unchanged. The test does not force explicit ally targets.

Gathering Silence appears in a readable central banner. However, the banner shows only its name: it does not explicitly explain that this is a safe response window or identify the next attack. This is a readability improvement opportunity, not a verified telegraph of a particular attack. The boss name is ellipsized in its compact card. Link is reachable by scrolling the Abilities list, but its row says `90 power | Focus | 4 turns after use` rather than explaining party healing; its details affordance is available. Consider a concise healing/party-target summary and a recovery-window cue in a separately scoped polish pass.

Evidence screenshots, UI semantics and before/after combat states are under `test-results/finale-presentation/ui-final/` (final pass: 1/1 in 10.296 seconds). The post-heal screenshot shows all four party members highlighted green with Regen indicators, restored HP bars and the boss still wounded. Early automation failures involved menu labels, scrolling and selector/root ambiguity; the final interaction test passed after correcting test navigation. No player save was cleared. This is targeted automated UI interaction plus screenshot review, not a manual full finale playthrough.

No release, commit, push or upload was performed. BurgQuest Demo remains untouched.
