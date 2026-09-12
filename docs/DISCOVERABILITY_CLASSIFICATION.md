# Static discoverability residuals

Reviewed 2026-09-12. The unchanged audit reports description/action pairs, not
confirmed bugs. After repairing three debug-manifest references, 22 campaign
pairs remain. Do not suppress them or add unavailable actions to prose to reach zero.

| Pairs | Rooms/actions | Classification and regression evidence |
| --- | --- | --- |
| 1 | `mine_shunt`: crew datapad, base | Power gates visibility and selects a description naming the datapad. `WorldOneDiscoverabilityStateTest`: `shunt datapad is named whenever power and quest gates expose it`. |
| 13 | `sector9_canopy_ridge`: Confront stalker, Face the Beast, Anchor Drill | Raw static pairs ignore room flags and variant priority. `WorldTwoDiscoverabilityStateTest` covers 108 raw combinations and 2,592 migrated flag/task/quest/milestone combinations, plus cumulative progression. Selected prose names usable objectives. |
| 2 | Gate emitters: memory/service log, completed variants | Source-only milestones can leave actions visible, but event conditions reject execution. Retirement milestones hide actions. World Two emitter compatibility tests cover all four combinations per emitter. |
| 1 | Hangar: dark console, post-launch variant | Launch can omit inspection, but completes MQ05; inspection requires active MQ05. World Two launch-without-inspection test dispatches launch and verifies inspection cannot execute afterward. |
| 5 | Archive vault, prism gallery, drone test alcove: scans | Events require `ms_w3_mq14_complete`; selected post-Lens descriptions name the scans. `WorldThreeDiscoverabilityStateTest` covers milestone combinations and dispatches blocked/unblocked events. |

Persistence evidence: AppServices passes restored/imported session state through
`migrateOpeningNarrativeState`, then `GameSessionStore.restore`. The ridge migration
reconstructs flags from tasks/completed quests; unrelated milestones remain
independent. These tests exercise production state selection and events, not disk
save/load or rendered hit targets.

Worlds 4–6 and Astra have zero static candidates. Three debug rooms had real,
low-impact missing references; their descriptions now name `debug manifest`.
The audit implementation remains unchanged.

Next: implement compatible explicit action references, preserving existing literal
matching and save data. Then verify rendered links, review route discovery and
narrative terminology, and complete device acceptance. This classification does
not sign off those later tasks.
