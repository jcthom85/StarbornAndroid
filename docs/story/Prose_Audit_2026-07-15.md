# Starborn Player-Facing Prose Audit

Date: 2026-07-15

Scope: current Android repository, active player-facing text

Authority: `Starborn_Master_Story.md`, `Lore_Bible.md`, `Character_Arcs.md`, `Starborn_Writer_Handbook.md`

## Executive verdict

Starborn has a strong voice, but it is not yet consistently controlled. The revised opening, the best cinematics, and most item descriptions already deliver the intended Blue Collar Cosmic mix: concrete work, specific consequences, dry character humor, and brief moments of earned wonder. Later room variants and quest-completion popups are substantially more overwritten. They repeatedly personify machinery, stack images, and explain the story's themes after the action has already demonstrated them.

Overall prose assessment: **6.7/10 (promising, uneven, revision-ready).** This is not a recommendation to flatten the game. Its warmth, odd food, worker humor, and occasional strangeness are assets. The needed pass is one of selection: keep the best image, put the mechanism first, and let different text types do different jobs.

Three active lines also conflict with canon or progression logic closely enough to require correction before a general style pass:

1. Orion is described as an “ancient human”; he is Aethel.
2. The Lens is said to recognize something in Nova; the Echo detects the protocol, not Nova's blood, history, or person.
3. A hangar hint says the Bridge wakes the Astra and needs the Chime routed through it. Ship power, Bridge function, and Chime authentication must remain separate.

## Coverage and method

The audit read the complete runtime asset corpus consecutively by system, then compared high-risk language across worlds, speakers, and text types. The structured extraction covered **6,010 string fields / 57,611 words** in the major JSON assets, plus skill-tree labels/effects and hardcoded Kotlin/Compose UI copy. Runtime loaders and presentation code were checked to distinguish shipped text from inactive residue.

Included:

- dialogue, cinematics, quests, quest stages, events, room descriptions, variants, actions, and lock messages;
- items, equipment, recipes, fishing gear, skills, skill-tree nodes, enemies, NPCs, milestones, tutorials, shops, and hubs;
- player-visible system messages, banners, snackbars, fallback descriptions, and accessibility/help copy in Kotlin/Compose;
- speaker voice, mobile readability, mechanical usefulness, canon compliance, and repetition.

Excluded from scoring:

- `.bak` files, archived/source handoff material, test fixtures, instrumentation logs, and debug-only scenario labels;
- identifiers and internal metadata that are never presented to the player;
- image-only world assets.

`quests_base.json` is packaged but is not loaded by `QuestAssetDataSource`; its stale mojibake is recorded below as inactive residue, not scored as campaign prose.

## Scorecard

| Dimension | Score | Assessment |
|---|---:|---|
| Canon accuracy | 8.0 | Broadly aligned after migration; three active lines need correction. |
| Concrete clarity | 7.0 | Opening, items, and combat instructions are strong; later completion copy obscures outcomes. |
| Voice differentiation | 6.5 | Nova and Jed are distinct; Zeke's joke is overextended; Gh0st and Orion receive oversized briefings. |
| Restraint | 5.5 | Too much personification, stacked metaphor, and thematic restatement in rooms and events. |
| Atmosphere | 8.0 | Strong sense of heat, pressure, bad wiring, debt, food, and dangerous infrastructure. |
| Mobile readability | 6.5 | Many dialogue lines are compact; several popups and quest descriptions are paragraph-sized. |
| Mechanical usefulness | 6.5 | Items are useful; skill descriptions and quest logs vary widely in specificity. |
| Consistency | 6.0 | Text types drift into a shared lyrical narrator; button and system-message conventions vary. |

### By content area

| Area | Grade | Finding |
|---|---:|---|
| Playable opening | A- | Clear quota pressure, preparation, bypass, surge, calibration, and material consequences. |
| Cinematics | B+ | Physical and well paced; a few legacy or tutorial lines break register. |
| Dialogue | B | Nova/Jed work well. Long mission briefs weaken Zeke, Gh0st, and Orion. |
| Items and equipment | B+ | The most consistently concise and useful descriptive category. |
| Skills and skill trees | C+ | Uneven mechanical precision; several descriptions announce canon guardrails or character themes. |
| Rooms | C+ | Strong industrial base, but World 4 is over-figurative and World 5 contains obvious template reuse. |
| Events and completion popups | C | Highest concentration of trailer lines, summaries, and theme declarations. |
| Quests and stages | B- | Early objectives are clear; later entries become long procedural/thematic chains. |
| Milestones | C+ | Functional but repetitive, inconsistent in viewpoint, and occasionally misleading. |
| Tutorials and shops | B | Mostly direct, readable, and voice-appropriate. A few lines overexplain canon. |
| Hardcoded UI copy | B- | Generally functional; crafting can expose internal IDs and control vocabulary is inconsistent. |

## Priority issue ledger

### Critical: active canon contradictions

#### C-01 — Orion's species

- Location: `app/src/main/assets/npcs.json`, NPC `orion`, `description`
- Current: “An ancient human awakened from stasis, possessing high focus and connection to Architect resonance.”
- Problem: Orion is Aethel, not human. “Possessing high focus” also reads like internal stat documentation.
- Revision: **“An Aethel navigator awakened from stasis, trained to read Architect signal structures and keep a crew alive inside them.”**

#### C-02 — Lens personally recognizes Nova

- Location: `app/src/main/assets/rooms.json`, room `spire_archive_vault`, action `lens relic`, `condition_unmet_message`
- Current: “The Lens floats inside a containment field, already reacting like it recognizes something in Nova.”
- Problem: This implies ancestry, prior exposure, or personal recognition. The canonical mechanism is that the Echo detects the protocol carried through the crew's calibrated system.
- Revision: **“The Lens floats inside the containment field. Its Echo rises when Nova's calibrated cutter-suit signal reaches the glass.”**

#### C-03 — Bridge, ship power, and Chime conflated

- Location: `app/src/main/assets/rooms.json`, room `sector9_hangar_bay`, action `bridge relic`, `condition_unmet_message`
- Current: “The bridge relic can wake the Astra, but it needs the recovered conduits and the Chime routed through it first.”
- Problem: The Astra's current powers the Bridge; the Chime authenticates Orion. The Chime is not a power component routed through the Bridge to wake the ship.
- Revision: **“The Astra needs her power conduits restored first. Once the Bridge has ship current, the Chime can authenticate Orion at the console.”**

### High: functional or systemic prose problems

#### H-01 — Event popups repeatedly turn outcomes into trailers

- Locations: `events.json`, especially World 3 onward; examples include `w3_mq12_assemble_planning`, `w3_mq13_enter_lobby`, `w4_mq19_open_anvil_cradle`, `w5_mq22_find_thorne`, `w6_mq28_reach_singularity`, and `w6_sq30_fit_hull`.
- Pattern: The popup reports the action, adds a metaphor, states its thematic meaning, and previews the next beat. Long examples reach 48–63 words.
- Cost: The player has already performed or watched the action. Re-explaining it slows play and makes every quest share one omniscient lyrical voice.
- Rule: Completion copy should normally state **result + concrete next pressure** in one or two short sentences.
- Representative revision for `w6_mq28_reach_singularity`: **“The lift locks at the singularity platform. Vale is ahead, and the Source pressure is climbing.”** Remove “Love that cannot survive truth is not love”; the climax should prove that idea.
- Representative revision for `w3_mq13_enter_lobby`: **“The lobby doors seal behind the crew. Security is moving from both stairwells.”**

#### H-02 — World 5 room expansion exposes repeated templates

- Location: `rooms.json`, approximately `orbital_executive_dock_scale_01` through `deep_server_farm_scale_04`.
- Pattern: “Ring Service Alcove,” “Ring Pressure Gallery,” “Ring Overlook Niche,” “Ring Control Pocket,” “Ring Quiet Cache,” and “Ring Echo Walk” recur across nodes with near-identical descriptions. Thirty-four actions repeat one of two messages verbatim.
- Cost: The Ring feels generated rather than engineered. Repetition also makes exploration rewards indistinguishable.
- Revision principle: Give each repeated utility space one local job, one failure state, and one trace of its workers or residents. Keep common architecture in nouns, not entire sentences.
- Example transformation: replace generic “maintenance crews would know to look” copy with local evidence such as **“A coolant bypass number is scratched below the official deck label.”** or **“The chalk arrow points toward the pressure-door handwheel.”**

#### H-03 — World 4 industrial spaces carry too many simultaneous images

- Locations: `foundry_forge_anvil`, `foundry_waste_intake`, `foundry_cooling_springs`, `foundry_forge_scale_01`, `foundry_forge_scale_04`, `deep_anchor_chamber_scale_01`, and `deep_anchor_chamber_scale_05`.
- Pattern: bellows become lungs, cables become veins or rosaries, maintenance becomes prayer, rooms confess, machinery argues, and signals crawl—often several in one beat.
- Cost: This is the world that should most strongly foreground mass, heat, slag, tooling, and labor. Constant personification makes the actual plant harder to picture.
- Representative rewrite for the “cable narthex / pulse rosary / maintenance hymn” passage: **“Bundled power cable crosses the chamber in ceramic clamps. Each load cycle knocks the loose clamps against the wall in a steady three-beat pattern.”**
- Representative rewrite for the Forge Anvil variant: **“The feed pistons retract. Heat rolls off the hammer wall, and the anvil locks open for service.”**

#### H-04 — Dialogue is used as a mission-brief container

- Locations: `dialogue.json`; largest examples include `w3_sq11_jax_intro` (43 words), `w3_mq12_jax_success` (40), `orion_w3_mq14_puzzle_intro` (39), `w3_sq12_ghost_intro` (36), and `w3_mq12_planning_table_dialogue` (36).
- Evidence: 15 of 34 Zeke lines, 7 of 16 Orion lines, and 5 of 14 Gh0st lines exceed the handbook's 18-word target. Nova exceeds it only twice.
- Cost: Objectives, destinations, rationale, and character reaction arrive in one tap. Gh0st in particular stops sounding clipped.
- Representative Gh0st revision for `w3_sq12_ghost_intro`: **“Trace is live. Source: Dominion relay stack. I am going in.”** Put destination and reward in the quest objective.
- Representative Orion approach: keep one perceptive image, then a direct instruction: **“The prism is answering in thirds. Rotate the lower mirror until the return signal holds.”**

#### H-05 — Crafting snackbars expose internal item IDs

- Location: `CraftingViewModel.kt`, scrap messages around lines 265–282.
- Current patterns: “No schematic to scrap `$itemId`,” “You don't have `$itemId` to scrap,” and “Scrapped `$itemId` into parts.”
- Problem: Players can see machine identifiers instead of display names. This is a functional copy defect, not merely tone.
- Revision: Resolve the item display name once, then use **“No scrap recipe for {Item Name}.”**, **“You don't have {Item Name}.”**, and **“Scrapped {Item Name} for parts.”**

#### H-06 — Skill descriptions mix combat instructions with defensive canon notes

- Locations: `skills.json`, especially `nova_link`, `zeke_guardian_covenant`, `zeke_unshackled`, and `gh0st_source_balance`.
- Pattern: “No identity is merged,” “without either becoming the other's property,” and “once usefulness is no longer the price of belonging” state guardrails or arc theses instead of explaining the skill.
- Cost: The skills menu sounds like the lore bible. Important targeting, potency, duration, and cooldown information is inconsistently supplied.
- Revisions:
  - `nova_link`: **“Restore party HP and apply Regen through the Bridge ground. Does not share damage.”**
  - `zeke_guardian_covenant`: describe the actual guard/redirect effect and duration; preserve consent in the unlock scene, not the tooltip.
  - `zeke_unshackled`: name the stat change and trigger. The current line belongs in character dialogue, if anywhere.
  - `gh0st_source_balance`: **“A clean carrier signal clears one control effect and raises Focus for two turns.”** Avoid implying that Elara supplies a protective command.

### Medium: recurring craft problems

#### M-01 — Quest descriptions combine goal, route, stakes, and theme

Late World 3–5 main quests commonly run 29–38 words. Examples include `w3_mq11`, `w3_mq12`, `w3_mq13`, `w3_mq15`, and `w5_mq22`. Several tell the player what the event means (“workers the city refuses to see”) instead of limiting the journal to action and stakes.

Use this hierarchy:

1. Summary: concrete goal, ideally under 12 words.
2. Description: why now and where, one or two sentences.
3. Stages: one imperative each.
4. Dialogue/cinematic: character response and emotional interpretation.

#### M-02 — Milestone copy is repetitive and changes viewpoint

Many toasts are simply “X is complete,” while descriptions switch between second person, Nova in third person, and omniscient summary. `ms_w2_mq04_complete` uses “Assailant Truce,” which understates or misnames the Beast victory and Gh0st recruitment.

Choose one presentation standard: title as state change, toast as immediate consequence. Example: **“Gh0st joined the crew. Link training is now available.”** only when the progression requirements are actually satisfied.

#### M-03 — Enemy descriptions overuse resonance terminology

Early entries such as `echo_borer`, `dominion_dampener`, `pressure_hauler`, `iron_warden`, `resonance_buoy`, `acoustic_bulwark`, and `siren_skimmer` layer “acoustic,” “neural,” “resonance,” and “frequency” onto otherwise physical machines.

Reserve Source-adjacent vocabulary for a real signal behavior. Lead with what the enemy does: armor, reach, heat, pressure, targeting, or status effect. “A vicious bite attack” is too generic at the other extreme; name the combat consequence.

#### M-04 — Zeke's corporate register risks becoming his whole identity

“Paperwork Mastery,” “Compliance Check,” “Executive Order,” and “Operations Overlord” are individually funny. In aggregate, his dialogue and skill tree lean so hard on the same register that practical competence and fear recede.

Keep the HR cadence under stress, but give more skill names to positioning, shielding, improvised electronics, and keeping other people alive. The joke works best as armor with something visible underneath it.

#### M-05 — Tutorial copy occasionally explains lore defensively

Most tutorials are excellent: direct verbs, named controls, and one concept at a time. `link_unlock` is the exception. “Every thought remains its owner's” and “It does not merge health pools or identities” read as corrective notes.

Suggested player-facing version: **“Link restores the party and applies Regen through the Bridge ground. Damage and status effects remain separate.”** Keep the stronger negative canon guardrail in authority documents and tests.

#### M-06 — Control language and capitalization vary

The UI alternates among “Continue,” “CONTINUE,” “Dismiss,” “GOT IT,” “Got it,” “Close,” and “Back.” These are small strings, but their inconsistency is conspicuous on mobile.

Adopt a UI lexicon: sentence case; **Continue** for advancing a sequence, **Close** for overlays, **Back** for navigation, and **Got it** only for tutorials. Avoid all caps except diegetic system alarms.

### Low: cleanup and inactive residue

- `quests_base.json` contains “Rusthavenâ€”better…” at approximately line 297. The current quest loader uses `quests.json`, so this is not active campaign copy, but the packaged residue should be repaired or removed deliberately.
- `cinematic_text_demo` includes “Narration settles over the scene like a system log with a pulse.” It is debug/gallery material and should not guide production prose.
- `ollie_intro_scene` asks the player to “Touch my name on your HUD.” It is functional but momentarily turns Ollie into tutorial middleware. Prefer a system tutorial immediately after Ollie's in-world line.

## Item and equipment audit

Items are a model for the broader pass. Most descriptions fit in one sentence, identify material or use, and allow a controlled joke. The opening gear is especially clear: the Cryo-Inductor and Flux Liner have legible functions and consequences without turning their descriptions into lore summaries.

What works:

- common supplies use concrete nouns and observable effects;
- food gives the setting warmth and class texture;
- key items usually distinguish physical function from story significance;
- descriptions rarely exceed the space needed to make a choice.

What to adjust:

- Defensive phrases in `ghost_signal_cell`, `nova_flux_liner`, and `bridge_relic` sometimes read like patch notes. Show remaining armor or absent power in the inventory state/effect rows where possible.
- Flavor lines such as “Tastes like yellow” are welcome in moderation. They should remain characterful exceptions, not become the baseline voice.
- `starborn_mod` is appropriately heightened for endgame gear, but “impossible” and “overwhelming” are generic superlatives. A specific material behavior would be more memorable.

Target standard: **material + use + one voice-bearing detail**, usually within 8–18 words.

## Skill and skill-tree audit

The core problem is information architecture, not word count. A few skills provide exact values (`nova_cryo_vent`, `nova_smoke_bomb`, `gh0st_headshot`), while many comparable skills only give flavor. Players cannot reliably compare choices.

Each combat tooltip should use the same order:

1. target;
2. effect and magnitude;
3. duration/status;
4. cooldown or trigger;
5. optional short flavor clause.

Skill-tree node names can carry more personality than tooltips. Even there, distribute each character's register. Nova should sound like field modification, Gh0st like acquisition and execution, Orion like navigation/tuning, Jed-derived skills like preparation and survival, and Zeke like operations with selective corporate euphemism.

## Cinematic audit

The cinematic layer is disciplined compared with the ambient corpus. `scene_cutter_surge`, `scene_relic_sync`, `scene_anchor_drill`, `scene_w6_final_note`, and the epilogue are physical, causal, and speaker-specific. The surge sequence correctly pays off quota pressure, repairs, the deliberate bypass, the test, the buried conduit, the Tuning Fork, the operator record, the counter-tune, the calibration handshake, the fused Cryo-Inductor, the spent ground strip, and the protocol mark without ancestry or memory-loss implications.

Specific issues:

- `scene_complete_gather_broken_gear` says “all three relics return.” These are broken devices/gear, not three relics. Replace with **“All three broken units are back on Jed's bench.”**
- `new_game_fade_in` is the only notably long ordinary step (about 36 words), but its quota setup earns the space. It could still be split for mobile rhythm.
- Source-contact logs may remain stranger than ordinary narration. Their elevated language is justified when the signal itself is the event.

## Dialogue and speaker audit

### Nova

Strongest and most consistent voice. Short, tactile, mechanically fluent. Preserve lines that identify a fault through sound, heat, fit, or tool behavior. Avoid giving Nova abstract theme summaries after she has already made the meaningful choice.

### Jed

Generally excellent. Care arrives through food, spares, warnings, and preparation. His best lines do not announce that he is a father figure. Keep that restraint.

### Zeke

The corporate-HR cadence is funny and defensive, especially when danger makes the euphemism transparent. Shorten his briefings and let practical logistics interrupt the bit. He should occasionally say the plain thing when the armor fails.

### Gh0st

Short imperatives and literal observations work. Five of fourteen lines exceed 18 words; these are the clearest voice mismatch. Move route and objective details into the journal. Avoid poetic abstractions such as “whether grief has an address.”

### Orion

He can carry the highest regular lyricism, but seven of sixteen lines exceed 18 words and several combine perception with full puzzle instructions. Give him one strange observation followed by an intelligible action. “It is off. That hum is not mine.” remains the benchmark.

### Supporting cast

Jax and Thorne have only four lines each, yet most are 28–43 words. This prevents a distinct cadence from forming. Split their speeches and replace general mission language with vocabulary tied to their work, rank, and immediate risk.

## Quantitative pattern notes

These counts are diagnostic, not automatic errors:

- 160 active JSON strings use a “like” or “as if” construction.
- 196 use memory/personhood verbs around rooms, machines, signals, or infrastructure.
- 90 use body-machine vocabulary such as veins, lungs, breath, pulse, or bones.
- 42 contain likely thematic declaration markers.
- Room figurative-marker density rises from roughly 11–13 per thousand words in Worlds 1–3 to 17 in World 4 and 21.5 in World 6.

World 6's increase is largely earned by Source proximity and climax. World 4's is the stronger warning sign because its subject is mass industrial production. The answer is not a banned-word list; it is to reserve personification and bodily imagery for moments when the machine's apparent agency matters.

## Revision principles

1. **Mechanism before metaphor.** Establish what moved, heated, failed, or drew current before selecting an image.
2. **One image per ordinary beat.** If a room contains a chapel, veins, breath, prayer, and memory, choose the one that carries plot or mood and cut the rest.
3. **One job per text type.** Objectives direct; rooms orient; dialogue characterizes; cinematics stage action; system messages report state.
4. **Trust demonstrated themes.** Remove the sentence that explains what the player's completed action “really means.”
5. **Keep lyricism on a budget.** Spend it on Orion, Source contact, revelation, grief, and climax—not routine doors and service corridors.
6. **Preserve material comedy.** Bad food, liability language, improvised repair, and worker shorthand make the world human.
7. **Prefer positive evidence in player copy.** Show independent health pools, intact armor, and a protocol response. Keep explicit negative guardrails in canon documents and tests.
8. **Make mechanics comparable.** Tooltips should not require the player to infer magnitude, target, or duration from flavor.

## Recommended implementation sequence

### P0 — Canon and functional correctness

1. Correct C-01 through C-03.
2. Correct “three relics” in `scene_complete_gather_broken_gear`.
3. Resolve display names before emitting crafting scrap messages.
4. Add narrow reference tests for Orion's species and the Bridge/Chime/power distinction.

### P1 — Highest player impact

1. Rewrite World 3–6 completion popups to result + next pressure.
2. Replace World 5 repeated room templates with locally specific utility spaces.
3. Pass World 4 rooms with a one-image maximum and physical-first descriptions.
4. Split the ten longest mission-brief dialogue entries.

### P2 — Mechanical clarity and voice

1. Normalize skill tooltip structure and exact values.
2. Shorten late quest descriptions and make stages imperative.
3. Audit Gh0st and Orion line-by-line against their distinct cadence.
4. Broaden Zeke's skill naming beyond corporate jokes.

### P3 — Consistency polish

1. Normalize milestone viewpoint and toast function.
2. Normalize UI action vocabulary and capitalization.
3. Replace inactive mojibake and decide whether legacy base assets should ship.
4. Run a final consecutive read-through after changes, not as isolated string review.

## Acceptance criteria for a prose revision pass

- No active line implies Nova's ancestry, prior exposure, prophecy, personal relic recognition, or memory loss.
- Bridge, Chime, ship current, Echo behavior, protocol mark, Link, and the outer Shield retain their separate canonical functions.
- Ordinary room beats use no more than one strong figurative image.
- Quest completion messages normally fit in two short sentences and do not state the theme.
- Gh0st's instructions are clipped; Orion's immediate instructions are actionable; Nova and Jed retain their existing practical strengths.
- Every combat skill presents enough information to compare target, effect, duration, and cooldown/trigger.
- Repeated World 5 room templates are replaced or intentionally differentiated.
- No player-facing message exposes an internal content ID.
- UI actions follow a documented capitalization and navigation lexicon.
- JSON, narrative-reference validators, unit tests touching changed systems, and a consecutive in-game opening read-through pass.

## Final assessment

The game's writing does not need a new identity. It needs firmer editing boundaries. Starborn is most convincing when somebody hears a bad bearing, counts the remaining parts, makes a joke because the alternative is panic, and then encounters one thing the available tools cannot explain. The opening now understands that rhythm. Applying the same restraint to later events and rooms would make the rare lyrical turns land harder—and would let the cast, rather than a shared narrator, carry the themes.
