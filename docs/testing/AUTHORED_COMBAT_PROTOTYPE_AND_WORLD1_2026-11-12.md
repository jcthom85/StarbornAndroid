# Authored combat prototype and World 1 — November 12, 2026

Implemented the first bounded stage of the six-world encounter plan: shared mechanics with temporary art, plus the existing World 1 Warden. Worlds 2–6 have not been converted to the new plan.

## Result

The Warden remains a solo encounter with its existing stats, resistances, 350 XP, 225 credits and guaranteed keycard. Its new sequence alternates protection, clearly prepared burst/gas attacks and recovery. Below 40% HP it drops Guard/Shield, overclocks once and switches to an offensive cycle. Players can respond with the new basic Guard command, interrupts or healing. Existing skills were not nerfed to force a particular solution. World 1 enemy placement, room gates, story IDs and resource sources are unchanged.

An intermittent startup catalog race was also corrected: catalog rebuilds and reads now share a lock, preventing new sessions from observing a partially rebuilt item collection.

New patterns are opt-in; ordinary enemies retain their existing AI. Android and desktop show persistent attack warnings with the actual source and original targets. Preparation consumes a real enemy ATB opportunity rather than a short animation delay. Breaking, disabling or destroying a weapon cancels its queued shot; losing every original target cancels rather than silently retargeting. Linked weapons use the body's clock, and their status durations still advance.

The Titan prototype has a body and four targetable weapons. The reserve prototype deploys real actors from a finite pool of three, with at most two live escorts. Starting escorts count toward that cap. Destroying the core shuts the encounter down; parts and reinforcements do not multiply rewards. Prototype fixtures deliberately carry tempting child rewards to verify that exclusion.

The Android Test Session browser has separate Titan and finite-reserve laboratory scenarios. They rebuild an isolated session and provide simulation controls. They do not insert these encounters into campaign rooms or move the player's campaign. Temporary existing sprites demonstrate targeting and assembly only; no final artwork or new audio was generated.

## Verification

- Runtime suite: 38 tests, zero failures/errors, including six new authored-rule tests. All 24 weapon destruction orders were checked at the rule level, along with Break/Jam/Stun interruption, original-target loss, one-time phase transitions, reserve caps and state serialization.
- Focused Android unit checks: 86 tests, zero failures/errors across authored runtime, opening combat, enemy support, combat view model, data integrity and the earned launch route. The runtime tests use real player commands for source destruction, reinforcement deployment, Guard, core victory and exact reward exclusion.
- Warden: six actual fights from the recorded earned World 1 launch checkpoint, three combat seeds with direct and reactive policies, Nova alone. Each won with one overclock phase and the original rewards. This proves these routes are possible; it does not establish ideal blind difficulty. Arc Tether remains a strong existing control option.
- Public progression: the checkpoint's remaining security fight, Warden, postfight story interactions, pod launch and World 2 arrival completed without synthetic victories or assistance. World 1/World 2 save slots reloaded with the expected progression and inventory. This starts at an earned late-world checkpoint, not a fresh game.
- Desktop: six focused tests passed, covering portrait/wide multipart presentation, normal combat flow, World 1 gate behavior and formation layout. Screenshots show five readable target controls independent of sprite scaling.
- Android device presentation: isolated emulator checks exercise normal height and a short screen with 130% font scale, persistent warnings and five target controls. The short multipart layout scrolls rather than dropping target rows; crew remains reachable below. A separate target-strip test checks that each of the five controls dispatches its own target ID. Both instrumentation tests passed.

Evidence is in [the evidence folder](evidence/authored_combat_world1_2026-11-12/). The complete Android unit suite was not declared green: 23 previously confirmed baseline failures remain outside this focused pass. A fresh beginning-to-end campaign, blind discovery and physical Pixel input/performance still need playtesting.

## Persistence and deployment boundaries

Warnings, phase flags and reserve counters live in combat state and survive UI recomposition/background pause. Their data round-trip is tested. Reopening an interrupted battle still uses the game's existing battle-entry restart checkpoint; exact midfight part/charge/reserve restoration is not implemented here.

A separate headless `Starborn_Encounter_Lab` emulator (port 5556) was created because the existing emulator could not accommodate the additional test package. Only that laboratory received the `.playtest` package. The original emulator and its saves were not replaced; no Pixel was connected. No version increment, regular install, commit, push or walkthrough publication was performed. The local World 1 walkthrough now describes the Warden warnings and Guard response without changing step numbering.

## Next acceptance

Play the Warden for pacing and the laboratory encounters for target clarity and tactical choices. The current work establishes mechanics and progression, not the subjective fun of finished encounters or final visual quality. Follow with the bounded World 2 pass, then each remaining world with its own earned-resource and story checks. Final art comes after the relevant behavior and assembled silhouette are accepted.
