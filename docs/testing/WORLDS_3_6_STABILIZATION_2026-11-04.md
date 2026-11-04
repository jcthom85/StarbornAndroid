# Worlds 3 to 6 stabilization

Implements the approved progression repairs from the November 4 readiness assessment. Gameplay changes remain local and have not been installed, committed or pushed.

## Repairs

- Zeke becomes visible when the safehouse is found, before Homecoming completes. The visible conversation is tested.
- Rylan, Cameron and Charles have unconditional terminal conversation options. Cameron and Charles require the actual material quantities before exposing their favor turn-ins.
- The Crucible tracks started and cleared rounds independently. Later rounds require prior clearance; victory handlers require their own round to have started. Starts are repeatable until that round is cleared, permitting retries. Shared enemy-list matching remains unchanged.
- Selected main-quest victory handlers explicitly opt into recorded-victory reconciliation. Saved encounter-clear evidence can satisfy them once their existing quest/task prerequisites become eligible. This runs in the shared controller after state updates and after restore. It executes objective event actions, not combat XP, drops or victory rewards a second time. Recovery task credit is persisted before its event receipt.
- The opted-in coverage includes Administrator, Titan, Fighters/security/Firewall/Avatar, nightmare rescues, memory waves and the two final forms. An active quest and the handler's existing prerequisites are still required.
- Sentinel Mk. I drops the Neon Sign Core reliably. Active Neon Fix saves with recorded Sentinel clearance and a missing uninstalled core recover one replacement, guarded by a saved receipt. This also supports killing the Sentinel before accepting the quest. It is not an unlimited sale/recovery source.
- The unsupported permanent shop-discount promise is removed; the existing Neon Fix rewards remain.
- The Center's east route requires ending completion. The ending callback unlocks it; completed older saves backfill the state on load. The Source-area postgame boarding action is also guarded, without breaking the shared boarding alias in earlier worlds.

## Calibration decision

Jason chose to keep the existing records/calibration interactions. Prototype Testing now describes correcting a targeting profile against recorded drone targets. Conditioning records describe recorded Phantom failures. Overclocked describes mapping and calibrating heat cycles. Their objectives and feedback no longer claim that a tap played combat waves. Save/task IDs and rewards remain intact.

## Playtest guidance

The World 3 guide was rebuilt around the tested main route, current journal tasks and current room catalog. The previous guide is retained in `docs/_archive/world3-guide-pre-readiness-2026-11-04.md` because its numerical reward tables, permanent lockout claims and high-orbit departure description were unreliable. The new guide does not claim all optional routes or presentation have been device-certified.

## Regression scope

The production-controller regression covers safehouse visibility and conversation entry, NPC exits, early Administrator/Titan/Vale clearance and objective recovery across save/load, both early-cleared Firewall sources, insufficient Cameron payment, Neon Core recovery and persistence, and pre-ending movement. A separate arena regression checks skipped rounds, retry availability, correct round credit and completed-round refusal. The existing full campaign suite remains part of validation.

Verification completed:

- Full Android host suite: **786 tests**, zero failures/errors/skips.
- Final shared-controller suite: **13 tests**, zero failures/errors/skips, including the copied Pixel Tideglass save and the World 2 regression group.
- The earned World 1 completionist route passed again after the final wording changes; all **10 checkpoint states** match the current content.
- The fresh debug APK is built separately; no device install is performed in this pass.
- See `evidence/worlds_3_6_stabilization_2026-11-04/` for verification and fixed runtime observations.


## Remaining acceptance

- Play World 3 with visible controls and record optional-quest/reward behavior; host tests are not a replacement for this.
- Check first/repeat dialogue, old-clear recovery messages, puzzle and quest banner timing on device.
- Run real Crucible losses, retreats and re-entry, beyond the event/controller lifecycle checks.
- Existing ambiguous legacy arena completion receipts are preserved rather than deleting potentially legitimate progress or rewards. A save already affected by the old all-round credit bug may need individual review; fresh and retry routes use the corrected guards.
- World 5 repeated side-room prose, broad encounter balance and finale pacing remain human evaluation/polish items. They were not rewritten in this stabilization pass. The safe preparation route before the World 5 arrival fight is preserved.
