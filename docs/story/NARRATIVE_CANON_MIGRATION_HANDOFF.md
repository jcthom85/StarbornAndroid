# Starborn Narrative Canon Migration Handoff

**Prepared:** July 14, 2026
**Purpose:** Portable implementation specification for applying this narrative repair to a different, mostly similar Starborn checkout with Codex CLI.
**Source state:** Based on the uncommitted narrative/runtime diff in `StarbornAndroid`; this is a semantic migration guide, not a patch file.

## 1. How to use this handoff

Copy this document into the destination checkout and give Codex CLI the prompt in [Section 12](#12-ready-to-paste-codex-cli-prompt). The destination version may have newer content, different callback signatures, renamed files, or additional quest logic. Codex should therefore locate equivalent records by meaning and stable IDs, merge the changes, and preserve all unrelated work.

Apply the migration in this order:

1. Establish the canon authority hierarchy and update the four authority documents.
2. Reconcile supporting story/world documents against those authorities.
3. Port player-facing dialogue, cinematics, rooms, quests, events, items, milestones, puzzles, and skills.
4. Add only the runtime behavior the destination engine does not already support.
5. Update unit and device/playtest coverage, then run the destination repository's full validation sequence.

Do not replace whole files from this checkout. Do not assume array order, schemas, event hooks, IDs, or APIs are identical. Where this document gives an exact ID, reuse it if it already exists or is safe to add; otherwise adapt the destination's semantic equivalent and document the mapping.

## 2. Outcome of the rewrite

The rewrite removed several conveniences and contradictions that weakened the opening and later campaign:

- Nova no longer survives because of prior Chime exposure, ancestry, genetics, fate, or a relic choosing her.
- The opening now contains a visible mechanical cause-and-effect chain that the player later operates during the Tuning Fork encounter.
- The Chime is consistently an identity key and paired beacon, never a battery, weapon, thruster, or Shield bypass.
- The Bridge is unambiguously Echo #2, recovered separately and powered separately.
- Gh0st's first protective act is voluntary; a failsafe only creates a pause in which he can choose.
- Architect and Aethel history are separated: Architects made the Echoes and exploited the Aethel.
- World travel now has a coherent escalation from low-altitude flight, to internal curtain crossing, to outer-Shield breach and orbit.
- Routine Source use no longer spends memories, and the ending preserves Nova's identity and recollections.
- The finale wins a regional opening rather than instantly defeating the entire Dominion.
- Character arcs are expressed through changed behavior and technical cooperation instead of destiny language or thesis speeches.

In the source checkout the implementation affected **63 intentional files**: 42 story documents, 15 production assets, one runtime file, two unit-test files, and three Maestro flows. This handoff is a sixty-fourth, newly added guide and is not counted as an implementation change.

## 3. Canon authority and non-negotiable rules

### 3.1 Authority order

When sources conflict, resolve them in this order:

1. **Master Story Bible** — plot, campaign causality, antagonist plan, transitions, and ending.
2. **Lore Bible** — cosmology, technology, artifacts, Source costs, and terminology.
3. **Character Arcs** — beliefs, relationships, choices, and emotional progression.
4. **Writer's Handbook** — prose, dialogue, UI copy, and tonal execution.
5. **World and hub documents** — implementation elaboration only.
6. **Game assets** — production expression of canon, never the authority used to settle a contradiction.

`Characters.md` and `Emotional_and_Conflict_Map.md` are supporting summaries subordinate to the first three authorities.

### 3.2 Canon locks

| Subject | Authoritative rule |
| --- | --- |
| Nova | A 22-year-old adult with no Aethel ancestry, genetic compatibility, chosen status, or prior Chime exposure. |
| Tuning Fork | Woken by Nova's ungoverned cutter through a buried conduit; counter-tuned through a cutter–suit–operator calibration loop. |
| Echo mark | An Architect protocol signature written into Nova's nervous system by the damaged Fork handshake. |
| Later Echoes | Detect the Fork protocol signature, not Nova personally. A handshake permits danger, not mastery or obedience. |
| Source cost | Pain, fatigue, sensory crossover, lost perceived seconds, and neurological instability. Memory is never routine fuel. |
| Chime | Orion's identity key, paired beacon, and imprint carrier. It supplies no sustained energy or reality-editing ability. |
| Bridge | Separate Architect Echo #2. It connects distinct willing minds without merging them. |
| Architects | Made the six Echoes and used Aethel consciousness as living material during the Great Silence. |
| Aethel | Victims, survivors, operators, adapters, and preservers of Architect systems; not the Echoes' creators. |
| Gh0st | A distinct person built around a partial derivative of Elara's neural map, without her memories or identity. |
| Source Lock | An Elara-calibrated failsafe that pauses one lethal action. It issues no protection or recruitment command. |
| Travel | W2: low-altitude to Spire. W3: Lens crosses an internal curtain to Foundry. W4/W5: Lens + Engine + Arrays breach outer Shield to Ring. |
| Finale | Burns out Nova's Source access but preserves memory and identity; tinnitus and hand tremor remain. |
| Political result | Vale and Harmony fall locally; the evidence is broadcast and a regional opening forms. The wider Dominion survives. |

## 4. Complete narrative change specification

### 4.1 Prologue: establish the Chime before it becomes a plot tool

Add a cold open set twenty years earlier in Sector 9:

- Fresh Dominion construction is bolted over older Architect geometry.
- A Source Beast breaches containment and bends the far doors inward.
- Orion tears a brass Chime from its signal cradle.
- He launches it through an emergency tube as both distress beacon and decoy.
- Its departing identity signal calls for help and draws the Beast away.
- Orion orders his remaining signal muted and enters stasis.
- The Chime continues broadcasting through the dark.

The source implementation uses cinematic ID `intro_prologue`, with `Lab PA` as a non-voiced/system profile. If the destination startup selector already prefers `intro_prologue`, adding the asset is enough; otherwise explicitly queue it before the new-game spawn/fade actions.

### 4.2 Nova's opening voice and flaw

Replace a passive, frightened, or vague introduction with active survival logic:

- Nova believes, “No one is coming. If I stay useful and owe nobody, I can survive alone.”
- Quota pressure is immediate: shift buzzer, first drill cage, absence penalties, scrip, hunger, and four hours of sleep.
- She has deliberately removed the mining cutter's safety governor because factory specification cannot make quota.
- Her independence is practical and dangerous rather than meek. She pushes machinery and herself past safe limits instead of admitting need.
- Jed and Nova disagree about survival strategy: preservation and repair versus overextension. Their conflict is not “stay here versus dream of freedom.”
- Use concrete physical detail in the bunk and workshop: drill vibration, tools, one ration, a metal bird Jed made from a fan blade, cold-loop whine, scorched insulation, and breaker behavior.
- Refer to Nova neutrally as a woman, miner, mechanic, operator, or by name. “Girl” should only be a deliberate relationship-specific address. Active Boggs dialogue changed “Cutter girl” to “Cutter.”

### 4.3 Exact Tuning Fork causality

This is the key repair. Preserve every link in the chain:

**Implementation-status distinction:** the authority/world documents specify a player-visible Flux Liner repair/setup and player-performed governor bypass before the test. The current production assets only grant the Flux Liner in Jed's dialogue, establish that Nova “pulled the safety governor again,” and make the Cryo-Inductor rebuild playable. The Homestead production lock explicitly marks the fuller Flux/governor interaction as pending. For the destination version, implement the complete intended flow—repair/setup both protections, let the player perform or explicitly confirm the bypass, then test—rather than treating the source asset shortcut as canon-complete.

1. Dominion mining cutters descend from crude Architect phase technology. Factory governors block frequencies that destabilize machinery, nerves, and phase boundaries.
2. Nova removes her cutter's governor to widen the phase sweep and make quota.
3. Jed repairs its **Cryo-Inductor** and patches her suit-linked **Flux Liner** before she tests it.
4. The Cryo-Inductor can absorb a short thermal spike. The Flux Liner contains sacrificial strips that can route one major discharge into a proper ground. Neither was designed for Source contact.
5. Nova test-fires the cutter in the workshop. The prohibited sweep enters a buried Architect conduit, blows a sector breaker, wakes the dormant Tuning Fork, and cracks its failing chamber seal. The terminal attaches the fault to Nova's operator ID, while the surviving damage report retains the cutter ID.
6. Zeke later changes the liability classification enough to prevent immediate recycling, but the damage report survives. Boggs therefore has a causal reason to assign Nova to Sector 4 to clear the interference she caused.
7. In the mine, the Fork's damaged calibration call repeats the same pulse and destabilizes the structure.
8. Nova recognizes harmonic feedback and uses the cutter diagnostics to answer it. This is learned mechanical reasoning, not inherited intuition.
9. The player brings three values into phase: **87 kHz phase sweep**, **68% cold loop**, and **180° ground phase**.
10. The damaged cradle interprets the cutter–suit–operator circuit as a calibration rig and opens an incomplete neural handshake through the available loop.
11. The Cryo-Inductor absorbs the thermal spike and fuses. Remove/consume the functional component.
12. The Flux Liner shunts most of the neural discharge into the chamber floor. Only its sacrificial strip burns open; the armor remains equipped and otherwise mechanically intact.
13. Nova experiences pain, tinnitus, a hand tremor, sensory crossover, and a brief unperceived interval. The active cinematic shows a half-second clock skip, while higher-level story documents describe several lost seconds; the destination should select one consistent duration. Her memories and identity remain intact.
14. The Fork writes the **Echo mark**, an Architect protocol signature, into her nervous system.
15. Project Harmony sensors recognize a live Architect handshake. Thorne changes Nova's status from disposable liability to capture-only mobile interface, while local containment still behaves lethally.

Why this did not happen previously:

- The chamber was dormant, sealed, and physically inaccessible before the workshop surge reached the conduit.
- Standard cutter governors cannot enter the required band.
- Workers and ordinary sensors interpret the response as breaker trouble or magnetic interference.
- Harmony's remote probes suppress feedback for safety, preventing the uncontrolled call-and-response the damaged Fork requires.
- Another sufficiently skilled operator with an ungoverned phase tool, working thermal protection, grounding, access, and the same willingness to answer could theoretically survive.

Do not use “prior exposure,” ancestry, blood, compatibility genes, prophecy, recognition of Nova as a person, or the Fork wanting her.

### 4.4 World 1 progression changes

`Wake Up Call` must not complete when the Cryo-Inductor is crafted. It completes only after the live cutter-test cinematic finishes.

Reference flow from the source version:

- Craft/rebuild cold loop.
- Set `ms_w1_mq01_cryo_repaired`; keep `w1_mq01` active and `w1_mq02` locked.
- Start Jed's post-repair argument about restoring the governor.
- Nova insists on one test with liner grounded and cold loop at 68%.
- Trigger `w1_mq01_cutter_surge` and play `scene_cutter_surge`.
- The key exchange is Jed's “Kill it!” followed by Nova's “It is off. That hum is not mine.”
- After the cinematic callback, complete MQ01; set existing `ms_w1_mq01_complete` and new `ms_w1_mq01_cutter_surge`; start/track MQ02; advance it to `reach_checkpoint`; set existing `ms_w1_mq02_clearance_ordered`; and unlock node `admin_gate`. Preserve the existing milestones because they gate downstream equipment and progression.

During the Fork encounter:

- Convert the `echo_heart` interaction from generic touch to tuning puzzle `w1_echo_counter_tune`.
- Rename the success action from generic `touch_relic` to `w1_mq03_touch_relic` so it matches the puzzle success event.
- Lock the emergency exit until `echo_heart.relic_synced` is true.
- When the puzzle succeeds, mark `touch_tuning_fork`, play `scene_relic_sync`, and wait for its callback. The callback marks `survive_source_sync`; sets `echo_heart.relic_synced`, `ms_w1_mq03_echo_marked`, and `ms_w1_mq03_liner_ground_spent`; consumes `functional_cryo_inductor`; retains `nova_flux_liner`; grants `tuning_fork`; unlocks `nova_blast_wave`; presents the Source Art tutorial; completes MQ03; sets existing `ms_w1_mq03_complete`; and reveals/unlocks `launch_bay`. Preserve `ms_w1_mq03_complete` because later equipment, shops, and room variants depend on it.
- Rename/rewrite memory-themed nearby content as signal feedback or suit-clock stutter. Do not imply routine memory erosion.

Jed's death remains a deliberate choice under structural pressure. It is not punishment for Nova being unready, and the story must not imply that fighting better could have saved him.

### 4.5 Chime lifecycle and restrictions

The Chime must remain one object with one bounded capability set:

1. Orion launches it during the prologue as identity-bearing distress beacon and decoy.
2. Dominion eventually recovers it as salvage; it filters down to Jed as a “Ghost Signal Cell.”
3. Jed gives it to Nova because old launch systems recognize Orion's authorization signature.
4. Zeke connects it to the ore pod's **authorization/navigation bus**, never its power core.
5. The pod launches on its own power. The Chime opens the launch interlock and its paired beacon locks navigation toward Sanctuary.
6. Zeke cannot override the forced route and blames himself for making the splice without understanding it. He does not blame Nova.
7. In World 2 the crew recovers it from the wrecked navigation console, not a reactor housing.
8. It authenticates Orion at the Temple Gate. Sanctuary's reserve cells release him from stasis.
9. It later authenticates Orion's identity to the Bridge/Astra interface. Salvaged conduits and the ship supply all startup current.

Never portray the Chime as a battery, fuel cell, weapon, flame source, thruster, Source burst, universal key, Bridge Echo, Bridge reboot device, or outer-Shield bypass. The Chorus may strengthen its direction like wind on water, but cannot supply thrust, steer a living pilot, or override one. The Source Beast follows the Chime's intermittent beacon as a territorial intrusion; it cannot feed on or draw energy from the Chime.

### 4.6 Architects, Aethel, and the six Echoes

Clarify the cosmology everywhere it is active:

- The Architects were engineers who tried to solve suffering through control.
- They made the six Echoes: Tuning Fork, Bridge, Lens, Anvil, Anchor, and Key.
- They exploited Aethel consciousness as living material while building the Great Silence.
- The Aethel are not primitive Echo makers. They survived, adapted, preserved, hid, and operated some Architect systems.
- Sanctuary is Architect-built and later adapted/sealed by Aethel survivors.
- Aethel burial and ancestral content must name Aethel, not “first Architects.”

The six functions remain:

| Echo | Function / player capability |
| --- | --- |
| Tuning Fork | Opens/destabilizes matter–Source boundaries; Blast. |
| Bridge | Connects distinct minds without merging; Link. |
| Lens | Perceives Source structures and phase corridors; Scan. |
| Anvil | Shapes matter; Construct. |
| Anchor | Stabilizes edits to reality; Stasis. |
| Key | Grants root-level Source authority; Ascension/root access. |

### 4.7 Bridge, Link, and the World 2 quest order

The Bridge is Echo #2, separate from the Chime and from an optional archive record.

Required order:

1. Survive the crash and recover the Chime from navigation.
2. Follow its paired Sanctuary receiver.
3. Authenticate the Temple Gate; reserve cells wake.
4. Stabilize Sanctuary and awaken Orion on facility power.
5. Recover the separate Bridge Echo from its sealed cradle.
6. Return to Canopy Ridge and confront Gh0st.
7. Nova's Fork mark trips the Source Lock and pauses one lethal action.
8. The Source Beast attacks; Gh0st freely turns his weapon toward it and protects the party.
9. Complete Orion's Anchor Drill before `The Hunter` closes or Link unlocks.
10. Return to Sanctuary, mount the Bridge in the Astra, feed it with salvaged conduits, authenticate it with the Chime, and launch.

Anchor Drill staging:

- Nova first tries to carry Link alone and begins burning out.
- Orion stops her: the Bridge connects; it does not make one body carry the circuit.
- Zeke clips a physical ground lead to Nova's bracer.
- Gh0st voluntarily offers his armored hand; no order forces him.
- Orion holds the counter-tone.
- Nova opens Link, Zeke's wound stabilizes, and every thought remains its owner's.
- Concise payoff: “Link. Not merge. I get it.”

The optional Hidden Reliquary contains a **Bridge calibration record**, not another Echo or Link Relic. It describes consent checks, separate carrier bands, and physical grounding. Unlock it after the mural/Ancient Echoes sidequest.

### 4.8 Gh0st and Elara

Replace copy/backup determinism with distinct personhood:

- Dominion built Unit 734 around a **partial derivative** of Elara's neural map.
- That derivative created compatible relay architecture but did not transfer her memories, identity, soul, or destiny.
- Elara interacted with and trained 734; they formed a real sibling relationship before he understood how he was made.
- “Backup” is Dominion's inventory label and Gh0st's fear, not the narrative truth.
- Nova's Fork mark creates a threshold match in an Elara-calibrated Source Lock. The lock interrupts one lethal trigger and provides no command.
- Gh0st tests that he can continue. When the Beast attacks, choosing to protect the crew is his first unsupplied decision.
- His override key can pause his kill suite long enough for him to choose; it cannot control him.
- At the Foundry, Rylos orders an execution. Gh0st refuses because he will not kill on command, not because of a loophole.
- On the Ring, Elara deliberately calls him “brother,” affirming the relationship they formed.
- Phantom Logs and related Chime decryptions carry resonance imprints of Elara and Gh0st's prior interactions. They recover evidence of an existing relationship; they must not imply that Gh0st never knew her, inherited her memories, or is chasing a stranger encoded in his hardware.

The preferred authority phrase is **Elara-calibrated Source Lock**. Lower technical prose may explain that Dominion inherited its recognition layer from Architect collision-safety logic, but that explanation must not turn Nova into Elara or make the failsafe a protection directive.

### 4.9 Travel and barrier logic

Keep two related but distinct barrier systems:

- **Internal sector curtains:** isolate facilities, ground routes, districts, and low-atmosphere corridors.
- **Outer Planetary Shield:** hard deck controlled from the Orbital Ring; seals the labor world and controls orbital/off-world travel.

Travel escalation:

- **World 2:** Repaired Astra flies at low altitude along a service airlane to the Spire. It does not attempt orbit or contact the outer Shield.
- **World 3:** Lens reveals a timing seam in an internal curtain. The Astra crosses at low altitude into Sector 7/Foundry.
- **World 4 to 5:** Lens reveals the rotating outer-Shield corridor; Deep-Core Engine supplies orbital velocity; Phase-Cutter Arrays hold the corridor open long enough to ascend.
- **At the Ring:** The Arrays cool after the breach; ordinary ship weapons handle the fighter screen.

Repurpose legacy W2 sky rooms as canopy flight, storm corridor, Spire airlane, security buoy, local traffic-curtain controller, and permitted-airspace pushback boundary. The outer Shield should remain a distant visible ceiling and future problem.

### 4.10 Lens and Anvil behavior

Neither artifact judges or recognizes Nova as a chosen individual:

- Lens instruments detect the Fork protocol scar in her signal and begin a dangerous handshake.
- The mark permits contact and exposes her to the compliance network; it grants neither mastery nor obedience.
- The Anvil's damaged Foundry cradle detects the same protocol and amplifies unstable input.
- Nova's first Construct collapses because of noisy signal and damaged hardware, not doubt, worthiness, judgment, or the relic testing her.

### 4.11 Antagonist causality

**Mara Thorne:**

- Dominion controls facilities around several Echoes but cannot safely activate or combine them because its suppression probes prevent valid feedback handshakes.
- Once Nova survives, Thorne treats her as a mobile protocol interface.
- Thorne tracks the crew and allows Nova to expose sealed systems, planning to collect both operator and Echo suite later.
- Her motive remains profit, monopoly, leverage, and control—not salvation.
- In World 5 she admits Harmony was a product-cage and that she exploited Nova. The admission establishes culpability but neither redeems nor absolves her before Vale executes her.

**Arden Vale:**

- His belief is constant: separate selves inevitably create suffering, so one coordinating will must eliminate variance.
- W1 seeds calm policy language about preserving “useful variance.”
- W3 has him condemn ungoverned variance.
- W4 coup has him state that one will must replace separate wills.
- W5 has him name individual consciousness as the source of pain.
- W6 enacts the Soloist conclusion.
- He mistakes certainty for moral qualification; his power and candor escalate, not his ideology.

### 4.12 Source cost, climax, and ending

Routine Source use causes pain, fatigue, sensory distortion, signal lag, lost perceived seconds, and accumulating neurological instability. Personal memory is threatened only by catastrophic uncontrolled failure; it is never spell fuel or a menu of sacrifices.

The Key answers only when the **five accumulated Echo protocol signatures** resonate through the crew's learned living circuit and complete the access pattern. It does not recognize Nova's hand, identity, or ancestry.

In the finale, show each arc through action:

- Zeke manually keeps the failing ground path open after the interfaces reject him.
- Gh0st takes the force aimed at the crew because protection is his choice.
- Orion holds the counter-tone.
- Nova trusts the circuit, reaches the Key, and relinquishes root access.
- The Chorus offers Orion real ancestral continuity. He answers “not yet” and remains embodied with the living; the Chorus is neither a lie nor death.

Nova uses the six Echoes to collapse Vale's forced network and open a **regional corridor** through the planetary curtain. The act permanently burns out her Source connection while preserving her memories and identity. The epilogue confirms that she remembers Tideglass Beach and the crew. A thin signal whine/tinnitus and an intermittent tremor in her tuning hand remain.

Vale and Project Harmony are stopped. Thorne's death creates a regional power vacuum, and broadcasting the evidence gives the labor world and neighboring sectors a chance to organize. The wider Dominion remains. The resolution is an opening and future work, not an instant galactic victory.

## 5. Character arc changes

### Nova

- **Lie:** “No one is coming. If I stay useful and owe nobody, I can survive alone.”
- **Truth:** “Survival is shared. Needing people does not make me owned by them.”
- Importance comes from causing the wake-up, recognizing the mechanism, taking the risk, learning technique, and building trust—not destiny.
- In the Foundry she creates room for Gh0st to choose rather than authoring his revenge.
- In the finale she gives up power without giving up herself.

### Zeke

- Owns the navigation splice and failed override without blaming Nova.
- Learns that belonging and care are not conditional on technical usefulness.
- Final proof is the manual ground path, not a speech explaining self-worth.

### Jed

- His opening repairs are the physical reason Nova survives later.
- His Chime gift supplies identity and direction, not energy.
- His chosen death rejects Dominion's calculus of disposable lives.

### Gh0st

- Weapon → interrupted directive → distinct person → brother and protector.
- Emotion appears first through changed priorities, pauses, and actions.
- Protection is voluntary and does not require an ownership declaration.

### Orion

- Recognizes Architect technology built through exploitation of his people, not “Aethel technology.”
- In World 3, he must move beyond contempt for the human operators of technology built through his people's exploitation, helping the crew use it without repeating either empire's control logic.
- The Chorus remains a genuine home he may join later.
- “Not yet” means he chooses the living now, not that the ancestral option was false.

### Starborn

- Begins as Dominion slang for Orion and unauthorized Aethel signals.
- Later attaches to Nova after her impossible launch and crash across controlled boundaries.
- The crew reclaims it only after choosing one another; it means a constellation, not one special star.

## 6. Voice and prose rules

Preserve the project's “Blue Collar Cosmic” voice with these added guardrails:

- **Mechanism before metaphor:** name the governor, sweep, cold loop, ground, and discharge before calling the process music.
- **No chosen-one verbs:** Echoes do not choose, accept, judge, test, want, or recognize Nova personally.
- **Dramatize the arc:** changed behavior should carry an emotional climax before dialogue explains it.
- **Nova:** tactile, sharp, mechanically fluent, and friction-generating. She asks “What is feeding back?” or “Can I break it?”, not generic lore questions.
- **Jed:** gruff, protective, and technically specific. Care arrives as repairs, food, warnings, and preparation.
- **Zeke:** corporate HR cadence used as armor and comedy; growing honesty should not erase his verbal identity.
- **Gh0st:** clipped statements of fact. Feeling emerges through pauses and reordered priorities before introspective language.
- **Orion:** lyrical but technically useful; wonder should not obscure the immediate action.
- **Source pain vocabulary:** erosion, static, pressure, signal lag, lost seconds, tinnitus, tremor. Avoid casual memory-loss language.
- **UI and room copy:** prefer observable behavior over defensive canon disclaimers.

Representative source lines:

- Nova: “It is off. That hum is not mine.”
- Gh0st: “No command attached. The next move is mine.”
- Nova after training: “Link. Not merge. I get it.”

## 7. Production asset and progression changes

### 7.1 Added IDs and values

| Type | ID / value | Purpose |
| --- | --- | --- |
| Cinematic | `intro_prologue` | Orion launches Chime twenty years earlier. |
| Cinematic | `scene_cutter_surge` | Workshop test wakes Fork and logs Nova. |
| Cinematic | `scene_anchor_drill` | Voluntary grounded Link training. |
| Event | `w1_mq01_cutter_surge` | Defers MQ01 completion until surge scene callback. |
| Event | `w2_mq04_beast_ambush` | Spawns Beast after Gh0st confrontation. |
| Event | `w2_mq04_anchor_drill` | Unlocks Link and closes MQ04 after the drill. |
| Event | `w2_sq04_decode_bridge_record` | Decodes optional Bridge safety record. |
| Item | `bridge_relic` | Separate mythic key item, “The Bridge.” |
| Skill | `nova_link` | Source Art; power 90, focus scaling, cooldown 4, regen; tags source/heal/support/aoe. |
| Puzzle | `w1_echo_counter_tune` | 87 kHz / 68% / 180° mechanical handshake. |
| Tutorial | `link_unlock` | Explains grounded, willing, non-merging Link. |

Added milestones:

- `ms_w1_mq01_cryo_repaired`
- `ms_w1_mq01_cutter_surge`
- `ms_w1_mq03_echo_marked`
- `ms_w1_mq03_liner_ground_spent`
- `ms_w2_bridge_recovered`
- `ms_w2_bridge_record_decoded`
- `ms_w2_bridge_installed`
- `ms_w2_link_unlocked`

Added quest tasks:

- `w2_mq03.recover_bridge_relic`
- `w2_mq04.complete_anchor_drill`

Added dynamic room-state keys:

- `echo_heart.relic_synced`
- `sector9_canopy_ridge.hunter_confronted`
- `sector9_canopy_ridge.beast_defeated`
- `sector9_canopy_ridge.anchor_drill_complete`
- `sector9_archive_secret_stash.bridge_record_decoded`

### 7.2 Existing records rewired in place

Locate and merge these stable records in an older checkout; do not create duplicates under new names.

| Category | Existing IDs | Migration role |
| --- | --- | --- |
| Items | `ghost_signal_cell`, `tuning_fork`, `mining_pistol`, `nova_flux_liner`, `cryo_inductor`, `functional_cryo_inductor`, `gh0st_override_key`, `the_anvil` | Rewrite Chime, Fork, cutter, grounding, thermal, autonomy, and non-judgmental Anvil descriptions. |
| Startup/cinematics | `new_game_fade_in`, `scene_relic_sync`, `new_game_spawn_player_and_fade` | Add quota PA/copy, mechanical Fork handshake, and serialized prologue-before-spawn behavior. |
| World 1 events | `w1_mq01_cryo_inductor_repaired`, `w1_mq03_touch_relic` | Delay MQ01 completion; route puzzle success through exact handshake tasks, state, milestones, inventory, skill, and launch unlock. |
| World 2 events | `w2_mq03_align_complete`, `w2_mq04_confront`, `w2_mq04_victory`, `w2_mq05_reboot` | Grant/recover Bridge, stage Gh0st and Beast states, defer Link/MQ04 completion to the drill, and install/power/authenticate the Bridge. |

The generic `touch_relic` action identifier is the exception: replace it with `w1_mq03_touch_relic` across the room, puzzle success event, tests, and any other active reference in one atomic change.

### 7.3 Important sequencing behavior

- Bridge recovery in this source version occurs automatically when the stasis-alignment completion event resolves; it is not a separate pickup interaction despite the quest-task wording.
- Installing the Bridge consumes/removes `bridge_relic`; it does **not** consume the Chime.
- Keep legacy IDs such as `w2_mq05_reboot`, task `reboot_bridge_relic`, and action `w2_sq04_collect_relic` if the destination already references them. Change their prose/semantics rather than renaming them without a save migration.
- The Flux Liner remains equipped and stat-identical after the handshake. The spent protection is represented by a milestone.
- Beast victory recruits Gh0st but does not complete MQ04, unlock Link, or start MQ05. After the Anchor Drill cinematic callback, mark `complete_anchor_drill`, set Ridge state `anchor_drill_complete`, unlock `nova_link`, set `ms_w2_link_unlocked`, show the Link tutorial, complete MQ04, set existing `ms_w2_mq04_complete`, and start/track MQ05.
- MQ01 similarly completes only after the cutter-surge cinematic callback, not after crafting.

### 7.4 Room/action behavior

Canopy Ridge no longer contains the Beast unconditionally. Stage its actions using:

- `show_when_milestone` for the post-Orion confrontation;
- `show_when_state` for “Face the Beast” and “Anchor Drill”;
- `hide_when_state` after each step is resolved.

The source runtime added these three raw action-map fields. If the destination already has equivalent condition lists, use the native system instead of duplicating this schema.

### 7.5 Voice profiles

- Add `Lab PA` and `Unknown System` as non-voiced/system speakers.
- Correct Thorne to female and Vale to male if the destination retains the reversed profiles.

## 8. Minimal runtime support added in the source version

The source `ExplorationViewModel` needed three behaviors. Port them only if the destination engine lacks equivalents.

1. **Serialized bootstrap cinematics:** play queued startup cinematics one at a time and invoke the next only after completion. Run bootstrap player actions after the cinematic queue is empty. Do not place the new-game black screen over a queued prologue.
2. **Event-driven encounter entry:** `spawn_encounter` still emits its notification, then resolves the explicit/current room and valid enemy IDs. When the target is the current room, prepare the pending encounter and enter combat. In this source, `encounter_id` is treated as a comma-separated list of enemy IDs; the Beast ID is exactly `the_beast`. Adapt this if the destination has true encounter-table IDs.
3. **Conditional action visibility:** evaluate `hide_when_state`, `show_when_state`, and `show_when_milestone` before presenting room actions, using dynamic room state with static-state fallback.

Preserve asynchronous callback order. A quest must not advance before its cinematic has actually completed.

The source changes do not have direct unit coverage for these three runtime behaviors. The destination implementation must add tests that verify:

- queued bootstrap cinematics serialize before bootstrap actions and remain visible instead of being covered by the new-game black screen;
- `spawn_encounter` rejects invalid IDs, respects the target/current room, prepares the encounter, and emits actual combat entry exactly once;
- `show_when_state`, `hide_when_state`, and `show_when_milestone` react correctly to false, true, missing, and persisted values.

## 9. Tests and acceptance criteria

### 9.1 Unit behavior to port

`Hub1CriticalFlowTest` coverage was changed to prove:

- Cryo repair alone leaves MQ01 active and MQ02 locked.
- `scene_cutter_surge` is requested.
- Completing its callback sets the surge milestone, completes MQ01, and activates MQ02.
- Fork success requires the new event and records Echo mark + spent ground strip.
- Functional Cryo-Inductor count becomes zero; Flux Liner count remains one.
- `echo_heart.relic_synced` becomes true and Blast/Tuning Fork rewards remain intact.
- Save/resume critical paths seed the required component and Flux Liner inventory prerequisites. The current test does not prove equipment-slot retention or stat identity; add that assertion if the destination models equipment separately.

`Hub2CriticalFlowTest` coverage was changed to prove:

- The positive post-confrontation path calls `w2_mq04_beast_ambush` and captures a `the_beast` spawn callback for Canopy Ridge. The existing test does not prove pre-confrontation hiding or real UI-to-combat integration.
- Beast victory adds Gh0st but does not complete MQ04, unlock Link, or start MQ05.
- Anchor Drill plays its cinematic, sets `ms_w2_link_unlocked` and existing `ms_w2_mq04_complete`, unlocks `nova_link`, completes MQ04, and starts MQ05.
- The post-mural sidequest decodes the Bridge calibration record.

Prefer state/milestone assertions over exact prose unless the text itself is the acceptance requirement.

Add destination tests for the uncovered edges:

- before Gh0st is confronted, “Face the Beast” is absent and no Beast encounter can start;
- after confrontation, the action appears, starts real combat once, and hides after victory;
- Anchor Drill appears only after victory and hides after its completion;
- incorrect counter-tune values do not fire success or consume either inventory prerequisite;
- correct values fire the handshake once even if the UI or event is tapped again;
- a missing Cryo-Inductor or Flux Liner cannot silently complete the handshake unless the destination deliberately supplies an equivalent precondition.

### 9.2 Maestro flows changed

- `start_new_game.yaml`: advances the full new prologue before `Wake Up Call`.
- `mainquest_wake_up_call.yaml`: verifies governor/quota rationale, cold-loop and Flux Liner setup, workshop surge, and fault logging before quest completion.
- `mainquest_the_echo.yaml`: operates the tuning UI from 52→87 kHz, 35→68% cold loop, and 0→180° ground phase, then verifies the mechanical-handshake sequence.

The source tap points (`56%,48%`, `62%,59%`, and `50%,70%`) are layout-sensitive. Reconfirm them on the destination device rather than copying blindly. Any naturalism rewrite that changes exact visible copy must also update the corresponding Maestro selectors.

Add or extend a World 2 device flow to verify the complete UI/runtime path: confront Gh0st → “Face the Beast” becomes visible → real combat begins → Beast victory reveals “Anchor Drill” → the drill cinematic completes → Link unlocks and MQ05 starts. None of the three source Maestro edits exercises this new World 2 integration.

### 9.3 Narrative acceptance scan

No authoritative or active player-facing source should claim that:

- Nova has Aethel ancestry, blood compatibility, chosen status, or prior Chime exposure.
- An Echo accepts, judges, tests, recognizes, wants, or remembers Nova personally.
- Aethel created the Echoes.
- The Chime supplies power, fuel, thrust, attacks, reboots the Bridge, or bypasses the Shield.
- Bridge and Chime are the same artifact.
- Gh0st is a full copy or literal backup of Elara.
- Elara's Song or a directive forces Gh0st to protect the party.
- Routine Source use spends memories.
- Nova forgets her life in the ending.
- The Astra reaches or strikes the outer Shield during World 2.
- The crew reaches low orbit at the end of World 3.
- Stopping Vale destroys the entire Dominion.

Negative guardrails in authority documents may contain these words while explicitly rejecting the claim. Review search results semantically.

### 9.4 Validation commands used in the source checkout

Run destination equivalents from the repository root:

```powershell
# Parse every production JSON file.
Get-ChildItem app/src/main/assets -Recurse -Filter *.json | ForEach-Object {
    Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json | Out-Null
}

.\scripts\validate_progression_references.ps1
.\scripts\validate_world1_content.ps1
.\scripts\validate_room_presence.ps1
python .\scripts\validate_dialogue_emotes.py
.\scripts\validate_audio_references.ps1
git diff --check

$env:GRADLE_USER_HOME = "$PWD\.gradle-codex"
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest `
  --tests "com.example.starborn.domain.dialogue.Hub1CriticalFlowTest.wakeUpCallCompletesOnlyAfterTheCutterSurgeCinematic" `
  --tests "com.example.starborn.domain.dialogue.Hub1CriticalFlowTest.world1CriticalPathReachesCrashSiteAfterLaunch" `
  --tests "com.example.starborn.domain.dialogue.Hub1CriticalFlowTest.world1CriticalPathResumesAfterSaveAtLaunchLockdown" `
  --tests "com.example.starborn.domain.dialogue.Hub2CriticalFlowTest"

# After resolving any known baseline failure, run the complete JVM suite.
.\gradlew.bat :app:testDebugUnitTest

# Then run the repository's full verification/lint wrapper and device flows, if available.
# Example in this checkout: .\scripts\verify_world1.ps1
```

Reproduced in this source checkout on July 14, 2026:

- All 45 asset JSON files parsed.
- Progression references: 67 World 1 events, 72 dialogue lines, 0 warnings.
- World 1 content: 81 rooms across 10 nodes, 0 warnings.
- Room presence: 465 rooms, 20 NPCs, 0 duplicate warnings.
- Dialogue emotes: 26 used references, pass.
- Audio references: 68 cues, 0 warnings.
- `git diff --check`: pass; line-ending warnings only.
- Kotlin debug compilation: pass.
- Five directly affected unit tests: pass.
- The three edited Maestro/device flows were **not rerun** during the final audit.

Known source-test caveat: the complete `Hub1CriticalFlowTest` suite still has one stale exact-copy assertion at line 222. It expects “Shield training complete...” while the active event now uses the newer Boggs/loader-bay message. Until that assertion is fixed, the full source JVM suite—and therefore `verify_world1.ps1`, which stops at JVM tests—should be expected to fail. The destination port should update this to a stable state/milestone assertion or reconcile the intended copy before claiming the full suite is green.

### 9.5 Save compatibility and migration checks

Retaining IDs and adding false-by-default milestones helps compatibility, but does not by itself make mid-quest saves safe. Inspect the destination's save-migration system and add idempotent backfills derived from completed quests/tasks rather than blindly granting every new object.

Required backfill decisions:

- completed MQ01 should carry the legacy completion state; if the old save skipped the new surge, choose whether to backfill the surge milestone or stage a safe one-time catch-up without replaying the whole opening;
- completed MQ03 should receive the Echo mark, spent-ground state, `echo_heart.relic_synced`, and existing completion milestone without duplicating the Fork or consuming an item twice;
- completed W2 MQ03 should reflect Bridge recovery, but grant `bridge_relic` only if it has not already been installed;
- completed confrontation/Beast/Anchor tasks should derive Ridge states `hunter_confronted`, `beast_defeated`, and `anchor_drill_complete` so action visibility cannot deadlock;
- completed W2 MQ04 should backfill `nova_link`, `ms_w2_link_unlocked`, and `ms_w2_mq04_complete`;
- progress at or beyond Astra reboot should set Bridge-installed state and must not re-grant a Bridge item that the install should have consumed;
- guard `w2_mq05_reboot` with ownership/backfill logic in an old-save port, because the fresh source flow assumes `bridge_relic` was granted earlier.

Test representative legacy saves at the MQ01 repair boundary, immediately before/after the old Fork interaction, W2 stasis/Bridge recovery, Ridge confrontation, Beast victory, completed MQ04, and Astra installation. Verify that repeated loads do not replay cinematics, duplicate or destroy items, consume the Chime, spawn the Beast twice, re-run tutorials indefinitely, or leave progression blocked.

## 10. Known cleanup to improve during the destination port

These are not reasons to reopen the structure. They are finishing work that should be folded into the newer checkout.

### 10.1 Naturalism pass

Some source lines defend the corrected canon too explicitly and sound like notes to writers. Preserve the mechanism but rewrite the active player-facing wording as observable behavior.

Examples:

- Replace “This is not another Echo” with the pedestal opening around a labeled Bridge calibration record.
- Replace “The relic is not judging her” with the damaged cradle amplifying the phase scar/noise.
- Replace “not recognized Nova herself” with Lens instruments matching the carrier pattern in the scar.
- Replace “not a memory in Nova” with the gate replaying the identity chord stored in the Chime.

Keep explicit “not X” guardrails in authority documentation where precision matters; avoid making normal room copy argue with an earlier draft.

### 10.2 Stale line to fix rather than reproduce

This source tree still contains a lower-ranked line in `world_2_the_wilds/hub_1_jungle_ruins/02_npcs.md` saying Zeke blames Nova for the crash. The authoritative rule is the opposite: Zeke blames himself for the Chime/navigation splice and never turns that guilt on Nova.

### 10.3 Experiential travel gap

The low-altitude Astra route is now canonically correct, and legacy sky rooms were rewritten around it, but the principal transition may still jump directly to the Spire. If the destination version has the capacity, make a short portion of the storm/airlane flight playable so the player experiences the distinction between a low route, an internal curtain, and the outer Shield.

### 10.4 Legacy residues deliberately outside this migration

The source audit noticed unrelated legacy issues such as a duplicate `ollie_intro_scene`, stale qpack relay references, and dialogue conditions tied to inactive quests. Do not delete or rewrite them automatically while porting this narrative package. Report them separately unless they directly contradict active canon.

Also review these semantically related residues in the destination:

- `World2_Exploration_Design.md` may still call “Elara's Song” a key metaphor. The term is valid for actual Elara recordings, but must never name Nova's signal or explain Gh0st's protection.
- `world_3_the_spire/hub_1_lower_city/04_events.md` may frame the Lens primarily as the answer to the outer Shield. The immediate W3 purpose is crossing the internal curtain to the Foundry; Engine and Arrays remain mandatory for the later outer breach.

## 11. Intentional file inventory

This list records where the source version expressed the migration. The destination may organize equivalent content differently.

### Authority and supporting story documents

- `docs/story/Starborn_Master_Story.md`
- `docs/story/Lore_Bible.md`
- `docs/story/Character_Arcs.md`
- `docs/story/Starborn_Writer_Handbook.md`
- `docs/story/Characters.md`
- `docs/story/Emotional_and_Conflict_Map.md`
- `docs/story/Hub_Requirements_Checklist.md`
- `docs/story/World2_Exploration_Design.md`
- `docs/story/_World_Story_Checklists.md`
- `docs/story/hub_ship_astra/01_functionality.md`

### World 1 documents

- `docs/story/world_1_mining_colony/00_quest_list.md`
- `docs/story/world_1_mining_colony/00_storybeat_packet.md`
- `docs/story/world_1_mining_colony/00_world_story.md`
- `docs/story/world_1_mining_colony/hub_1_homestead/00_overview.md`
- `docs/story/world_1_mining_colony/hub_1_homestead/01_quests.md`
- `docs/story/world_1_mining_colony/hub_1_homestead/02_npcs.md`
- `docs/story/world_1_mining_colony/hub_1_homestead/03_locations.md`
- `docs/story/world_1_mining_colony/hub_1_homestead/04_events.md`
- `docs/story/world_1_mining_colony/hub_1_homestead/04_production_lock.md`
- `docs/story/world_1_mining_colony/hub_2_logistics_sector/00_overview.md`
- `docs/story/world_1_mining_colony/hub_2_logistics_sector/01_quests.md`
- `docs/story/world_1_mining_colony/hub_2_logistics_sector/02_npcs.md`
- `docs/story/world_1_mining_colony/hub_2_logistics_sector/03_locations.md`
- `docs/story/world_1_mining_colony/hub_2_logistics_sector/04_events.md`

### World 2 documents

- `docs/story/world_2_the_wilds/00_quest_list.md`
- `docs/story/world_2_the_wilds/00_storybeat_packet.md`
- `docs/story/world_2_the_wilds/00_world_story.md`
- `docs/story/world_2_the_wilds/hub_1_jungle_ruins/00_overview.md`
- `docs/story/world_2_the_wilds/hub_1_jungle_ruins/01_quests.md`
- `docs/story/world_2_the_wilds/hub_1_jungle_ruins/02_npcs.md`
- `docs/story/world_2_the_wilds/hub_1_jungle_ruins/03_locations.md`
- `docs/story/world_2_the_wilds/hub_1_jungle_ruins/04_events.md`
- `docs/story/world_2_the_wilds/hub_2_sector_9_ruins/01_quests.md`
- `docs/story/world_2_the_wilds/hub_2_sector_9_ruins/02_npcs.md`
- `docs/story/world_2_the_wilds/hub_2_sector_9_ruins/03_locations.md`

### Later-world documents

- `docs/story/world_3_the_spire/hub_1_lower_city/00_overview.md`
- `docs/story/world_3_the_spire/hub_2_upper_city/01_quests.md`
- `docs/story/world_3_the_spire/hub_2_upper_city/04_events.md`
- `docs/story/world_4_the_foundry/hub_1_slag_pits/01_quests.md`
- `docs/story/world_4_the_foundry/hub_1_slag_pits/04_events.md`
- `docs/story/world_6_the_source/hub_1_event_horizon/04_events.md`
- `docs/story/world_6_the_source/hub_2_singularity/01_quests.md`

### Production assets and runtime

- `app/src/main/assets/cinematics.json`
- `app/src/main/assets/dialogue.json`
- `app/src/main/assets/dialogue_voice_profiles.json`
- `app/src/main/assets/events.json`
- `app/src/main/assets/hub_node_descriptions.json`
- `app/src/main/assets/hubs.json`
- `app/src/main/assets/items.json`
- `app/src/main/assets/milestones.json`
- `app/src/main/assets/quests.json`
- `app/src/main/assets/recipes_tinkering.json`
- `app/src/main/assets/rooms.json`
- `app/src/main/assets/skills.json`
- `app/src/main/assets/tuning_puzzles.json`
- `app/src/main/assets/tutorial_scripts.json`
- `app/src/main/assets/worlds/world_2.json`
- `app/src/main/java/com/example/starborn/feature/exploration/viewmodel/ExplorationViewModel.kt`

### Tests and playtests

- `app/src/test/java/com/example/starborn/domain/dialogue/Hub1CriticalFlowTest.kt`
- `app/src/test/java/com/example/starborn/domain/dialogue/Hub2CriticalFlowTest.kt`
- `playtests/maestro/start_new_game.yaml`
- `playtests/maestro/mainquest_wake_up_call.yaml`
- `playtests/maestro/mainquest_the_echo.yaml`

Do **not** transplant the source checkout's unrelated workstation/user changes:

- `.idea/deploymentTargetSelector.xml`
- `app/build.gradle.kts`
- `app/google-services.json`
- `app/src/androidTest/java/com/example/starborn/ExampleInstrumentedTest.kt`
- `app/src/androidTest/java/com/example/starborn/NarrativeSystemsInstrumentedTest.kt`

Keep the destination's IDE selection, Firebase credentials/configuration, build settings, and instrumented-test state. Codex should still inspect and update genuinely stale destination instrumented tests; it must not copy this source tree's blanket `@Ignore` annotations as a shortcut. Preserve or replace that coverage with working equivalent tests.

## Appendix A: Exact source-data reference and secondary edits

These details are useful when the destination schema closely resembles this checkout.

### Counter-tune puzzle configuration

- `frequency`: label `Phase Sweep`; range 40–120; initial 52; target 87; tolerance 2; unit ` kHz`.
- `coolant`: label `Cold Loop`; range 0–100; initial 35; target 68; tolerance 3; unit `%`.
- `ground_phase`: label `Ground Phase`; range 0–360; initial 0; target 180; tolerance 6; unit `°`.
- Success event: `w1_mq03_touch_relic`.
- Failure feedback should say the signals are beating against each other and the cold loop is overheating, then allow another adjustment rather than treating failure as a lore reveal.

### Bridge item and Link skill

- `bridge_relic` is a mythic key item named `The Bridge` with aliases `bridge`, `bridge relic`, and `sanctuary bridge`.
- Its description identifies it as an Architect Echo that coordinates distinct signals and requires both Astra power and a recognized identity.
- `nova_link` uses base power 90, focus scaling, cooldown 4, applies regen, and carries `source`, `heal`, `support`, and `aoe` tags.
- No top-level asset record IDs were removed. The generic `touch_relic` player-action trigger was replaced by `w1_mq03_touch_relic`, so migrate every reference atomically. Additive records and semantic rewiring reduce reference churn, but they do not guarantee mid-quest save compatibility; perform the migrations and legacy-save checks in Section 9.5.

### Secondary World 2 content corrections

- Lost Patrol should trace three signals, recover a transceiver and damaged cutter, and repair a Thermal Cutter. The Chime is not a weapon or environmental flame source.
- Tideglass Day should gather herbs/clean meat and craft stabilized resin rather than use Chime energy.
- Ancient Echoes should unlock the optional Bridge calibration record after the mural chord is stabilized; it never awards another relic.
- Canopy Ridge loses its unconditional `boss_arena`/`boss_id` and static `the_beast` enemy. The confrontation, Beast, and Anchor Drill are state-gated actions instead.

### Other production copy changes

- Tuning Fork, mining cutter, Flux Liner, Cryo-Inductor, functional inductor, Chime, Bridge, Gh0st override key, and Anvil descriptions were rewritten to match the corrected mechanics.
- Quest summaries, milestones, room descriptions, completion messages, hub descriptions, and node copy were synchronized so player-facing text no longer contradicts the authority docs.
- The active opening was tightened around quota, concrete work conditions, Nova's agency, Jed's technical care, and Zeke's involuntary corporate-committee humor.
- The launch/crash text now distinguishes authorization, navigation, power, and the missing Shield corridor.
- `ms_w2_mq05_complete` changed in presentation from “Planetary Escape” to “Astra Liftoff,” because the crew remains below the outer Shield.
- W6 rest and epilogue copy explicitly preserves names, jokes, Tideglass memories, and identity while showing tinnitus and tremor.
- Active Boggs dialogue spelling/addressing was normalized, but unrelated legacy documents may still contain the older `Bogs` spelling and should be reviewed separately rather than mass-replaced.

## 12. Ready-to-paste Codex CLI prompt

```text
Read docs/story/NARRATIVE_CANON_MIGRATION_HANDOFF.md completely and treat its migration invariants, sequencing rules, and behavioral acceptance criteria as the requested change set. After migration, the four in-repo authority documents defined by the handoff remain canonical.

This checkout is a different and possibly newer Starborn version. Before editing, capture `git status --short` and a diff/stat, inspect its story authority documents, active player-facing assets, event and room schemas, runtime APIs, tests, and validation scripts, and run/record the available baseline validators and tests. Use that baseline to distinguish pre-existing failures from migration regressions. Port the handoff semantically. Do not copy whole files, overwrite newer content, touch credentials or IDE state, or assume IDs and callback signatures match. Never reset, clean, stash, checkout, or otherwise discard unrelated work. Preserve every unrelated change.

Implement in layers: (1) authority docs and canon hierarchy, (2) supporting world/story docs, (3) player-facing assets and progression, (4) only the minimal runtime support the target does not already provide, and (5) unit tests and Maestro/playtest coverage. Recheck status/diff and validate after each layer. Preserve asynchronous ordering: Wake Up Call completes after the cutter-surge scene, and The Hunter/Link complete after the Anchor Drill, not after the prior crafting or Beast-victory steps. Retain referenced legacy IDs where possible, then implement and test the idempotent mid-quest save backfills required by Section 9.5.

Search all active content for contradictions listed in the acceptance scan. Fix active contradictions, but report inactive or unrelated legacy residues rather than deleting them blindly. During the player-facing prose pass, express the repaired mechanics through observable behavior instead of defensive phrases such as “not another Echo” or “the relic is not judging her.”

Validate after each layer. At completion, parse all JSON, run reference/content/room/emote/audio validators, run git diff --check, compile Kotlin, run targeted and full unit suites, run the three edited Maestro flows, and add/run the World 2 confrontation → Beast → Anchor Drill flow when a device is available. Recheck `git status --short` and diff/stat to confirm only intended files changed. If the destination uses different tools, run its equivalents. Do not claim a check passed unless it was actually run. Report: files changed, ID/API/save-migration adaptations made for this version, baseline versus final validation results, any remaining contradictions, and anything that could not be ported safely.
```
