# Starborn Encounter Design Standard

**Status:** Canon authoring standard, derived from the World 1 vertical slice (2026-07-28)
**Applies to:** All worlds. World 1 is the reference implementation.

World 1 is the vertical slice. The rules below were established while rebuilding its combat and are the formula the remaining worlds should be built and rebuilt against. When a later world contradicts this document, the later world is wrong.

---

## 1. Why this document exists

World 1 originally shipped 81 rooms and 12 encounters, every one of them a single enemy against a solo Nova. The engine had supported multi-enemy formations the whole time — Worlds 3 through 6 already used `enemy_parties` — but World 1 never adopted it. The result was a world with no target-priority decisions, two AoE skills with nothing to hit, and one element (Shock) that answered 9 of 12 fights.

Combat was not shallow because the systems were shallow. It was shallow because the *content* never asked a question.

---

## 2. The mechanical contract you are authoring against

Before composing an encounter, know what the engine actually rewards.

**Affinity codes** (`resistances` in `enemies.json`) are exact tiers, defined in `ElementalAffinityRules.kt`:

| Code | Tier | Damage |
|---:|---|---:|
| `-100` | WEAKNESS | 2.0x |
| `-50` | VANTAGE | 1.5x |
| `0` | NEUTRAL | 1.0x |
| `50` | RESIST | 0.5x |
| `100` | IMMUNE | 0x |

**Effective affinity = tags first, then `resistances` overrides.** Tags imply affinities before you write a single number: `biological` -> burn weak, `robotic`/`electronic` -> shock weak, `industrial`/`plated` -> acid weak, `armored`/`plated` -> physical resist, and so on. **Check the tag-derived baseline before editing `resistances`, or you will author a weakness the enemy already had.**

**A skill's element comes from its `combat_tags`**, not from an `element` field. A character's basic attack element comes from the equipped weapon's `equipment.attack_element`.

**Hitting a weakness pays three ways at once:**
1. 2x damage
2. 2x stability damage, so Break arrives sooner
3. **-1 turn on every active cooldown**, plus the snack cooldown

That third one is Starborn's signature. Exploiting a weakness does not just deal more damage — it accelerates your entire rotation. Every encounter should be composed so the player has a reason to find that acceleration.

**Break creates a direct-damage window.** A target that was already Broken when an Attack or damaging skill connects takes **25% more direct damage**. The hit that empties Stability creates the window but does not receive the bonus. Damage-over-time, environmental damage, and self-damage do not receive it. This makes Break a setup-and-payoff choice without causing status ticks to snowball.

---

## 3. Composition rules

### 3.1 Formations, not duels

Author encounters with `enemy_parties` (`List<List<String>>`) in the room. Each inner list is one fight.

**A bare `enemies` list is not a group — it spawns one solo fight per entry.** This is the single most common authoring mistake and it is invisible in the data. When `enemy_parties` is present the runtime overwrites `enemies` with its flattened contents, so **author both and keep them consistent**.

```json
"enemies": ["acoustic_bulwark", "resonance_buoy"],
"enemy_parties": [["acoustic_bulwark", "resonance_buoy"]]
```

Solo encounters are legitimate, but only when the fight is a **lesson with one variable**: a tutorial (Faulted Loader), a mandatory mechanic drill (the SQ03 Guard Break Bulwark), or a boss.

### 3.2 Hard caps (enforced by `scripts/validate_world1_balance.ps1`)

- Party size **<= 3** enemies
- Non-boss party total **HP <= 300**
- Non-boss party total **XP <= 250**

### 3.3 Every party should pose one question

A formation is only worth authoring if it creates a decision. Use one of these shapes:

- **Support + threat** — a Resonance Buoy that calls backup beside something that hurts. Question: *do I spend a turn on the support first?*
- **Shield + damage** — an Acoustic Bulwark that must be Guard Broken while a Dampener suppresses. Question: *do I break the wall or silence the gun?*
- **Pair of the same unit** — two Skimmers, two Borers. Question: *is AoE worth the cooldown here?*
- **Split weakness** — two units with different exploitable axes. Question: *which element do I commit to?*

If a party asks none of these, it is padding. Cut it or merge it.

---

## 4. The element spread rule

**No single element may be the optimal answer to more than ~40% of a world's encounters.**

Measure this against **what the player can actually deal in that world**, not against the affinity table in the abstract. World 1 has burn-weak enemies and Nova has no burn — those weaknesses are correctly authored as unreachable foreshadowing, not as the intended answer.

Every element the party *can* deal needs somewhere to be the best line. World 1's shipped distribution:

| Answer | Encounters |
|---|---:|
| Shock | 5 |
| Freeze | 4 |
| Either (split party) | 2 |

Three deliberate exceptions are worth copying:

1. **The tutorial has exactly one answer.** The Faulted Loader is Shock-weak and nothing else. First contact with a mechanic should not present a choice.
2. **The mechanic-teacher has no elemental answer.** The Acoustic Bulwark is deliberately weak to nothing. Its lesson is Guard Break, and an elemental shortcut would let the player skip it. Freeze gets a `-50` vantage only because Brittle -> physical is the *intended* combo, not an alternative to breaking guard.
3. **A side quest's reward should have a home.** SQ02 grants Corrosive Rounds; Dominion Dampeners are acid-weak. Optional content should visibly pay off on the critical path.

---

## 5. Telegraph the fight in the room prose

The fun thesis is **notice -> investigate -> prepare -> exploit**. The player cannot prepare for what the room does not show them.

**The room description must name what is in the room before the fight starts**, and should hint at the exploitable axis through physical detail rather than stating the mechanic:

> Two skimmers hang in the warm column above it, throat sacs working, waiting for something to walk under them.

That tells the player there are two of them, that they are airborne, and — with the enemy's own flavor text, which explains the sac only works hot — that cold will drop them. It never says "weak to Freeze."

Constraints:
- Room descriptions are capped at **45 words** (`scripts/validate_narrative_prose.ps1`)
- Every room *action* name must appear verbatim in the description (`validate_world1_content.ps1 -StrictInlineActions`), so preserve existing keywords when rewriting

---

## 6. XP budgeting when you add enemies

Adding enemies adds XP, and XP drift silently rebalances every downstream world.

**Procedure:**
1. Sum the critical-path enemy XP *and* quest XP for the world.
2. Check the result against `leveling_data.json`. The target is that the player reaches the world's boss at the **same level as before your change**.
3. Prefer trimming per-enemy `xp_reward` on units **exclusive to that world**. Check reuse first — in World 1, `echo_borer` appears 4x in World 2 and `siren_skimmer` once, so both were left alone and the trim was concentrated on the World-1-exclusive `resonance_buoy` (50 -> 30).

World 1 reference: 1260 enemy XP + 1175 quest XP = 2435 total, landing the player at level 8 for the Warden — unchanged from before the pass, against a level 9 threshold of 2500.

**Known accepted drift:** the optional Pressure Hauler patrol (250 XP) can push a completionist to level 9. Optional elite content rewarding a level is acceptable; critical-path drift is not.

---

## 7. A quest's first objective must point forward

Do not open a quest with a task the player has already satisfied.

Both World 1 side quests originally started like this:

```
start_quest:w1_sq02, track_quest:w1_sq02,
set_quest_task_done:w1_sq02:talk_to_doc, set_milestone:ms_w1_sq02_started
```

The quest began *and* its first task (`talk_to_doc`) was marked complete in the same interaction, because talking to Doc is what started the quest. The task was never visible as a pending objective — it existed only to be auto-completed, and the UI dutifully announced "you completed: Talk to Doc" one beat after the player talked to Doc.

Removed from `w1_sq01` and `w1_sq02` on 2026-07-29. Both now open on their first real objective ("Find the hidden stash", "Enter the ventilation hub").

**Rule:** the quest-start card already names the giver. If the first task is "Talk to \<giver\>", delete it. When removing one, update in lockstep: the task entry in `quests.json`, the `set_quest_task_done` in the dialogue trigger, any `Hub*CriticalFlowTest` assertion, and any Maestro flow asserting the task text.

This is the same instinct as the fun-factor onboarding pass: the first thing the game says after the player commits to something should tell them where to go next.

## 8. Dark rooms

Darkness is a complete, data-driven mechanic. **No engine work is needed to add a dark room** — `darkCapableRoomIds` is derived from the data (`ExplorationViewModel.kt`), so authoring `"dark": true` (or `state.dark: true`) is sufficient.

While a room is dark: NPCs are hidden, ground items are hidden, quick actions are suppressed, the environment theme is suppressed, and **exits are restricted to perceivable directions**.

Darkness resolves in this order (`ExplorationScreen.kt`): room state `dark` → room state `light_on` → the authored `dark` flag. Then two overrides: a room that is not dark-capable is never dark, and an `env: "mine"` room is lit whenever the Stellarium generator (`mine_junction`, state `power_on`) is on. Setting `power_on` writes `dark`/`light_on` across **every** dark-capable `env: "mine"` room at once, which is why environment matters when choosing a light source.

Three usable light sources:

| Source | Scope | Use for |
| --- | --- | --- |
| `toggle` action bound to `light_on` | one room | self-contained rooms; see `pit_nova_bunk` |
| Generator `power_on` on `mine_junction` | every dark-capable `env: "mine"` room | payoff for MQ03's "Restore power at the Cavern Junction" beat |
| `set_room_state` from any event | whichever rooms you name | story-driven lighting, e.g. the Fork sync lighting the Architect rooms |

**Reference implementation: `pit_nova_bunk`.** It shows the whole toolkit — `dark`, `reveal_title_when_dark`, `description_dark`, `state.light_on`, a `toggle` action, `blocked_directions` gated on room state, and `description_variants` keyed on the lit state.

### The rule that matters

**Every action the player must use while a room is dark has to be named in `description_dark`.** Only `description_dark` renders when unlit, and inline actions are matched against the *currently rendered* description. `validate_world1_content.ps1 -StrictInlineActions` will **not** catch a violation, because it checks whether the name appears in *any* description — so a keyword present only in the lit copy passes validation while being unreachable in play. This is the same trap as `description_variants` (see `World1_Vertical_Slice_Status.md`).

Omit a keyword deliberately only when the action *should* be undiscoverable until there is light — `pit_nova_bunk` does this with `scorched conduit`, which is milestone-gated behind turning the light on.

Also: a dark room with no `description_dark` falls back to the bare string `"It's too dark to make out the room."` and exposes no interactions at all. `mine_gas` shipped in that state — flagged dark, never finished.

Other constraints: the 45-word cap applies to `description_dark`; and because exits are restricted while dark, every dark room needs a way back out, or a deliberately gated exit with a clear `message_locked`.

## 9. Validation gate

Run before committing any encounter change:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:runAssetIntegrity :app:testDebugUnitTest
```

`runAssetIntegrity` runs every validator, including `validateMaestroSelectors`, which catches flow assertions orphaned by a prose or quest edit.

Device runs need a large APK installed (~1.15 GB debug build). Two gotchas:

- `scripts/adb.ps1` times out at 45 s by default, which is shorter than the install takes. Set `STARBORN_ADB_TIMEOUT_SECONDS=900`.
- `INSTALL_FAILED_INSUFFICIENT_STORAGE` on an emulator with a couple of GB free usually means the old copy is still resident — `adb uninstall` first, then install.

`encounter_victory` event triggers match with `any{}`, not an exact set, so **adding** enemies to an existing party will not break a quest trigger. Removing the specific enemy a trigger names will.
