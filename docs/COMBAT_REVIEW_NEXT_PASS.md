# Next combat review — 2026-09-22

> **Resume from [OVERALL_PLAN_CHECKPOINT.md](OVERALL_PLAN_CHECKPOINT.md).** Avatar recovery has since been implemented and verified locally. Finale pacing and no-stagger comparisons are complete; every-other Reality Break is the next proposed experiment. Combat work is paused for BurgQuest preparation. The original sequence below is historical.

Resumed after the Astra arcade navigation repair. This is a diagnostic plan, not an approved production balance change. Baseline evidence: `REMAINING_BOSS_DIAGNOSTICS.md`.

## 1. Avatar response windows

The enemy definition currently has no `opening_skill` or `recovery_after`, unlike Titan and the finale. The existing AI supports definition-scoped recovery actions. That is a bounded experiment path, but a recovery after a special attack will not necessarily solve the observed lethal basic-attack critical.

First extend the prepared Avatar fixture to seeds 1–35, recording target streaks, lethal damage/critical flags, player readiness and remaining supplies. Keep seeds 1–5 as regression cases; do not optimize only for the one loss. Then compare a test-only recovery after major specials against a separate repeated-target restriction. Keep stats, equipment, supplies and player policy identical; never combine both candidates in the initial comparison. Report deaths and fight duration as well as victory rate. Do not promote either candidate merely because it produces 100% wins.

## 2. Finale pacing

Keep the original four-minute acceptance outcome and separate longer diagnostic outcome. Existing evidence distinguishes slow wins from finite attrition losses, not deadlocks. Measure time spent waiting for player readiness versus resolving enemy actions for standard, control-first and no-Venom policies. Review response-window visibility before changing HP or damage. Retain the two-phase carryover inventory and HP; do not reset between phases.

## 3. Foundry encounter guidance and resources

Golem + Drone is highly target-order sensitive. Review whether the Drone's support role is visible before the player's first action; a readable hint is preferable to an unexplained mandatory target order. Validate any hint with both target policies rather than replacing the weaker-policy baseline.

Next route evidence must carry actual HP, inventory, earned AP, purchases and rewards across consecutive encounters and rests from arrival through Titan. Current level-9 fresh-fight batches and selected gauntlets do not prove full-route affordability. Log every rest and purchase; do not silently refill supplies. Keep the campaign-earned level-7 arrival and level-8 pre-Titan checkpoints distinct from the old level-9 fixtures.

## Acceptance boundaries

- No production numeric changes during these comparisons.
- No global erosion nerf from the current policy comparisons.
- Preserve both Debug Scenarios and BurgQuest Demo menu buttons.
- No release/version/commit/upload implied by this pass.
