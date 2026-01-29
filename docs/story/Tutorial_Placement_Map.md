# Starborn - Tutorial Placement Map (Implicit + Explicit)

This file is the single source of truth for *where* we teach every mechanic, UI convention, and "player literacy" expectation in Starborn - including:
- Full, gated tutorials
- Micro-tutorials (one prompt or one line of dialogue)
- Implicit teaching via intuitive UI (affordances, highlights, disabled states)

If it is something the player must understand to play well, it goes here.

---

## How To Read This Doc

- "Where" uses the story hierarchy: **World -> Hub -> Node -> Quest/Event -> Beat**.
- "Delivery" describes *how* the player learns it:
  - `Implicit UI`: taught by affordance (highlight, pulse, locked state label).
  - `Dialogue`: a single line (or short exchange) that teaches/frames the mechanic.
  - `Prompt`: a one-off tutorial popup (TutorialRuntimeManager).
  - `Script`: a multi-step tutorial script (TutorialScriptRepository / `tutorial_scripts.json`).
  - `Gated Step`: progression is blocked until the player performs the taught action.
  - `Practice + Feedback`: a safe/controlled scenario with success/failure tips.
- "Mandatory" means the main path guarantees the teach. "Optional" means it is taught in a side quest or optional node.
- Every Optional teach must have a Mandatory fallback if it is required for main progression.

---

## Tutorial Tone / Framing Rules (Global)

- Tutorials should feel like **in-universe compliance UI** when possible (Dominion voice, sterile phrasing, dark humor).
- Prefer showing over telling:
  - 1 interaction to demonstrate
  - 1 prompt to name/confirm
  - 1 immediate use case to reinforce
- Avoid stacking prompts. If multiple concepts are introduced in one moment, teach the *minimum* needed to proceed, then drip the rest on the next beat.

---

## Meta - Main Menu / Settings (Out Of Story)

| Teaches | Delivery | Where (exact) | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| New Game vs Load Game | Implicit UI | Main Menu -> first launch | Yes | Keep labels plain. "Debug" options (if present) should be visually separated from normal play. |
| Save slots (what a slot is) | Prompt | First time opening Load/Save dialog | Optional | If slots are the only save UX, teach it here, not mid-story. |
| Autosave expectations | Prompt + Implicit UI | First autosave trigger in World 1 (after the first tracked quest starts) | Yes | Use a small "Autosaved" toast the first time, then silent thereafter. |
| Accessibility settings (text speed, reduce motion/flicker) | Prompt | First time opening Settings | Optional | If Settings are not available on main menu, add a single in-game Settings entry before launch. |
| How to dismiss tutorial prompts | Implicit UI | First tutorial prompt shown | Yes | Make the dismiss affordance obvious and consistent; do not tutorial the tutorial more than once. |

---

## World 0 - Prologue: "The Signal" (Cold Open)

| Teaches | Delivery | Where (exact) | Mandatory | Notes / Fallback |
| --- | --- | --- | --- | --- |
| Tap to advance text/cinematics | Implicit UI | PROLOGUE cinematic: Lab PA / containment breach montage | Yes | First interaction on first launch. Keep it silent and obvious (single "Continue" affordance). |

---

## World 1 - The Mines

### Hub 1.1 - Homestead Quarter (Onboarding Hub)

| Teaches | Delivery | Where (exact) | Mandatory | Notes / Implementation hook |
| --- | --- | --- | --- | --- |
| Swipe movement between rooms (N/S/E/W) | Script + Gated Step | W1_MQ01 "Wake Up Call" -> Node 1: The Pit -> Nova's Bunk: leave the room | Yes | Script: `movement` or `scene_swipe_movement`. Gate the first exit until the player swipes once. |
| Exits/minimap meaning (what is currently reachable) | Prompt | W1_MQ01 -> first successful swipe to a new room | Yes | Keep text short. Reinforce by pulsing the open exit indicators for ~2s. |
| Highlighted words/hotspots are tappable (no typing) | Implicit UI + Prompt | W1_MQ01 -> Nova's Bunk: first interactive noun in the room description | Yes | Pulse the first highlighted word once; prompt only if player pauses. |
| Action menu (Examine/Use/Talk/Take) from a hotspot | Gated Step | W1_MQ01 -> Nova's Bunk: interact with 1 object that has multiple actions | Yes | Make the first hotspot have exactly 2 actions so the menu concept is clear. |
| NPC interaction (tap name -> choose Talk) | Script + Gated Step | W1_MQ01 -> first mandatory NPC conversation (Jed) | Yes | Script: `npc_talk`. Gate only the first conversation until the player selects Talk once. |
| "Room state" changes (the text updates after actions) | Implicit UI | W1_MQ01 -> Nova's Bunk: after examining/taking an item | Yes | Teach by doing: change the room description line immediately. |
| Save/Rest at bed (safe point) | Dialogue + Prompt | W1_MQ01 -> Nova's Bunk: interact with bed | Yes | One-line: "If you need to reset, use the bunk." Show that HP/snack charges restore (or explicitly say what restores). |
| Quest start + objective is pinned/tracked | Prompt | W1_MQ01 -> after talking to Jed in Node 2: Jed's Workshop | Yes | Immediately show the tracked objective UI and the "pinned" marker. |
| Objective navigation (locator highlight / "go here next") | Script | W1_MQ01 -> first time leaving Jed's Workshop with an active objective | Yes | Script: `scene_market_locator` (or equivalent). Teach that the minimap/locator is a hint, not a GPS. |
| Journal/Quest Log UI | Prompt + Practice | W1_MQ01 -> Jed: "Check your journal if you forget." | Yes | Script: `scene_market_journal` (if still used). Gate nothing; just prompt + ask player to open/close journal once. |
| Bag/Inventory UI (open/close) | Script + Practice | W1_MQ01 -> first time player receives an item reward | Yes | Script: `bag_basics`. Consider auto-opening the bag once with a "Dismiss" to reduce hunting. |
| Auto-loot behavior (picked items go to bag) | Dialogue | W1_MQ01 -> first pickup | Yes | One-line reinforcement so player trusts they did not "lose" the item. |
| Equip a weapon/armor (difference between loot and equipped) | Gated Step | W1_MQ01 -> Jed's Workshop: "Equip your cutter/pistol before you leave." | Yes | Gate the exit from Workshop until weapon is equipped (single step). |
| Tinkering (crafting/modding) basics | Script + Gated Step | W1_MQ01 -> Jed's Workshop -> Tinkering Table | Yes | Script: `scene_tinkering_tutorial`. Reward the crafted item immediately so the loop closes. |
| "Restore power" style interaction (flip switch / install fuse) | Script + Gated Step | Optional early onboarding quest in Node 1: The Pit (e.g., blackout) | Optional | Scripts: `scene_light_switch_hint`, `scene_lights_out_intro`, `scene_lights_out_breaker`, `scene_lights_out_trunk`, `scene_lights_out_install`. If present, keep it before combat. |
| Shop: buy/sell + currency framing | Prompt + Practice | Node 3B: Trade Row -> first time entering a shop stall | Optional | Must have fallback prompt later on a mandatory shop interaction (World 1 Hub 1.2 or World 2). |
| Healing outside combat (med items / first aid station) | Practice + Feedback | Node 3A: Med-Bay -> treat a minor injury | Optional | If First Aid minigame exists, teach it here as a low-stakes version. Fallback is World 2 crash. |
| Mods exist (and may be locked by milestones) | Implicit UI + Prompt | When player receives first weapon mod reward (W1_SQ02 Corrosive Rounds) | Optional | Show mod sockets UI in equipment detail. If sockets locked, label: "Unlocks after Main Story milestone." |
| Guard Break / Stagger as the primary "puzzle key" | Practice + Feedback | W1_SQ03 "Heavy Lifting" -> reward beat: acquire Hydraulic Kick, then use it once on a shielded target (training dummy or scripted spar) | Yes (soft gate) | Strongly signpost as required training before Logistics. Teach: some enemies take **0 damage** until Guard Broken/Staggered. |
| Point-of-no-return warning language | Prompt | Node 4: Admin Gate -> first time approaching the Gate | Yes | "Crossing the checkpoint may lock out unfinished errands." Keep it honest and specific. |

### Hub 1.2 - Logistics Sector (First Dungeon + Escape)

| Teaches | Delivery | Where (exact) | Mandatory | Notes / Implementation hook |
| --- | --- | --- | --- | --- |
| Hacking interaction (no typing, puzzle UI) | Script + Gated Step | W1_MQ02 "Paperwork" -> Admin Gate/Window: clearance denial -> hack | Yes | This is the first "minigame" style tutorial. Keep failure cheap, give clear feedback. |
| Combat entry (encounter start/readiness) | Prompt | W1_MQ03 "The Echo" -> Node 2B: Deep Mine -> first fight trigger | Yes | One prompt only: how to pick an action + end turn (or equivalent). |
| Targeting (single target vs multi target) | Practice + Feedback | W1_MQ03 -> first fight: 2 enemies | Yes | Teach switching targets and reading target info. |
| Shielded enemies (damage isn't everything) | Practice + Feedback | W1_MQ03 -> first fight includes a Riot Shield enemy that is immune until Staggered/Guard Broken | Yes | Tooltip-on-first-0-damage: "Guard Up. Break it with Slide Kick / Hydraulic Kick / Blast Wave." This is the pre-boss teach for The Warden's shield. |
| Cooldowns as primary resource (no MP) | Prompt + Practice | W1_MQ03 -> first time player taps a skill on cooldown | Yes | Make the UI do the teaching (cooldown badge), prompt only on first confusion. |
| Elemental weakness (+ tempo/stagger payoff) | Practice + Feedback | W1_MQ03 -> first enemy with clear weakness (e.g., Drone weak to Shock) | Yes | First weakness hit triggers a small "Weakness! Cooldowns -1" style toast. |
| Status effects (DoT, stun, blind) basics | Practice + Feedback | W1_SQ02 (if completed) OR W1_MQ03 second combat | Yes (fallback) | Keep World 1 reactive/brute-force only. Do not tutorial **Jammed** or **Marked** until World 2 (Zeke joins too late). |
| Source Art acquisition (Relic sync unlocks a new ability) | Cinematic + Prompt | W1_MQ03 -> Node 3: Echo Chamber -> Ancient Chamber -> sync with Tuning Fork | Yes | Immediately show the new skill in combat UI with a "Source Art" label. |
| Chase/gauntlet pacing (fast movement, limited detours) | Dialogue + Prompt | W1_MQ04 "Red Alert" -> Maintenance Tunnels | Yes | Use Zeke comms as the "tutorial voice" here: minimal UI prompts. |
| Boss telegraphs (read intent, don't auto-attack) | Practice + Feedback | W1_MQ05 "The Launch" -> Boss: The Warden | Yes | The first boss should explicitly telegraph at least one move; tooltip-on-first-telegraph. |
| Party member joins (Zeke) + party UI basics | Script + Practice | W1_MQ05 end: Zeke joins party (post-boss / escape) | Yes | Script: (new) `party_basics` - portraits, swapping, role hint. Avoid teaching Zeke's advanced status layer (Jam/Mark) here; first teach in World 2. |
| Synergy skills / combo conditions | Practice + Feedback | First post-recruit fight with Nova+Zeke | Yes | Teach one "obvious" synergy with a highlighted button when conditions are met. |
| Snack slot in combat (reusable cooldown tool, not item spam) | Prompt + Practice | First combat after player equips a snack OR first time snack button is available | Yes | Use a single line: "Snacks recharge; cooldown limits use." Reinforce by showing cooldown after use. |

---

## Interlude - The Crash

| Teaches | Delivery | Where (exact) | Mandatory | Notes / Fallback |
| --- | --- | --- | --- | --- |
| Party injury pressure (need to heal before pushing on) | Dialogue | Crash cinematic -> wake in wreckage | Yes | This primes World 2's survival onboarding without pausing to explain systems. |

---

## World 2 - The Wilds

### Hub 2.1 - Crash Site (Survival Onboarding)

| Teaches | Delivery | Where (exact) | Mandatory | Notes / Implementation hook |
| --- | --- | --- | --- | --- |
| First Aid / healing minigame (if used) | Practice + Feedback | MQ_06 "Impact" -> Node 1: Impact Crater -> treat Zeke | Yes | Scripts: `first_aid_failure` on failure. Keep this extremely forgiving. |
| Food/cooking as prep (if used) | Practice + Feedback | MQ_06 or first campfire in Node 1 | Optional | Scripts: `cooking_failure` on failure. If cooking is optional, teach it on The Astra instead. |
| Quest gating via key items (Chime as a physical key) | Dialogue + Prompt | MQ_07 "The Signal" -> Temple Gate "Source Lock" | Yes | Teach: key items are used via hotspot action; they are not "consumed." |
| Field breather spaces (safe nodes) | Implicit UI | Node 2B: Tideglass Beach | Optional | Teach by contrast: different music + "no encounters" indicator. |
| Fishing (if present) | Practice + Feedback | Tideglass Beach -> fishing spot hotspot | Optional | Scripts: `fishing_basics`, `fishing_failure`, `fishing_success`. Must not be required for main progression. |

### Hub 2.2 - Sector 9 Ruins (Puzzle + Party Growth)

| Teaches | Delivery | Where (exact) | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Puzzle interaction language (alignment, rotate, tune) | Practice + Feedback | MQ_08 "Sleeping Giant" -> Stasis Chamber alignment puzzle | Yes | Keep vocabulary consistent with later relic puzzles. |
| New party member role (Orion = healer/utility) | Dialogue + Practice | MQ_08 end: Orion joins -> next combat | Yes | First combat after recruit should *require* at least one heal/buff (soft gate via enemy damage). |
| Tank/protector framing (Gh0st = protection) | Dialogue + Practice | MQ_09 "The Hunter" -> Gh0st joins during boss | Yes | Teach "Protect/Intercept" by showing damage redirection once, with one tooltip. |
| Jammed (Silence) basics (deny enemy skills) | Practice + Feedback | Hub 2.2 -> first enemy that charges an obvious special attack | Yes | First teach for Zeke's Signal Jammer. Teach: Jam stops Skills/Arts, not basic attacks. |
| Marked / Target Lock (setup → payoff) | Practice + Feedback | Hub 2.2 -> first fight designed around "Target Lock" then a payoff skill | Yes | Teach the tactical layer here (not World 1). Keep it to one simple combo highlight; no multi-step puzzle fights yet. |
| The Astra unlock (mobile hub access) | Prompt | MQ_10 "Liftoff" -> Hangar Bay -> interact with The Astra | Yes | Teach: "Tap the ship to enter your hub." Reinforce by making ship image available from future hub screens. |

---

## The Astra - Mobile Hub (World 2+)

| Teaches | Delivery | Where (exact) | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Entering the Astra from any hub | Implicit UI + Prompt | First hub screen after MQ_10 (ship available) | Yes | The ship image is an affordance; add a one-time prompt the first time it becomes available. |
| Ship nodes (Bridge/Common Room/Quarters/etc.) | Implicit UI | First time in Astra hub | Yes | Teach through layout and labels; avoid a tour popup. |
| Rest restores party + resets daily resources | Prompt + Practice | Astra -> Quarters -> first bed interaction | Yes | Show before/after HP; mention snack recharge explicitly if applicable. |
| World travel from Bridge | Prompt + Practice | Astra -> Bridge -> first "Depart" interaction | Yes | Include a clear confirmation + destination preview. |
| Relic Array catch-up system | Prompt + Practice | Astra -> Common Room -> Relic Array interact | Yes | Teach when first character joins *after* first relic (World 2 recruits). |
| Storage/Bank (cargo stash) | Prompt | Astra -> Cargo Bay -> first stash interaction | Optional | If implemented, teach here. Otherwise remove from UX until ready. |
| Simulation Deck (practice/replay) | Prompt | Astra -> Simulation Deck -> first entry (World 3+ unlock) | Optional | This is a perfect place for "advanced combat tips" tutorials that are truly optional. |
| Phantom Logs (automatic banter + plot drip) | Dialogue + Implicit UI | Space travel interludes | Yes | No tutorial popup; use a line like "The Chime is acting up again..." and a consistent UI panel. |

---

## World 3 - The Spire

### Hub 3.1 - Lower City (Stealth + Social Systems)

| Teaches | Delivery | Where (exact) | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Stealth/disguise basics (avoid patrols, blend, line-of-sight) | Practice + Feedback | Hub 3.1 -> Node 3A: Night Market -> first Dominion patrol sweep | Yes | Keep it simple: one meter, one rule, one escape option. The first sweep should be survivable even if the player bumbles. |
| High-volume shop comparison (buy/sell across vendors) | Implicit UI | Night Market | Optional | Already taught in World 1; here is reinforcement + variety. |

### Hub 3.2 - Upper City (Heist + Scan)

| Teaches | Delivery | Where (exact) | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Scan (Lens Relic) as exploration tool | Practice + Feedback | Hub 3.2 -> Node 3: The Archive -> immediately after obtaining the Lens -> Scan a highlighted "data seam" | Yes | Teach by necessity: require 1 Scan use to reveal the safest route (or a critical code) before Stage 4. |
| "Heist staging" (multi-step objectives, switching contexts) | Implicit UI | MQ_12 "The Plan" -> assemble at table | Yes | No tutorial popup; teach by clear quest stage UI. |

---

## World 4 - The Foundry

### Hub 4.1 - Slag Pits (Environmental Hazards)

| Teaches | Delivery | Where (exact) | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Environmental hazard zones (heat/burn) | Practice + Feedback | MQ_16 "Into the Fire" -> Slag Rivers traversal | Yes | First hazard should be obvious and short. Show hazard meter + "retreat is allowed" note if applicable. |
| Resistance gear concept (mitigation, not immunity) | Prompt | MQ_16 -> after acquiring Burn Resistance Gear | Yes | One line only. Reinforce by showing reduced hazard tick. |

### Hub 4.2 - Assembly Line (Relic + Advanced Crafting)

| Teaches | Delivery | Where (exact) | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| New Relic unlock pattern (Anvil -> Construct) | Prompt | Obtain Relic (Anvil) -> first combat after | Yes | Same pattern as World 1 relic tutorial, but shorter (player already knows the loop). |
| Mod socket milestone unlocks | Implicit UI + Prompt | First time a Main Story milestone unlocks sockets | Yes | Use a celebratory UI moment: "New slot unlocked" rather than a dry tooltip. |

---

## World 5 - The Void

### Hub 5.1 - Orbital Ring (Space + Zero-G)

| Teaches | Delivery | Where (exact) | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Space combat minigame basics | Practice + Feedback | MQ_21 "Docking Procedure" -> shoot down fighters | Yes | Teach controls with one prompt; keep failure forgiving or checkpointed. |
| Zero-G traversal rules | Practice + Feedback | MQ_22 "Zero G" -> maintenance shafts / Service Shaft | Yes | Teach: movement changes + momentum/anchor points (whatever the implementation is). |
| Invincible enemies (avoid, don't fight) | Practice + Feedback | MQ_22 -> first Hunter-Killer droid | Yes | Teach the "run" option explicitly. One tooltip on first contact. |
| Timed objective pressure (seal breaches) | Practice + Feedback | SQ_23 "Vacuum Seal" | Optional | If timers are rare, keep this optional. If timers become common, add a mandatory timer later. |

### Hub 5.2 - Deep Ring (Stasis + Finale Setup)

| Teaches | Delivery | Where (exact) | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Anchor Relic / Stasis concept | Prompt + Practice | Obtain Relic (Anchor) -> immediately after, a small fight that showcases Stasis value | Yes | Make Stasis solve a problem (interrupt a wipe, stabilize hazard, etc.). |

---

## World 6 - The Source (Finale)

| Teaches | Delivery | Where (exact) | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| "Nightmare loop" navigation rules (how this hub differs) | Dialogue + Implicit UI | MQ_26 "Fractured Minds" -> first nightmare entry | Yes | Keep it narrative-first. Avoid new mechanics here unless absolutely required. |

---

## Contextual / Failure-Only Tutorials (Always On)

These are not placed in the story flow; they trigger as-needed anywhere the activity exists.

| Teaches | Delivery | Trigger | Notes / Implementation hook |
| --- | --- | --- | --- |
| Cooking timing correction | Script | Cooking minigame -> failure result | Script: `cooking_failure` (key: `cook_timing`). |
| First Aid timing correction | Script | First Aid minigame -> failure result | Script: `first_aid_failure` (key: `first_aid_timing`). |
| Fishing basics reminder | Script | Fishing minigame -> first attempt OR failure | Scripts: `fishing_basics`, `fishing_failure`, `fishing_success`. |
| "You're stuck" nudges (rare) | Prompt | Player idles in a tutorial-critical room for N seconds | Use sparingly; prefer UI pulses before text. |

---

## Appendix A - Tutorial Script Inventory (Current)

This maps existing script IDs in `app/src/main/assets/tutorial_scripts.json` to their intended placement in the plan above.

If a script exists but does not have a clear placement, either (1) assign it a placement, or (2) delete/merge it to reduce drift.

| Script ID | Teaches | Primary placement (exact) | Fallback / Notes |
| --- | --- | --- | --- |
| `movement` | Swipe movement basics | World 1 -> Hub 1.1 -> W1_MQ01 -> Nova's Bunk exit | Merge with `scene_swipe_movement` if redundant. |
| `scene_swipe_movement` | Swipe movement basics (duplicate) | World 1 -> Hub 1.1 -> W1_MQ01 -> Nova's Bunk exit | Candidate for removal once `movement` is canonical. |
| `npc_talk` | NPC interaction menu + Talk | World 1 -> Hub 1.1 -> first mandatory conversation with Jed | Used by current `talk_to_jed` quest tasks in assets. |
| `bag_basics` | Open bag + filters | World 1 -> Hub 1.1 -> first item reward | Also ok as fallback on first "inventory full" warning. |
| `scene_market_locator` | Objective navigation / locator cue | World 1 -> Hub 1.1 -> leaving Jed with an active objective | Currently referenced by `talk_to_jed` quest tasks in assets. |
| `scene_market_journal` | Journal / quest tracking | World 1 -> Hub 1.1 -> after first quest is started and tracked | Keep this short; player should not feel punished for closing it. |
| `scene_tinkering_tutorial` | Tinkering minigame basics | World 1 -> Hub 1.1 -> Jed's Workshop -> Tinkering Table | Use for the "first ever craft" only. |
| `scene_fixers_favor_jed` | Tinkering tutorial beat: talk to Jed | World 1 -> Hub 1.1 -> "Fixer's Favor" onboarding quest (if used) | Optional quest-chain framing; keep aligned with canonical W1_MQ01 tinkering moment. |
| `scene_fixers_favor_table` | Tinkering tutorial beat: open table | World 1 -> Hub 1.1 -> Fixer's Favor -> open table | Same as above. |
| `scene_fixers_favor_craft` | Tinkering tutorial beat: craft | World 1 -> Hub 1.1 -> Fixer's Favor -> craft first repair | Same as above. |
| `scene_fixers_favor_return` | Tinkering tutorial beat: return to Jed | World 1 -> Hub 1.1 -> Fixer's Favor -> wrap | Same as above. |
| `scene_light_switch_hint` | Single-step hotspot action (flip switch) | World 1 -> Hub 1.1 -> early blackout opener (if used) | Good "teach hotspots" micro-tutorial; do not overuse. |
| `scene_lights_out_intro` | Multi-step lights-out onboarding | World 1 -> Hub 1.1 -> Lights Out quest (if used) | If Lights Out is cut, remove these scripts. |
| `scene_lights_out_breaker` | Lights Out: inspect breaker | World 1 -> Hub 1.1 -> Lights Out quest | - |
| `scene_lights_out_trunk` | Lights Out: open trunk + item pickup | World 1 -> Hub 1.1 -> Lights Out quest | Reinforces "items go to bag". |
| `scene_lights_out_install` | Lights Out: install fuse + finish | World 1 -> Hub 1.1 -> Lights Out quest | - |
| `scene_scrap_run_tyson` | Quest handoff + talk prompt | World 1 -> Hub 1.1 -> Trade Row side errand (if used) | Keep optional; use as reinforcement for NPC talk if needed. |
| `scene_scrap_run_shop` | Pickup/loot cue | World 1 -> Hub 1.1 -> Trade Row side errand (if used) | Good for teaching "grab item, auto-goes-to-bag". |
| `scene_scrap_run_return` | Return handoff cue | World 1 -> Hub 1.1 -> Trade Row side errand (if used) | - |
| `scene_ollie_recruitment` | Party/ally portrait concept | World 1 -> Hub 1.1 -> first time an ally follows (if used) | If Ollie is not a party member in canon, repurpose or remove. |
| `fishing_basics` | Fishing minigame basics | World 2 -> Hub 2.1 -> Tideglass Beach fishing spot | Must remain optional content. |
| `fishing_failure` | Fishing failure hint | Any fishing failure | Contextual-only. |
| `fishing_success` | Fishing success tip | Any fishing success | Contextual-only. |
| `cooking_failure` | Cooking failure hint | Any cooking failure | Contextual-only; only keep if cooking ships. |
| `first_aid_failure` | First Aid failure hint | Any first aid failure | Contextual-only; keep the minigame forgiving. |

---
