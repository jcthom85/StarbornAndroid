# Starborn Fun-Factor Audit

## Fun Thesis

Starborn's core loop is **notice -> investigate -> prepare -> exploit -> earn -> witness change**. Discovery should give the player an actionable advantage, and combat mastery should turn that advantage into stolen tempo, a setup, and a payoff. Major arcs should deliver one meaningful discovery, decision, mastery moment, or payoff every 10-20 minutes.

## Implemented Opening Slice

- Nova is identified immediately instead of presented as `???`.
- The bunk light reveals an impossible resonance before movement instruction appears.
- Investigating the anomaly teaches a three-beat clue that returns at the workshop loader.
- Jed's starter gear arrives as one kit instead of five item interruptions plus an inventory tutorial.
- The Faulted Loader is a short authored Shock-weak encounter that demonstrates weakness-driven cooldown acceleration.
- Tempo feedback names the cooldown that moved and shows its previous and new values.
- Crafting the Cryo-Inductor unlocks Cryo Vent, a new brittle setup route, and permanently changes the workshop description.
- Local JSONL playtest telemetry records room actions, dialogue choices, combat actions, retries, quest duration, equipment, arc scores, and session exits without player-entered text or network transport.

## Campaign Audit

Run `powershell -ExecutionPolicy Bypass -File scripts/audit_fun_cadence.ps1` to regenerate the measurements.

The July 2026 baseline found:

| World | Side-quest average tasks | One-task side quests | Reactive rooms |
|---|---:|---:|---:|
| 1 | 4.6 | 0/5 | 7/88 |
| 2 | 4.2 | 0/5 | 13/91 |
| 3 | 2.8 | 0/5 | 18/65 |
| 4 | 3.0 | 0/5 | 17/71 |
| 5 | 2.8 | 0/5 | 17/80 |
| 6 | 3.0 | 0/5 | 11/65 |

Generic actions remain dominant in every world (804 of 827 actions overall). Task count is not a proxy for fun, but all side quests now have multiple consequential beats instead of collapsing into one-tap completion after World 2.

## Remediation Order

1. **World 6:** implemented. Each side quest now crosses three connected-room beats (discovery, manipulation or choice, payoff), and five additional rooms retain the consequence in their descriptions.
2. **Worlds 3 and 5:** implemented for side quests. Surveillance and access discoveries now gate evidence leaks, telemetry scrubs, public access changes, and independent system restoration.
3. **World 4:** implemented for side quests. Industrial observations now feed worker escape routes, cooling bypasses, freed units, and stolen overclock profiles.
4. **Worlds 1 and 2:** preserve their stronger quest depth while increasing persistent visual state changes along the critical path.

For each revised arc, remove travel-only tasks, retain at most one pure flavor interaction between consequential actions, and record a six-axis score for anticipation, agency, mastery, variety, payoff, and momentum.

## July 2026 Representative Quest Playtest

Run `scripts/run_fun_audit.ps1 -Device <device-id>` to regenerate the deterministic combat/skill reports, execute all four Maestro arcs, and pull their local telemetry.

All four representative quests pass from clean, campaign-appropriate checkpoints on the connected emulator:

| Quest | Automated quest time | Rooms | Actions | A | Ag | M | V | P | Mo |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Corporate Espionage | 25.8s | 2 | 3 | 3 | 2 | 2 | 2 | 3 | 4 |
| Quality Control | 39.7s | 2 | 3 | 4 | 3 | 2 | 3 | 4 | 4 |
| Ghost in the Shell | 47.3s | 2 | 3 | 3 | 2 | 2 | 2 | 4 | 3 |
| The HR Record | 37.6s | 3 | 3 | 4 | 3 | 2 | 3 | 4 | 4 |

`A`, `Ag`, `M`, `V`, `P`, and `Mo` are anticipation, agency, mastery, variety, payoff, and momentum on a 1-5 visual-review scale. Automated times compare arc structure; they are not estimates of blind-player completion time.

Findings:

- The arcs are concise, readable, and have clear discovery, complication, and completion beats.
- Quality Control and The HR Record provide the strongest character payoff.
- Ghost in the Shell loses momentum by returning through an unchanged route.
- None of the four asks for tactical mastery or a consequential choice. Their interaction remains three highlighted actions plus authored text.
- Device testing found nine generic actions hidden because their names were absent from the active room description, including a World 6 post-main-quest description variant. The descriptions now expose every required action.
- Quest update cards, narrative overlays, and quest-start cards can stack visually. They remain readable, but the layered notifications add friction and should be consolidated in a later UI pass.

## Combat and Skill Decision Baseline

The deterministic harness resolves 25 main-quest victory gates and runs 20 fixed seeds under basic, greedy, and weakness/setup-aware policies. It uses normalized world snapshots and the production combat engine; its flags identify fights for device validation, not final balance decisions.

- Nine encounters exceed six median rounds without an authored phase change in the normalized tactical baseline.
- Four encounter gates produce baseline defeat risk, concentrated in the repeated Acoustic Bulwark route and Iron Warden.
- Six encounters show a dominant player action in the tactical policy.
- Several weakness encounters show no speed advantage over greedy damage, indicating that weakness reward value needs device verification.
- The catalog audit found one exact player-skill duplicate (`corporate_insight` / `nova_legacy`), two potentially dominated player choices (`nova_pulse_grenade` / `nova_rend_round`, `zeke_quake_slam` / `zeke_shatter_blow`), and seven high-power zero/one-turn cooldown rotation risks.
- No skill condition consumes a status produced by another skill. Setup/payoff currently comes from systemic weakness and status behavior rather than explicit party ability chains.

Generated evidence lives in ignored `reports/fun-audit/` and `playtests/artifacts/fun-audit/`.
